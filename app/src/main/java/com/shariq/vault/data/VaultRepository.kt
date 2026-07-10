package com.shariq.vault.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.shariq.vault.model.VaultDocument
import com.shariq.vault.security.CryptoManager
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.lang.reflect.Type

class VaultRepository(
    private val context: Context,
    private val cryptoManager: CryptoManager = CryptoManager()
) {
    private val gson = Gson()
    private val file: File
        get() = File(context.filesDir, FILE_NAME)

    private val listType: Type = object : TypeToken<List<VaultDocument>>() {}.type

    @Synchronized
    fun loadDocuments(): List<VaultDocument> {
        if (!file.exists()) {
            return emptyList()
        }
        return try {
            val encryptedBytes = FileInputStream(file).use { it.readBytes() }
            val decryptedBytes = cryptoManager.decrypt(encryptedBytes)
            val jsonString = String(decryptedBytes, Charsets.UTF_8)
            gson.fromJson(jsonString, listType) ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            // In case of decryption error (e.g. key invalidated or corrupt data), return empty list.
            // In production, we'd handle this more gracefully, but for a local vault it's standard.
            emptyList()
        }
    }

    @Synchronized
    fun saveDocuments(documents: List<VaultDocument>) {
        try {
            val jsonString = gson.toJson(documents)
            val jsonBytes = jsonString.toByteArray(Charsets.UTF_8)
            val encryptedBytes = cryptoManager.encrypt(jsonBytes)
            FileOutputStream(file).use { it.write(encryptedBytes) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun addDocument(document: VaultDocument) {
        val currentList = loadDocuments().toMutableList()
        currentList.removeAll { it.id == document.id } // Avoid duplicates
        currentList.add(0, document) // Prepend new items
        saveDocuments(currentList)
    }

    @Synchronized
    fun deleteDocument(documentId: String) {
        val currentList = loadDocuments().toMutableList()
        val doc = currentList.find { it.id == documentId }
        doc?.imagePath?.let { deleteEncryptedImage(it) }
        currentList.removeAll { it.id == documentId }
        saveDocuments(currentList)
    }

    @Synchronized
    fun updateDocument(document: VaultDocument) {
        val currentList = loadDocuments().toMutableList()
        val index = currentList.indexOfFirst { it.id == document.id }
        if (index != -1) {
            currentList[index] = document
            saveDocuments(currentList)
        }
    }

    @Synchronized
    fun saveEncryptedImage(fileName: String, imageBytes: ByteArray) {
        try {
            val encryptedBytes = cryptoManager.encrypt(imageBytes)
            val imageFile = File(context.filesDir, fileName)
            FileOutputStream(imageFile).use { it.write(encryptedBytes) }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @Synchronized
    fun loadDecryptedImage(fileName: String): ByteArray? {
        val imageFile = File(context.filesDir, fileName)
        if (!imageFile.exists()) return null
        return try {
            val encryptedBytes = FileInputStream(imageFile).use { it.readBytes() }
            cryptoManager.decrypt(encryptedBytes)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    @Synchronized
    fun deleteEncryptedImage(fileName: String) {
        try {
            val imageFile = File(context.filesDir, fileName)
            if (imageFile.exists()) {
                imageFile.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    companion object {
        private const val FILE_NAME = "vault_documents.bin"
    }
}
