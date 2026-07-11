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
    var aadhaarNumber by remember {
        mutableStateOf(
            if (documentToEdit?.aadhaarNumber != null) documentToEdit.aadhaarNumber.chunked(4).joinToString(" ")
            else ""
        )
    }
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
    var cardNumber by remember {
        mutableStateOf(
            if (documentToEdit?.cardNumber != null) documentToEdit.cardNumber.chunked(4).joinToString(" ")
            else ""
        )
    }
    var cardExpiry by remember { mutableStateOf(documentToEdit?.cardExpiry ?: "") }
    var cardCvv by remember { mutableStateOf(documentToEdit?.cardCvv ?: "") }
    var cardType by remember { mutableStateOf(documentToEdit?.cardType ?: "Visa") }

    // Attachment State
    var selectedImageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isPdfAttached by remember { mutableStateOf(false) }
    var ocrTextResult by remember { mutableStateOf<String?>(null) }
    var isOcrRunning by remember { mutableStateOf(false) }
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

    // Load existing attachment when editing
    LaunchedEffect(documentToEdit) {
        if (documentToEdit != null && onLoadAttachment != null) {
            documentToEdit.imagePath?.let { path ->
                coroutineScope.launch(Dispatchers.IO) {
                    isOcrRunning = true
                    val bytes = onLoadAttachment(path)
                    if (bytes != null) {
                        selectedImageBytes = bytes
                        if (path.endsWith(".pdf")) {
                            isPdfAttached = true
                        } else {
                            try {
                                selectedImageBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                            } catch (e: Exception) { e.printStackTrace() }
                        }
                    }
                    isOcrRunning = false
                }
            }
        }
    }

    val scrollState = rememberScrollState()
    var showErrorAlert by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf("") }

    // ── IMPROVED OCR Auto-fill ────────────────────────────────────────────────
    fun runAutoFill(text: String) {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return

        // ── Step 1: structured label→value scanning ─────────────────────────
        // Build a map of which line index holds which label
        val labelMap = mutableMapOf<String, Int>() // label keyword → line index
        val labelKeywords = listOf(
            "NAME", "FATHER", "MOTHER", "S/O", "D/O", "W/O",
            "DOB", "DATE OF BIRTH", "YEAR OF BIRTH", "VALIDITY",
            "VALID", "EXPIRY", "CARDHOLDER", "HOLDER"
        )
        lines.forEachIndexed { index, line ->
            labelKeywords.forEach { keyword ->
                if (line.contains(keyword, ignoreCase = true)) {
                    labelMap[keyword] = index
                }
            }
        }

        // Extracts the best non-label non-number text from the next N lines after `fromIndex`
        fun getValueAfterLabel(fromIndex: Int, maxLook: Int = 2): String? {
            for (i in (fromIndex + 1)..(fromIndex + maxLook)) {
                if (i >= lines.size) break
                val candidate = lines[i].trim()
                // Must be primarily alphabetic (allow spaces/hyphens/dots), at least 2 words or 5 chars
                val cleaned = candidate.replace("-", " ").replace(".", " ").trim()
                val words = cleaned.split(" ").filter { it.isNotEmpty() }
                val isAlphaWords = words.all { w -> w.all { it.isLetter() } }
                if (isAlphaWords && cleaned.length >= 3) return candidate
            }
            return null
        }

        // ── Step 2: gather name candidates from all lines ────────────────────
        // A valid name candidate: ≥2 words, all alphabetic, not a header keyword
        val headerKeywords = setOf(
            "INCOME", "TAX", "DEPT", "GOVT", "INDIA", "CARD", "PERMANENT",
            "ACCOUNT", "NUMBER", "SIGNATURE", "GENDER", "MALE", "FEMALE",
            "DOB", "BIRTH", "YEAR", "AUTHORITY", "UNIQUE", "IDENTIFICATION",
            "GOVERNMENT", "DEPARTMENT", "REPUBLIC", "AADHAAR", "AADHAR",
            "PAN", "MINISTRY", "COMMISSION", "ENROLLMENT", "VID"
        )

        val nameCandidates = lines.filter { line ->
            // Allow spaces and hyphens, require all tokens to be alphabetic
            val words = line.trim().split("\\s+".toRegex()).filter { it.isNotEmpty() }
            val allAlpha = words.isNotEmpty() && words.all { w -> w.replace("-", "").all { it.isLetter() } }
            val notHeader = words.none { w -> w.uppercase() in headerKeywords }
            val longEnough = line.trim().length >= 4
            // Must have at least 2 words OR be clearly a single-name-only document
            allAlpha && notHeader && longEnough
        }

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
                // Aadhaar number: 12 digits in groups of 4
                Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b").find(text)?.value?.let {
                    aadhaarNumber = it.replace(" ", "").replace("-", "").chunked(4).joinToString(" ")
                }

                // Gender
                if (text.contains("Female", ignoreCase = true)) aadhaarGender = "Female"
                else if (text.contains("Male", ignoreCase = true)) aadhaarGender = "Male"

                // DOB: handle DD/MM/YYYY, DD-MM-YYYY, and "Year of Birth: YYYY"
                val fullDobRegex = Regex("\\b\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}\\b")
                val yobRegex = Regex("(?:Year\\s*of\\s*Birth|YOB)\\s*[:\\-]?\\s*(\\d{4})", RegexOption.IGNORE_CASE)
                val yobSimple = Regex("\\b(19|20)\\d{2}\\b")
                fullDobRegex.find(text)?.value?.let { aadhaarDob = it }
                    ?: yobRegex.find(text)?.groupValues?.getOrNull(1)?.let { aadhaarDob = it }
                    ?: yobSimple.find(text)?.value?.let { aadhaarDob = it }

                // Name: prefer structured NAME label → next alpha line
                val nameIdx = labelMap["NAME"]
                if (nameIdx != null) {
                    getValueAfterLabel(nameIdx)?.let { aadhaarName = it }
                } else {
                    val multiWordCandidates = nameCandidates.filter { it.split(" ").size >= 2 }
                    if (multiWordCandidates.isNotEmpty()) aadhaarName = multiWordCandidates.first()
                    else if (nameCandidates.isNotEmpty()) aadhaarName = nameCandidates.first()
                }
            }

            DocumentType.PAN_CARD -> {
                // PAN: 5 letters + 4 digits + 1 letter
                Regex("[A-Z]{5}[0-9]{4}[A-Z]").find(text.uppercase())?.value?.let { panNumber = it }

                // DOB
                Regex("\\b\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}\\b").find(text)?.value?.let { panDob = it }

                // Name and Father's name via structured label scanning
                val nameIdx = labelMap["NAME"]
                val fatherIdx = labelMap["FATHER"] ?: labelMap["S/O"] ?: labelMap["D/O"] ?: labelMap["W/O"]

                if (nameIdx != null) {
                    getValueAfterLabel(nameIdx)?.let { panName = it }
                }
                if (fatherIdx != null) {
                    getValueAfterLabel(fatherIdx)?.let { panFatherName = it }
                }

                // Fallback: use multi-word name candidates in order
                if (panName.isEmpty() || panFatherName.isEmpty()) {
                    val multiWord = nameCandidates.filter { it.split(" ").size >= 2 }
                    if (panName.isEmpty() && multiWord.isNotEmpty()) panName = multiWord.first()
                    if (panFatherName.isEmpty() && multiWord.size > 1) panFatherName = multiWord[1]
                }
            }

            DocumentType.DRIVERS_LICENSE -> {
                // Indian DL format: STATE_CODE + 2-digit year + 7 digits (e.g. MH12 20120012345)
                val dlRegex = Regex("[A-Z]{2}[0-9]{2}\\s?[0-9]{4}\\s?[0-9]{7}|[A-Z]{2}-[0-9]{2}-[0-9]{4}-[0-9]{7}")
                dlRegex.find(text.uppercase())?.value?.let {
                    dlNumber = it.replace(" ", "").replace("-", "")
                }

                // Dates: first date = DOB, last date = validity
                val dates = Regex("\\b\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}\\b").findAll(text).toList()
                if (dates.isNotEmpty()) dlDob = dates.first().value
                if (dates.size > 1) dlExpiry = dates.last().value

                // Name via label or candidate
                val nameIdx = labelMap["NAME"] ?: labelMap["HOLDER"]
                if (nameIdx != null) {
                    getValueAfterLabel(nameIdx)?.let { dlHolderName = it }
                } else {
                    val multiWord = nameCandidates.filter { it.split(" ").size >= 2 }
                    if (multiWord.isNotEmpty()) dlHolderName = multiWord.first()
                }

                // State from DL number prefix
                if (dlNumber.length >= 2) dlState = dlNumber.take(2)
            }

            DocumentType.VEHICLE_RC -> {
                // RC format: e.g. MH12AB1234 or MH 12 AB 1234
                val rcRegex = Regex("[A-Z]{2}[\\s-]?[0-9]{1,2}[\\s-]?[A-Z]{1,3}[\\s-]?[0-9]{4}")
                rcRegex.find(text.uppercase())?.value?.let {
                    rcNumber = it.replace(" ", "").replace("-", "")
                }

                // RC expiry: last date in text
                val dates = Regex("\\b\\d{2}[/\\-]\\d{2}[/\\-]\\d{4}\\b").findAll(text).toList()
                if (dates.isNotEmpty()) rcExpiry = dates.last().value

                // Owner name
                val nameIdx = labelMap["NAME"] ?: labelMap["HOLDER"]
                if (nameIdx != null) {
                    getValueAfterLabel(nameIdx)?.let { rcOwnerName = it }
                } else {
                    val multiWord = nameCandidates.filter { it.split(" ").size >= 2 }
                    if (multiWord.isNotEmpty()) rcOwnerName = multiWord.first()
                }
            }
        }
    }

    // ── Image/PDF Processors ─────────────────────────────────────────────────
    fun processImageUri(uri: Uri) {
        try {
            isOcrRunning = true
            isPdfAttached = false
            context.contentResolver.openInputStream(uri)?.use { stream ->
                selectedImageBytes = stream.readBytes()
            }
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))
            }
            selectedImageBitmap = bitmap
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(InputImage.fromFilePath(context, uri))
                .addOnSuccessListener { visionText ->
                    isOcrRunning = false
                    ocrTextResult = visionText.text
                    runAutoFill(visionText.text)
                    Toast.makeText(context, "Details extracted!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    isOcrRunning = false
                    e.printStackTrace()
                    Toast.makeText(context, "OCR failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            isOcrRunning = false
            e.printStackTrace()
            Toast.makeText(context, "Error loading image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    fun processPdfUri(uri: Uri) {
        isOcrRunning = true
        selectedImageBitmap = null
        isPdfAttached = true
        coroutineScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        selectedImageBytes = stream.readBytes()
                    }
                }
                val extractedText = withContext(Dispatchers.IO) {
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        val renderer = PdfRenderer(pfd)
                        val textBuilder = StringBuilder()
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        val pageCount = minOf(renderer.pageCount, 3)
                        for (i in 0 until pageCount) {
                            val page = renderer.openPage(i)
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            val visionText = Tasks.await(recognizer.process(InputImage.fromBitmap(bitmap, 0)))
                            textBuilder.append(visionText.text).append("\n")
                        }
                        renderer.close(); pfd.close()
                        textBuilder.toString()
                    } else ""
                }
                isOcrRunning = false
                if (extractedText.isNotEmpty()) {
                    ocrTextResult = extractedText
                    runAutoFill(extractedText)
                    Toast.makeText(context, "PDF extracted!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No text found in PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isOcrRunning = false
                isPdfAttached = false
                e.printStackTrace()
                Toast.makeText(context, "PDF error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
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
                .setPageLimit(1)
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
                        Toast.makeText(context, "Scanner failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
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

                // ── Attachment Section ────────────────────────────────────
                SectionHeader("Document Attachment", accentColor)
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
                        Text("Scanning & extracting text offline…", color = TextSecondary, fontSize = 13.sp)
                    }
                } else if (isPdfAttached || selectedImageBitmap != null) {
                    // Attachment preview row
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
                            if (selectedImageBitmap != null) {
                                Image(
                                    bitmap = selectedImageBitmap!!.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(Icons.Default.Description, contentDescription = null, tint = Color(0xFFFF4444), modifier = Modifier.size(28.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isPdfAttached) "PDF Attached" else "Image Attached",
                                color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp
                            )
                            Text(
                                text = if (ocrTextResult != null) "Text extracted (${ocrTextResult!!.length} chars)" else "No text extracted",
                                color = if (ocrTextResult != null) AccentEmerald else TextMuted,
                                fontSize = 11.sp
                            )
                        }
                        IconButton(onClick = {
                            selectedImageBitmap = null
                            selectedImageBytes = null
                            isPdfAttached = false
                            ocrTextResult = null
                        }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Remove", tint = TextSecondary, modifier = Modifier.size(16.dp))
                        }
                    }
                } else {
                    // Empty attachment upload zone
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CinemaElevated)
                            .border(
                                width = 1.dp,
                                brush = Brush.linearGradient(listOf(CinemaStroke, CinemaStroke)),
                                shape = RoundedCornerShape(14.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.UploadFile, contentDescription = null, tint = TextMuted, modifier = Modifier.size(26.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Scan, photo or PDF to auto-fill", color = TextMuted, fontSize = 12.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Attachment action row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        Triple(Icons.Default.DocumentScanner, "Scan", { launchScanner() }),
                        Triple(Icons.Default.PhotoLibrary, "Gallery", {
                            MainActivity.isLaunchingSystemIntent = true
                            galleryLauncher.launch("image/*")
                        }),
                        Triple(Icons.Default.Description, "PDF", {
                            MainActivity.isLaunchingSystemIntent = true
                            pdfLauncher.launch("application/pdf")
                        })
                    ).forEach { (icon, label, action) ->
                        OutlinedButton(
                            onClick = { action() },
                            modifier = Modifier.weight(1f).height(42.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.4f)),
                            contentPadding = PaddingValues(4.dp)
                        ) {
                            Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(label, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
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
                                    cardNumber = cleaned.chunked(4).joinToString(" ")
                                    cardType = when (cleaned.firstOrNull()) {
                                        '4' -> "Visa"; '5' -> "Mastercard"; '3' -> "Amex"; '6' -> "RuPay"; else -> "Visa"
                                    }
                                }
                            },
                            label = "Card Number",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            accentColor = accentColor
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
                                        cardExpiry = if (cleaned.length >= 3) cleaned.substring(0, 2) + "/" + cleaned.substring(2) else cleaned
                                    }
                                },
                                label = "Expiry (MM/YY)",
                                placeholder = "MM/YY",
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                accentColor = accentColor
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
                                if (cleaned.length <= 12) aadhaarNumber = cleaned.chunked(4).joinToString(" ")
                            },
                            label = "Aadhaar Number",
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            accentColor = accentColor
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

                Spacer(modifier = Modifier.height(28.dp))

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
                                isPdfAttached -> "pdf"
                                selectedImageBitmap != null -> "png"
                                else -> null
                            }
                            val doc = VaultDocument(
                                id = documentToEdit?.id ?: UUID.randomUUID().toString(),
                                title = title.trim(),
                                type = documentType,
                                dlNumber = dlNumber.trim(), dlHolderName = dlHolderName.trim(),
                                dlDob = dlDob.trim(), dlExpiry = dlExpiry.trim(), dlState = dlState.trim(),
                                rcNumber = rcNumber.trim(), rcOwnerName = rcOwnerName.trim(),
                                rcChassisNumber = rcChassisNumber.trim(), rcEngineNumber = rcEngineNumber.trim(), rcExpiry = rcExpiry.trim(),
                                aadhaarNumber = aadhaarNumber.replace(" ", ""),
                                aadhaarName = aadhaarName.trim(), aadhaarDob = aadhaarDob.trim(), aadhaarGender = aadhaarGender,
                                panNumber = panNumber.trim(), panName = panName.trim(),
                                panFatherName = panFatherName.trim(), panDob = panDob.trim(),
                                cardNumber = cardNumber.replace(" ", ""), cardholderName = cardholderName.trim(),
                                cardExpiry = cardExpiry.trim(), cardCvv = cardCvv.trim(), cardType = cardType,
                                ocrText = ocrTextResult ?: documentToEdit?.ocrText
                            )
                            onSave(doc, selectedImageBytes, extension)
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
    accentColor: Color = AccentIndigo
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
            if (!cardExp.contains("/")) return "Expiry must be MM/YY format."
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
