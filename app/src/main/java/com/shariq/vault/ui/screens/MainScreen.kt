package com.shariq.vault.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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

    // Categories definition
    val categories = listOf("All", "Cards", "IDs", "Vehicle")

    // Filter logic
    val filteredDocuments by remember(searchQuery, selectedCategory) {
        derivedStateOf {
            documents.filter { doc ->
                // Search filter
                val matchesSearch = doc.title.contains(searchQuery, ignoreCase = true) ||
                        (doc.cardNumber?.contains(searchQuery) ?: false) ||
                        (doc.aadhaarNumber?.contains(searchQuery) ?: false) ||
                        (doc.panNumber?.contains(searchQuery) ?: false) ||
                        (doc.dlNumber?.contains(searchQuery) ?: false) ||
                        (doc.rcNumber?.contains(searchQuery) ?: false)

                // Category filter
                val matchesCategory = when (selectedCategory) {
                    "All" -> true
                    "Cards" -> doc.type == DocumentType.PAYMENT_CARD
                    "IDs" -> doc.type == DocumentType.AADHAAR_CARD || doc.type == DocumentType.PAN_CARD || doc.type == DocumentType.DRIVERS_LICENSE
                    "Vehicle" -> doc.type == DocumentType.VEHICLE_RC
                    else -> true
                }

                matchesSearch && matchesCategory
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock",
                            tint = CyberCyan,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "VAULT",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = ObsidianBlack
                ),
                actions = {
                    IconButton(onClick = { /* Could trigger app lock manual lock */ }) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = "Secured",
                            tint = MintGreen
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Box(contentAlignment = Alignment.BottomEnd) {
                // Dynamic rotating speed dial equivalent
                FloatingActionButton(
                    onClick = { showAddMenu = !showAddMenu },
                    containerColor = CyberCyan,
                    contentColor = ObsidianBlack,
                    shape = CircleShape
                ) {
                    Icon(
                        imageVector = if (showAddMenu) Icons.Default.Close else Icons.Default.Add,
                        contentDescription = "Add Document",
                        modifier = Modifier.size(28.dp)
                    )
                }

                // Add Type selection popup menu
                DropdownMenu(
                    expanded = showAddMenu,
                    onDismissRequest = { showAddMenu = false },
                    modifier = Modifier
                        .background(DarkSurface)
                        .width(220.dp)
                ) {
                    DropdownMenuItem(
                        text = { Text("💳 Credit / Debit Card", color = Color.White) },
                        onClick = {
                            showAddMenu = false
                            onAddDocumentClicked(DocumentType.PAYMENT_CARD)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🪪 Aadhaar Card", color = Color.White) },
                        onClick = {
                            showAddMenu = false
                            onAddDocumentClicked(DocumentType.AADHAAR_CARD)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🛡️ PAN Card", color = Color.White) },
                        onClick = {
                            showAddMenu = false
                            onAddDocumentClicked(DocumentType.PAN_CARD)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🪪 Driver's License", color = Color.White) },
                        onClick = {
                            showAddMenu = false
                            onAddDocumentClicked(DocumentType.DRIVERS_LICENSE)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("🚗 Vehicle RC", color = Color.White) },
                        onClick = {
                            showAddMenu = false
                            onAddDocumentClicked(DocumentType.VEHICLE_RC)
                        }
                    )
                }
            }
        },
        containerColor = ObsidianBlack
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
        ) {
            // Search Input
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search encrypted items...", color = TextSecondary) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = TextSecondary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = CyberCyan,
                    unfocusedBorderColor = BorderGray,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            )

            // Categories horizontal bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { category ->
                    val isSelected = selectedCategory == category
                    InputChip(
                        selected = isSelected,
                        onClick = { selectedCategory = category },
                        label = { Text(category) },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = DarkSurface,
                            selectedContainerColor = CyberCyan,
                            labelColor = TextSecondary,
                            selectedLabelColor = ObsidianBlack
                        ),
                        border = InputChipDefaults.inputChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderGray,
                            selectedBorderColor = CyberCyan,
                            borderWidth = 1.dp
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main List
            if (filteredDocuments.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.EnhancedEncryption,
                            contentDescription = "Empty Vault",
                            tint = BorderGray,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No matching records" else "Your Vault is Empty",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try refining your search terms." else "Tap the '+' button below to securely store your drivers licence, PAN, Aadhaar, RC, or debit/credit cards.",
                            color = TextSecondary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredDocuments, key = { it.id }) { doc ->
                        DocumentItem(
                            document = doc,
                            onClick = { onDocumentClicked(doc.id) },
                            onLongClick = { documentToDelete = doc }
                        )
                    }
                }
            }
        }
    }

    // Delete Confirmation Dialog
    documentToDelete?.let { doc ->
        AlertDialog(
            onDismissRequest = { documentToDelete = null },
            title = { Text("Delete Document", color = Color.White) },
            text = { Text("Are you sure you want to permanently delete '${doc.title}'? This action is local, irreversible, and the encrypted data will be wiped.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteDocument(doc.id)
                        documentToDelete = null
                    }
                ) {
                    Text("Delete", color = CyberPink)
                }
            },
            dismissButton = {
                TextButton(onClick = { documentToDelete = null }) {
                    Text("Cancel", color = Color.White)
                }
            },
            containerColor = DarkSurface
        )
    }
}

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
        DocumentType.VEHICLE_RC -> Icons.Default.CarRental
    }

    val maskedDetails = when (document.type) {
        DocumentType.PAYMENT_CARD -> formatCardNumber(document.cardNumber)
        DocumentType.AADHAAR_CARD -> formatAadhaarNumber(document.aadhaarNumber)
        DocumentType.PAN_CARD -> formatPanNumber(document.panNumber)
        DocumentType.DRIVERS_LICENSE -> document.dlNumber ?: ""
        DocumentType.VEHICLE_RC -> document.rcNumber ?: ""
    }

    val typeLabel = when (document.type) {
        DocumentType.PAYMENT_CARD -> document.cardType ?: "Payment Card"
        DocumentType.AADHAAR_CARD -> "Aadhaar Card"
        DocumentType.PAN_CARD -> "PAN Card"
        DocumentType.DRIVERS_LICENSE -> "Driver's License"
        DocumentType.VEHICLE_RC -> "Vehicle RC"
    }

    GlassmorphicCard(
        gradient = gradient,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = typeLabel.uppercase(),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.5.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = document.title,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = typeIcon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier
                    .size(40.dp)
                    .padding(4.dp)
            )
        }

        Spacer(modifier = Modifier.height(28.dp))

        if (document.type == DocumentType.PAYMENT_CARD) {
            // Payment Card visual representation
            Text(
                text = maskedDetails,
                fontSize = 20.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 2.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "CARDHOLDER",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = document.cardholderName ?: "Holder Name",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "EXPIRES",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = document.cardExpiry ?: "MM/YY",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
            }
        } else {
            // ID Card style visual representation
            Text(
                text = maskedDetails,
                fontSize = 18.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "NAME",
                        fontSize = 9.sp,
                        color = Color.White.copy(alpha = 0.6f)
                    )
                    Text(
                        text = when (document.type) {
                            DocumentType.AADHAAR_CARD -> document.aadhaarName ?: ""
                            DocumentType.PAN_CARD -> document.panName ?: ""
                            DocumentType.DRIVERS_LICENSE -> document.dlHolderName ?: ""
                            DocumentType.VEHICLE_RC -> document.rcOwnerName ?: ""
                            else -> ""
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (document.dlExpiry != null || document.rcExpiry != null) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "VALID UNTIL",
                            fontSize = 9.sp,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                        Text(
                            text = document.dlExpiry ?: document.rcExpiry ?: "N/A",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}

// Helpers for masking card and ID numbers
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
