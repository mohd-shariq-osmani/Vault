import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:path_provider/path_provider.dart';
import 'package:open_filex/open_filex.dart';
import 'package:pdfx/pdfx.dart';
import 'package:share_plus/share_plus.dart';
import '../../models/document.dart';
import '../../providers/vault_provider.dart';
import '../../providers/auth_provider.dart';
import '../theme/colors.dart';
import '../widgets/detail_row.dart';
import '../widgets/glassmorphic_card.dart';
import 'add_document_screen.dart';

class ViewDocumentScreen extends ConsumerStatefulWidget {
  final String documentId;
  final VoidCallback onBack;

  const ViewDocumentScreen({
    super.key,
    required this.documentId,
    required this.onBack,
  });

  @override
  ConsumerState<ViewDocumentScreen> createState() => _ViewDocumentScreenState();
}

class _ViewDocumentScreenState extends ConsumerState<ViewDocumentScreen> {
  bool _revealed = false;
  bool _imageRevealed = true;
  Uint8List? _imageBytes;
  bool _loadingImage = false;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _loadImage());
  }

  Future<void> _loadImage() async {
    final vaultState = ref.read(vaultProvider);
    final docs = vaultState.valueOrNull ?? [];
    final doc = docs.cast<VaultDocument?>().firstWhere(
          (d) => d?.id == widget.documentId,
          orElse: () => null,
        );
    if (doc?.imagePath == null) return;

    setState(() => _loadingImage = true);
    final bytes = await ref.read(vaultProvider.notifier).loadImage(doc!.imagePath!);
    if (bytes == null) {
      if (mounted) setState(() => _loadingImage = false);
      return;
    }

    if (doc.imagePath!.endsWith('.pdf')) {
      try {
        final document = await PdfDocument.openData(bytes);
        final page = await document.getPage(1);
        final pageImage = await page.render(
          width: page.width * 2,
          height: page.height * 2,
          format: PdfPageImageFormat.png,
        );
        await page.close();
        await document.close();
        if (mounted) {
          setState(() {
            _imageBytes = pageImage?.bytes;
            _loadingImage = false;
          });
        }
      } catch (e) {
        if (mounted) {
          setState(() {
            _imageBytes = null;
            _loadingImage = false;
          });
        }
      }
    } else {
      if (mounted) {
        setState(() {
          _imageBytes = bytes;
          _loadingImage = false;
        });
      }
    }
  }

  Future<void> _openAttachment(VaultDocument doc) async {
    try {
      final bytes = await ref.read(vaultProvider.notifier).loadImage(doc.imagePath!);
      if (bytes == null) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            const SnackBar(content: Text('Could not load document')),
          );
        }
        return;
      }

      final ext = doc.imagePath!.split('.').last.toLowerCase();
      final safeTitle = doc.title.replaceAll(RegExp(r'[\\/:*?"<>|]'), '_');
      final tempDir = await getTemporaryDirectory();
      final tempFile = File('${tempDir.path}/$safeTitle.$ext');
      await tempFile.writeAsBytes(bytes, flush: true);

      ref.read(isLaunchingExternalProvider.notifier).state = true;

      final result = await OpenFilex.open(tempFile.path);
      if (result.type != ResultType.done) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(content: Text('Could not open document: ${result.message}')),
          );
        }
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to open document: $e')),
        );
      }
    }
  }

  String _typeName(DocumentType type) {
    switch (type) {
      case DocumentType.paymentCard:
        return 'Payment Card';
      case DocumentType.aadhaarCard:
        return 'Aadhaar Card';
      case DocumentType.panCard:
        return 'PAN Card';
      case DocumentType.driversLicense:
        return "Driver's Licence";
      case DocumentType.vehicleRc:
        return 'Vehicle RC';
      case DocumentType.genericId:
        return 'ID Card';
    }
  }

  IconData _typeIcon(DocumentType type) {
    switch (type) {
      case DocumentType.paymentCard:
        return Icons.credit_card;
      case DocumentType.aadhaarCard:
        return Icons.badge;
      case DocumentType.panCard:
        return Icons.article;
      case DocumentType.driversLicense:
        return Icons.drive_eta;
      case DocumentType.vehicleRc:
        return Icons.directions_car;
      case DocumentType.genericId:
        return Icons.card_membership;
    }
  }

  String _getPrimaryNumber(VaultDocument doc) {
    switch (doc.type) {
      case DocumentType.paymentCard:
        return doc.cardNumber ?? '';
      case DocumentType.aadhaarCard:
        return doc.aadhaarNumber ?? '';
      case DocumentType.panCard:
        return doc.panNumber ?? '';
      case DocumentType.driversLicense:
        return doc.dlNumber ?? '';
      case DocumentType.vehicleRc:
        return doc.rcNumber ?? '';
      case DocumentType.genericId:
        return doc.genericIdNumber ?? '';
    }
  }

  String _getMaskedNumber(VaultDocument doc) {
    switch (doc.type) {
      case DocumentType.paymentCard:
        final n = doc.cardNumber ?? '';
        if (n.length >= 4) return '•••• •••• •••• ${n.substring(n.length - 4)}';
        return '•••• •••• •••• ••••';
      case DocumentType.aadhaarCard:
        final n = doc.aadhaarNumber ?? '';
        if (n.length >= 4) return '•••• •••• ${n.substring(n.length - 4)}';
        return '•••• •••• ••••';
      case DocumentType.panCard:
        final n = doc.panNumber ?? '';
        if (n.length >= 4) return '••••• ${n.substring(n.length - 4).toUpperCase()} •';
        return '•••• ••••••';
      case DocumentType.driversLicense:
        return doc.dlNumber ?? '—';
      case DocumentType.vehicleRc:
        return doc.rcNumber ?? '—';
      case DocumentType.genericId:
        final n = doc.genericIdNumber ?? '';
        if (n.length >= 4) return '•••• •••• ${n.substring(n.length - 4)}';
        return '•••• ••••';
    }
  }

  void _showDeleteConfirm(BuildContext context) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        title: const Text('Delete Document'),
        content: const Text('Are you sure you want to permanently delete this document?'),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancel', style: TextStyle(color: textSecondary)),
          ),
          TextButton(
            onPressed: () async {
              Navigator.pop(ctx);
              await ref.read(vaultProvider.notifier).deleteDocument(widget.documentId);
              if (mounted) widget.onBack();
            },
            child: const Text('Delete', style: TextStyle(color: accentRed)),
          ),
        ],
      ),
    );
  }

  String _buildShareText(VaultDocument doc) {
    final buf = StringBuffer();
    buf.writeln('=== ${doc.title} ===');
    buf.writeln('Type: ${_typeName(doc.type)}');

    switch (doc.type) {
      case DocumentType.paymentCard:
        if (doc.cardholderName?.isNotEmpty == true) buf.writeln('Cardholder: ${doc.cardholderName}');
        if (doc.cardNumber?.isNotEmpty == true) buf.writeln('Card Number: ${doc.cardNumber}');
        if (doc.cardExpiry?.isNotEmpty == true) buf.writeln('Expiry: ${doc.cardExpiry}');
        if (doc.cardType?.isNotEmpty == true) buf.writeln('Network: ${doc.cardType}');
      case DocumentType.aadhaarCard:
        if (doc.aadhaarName?.isNotEmpty == true) buf.writeln('Name: ${doc.aadhaarName}');
        if (doc.aadhaarNumber?.isNotEmpty == true) buf.writeln('Aadhaar: ${doc.aadhaarNumber}');
        if (doc.aadhaarDob?.isNotEmpty == true) buf.writeln('DOB: ${doc.aadhaarDob}');
        if (doc.aadhaarGender?.isNotEmpty == true) buf.writeln('Gender: ${doc.aadhaarGender}');
      case DocumentType.panCard:
        if (doc.panName?.isNotEmpty == true) buf.writeln('Name: ${doc.panName}');
        if (doc.panNumber?.isNotEmpty == true) buf.writeln('PAN: ${doc.panNumber}');
        if (doc.panFatherName?.isNotEmpty == true) buf.writeln("Father's Name: ${doc.panFatherName}");
        if (doc.panDob?.isNotEmpty == true) buf.writeln('DOB: ${doc.panDob}');
      case DocumentType.driversLicense:
        if (doc.dlHolderName?.isNotEmpty == true) buf.writeln('Name: ${doc.dlHolderName}');
        if (doc.dlNumber?.isNotEmpty == true) buf.writeln('DL Number: ${doc.dlNumber}');
        if (doc.dlDob?.isNotEmpty == true) buf.writeln('DOB: ${doc.dlDob}');
        if (doc.dlExpiry?.isNotEmpty == true) buf.writeln('Expiry: ${doc.dlExpiry}');
        if (doc.dlState?.isNotEmpty == true) buf.writeln('State: ${doc.dlState}');
      case DocumentType.vehicleRc:
        if (doc.rcOwnerName?.isNotEmpty == true) buf.writeln('Owner: ${doc.rcOwnerName}');
        if (doc.rcNumber?.isNotEmpty == true) buf.writeln('RC Number: ${doc.rcNumber}');
        if (doc.rcChassisNumber?.isNotEmpty == true) buf.writeln('Chassis: ${doc.rcChassisNumber}');
        if (doc.rcEngineNumber?.isNotEmpty == true) buf.writeln('Engine: ${doc.rcEngineNumber}');
        if (doc.rcExpiry?.isNotEmpty == true) buf.writeln('Expiry: ${doc.rcExpiry}');
      case DocumentType.genericId:
        if (doc.genericIdType?.isNotEmpty == true) buf.writeln('ID Card Type: ${doc.genericIdType}');
        if (doc.genericIdName?.isNotEmpty == true) buf.writeln('Name on ID: ${doc.genericIdName}');
        if (doc.genericIdNumber?.isNotEmpty == true) buf.writeln('ID Number: ${doc.genericIdNumber}');
        if (doc.genericIdExpiry?.isNotEmpty == true) buf.writeln('Expiry Date: ${doc.genericIdExpiry}');
    }
    return buf.toString();
  }

  @override
  Widget build(BuildContext context) {
    final vaultState = ref.watch(vaultProvider);
    final docs = vaultState.valueOrNull ?? [];
    final doc = docs.cast<VaultDocument?>().firstWhere(
          (d) => d?.id == widget.documentId,
          orElse: () => null,
        );

    if (doc == null) {
      return const Scaffold(
        backgroundColor: cinemaBase,
        body: Center(child: CircularProgressIndicator(color: accentIndigo)),
      );
    }

    final grad = gradientForDoc(doc.cardColorIndex);
    final accent = accentForDoc(doc.cardColorIndex);
    final primaryNum = _getPrimaryNumber(doc);
    final maskedNum = _getMaskedNumber(doc);
    final displayNum = _revealed ? primaryNum : maskedNum;

    return Scaffold(
      backgroundColor: cinemaBase,
      appBar: AppBar(
        backgroundColor: cinemaElevated,
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: widget.onBack,
        ),
        title: Row(
          children: [
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
              decoration: BoxDecoration(
                color: accent.withAlpha(51),
                borderRadius: BorderRadius.circular(6),
              ),
              child: Text(
                _typeName(doc.type).toUpperCase(),
                style: GoogleFonts.inter(
                  fontSize: 9,
                  color: accent,
                  fontWeight: FontWeight.w700,
                  letterSpacing: 1,
                ),
              ),
            ),
            const SizedBox(width: 8),
            Expanded(
              child: Text(
                doc.title,
                style: GoogleFonts.inter(color: textPrimary, fontWeight: FontWeight.w700, fontSize: 16),
                overflow: TextOverflow.ellipsis,
              ),
            ),
          ],
        ),
        actions: [
          IconButton(
            icon: Icon(_revealed ? Icons.visibility_off : Icons.visibility, color: textSecondary),
            tooltip: _revealed ? 'Hide details' : 'Reveal details',
            onPressed: () => setState(() => _revealed = !_revealed),
          ),
          IconButton(
            icon: const Icon(Icons.edit, color: textSecondary),
            tooltip: 'Edit',
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => AddDocumentScreen(
                    documentType: doc.type,
                    existingDocument: doc,
                    onSaved: () => Navigator.pop(context),
                  ),
                ),
              );
            },
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline, color: accentRed),
            tooltip: 'Delete',
            onPressed: () => _showDeleteConfirm(context),
          ),
        ],
      ),
      body: ListView(
        padding: const EdgeInsets.all(16),
        children: [
          // Card mockup
          GlassmorphicCard(
            gradient: grad,
            glowColor: accent,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                      decoration: BoxDecoration(
                        color: accent.withAlpha(51),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: accent.withAlpha(77)),
                      ),
                      child: Text(
                        _typeName(doc.type).toUpperCase(),
                        style: GoogleFonts.inter(
                          fontSize: 9,
                          color: accent,
                          fontWeight: FontWeight.w700,
                          letterSpacing: 1.2,
                        ),
                      ),
                    ),
                    const Spacer(),
                    Icon(_typeIcon(doc.type), color: accent, size: 24),
                  ],
                ),
                const SizedBox(height: 20),
                Text(
                  doc.title,
                  style: GoogleFonts.inter(
                    color: textPrimary,
                    fontWeight: FontWeight.w700,
                    fontSize: 20,
                  ),
                ),
                const SizedBox(height: 10),
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        displayNum,
                        style: GoogleFonts.robotoMono(
                          color: textPrimary,
                          fontSize: 16,
                          letterSpacing: 2,
                        ),
                      ),
                    ),
                    if (primaryNum.isNotEmpty)
                      IconButton(
                        icon: const Icon(Icons.copy, size: 18, color: textSecondary),
                        onPressed: () {
                          Clipboard.setData(ClipboardData(text: primaryNum));
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Copied to clipboard')),
                          );
                        },
                      ),
                  ],
                ),
                const SizedBox(height: 16),
                Row(
                  children: [
                    _buildCardFooterCol(doc),
                    const Spacer(),
                    Icon(Icons.lock, size: 12, color: accent.withAlpha(128)),
                  ],
                ),
              ],
            ),
          ),

          const SizedBox(height: 20),

          // Attachment section
          if (doc.imagePath != null) ...[
            _buildSectionHeader('Scanned Attachments'),
            Container(
              decoration: BoxDecoration(
                color: cinemaElevated,
                borderRadius: BorderRadius.circular(16),
                border: Border.all(color: cinemaStroke),
              ),
              child: Column(
                children: [
                  if (!_imageRevealed)
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: Row(
                        children: [
                          const Icon(Icons.visibility_off, color: textMuted, size: 18),
                          const SizedBox(width: 8),
                          Text('Scan hidden', style: GoogleFonts.inter(color: textMuted, fontSize: 14)),
                          const Spacer(),
                          TextButton(
                            onPressed: () => setState(() => _imageRevealed = true),
                            child: const Text('Show', style: TextStyle(color: accentIndigo)),
                          ),
                        ],
                      ),
                    )
                  else ...[
                    Padding(
                      padding: const EdgeInsets.all(16),
                      child: _loadingImage
                          ? const Center(
                              child: CircularProgressIndicator(color: accentIndigo),
                            )
                          : _imageBytes != null
                              ? ClipRRect(
                                  borderRadius: BorderRadius.circular(12),
                                  child: Image.memory(_imageBytes!, fit: BoxFit.contain),
                                )
                              : Text('Could not load scan', style: GoogleFonts.inter(color: textMuted)),
                    ),
                    Padding(
                      padding: const EdgeInsets.only(bottom: 12, right: 12, left: 12),
                      child: Row(
                        mainAxisAlignment: MainAxisAlignment.end,
                        children: [
                          TextButton.icon(
                            onPressed: () => _openAttachment(doc),
                            icon: Icon(
                              doc.imagePath!.endsWith('.pdf')
                                  ? Icons.picture_as_pdf
                                  : Icons.open_in_new,
                              size: 16,
                              color: accentIndigo,
                            ),
                            label: Text(
                              doc.imagePath!.endsWith('.pdf')
                                  ? 'Open PDF'
                                  : 'View Document',
                              style: const TextStyle(color: accentIndigo),
                            ),
                          ),
                          const SizedBox(width: 8),
                          TextButton.icon(
                            onPressed: () => setState(() => _imageRevealed = false),
                            icon: const Icon(Icons.visibility_off, size: 16),
                            label: const Text('Hide Scan'),
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 20),
          ],

          // Document details
          _buildSectionHeader('Document Details'),
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: cinemaElevated,
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: cinemaStroke),
            ),
            child: _buildDetailRows(doc),
          ),

          const SizedBox(height: 20),

          // Share button
          SizedBox(
            width: double.infinity,
            child: ElevatedButton.icon(
              onPressed: () async {
                final text = _buildShareText(doc);
                await Clipboard.setData(ClipboardData(text: text));
                await Share.share(text);
              },
              icon: const Icon(Icons.share),
              label: const Text('Share Document Details'),
            ),
          ),
          const SizedBox(height: 40),
        ],
      ),
    );
  }

  Widget _buildCardFooterCol(VaultDocument doc) {
    String name = '';
    String validity = '';
    switch (doc.type) {
      case DocumentType.paymentCard:
        name = doc.cardholderName ?? '';
        validity = doc.cardExpiry ?? '';
      case DocumentType.aadhaarCard:
        name = doc.aadhaarName ?? '';
      case DocumentType.panCard:
        name = doc.panName ?? '';
      case DocumentType.driversLicense:
        name = doc.dlHolderName ?? '';
        validity = doc.dlExpiry ?? '';
      case DocumentType.vehicleRc:
        name = doc.rcOwnerName ?? '';
        validity = doc.rcExpiry ?? '';
      case DocumentType.genericId:
        name = doc.genericIdName ?? '';
        validity = doc.genericIdExpiry ?? '';
    }

    return Row(
      children: [
        if (name.isNotEmpty)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('NAME', style: GoogleFonts.inter(fontSize: 8, color: textMuted, letterSpacing: 1.2, fontWeight: FontWeight.w600)),
              Text(name.toUpperCase(), style: GoogleFonts.inter(fontSize: 11, color: textSecondary, fontWeight: FontWeight.w500)),
            ],
          ),
        if (name.isNotEmpty && validity.isNotEmpty) const SizedBox(width: 16),
        if (validity.isNotEmpty)
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text('VALID THRU', style: GoogleFonts.inter(fontSize: 8, color: textMuted, letterSpacing: 1.2, fontWeight: FontWeight.w600)),
              Text(validity, style: GoogleFonts.inter(fontSize: 11, color: textSecondary, fontWeight: FontWeight.w500)),
            ],
          ),
      ],
    );
  }

  Widget _buildSectionHeader(String title) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 10),
      child: Text(
        title,
        style: GoogleFonts.inter(
          color: textSecondary,
          fontWeight: FontWeight.w600,
          fontSize: 13,
          letterSpacing: 0.5,
        ),
      ),
    );
  }

  Widget _buildDetailRows(VaultDocument doc) {
    switch (doc.type) {
      case DocumentType.paymentCard:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailRow(label: 'Cardholder', value: doc.cardholderName ?? ''),
            DetailRow(label: 'Card Number', value: doc.cardNumber ?? '', isSensitive: true),
            DetailRow(label: 'Expiry', value: doc.cardExpiry ?? ''),
            DetailRow(label: 'CVV', value: doc.cardCvv ?? '', isSensitive: true),
            DetailRow(label: 'Network', value: doc.cardType ?? ''),
          ],
        );
      case DocumentType.aadhaarCard:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailRow(label: 'Name', value: doc.aadhaarName ?? ''),
            DetailRow(label: 'Aadhaar Number', value: doc.aadhaarNumber ?? '', isSensitive: true),
            DetailRow(label: 'Date of Birth', value: doc.aadhaarDob ?? ''),
            DetailRow(label: 'Gender', value: doc.aadhaarGender ?? ''),
          ],
        );
      case DocumentType.panCard:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailRow(label: 'Name', value: doc.panName ?? ''),
            DetailRow(label: 'PAN Number', value: doc.panNumber ?? '', isSensitive: true),
            DetailRow(label: "Father's Name", value: doc.panFatherName ?? ''),
            DetailRow(label: 'Date of Birth', value: doc.panDob ?? ''),
          ],
        );
      case DocumentType.driversLicense:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailRow(label: 'Name', value: doc.dlHolderName ?? ''),
            DetailRow(label: 'DL Number', value: doc.dlNumber ?? ''),
            DetailRow(label: 'Date of Birth', value: doc.dlDob ?? ''),
            DetailRow(label: 'Expiry', value: doc.dlExpiry ?? ''),
            DetailRow(label: 'State / RTO', value: doc.dlState ?? ''),
          ],
        );
      case DocumentType.vehicleRc:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailRow(label: 'Owner', value: doc.rcOwnerName ?? ''),
            DetailRow(label: 'RC Number', value: doc.rcNumber ?? ''),
            DetailRow(label: 'Chassis Number', value: doc.rcChassisNumber ?? '', isSensitive: true),
            DetailRow(label: 'Engine Number', value: doc.rcEngineNumber ?? '', isSensitive: true),
            DetailRow(label: 'Expiry', value: doc.rcExpiry ?? ''),
          ],
        );
      case DocumentType.genericId:
        return Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            DetailRow(label: 'ID Card Type', value: doc.genericIdType ?? ''),
            DetailRow(label: 'Name on ID', value: doc.genericIdName ?? ''),
            DetailRow(label: 'ID Number', value: doc.genericIdNumber ?? '', isSensitive: true),
            DetailRow(label: 'Expiry Date', value: doc.genericIdExpiry ?? ''),
          ],
        );
    }
  }
}
