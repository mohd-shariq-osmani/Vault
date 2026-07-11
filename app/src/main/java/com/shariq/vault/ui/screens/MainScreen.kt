package com.shariq.vault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.shariq.vault.model.DocumentType
import com.shariq.vault.model.VaultDocument
import com.shariq.vault.ui.components.GlassmorphicCard
import com.shariq.vault.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    documents: List<VaultDocument>,
    onAddDocumentClicked: (DocumentType) -> Unit,
    onDocumentClicked: (String) -> Unit,
    onDeleteDocument: (String) -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    var showAddMenu by remember { mutableStateOf(false) }
    var documentToDelete by remember { mutableStateOf<VaultDocument?>(null) }

    data class Category(val label: String, val icon: ImageVector)
    val categories = listOf(
        Category("All", Icons.Default.GridView),
        Category("Cards", Icons.Default.CreditCard),
        Category("IDs", Icons.Default.Badge),
        Category("Vehicle", Icons.Default.DirectionsCar)
    )

    val filteredDocuments by remember(searchQuery, selectedCategory, documents.size) {
        derivedStateOf {
            documents.filter { doc ->
                val matchesSearch = searchQuery.isEmpty() ||
                        doc.title.contains(searchQuery, ignoreCase = true) ||
                        (doc.cardNumber?.contains(searchQuery) == true) ||
                        (doc.aadhaarNumber?.contains(searchQuery) == true) ||
                        (doc.panNumber?.contains(searchQuery) == true) ||
                        (doc.dlNumber?.contains(searchQuery) == true) ||
                        (doc.rcNumber?.contains(searchQuery) == true)

                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Cards" -> doc.type == DocumentType.PAYMENT_CARD
                    "IDs" -> doc.type == DocumentType.AADHAAR_CARD ||
                              doc.type == DocumentType.PAN_CARD ||
                              doc.type == DocumentType.DRIVERS_LICENSE
                    "Vehicle" -> doc.type == DocumentType.VEHICLE_RC
                    else -> true
                }
                matchesSearch && matchesCategory
            }
        }
    }

    Scaffold(
        containerColor = CinemaBase,
        floatingActionButton = {
            Box {
                FloatingActionButton(
                    onClick = { showAddMenu = !showAddMenu },
                    containerColor = AccentIndigo,
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(58.dp)
                ) {
                    Icon(
                        imageVector = if (showAddMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add Document",
                        modifier = Modifier.size(26.dp)
                    )
                }

                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    modifier = Modifier
                        .background(CinemaElevated)
                        .width(230.dp)
                ) {
                    listOf(
                        Triple(Icons.Default.CreditCard, "Credit / Debit Card", DocumentType.PAYMENT_CARD),
                        Triple(Icons.Default.Badge, "Aadhaar Card", DocumentType.AADHAAR_CARD),
                        Triple(Icons.Default.AssignmentInd, "PAN Card", DocumentType.PAN_CARD),
                        Triple(Icons.Default.DriveEta, "Driver's License", DocumentType.DRIVERS_LICENSE),
                        Triple(Icons.Default.DirectionsCar, "Vehicle RC", DocumentType.VEHICLE_RC)
                    ).forEach { (icon, label, type) ->
                        DropdownMenuItem(
                            leadingIcon = {
                                Icon(icon, contentDescription = null, tint = AccentIndigo, modifier = Modifier.size(18.dp))
                            },
                            text = {
                                Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            },
                            onClick = {
                                showAddMenu = false
                                onAddDocumentClicked(type)
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CinemaBase)
                .padding(paddingValues)
        ) {

            // ── Premium Header ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(CinemaElevated, CinemaBase)
                        )
                    )
                    .padding(horizontal = 24.dp, vertical = 20.dp)
            ) {
                // Subtle ambient blob
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .align(Alignment.TopEnd)
                        .offset(x = 20.dp, y = (-20).dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(AccentGlow, Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentGlow, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Vault",
                                tint = AccentIndigo,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "VAULT",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 4.sp,
                            color = TextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = AccentEmerald,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${documents.size} encrypted ${if (documents.size == 1) "item" else "items"} · 256-bit AES",
                            color = AccentEmerald,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Column(modifier = Modifier.padding(horizontal = 20.dp)) {

                // ── Search Bar ───────────────────────────────────────────
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search your vault…", color = TextMuted, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = "Search",
                            tint = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary, modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentIndigo,
                        unfocusedBorderColor = CinemaStroke,
                        focusedContainerColor = CinemaSurface,
                        unfocusedContainerColor = CinemaSurface,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentIndigo
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp)
                )

                // ── Category Filter Chips ────────────────────────────────
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(categories) { category ->
                        val isSelected = selectedCategory == category.label
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = category.label },
                            label = {
                                Text(
                                    category.label,
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            },
                            leadingIcon = {
                                Icon(category.icon, contentDescription = null, modifier = Modifier.size(15.dp))
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = CinemaSurface,
                                selectedContainerColor = AccentIndigo,
                                labelColor = TextSecondary,
                                selectedLabelColor = Color.White,
                                iconColor = TextSecondary,
                                selectedLeadingIconColor = Color.White
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = isSelected,
                                borderColor = CinemaStroke,
                                selectedBorderColor = AccentIndigo,
                                borderWidth = 0.8.dp,
                                selectedBorderWidth = 0.dp
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
            }

            // ── Documents List / Empty State ─────────────────────────────
            if (filteredDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(40.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .background(
                                    brush = Brush.radialGradient(
                                        colors = listOf(AccentGlow, Color.Transparent)
                                    ),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.EnhancedEncryption,
                                contentDescription = null,
                                tint = AccentIndigo.copy(alpha = 0.6f),
                                modifier = Modifier.size(52.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(20.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching records" else "Vault is empty",
                            color = TextPrimary,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty())
                                "Try different search keywords."
                            else
                                "Tap + to securely store your Aadhaar, PAN, Driving Licence, RC or payment cards.",
                            color = TextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 0.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(0.dp)) }
                    items(filteredDocuments, key = { it.id }) { doc ->
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 5 })
                        ) {
                            DocumentItem(
                                document = doc,
                                onClick = { onDocumentClicked(doc.id) },
                                onLongClick = { documentToDelete = doc }
                            )
                        }
                    }
                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }
            }
        }
    }

    // ── Delete Confirmation Dialog ───────────────────────────────────────────
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete document?", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Text(
                    "Permanently delete '${doc.title}'? The encrypted file will be wiped from local storage and cannot be recovered.",
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onDeleteDocument(doc.id)
                    documentToDelete = null
                }) {
                    Text("Delete", color = AccentRed, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CinemaElevated,
            shape = RoundedCornerShape(20.dp)
        )
    }
}

// ── Document List Item ───────────────────────────────────────────────────────
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DocumentItem(
    document: VaultDocument,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val gradient = when (document.type) {
        DocumentType.PAYMENT_CARD -> CardGradientBluePurple
        DocumentType.AADHAAR_CARD -> CardGradientEmerald
        DocumentType.PAN_CARD -> CardGradientSunset
        DocumentType.DRIVERS_LICENSE -> CardGradientDeepSpace
        DocumentType.VEHICLE_RC -> CardGradientDarkPurple
    }

    val typeIcon = when (document.type) {
        DocumentType.PAYMENT_CARD -> Icons.Default.CreditCard
        DocumentType.AADHAAR_CARD -> Icons.Default.Badge
        DocumentType.PAN_CARD -> Icons.Default.AssignmentInd
        DocumentType.DRIVERS_LICENSE -> Icons.Default.DriveEta
        DocumentType.VEHICLE_RC -> Icons.Default.DirectionsCar
    }

    val typeLabel = when (document.type) {
        DocumentType.PAYMENT_CARD -> document.cardType ?: "Payment Card"
        DocumentType.AADHAAR_CARD -> "Aadhaar Card"
        DocumentType.PAN_CARD -> "PAN Card"
        DocumentType.DRIVERS_LICENSE -> "Driver's Licence"
        DocumentType.VEHICLE_RC -> "Vehicle RC"
    }

    val maskedDetails = when (document.type) {
        DocumentType.PAYMENT_CARD -> formatCardNumber(document.cardNumber)
        DocumentType.AADHAAR_CARD -> formatAadhaarNumber(document.aadhaarNumber)
        DocumentType.PAN_CARD -> formatPanNumber(document.panNumber)
        DocumentType.DRIVERS_LICENSE -> document.dlNumber ?: ""
        DocumentType.VEHICLE_RC -> document.rcNumber ?: ""
    }

    val holderName = when (document.type) {
        DocumentType.PAYMENT_CARD -> document.cardholderName
        DocumentType.AADHAAR_CARD -> document.aadhaarName
        DocumentType.PAN_CARD -> document.panName
        DocumentType.DRIVERS_LICENSE -> document.dlHolderName
        DocumentType.VEHICLE_RC -> document.rcOwnerName
    }

    val validityDate = when (document.type) {
        DocumentType.PAYMENT_CARD -> document.cardExpiry
        DocumentType.DRIVERS_LICENSE -> document.dlExpiry
        DocumentType.VEHICLE_RC -> document.rcExpiry
        else -> null
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "cardScale"
    )

    GlassmorphicCard(
        gradient = gradient,
        glowColor = gradient.first().copy(alpha = 0.4f),
        modifier = Modifier
            .scale(scale)
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        // Header row: type badge + icon
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Type badge chip
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color.White.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = typeLabel.uppercase(),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }
            // Document type icon
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.White.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Title
        Text(
            text = document.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Masked number
        Text(
            text = maskedDetails,
            fontSize = if (document.type == DocumentType.PAYMENT_CARD) 18.sp else 16.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            letterSpacing = 2.sp,
            color = Color.White.copy(alpha = 0.90f)
        )

        Spacer(modifier = Modifier.height(18.dp))

        // Footer: holder name + validity
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            if (!holderName.isNullOrEmpty()) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "NAME",
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Text(
                        text = holderName,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            if (validityDate != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (document.type == DocumentType.PAYMENT_CARD) "EXPIRES" else "VALID UNTIL",
                        fontSize = 8.sp,
                        letterSpacing = 1.sp,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Text(
                        text = validityDate,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
            // Lock watermark at the bottom-right
            if (holderName.isNullOrEmpty() && validityDate == null) {
                Spacer(modifier = Modifier.weight(1f))
            }
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.08f),
                modifier = Modifier
                    .padding(start = 8.dp)
                    .size(28.dp)
            )
        }
    }
}

// ── Number Formatting Helpers ────────────────────────────────────────────────
private fun formatCardNumber(num: String?): String {
    if (num == null) return ""
    val cleaned = num.replace(" ", "")
    if (cleaned.length < 4) return cleaned
    return "•••• •••• •••• " + cleaned.takeLast(4)
}

private fun formatAadhaarNumber(num: String?): String {
    if (num == null) return ""
    val cleaned = num.replace(" ", "")
    if (cleaned.length < 4) return cleaned
    return "•••• •••• " + cleaned.takeLast(4)
}

private fun formatPanNumber(num: String?): String {
    if (num == null) return ""
    if (num.length < 4) return num
    return "••••• " + num.takeLast(4)
}
