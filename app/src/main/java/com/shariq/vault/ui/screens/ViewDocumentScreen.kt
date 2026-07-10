package com.shariq.vault.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.shariq.vault.MainActivity
import com.shariq.vault.model.DocumentType
import com.shariq.vault.model.VaultDocument
import com.shariq.vault.ui.components.GlassmorphicCard
import com.shariq.vault.ui.theme.*
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ViewDocumentScreen(
    document: VaultDocument,
    onLoadImage: (String) -> ByteArray?,
    onEditClicked: () -> Unit,
    onBack: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    
    // Default to true so that users can view their document scan instantly
    var isRevealed by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }

    // State for decrypted attachment bitmap preview (PDF page 1 or image)
    var decryptedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isImageLoading by remember { mutableStateOf(false) }

    // Fetch and decrypt attachment on launch/change
    LaunchedEffect(document.imagePath) {
        document.imagePath?.let { path ->
            isImageLoading = true
            val bytes = onLoadImage(path)
            if (bytes != null) {
                try {
                    if (path.endsWith(".pdf")) {
                        // Render page 1 of the PDF to preview it inline
                        val tempFile = File(context.cacheDir, "temp_render.pdf").apply {
                            writeBytes(bytes)
                            deleteOnExit()
                        }
                        val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        if (renderer.pageCount > 0) {
                            val page = renderer.openPage(0)
                            // Scale up for a sharp text representation
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            decryptedBitmap = bitmap
                            page.close()
                        }
                        renderer.close()
                        pfd.close()
                        tempFile.delete()
                    } else {
                        // Standard image decoding
                        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                        decryptedBitmap = bitmap
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            isImageLoading = false
        }
    }

    val gradient = when (document.type) {
        DocumentType.PAYMENT_CARD -> CardGradientBluePurple
        DocumentType.AADHAAR_CARD -> CardGradientEmerald
        DocumentType.PAN_CARD -> CardGradientSunset
        DocumentType.DRIVERS_LICENSE -> CardGradientDeepSpace
        DocumentType.VEHICLE_RC -> CardGradientDarkPurple
    }

    val themeAccentColor = when (document.type) {
        DocumentType.PAYMENT_CARD -> CyberCyan
        DocumentType.AADHAAR_CARD -> MintGreen
        DocumentType.PAN_CARD -> NeonOrange
        DocumentType.DRIVERS_LICENSE -> CyberCyan
        DocumentType.VEHICLE_RC -> NeonPurple
    }

    val typeLabel = when (document.type) {
        DocumentType.PAYMENT_CARD -> document.cardType ?: "Payment Card"
        DocumentType.AADHAAR_CARD -> "Aadhaar Card"
        DocumentType.PAN_CARD -> "PAN Card"
        DocumentType.DRIVERS_LICENSE -> "Driver's License"
        DocumentType.VEHICLE_RC -> "Vehicle Registration Certificate (RC)"
    }

    val primaryNumberLabel = when (document.type) {
        DocumentType.PAYMENT_CARD -> "Card Number"
        DocumentType.AADHAAR_CARD -> "Aadhaar Number"
        DocumentType.PAN_CARD -> "PAN Number"
        DocumentType.DRIVERS_LICENSE -> "License Number"
        DocumentType.VEHICLE_RC -> "Registration Number"
    }

    val rawPrimaryNumber = when (document.type) {
        DocumentType.PAYMENT_CARD -> document.cardNumber ?: ""
        DocumentType.AADHAAR_CARD -> document.aadhaarNumber ?: ""
        DocumentType.PAN_CARD -> document.panNumber ?: ""
        DocumentType.DRIVERS_LICENSE -> document.dlNumber ?: ""
        DocumentType.VEHICLE_RC -> document.rcNumber ?: ""
    }

    val displayedPrimaryNumber = if (isRevealed) {
        when (document.type) {
            DocumentType.PAYMENT_CARD -> rawPrimaryNumber.chunked(4).joinToString(" ")
            DocumentType.AADHAAR_CARD -> rawPrimaryNumber.chunked(4).joinToString(" ")
            else -> rawPrimaryNumber
        }
    } else {
        when (document.type) {
            DocumentType.PAYMENT_CARD -> "•••• •••• •••• " + rawPrimaryNumber.takeLast(4)
            DocumentType.AADHAAR_CARD -> "•••• •••• " + rawPrimaryNumber.takeLast(4)
            DocumentType.PAN_CARD -> "••••• " + rawPrimaryNumber.takeLast(4)
            else -> "••••••••••••"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(document.title) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isRevealed = !isRevealed }) {
                        Icon(
                            imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = "Toggle Visibility"
                        )
                    }
                    IconButton(onClick = onEditClicked) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Document",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Document",
                            tint = CyberPink
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBlack,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
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
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Card mockup
            GlassmorphicCard(gradient = gradient) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column {
                        Text(
                            text = typeLabel.uppercase(),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = document.title,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    Icon(
                        imageVector = when (document.type) {
                            DocumentType.PAYMENT_CARD -> Icons.Default.CreditCard
                            DocumentType.AADHAAR_CARD -> Icons.Default.Badge
                            DocumentType.PAN_CARD -> Icons.Default.AssignmentInd
                            DocumentType.DRIVERS_LICENSE -> Icons.Default.DriveEta
                            DocumentType.VEHICLE_RC -> Icons.Default.CarRental
                        },
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(36.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = displayedPrimaryNumber,
                        fontSize = if (document.type == DocumentType.PAYMENT_CARD) 20.sp else 18.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.5.sp,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            copyToClipboard(context, primaryNumberLabel, rawPrimaryNumber)
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Number",
                            tint = Color.White.copy(alpha = 0.7f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "NAME",
                            fontSize = 8.sp,
                            color = Color.White.copy(alpha = 0.6f),
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = when (document.type) {
                                DocumentType.PAYMENT_CARD -> document.cardholderName ?: ""
                                DocumentType.AADHAAR_CARD -> document.aadhaarName ?: ""
                                DocumentType.PAN_CARD -> document.panName ?: ""
                                DocumentType.DRIVERS_LICENSE -> document.dlHolderName ?: ""
                                DocumentType.VEHICLE_RC -> document.rcOwnerName ?: ""
                            },
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    
                    val validityDate = when (document.type) {
                        DocumentType.PAYMENT_CARD -> document.cardExpiry
                        DocumentType.DRIVERS_LICENSE -> document.dlExpiry
                        DocumentType.VEHICLE_RC -> document.rcExpiry
                        else -> null
                    }
                    if (validityDate != null) {
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = if (document.type == DocumentType.PAYMENT_CARD) "EXPIRES" else "VALID UNTIL",
                                fontSize = 8.sp,
                                color = Color.White.copy(alpha = 0.6f),
                                letterSpacing = 1.sp
                            )
                            Text(
                                text = validityDate,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Document visualization box
            document.imagePath?.let { path ->
                val isPdf = path.endsWith(".pdf")
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = if (isPdf) "SECURED DOCUMENT PREVIEW (PDF)" else "SECURED IMAGE SCAN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = themeAccentColor,
                    letterSpacing = 1.5.sp,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isImageLoading) {
                        CircularProgressIndicator(color = CyberCyan)
                    } else if (decryptedBitmap != null) {
                        if (isRevealed) {
                            Image(
                                bitmap = decryptedBitmap!!.asImageBitmap(),
                                contentDescription = "Decrypted Document Scan",
                                modifier = Modifier.fillMaxSize().padding(8.dp),
                                contentScale = ContentScale.Fit
                            )
                        } else {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = BorderGray, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Scan hidden for safety",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Tap the eye icon in the top toolbar to reveal the scan.",
                                    color = TextSecondary,
                                    fontSize = 11.sp,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                            }
                        }
                    } else {
                        Text("Unable to load decrypted preview", color = Color.Red, fontSize = 13.sp)
                    }
                }

                if (isPdf) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = { openPdfAttachment(context, path, onLoadImage) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurface, contentColor = CyberCyan),
                        border = androidx.compose.foundation.BorderStroke(1.dp, BorderGray)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Open Full PDF Document", fontWeight = FontWeight.Bold)
                    }
                }
            }

            // Scanned raw copyable details
            document.ocrText?.let { ocr ->
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "EXTRACTED DETAILS (OCR)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = themeAccentColor,
                        letterSpacing = 1.5.sp
                    )
                    TextButton(
                        onClick = { copyToClipboard(context, "Scanned Text", ocr) },
                        colors = ButtonDefaults.textButtonColors(contentColor = themeAccentColor)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Copy Raw Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 200.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkSurface)
                        .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
                        .padding(16.dp)
                ) {
                    val ocrScrollState = rememberScrollState()
                    Column(modifier = Modifier.verticalScroll(ocrScrollState)) {
                        Text(
                            text = ocr,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White.copy(alpha = 0.85f),
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            // Subtitle
            Text(
                text = "ALL SECURE INFORMATION",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = themeAccentColor,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Text detail listings
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(DarkSurface)
                    .border(1.dp, BorderGray, RoundedCornerShape(16.dp))
            ) {
                DetailRow(label = "Title Alias", value = document.title, context = context)

                when (document.type) {
                    DocumentType.PAYMENT_CARD -> {
                        DetailRow(label = "Card Network", value = document.cardType ?: "", context = context)
                        DetailRow(label = "Cardholder Name", value = document.cardholderName ?: "", context = context)
                        DetailRow(
                            label = "Card Number", 
                            value = document.cardNumber ?: "", 
                            context = context,
                            isSensitive = true,
                            isRevealed = isRevealed
                        )
                        DetailRow(label = "Expiry Date", value = document.cardExpiry ?: "", context = context)
                        DetailRow(
                            label = "CVV / PIN", 
                            value = document.cardCvv ?: "", 
                            context = context,
                            isSensitive = true,
                            isRevealed = isRevealed
                        )
                    }
                    DocumentType.AADHAAR_CARD -> {
                        DetailRow(label = "Full Name", value = document.aadhaarName ?: "", context = context)
                        DetailRow(
                            label = "Aadhaar Number", 
                            value = document.aadhaarNumber ?: "", 
                            context = context,
                            isSensitive = true,
                            isRevealed = isRevealed
                        )
                        DetailRow(label = "Date of Birth", value = document.aadhaarDob ?: "", context = context)
                        DetailRow(label = "Gender", value = document.aadhaarGender ?: "", context = context)
                    }
                    DocumentType.PAN_CARD -> {
                        DetailRow(label = "Full Name", value = document.panName ?: "", context = context)
                        DetailRow(
                            label = "PAN Number", 
                            value = document.panNumber ?: "", 
                            context = context,
                            isSensitive = true,
                            isRevealed = isRevealed
                        )
                        DetailRow(label = "Father's Name", value = document.panFatherName ?: "", context = context)
                        DetailRow(label = "Date of Birth", value = document.panDob ?: "", context = context)
                    }
                    DocumentType.DRIVERS_LICENSE -> {
                        DetailRow(label = "Holder Name", value = document.dlHolderName ?: "", context = context)
                        DetailRow(label = "License Number", value = document.dlNumber ?: "", context = context)
                        DetailRow(label = "Date of Birth", value = document.dlDob ?: "", context = context)
                        DetailRow(label = "Expiry Date", value = document.dlExpiry ?: "", context = context)
                        DetailRow(label = "Issuing State", value = document.dlState ?: "", context = context)
                    }
                    DocumentType.VEHICLE_RC -> {
                        DetailRow(label = "Registration Number", value = document.rcNumber ?: "", context = context)
                        DetailRow(label = "Registered Owner", value = document.rcOwnerName ?: "", context = context)
                        DetailRow(label = "Chassis Number", value = document.rcChassisNumber ?: "", context = context)
                        DetailRow(label = "Engine Number", value = document.rcEngineNumber ?: "", context = context)
                        DetailRow(label = "Registration Expiry", value = document.rcExpiry ?: "", context = context)
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Item", color = Color.White) },
            text = { Text("Are you sure you want to permanently erase '${document.title}' and its associated encrypted file from your secure local vault? This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete Permanently", color = CyberPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun DetailRow(
    label: String,
    value: String,
    context: Context,
    isSensitive: Boolean = false,
    isRevealed: Boolean = false
) {
    if (value.isEmpty()) return

    val displayedValue = if (isSensitive && !isRevealed) {
        "••••••••"
    } else {
        value
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                copyToClipboard(context, label, value)
            }
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label.uppercase(),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextSecondary,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = displayedValue,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    fontFamily = if (isSensitive) FontFamily.Monospace else FontFamily.Default
                )
            }
            Icon(
                imageVector = Icons.Default.ContentCopy,
                contentDescription = "Copy to clipboard",
                tint = BorderGray,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = BorderGray, thickness = 0.5.dp)
    }
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard!", Toast.LENGTH_SHORT).show()
}

// Opens the decrypted PDF attachment in an external viewer intent, bypassing auto-lock
private fun openPdfAttachment(context: Context, fileName: String, onLoadImage: (String) -> ByteArray?) {
    try {
        val bytes = onLoadImage(fileName)
        if (bytes == null) {
            Toast.makeText(context, "Failed to load document", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Write decrypted bytes to cache file
        val tempFile = File(context.cacheDir, "decrypted_document.pdf").apply {
            writeBytes(bytes)
            deleteOnExit()
        }
        
        val uri = FileProvider.getUriForFile(
            context,
            "com.shariq.vault.fileprovider",
            tempFile
        )
        
        // Set bypass auto-lock
        MainActivity.isLaunchingSystemIntent = true
        
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No app available to open PDF files", Toast.LENGTH_SHORT).show()
    }
}
