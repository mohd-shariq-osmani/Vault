import 'dart:io';
import 'dart:math';
import 'package:file_picker/file_picker.dart';
import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_fonts/google_fonts.dart';
import 'package:google_mlkit_document_scanner/google_mlkit_document_scanner.dart';
import 'package:google_mlkit_text_recognition/google_mlkit_text_recognition.dart';
import 'package:path_provider/path_provider.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:pdfx/pdfx.dart' as px;
import 'package:uuid/uuid.dart';
import '../../models/document.dart';
import '../../providers/vault_provider.dart';
import '../../providers/auth_provider.dart';
import '../../utils/number_formatters.dart';
import '../../utils/ocr_autofill.dart';
import '../theme/colors.dart';
import '../widgets/apple_touchable.dart';

class AddDocumentScreen extends ConsumerStatefulWidget {
  final DocumentType documentType;
  final VaultDocument? existingDocument;
  final VoidCallback? onSaved;

  const AddDocumentScreen({
    super.key,
    required this.documentType,
    this.existingDocument,
    this.onSaved,
  });

  @override
  ConsumerState<AddDocumentScreen> createState() => _AddDocumentScreenState();
}

class _AddDocumentScreenState extends ConsumerState<AddDocumentScreen> {
  final _formKey = GlobalKey<FormState>();
  final _uuid = const Uuid();

  // Common
  late TextEditingController _titleCtrl;

  // Payment card
  late TextEditingController _cardholderCtrl;
  late TextEditingController _cardNumberCtrl;
  late TextEditingController _cardExpiryCtrl;
  late TextEditingController _cardCvvCtrl;
  String _cardType = 'Visa';

  // Aadhaar
  late TextEditingController _aadhaarNameCtrl;
  late TextEditingController _aadhaarNumberCtrl;
  late TextEditingController _aadhaarDobCtrl;
  String _aadhaarGender = 'Male';

  // PAN
  late TextEditingController _panNameCtrl;
  late TextEditingController _panNumberCtrl;
  late TextEditingController _panFatherNameCtrl;
  late TextEditingController _panDobCtrl;

  // DL
  late TextEditingController _dlHolderNameCtrl;
  late TextEditingController _dlNumberCtrl;
  late TextEditingController _dlDobCtrl;
  late TextEditingController _dlExpiryCtrl;
  late TextEditingController _dlStateCtrl;

  // RC
  late TextEditingController _rcOwnerNameCtrl;
  late TextEditingController _rcNumberCtrl;
  late TextEditingController _rcChassisCtrl;
  late TextEditingController _rcEngineCtrl;
  late TextEditingController _rcExpiryCtrl;

  // Generic ID
  late TextEditingController _genericIdNumberCtrl;
  late TextEditingController _genericIdNameCtrl;
  late TextEditingController _genericIdExpiryCtrl;
  late TextEditingController _genericIdTypeCtrl;

  // Attachments
  final List<Uint8List> _pageBytes = [];
  final List<String> _pageExtensions = [];
  bool _isProcessingOcr = false;

  @override
  void initState() {
    super.initState();
    final doc = widget.existingDocument;
    _titleCtrl = TextEditingController(text: doc?.title ?? '');
    _cardholderCtrl = TextEditingController(text: doc?.cardholderName ?? '');
    _cardNumberCtrl = TextEditingController(
        text: doc != null ? formatCardNumberInput(doc.cardNumber ?? '') : '');
    _cardExpiryCtrl = TextEditingController(
        text: doc != null ? formatExpiryInput(doc.cardExpiry ?? '') : '');
    _cardCvvCtrl = TextEditingController(text: doc?.cardCvv ?? '');
    _cardType = doc?.cardType ?? 'Visa';

    _aadhaarNameCtrl = TextEditingController(text: doc?.aadhaarName ?? '');
    _aadhaarNumberCtrl = TextEditingController(
        text: doc != null ? formatAadhaarInput(doc.aadhaarNumber ?? '') : '');
    _aadhaarDobCtrl = TextEditingController(text: doc?.aadhaarDob ?? '');
    _aadhaarGender = doc?.aadhaarGender ?? 'Male';

    _panNameCtrl = TextEditingController(text: doc?.panName ?? '');
    _panNumberCtrl = TextEditingController(text: doc?.panNumber ?? '');
    _panFatherNameCtrl = TextEditingController(text: doc?.panFatherName ?? '');
    _panDobCtrl = TextEditingController(text: doc?.panDob ?? '');

    _dlHolderNameCtrl = TextEditingController(text: doc?.dlHolderName ?? '');
    _dlNumberCtrl = TextEditingController(text: doc?.dlNumber ?? '');
    _dlDobCtrl = TextEditingController(text: doc?.dlDob ?? '');
    _dlExpiryCtrl = TextEditingController(text: doc?.dlExpiry ?? '');
    _dlStateCtrl = TextEditingController(text: doc?.dlState ?? '');

    _rcOwnerNameCtrl = TextEditingController(text: doc?.rcOwnerName ?? '');
    _rcNumberCtrl = TextEditingController(text: doc?.rcNumber ?? '');
    _rcChassisCtrl = TextEditingController(text: doc?.rcChassisNumber ?? '');
    _rcEngineCtrl = TextEditingController(text: doc?.rcEngineNumber ?? '');
    _rcExpiryCtrl = TextEditingController(text: doc?.rcExpiry ?? '');

    _genericIdNumberCtrl = TextEditingController(text: doc?.genericIdNumber ?? '');
    _genericIdNameCtrl = TextEditingController(text: doc?.genericIdName ?? '');
    _genericIdExpiryCtrl = TextEditingController(text: doc?.genericIdExpiry ?? '');
    _genericIdTypeCtrl = TextEditingController(text: doc?.genericIdType ?? 'ID Card');
  }

  @override
  void dispose() {
    _titleCtrl.dispose();
    _cardholderCtrl.dispose();
    _cardNumberCtrl.dispose();
    _cardExpiryCtrl.dispose();
    _cardCvvCtrl.dispose();
    _aadhaarNameCtrl.dispose();
    _aadhaarNumberCtrl.dispose();
    _aadhaarDobCtrl.dispose();
    _panNameCtrl.dispose();
    _panNumberCtrl.dispose();
    _panFatherNameCtrl.dispose();
    _panDobCtrl.dispose();
    _dlHolderNameCtrl.dispose();
    _dlNumberCtrl.dispose();
    _dlDobCtrl.dispose();
    _dlExpiryCtrl.dispose();
    _dlStateCtrl.dispose();
    _rcOwnerNameCtrl.dispose();
    _rcNumberCtrl.dispose();
    _rcChassisCtrl.dispose();
    _rcEngineCtrl.dispose();
    _rcExpiryCtrl.dispose();
    _genericIdNumberCtrl.dispose();
    _genericIdNameCtrl.dispose();
    _genericIdExpiryCtrl.dispose();
    _genericIdTypeCtrl.dispose();
    super.dispose();
  }

  String _getTypeTitle() {
    if (widget.existingDocument != null) {
      return 'Edit ${widget.existingDocument!.title}';
    }
    switch (widget.documentType) {
      case DocumentType.paymentCard:
        return 'New Payment Card';
      case DocumentType.aadhaarCard:
        return 'New Aadhaar Card';
      case DocumentType.panCard:
        return 'New PAN Card';
      case DocumentType.driversLicense:
        return "New Driver's Licence";
      case DocumentType.vehicleRc:
        return 'New Vehicle RC';
      case DocumentType.genericId:
        return 'New ID Card';
    }
  }

  bool _isMobilePlatform() {
    if (kIsWeb) return false;
    return Platform.isAndroid || Platform.isIOS;
  }

  Future<void> _selectDate(BuildContext context, TextEditingController controller) async {
    HapticFeedback.selectionClick();
    DateTime initialDate = DateTime.now();
    if (controller.text.isNotEmpty) {
      final parts = controller.text.split('/');
      if (parts.length == 3) {
        final day = int.tryParse(parts[0]);
        final month = int.tryParse(parts[1]);
        final year = int.tryParse(parts[2]);
        if (day != null && month != null && year != null) {
          try {
            initialDate = DateTime(year, month, day);
          } catch (_) {}
        }
      }
    }

    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: initialDate,
      firstDate: DateTime(1900),
      lastDate: DateTime(2100),
      builder: (context, child) {
        return Theme(
          data: Theme.of(context).copyWith(
            colorScheme: const ColorScheme.dark(
              primary: appleBlue,
              onPrimary: Colors.white,
              surface: appleCardBackground,
              onSurface: textPrimary,
            ),
          ),
          child: child!,
        );
      },
    );

    if (picked != null) {
      final day = picked.day.toString().padLeft(2, '0');
      final month = picked.month.toString().padLeft(2, '0');
      final year = picked.year.toString();
      setState(() {
        controller.text = '$day/$month/$year';
      });
    }
  }

  Future<void> _scanDocument() async {
    if (!_isMobilePlatform()) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Document scanner not available on desktop. Please upload files instead.'),
          ),
        );
      }
      return;
    }

    try {
      ref.read(isLaunchingExternalProvider.notifier).state = true;
      final options = DocumentScannerOptions(
        documentFormats: {DocumentFormat.jpeg},
        mode: ScannerMode.full,
        isGalleryImport: false,
        pageLimit: 15,
      );
      final documentScanner = DocumentScanner(options: options);
      final result = await documentScanner.scanDocument();
      documentScanner.close();
      ref.read(isLaunchingExternalProvider.notifier).state = false;

      if (result.images == null || result.images!.isEmpty) return;

      for (final imagePath in result.images!) {
        final file = File(imagePath);
        final bytes = await file.readAsBytes();
        final ext = imagePath.split('.').last.toLowerCase();
        setState(() {
          _pageBytes.add(bytes);
          _pageExtensions.add(ext);
        });
      }

      if (result.images != null && result.images!.isNotEmpty) {
        await _runOcr(File(result.images!.first));
      }
    } catch (e) {
      ref.read(isLaunchingExternalProvider.notifier).state = false;
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Scanner error: $e')),
        );
      }
    }
  }

  Future<void> _pickFiles() async {
    try {
      final result = await FilePicker.platform.pickFiles(
        allowMultiple: true,
        type: FileType.custom,
        allowedExtensions: ['jpg', 'jpeg', 'png', 'pdf'],
      );

      if (result == null || result.files.isEmpty) return;

      for (final file in result.files) {
        if (file.path == null) continue;
        final ext = file.extension?.toLowerCase() ?? 'jpg';
        final f = File(file.path!);

        if (ext == 'pdf') {
          final document = await px.PdfDocument.openFile(file.path!);
          final pageCount = document.pagesCount;
          for (int i = 1; i <= pageCount; i++) {
            final page = await document.getPage(i);
            final pageImage = await page.render(
              width: page.width * 2,
              height: page.height * 2,
              format: px.PdfPageImageFormat.jpeg,
            );
            await page.close();
            if (pageImage != null) {
              setState(() {
                _pageBytes.add(pageImage.bytes);
                _pageExtensions.add('jpeg');
              });
            }
          }
          await document.close();
        } else {
          final bytes = await f.readAsBytes();
          setState(() {
            _pageBytes.add(bytes);
            _pageExtensions.add(ext);
          });
        }
      }

      if (_pageBytes.isNotEmpty) {
        final tempDir = await getTemporaryDirectory();
        final tempFile = File('${tempDir.path}/temp_ocr_image.jpg');
        await tempFile.writeAsBytes(_pageBytes.first);
        await _runOcr(tempFile);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Error picking file: $e')),
        );
      }
    }
  }

  Future<void> _runOcr(File imageFile) async {
    setState(() => _isProcessingOcr = true);
    try {
      final inputImage = InputImage.fromFile(imageFile);
      final textRecognizer = TextRecognizer();
      final RecognizedText recognizedText = await textRecognizer.processImage(inputImage);
      await textRecognizer.close();

      final extracted = OcrAutofill.runAutoFill(recognizedText.text, widget.documentType);

      setState(() {
        if (extracted['title'] != null && _titleCtrl.text.isEmpty) {
          _titleCtrl.text = extracted['title']!;
        }
        if (extracted['cardholderName'] != null) {
          _cardholderCtrl.text = extracted['cardholderName']!;
        }
        if (extracted['cardNumber'] != null) {
          _cardNumberCtrl.text = formatCardNumberInput(extracted['cardNumber']!);
        }
        if (extracted['cardExpiry'] != null) {
          _cardExpiryCtrl.text = formatExpiryInput(extracted['cardExpiry']!);
        }
        if (extracted['cardCvv'] != null) {
          _cardCvvCtrl.text = extracted['cardCvv']!;
        }
        if (extracted['aadhaarName'] != null) {
          _aadhaarNameCtrl.text = extracted['aadhaarName']!;
        }
        if (extracted['aadhaarNumber'] != null) {
          _aadhaarNumberCtrl.text = formatAadhaarInput(extracted['aadhaarNumber']!);
        }
        if (extracted['aadhaarDob'] != null) {
          _aadhaarDobCtrl.text = extracted['aadhaarDob']!;
        }
        if (extracted['panName'] != null) {
          _panNameCtrl.text = extracted['panName']!;
        }
        if (extracted['panNumber'] != null) {
          _panNumberCtrl.text = extracted['panNumber']!;
        }
        if (extracted['panFatherName'] != null) {
          _panFatherNameCtrl.text = extracted['panFatherName']!;
        }
        if (extracted['panDob'] != null) {
          _panDobCtrl.text = extracted['panDob']!;
        }
        if (extracted['dlHolderName'] != null) {
          _dlHolderNameCtrl.text = extracted['dlHolderName']!;
        }
        if (extracted['dlNumber'] != null) {
          _dlNumberCtrl.text = extracted['dlNumber']!;
        }
        if (extracted['dlDob'] != null) {
          _dlDobCtrl.text = extracted['dlDob']!;
        }
        if (extracted['dlExpiry'] != null) {
          _dlExpiryCtrl.text = extracted['dlExpiry']!;
        }
        if (extracted['rcOwnerName'] != null) {
          _rcOwnerNameCtrl.text = extracted['rcOwnerName']!;
        }
        if (extracted['rcNumber'] != null) {
          _rcNumberCtrl.text = extracted['rcNumber']!;
        }
        if (extracted['rcExpiry'] != null) {
          _rcExpiryCtrl.text = extracted['rcExpiry']!;
        }
        if (extracted['genericIdNumber'] != null) {
          _genericIdNumberCtrl.text = extracted['genericIdNumber']!;
        }
        if (extracted['genericIdName'] != null) {
          _genericIdNameCtrl.text = extracted['genericIdName']!;
        }
      });

      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('ML Kit OCR applied: Form fields auto-filled'),
            duration: Duration(seconds: 3),
          ),
        );
      }
    } catch (_) {
      // OCR fail silently
    } finally {
      if (mounted) setState(() => _isProcessingOcr = false);
    }
  }

  Future<void> _saveDocument() async {
    if (!_formKey.currentState!.validate()) return;
    HapticFeedback.mediumImpact();

    Uint8List? finalImageBytes;
    String ext = 'jpg';

    if (_pageBytes.length == 1) {
      finalImageBytes = _pageBytes.first;
      ext = _pageExtensions.first;
    } else if (_pageBytes.length > 1) {
      final pdf = pw.Document();
      for (final bytes in _pageBytes) {
        final image = pw.MemoryImage(bytes);
        pdf.addPage(
          pw.Page(
            build: (pw.Context context) {
              return pw.FullPage(
                ignoreMargins: true,
                child: pw.Image(image, fit: pw.BoxFit.contain),
              );
            },
          ),
        );
      }
      finalImageBytes = await pdf.save();
      ext = 'pdf';
    }

    final id = widget.existingDocument?.id ?? _uuid.v4();
    final dateAdded = widget.existingDocument?.dateAdded ?? DateTime.now().millisecondsSinceEpoch;
    final colorIdx = widget.existingDocument?.cardColorIndex ?? Random().nextInt(100);

    final doc = VaultDocument(
      id: id,
      title: _titleCtrl.text.trim(),
      type: widget.documentType,
      dateAdded: dateAdded,
      cardColorIndex: colorIdx,
      cardholderName: widget.documentType == DocumentType.paymentCard ? _cardholderCtrl.text.trim() : null,
      cardNumber: widget.documentType == DocumentType.paymentCard ? _cardNumberCtrl.text.trim() : null,
      cardExpiry: widget.documentType == DocumentType.paymentCard ? _cardExpiryCtrl.text.trim() : null,
      cardCvv: widget.documentType == DocumentType.paymentCard ? _cardCvvCtrl.text.trim() : null,
      cardType: widget.documentType == DocumentType.paymentCard ? _cardType : null,
      aadhaarName: widget.documentType == DocumentType.aadhaarCard ? _aadhaarNameCtrl.text.trim() : null,
      aadhaarNumber: widget.documentType == DocumentType.aadhaarCard ? _aadhaarNumberCtrl.text.trim() : null,
      aadhaarDob: widget.documentType == DocumentType.aadhaarCard ? _aadhaarDobCtrl.text.trim() : null,
      aadhaarGender: widget.documentType == DocumentType.aadhaarCard ? _aadhaarGender : null,
      panName: widget.documentType == DocumentType.panCard ? _panNameCtrl.text.trim() : null,
      panNumber: widget.documentType == DocumentType.panCard ? _panNumberCtrl.text.trim().toUpperCase() : null,
      panFatherName: widget.documentType == DocumentType.panCard ? _panFatherNameCtrl.text.trim() : null,
      panDob: widget.documentType == DocumentType.panCard ? _panDobCtrl.text.trim() : null,
      dlHolderName: widget.documentType == DocumentType.driversLicense ? _dlHolderNameCtrl.text.trim() : null,
      dlNumber: widget.documentType == DocumentType.driversLicense ? _dlNumberCtrl.text.trim() : null,
      dlDob: widget.documentType == DocumentType.driversLicense ? _dlDobCtrl.text.trim() : null,
      dlExpiry: widget.documentType == DocumentType.driversLicense ? _dlExpiryCtrl.text.trim() : null,
      dlState: widget.documentType == DocumentType.driversLicense ? _dlStateCtrl.text.trim() : null,
      rcOwnerName: widget.documentType == DocumentType.vehicleRc ? _rcOwnerNameCtrl.text.trim() : null,
      rcNumber: widget.documentType == DocumentType.vehicleRc ? _rcNumberCtrl.text.trim() : null,
      rcChassisNumber: widget.documentType == DocumentType.vehicleRc ? _rcChassisCtrl.text.trim() : null,
      rcEngineNumber: widget.documentType == DocumentType.vehicleRc ? _rcEngineCtrl.text.trim() : null,
      rcExpiry: widget.documentType == DocumentType.vehicleRc ? _rcExpiryCtrl.text.trim() : null,
      genericIdNumber: widget.documentType == DocumentType.genericId ? _genericIdNumberCtrl.text.trim() : null,
      genericIdName: widget.documentType == DocumentType.genericId ? _genericIdNameCtrl.text.trim() : null,
      genericIdExpiry: widget.documentType == DocumentType.genericId ? _genericIdExpiryCtrl.text.trim() : null,
      genericIdType: widget.documentType == DocumentType.genericId ? _genericIdTypeCtrl.text.trim() : null,
      imagePath: widget.existingDocument?.imagePath,
      ocrText: widget.existingDocument?.ocrText,
    );

    if (widget.existingDocument != null) {
      await ref.read(vaultProvider.notifier).updateDocument(doc, imageBytes: finalImageBytes, ext: ext);
    } else {
      await ref.read(vaultProvider.notifier).addDocument(doc, imageBytes: finalImageBytes, ext: ext);
    }

    if (widget.onSaved != null) widget.onSaved!();
    if (mounted) Navigator.pop(context);
  }

  Widget _buildTextField({
    required String label,
    required TextEditingController controller,
    String? hint,
    TextInputType keyboardType = TextInputType.text,
    List<TextInputFormatter>? inputFormatters,
    String? Function(String?)? validator,
    bool readOnly = false,
    VoidCallback? onTap,
    IconData? icon,
    Widget? suffixIcon,
  }) {
    return Container(
      margin: const EdgeInsets.only(bottom: 14),
      decoration: BoxDecoration(
        color: appleCardBackground,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: appleBorderStroke),
      ),
      child: TextFormField(
        controller: controller,
        keyboardType: keyboardType,
        inputFormatters: inputFormatters,
        validator: validator,
        readOnly: readOnly,
        onTap: onTap,
        style: GoogleFonts.inter(color: textPrimary, fontSize: 16, fontWeight: FontWeight.w500),
        decoration: InputDecoration(
          labelText: label,
          hintText: hint,
          labelStyle: GoogleFonts.inter(color: textSecondary, fontSize: 14),
          hintStyle: GoogleFonts.inter(color: textMuted, fontSize: 14),
          prefixIcon: icon != null ? Icon(icon, color: appleBlue, size: 20) : null,
          suffixIcon: suffixIcon,
          border: InputBorder.none,
          enabledBorder: InputBorder.none,
          focusedBorder: InputBorder.none,
          errorBorder: InputBorder.none,
          focusedErrorBorder: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 14),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: applePitchBlack,
      appBar: AppBar(
        backgroundColor: applePitchBlack,
        leading: TextButton(
          onPressed: () => Navigator.pop(context),
          child: Text(
            'Cancel',
            style: GoogleFonts.inter(color: appleBlue, fontSize: 16, fontWeight: FontWeight.w500),
          ),
        ),
        leadingWidth: 80,
        title: Text(_getTypeTitle()),
        actions: [
          TextButton(
            onPressed: _saveDocument,
            child: Text(
              'Save',
              style: GoogleFonts.inter(color: appleBlue, fontSize: 16, fontWeight: FontWeight.w700),
            ),
          ),
          const SizedBox(width: 8),
        ],
      ),
      body: SingleChildScrollView(
        physics: const BouncingScrollPhysics(),
        padding: const EdgeInsets.all(20),
        child: Form(
          key: _formKey,
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Attachments & Scanning Header Card
              Container(
                width: double.infinity,
                padding: const EdgeInsets.all(16),
                decoration: BoxDecoration(
                  color: appleCardBackground,
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: appleBorderStroke),
                ),
                child: Column(
                  children: [
                    if (_pageBytes.isNotEmpty) ...[
                      SizedBox(
                        height: 120,
                        child: ListView.builder(
                          scrollDirection: Axis.horizontal,
                          physics: const BouncingScrollPhysics(),
                          itemCount: _pageBytes.length,
                          itemBuilder: (ctx, i) => Stack(
                            children: [
                              Container(
                                margin: const EdgeInsets.only(right: 12),
                                width: 90,
                                height: 120,
                                decoration: BoxDecoration(
                                  borderRadius: BorderRadius.circular(12),
                                  border: Border.all(color: appleBorderStroke),
                                  image: DecorationImage(
                                    image: MemoryImage(_pageBytes[i]),
                                    fit: BoxFit.cover,
                                  ),
                                ),
                              ),
                              Positioned(
                                top: 4,
                                right: 16,
                                child: AppleTouchable(
                                  onTap: () {
                                    setState(() {
                                      _pageBytes.removeAt(i);
                                      _pageExtensions.removeAt(i);
                                    });
                                  },
                                  child: Container(
                                    padding: const EdgeInsets.all(4),
                                    decoration: BoxDecoration(
                                      color: Colors.black.withAlpha(180),
                                      shape: BoxShape.circle,
                                    ),
                                    child: const Icon(Icons.close_rounded, size: 14, color: Colors.white),
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ),
                      const SizedBox(height: 14),
                    ],
                    Row(
                      children: [
                        Expanded(
                          child: AppleTouchable(
                            onTap: _scanDocument,
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              decoration: BoxDecoration(
                                color: appleBlue.withAlpha(30),
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(color: appleBlue.withAlpha(60)),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Icon(Icons.document_scanner_rounded, color: appleBlue, size: 20),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Scan Document',
                                    style: GoogleFonts.inter(
                                      color: appleBlue,
                                      fontWeight: FontWeight.w700,
                                      fontSize: 14,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                        const SizedBox(width: 12),
                        Expanded(
                          child: AppleTouchable(
                            onTap: _pickFiles,
                            child: Container(
                              padding: const EdgeInsets.symmetric(vertical: 14),
                              decoration: BoxDecoration(
                                color: appleCardSecondary,
                                borderRadius: BorderRadius.circular(14),
                                border: Border.all(color: appleBorderStroke),
                              ),
                              child: Row(
                                mainAxisAlignment: MainAxisAlignment.center,
                                children: [
                                  const Icon(Icons.upload_file_rounded, color: textSecondary, size: 20),
                                  const SizedBox(width: 8),
                                  Text(
                                    'Upload File',
                                    style: GoogleFonts.inter(
                                      color: textPrimary,
                                      fontWeight: FontWeight.w600,
                                      fontSize: 14,
                                    ),
                                  ),
                                ],
                              ),
                            ),
                          ),
                        ),
                      ],
                    ),
                    if (_isProcessingOcr) ...[
                      const SizedBox(height: 12),
                      Row(
                        mainAxisAlignment: MainAxisAlignment.center,
                        children: [
                          const SizedBox(
                            width: 16,
                            height: 16,
                            child: CircularProgressIndicator(strokeWidth: 2, color: appleBlue),
                          ),
                          const SizedBox(width: 8),
                          Text(
                            'ML Kit OCR parsing document...',
                            style: GoogleFonts.inter(color: appleBlue, fontSize: 13, fontWeight: FontWeight.w500),
                          ),
                        ],
                      ),
                    ],
                  ],
                ),
              ),

              const SizedBox(height: 24),
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

              // Title Field
              _buildTextField(
                label: 'Title',
                controller: _titleCtrl,
                hint: 'e.g. Personal Credit Card',
                icon: Icons.title_rounded,
                validator: (v) => v == null || v.trim().isEmpty ? 'Title is required' : null,
              ),

              // Dynamic Fields per Type
              if (widget.documentType == DocumentType.paymentCard) ...[
                Container(
                  margin: const EdgeInsets.only(bottom: 14),
                  padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 4),
                  decoration: BoxDecoration(
                    color: appleCardBackground,
                    borderRadius: BorderRadius.circular(16),
                    border: Border.all(color: appleBorderStroke),
                  ),
                  child: DropdownButtonHideUnderline(
                    child: DropdownButton<String>(
                      value: _cardType,
                      isExpanded: true,
                      dropdownColor: appleCardSecondary,
                      icon: const Icon(Icons.keyboard_arrow_down_rounded, color: appleBlue),
                      items: ['Visa', 'Mastercard', 'Amex', 'RuPay', 'Other']
                          .map((t) => DropdownMenuItem(
                                value: t,
                                child: Text(t, style: GoogleFonts.inter(color: textPrimary, fontSize: 15, fontWeight: FontWeight.w500)),
                              ))
                          .toList(),
                      onChanged: (val) {
                        if (val != null) setState(() => _cardType = val);
                      },
                    ),
                  ),
                ),
                _buildTextField(
                  label: 'Cardholder Name',
                  controller: _cardholderCtrl,
                  icon: Icons.person_rounded,
                ),
                _buildTextField(
                  label: 'Card Number',
                  controller: _cardNumberCtrl,
                  keyboardType: TextInputType.number,
                  inputFormatters: [CreditCardFormatter()],
                  icon: Icons.credit_card_rounded,
                  validator: (v) {
                    if (v == null || v.isEmpty) return null;
                    final cleaned = v.replaceAll(' ', '');
                    if (cleaned.length < 15) return 'Invalid card number length';
                    return null;
                  },
                ),
                Row(
                  children: [
                    Expanded(
                      child: _buildTextField(
                        label: 'Expiry (MM/YY)',
                        controller: _cardExpiryCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [ExpiryDateFormatter()],
                        icon: Icons.calendar_today_rounded,
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: _buildTextField(
                        label: 'CVV',
                        controller: _cardCvvCtrl,
                        keyboardType: TextInputType.number,
                        inputFormatters: [FilteringTextInputFormatter.digitsOnly, LengthLimitingTextInputFormatter(4)],
                        icon: Icons.lock_outline_rounded,
                      ),
                    ),
                  ],
                ),
              ] else if (widget.documentType == DocumentType.aadhaarCard) ...[
                _buildTextField(
                  label: 'Full Name',
                  controller: _aadhaarNameCtrl,
                  icon: Icons.person_rounded,
                ),
                _buildTextField(
                  label: 'Aadhaar Number',
                  controller: _aadhaarNumberCtrl,
                  keyboardType: TextInputType.number,
                  inputFormatters: [AadhaarFormatter()],
                  icon: Icons.badge_rounded,
                  validator: (v) {
                    if (v == null || v.isEmpty) return null;
                    final cleaned = v.replaceAll(' ', '');
                    if (cleaned.length != 12) return 'Aadhaar must be 12 digits';
                    return null;
                  },
                ),
                _buildTextField(
                  label: 'Date of Birth / YOB',
                  controller: _aadhaarDobCtrl,
                  readOnly: true,
                  onTap: () => _selectDate(context, _aadhaarDobCtrl),
                  icon: Icons.calendar_today_rounded,
                  suffixIcon: const Icon(Icons.date_range_rounded, color: appleBlue, size: 20),
                ),
              ] else if (widget.documentType == DocumentType.panCard) ...[
                _buildTextField(
                  label: 'Full Name',
                  controller: _panNameCtrl,
                  icon: Icons.person_rounded,
                ),
                _buildTextField(
                  label: 'PAN Number',
                  controller: _panNumberCtrl,
                  inputFormatters: [LengthLimitingTextInputFormatter(10)],
                  icon: Icons.article_rounded,
                  validator: (v) {
                    if (v == null || v.isEmpty) return null;
                    if (v.length != 10) return 'PAN must be 10 characters';
                    return null;
                  },
                ),
                _buildTextField(
                  label: "Father's Name",
                  controller: _panFatherNameCtrl,
                  icon: Icons.person_outline_rounded,
                ),
                _buildTextField(
                  label: 'Date of Birth',
                  controller: _panDobCtrl,
                  readOnly: true,
                  onTap: () => _selectDate(context, _panDobCtrl),
                  icon: Icons.calendar_today_rounded,
                  suffixIcon: const Icon(Icons.date_range_rounded, color: appleBlue, size: 20),
                ),
              ] else if (widget.documentType == DocumentType.driversLicense) ...[
                _buildTextField(
                  label: 'Holder Name',
                  controller: _dlHolderNameCtrl,
                  icon: Icons.person_rounded,
                ),
                _buildTextField(
                  label: 'Licence Number',
                  controller: _dlNumberCtrl,
                  icon: Icons.drive_eta_rounded,
                ),
                _buildTextField(
                  label: 'Date of Birth',
                  controller: _dlDobCtrl,
                  readOnly: true,
                  onTap: () => _selectDate(context, _dlDobCtrl),
                  icon: Icons.calendar_today_rounded,
                  suffixIcon: const Icon(Icons.date_range_rounded, color: appleBlue, size: 20),
                ),
                _buildTextField(
                  label: 'Expiry Date',
                  controller: _dlExpiryCtrl,
                  readOnly: true,
                  onTap: () => _selectDate(context, _dlExpiryCtrl),
                  icon: Icons.event_available_rounded,
                  suffixIcon: const Icon(Icons.date_range_rounded, color: appleBlue, size: 20),
                ),
                _buildTextField(
                  label: 'State',
                  controller: _dlStateCtrl,
                  icon: Icons.map_rounded,
                ),
              ] else if (widget.documentType == DocumentType.vehicleRc) ...[
                _buildTextField(
                  label: 'Owner Name',
                  controller: _rcOwnerNameCtrl,
                  icon: Icons.person_rounded,
                ),
                _buildTextField(
                  label: 'Registration Number',
                  controller: _rcNumberCtrl,
                  icon: Icons.directions_car_rounded,
                ),
                _buildTextField(
                  label: 'Chassis Number',
                  controller: _rcChassisCtrl,
                  icon: Icons.build_rounded,
                ),
                _buildTextField(
                  label: 'Engine Number',
                  controller: _rcEngineCtrl,
                  icon: Icons.engineering_rounded,
                ),
                _buildTextField(
                  label: 'Expiry Date',
                  controller: _rcExpiryCtrl,
                  readOnly: true,
                  onTap: () => _selectDate(context, _rcExpiryCtrl),
                  icon: Icons.event_available_rounded,
                  suffixIcon: const Icon(Icons.date_range_rounded, color: appleBlue, size: 20),
                ),
              ] else if (widget.documentType == DocumentType.genericId) ...[
                _buildTextField(
                  label: 'Card Type',
                  controller: _genericIdTypeCtrl,
                  hint: 'e.g. Passport, Employee ID',
                  icon: Icons.card_membership_rounded,
                ),
                _buildTextField(
                  label: 'ID Number',
                  controller: _genericIdNumberCtrl,
                  icon: Icons.pin_rounded,
                ),
                _buildTextField(
                  label: 'Name on Card',
                  controller: _genericIdNameCtrl,
                  icon: Icons.person_rounded,
                ),
                _buildTextField(
                  label: 'Expiry Date',
                  controller: _genericIdExpiryCtrl,
                  readOnly: true,
                  onTap: () => _selectDate(context, _genericIdExpiryCtrl),
                  icon: Icons.event_available_rounded,
                  suffixIcon: const Icon(Icons.date_range_rounded, color: appleBlue, size: 20),
                ),
              ],

              const SizedBox(height: 32),
              // Large Apple Action Button
              SizedBox(
                width: double.infinity,
                child: AppleTouchable(
                  onTap: _saveDocument,
                  child: Container(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                    decoration: BoxDecoration(
                      color: appleBlue,
                      borderRadius: BorderRadius.circular(16),
                      boxShadow: [
                        BoxShadow(
                          color: appleBlue.withAlpha(80),
                          blurRadius: 18,
                          offset: const Offset(0, 4),
                        ),
                      ],
                    ),
                    child: Center(
                      child: Text(
                        'Save Document',
                        style: GoogleFonts.inter(
                          color: Colors.white,
                          fontWeight: FontWeight.w700,
                          fontSize: 16,
                        ),
                      ),
                    ),
                  ),
                ),
              ),
              const SizedBox(height: 40),
            ],
          ),
        ),
      ),
    );
  }
}
