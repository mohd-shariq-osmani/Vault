package com.shariq.vault.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

class CryptoManager {

    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply {
        load(null)
    }

    private fun getKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        return existingKey?.secretKey ?: generateKey()
    }

    private fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER
        )
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(KEY_SIZE_BITS)
            .build()
        
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    private fun getEncryptCipher(): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.ENCRYPT_MODE, getKey())
        }
    }

    private fun getDecryptCipher(iv: ByteArray): Cipher {
        return Cipher.getInstance(TRANSFORMATION).apply {
            init(Cipher.DECRYPT_MODE, getKey(), GCMParameterSpec(TAG_SIZE_BITS, iv))
        }
    }

    /**
     * Encrypts the input data.
     * Output format: [IV size (4 bytes)] + [IV bytes] + [Ciphertext]
     */
    @Synchronized
    fun encrypt(data: ByteArray): ByteArray {
        val cipher = getEncryptCipher()
        val ciphertext = cipher.doFinal(data)
        val iv = cipher.iv

        val buffer = java.nio.ByteBuffer.allocate(4 + iv.size + ciphertext.size)
        buffer.putInt(iv.size)
        buffer.put(iv)
        buffer.put(ciphertext)
        return buffer.array()
    }

    /**
     * Decrypts the combined input data.
     * Input format: [IV size (4 bytes)] + [IV bytes] + [Ciphertext]
     */
    @Synchronized
    fun decrypt(encryptedData: ByteArray): ByteArray {
        val buffer = java.nio.ByteBuffer.wrap(encryptedData)
        val ivSize = buffer.int
        if (ivSize <= 0 || ivSize > 100) {
            throw IllegalArgumentException("Invalid IV size in encrypted data: $ivSize")
        }
        val iv = ByteArray(ivSize)
        buffer.get(iv)
        val ciphertext = ByteArray(buffer.remaining())
        buffer.get(ciphertext)

        val cipher = getDecryptCipher(iv)
        return cipher.doFinal(ciphertext)
    }

    companion object {
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val KEY_ALIAS = "vault_encryption_key"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val KEY_SIZE_BITS = 256
        private const val TAG_SIZE_BITS = 128
    }
}
