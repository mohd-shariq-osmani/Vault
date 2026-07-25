import 'dart:io';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:open_filex/open_filex.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdfx/pdfx.dart' as px;
import 'package:share_plus/share_plus.dart';
import '../../models/document.dart';
import '../../providers/vault_provider.dart';
import '../theme/colors.dart';
import '../widgets/apple_touchable.dart';
import '../widgets/detail_row.dart';
import '../widgets/glassmorphic_card.dart';
import 'add_document_screen.dart';

class ViewDocumentScreen extends ConsumerStatefulWidget {
  final String documentId;

  const ViewDocumentScreen({
    super.key,
    required this.documentId,
  });

  @override
  ConsumerState<ViewDocumentScreen> createState() => _ViewDocumentScreenState();
}

class _ViewDocumentScreenState extends ConsumerState<ViewDocumentScreen> {
  bool _revealSensitive = false;
  Uint8List? _decryptedImageBytes;
  bool _isLoadingImage = false;
  px.PdfController? _pdfController;
  int _pdfPageCount = 0;
  int _pdfCurrentPage = 1;
  bool _isPdf = false;
  String _fileExtension = 'jpg';

  @override
  void initState() {
    super.initState();
    _loadAttachment();
  }

  @override
  void dispose() {
    _pdfController?.dispose();
    super.dispose();
  }

  Future<void> _loadAttachment() async {
    final docs = ref.read(vaultProvider).value ?? [];
    VaultDocument? doc;
    try {
      doc = docs.firstWhere((d) => d.id == widget.documentId);
    } catch (_) {
      return;
    }

    if (doc.imagePath == null) return;

    setState(() => _isLoadingImage = true);

    try {
      final bytes = await ref
          .read(vaultProvider.notifier)
          .loadImage(doc.imagePath!);

      if (bytes != null && bytes.isNotEmpty) {
        final path = doc.imagePath!.toLowerCase();
        _isPdf = path.endsWith('.pdf');
        _fileExtension = path.contains('.') ? path.split('.').last : (_isPdf ? 'pdf' : 'jpg');

        if (_isPdf) {
          final document = await px.PdfDocument.openData(bytes);
          _pdfPageCount = document.pagesCount;
          await document.close();

          _pdfController = px.PdfController(
            document: px.PdfDocument.openData(bytes),
          );
        }

        setState(() {
          _decryptedImageBytes = bytes;
        });
      }
    } catch (e) {
      // Failed to load attachment silently
    } finally {
      if (mounted) setState(() => _isLoadingImage = false);
    }
  }

  Future<void> _openAttachmentFile(VaultDocument doc) async {
    if (_decryptedImageBytes == null) return;
    try {
      HapticFeedback.lightImpact();
      final tempDir = await getTemporaryDirectory();
      final sanitizedTitle = doc.title.replaceAll(RegExp(r'[^\w\s\.-]'), '_');
      final fileName = '$sanitizedTitle.$_fileExtension';
      final file = File('${tempDir.path}/$fileName');
      await file.writeAsBytes(_decryptedImageBytes!);
      await OpenFilex.open(file.path);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Could not open file: $e')),
        );
      }
    }
  }

  String _getTypeBadge(DocumentType type) {
    switch (type) {
      case DocumentType.paymentCard:
        return 'PAYMENT CARD';
      case DocumentType.aadhaarCard:
        return 'AADHAAR CARD';
      case DocumentType.panCard:
        return 'PAN CARD';
      case DocumentType.driversLicense:
        return "DRIVER'S LICENCE";
      case DocumentType.vehicleRc:
        return 'VEHICLE RC';
      case DocumentType.genericId:
        return 'ID CARD';
    }
  }

  IconData _getTypeIcon(DocumentType type) {
    switch (type) {
      case DocumentType.paymentCard:
        return Icons.credit_card_rounded;
      case DocumentType.aadhaarCard:
        return Icons.badge_rounded;
      case DocumentType.panCard:
        return Icons.article_rounded;
      case DocumentType.driversLicense:
        return Icons.drive_eta_rounded;
      case DocumentType.vehicleRc:
        return Icons.directions_car_rounded;
      case DocumentType.genericId:
        return Icons.card_membership_rounded;
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

  void _confirmDelete(BuildContext context, VaultDocument doc) {
    HapticFeedback.heavyImpact();
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
              await ref.read(vaultProvider.notifier).deleteDocument(doc.id);
              if (context.mounted) Navigator.pop(context);
            },
            child: const Text('Delete', style: TextStyle(color: appleRed, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }

  String _buildShareText(VaultDocument doc) {
    final buffer = StringBuffer();
    buffer.writeln('--- ${doc.title.toUpperCase()} ---');
    buffer.writeln('Type: ${_getTypeBadge(doc.type)}');

    switch (doc.type) {
      case DocumentType.paymentCard:
        if (doc.cardholderName?.isNotEmpty ?? false) buffer.writeln('Name: ${doc.cardholderName}');
        if (doc.cardNumber?.isNotEmpty ?? false) buffer.writeln('Card Number: ${doc.cardNumber}');
        if (doc.cardExpiry?.isNotEmpty ?? false) buffer.writeln('Expiry: ${doc.cardExpiry}');
        if (doc.cardCvv?.isNotEmpty ?? false) buffer.writeln('CVV: ${doc.cardCvv}');
        if (doc.cardType?.isNotEmpty ?? false) buffer.writeln('Network: ${doc.cardType}');
      case DocumentType.aadhaarCard:
        if (doc.aadhaarName?.isNotEmpty ?? false) buffer.writeln('Name: ${doc.aadhaarName}');
        if (doc.aadhaarNumber?.isNotEmpty ?? false) buffer.writeln('Aadhaar Number: ${doc.aadhaarNumber}');
        if (doc.aadhaarDob?.isNotEmpty ?? false) buffer.writeln('DOB: ${doc.aadhaarDob}');
        if (doc.aadhaarGender?.isNotEmpty ?? false) buffer.writeln('Gender: ${doc.aadhaarGender}');
      case DocumentType.panCard:
        if (doc.panName?.isNotEmpty ?? false) buffer.writeln('Name: ${doc.panName}');
        if (doc.panNumber?.isNotEmpty ?? false) buffer.writeln('PAN Number: ${doc.panNumber}');
        if (doc.panFatherName?.isNotEmpty ?? false) buffer.writeln("Father's Name: ${doc.panFatherName}");
        if (doc.panDob?.isNotEmpty ?? false) buffer.writeln('DOB: ${doc.panDob}');
      case DocumentType.driversLicense:
        if (doc.dlHolderName?.isNotEmpty ?? false) buffer.writeln('Holder Name: ${doc.dlHolderName}');
        if (doc.dlNumber?.isNotEmpty ?? false) buffer.writeln('Licence Number: ${doc.dlNumber}');
        if (doc.dlDob?.isNotEmpty ?? false) buffer.writeln('DOB: ${doc.dlDob}');
        if (doc.dlExpiry?.isNotEmpty ?? false) buffer.writeln('Expiry: ${doc.dlExpiry}');
        if (doc.dlState?.isNotEmpty ?? false) buffer.writeln('State: ${doc.dlState}');
      case DocumentType.vehicleRc:
        if (doc.rcOwnerName?.isNotEmpty ?? false) buffer.writeln('Owner Name: ${doc.rcOwnerName}');
        if (doc.rcNumber?.isNotEmpty ?? false) buffer.writeln('Registration Number: ${doc.rcNumber}');
        if (doc.rcChassisNumber?.isNotEmpty ?? false) buffer.writeln('Chassis Number: ${doc.rcChassisNumber}');
        if (doc.rcEngineNumber?.isNotEmpty ?? false) buffer.writeln('Engine Number: ${doc.rcEngineNumber}');
        if (doc.rcExpiry?.isNotEmpty ?? false) buffer.writeln('Expiry: ${doc.rcExpiry}');
      case DocumentType.genericId:
        if (doc.genericIdType?.isNotEmpty ?? false) buffer.writeln('ID Type: ${doc.genericIdType}');
        if (doc.genericIdNumber?.isNotEmpty ?? false) buffer.writeln('ID Number: ${doc.genericIdNumber}');
        if (doc.genericIdName?.isNotEmpty ?? false) buffer.writeln('Name: ${doc.genericIdName}');
        if (doc.genericIdExpiry?.isNotEmpty ?? false) buffer.writeln('Expiry: ${doc.genericIdExpiry}');
    }

    return buffer.toString().trim();
  }

  @override
  Widget build(BuildContext context) {
    final docs = ref.watch(vaultProvider).value ?? [];
    VaultDocument? doc;
    try {
      doc = docs.firstWhere((d) => d.id == widget.documentId);
    } catch (_) {
      return Scaffold(
        backgroundColor: applePitchBlack,
        appBar: AppBar(),
        body: const Center(child: Text('Document not found', style: TextStyle(color: textSecondary))),
      );
    }

    final grad = gradientForDoc(doc.cardColorIndex);
    final accent = accentForDoc(doc.cardColorIndex);
    final primaryNum = _getPrimaryNumber(doc);
    final maskedNum = _getMaskedNumber(doc);

    return Scaffold(
      backgroundColor: applePitchBlack,
      appBar: AppBar(
        backgroundColor: applePitchBlack,
        title: Text(doc.title),
        actions: [
          IconButton(
            icon: Icon(
              _revealSensitive ? Icons.visibility_off_rounded : Icons.visibility_rounded,
              color: appleBlue,
            ),
            onPressed: () {
              HapticFeedback.lightImpact();
              setState(() => _revealSensitive = !_revealSensitive);
            },
            tooltip: _revealSensitive ? 'Hide Sensitive Data' : 'Reveal Sensitive Data',
          ),
          IconButton(
            icon: const Icon(Icons.edit_rounded, color: appleBlue),
            onPressed: () {
              Navigator.push(
                context,
                MaterialPageRoute(
                  builder: (_) => AddDocumentScreen(
                    documentType: doc!.type,
                    existingDocument: doc,
                    onSaved: () => setState(() {}),
                  ),
                ),
              );
            },
            tooltip: 'Edit',
          ),
          IconButton(
            icon: const Icon(Icons.delete_outline_rounded, color: appleRed),
            onPressed: () => _confirmDelete(context, doc!),
            tooltip: 'Delete',
          ),
        ],
      ),
      body: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        padding: const EdgeInsets.all(20),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Apple Glassmorphic Card Hero Preview
            GlassmorphicCard(
              gradient: grad,
              glowColor: accent,
              borderRadius: 24,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: Colors.white.withAlpha(30),
                          borderRadius: BorderRadius.circular(20),
                          border: Border.all(color: Colors.white.withAlpha(50)),
                        ),
                        child: Text(
                          _getTypeBadge(doc.type),
                          style: GoogleFonts.inter(
                            fontSize: 10,
                            color: Colors.white,
                            fontWeight: FontWeight.w700,
                            letterSpacing: 1.2,
                          ),
                        ),
                      ),
                      const Spacer(),
                      Icon(_getTypeIcon(doc.type), color: Colors.white.withAlpha(200), size: 22),
                    ],
                  ),
                  const SizedBox(height: 20),
                  Text(
                    doc.title,
                    style: GoogleFonts.inter(
                      fontSize: 22,
                      fontWeight: FontWeight.w800,
                      color: Colors.white,
                      letterSpacing: -0.3,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Row(
                    children: [
                      Expanded(
                        child: Text(
                          _revealSensitive ? primaryNum : maskedNum,
                          style: GoogleFonts.inter(
                            fontSize: 16,
                            color: Colors.white.withAlpha(230),
                            letterSpacing: 1.5,
                            fontWeight: FontWeight.w600,
                          ),
                        ),
                      ),
                      AppleTouchable(
                        onTap: () {
                          Clipboard.setData(ClipboardData(text: primaryNum));
                          HapticFeedback.mediumImpact();
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(content: Text('Document number copied to clipboard')),
                          );
                        },
                        child: Container(
                          padding: const EdgeInsets.all(6),
                          decoration: BoxDecoration(
                            color: Colors.white.withAlpha(30),
                            shape: BoxShape.circle,
                          ),
                          child: const Icon(Icons.copy_rounded, size: 16, color: Colors.white),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 20),
                  _buildCardFooterCol(doc),
                ],
              ),
            ),

            const SizedBox(height: 28),

            // Attachment Section
            if (doc.imagePath != null) ...[
              Text(
                'ATTACHMENT',
                style: GoogleFonts.inter(
                  fontSize: 11,
                  color: textSecondary,
                  fontWeight: FontWeight.w600,
                  letterSpacing: 1.1,
                ),
              ),
              const SizedBox(height: 10),
              Container(
                width: double.infinity,
                decoration: BoxDecoration(
                  color: appleCardBackground,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: appleBorderStroke),
                ),
                child: Column(
                  children: [
                    if (!_revealSensitive) ...[
                      Padding(
                        padding: const EdgeInsets.all(24),
                        child: Column(
                          children: [
                            const Icon(Icons.visibility_off_rounded, size: 36, color: textMuted),
                            const SizedBox(height: 10),
                            Text(
                              'Attachment Hidden',
                              style: GoogleFonts.inter(fontSize: 15, fontWeight: FontWeight.w600, color: textPrimary),
                            ),
                            const SizedBox(height: 4),
                            Text(
                              'Tap reveal icon in upper right to view preview',
                              style: GoogleFonts.inter(fontSize: 12, color: textSecondary),
                            ),
                          ],
                        ),
                      ),
                    ] else if (_isLoadingImage) ...[
                      const Padding(
                        padding: EdgeInsets.all(40),
                        child: CircularProgressIndicator(color: appleBlue),
                      ),
                    ] else if (_decryptedImageBytes != null) ...[
                      if (_isPdf && _pdfController != null) ...[
                        ClipRRect(
                          borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
                          child: SizedBox(
                            height: 320,
                            child: px.PdfView(
                              controller: _pdfController!,
                              onPageChanged: (page) {
                                setState(() => _pdfCurrentPage = page);
                              },
                            ),
                          ),
                        ),
                        if (_pdfPageCount > 1)
                          Padding(
                            padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 8),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                Text(
                                  'Page $_pdfCurrentPage of $_pdfPageCount',
                                  style: GoogleFonts.inter(color: textSecondary, fontSize: 13, fontWeight: FontWeight.w500),
                                ),
                              ],
                            ),
                          ),
                      ] else ...[
                        ClipRRect(
                          borderRadius: const BorderRadius.vertical(top: Radius.circular(20)),
                          child: Image.memory(
                            _decryptedImageBytes!,
                            width: double.infinity,
                            fit: BoxFit.cover,
                          ),
                        ),
                      ],
                      Padding(
                        padding: const EdgeInsets.all(12),
                        child: AppleTouchable(
                          onTap: () => _openAttachmentFile(doc!),
                          child: Container(
                            width: double.infinity,
                            padding: const EdgeInsets.symmetric(vertical: 12),
                            decoration: BoxDecoration(
                              color: appleCardSecondary,
                              borderRadius: BorderRadius.circular(14),
                              border: Border.all(color: appleBorderStroke),
                            ),
                            child: Row(
                              mainAxisAlignment: MainAxisAlignment.center,
                              children: [
                                const Icon(Icons.open_in_new_rounded, size: 18, color: appleBlue),
                                const SizedBox(width: 8),
                                Text(
                                  'Open Attachment (${doc.title})',
                                  style: GoogleFonts.inter(
                                    color: appleBlue,
                                    fontSize: 14,
                                    fontWeight: FontWeight.w700,
                                  ),
                                ),
                              ],
                            ),
                          ),
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 24),
            ],

            // Details Grouped Section
            Text(
              'DOCUMENT DETAILS',
              style: GoogleFonts.inter(
                fontSize: 11,
                color: textSecondary,
                fontWeight: FontWeight.w600,
                letterSpacing: 1.1,
              ),
            ),
            const SizedBox(height: 10),

            _buildDocDetailsRows(doc),

            const SizedBox(height: 24),

            // Share Action Button
            SizedBox(
              width: double.infinity,
              child: AppleTouchable(
                onTap: () async {
                  HapticFeedback.mediumImpact();
                  final text = _buildShareText(doc!);
                  await Clipboard.setData(ClipboardData(text: text));
                  await Share.share(text);
                },
                child: Container(
                  padding: const EdgeInsets.symmetric(vertical: 16),
                  decoration: BoxDecoration(
                    color: appleCardBackground,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: appleBorderStroke),
                  ),
                  child: Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      const Icon(Icons.ios_share_rounded, size: 20, color: appleBlue),
                      const SizedBox(width: 10),
                      Text(
                        'Share Document Details',
                        style: GoogleFonts.inter(
                          color: appleBlue,
                          fontWeight: FontWeight.w700,
                          fontSize: 15,
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),

            const SizedBox(height: 40),
          ],
        ),
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
              Text(
                'NAME',
                style: GoogleFonts.inter(
                  fontSize: 9,
                  color: Colors.white.withAlpha(140),
                  letterSpacing: 1.2,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                name.toUpperCase(),
                style: GoogleFonts.inter(
                  fontSize: 12,
                  color: Colors.white.withAlpha(230),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
        const Spacer(),
        if (validity.isNotEmpty)
          Column(
            crossAxisAlignment: CrossAxisAlignment.end,
            children: [
              Text(
                'EXPIRES',
                style: GoogleFonts.inter(
                  fontSize: 9,
                  color: Colors.white.withAlpha(140),
                  letterSpacing: 1.2,
                  fontWeight: FontWeight.w600,
                ),
              ),
              const SizedBox(height: 2),
              Text(
                validity,
                style: GoogleFonts.inter(
                  fontSize: 12,
                  color: Colors.white.withAlpha(230),
                  fontWeight: FontWeight.w600,
                ),
              ),
            ],
          ),
      ],
    );
  }

  Widget _buildDocDetailsRows(VaultDocument doc) {
    final rows = <Widget>[];

    switch (doc.type) {
      case DocumentType.paymentCard:
        if (doc.cardholderName?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Cardholder Name', value: doc.cardholderName!));
        }
        if (doc.cardNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Card Number', value: doc.cardNumber!, isSensitive: true));
        }
        if (doc.cardExpiry?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Expiry Date', value: doc.cardExpiry!));
        }
        if (doc.cardCvv?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'CVV Security Code', value: doc.cardCvv!, isSensitive: true));
        }
        if (doc.cardType?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Network', value: doc.cardType!));
        }

      case DocumentType.aadhaarCard:
        if (doc.aadhaarName?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Full Name', value: doc.aadhaarName!));
        }
        if (doc.aadhaarNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Aadhaar Number', value: doc.aadhaarNumber!, isSensitive: true));
        }
        if (doc.aadhaarDob?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Date of Birth / YOB', value: doc.aadhaarDob!));
        }
        if (doc.aadhaarGender?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Gender', value: doc.aadhaarGender!));
        }

      case DocumentType.panCard:
        if (doc.panName?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Full Name', value: doc.panName!));
        }
        if (doc.panNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'PAN Number', value: doc.panNumber!, isSensitive: true));
        }
        if (doc.panFatherName?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: "Father's Name", value: doc.panFatherName!));
        }
        if (doc.panDob?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Date of Birth', value: doc.panDob!));
        }

      case DocumentType.driversLicense:
        if (doc.dlHolderName?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Holder Name', value: doc.dlHolderName!));
        }
        if (doc.dlNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Licence Number', value: doc.dlNumber!, isSensitive: true));
        }
        if (doc.dlDob?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Date of Birth', value: doc.dlDob!));
        }
        if (doc.dlExpiry?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Expiry Date', value: doc.dlExpiry!));
        }
        if (doc.dlState?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'State', value: doc.dlState!));
        }

      case DocumentType.vehicleRc:
        if (doc.rcOwnerName?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Owner Name', value: doc.rcOwnerName!));
        }
        if (doc.rcNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Registration Number', value: doc.rcNumber!, isSensitive: true));
        }
        if (doc.rcChassisNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Chassis Number', value: doc.rcChassisNumber!, isSensitive: true));
        }
        if (doc.rcEngineNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Engine Number', value: doc.rcEngineNumber!, isSensitive: true));
        }
        if (doc.rcExpiry?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Expiry Date', value: doc.rcExpiry!));
        }

      case DocumentType.genericId:
        if (doc.genericIdType?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Card Type', value: doc.genericIdType!));
        }
        if (doc.genericIdName?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Name on Card', value: doc.genericIdName!));
        }
        if (doc.genericIdNumber?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'ID Number', value: doc.genericIdNumber!, isSensitive: true));
        }
        if (doc.genericIdExpiry?.isNotEmpty ?? false) {
          rows.add(DetailRow(label: 'Expiry Date', value: doc.genericIdExpiry!));
        }
    }

    return Column(children: rows);
  }
}
