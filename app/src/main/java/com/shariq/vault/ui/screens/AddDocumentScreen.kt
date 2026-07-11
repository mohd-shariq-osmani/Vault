package com.shariq.vault.ui.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.shariq.vault.MainActivity
import com.shariq.vault.model.DocumentType
import com.shariq.vault.model.VaultDocument
import com.shariq.vault.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDocumentScreen(
    documentType: DocumentType,
    documentToEdit: VaultDocument? = null,
    onLoadAttachment: ((String) -> ByteArray?)? = null,
    onSave: (VaultDocument, ByteArray?, String?) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var title by remember { mutableStateOf(documentToEdit?.title ?: "") }

    // DL fields
    var dlNumber by remember { mutableStateOf(documentToEdit?.dlNumber ?: "") }
    var dlHolderName by remember { mutableStateOf(documentToEdit?.dlHolderName ?: "") }
    var dlDob by remember { mutableStateOf(documentToEdit?.dlDob ?: "") }
    var dlExpiry by remember { mutableStateOf(documentToEdit?.dlExpiry ?: "") }
    var dlState by remember { mutableStateOf(documentToEdit?.dlState ?: "") }

    // RC fields
    var rcNumber by remember { mutableStateOf(documentToEdit?.rcNumber ?: "") }
    var rcOwnerName by remember { mutableStateOf(documentToEdit?.rcOwnerName ?: "") }
    var rcChassisNumber by remember { mutableStateOf(documentToEdit?.rcChassisNumber ?: "") }
    var rcEngineNumber by remember { mutableStateOf(documentToEdit?.rcEngineNumber ?: "") }
    var rcExpiry by remember { mutableStateOf(documentToEdit?.rcExpiry ?: "") }

    // Aadhaar fields
    var aadhaarNumber by remember { mutableStateOf(documentToEdit?.aadhaarNumber ?: "") }
    var aadhaarName by remember { mutableStateOf(documentToEdit?.aadhaarName ?: "") }
    var aadhaarDob by remember { mutableStateOf(documentToEdit?.aadhaarDob ?: "") }
    var aadhaarGender by remember { mutableStateOf(documentToEdit?.aadhaarGender ?: "Male") }

    // PAN fields
    var panNumber by remember { mutableStateOf(documentToEdit?.panNumber ?: "") }
    var panName by remember { mutableStateOf(documentToEdit?.panName ?: "") }
    var panFatherName by remember { mutableStateOf(documentToEdit?.panFatherName ?: "") }
    var panDob by remember { mutableStateOf(documentToEdit?.panDob ?: "") }

    // Card fields
    var cardholderName by remember { mutableStateOf(documentToEdit?.cardholderName ?: "") }
    var cardNumber by remember { mutableStateOf(documentToEdit?.cardNumber ?: "") }
    var cardExpiry by remember {
        mutableStateOf(
            (documentToEdit?.cardExpiry ?: "").filter { it.isDigit() }
        )
    }
    var cardCvv by remember { mutableStateOf(documentToEdit?.cardCvv ?: "") }
    var cardType by remember { mutableStateOf(documentToEdit?.cardType ?: "Visa") }

    // Merged layout list representing attached pages (maximum 2 pages)
    val attachedBitmaps = remember { mutableStateListOf<Bitmap>() }
    var isPdfAttached by remember { mutableStateOf(false) }
    var pdfBytes by remember { mutableStateOf<ByteArray?>(null) }

    var ocrTextResult by remember { mutableStateOf<String?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }

    // Load existing attachments when editing (compatible with single image or multi-page PDF/back image paths)
    LaunchedEffect(documentToEdit) {
        if (documentToEdit != null && onLoadAttachment != null) {
            coroutineScope.launch(Dispatchers.IO) {
                isOcrRunning = true
                // Load Front/Merged
                documentToEdit.imagePath?.let { path ->
                    val bytes = onLoadAttachment(path)
                    if (bytes != null) {
                        if (path.endsWith(".pdf")) {
                            isPdfAttached = true
                            pdfBytes = bytes
                            val tempFile = File(context.cacheDir, "temp_edit.pdf").apply {
                                writeBytes(bytes); deleteOnExit()
                            }
                            val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                            val renderer = PdfRenderer(pfd)
                            val list = mutableListOf<Bitmap>()
                            val pageCount = minOf(renderer.pageCount, 2) // Limit to 2 pages
                            for (i in 0 until pageCount) {
                                val page = renderer.openPage(i)
                                val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                list.add(bitmap)
                                page.close()
                            }
                            renderer.close(); pfd.close(); tempFile.delete()
                            withContext(Dispatchers.Main) {
                                attachedBitmaps.clear()
                                attachedBitmaps.addAll(list)
                            }
                        } else {
                            try {
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                withContext(Dispatchers.Main) {
                                    attachedBitmaps.clear()
                                    attachedBitmaps.add(bitmap)
                                }
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                }
                // Load Back if it existed separately
                documentToEdit.backImagePath?.let { path ->
                    val bytes = onLoadAttachment(path)
                    if (bytes != null) {
                        try {
                            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            withContext(Dispatchers.Main) {
                                if (attachedBitmaps.size < 2) {
                                    attachedBitmaps.add(bitmap)
                                }
                            }
                        } catch (e: Exception) { e.printStackTrace() }
                    }
                }
                isOcrRunning = false
            }
        }
    }

    val scrollState = rememberScrollState()
    var showErrorAlert by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf("") }

    // ── HIGHLY ACCURATE OCR Auto-fill ─────────────────────────────────────────
    fun runAutoFill(text: String) {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return

        // ── Helper: clean misread characters in numeric fields ──────────────
        fun cleanNumericString(input: String): String {
            return input.uppercase()
                .replace("O", "0")
                .replace("I", "1")
                .replace("L", "1")
                .replace("S", "5")
                .replace("Z", "2")
                .replace("B", "8")
        }

        // Expanded headerKeywords to filter out document card labels and noise
        val headerKeywords = setOf(
            "INCOME", "TAX", "DEPT", "GOVT", "INDIA", "CARD", "PERMANENT",
            "ACCOUNT", "NUMBER", "SIGNATURE", "GENDER", "MALE", "FEMALE",
            "DOB", "BIRTH", "YEAR", "AUTHORITY", "UNIQUE", "IDENTIFICATION",
            "GOVERNMENT", "DEPARTMENT", "REPUBLIC", "AADHAAR", "AADHAR",
            "PAN", "MINISTRY", "COMMISSION", "ENROLLMENT", "VID", "STATE",
            "DRIVING", "LICENCE", "LICENSE", "CERTIFICATE", "REGISTRATION",
            "TRANSPORT", "OFFICE", "FATHER", "MOTHER", "SPOUSE", "HUSBAND",
            "WIFE", "GUARDIAN", "NAME", "ADDRESS", "INCOME TAX", "VALID",
            "EXPIRY", "ISSUE", "DATE", "HOLDER", "PHOTO", "SEX", "TELEPHONE",
            "MOBILE", "PHONE", "NO", "NUM", "RTO", "UNION", "TERRITORY",
            "DETAILS", "INFO", "NATIONAL", "AUTHORITY"
        )

        // Helper to extract suffix values on the same line as a label
        fun getValueFromLine(line: String, keyword: String): String? {
            val idx = line.indexOf(keyword, ignoreCase = true)
            if (idx != -1) {
                val suffix = line.substring(idx + keyword.length).trim()
                // Clean leading punctuation like ":", "-", "/", etc.
                val cleanedSuffix = suffix.replace(Regex("^[:\\-/.\\s]+"), "").trim()
                
                val hasDigits = cleanedSuffix.any { it.isDigit() }
                val alphaSpaceDots = cleanedSuffix.count { it.isLetter() || it == ' ' || it == '.' || it == '-' }
                val isMostlyAlpha = cleanedSuffix.isNotEmpty() && (alphaSpaceDots.toFloat() / cleanedSuffix.length >= 0.8f)
                val isHeaderWord = headerKeywords.any { cleanedSuffix.contains(it, ignoreCase = true) }
                
                if (!hasDigits && isMostlyAlpha && !isHeaderWord && cleanedSuffix.length >= 4) {
                    return cleanedSuffix
                }
            }
            return null
        }

        // Helper to extract a name candidate after a label index
        fun getValueAfterLabel(fromIndex: Int, maxLook: Int = 2): String? {
            for (i in (fromIndex + 1)..(fromIndex + maxLook)) {
                if (i >= lines.size) break
                val candidate = lines[i].trim()
                val hasDigits = candidate.any { it.isDigit() }
                
                val alphaSpaceDots = candidate.count { it.isLetter() || it == ' ' || it == '.' || it == '-' }
                val isMostlyAlpha = candidate.isNotEmpty() && (alphaSpaceDots.toFloat() / candidate.length >= 0.8f)
                val isHeaderWord = headerKeywords.any { candidate.contains(it, ignoreCase = true) }

                if (!hasDigits && isMostlyAlpha && !isHeaderWord && candidate.length >= 4) {
                    return candidate
                }
            }
            return null
        }

        // Helper to search both suffix and next-lines for a label keyword
        fun findValueForLabel(keyword: String): String? {
            for (i in lines.indices) {
                if (lines[i].contains(keyword, ignoreCase = true)) {
                    val sameLine = getValueFromLine(lines[i], keyword)
                    if (sameLine != null) return sameLine
                    val nextLines = getValueAfterLabel(i)
                    if (nextLines != null) return nextLines
                }
            }
            return null
        }

        // ── Step 2: gather name candidates from all lines ────────────────────
        val nameCandidates = lines.map { it.trim() }.filter { line ->
            val hasDigits = line.any { it.isDigit() }
            val isHeader = headerKeywords.any { line.contains(it, ignoreCase = true) }
            val alphaSpaceDots = line.count { it.isLetter() || it == ' ' || it == '.' || it == '-' }
            val isMostlyAlpha = line.isNotEmpty() && (alphaSpaceDots.toFloat() / line.length >= 0.8f)
            
            !hasDigits && !isHeader && isMostlyAlpha && line.length >= 4
        }

        val dateRegex = Regex("\\b\\d{2}\\s*[/.\\-]\\s*\\d{2}\\s*[/.\\-]\\s*\\d{4}\\b")

        when (documentType) {
            DocumentType.PAYMENT_CARD -> {
                val cardRegex = Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b|\\b\\d{13,16}\\b")
                cardRegex.find(text)?.value?.let {
                    val cleaned = it.replace(" ", "").replace("-", "")
                    cardNumber = cleaned.chunked(4).joinToString(" ")
                    cardType = when (cleaned.firstOrNull()) {
                        '4' -> "Visa"; '5' -> "Mastercard"; '3' -> "Amex"; '6' -> "RuPay"; else -> "Visa"
                    }
                }
                Regex("\\b(0[1-9]|1[0-2])/(\\d{2})\\b").find(text)?.value?.let { cardExpiry = it }
                
                val multiWordCandidates = nameCandidates.filter { it.split(" ").size >= 2 }
                if (multiWordCandidates.isNotEmpty()) cardholderName = multiWordCandidates.first()
                else if (nameCandidates.isNotEmpty()) cardholderName = nameCandidates.first()
            }

            DocumentType.AADHAAR_CARD -> {
                val cleanedTextForAadhaar = cleanNumericString(text)
                Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b").find(cleanedTextForAadhaar)?.value?.let {
                    aadhaarNumber = it.replace(" ", "").replace("-", "").chunked(4).joinToString(" ")
                }

                if (text.contains("Female", ignoreCase = true)) aadhaarGender = "Female"
                else if (text.contains("Male", ignoreCase = true)) aadhaarGender = "Male"

                dateRegex.find(text)?.value?.let { aadhaarDob = it.replace(" ", "").replace(".", "-").replace("/", "-") }
                    ?: Regex("(?:Year\\s*of\\s*Birth|YOB)\\s*[:\\-]?\\s*(\\d{4})", RegexOption.IGNORE_CASE).find(text)?.groupValues?.getOrNull(1)?.let { aadhaarDob = it }
                    ?: Regex("\\b(19|20)\\d{2}\\b").find(cleanNumericString(text))?.value?.let { aadhaarDob = it }

                // Aadhaar Name: Scan upwards from the DOB/YOB line if found
                var foundNameUpwards = false
                var dobIndex = -1
                for (i in lines.indices) {
                    if (lines[i].contains("DOB", ignoreCase = true) || 
                        lines[i].contains("Birth", ignoreCase = true) || 
                        lines[i].contains("YOB", ignoreCase = true) ||
                        dateRegex.containsMatchIn(lines[i])) {
                        dobIndex = i
                        break
                    }
                }
                if (dobIndex > 0) {
                    for (i in (dobIndex - 1) downTo maxOf(0, dobIndex - 3)) {
                        val candidate = lines[i].trim()
                        val hasDigits = candidate.any { it.isDigit() }
                        val isHeader = headerKeywords.any { candidate.contains(it, ignoreCase = true) }
                        val alphaSpaceDots = candidate.count { it.isLetter() || it == ' ' || it == '.' || it == '-' }
                        val isMostlyAlpha = candidate.isNotEmpty() && (alphaSpaceDots.toFloat() / candidate.length >= 0.8f)
                        
                        if (!hasDigits && !isHeader && isMostlyAlpha && candidate.length >= 4) {
                            aadhaarName = candidate
                            foundNameUpwards = true
                            break
                        }
                    }
                }

                if (!foundNameUpwards) {
                    val nameValue = findValueForLabel("NAME")
                    if (nameValue != null) {
                        aadhaarName = nameValue
                    } else {
                        val multiWord = nameCandidates.filter { it.split(" ").size >= 2 }
                        if (multiWord.isNotEmpty()) aadhaarName = multiWord.first()
                        else if (nameCandidates.isNotEmpty()) aadhaarName = nameCandidates.first()
                    }
                }
            }

            DocumentType.PAN_CARD -> {
                val panRawMatch = Regex("\\b[A-Z0-9]{5}[0-9A-Z]{4}[A-Z0-9]\\b", RegexOption.IGNORE_CASE).find(text)
                panRawMatch?.value?.let { raw ->
                    val sb = java.lang.StringBuilder()
                    val upper = raw.uppercase()
                    for (i in 0 until 5) {
                        val c = upper[i]
                        sb.append(if (c.isDigit()) when(c) { '0'->'O'; '1'->'I'; '5'->'S'; '2'->'Z'; '8'->'B'; else->c } else c)
                    }
                    for (i in 5 until 9) {
                        val c = upper[i]
                        sb.append(if (c.isLetter()) when(c) { 'O'->'0'; 'I'->'1'; 'L'->'1'; 'S'->'5'; 'Z'->'2'; 'B'->'8'; else->c } else c)
                    }
                    val last = upper[9]
                    sb.append(if (last.isDigit()) when(last) { '0'->'O'; '1'->'I'; '5'->'S'; '2'->'Z'; '8'->'B'; else->last } else last)
                    panNumber = sb.toString()
                }

                dateRegex.find(text)?.value?.let { panDob = it.replace(" ", "").replace(".", "-").replace("/", "-") }

                val nameVal = findValueForLabel("NAME")
                if (nameVal != null) panName = nameVal

                val fatherVal = findValueForLabel("FATHER") ?: findValueForLabel("S/O") ?: findValueForLabel("D/O") ?: findValueForLabel("W/O")
                if (fatherVal != null) panFatherName = fatherVal

                val multiWord = nameCandidates.filter { it.split(" ").size >= 2 }
                if (panName.isEmpty()) {
                    multiWord.getOrNull(0)?.let { panName = it }
                    if (panFatherName.isEmpty()) {
                        multiWord.getOrNull(1)?.let { panFatherName = it }
                    }
                } else if (panFatherName.isEmpty()) {
                    multiWord.find { it != panName }?.let { panFatherName = it }
                }
            }

            DocumentType.DRIVERS_LICENSE -> {
                val dlRawMatch = Regex("\\b([A-Z]{2})[\\s-]?([0-9A-Z]{2})[\\s-]?([0-9A-Z]{4})[\\s-]?([0-9A-Z]{7})\\b", RegexOption.IGNORE_CASE).find(text)
                dlRawMatch?.let { match ->
                    val state = match.groupValues[1].uppercase()
                    val rto = cleanNumericString(match.groupValues[2])
                    val year = cleanNumericString(match.groupValues[3])
                    val unique = cleanNumericString(match.groupValues[4])
                    dlNumber = "$state$rto$year$unique"
                } ?: run {
                    Regex("\\b[A-Z0-9]{11,16}\\b", RegexOption.IGNORE_CASE).find(text)?.value?.let {
                        dlNumber = it.uppercase().replace(" ", "").replace("-", "")
                    }
                }

                val dates = dateRegex.findAll(text).toList()
                if (dates.isNotEmpty()) {
                    dlDob = dates.first().value.replace(" ", "").replace(".", "-").replace("/", "-")
                    if (dates.size > 1) {
                        dlExpiry = dates.last().value.replace(" ", "").replace(".", "-").replace("/", "-")
                    }
                }

                val holderVal = findValueForLabel("NAME") ?: findValueForLabel("HOLDER")
                if (holderVal != null) dlHolderName = holderVal

                if (dlHolderName.isEmpty()) {
                    val multiWord = nameCandidates.filter { it.split(" ").size >= 2 }
                    if (multiWord.isNotEmpty()) dlHolderName = multiWord.first()
                }

                if (dlNumber.length >= 2) dlState = dlNumber.take(2)
            }

            DocumentType.VEHICLE_RC -> {
                val rcRawMatch = Regex("\\b([A-Z]{2})[\\s-]?([0-9A-Z]{2})[\\s-]?([A-Z]{1,3})[\\s-]?([0-9A-Z]{4})\\b", RegexOption.IGNORE_CASE).find(text)
                rcRawMatch?.let { match ->
                    val state = match.groupValues[1].uppercase()
                    val rto = cleanNumericString(match.groupValues[2])
                    val code = match.groupValues[3].uppercase()
                    val unique = cleanNumericString(match.groupValues[4])
                    rcNumber = "$state$rto$code$unique"
                }

                val dates = dateRegex.findAll(text).toList()
                if (dates.isNotEmpty()) {
                    rcExpiry = dates.last().value.replace(" ", "").replace(".", "-").replace("/", "-")
                }

                val ownerVal = findValueForLabel("NAME") ?: findValueForLabel("HOLDER") ?: findValueForLabel("OWNER")
                if (ownerVal != null) rcOwnerName = ownerVal

                if (rcOwnerName.isEmpty()) {
                    val multiWord = nameCandidates.filter { it.split(" ").size >= 2 }
                    if (multiWord.isNotEmpty()) rcOwnerName = multiWord.first()
                }
            }
        }
    }

    // Runs OCR on all attached bitmaps in order, aggregates text, and triggers auto-fill
    fun runOcrOnBitmaps(bitmaps: List<Bitmap>) {
        isOcrRunning = true
        coroutineScope.launch(Dispatchers.Default) {
            try {
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val sb = java.lang.StringBuilder()
                for (bitmap in bitmaps) {
                    val image = InputImage.fromBitmap(bitmap, 0)
                    val visionText = Tasks.await(recognizer.process(image))
                    sb.append(visionText.text).append("\n")
                }
                val compiledText = sb.toString()
                withContext(Dispatchers.Main) {
                    ocrTextResult = compiledText.trim()
                    runAutoFill(compiledText)
                    isOcrRunning = false
                    Toast.makeText(context, "OCR details filled successfully!", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    isOcrRunning = false
                    Toast.makeText(context, "OCR scanning failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // ── Image / PDF Processing Handlers ─────────────────────────────────────
    fun processImageUri(uri: Uri) {
        try {
            isOcrRunning = true
            isPdfAttached = false
            pdfBytes = null

            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            }

            attachedBitmaps.add(bitmap)
            runOcrOnBitmaps(attachedBitmaps)
        } catch (e: Exception) {
            isOcrRunning = false
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun processPdfUri(uri: Uri) {
        isOcrRunning = true
        isPdfAttached = true
        coroutineScope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }
                pdfBytes = bytes

                val bitmaps = withContext(Dispatchers.IO) {
                    val tempFile = File(context.cacheDir, "temp_ocr.pdf").apply {
                        writeBytes(bytes!!)
                        deleteOnExit()
                    }
                    val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                    val renderer = PdfRenderer(pfd)
                    val list = mutableListOf<Bitmap>()
                    val pageCount = minOf(renderer.pageCount, 2) // Limit to first 2 pages
                    for (i in 0 until pageCount) {
                        val page = renderer.openPage(i)
                        val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                        list.add(bitmap)
                        page.close()
                    }
                    renderer.close(); pfd.close(); tempFile.delete()
                    list
                }

                attachedBitmaps.clear()
                attachedBitmaps.addAll(bitmaps)
                runOcrOnBitmaps(bitmaps)
            } catch (e: Exception) {
                isOcrRunning = false
                isPdfAttached = false
                e.printStackTrace()
                Toast.makeText(context, "PDF processing failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ── Launchers ─────────────────────────────────────────────────────────────
    val galleryLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processImageUri(it) }
    }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { processPdfUri(it) }
    }
    val scannerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanResult?.pdf?.uri?.let { processPdfUri(it) }
        }
    }

    fun launchScanner() {
        try {
            MainActivity.isLaunchingSystemIntent = true
            val options = com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.Builder()
                .setGalleryImportAllowed(false)
                .setPageLimit(2) // Allow scanning maximum 2 pages inside a single scan!
                .setResultFormats(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.RESULT_FORMAT_PDF)
                .setScannerMode(com.google.mlkit.vision.documentscanner.GmsDocumentScannerOptions.SCANNER_MODE_FULL)
                .build()
            val scannerClient = com.google.mlkit.vision.documentscanner.GmsDocumentScanning.getClient(options)
            val activity = context as? androidx.fragment.app.FragmentActivity
            if (activity != null) {
                scannerClient.getStartScanIntent(activity)
                    .addOnSuccessListener { intentSender ->
                        scannerLauncher.launch(androidx.activity.result.IntentSenderRequest.Builder(intentSender).build())
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        Toast.makeText(context, "Scanner failed", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Combines separate pages into a single PDF document file bytes
    fun createPdfFromBitmaps(bitmaps: List<Bitmap>): ByteArray {
        val pdfDocument = android.graphics.pdf.PdfDocument()
        bitmaps.forEachIndexed { index, bitmap ->
            val pageInfo = android.graphics.pdf.PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, index + 1).create()
            val page = pdfDocument.startPage(pageInfo)
            page.canvas.drawBitmap(bitmap, 0f, 0f, null)
            pdfDocument.finishPage(page)
        }
        val stream = java.io.ByteArrayOutputStream()
        pdfDocument.writeTo(stream)
        pdfDocument.close()
        return stream.toByteArray()
    }

    // ── UI ────────────────────────────────────────────────────────────────────
    val isEditing = documentToEdit != null
    val typeLabel = documentTypeToLabel(documentType)
    val accentColor = when (documentType) {
        DocumentType.PAYMENT_CARD    -> AccentIndigo
        DocumentType.AADHAAR_CARD    -> AccentEmerald
        DocumentType.PAN_CARD        -> Color(0xFFFF9F00)
        DocumentType.DRIVERS_LICENSE -> AccentIndigo
        DocumentType.VEHICLE_RC      -> Color(0xFF9D6FFF)
    }

    Scaffold(containerColor = CinemaBase) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaBase)
                .padding(paddingValues)
                .verticalScroll(scrollState)
        ) {
            // ── Header ───────────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(colors = listOf(CinemaElevated, CinemaBase)))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CinemaSurface)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.15f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(typeLabel.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = accentColor)
                        }
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = if (isEditing) "Edit $typeLabel" else "Add $typeLabel",
                            fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                Spacer(modifier = Modifier.height(16.dp))

                // ── ATTACHMENT AREA ──────────────────────────────────────────
                SectionHeader("Document scan (Merge Front & Back)", accentColor)
                Spacer(modifier = Modifier.height(10.dp))

                if (isOcrRunning) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(CinemaElevated)
                            .border(0.8.dp, CinemaStroke, RoundedCornerShape(14.dp))
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(color = accentColor, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Reading & running offline OCR…", color = TextSecondary, fontSize = 13.sp)
                    }
                } else if (attachedBitmaps.isNotEmpty()) {
                    // Render current page previews
                    Column {
                        attachedBitmaps.forEachIndexed { index, bitmap ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(CinemaElevated)
                                    .border(0.8.dp, CinemaStroke, RoundedCornerShape(14.dp))
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier.size(58.dp).clip(RoundedCornerShape(10.dp)).background(CinemaSurface),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isPdfAttached) {
                                        Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFFF4444), modifier = Modifier.size(28.dp))
                                    } else {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (index == 0) "Front Side (Page 1)" else "Back Side (Page 2)",
                                        color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                                    )
                                    Text("Ready for merge", color = AccentEmerald, fontSize = 11.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        // Remove attachment controls
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            TextButton(
                                onClick = {
                                    attachedBitmaps.clear()
                                    isPdfAttached = false
                                    pdfBytes = null
                                    ocrTextResult = null
                                },
                                colors = ButtonDefaults.textButtonColors(contentColor = AccentRed)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Remove all pages", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    // Empty state upload container
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(85.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CinemaElevated)
                            .border(0.8.dp, CinemaStroke, RoundedCornerShape(14.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = TextMuted, modifier = Modifier.size(24.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("No scan or file attached", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Upload controls
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    val label = if (attachedBitmaps.isEmpty()) "Scan Document" else "Add Back Scan"
                    val galleryLabel = if (attachedBitmaps.isEmpty()) "Gallery Photo" else "Add Back Photo"
                    
                    if (attachedBitmaps.size < 2 && !isPdfAttached) {
                        OutlinedButton(
                            onClick = { launchScanner() },
                            modifier = Modifier.weight(1.2f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }

                        OutlinedButton(
                            onClick = {
                                MainActivity.isLaunchingSystemIntent = true
                                galleryLauncher.launch("image/*")
                            },
                            modifier = Modifier.weight(1.2f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(galleryLabel, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    if (attachedBitmaps.isEmpty()) {
                        OutlinedButton(
                            onClick = {
                                MainActivity.isLaunchingSystemIntent = true
                                pdfLauncher.launch("application/pdf")
                            },
                            modifier = Modifier.weight(0.8f).height(40.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("PDF File", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))

                // ── Document Details Section ──────────────────────────────
                SectionHeader("Document Details", accentColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Title field
                VaultTextField(value = title, onValueChange = { title = it }, label = "Friendly Title / Alias", accentColor = accentColor)
                Spacer(modifier = Modifier.height(12.dp))

                // Type-specific fields
                when (documentType) {
                    DocumentType.PAYMENT_CARD -> {
                        VaultTextField(
                            value = cardNumber,
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                if (cleaned.length <= 16) {
                                    cardNumber = cleaned
                                    cardType = when (cleaned.firstOrNull()) {
                                        '4' -> "Visa"; '5' -> "Mastercard"; '3' -> "Amex"; '6' -> "RuPay"; else -> "Visa"
                                    }
                                }
                            },
                            label = "Card Number",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            accentColor = accentColor,
                            visualTransformation = CreditCardFilter()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = cardholderName, onValueChange = { cardholderName = it }, label = "Cardholder Name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VaultTextField(
                                value = cardExpiry,
                                onValueChange = { input ->
                                    val cleaned = input.filter { it.isDigit() }
                                    if (cleaned.length <= 4) {
                                        cardExpiry = cleaned
                                    }
                                },
                                label = "Expiry (MM/YY)",
                                placeholder = "MM/YY",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                accentColor = accentColor,
                                visualTransformation = ExpiryDateFilter()
                            )
                            VaultTextField(
                                value = cardCvv,
                                onValueChange = { if (it.length <= 4 && it.all { c -> c.isDigit() }) cardCvv = it },
                                label = "CVV",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                                modifier = Modifier.weight(1f),
                                accentColor = accentColor
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        var networkExpanded by remember { mutableStateOf(false) }
                        Box {
                            VaultTextField(value = cardType, onValueChange = {}, label = "Card Network", readOnly = true,
                                trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary) },
                                accentColor = accentColor
                            )
                            Box(modifier = Modifier.matchParentSize().clickable { networkExpanded = true })
                            DropdownMenu(expanded = networkExpanded, onDismissRequest = { networkExpanded = false },
                                modifier = Modifier.background(CinemaElevated)) {
                                listOf("Visa", "Mastercard", "RuPay", "Amex").forEach { n ->
                                    DropdownMenuItem(text = { Text(n, color = TextPrimary) }, onClick = { cardType = n; networkExpanded = false })
                                }
                            }
                        }
                    }

                    DocumentType.AADHAAR_CARD -> {
                        VaultTextField(
                            value = aadhaarNumber,
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                if (cleaned.length <= 12) {
                                    aadhaarNumber = cleaned
                                }
                            },
                            label = "Aadhaar Number",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            accentColor = accentColor,
                            visualTransformation = AadhaarFilter()
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = aadhaarName, onValueChange = { aadhaarName = it }, label = "Full Name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VaultTextField(value = aadhaarDob, onValueChange = { aadhaarDob = it }, label = "DOB / Year",
                                placeholder = "DD-MM-YYYY", modifier = Modifier.weight(1f), accentColor = accentColor)
                            var genderExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.weight(1f)) {
                                VaultTextField(value = aadhaarGender, onValueChange = {}, label = "Gender", readOnly = true,
                                    trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = TextSecondary) },
                                    accentColor = accentColor
                                )
                                Box(modifier = Modifier.matchParentSize().clickable { genderExpanded = true })
                                DropdownMenu(expanded = genderExpanded, onDismissRequest = { genderExpanded = false },
                                    modifier = Modifier.background(CinemaElevated)) {
                                    listOf("Male", "Female", "Other").forEach { g ->
                                        DropdownMenuItem(text = { Text(g, color = TextPrimary) }, onClick = { aadhaarGender = g; genderExpanded = false })
                                    }
                                }
                            }
                        }
                    }

                    DocumentType.PAN_CARD -> {
                        VaultTextField(value = panNumber, onValueChange = { if (it.length <= 10) panNumber = it.uppercase() }, label = "PAN Number", accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = panName, onValueChange = { panName = it }, label = "Full Name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = panFatherName, onValueChange = { panFatherName = it }, label = "Father's Name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = panDob, onValueChange = { panDob = it }, label = "Date of Birth", placeholder = "DD-MM-YYYY", accentColor = accentColor)
                    }

                    DocumentType.DRIVERS_LICENSE -> {
                        VaultTextField(value = dlNumber, onValueChange = { dlNumber = it.uppercase() }, label = "Licence Number", accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = dlHolderName, onValueChange = { dlHolderName = it }, label = "Holder Name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VaultTextField(value = dlDob, onValueChange = { dlDob = it }, label = "DOB", modifier = Modifier.weight(1f), accentColor = accentColor)
                            VaultTextField(value = dlExpiry, onValueChange = { dlExpiry = it }, label = "Expiry", modifier = Modifier.weight(1f), accentColor = accentColor)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = dlState, onValueChange = { dlState = it }, label = "Issuing State", accentColor = accentColor)
                    }

                    DocumentType.VEHICLE_RC -> {
                        VaultTextField(value = rcNumber, onValueChange = { rcNumber = it.uppercase() }, label = "Registration Number", accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = rcOwnerName, onValueChange = { rcOwnerName = it }, label = "Owner Name",
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words), accentColor = accentColor)
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            VaultTextField(value = rcChassisNumber, onValueChange = { rcChassisNumber = it.uppercase() }, label = "Chassis No.", modifier = Modifier.weight(1f), accentColor = accentColor)
                            VaultTextField(value = rcEngineNumber, onValueChange = { rcEngineNumber = it.uppercase() }, label = "Engine No.", modifier = Modifier.weight(1f), accentColor = accentColor)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        VaultTextField(value = rcExpiry, onValueChange = { rcExpiry = it }, label = "RC Expiry Date", accentColor = accentColor)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // ── Save Button ───────────────────────────────────────────
                Button(
                    onClick = {
                        val error = validateInputs(
                            documentType, title, dlNumber, dlHolderName,
                            rcNumber, rcOwnerName, aadhaarNumber, aadhaarName,
                            panNumber, panName, cardNumber, cardholderName, cardExpiry, cardCvv
                        )
                        if (error != null) {
                            validationErrorMessage = error
                            showErrorAlert = true
                        } else {
                            val extension = when {
                                isPdfAttached || attachedBitmaps.size > 1 -> "pdf"
                                attachedBitmaps.size == 1 -> "png"
                                else -> null
                            }
                            val finalBytes = when {
                                isPdfAttached -> pdfBytes
                                attachedBitmaps.size > 1 -> createPdfFromBitmaps(attachedBitmaps)
                                attachedBitmaps.size == 1 -> {
                                    val stream = java.io.ByteArrayOutputStream()
                                    attachedBitmaps.first().compress(Bitmap.CompressFormat.PNG, 100, stream)
                                    stream.toByteArray()
                                }
                                else -> null
                            }
                            val doc = VaultDocument(
                                id = documentToEdit?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                type = documentType,
                                dateAdded = documentToEdit?.dateAdded ?: System.currentTimeMillis(),
                                cardColorIndex = documentToEdit?.cardColorIndex ?: java.util.Random().nextInt(5),
                                dlNumber = dlNumber.trim(), dlHolderName = dlHolderName.trim(),
                                dlDob = dlDob.trim(), dlExpiry = dlExpiry.trim(), dlState = dlState.trim(),
                                rcNumber = rcNumber.trim(), rcOwnerName = rcOwnerName.trim(),
                                rcChassisNumber = rcChassisNumber.trim(), rcEngineNumber = rcEngineNumber.trim(), rcExpiry = rcExpiry.trim(),
                                aadhaarNumber = aadhaarNumber.replace(" ", ""),
                                aadhaarName = aadhaarName.trim(), aadhaarDob = aadhaarDob.trim(), aadhaarGender = aadhaarGender,
                                panNumber = panNumber.trim(), panName = panName.trim(),
                                panFatherName = panFatherName.trim(), panDob = panDob.trim(),
                                cardNumber = cardNumber.replace(" ", ""), cardholderName = cardholderName.trim(),
                                cardExpiry = if (cardExpiry.length == 4) cardExpiry.take(2) + "/" + cardExpiry.drop(2) else cardExpiry.trim(),
                                cardCvv = cardCvv.trim(), cardType = cardType,
                                ocrText = ocrTextResult ?: documentToEdit?.ocrText
                            )
                            onSave(doc, finalBytes, extension)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor, contentColor = Color.White)
                ) {
                    Icon(
                        imageVector = if (isEditing) Icons.Default.Check else Icons.Default.Lock,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (isEditing) "Update Document" else "Save Encrypted",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        letterSpacing = 0.5.sp
                    )
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { showErrorAlert = false },
            title = { Text("Validation Error", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text(validationErrorMessage, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showErrorAlert = false }) {
                    Text("OK", color = AccentIndigo, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = CinemaElevated,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

// ── Shared UI Components ─────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String, accentColor: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
        Spacer(modifier = Modifier.width(8.dp))
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = 0.3.sp)
    }
}

@Composable
private fun VaultTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    modifier: Modifier = Modifier.fillMaxWidth(),
    readOnly: Boolean = false,
    trailingIcon: @Composable (() -> Unit)? = null,
    accentColor: Color = AccentIndigo,
    visualTransformation: VisualTransformation = VisualTransformation.None
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = if (placeholder.isNotEmpty()) ({ Text(placeholder, color = TextMuted, fontSize = 13.sp) }) else null,
        singleLine = true,
        readOnly = readOnly,
        trailingIcon = trailingIcon,
        keyboardOptions = keyboardOptions,
        visualTransformation = visualTransformation,
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = accentColor,
            unfocusedBorderColor = CinemaStroke,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = TextSecondary,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            focusedContainerColor = CinemaSurface,
            unfocusedContainerColor = CinemaSurface,
            cursorColor = accentColor
        )
    )
}

private fun documentTypeToLabel(type: DocumentType): String = when (type) {
    DocumentType.PAYMENT_CARD    -> "Payment Card"
    DocumentType.AADHAAR_CARD    -> "Aadhaar Card"
    DocumentType.PAN_CARD        -> "PAN Card"
    DocumentType.DRIVERS_LICENSE -> "Driver's Licence"
    DocumentType.VEHICLE_RC      -> "Vehicle RC"
}

private fun validateInputs(
    type: DocumentType, title: String,
    dlNum: String, dlName: String,
    rcNum: String, rcName: String,
    adNum: String, adName: String,
    panNum: String, panName: String,
    cardNum: String, cardName: String, cardExp: String, cardCvv: String
): String? {
    if (title.trim().isEmpty()) return "Please enter a title for this document."
    when (type) {
        DocumentType.PAYMENT_CARD -> {
            val cleaned = cardNum.replace(" ", "")
            if (cleaned.length < 13 || cleaned.length > 19) return "Card number must be 13-19 digits."
            if (cardName.trim().isEmpty()) return "Cardholder name is required."
            val expDigits = cardExp.filter { it.isDigit() }
            if (expDigits.length != 4) return "Expiry must be MM/YY format."
            val month = expDigits.take(2).toIntOrNull() ?: 0
            if (month < 1 || month > 12) return "Expiry month must be between 01 and 12."
            if (cardCvv.length < 3) return "CVV must be at least 3 digits."
        }
        DocumentType.AADHAAR_CARD -> {
            if (adNum.replace(" ", "").length != 12) return "Aadhaar number must be exactly 12 digits."
            if (adName.trim().isEmpty()) return "Full name is required."
        }
        DocumentType.PAN_CARD -> {
            if (panNum.trim().length != 10) return "PAN number must be exactly 10 characters."
            if (panName.trim().isEmpty()) return "Full name is required."
        }
        DocumentType.DRIVERS_LICENSE -> {
            if (dlNum.trim().isEmpty()) return "Licence number is required."
            if (dlName.trim().isEmpty()) return "Holder name is required."
        }
        DocumentType.VEHICLE_RC -> {
            if (rcNum.trim().isEmpty()) return "Registration number is required."
            if (rcName.trim().isEmpty()) return "Owner name is required."
        }
    }
    return null
}

class AadhaarFilter : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != trimmed.lastIndex) {
                out += " "
            }
        }
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return offset
                val spaces = (offset - 1) / 4
                return offset + spaces
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return offset
                val spaces = offset / 5
                return offset - spaces
            }
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

class CreditCardFilter : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i % 4 == 3 && i != trimmed.lastIndex) {
                out += " "
            }
        }
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return offset
                val spaces = (offset - 1) / 4
                return offset + spaces
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return offset
                val spaces = offset / 5
                return offset - spaces
            }
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}

class ExpiryDateFilter : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val trimmed = text.text
        var out = ""
        for (i in trimmed.indices) {
            out += trimmed[i]
            if (i == 1 && i != trimmed.lastIndex) {
                out += "/"
            }
        }
        val offsetTranslator = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 2) return offset
                return offset + 1
            }
            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 2) return offset
                return offset - 1
            }
        }
        return TransformedText(AnnotatedString(out), offsetTranslator)
    }
}
