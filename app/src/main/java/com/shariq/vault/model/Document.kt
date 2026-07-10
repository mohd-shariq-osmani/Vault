package com.shariq.vault.model

enum class DocumentType {
    DRIVERS_LICENSE,
    VEHICLE_RC,
    AADHAAR_CARD,
    PAN_CARD,
    PAYMENT_CARD
}

data class VaultDocument(
    val id: String,
    val title: String,
    val type: DocumentType,
    val dateAdded: Long = System.currentTimeMillis(),
    
    // Drivers License fields
    val dlNumber: String? = null,
    val dlHolderName: String? = null,
    val dlDob: String? = null,
    val dlExpiry: String? = null,
    val dlState: String? = null,

    // Vehicle RC fields
    val rcNumber: String? = null,
    val rcOwnerName: String? = null,
    val rcChassisNumber: String? = null,
    val rcEngineNumber: String? = null,
    val rcExpiry: String? = null,

    // Aadhaar Card fields
    val aadhaarNumber: String? = null,
    val aadhaarName: String? = null,
    val aadhaarDob: String? = null,
    val aadhaarGender: String? = null,

    // PAN Card fields
    val panNumber: String? = null,
    val panName: String? = null,
    val panFatherName: String? = null,
    val panDob: String? = null,

    // Payment Card fields (Credit/Debit)
    val cardholderName: String? = null,
    val cardNumber: String? = null,
    val cardExpiry: String? = null,
    val cardCvv: String? = null,
    val cardType: String? = null, // Visa, Mastercard, RuPay, Amex, etc.
    
    // Scanned Image and OCR fields
    val imagePath: String? = null,
    val ocrText: String? = null
)
