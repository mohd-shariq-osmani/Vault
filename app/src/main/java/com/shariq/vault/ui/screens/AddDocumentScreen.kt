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
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import com.shariq.vault.MainActivity
import com.shariq.vault.model.DocumentType
import com.shariq.vault.model.VaultDocument
import com.shariq.vault.ui.theme.BorderGray
import com.shariq.vault.ui.theme.CyberCyan
import com.shariq.vault.ui.theme.DarkSurface
import com.shariq.vault.ui.theme.ObsidianBlack
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
            if (documentToEdit?.aadhaarNumber != null) {
                documentToEdit.aadhaarNumber.chunked(4).joinToString(" ")
            } else ""
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
            if (documentToEdit?.cardNumber != null) {
                documentToEdit.cardNumber.chunked(4).joinToString(" ")
            } else ""
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

    // Temporary URI for camera photo captures
    var tempPhotoUri by remember { mutableStateOf<Uri?>(null) }

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
                                val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                selectedImageBitmap = bitmap
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }
                    isOcrRunning = false
                }
            }
        }
    }

    val scrollState = rememberScrollState()

    // Validation State
    var showErrorAlert by remember { mutableStateOf(false) }
    var validationErrorMessage by remember { mutableStateOf("") }

    // Core regex and structural auto-fill parser
    fun runAutoFill(text: String) {
        val lines = text.split("\n").map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) return

        val cleanedLines = lines.map { line ->
            line.replace("NAME", "", ignoreCase = true)
                .replace("FATHER'S", "", ignoreCase = true)
                .replace("FATHERS", "", ignoreCase = true)
                .replace("FATHER", "", ignoreCase = true)
                .replace("MOTHER", "", ignoreCase = true)
                .replace("S/O", "", ignoreCase = true)
                .replace("D/O", "", ignoreCase = true)
                .replace("W/O", "", ignoreCase = true)
                .replace(":", "")
                .replace(";", "")
                .trim()
        }.filter { it.isNotEmpty() }

        val nameCandidates = cleanedLines.filter { line ->
            val cleaned = line.replace(" ", "").replace(".", "").trim()
            val isAlpha = cleaned.isNotEmpty() && cleaned.all { it.isLetter() }
            val isHeader = line.contains("INCOME", ignoreCase = true) ||
                           line.contains("TAX", ignoreCase = true) ||
                           line.contains("DEPT", ignoreCase = true) ||
                           line.contains("GOVT", ignoreCase = true) ||
                           line.contains("INDIA", ignoreCase = true) ||
                           line.contains("CARD", ignoreCase = true) ||
                           line.contains("PERMANENT", ignoreCase = true) ||
                           line.contains("ACCOUNT", ignoreCase = true) ||
                           line.contains("NUMBER", ignoreCase = true) ||
                           line.contains("SIGNATURE", ignoreCase = true) ||
                           line.contains("GENDER", ignoreCase = true) ||
                           line.contains("MALE", ignoreCase = true) ||
                           line.contains("FEMALE", ignoreCase = true) ||
                           line.contains("DOB", ignoreCase = true) ||
                           line.contains("BIRTH", ignoreCase = true) ||
                           line.contains("YEAR", ignoreCase = true) ||
                           line.contains("AUTHORITY", ignoreCase = true) ||
                           line.contains("UNIQUE", ignoreCase = true) ||
                           line.contains("IDENTIFICATION", ignoreCase = true) ||
                           line.contains("GOVERNMENT", ignoreCase = true)
            isAlpha && !isHeader && line.length > 2
        }

        when (documentType) {
            DocumentType.PAYMENT_CARD -> {
                // Card number: 13 to 19 digits
                val cardRegex = Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b|\\b\\d{13,16}\\b")
                val match = cardRegex.find(text)
                match?.value?.let {
                    val cleaned = it.replace(" ", "").replace("-", "")
                    cardNumber = cleaned.chunked(4).joinToString(" ")
                    if (cleaned.isNotEmpty()) {
                        cardType = when (cleaned.first()) {
                            '4' -> "Visa"
                            '5' -> "Mastercard"
                            '3' -> "Amex"
                            '6' -> "RuPay"
                            else -> "Visa"
                        }
                    }
                }
                
                val expiryRegex = Regex("\\b(0[1-9]|1[0-2])/([0-9]{2})\\b")
                val expMatch = expiryRegex.find(text)
                expMatch?.value?.let { cardExpiry = it }

                if (nameCandidates.isNotEmpty()) {
                    cardholderName = nameCandidates.first()
                }
            }
            DocumentType.AADHAAR_CARD -> {
                // 12 digits
                val aadhaarRegex = Regex("\\b\\d{4}[ -]?\\d{4}[ -]?\\d{4}\\b")
                val match = aadhaarRegex.find(text)
                match?.value?.let {
                    aadhaarNumber = it.replace(" ", "").replace("-", "").chunked(4).joinToString(" ")
                }
                
                if (text.contains("Male", ignoreCase = true)) {
                    aadhaarGender = "Male"
                } else if (text.contains("Female", ignoreCase = true)) {
                    aadhaarGender = "Female"
                }
                
                // DOB / YOB extraction
                val dobRegex = Regex("\\b\\d{2}[/-]\\d{2}[/-]\\d{4}\\b|Year of Birth\\s*:\\s*(\\d{4})")
                val dobMatch = dobRegex.find(text)
                dobMatch?.let {
                    if (it.groupValues.size > 1 && it.groupValues[1].isNotEmpty()) {
                        aadhaarDob = it.groupValues[1]
                    } else {
                        aadhaarDob = it.value
                    }
                }

                if (nameCandidates.isNotEmpty()) {
                    aadhaarName = nameCandidates.first()
                }
            }
            DocumentType.PAN_CARD -> {
                // 10 alphanumeric characters
                val panRegex = Regex("[A-Z]{5}[0-9]{4}[A-Z]")
                val match = panRegex.find(text.uppercase())
                match?.value?.let { panNumber = it }
                
                // DOB extraction
                val dobRegex = Regex("\\b\\d{2}[/-]\\d{2}[/-]\\d{4}\\b")
                val dobMatch = dobRegex.find(text)
                dobMatch?.value?.let { panDob = it }

                if (nameCandidates.isNotEmpty()) {
                    panName = nameCandidates.first()
                    if (nameCandidates.size > 1) {
                        panFatherName = nameCandidates[1]
                    }
                }
            }
            DocumentType.DRIVERS_LICENSE -> {
                // DL Format
                val dlRegex = Regex("[A-Z]{2}[ -]?\\d{2}[ -]?\\d{11,13}")
                val match = dlRegex.find(text.uppercase())
                match?.value?.let { dlNumber = it.replace(" ", "").replace("-", "") }
                
                // Dates extraction
                val dobRegex = Regex("\\b\\d{2}[/-]\\d{2}[/-]\\d{4}\\b")
                val matches = dobRegex.findAll(text).toList()
                if (matches.isNotEmpty()) {
                    dlDob = matches.first().value
                    if (matches.size > 1) {
                        dlExpiry = matches[1].value
                    }
                }

                if (nameCandidates.isNotEmpty()) {
                    dlHolderName = nameCandidates.first()
                }
            }
            DocumentType.VEHICLE_RC -> {
                // RC Format
                val rcRegex = Regex("[A-Z]{2}[ -]?\\d{2}[ -]?[A-Z]{1,3}[ -]?\\d{4}")
                val match = rcRegex.find(text.uppercase())
                match?.value?.let { rcNumber = it.replace(" ", "").replace("-", "") }
                
                val expRegex = Regex("\\b\\d{2}[/-]\\d{2}[/-]\\d{4}\\b")
                val matches = expRegex.findAll(text).toList()
                if (matches.isNotEmpty()) {
                    rcExpiry = matches.last().value
                }

                if (nameCandidates.isNotEmpty()) {
                    rcOwnerName = nameCandidates.first()
                }
            }
        }
    }

    // Handles the selected image Uri, extracts bytes, runs OCR
    fun processImageUri(uri: Uri) {
        try {
            isOcrRunning = true
            isPdfAttached = false

            // Read raw bytes for storage
            context.contentResolver.openInputStream(uri)?.use { stream ->
                selectedImageBytes = stream.readBytes()
            }

            // Load visual bitmap for Compose UI
            val bitmap = if (Build.VERSION.SDK_INT < 28) {
                @Suppress("DEPRECATION")
                MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
            } else {
                val source = ImageDecoder.createSource(context.contentResolver, uri)
                ImageDecoder.decodeBitmap(source)
            }
            selectedImageBitmap = bitmap

            // Run offline OCR using ML Kit
            val inputImage = InputImage.fromFilePath(context, uri)
            val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            recognizer.process(inputImage)
                .addOnSuccessListener { visionText ->
                    isOcrRunning = false
                    val extractedText = visionText.text
                    ocrTextResult = extractedText
                    runAutoFill(extractedText)
                    Toast.makeText(context, "Details extracted and pre-filled!", Toast.LENGTH_SHORT).show()
                }
                .addOnFailureListener { e ->
                    isOcrRunning = false
                    e.printStackTrace()
                    Toast.makeText(context, "Could not extract text: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                }
        } catch (e: Exception) {
            isOcrRunning = false
            e.printStackTrace()
            Toast.makeText(context, "Error loading document image: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Handles selected PDF Uri, extracts bytes, runs multi-page OCR
    fun processPdfUri(uri: Uri) {
        isOcrRunning = true
        selectedImageBitmap = null
        isPdfAttached = true

        coroutineScope.launch {
            try {
                // Read PDF bytes
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { stream ->
                        selectedImageBytes = stream.readBytes()
                    }
                }

                // Render pages to bitmaps & perform OCR synchronously in worker thread
                val extractedText = withContext(Dispatchers.IO) {
                    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                    if (pfd != null) {
                        val renderer = PdfRenderer(pfd)
                        val textBuilder = StringBuilder()
                        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                        
                        // Limit to first 3 pages to avoid memory pressure
                        val pageCount = minOf(renderer.pageCount, 3)
                        for (i in 0 until pageCount) {
                            val page = renderer.openPage(i)
                            // Scale page size slightly for better OCR clarity
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            page.close()
                            
                            val image = InputImage.fromBitmap(bitmap, 0)
                            val visionText = Tasks.await(recognizer.process(image))
                            textBuilder.append(visionText.text).append("\n")
                        }
                        renderer.close()
                        pfd.close()
                        textBuilder.toString()
                    } else ""
                }

                isOcrRunning = false
                if (extractedText.isNotEmpty()) {
                    ocrTextResult = extractedText
                    runAutoFill(extractedText)
                    Toast.makeText(context, "PDF details extracted & pre-filled!", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "No text found in PDF", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                isOcrRunning = false
                isPdfAttached = false
                e.printStackTrace()
                Toast.makeText(context, "Error processing PDF: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Camera, Gallery, and PDF launchers (setting isLaunchingSystemIntent to true to bypass auto-lock)
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processImageUri(it) }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { processPdfUri(it) }
    }

    val scannerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val scanResult = com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            if (scanResult != null) {
                val pdf = scanResult.pdf
                pdf?.let {
                    val pdfUri = it.uri
                    processPdfUri(pdfUri)
                }
            }
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
                        scannerLauncher.launch(
                            androidx.activity.result.IntentSenderRequest.Builder(intentSender).build()
                        )
                    }
                    .addOnFailureListener { e ->
                        e.printStackTrace()
                        Toast.makeText(context, "Scanner failed to launch: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
                    }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(context, "Error: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    // Live preview
    val previewDoc = remember(
        title, documentType, dlNumber, dlHolderName, dlExpiry,
        rcNumber, rcOwnerName, rcExpiry,
        aadhaarNumber, aadhaarName,
        panNumber, panName,
        cardNumber, cardholderName, cardExpiry, cardType
    ) {
        VaultDocument(
            id = "preview",
            title = if (title.isEmpty()) "Document Title" else title,
            type = documentType,
            dlNumber = dlNumber,
            dlHolderName = dlHolderName,
            dlExpiry = dlExpiry,
            rcNumber = rcNumber,
            rcOwnerName = rcOwnerName,
            rcExpiry = rcExpiry,
            aadhaarNumber = aadhaarNumber,
            aadhaarName = aadhaarName,
            panNumber = panNumber,
            panName = panName,
            cardNumber = cardNumber,
            cardholderName = cardholderName,
            cardExpiry = cardExpiry,
            cardType = cardType
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add ${documentTypeToLabel(documentType)}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(scrollState)
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "REAL-TIME CARD PREVIEW",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = CyberCyan,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            DocumentItem(
                document = previewDoc,
                onClick = {},
                onLongClick = {}
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Attachment upload options
            Text(
                text = "ATTACH DOCUMENT FILE / SCAN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { launchScanner() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.DocumentScanner, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Scan Card", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        MainActivity.isLaunchingSystemIntent = true
                        galleryLauncher.launch("image/*")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.PhotoLibrary, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Gallery", fontSize = 12.sp)
                }

                OutlinedButton(
                    onClick = {
                        MainActivity.isLaunchingSystemIntent = true
                        pdfLauncher.launch("application/pdf")
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = CyberCyan),
                    border = ButtonDefaults.outlinedButtonBorder.copy(width = 1.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("PDF File", fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attached preview
            if (isOcrRunning) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator(color = CyberCyan, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Scanning file contents offline...", color = Color.White, fontSize = 14.sp)
                }
            } else if (isPdfAttached) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BorderGray),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Description, contentDescription = null, tint = Color.Red, modifier = Modifier.size(32.dp))
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("PDF Document Attached", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (ocrTextResult != null) "Text extracted (${ocrTextResult!!.length} chars)" else "No text extracted",
                            color = Color.Green,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = {
                        selectedImageBytes = null
                        isPdfAttached = false
                        ocrTextResult = null
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove PDF", tint = Color.Red)
                    }
                }
            } else if (selectedImageBitmap != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        bitmap = selectedImageBitmap!!.asImageBitmap(),
                        contentDescription = "Document Thumbnail",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Image Attached", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(
                            text = if (ocrTextResult != null) "Text extracted (${ocrTextResult!!.length} chars)" else "No text extracted",
                            color = Color.Green,
                            fontSize = 12.sp
                        )
                    }
                    IconButton(onClick = {
                        selectedImageBitmap = null
                        selectedImageBytes = null
                        ocrTextResult = null
                    }) {
                        Icon(Icons.Default.Delete, contentDescription = "Remove Image", tint = Color.Red)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "DOCUMENT DETAILS",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White.copy(alpha = 0.5f),
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Friendly Nickname
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Friendly Alias / Title") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = outlinedColors()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Forms
            when (documentType) {
                DocumentType.PAYMENT_CARD -> {
                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { input ->
                            val cleaned = input.filter { it.isDigit() }
                            if (cleaned.length <= 16) {
                                cardNumber = cleaned.chunked(4).joinToString(" ")
                                if (cleaned.isNotEmpty()) {
                                    cardType = when (cleaned.first()) {
                                        '4' -> "Visa"
                                        '5' -> "Mastercard"
                                        '3' -> "Amex"
                                        '6' -> "RuPay"
                                        else -> "Visa"
                                    }
                                }
                            }
                        },
                        label = { Text("Card Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = cardholderName,
                        onValueChange = { cardholderName = it },
                        label = { Text("Cardholder Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = cardExpiry,
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                if (cleaned.length <= 4) {
                                    cardExpiry = if (cleaned.length >= 3) {
                                        cleaned.substring(0, 2) + "/" + cleaned.substring(2)
                                    } else {
                                        cleaned
                                    }
                                }
                            },
                            label = { Text("Expiry (MM/YY)") },
                            placeholder = { Text("MM/YY") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                        OutlinedTextField(
                            value = cardCvv,
                            onValueChange = { input ->
                                val cleaned = input.filter { it.isDigit() }
                                if (cleaned.length <= 4) {
                                    cardCvv = cleaned
                                }
                            },
                            label = { Text("CVV / PIN") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        OutlinedTextField(
                            value = cardType,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Card Network") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = outlinedColors()
                        )
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false },
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .background(DarkSurface)
                        ) {
                            listOf("Visa", "Mastercard", "RuPay", "Amex").forEach { network ->
                                DropdownMenuItem(
                                    text = { Text(network, color = Color.White) },
                                    onClick = {
                                        cardType = network
                                        expanded = false
                                    }
                                )
                            }
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .clickable { expanded = true }
                        )
                    }
                }
                DocumentType.AADHAAR_CARD -> {
                    OutlinedTextField(
                        value = aadhaarNumber,
                        onValueChange = { input ->
                            val cleaned = input.filter { it.isDigit() }
                            if (cleaned.length <= 12) {
                                aadhaarNumber = cleaned.chunked(4).joinToString(" ")
                            }
                        },
                        label = { Text("Aadhaar Number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = aadhaarName,
                        onValueChange = { aadhaarName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = aadhaarDob,
                            onValueChange = { aadhaarDob = it },
                            label = { Text("DOB / YOB") },
                            placeholder = { Text("DD-MM-YYYY") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                        
                        var genderExpanded by remember { mutableStateOf(false) }
                        Box(modifier = Modifier.weight(1f)) {
                            OutlinedTextField(
                                value = aadhaarGender,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Gender") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = genderExpanded) },
                                modifier = Modifier.fillMaxWidth(),
                                colors = outlinedColors()
                            )
                            DropdownMenu(
                                expanded = genderExpanded,
                                onDismissRequest = { genderExpanded = false },
                                modifier = Modifier.background(DarkSurface)
                            ) {
                                listOf("Male", "Female", "Other").forEach { gender ->
                                    DropdownMenuItem(
                                        text = { Text(gender, color = Color.White) },
                                        onClick = {
                                            aadhaarGender = gender
                                            genderExpanded = false
                                        }
                                    )
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { genderExpanded = true }
                            )
                        }
                    }
                }
                DocumentType.PAN_CARD -> {
                    OutlinedTextField(
                        value = panNumber,
                        onValueChange = { input ->
                            if (input.length <= 10) {
                                panNumber = input.uppercase()
                            }
                        },
                        label = { Text("PAN Card Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = panName,
                        onValueChange = { panName = it },
                        label = { Text("Full Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = panFatherName,
                        onValueChange = { panFatherName = it },
                        label = { Text("Father's Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = panDob,
                        onValueChange = { panDob = it },
                        label = { Text("Date of Birth (DD-MM-YYYY)") },
                        placeholder = { Text("DD-MM-YYYY") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                }
                DocumentType.DRIVERS_LICENSE -> {
                    OutlinedTextField(
                        value = dlNumber,
                        onValueChange = { dlNumber = it.uppercase() },
                        label = { Text("License Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dlHolderName,
                        onValueChange = { dlHolderName = it },
                        label = { Text("Holder Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = dlDob,
                            onValueChange = { dlDob = it },
                            label = { Text("DOB") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                        OutlinedTextField(
                            value = dlExpiry,
                            onValueChange = { dlExpiry = it },
                            label = { Text("Expiry") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = dlState,
                        onValueChange = { dlState = it },
                        label = { Text("Issuing State") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                }
                DocumentType.VEHICLE_RC -> {
                    OutlinedTextField(
                        value = rcNumber,
                        onValueChange = { rcNumber = it.uppercase() },
                        label = { Text("Registration Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rcOwnerName,
                        onValueChange = { rcOwnerName = it },
                        label = { Text("Owner Name") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = rcChassisNumber,
                            onValueChange = { rcChassisNumber = it.uppercase() },
                            label = { Text("Chassis Number") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                        OutlinedTextField(
                            value = rcEngineNumber,
                            onValueChange = { rcEngineNumber = it.uppercase() },
                            label = { Text("Engine Number") },
                            singleLine = true,
                            modifier = Modifier.weight(1f),
                            colors = outlinedColors()
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = rcExpiry,
                        onValueChange = { rcExpiry = it },
                        label = { Text("RC Expiry Date") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = outlinedColors()
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save
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
                            dlNumber = dlNumber.trim(),
                            dlHolderName = dlHolderName.trim(),
                            dlDob = dlDob.trim(),
                            dlExpiry = dlExpiry.trim(),
                            dlState = dlState.trim(),
                            rcNumber = rcNumber.trim(),
                            rcOwnerName = rcOwnerName.trim(),
                            rcChassisNumber = rcChassisNumber.trim(),
                            rcEngineNumber = rcEngineNumber.trim(),
                            rcExpiry = rcExpiry.trim(),
                            aadhaarNumber = aadhaarNumber.replace(" ", ""),
                            aadhaarName = aadhaarName.trim(),
                            aadhaarDob = aadhaarDob.trim(),
                            aadhaarGender = aadhaarGender,
                            panNumber = panNumber.trim(),
                            panName = panName.trim(),
                            panFatherName = panFatherName.trim(),
                            panDob = panDob.trim(),
                            cardNumber = cardNumber.replace(" ", ""),
                            cardholderName = cardholderName.trim(),
                            cardExpiry = cardExpiry.trim(),
                            cardCvv = cardCvv.trim(),
                            cardType = cardType,
                            ocrText = ocrTextResult ?: documentToEdit?.ocrText
                        )
                        onSave(doc, selectedImageBytes, extension)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = ObsidianBlack
                )
            ) {
                Icon(Icons.Default.Check, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("SAVE ENCRYPTED", fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
            }
        }
    }

    if (showErrorAlert) {
        AlertDialog(
            onDismissRequest = { showErrorAlert = false },
            title = { Text("Validation Error", color = Color.White) },
            text = { Text(validationErrorMessage, color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { showErrorAlert = false }) {
                    Text("OK", color = CyberCyan)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
private fun outlinedColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyberCyan,
    unfocusedBorderColor = BorderGray,
    focusedLabelColor = CyberCyan,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = Color.White,
    unfocusedTextColor = Color.White,
    focusedContainerColor = DarkSurface,
    unfocusedContainerColor = DarkSurface
)

private val TextSecondary = Color(0xFF8E8E9F)

private fun documentTypeToLabel(type: DocumentType): String = when (type) {
    DocumentType.PAYMENT_CARD -> "Payment Card"
    DocumentType.AADHAAR_CARD -> "Aadhaar Card"
    DocumentType.PAN_CARD -> "PAN Card"
    DocumentType.DRIVERS_LICENSE -> "Driver's License"
    DocumentType.VEHICLE_RC -> "Vehicle RC"
}

private fun validateInputs(
    type: DocumentType,
    title: String,
    dlNum: String, dlName: String,
    rcNum: String, rcName: String,
    adNum: String, adName: String,
    panNum: String, panName: String,
    cardNum: String, cardName: String, cardExp: String, cardCvv: String
): String? {
    if (title.trim().isEmpty()) return "Please enter a friendly alias title."
    
    when (type) {
        DocumentType.PAYMENT_CARD -> {
            val cleanCard = cardNum.replace(" ", "")
            if (cleanCard.length < 13 || cleanCard.length > 19) return "Invalid credit/debit card number. Must be between 13 and 19 digits."
            if (cardName.trim().isEmpty()) return "Cardholder name is required."
            if (!cardExp.contains("/")) return "Expiry date must be in MM/YY format."
            if (cardCvv.length < 3) return "CVV/PIN must be at least 3 digits."
        }
        DocumentType.AADHAAR_CARD -> {
            val cleanAd = adNum.replace(" ", "")
            if (cleanAd.length != 12) return "Aadhaar Number must be exactly 12 digits."
            if (adName.trim().isEmpty()) return "Full Name is required."
        }
        DocumentType.PAN_CARD -> {
            if (panNum.trim().length != 10) return "PAN Card number must be exactly 10 alphanumeric characters."
            if (panName.trim().isEmpty()) return "Full Name is required."
        }
        DocumentType.DRIVERS_LICENSE -> {
            if (dlNum.trim().isEmpty()) return "Driver's license number is required."
            if (dlName.trim().isEmpty()) return "Holder Name is required."
        }
        DocumentType.VEHICLE_RC -> {
            if (rcNum.trim().isEmpty()) return "Vehicle Registration number is required."
            if (rcName.trim().isEmpty()) return "Registered Owner Name is required."
        }
    }
    return null
}
