package com.shariq.vault

import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.shariq.vault.data.VaultRepository
import com.shariq.vault.model.DocumentType
import com.shariq.vault.model.VaultDocument
import com.shariq.vault.ui.screens.AddDocumentScreen
import com.shariq.vault.ui.screens.MainScreen
import com.shariq.vault.ui.screens.ViewDocumentScreen
import com.shariq.vault.ui.theme.CyberCyan
import com.shariq.vault.ui.theme.DarkSurface
import com.shariq.vault.ui.theme.ObsidianBlack
import com.shariq.vault.ui.theme.VaultTheme
import java.util.concurrent.Executor

enum class Screen {
    Dashboard,
    AddDocument,
    ViewDocument
}

class MainActivity : FragmentActivity() {

    private lateinit var executor: Executor
    private lateinit var biometricPrompt: BiometricPrompt
    private lateinit var promptInfo: BiometricPrompt.PromptInfo

    // State indicating whether the user has successfully unlocked the vault
    private val isUnlocked = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Setup Biometric Auth
        executor = ContextCompat.getMainExecutor(this)
        biometricPrompt = BiometricPrompt(this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    Toast.makeText(applicationContext, "Authentication error: $errString", Toast.LENGTH_SHORT).show()
                }

                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isUnlocked.value = true
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    Toast.makeText(applicationContext, "Authentication failed", Toast.LENGTH_SHORT).show()
                }
            })

        promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Vault")
            .setSubtitle("Authenticate to access your private documents")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        // Launch biometric prompt immediately
        triggerBiometricUnlock()

        setContent {
            VaultTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = ObsidianBlack
                ) {
                    val unlocked by isUnlocked

                    if (unlocked) {
                        VaultApp()
                    } else {
                        LockScreen(onUnlockClick = { triggerBiometricUnlock() })
                    }
                }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // Auto-lock only if we are not launching a system intent (like camera/gallery picker)
        if (!isLaunchingSystemIntent) {
            isUnlocked.value = false
        }
    }

    override fun onResume() {
        super.onResume()
        // Reset the flag upon returning to the app
        isLaunchingSystemIntent = false
    }

    private fun triggerBiometricUnlock() {
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            e.printStackTrace()
            // In case biometric prompt fails to start (e.g. no screen lock is set up), 
            // you might want to show a warning, but for security, keep it locked.
            Toast.makeText(this, "Security setup missing or incompatible. Please set up screen lock.", Toast.LENGTH_LONG).show()
        }
    }

    @Composable
    fun LockScreen(onUnlockClick: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(ObsidianBlack)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(DarkSurface),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = CyberCyan,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "VAULT SECURED",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your sensitive documents are locally encrypted.\nPlease authenticate using your fingerprint or device screen lock to gain entry.",
                fontSize = 14.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center,
                lineHeight = 20.sp
            )

            Spacer(modifier = Modifier.height(48.dp))

            Button(
                onClick = onUnlockClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = CyberCyan,
                    contentColor = ObsidianBlack
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Fingerprint,
                    contentDescription = "Unlock"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "TAP TO UNLOCK",
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }
    }

    @Composable
    fun VaultApp() {
        val repository = remember { VaultRepository(applicationContext) }
        val documents = remember { mutableStateListOf<VaultDocument>() }
        
        // Initial load
        LaunchedEffect(Unit) {
            documents.clear()
            documents.addAll(repository.loadDocuments())
        }

        var currentScreen by remember { mutableStateOf(Screen.Dashboard) }
        var selectedDocId by remember { mutableStateOf<String?>(null) }
        var selectedDocTypeToAdd by remember { mutableStateOf<DocumentType?>(null) }
        var selectedDocToEdit by remember { mutableStateOf<VaultDocument?>(null) }

        // Back navigation interceptor
        BackHandler(enabled = currentScreen != Screen.Dashboard) {
            selectedDocToEdit = null
            currentScreen = Screen.Dashboard
        }

        when (currentScreen) {
            Screen.Dashboard -> {
                MainScreen(
                    documents = documents,
                    onAddDocumentClicked = { type ->
                        selectedDocTypeToAdd = type
                        currentScreen = Screen.AddDocument
                    },
                    onDocumentClicked = { id ->
                        selectedDocId = id
                        currentScreen = Screen.ViewDocument
                    },
                    onDeleteDocument = { id ->
                        repository.deleteDocument(id)
                        documents.clear()
                        documents.addAll(repository.loadDocuments())
                    },
                    onMoveDocument = { id, moveUp ->
                        val index = documents.indexOfFirst { it.id == id }
                        if (index != -1) {
                            val newIndex = if (moveUp) index - 1 else index + 1
                            if (newIndex in 0 until documents.size) {
                                val item = documents.removeAt(index)
                                documents.add(newIndex, item)
                                repository.saveDocuments(documents)
                            }
                        }
                    }
                )
            }
            Screen.AddDocument -> {
                selectedDocTypeToAdd?.let { type ->
                    AddDocumentScreen(
                        documentType = type,
                        documentToEdit = selectedDocToEdit,
                        onLoadAttachment = { path -> repository.loadDecryptedImage(path) },
                        onSave = { doc, imageBytes, extension ->
                            var finalDoc = doc
                            val docToEdit = selectedDocToEdit
                            
                            if (imageBytes != null && extension != null) {
                                docToEdit?.imagePath?.let { oldPath ->
                                    repository.deleteEncryptedImage(oldPath)
                                }
                                docToEdit?.backImagePath?.let { oldPath ->
                                    repository.deleteEncryptedImage(oldPath)
                                }
                                val imgFileName = "file_${java.util.UUID.randomUUID()}.$extension"
                                repository.saveEncryptedImage(imgFileName, imageBytes)
                                finalDoc = finalDoc.copy(imagePath = imgFileName, backImagePath = null)
                            } else if (docToEdit != null && docToEdit.imagePath != null && imageBytes == null && extension == null) {
                                docToEdit.imagePath?.let { oldPath ->
                                    repository.deleteEncryptedImage(oldPath)
                                }
                                docToEdit.backImagePath?.let { oldPath ->
                                    repository.deleteEncryptedImage(oldPath)
                                }
                                finalDoc = finalDoc.copy(imagePath = null, backImagePath = null)
                            } else if (docToEdit != null) {
                                finalDoc = finalDoc.copy(imagePath = docToEdit.imagePath, backImagePath = docToEdit.backImagePath)
                            }

                            if (docToEdit != null) {
                                repository.updateDocument(finalDoc)
                            } else {
                                repository.addDocument(finalDoc)
                            }

                            selectedDocToEdit = null
                            documents.clear()
                            documents.addAll(repository.loadDocuments())
                            currentScreen = Screen.Dashboard
                        },
                        onBack = {
                            selectedDocToEdit = null
                            currentScreen = Screen.Dashboard
                        }
                    )
                }
            }
            Screen.ViewDocument -> {
                selectedDocId?.let { id ->
                    val doc = documents.find { it.id == id }
                    if (doc != null) {
                        ViewDocumentScreen(
                            document = doc,
                            onLoadImage = { path -> repository.loadDecryptedImage(path) },
                            onEditClicked = {
                                selectedDocToEdit = doc
                                selectedDocTypeToAdd = doc.type
                                currentScreen = Screen.AddDocument
                            },
                            onBack = {
                                currentScreen = Screen.Dashboard
                            },
                            onDelete = {
                                repository.deleteDocument(id)
                                documents.clear()
                                documents.addAll(repository.loadDocuments())
                                currentScreen = Screen.Dashboard
                            }
                        )
                    } else {
                        currentScreen = Screen.Dashboard
                    }
                }
            }
        }
    }

    companion object {
        var isLaunchingSystemIntent = false
    }
}
