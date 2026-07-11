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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    var isRevealed by remember { mutableStateOf(true) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var decryptedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isImageLoading by remember { mutableStateOf(false) }

    LaunchedEffect(document.imagePath) {
        document.imagePath?.let { path ->
            isImageLoading = true
            val bytes = onLoadImage(path)
            if (bytes != null) {
                try {
                    if (path.endsWith(".pdf")) {
                        val tempFile = File(context.cacheDir, "temp_render.pdf").apply {
                            writeBytes(bytes); deleteOnExit()
                        }
                        val pfd = android.os.ParcelFileDescriptor.open(tempFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY)
                        val renderer = PdfRenderer(pfd)
                        if (renderer.pageCount > 0) {
                            val page = renderer.openPage(0)
                            val bitmap = Bitmap.createBitmap(page.width * 2, page.height * 2, Bitmap.Config.ARGB_8888)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            decryptedBitmap = bitmap
                            page.close()
                        }
                        renderer.close(); pfd.close(); tempFile.delete()
                    } else {
                        decryptedBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    }
                } catch (e: Exception) { e.printStackTrace() }
            }
            isImageLoading = false
        }
    }

    val gradient = when (document.type) {
        DocumentType.PAYMENT_CARD    -> CardGradientBluePurple
        DocumentType.AADHAAR_CARD    -> CardGradientEmerald
        DocumentType.PAN_CARD        -> CardGradientSunset
        DocumentType.DRIVERS_LICENSE -> CardGradientDeepSpace
        DocumentType.VEHICLE_RC      -> CardGradientDarkPurple
    }

    val accentColor = when (document.type) {
        DocumentType.PAYMENT_CARD    -> AccentIndigo
        DocumentType.AADHAAR_CARD    -> AccentEmerald
        DocumentType.PAN_CARD        -> Color(0xFFFF9F00)
        DocumentType.DRIVERS_LICENSE -> AccentIndigo
        DocumentType.VEHICLE_RC      -> Color(0xFF9D6FFF)
    }

    val typeIcon = when (document.type) {
        DocumentType.PAYMENT_CARD    -> Icons.Default.CreditCard
        DocumentType.AADHAAR_CARD    -> Icons.Default.Badge
        DocumentType.PAN_CARD        -> Icons.Default.AssignmentInd
        DocumentType.DRIVERS_LICENSE -> Icons.Default.DriveEta
        DocumentType.VEHICLE_RC      -> Icons.Default.DirectionsCar
    }

    val typeLabel = when (document.type) {
        DocumentType.PAYMENT_CARD    -> document.cardType ?: "Payment Card"
        DocumentType.AADHAAR_CARD    -> "Aadhaar Card"
        DocumentType.PAN_CARD        -> "PAN Card"
        DocumentType.DRIVERS_LICENSE -> "Driver's Licence"
        DocumentType.VEHICLE_RC      -> "Vehicle RC"
    }

    val primaryNumberLabel = when (document.type) {
        DocumentType.PAYMENT_CARD    -> "Card Number"
        DocumentType.AADHAAR_CARD    -> "Aadhaar Number"
        DocumentType.PAN_CARD        -> "PAN Number"
        DocumentType.DRIVERS_LICENSE -> "Licence Number"
        DocumentType.VEHICLE_RC      -> "Registration Number"
    }

    val rawPrimaryNumber = when (document.type) {
        DocumentType.PAYMENT_CARD    -> document.cardNumber ?: ""
        DocumentType.AADHAAR_CARD    -> document.aadhaarNumber ?: ""
        DocumentType.PAN_CARD        -> document.panNumber ?: ""
        DocumentType.DRIVERS_LICENSE -> document.dlNumber ?: ""
        DocumentType.VEHICLE_RC      -> document.rcNumber ?: ""
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
            DocumentType.PAN_CARD     -> "••••• " + rawPrimaryNumber.takeLast(4)
            else -> "••••••••••••"
        }
    }

    Scaffold(containerColor = CinemaBase) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaBase)
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {

            // ── Custom Header ────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(CinemaElevated, CinemaBase)
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Back button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CinemaSurface)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary, modifier = Modifier.size(20.dp))
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Type pill + title
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
                        Text(document.title, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = TextPrimary, maxLines = 1)
                    }

                    // Action icons
                    Row {
                        IconButton(
                            onClick = { isRevealed = !isRevealed },
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(CinemaSurface)
                        ) {
                            Icon(
                                if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle", tint = TextSecondary, modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = onEditClicked,
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accentColor.copy(alpha = 0.15f))
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = accentColor, modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(
                            onClick = { showDeleteConfirm = true },
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(AccentRed.copy(alpha = 0.12f))
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = AccentRed, modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // ── Card Mockup ──────────────────────────────────────────
                GlassmorphicCard(gradient = gradient, glowColor = gradient.first().copy(alpha = 0.45f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color.White.copy(alpha = 0.10f))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(typeLabel.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White.copy(alpha = 0.8f))
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(document.title, fontSize = 19.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        }
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.White.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(typeIcon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Primary number with copy icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = displayedPrimaryNumber,
                            fontSize = if (document.type == DocumentType.PAYMENT_CARD) 19.sp else 17.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.5.sp,
                            color = Color.White,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { copyToClipboard(context, primaryNumberLabel, rawPrimaryNumber) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    // Footer: name + validity
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text("NAME", fontSize = 8.sp, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.sp)
                            Text(
                                text = when (document.type) {
                                    DocumentType.PAYMENT_CARD    -> document.cardholderName ?: ""
                                    DocumentType.AADHAAR_CARD    -> document.aadhaarName ?: ""
                                    DocumentType.PAN_CARD        -> document.panName ?: ""
                                    DocumentType.DRIVERS_LICENSE -> document.dlHolderName ?: ""
                                    DocumentType.VEHICLE_RC      -> document.rcOwnerName ?: ""
                                },
                                fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White
                            )
                        }
                        val validityDate = when (document.type) {
                            DocumentType.PAYMENT_CARD    -> document.cardExpiry
                            DocumentType.DRIVERS_LICENSE -> document.dlExpiry
                            DocumentType.VEHICLE_RC      -> document.rcExpiry
                            else -> null
                        }
                        if (validityDate != null) {
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    if (document.type == DocumentType.PAYMENT_CARD) "EXPIRES" else "VALID UNTIL",
                                    fontSize = 8.sp, color = Color.White.copy(alpha = 0.55f), letterSpacing = 1.sp
                                )
                                Text(validityDate, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                            }
                        }
                    }
                }

                // ── Document Attachment Preview ───────────────────────────
                document.imagePath?.let { path ->
                    val isPdf = path.endsWith(".pdf")
                    Spacer(modifier = Modifier.height(24.dp))

                    // Section header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(3.dp, 14.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isPdf) "Document Preview" else "Scan Preview",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            letterSpacing = 0.3.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(CinemaElevated)
                            .border(0.8.dp, CinemaStroke, RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImageLoading) {
                            CircularProgressIndicator(color = accentColor, strokeWidth = 2.dp)
                        } else if (decryptedBitmap != null) {
                            if (isRevealed) {
                                Image(
                                    bitmap = decryptedBitmap!!.asImageBitmap(),
                                    contentDescription = "Document Scan",
                                    modifier = Modifier.fillMaxSize().padding(8.dp),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(24.dp)) {
                                    Icon(Icons.Default.VisibilityOff, contentDescription = null, tint = TextMuted, modifier = Modifier.size(40.dp))
                                    Spacer(modifier = Modifier.height(10.dp))
                                    Text("Scan hidden", color = TextSecondary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Text("Tap the eye icon to reveal", color = TextMuted, fontSize = 11.sp, textAlign = TextAlign.Center)
                                }
                            }
                        } else {
                            Text("Could not load preview", color = TextMuted, fontSize = 12.sp)
                        }
                    }

                    if (isPdf) {
                        Spacer(modifier = Modifier.height(10.dp))
                        OutlinedButton(
                            onClick = { openPdfAttachment(context, path, onLoadImage) },
                            modifier = Modifier.fillMaxWidth().height(46.dp),
                            shape = RoundedCornerShape(12.dp),
                            border = androidx.compose.foundation.BorderStroke(0.8.dp, accentColor.copy(alpha = 0.5f)),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor)
                        ) {
                            Icon(Icons.Default.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Open Full PDF", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                        }
                    }
                }

                // ── All Fields Section ───────────────────────────────────
                Spacer(modifier = Modifier.height(28.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Document Details", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary, letterSpacing = 0.3.sp)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(CinemaElevated)
                        .border(0.8.dp, CinemaStroke, RoundedCornerShape(16.dp))
                ) {
                    DetailRow(label = "Title", value = document.title, context = context, accentColor = accentColor)

                    when (document.type) {
                        DocumentType.PAYMENT_CARD -> {
                            DetailRow("Card Network", document.cardType ?: "", context, accentColor = accentColor)
                            DetailRow("Cardholder Name", document.cardholderName ?: "", context, accentColor = accentColor)
                            DetailRow("Card Number", document.cardNumber ?: "", context, isSensitive = true, isRevealed = isRevealed, accentColor = accentColor)
                            DetailRow("Expiry Date", document.cardExpiry ?: "", context, accentColor = accentColor)
                            DetailRow("CVV", document.cardCvv ?: "", context, isSensitive = true, isRevealed = isRevealed, accentColor = accentColor)
                        }
                        DocumentType.AADHAAR_CARD -> {
                            DetailRow("Full Name", document.aadhaarName ?: "", context, accentColor = accentColor)
                            DetailRow("Aadhaar Number", document.aadhaarNumber ?: "", context, isSensitive = true, isRevealed = isRevealed, accentColor = accentColor)
                            DetailRow("Date of Birth", document.aadhaarDob ?: "", context, accentColor = accentColor)
                            DetailRow("Gender", document.aadhaarGender ?: "", context, accentColor = accentColor)
                        }
                        DocumentType.PAN_CARD -> {
                            DetailRow("Full Name", document.panName ?: "", context, accentColor = accentColor)
                            DetailRow("PAN Number", document.panNumber ?: "", context, isSensitive = true, isRevealed = isRevealed, accentColor = accentColor)
                            DetailRow("Father's Name", document.panFatherName ?: "", context, accentColor = accentColor)
                            DetailRow("Date of Birth", document.panDob ?: "", context, accentColor = accentColor)
                        }
                        DocumentType.DRIVERS_LICENSE -> {
                            DetailRow("Holder Name", document.dlHolderName ?: "", context, accentColor = accentColor)
                            DetailRow("Licence Number", document.dlNumber ?: "", context, accentColor = accentColor)
                            DetailRow("Date of Birth", document.dlDob ?: "", context, accentColor = accentColor)
                            DetailRow("Expiry Date", document.dlExpiry ?: "", context, accentColor = accentColor)
                            DetailRow("Issuing State", document.dlState ?: "", context, accentColor = accentColor)
                        }
                        DocumentType.VEHICLE_RC -> {
                            DetailRow("Registration Number", document.rcNumber ?: "", context, accentColor = accentColor)
                            DetailRow("Registered Owner", document.rcOwnerName ?: "", context, accentColor = accentColor)
                            DetailRow("Chassis Number", document.rcChassisNumber ?: "", context, accentColor = accentColor)
                            DetailRow("Engine Number", document.rcEngineNumber ?: "", context, accentColor = accentColor)
                            DetailRow("Registration Expiry", document.rcExpiry ?: "", context, accentColor = accentColor)
                        }
                    }
                }

                // ── OCR Raw Text ─────────────────────────────────────────
                document.ocrText?.let { ocr ->
                    Spacer(modifier = Modifier.height(24.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.size(3.dp, 14.dp).clip(RoundedCornerShape(2.dp)).background(accentColor))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Raw OCR Text", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        }
                        IconButton(onClick = { copyToClipboard(context, "OCR Text", ocr) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = accentColor, modifier = Modifier.size(16.dp))
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 180.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(CinemaSurface)
                            .border(0.8.dp, CinemaStroke, RoundedCornerShape(14.dp))
                            .padding(14.dp)
                    ) {
                        val ocrScroll = rememberScrollState()
                        Column(modifier = Modifier.verticalScroll(ocrScroll)) {
                            Text(ocr, fontSize = 12.sp, fontFamily = FontFamily.Monospace, color = TextSecondary, lineHeight = 17.sp)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }

    // ── Delete Dialog ────────────────────────────────────────────────────────
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete permanently?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "This will erase '${document.title}' and its encrypted file from your device. This cannot be undone.",
                    color = TextSecondary, lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = { onDelete(); showDeleteConfirm = false }) {
                    Text("Delete", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CinemaElevated,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ── Detail Row Component ─────────────────────────────────────────────────────
@Composable
fun DetailRow(
    label: String,
    value: String,
    context: Context,
    isSensitive: Boolean = false,
    isRevealed: Boolean = false,
    accentColor: Color = AccentIndigo,
    isLast: Boolean = false
) {
    if (value.isEmpty()) return

    val displayedValue = if (isSensitive && !isRevealed) "••••••••••" else value

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { copyToClipboard(context, label, value) }
            .padding(horizontal = 18.dp, vertical = 14.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label.uppercase(), fontSize = 9.sp, fontWeight = FontWeight.Bold, color = TextSecondary, letterSpacing = 1.sp)
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = displayedValue,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    fontFamily = if (isSensitive) FontFamily.Monospace else FontFamily.Default
                )
            }
            Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = TextMuted, modifier = Modifier.size(14.dp))
        }
    }
    HorizontalDivider(color = CinemaStroke, thickness = 0.5.dp, modifier = Modifier.padding(horizontal = 18.dp))
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}

private fun openPdfAttachment(context: Context, fileName: String, onLoadImage: (String) -> ByteArray?) {
    try {
        val bytes = onLoadImage(fileName) ?: run {
            Toast.makeText(context, "Could not load document", Toast.LENGTH_SHORT).show()
            return
        }
        val tempFile = File(context.cacheDir, "decrypted_document.pdf").apply {
            writeBytes(bytes); deleteOnExit()
        }
        val uri = FileProvider.getUriForFile(context, "com.shariq.vault.fileprovider", tempFile)
        MainActivity.isLaunchingSystemIntent = true
        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or android.content.Intent.FLAG_ACTIVITY_NO_HISTORY
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "No PDF app available", Toast.LENGTH_SHORT).show()
    }
}
