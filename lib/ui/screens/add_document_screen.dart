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

class AddDocumentScreen extends ConsumerStatefulWidget {
  final DocumentType documentType;
  final VaultDocument? existingDocument;
  final VoidCallback onSaved;

  const AddDocumentScreen({
    super.key,
    required this.documentType,
    this.existingDocument,
    required this.onSaved,
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

  String _typeName() {
    switch (widget.documentType) {
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

  bool _isMobilePlatform() {
    if (kIsWeb) return false;
    return Platform.isAndroid || Platform.isIOS;
  }

  Future<void> _selectDate(BuildContext context, TextEditingController controller) async {
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
              primary: accentIndigo,
              onPrimary: Colors.black,
              surface: cinemaSurface,
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
          SnackBar(content: Text('Scanning failed: $e')),
        );
      }
    }
  }

  Future<void> _importPdfPages(Uint8List pdfBytes) async {
    try {
      final document = await px.PdfDocument.openData(pdfBytes);
      final pageCount = document.pagesCount;
      for (int i = 1; i <= pageCount; i++) {
        final page = await document.getPage(i);
        final pageImage = await page.render(
          width: page.width * 2,
          height: page.height * 2,
          format: px.PdfPageImageFormat.png,
        );
        await page.close();
        if (pageImage != null) {
          setState(() {
            _pageBytes.add(pageImage.bytes);
            _pageExtensions.add('png');
          });
        }
      }
      await document.close();
    } catch (e) {
      debugPrint('Error rendering PDF page: $e');
    }
  }

  Future<void> _uploadFiles() async {
    try {
      ref.read(isLaunchingExternalProvider.notifier).state = true;
      final result = await FilePicker.platform.pickFiles(
        allowMultiple: true,
        type: FileType.custom,
        allowedExtensions: ['jpg', 'jpeg', 'png', 'pdf'],
      );
      ref.read(isLaunchingExternalProvider.notifier).state = false;

      if (result == null || result.files.isEmpty) return;

      setState(() => _isProcessingOcr = true);

      for (final file in result.files) {
        if (file.path == null) continue;
        final bytes = await File(file.path!).readAsBytes();
        final ext = file.extension?.toLowerCase() ?? '';

        if (ext == 'pdf') {
          await _importPdfPages(bytes);
        } else {
          setState(() {
            _pageBytes.add(bytes);
            _pageExtensions.add(ext);
          });
        }
      }

      if (_pageBytes.isNotEmpty) {
        final tempDir = await getTemporaryDirectory();
        final tempFile = File('${tempDir.path}/temp_ocr_first_page.png');
        await tempFile.writeAsBytes(_pageBytes.first);
        await _runOcr(tempFile);
      }
    } catch (e) {
      ref.read(isLaunchingExternalProvider.notifier).state = false;
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Failed to upload files: $e')),
        );
      }
    } finally {
      setState(() => _isProcessingOcr = false);
    }
  }

  Future<void> _runOcr(File imageFile) async {
    if (!_isMobilePlatform()) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('OCR not available on desktop. Please fill fields manually.'),
          ),
        );
      }
      return;
    }

    setState(() => _isProcessingOcr = true);
    try {
      // Dynamic import to avoid compile error on desktop
      final recognizer = _createTextRecognizer();
      if (recognizer == null) return;
      final inputImage = _createInputImage(imageFile);
      if (inputImage == null) return;
      final result = await _processImage(recognizer, inputImage);
      await _closeRecognizer(recognizer);
      if (result != null && mounted) {
        _applyOcrResult(result);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('OCR failed: $e')),
        );
      }
    } finally {
      if (mounted) setState(() => _isProcessingOcr = false);
    }
  }

  // These use dynamic to avoid import issues on desktop
  dynamic _createTextRecognizer() {
    try {
      // ignore: avoid_dynamic_calls
      return _MlKitHelper.createRecognizer();
    } catch (_) {
      return null;
    }
  }

  dynamic _createInputImage(File file) {
    try {
      return _MlKitHelper.createInputImage(file);
    } catch (_) {
      return null;
    }
  }

  Future<String?> _processImage(dynamic recognizer, dynamic inputImage) async {
    try {
      return await _MlKitHelper.processImage(recognizer, inputImage);
    } catch (_) {
      return null;
    }
  }

  Future<void> _closeRecognizer(dynamic recognizer) async {
    try {
      await _MlKitHelper.closeRecognizer(recognizer);
    } catch (_) {}
  }

  void _applyOcrResult(String text) {
    final fields = OcrAutofill.runAutoFill(text, widget.documentType);
    setState(() {
      switch (widget.documentType) {
        case DocumentType.paymentCard:
          if (fields['cardNumber'] != null && _cardNumberCtrl.text.isEmpty) {
            _cardNumberCtrl.text = fields['cardNumber']!;
          }
          if (fields['cardExpiry'] != null && _cardExpiryCtrl.text.isEmpty) {
            _cardExpiryCtrl.text = fields['cardExpiry']!;
          }
          if (fields['cardholderName'] != null && _cardholderCtrl.text.isEmpty) {
            _cardholderCtrl.text = fields['cardholderName']!;
          }
        case DocumentType.aadhaarCard:
          if (fields['aadhaarNumber'] != null && _aadhaarNumberCtrl.text.isEmpty) {
            _aadhaarNumberCtrl.text = fields['aadhaarNumber']!;
          }
          if (fields['aadhaarName'] != null && _aadhaarNameCtrl.text.isEmpty) {
            _aadhaarNameCtrl.text = fields['aadhaarName']!;
          }
          if (fields['aadhaarDob'] != null && _aadhaarDobCtrl.text.isEmpty) {
            _aadhaarDobCtrl.text = fields['aadhaarDob']!;
          }
          if (fields['aadhaarGender'] != null) {
            _aadhaarGender = fields['aadhaarGender']!;
          }
        case DocumentType.panCard:
          if (fields['panNumber'] != null && _panNumberCtrl.text.isEmpty) {
            _panNumberCtrl.text = fields['panNumber']!;
          }
          if (fields['panName'] != null && _panNameCtrl.text.isEmpty) {
            _panNameCtrl.text = fields['panName']!;
          }
          if (fields['panFatherName'] != null && _panFatherNameCtrl.text.isEmpty) {
            _panFatherNameCtrl.text = fields['panFatherName']!;
          }
          if (fields['panDob'] != null && _panDobCtrl.text.isEmpty) {
            _panDobCtrl.text = fields['panDob']!;
          }
        case DocumentType.driversLicense:
          if (fields['dlNumber'] != null && _dlNumberCtrl.text.isEmpty) {
            _dlNumberCtrl.text = fields['dlNumber']!;
          }
          if (fields['dlHolderName'] != null && _dlHolderNameCtrl.text.isEmpty) {
            _dlHolderNameCtrl.text = fields['dlHolderName']!;
          }
          if (fields['dlDob'] != null && _dlDobCtrl.text.isEmpty) {
            _dlDobCtrl.text = fields['dlDob']!;
          }
          if (fields['dlExpiry'] != null && _dlExpiryCtrl.text.isEmpty) {
            _dlExpiryCtrl.text = fields['dlExpiry']!;
          }
          if (fields['dlState'] != null && _dlStateCtrl.text.isEmpty) {
            _dlStateCtrl.text = fields['dlState']!;
          }
        case DocumentType.vehicleRc:
          if (fields['rcNumber'] != null && _rcNumberCtrl.text.isEmpty) {
            _rcNumberCtrl.text = fields['rcNumber']!;
          }
          if (fields['rcOwnerName'] != null && _rcOwnerNameCtrl.text.isEmpty) {
            _rcOwnerNameCtrl.text = fields['rcOwnerName']!;
          }
          if (fields['rcChassisNumber'] != null && _rcChassisCtrl.text.isEmpty) {
            _rcChassisCtrl.text = fields['rcChassisNumber']!;
          }
          if (fields['rcEngineNumber'] != null && _rcEngineCtrl.text.isEmpty) {
            _rcEngineCtrl.text = fields['rcEngineNumber']!;
          }
          if (fields['rcExpiry'] != null && _rcExpiryCtrl.text.isEmpty) {
            _rcExpiryCtrl.text = fields['rcExpiry']!;
          }
        case DocumentType.genericId:
          if (fields['genericIdNumber'] != null && _genericIdNumberCtrl.text.isEmpty) {
            _genericIdNumberCtrl.text = fields['genericIdNumber']!;
          }
          if (fields['genericIdName'] != null && _genericIdNameCtrl.text.isEmpty) {
            _genericIdNameCtrl.text = fields['genericIdName']!;
          }
          if (fields['genericIdExpiry'] != null && _genericIdExpiryCtrl.text.isEmpty) {
            _genericIdExpiryCtrl.text = fields['genericIdExpiry']!;
          }
          if (fields['genericIdType'] != null && _genericIdTypeCtrl.text.isEmpty) {
            _genericIdTypeCtrl.text = fields['genericIdType']!;
          }
      }
    });
  }

  void _removePage(int index) {
    setState(() {
      _pageBytes.removeAt(index);
      _pageExtensions.removeAt(index);
    });
  }

  Future<Uint8List?> _buildAttachment() async {
    if (_pageBytes.isEmpty) return null;
    if (_pageBytes.length == 1) return _pageBytes[0];

    // Merge two pages into PDF
    final pdf = pw.Document();
    for (final bytes in _pageBytes) {
      final pdfImage = pw.MemoryImage(bytes);
      pdf.addPage(
        pw.Page(
          build: (ctx) => pw.Center(child: pw.Image(pdfImage)),
        ),
      );
    }
    return await pdf.save();
  }

  Future<void> _save() async {
    if (!_formKey.currentState!.validate()) return;

    final id = widget.existingDocument?.id ?? _uuid.v4();
    final dateAdded =
        widget.existingDocument?.dateAdded ?? DateTime.now().millisecondsSinceEpoch;
    final cardColorIndex =
        widget.existingDocument?.cardColorIndex ?? Random().nextInt(5);

    Uint8List? attachmentBytes;
    String? attachmentExt;
    final oldImagePath = widget.existingDocument?.imagePath;
    final oldBackImagePath = widget.existingDocument?.backImagePath;

    if (_pageBytes.isNotEmpty) {
      if (_pageBytes.length > 1) {
        attachmentBytes = await _buildAttachment();
        attachmentExt = 'pdf';
      } else {
        attachmentBytes = _pageBytes[0];
        attachmentExt = _pageExtensions[0];
      }
    }

    VaultDocument doc;
    switch (widget.documentType) {
      case DocumentType.paymentCard:
        doc = VaultDocument(
          id: id,
          title: _titleCtrl.text.trim(),
          type: DocumentType.paymentCard,
          dateAdded: dateAdded,
          cardColorIndex: cardColorIndex,
          cardholderName: _cardholderCtrl.text.trim(),
          cardNumber: _cardNumberCtrl.text.replaceAll(' ', ''),
          cardExpiry: _cardExpiryCtrl.text.trim(),
          cardCvv: _cardCvvCtrl.text.trim(),
          cardType: _cardType,
          imagePath: attachmentBytes != null ? null : oldImagePath,
          backImagePath: attachmentBytes != null ? null : oldBackImagePath,
        );
      case DocumentType.aadhaarCard:
        doc = VaultDocument(
          id: id,
          title: _titleCtrl.text.trim(),
          type: DocumentType.aadhaarCard,
          dateAdded: dateAdded,
          cardColorIndex: cardColorIndex,
          aadhaarName: _aadhaarNameCtrl.text.trim(),
          aadhaarNumber: _aadhaarNumberCtrl.text.replaceAll(' ', ''),
          aadhaarDob: _aadhaarDobCtrl.text.trim(),
          aadhaarGender: _aadhaarGender,
          imagePath: attachmentBytes != null ? null : oldImagePath,
          backImagePath: attachmentBytes != null ? null : oldBackImagePath,
        );
      case DocumentType.panCard:
        doc = VaultDocument(
          id: id,
          title: _titleCtrl.text.trim(),
          type: DocumentType.panCard,
          dateAdded: dateAdded,
          cardColorIndex: cardColorIndex,
          panName: _panNameCtrl.text.trim(),
          panNumber: _panNumberCtrl.text.trim().toUpperCase(),
          panFatherName: _panFatherNameCtrl.text.trim(),
          panDob: _panDobCtrl.text.trim(),
          imagePath: attachmentBytes != null ? null : oldImagePath,
          backImagePath: attachmentBytes != null ? null : oldBackImagePath,
        );
      case DocumentType.driversLicense:
        doc = VaultDocument(
          id: id,
          title: _titleCtrl.text.trim(),
          type: DocumentType.driversLicense,
          dateAdded: dateAdded,
          cardColorIndex: cardColorIndex,
          dlHolderName: _dlHolderNameCtrl.text.trim(),
          dlNumber: _dlNumberCtrl.text.trim(),
          dlDob: _dlDobCtrl.text.trim(),
          dlExpiry: _dlExpiryCtrl.text.trim(),
          dlState: _dlStateCtrl.text.trim(),
          imagePath: attachmentBytes != null ? null : oldImagePath,
          backImagePath: attachmentBytes != null ? null : oldBackImagePath,
        );
      case DocumentType.vehicleRc:
        doc = VaultDocument(
          id: id,
          title: _titleCtrl.text.trim(),
          type: DocumentType.vehicleRc,
          dateAdded: dateAdded,
          cardColorIndex: cardColorIndex,
          rcOwnerName: _rcOwnerNameCtrl.text.trim(),
          rcNumber: _rcNumberCtrl.text.trim(),
          rcChassisNumber: _rcChassisCtrl.text.trim(),
          rcEngineNumber: _rcEngineCtrl.text.trim(),
          rcExpiry: _rcExpiryCtrl.text.trim(),
          imagePath: attachmentBytes != null ? null : oldImagePath,
          backImagePath: attachmentBytes != null ? null : oldBackImagePath,
        );
      case DocumentType.genericId:
        doc = VaultDocument(
          id: id,
          title: _titleCtrl.text.trim(),
          type: DocumentType.genericId,
          dateAdded: dateAdded,
          cardColorIndex: cardColorIndex,
          genericIdNumber: _genericIdNumberCtrl.text.trim(),
          genericIdName: _genericIdNameCtrl.text.trim(),
          genericIdExpiry: _genericIdExpiryCtrl.text.trim(),
          genericIdType: _genericIdTypeCtrl.text.trim(),
          imagePath: attachmentBytes != null ? null : oldImagePath,
          backImagePath: attachmentBytes != null ? null : oldBackImagePath,
        );
    }

    if (widget.existingDocument != null) {
      await ref.read(vaultProvider.notifier).updateDocument(
            doc,
            imageBytes: attachmentBytes,
            ext: attachmentExt,
          );
    } else {
      await ref.read(vaultProvider.notifier).addDocument(
            doc,
            imageBytes: attachmentBytes,
            ext: attachmentExt,
          );
    }

    if (mounted) widget.onSaved();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: cinemaBase,
      appBar: AppBar(
        backgroundColor: cinemaElevated,
        title: Text(
          '${widget.existingDocument != null ? 'Edit' : 'Add'} ${_typeName()}',
        ),
        leading: IconButton(
          icon: const Icon(Icons.arrow_back),
          onPressed: () => Navigator.pop(context),
        ),
      ),
      body: Form(
        key: _formKey,
        child: ListView(
          padding: const EdgeInsets.all(16),
          children: [
            _buildImagePanel(),
            const SizedBox(height: 20),
            _buildFormFields(),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: _save,
                child: Padding(
                  padding: const EdgeInsets.symmetric(vertical: 4),
                  child: Text(
                    widget.existingDocument != null ? 'SAVE CHANGES' : 'SAVE TO VAULT',
                    style: GoogleFonts.inter(
                      fontWeight: FontWeight.w700,
                      fontSize: 15,
                      letterSpacing: 1,
                    ),
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

  Widget _buildImagePanel() {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: cinemaElevated,
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: cinemaStroke),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Icon(Icons.document_scanner, color: accentIndigo, size: 18),
              const SizedBox(width: 8),
              Text(
                'Document Scan',
                style: GoogleFonts.inter(
                  color: textPrimary,
                  fontWeight: FontWeight.w600,
                  fontSize: 15,
                ),
              ),
              if (_isProcessingOcr) ...[
                const SizedBox(width: 12),
                const SizedBox(
                  width: 16,
                  height: 16,
                  child: CircularProgressIndicator(strokeWidth: 2, color: accentIndigo),
                ),
                const SizedBox(width: 6),
                Text('Scanning...', style: GoogleFonts.inter(color: textSecondary, fontSize: 12)),
              ],
            ],
          ),
          if (_pageBytes.isNotEmpty) ...[
            const SizedBox(height: 12),
            SizedBox(
              height: 130,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: _pageBytes.length,
                separatorBuilder: (_, __) => const SizedBox(width: 8),
                itemBuilder: (context, i) {
                  return Stack(
                    children: [
                      ClipRRect(
                        borderRadius: BorderRadius.circular(8),
                        child: Image.memory(
                          _pageBytes[i],
                          height: 120,
                          width: 100,
                          fit: BoxFit.cover,
                        ),
                      ),
                      Positioned(
                        top: 4,
                        right: 4,
                        child: GestureDetector(
                          onTap: () => _removePage(i),
                          child: Container(
                            width: 24,
                            height: 24,
                            decoration: const BoxDecoration(
                              color: accentRed,
                              shape: BoxShape.circle,
                            ),
                            child: const Icon(Icons.close, size: 14, color: Colors.white),
                          ),
                        ),
                      ),
                      Positioned(
                        bottom: 4,
                        left: 4,
                        child: Container(
                          padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                          decoration: BoxDecoration(
                            color: Colors.black.withAlpha(153),
                            borderRadius: BorderRadius.circular(4),
                          ),
                          child: Text(
                            'Page ${i + 1}',
                            style: const TextStyle(color: Colors.white, fontSize: 10),
                          ),
                        ),
                      ),
                    ],
                  );
                },
              ),
            ),
          ],
          const SizedBox(height: 12),
          Row(
            children: [
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _scanDocument,
                  icon: const Icon(Icons.camera_alt, size: 18),
                  label: Text(
                    _pageBytes.isEmpty ? 'Scan' : 'Scan More',
                    style: GoogleFonts.inter(fontSize: 13),
                  ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: accentIndigo,
                    side: const BorderSide(color: accentIndigo),
                  ),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                child: OutlinedButton.icon(
                  onPressed: _uploadFiles,
                  icon: const Icon(Icons.photo_library, size: 18),
                  label: Text(
                    _pageBytes.isEmpty ? 'Upload' : 'Upload More',
                    style: GoogleFonts.inter(fontSize: 13),
                  ),
                  style: OutlinedButton.styleFrom(
                    foregroundColor: accentIndigo,
                    side: const BorderSide(color: accentIndigo),
                  ),
                ),
              ),
            ],
          ),
          if (_pageBytes.length > 1) ...[
            const SizedBox(height: 8),
            Text(
              'Multiple pages added (${_pageBytes.length}). Will be saved as a merged PDF.',
              style: GoogleFonts.inter(color: accentEmerald, fontSize: 12),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildFormFields() {
    return Column(
      children: [
        _buildTextField(_titleCtrl, 'Title *', required: true),
        const SizedBox(height: 12),
        ..._getTypeSpecificFields(),
      ],
    );
  }

  List<Widget> _getTypeSpecificFields() {
    switch (widget.documentType) {
      case DocumentType.paymentCard:
        return _paymentCardFields();
      case DocumentType.aadhaarCard:
        return _aadhaarFields();
      case DocumentType.panCard:
        return _panFields();
      case DocumentType.driversLicense:
        return _dlFields();
      case DocumentType.vehicleRc:
        return _rcFields();
      case DocumentType.genericId:
        return _genericIdFields();
    }
  }

  List<Widget> _paymentCardFields() {
    return [
      _buildDropdown(
        'Card Type',
        _cardType,
        ['Visa', 'Mastercard', 'Amex', 'RuPay'],
        (v) => setState(() => _cardType = v!),
      ),
      const SizedBox(height: 12),
      _buildTextField(_cardholderCtrl, 'Cardholder Name *', required: true),
      const SizedBox(height: 12),
      _buildTextField(
        _cardNumberCtrl,
        'Card Number *',
        keyboardType: TextInputType.number,
        formatters: [CreditCardFormatter()],
        validator: (v) {
          final digits = v?.replaceAll(' ', '') ?? '';
          if (digits.isEmpty) return 'Card number is required';
          if (digits.length < 13 || digits.length > 19) return 'Card number must be 13-19 digits';
          return null;
        },
      ),
      const SizedBox(height: 12),
      Row(
        children: [
          Expanded(
            child: _buildTextField(
              _cardExpiryCtrl,
              'Expiry (MM/YY) *',
              keyboardType: TextInputType.number,
              formatters: [ExpiryDateFormatter()],
              validator: (v) {
                if (v == null || v.trim().isEmpty) return 'Expiry is required';
                final parts = v.split('/');
                if (parts.length != 2) return 'Use MM/YY format';
                final month = int.tryParse(parts[0]);
                if (month == null || month < 1 || month > 12) return 'Invalid month';
                if (parts[1].length != 2) return 'Use MM/YY format';
                return null;
              },
            ),
          ),
          const SizedBox(width: 12),
          Expanded(
            child: _buildTextField(
              _cardCvvCtrl,
              'CVV *',
              obscureText: true,
              keyboardType: TextInputType.number,
              maxLength: 4,
              validator: (v) {
                final cleaned = v?.trim() ?? '';
                if (cleaned.isEmpty) return 'CVV is required';
                if (cleaned.length < 3) return 'CVV must be at least 3 digits';
                return null;
              },
            ),
          ),
        ],
      ),
    ];
  }

  List<Widget> _aadhaarFields() {
    return [
      _buildTextField(_aadhaarNameCtrl, 'Full Name *', required: true),
      const SizedBox(height: 12),
      _buildTextField(
        _aadhaarNumberCtrl,
        'Aadhaar Number *',
        keyboardType: TextInputType.number,
        formatters: [AadhaarFormatter()],
        validator: (v) {
          final digits = v?.replaceAll(' ', '') ?? '';
          if (digits.isEmpty) return 'Aadhaar number is required';
          if (digits.length != 12) return 'Aadhaar must be 12 digits';
          return null;
        },
      ),
      const SizedBox(height: 12),
      _buildTextField(_aadhaarDobCtrl, 'Date of Birth (DD/MM/YYYY)', isDatePicker: true),
      const SizedBox(height: 12),
      _buildDropdown(
        'Gender',
        _aadhaarGender,
        ['Male', 'Female', 'Other'],
        (v) => setState(() => _aadhaarGender = v!),
      ),
    ];
  }

  List<Widget> _panFields() {
    return [
      _buildTextField(_panNameCtrl, 'Full Name *', required: true),
      const SizedBox(height: 12),
      _buildTextField(
        _panNumberCtrl,
        'PAN Number *',
        textCapitalization: TextCapitalization.characters,
        maxLength: 10,
        validator: (v) {
          final cleaned = v?.trim() ?? '';
          if (cleaned.isEmpty) return 'PAN number is required';
          if (cleaned.length != 10) return 'PAN must be 10 characters';
          if (!RegExp(r'^[A-Z]{5}[0-9]{4}[A-Z]$').hasMatch(cleaned.toUpperCase())) {
            return 'Invalid PAN format (AAAAA0000A)';
          }
          return null;
        },
      ),
      const SizedBox(height: 12),
      _buildTextField(_panFatherNameCtrl, "Father's Name"),
      const SizedBox(height: 12),
      _buildTextField(_panDobCtrl, 'Date of Birth (DD/MM/YYYY)', isDatePicker: true),
    ];
  }

  List<Widget> _dlFields() {
    return [
      _buildTextField(_dlHolderNameCtrl, 'Holder Name *', required: true),
      const SizedBox(height: 12),
      _buildTextField(_dlNumberCtrl, 'DL Number *', required: true),
      const SizedBox(height: 12),
      _buildTextField(_dlDobCtrl, 'Date of Birth', isDatePicker: true),
      const SizedBox(height: 12),
      _buildTextField(_dlExpiryCtrl, 'Expiry Date', isDatePicker: true),
      const SizedBox(height: 12),
      _buildTextField(_dlStateCtrl, 'State / RTO'),
    ];
  }

  List<Widget> _rcFields() {
    return [
      _buildTextField(_rcOwnerNameCtrl, 'Owner Name *', required: true),
      const SizedBox(height: 12),
      _buildTextField(_rcNumberCtrl, 'Registration Number *', required: true),
      const SizedBox(height: 12),
      _buildTextField(_rcChassisCtrl, 'Chassis Number'),
      const SizedBox(height: 12),
      _buildTextField(_rcEngineCtrl, 'Engine Number'),
      const SizedBox(height: 12),
      _buildTextField(_rcExpiryCtrl, 'Expiry / Valid Upto', isDatePicker: true),
    ];
  }

  List<Widget> _genericIdFields() {
    return [
      _buildTextField(_genericIdTypeCtrl, 'ID Card Type * (e.g. Employee ID)', required: true),
      const SizedBox(height: 12),
      _buildTextField(_genericIdNameCtrl, 'Name on ID *', required: true),
      const SizedBox(height: 12),
      _buildTextField(_genericIdNumberCtrl, 'ID Number *', required: true),
      const SizedBox(height: 12),
      _buildTextField(_genericIdExpiryCtrl, 'Expiry Date', isDatePicker: true),
    ];
  }

  Widget _buildTextField(
    TextEditingController ctrl,
    String label, {
    bool required = false,
    bool obscureText = false,
    TextInputType keyboardType = TextInputType.text,
    TextCapitalization textCapitalization = TextCapitalization.words,
    List<TextInputFormatter>? formatters,
    String? Function(String?)? validator,
    int? maxLength,
    bool isDatePicker = false,
  }) {
    return TextFormField(
      controller: ctrl,
      obscureText: obscureText,
      keyboardType: keyboardType,
      textCapitalization: textCapitalization,
      inputFormatters: formatters,
      maxLength: maxLength,
      readOnly: isDatePicker,
      onTap: isDatePicker ? () => _selectDate(context, ctrl) : null,
      style: GoogleFonts.inter(color: textPrimary, fontSize: 14),
      decoration: InputDecoration(
        labelText: label,
        counterText: maxLength != null ? null : '',
        suffixIcon: isDatePicker
            ? IconButton(
                icon: const Icon(Icons.calendar_today, size: 18, color: textSecondary),
                onPressed: () => _selectDate(context, ctrl),
              )
            : null,
      ),
      validator: validator ??
          (v) {
            if (required && (v == null || v.trim().isEmpty)) {
              return '${label.replaceAll(' *', '')} is required';
            }
            return null;
          },
    );
  }

  Widget _buildDropdown(
    String label,
    String value,
    List<String> items,
    ValueChanged<String?> onChanged,
  ) {
    return DropdownButtonFormField<String>(
      initialValue: value,
      items: items
          .map((e) => DropdownMenuItem(value: e, child: Text(e)))
          .toList(),
      onChanged: onChanged,
      decoration: InputDecoration(labelText: label),
      dropdownColor: cinemaElevated,
      style: GoogleFonts.inter(color: textPrimary, fontSize: 14),
    );
  }
}

/// Helper class that wraps ML Kit calls — only works on Android/iOS.
class _MlKitHelper {
  static TextRecognizer createRecognizer() {
    return TextRecognizer(script: TextRecognitionScript.latin);
  }

  static InputImage createInputImage(File file) {
    return InputImage.fromFile(file);
  }

  static Future<String?> processImage(dynamic recognizer, dynamic inputImage) async {
    if (recognizer is TextRecognizer && inputImage is InputImage) {
      final RecognizedText recognizedText = await recognizer.processImage(inputImage);
      return recognizedText.text;
    }
    return null;
  }

  static Future<void> closeRecognizer(dynamic recognizer) async {
    if (recognizer is TextRecognizer) {
      await recognizer.close();
    }
  }
}
