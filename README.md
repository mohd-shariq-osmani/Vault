# Vault 🔒

Vault is a modern, premium, secure Android application built with Jetpack Compose. It allows you to store your private personal documents (Aadhaar Cards, PAN Cards, Driving Licenses, Vehicle RCs, Credit/Debit cards) locally on your device with hardware-backed encryption. 

Vault is designed for **100% offline privacy**—it requires zero network permissions, ensuring your sensitive data never leaves your device.

---

## Key Features

*   **Hardware-Backed Encryption**: Vault serializes all document details and local scans using AES-256-GCM encryption. The cryptographic keys are managed securely by the Android Keystore system.
*   **Biometric Access Control**: Prompt biometric (fingerprint/face) unlock screen on startup. The app automatically locks itself when paused, backgrounded, or when the screen is turned off.
*   **Integrated Document Scanner**: Native integration with the Google Play services Document Scanner API. It automatically detects document edges, crops, aligns, and scans card layouts directly into high-res PDF files.
*   **Smart Offline OCR & Auto-Fill**: Parses scanned text offline using Google ML Kit. It uses smart line-cleansing and alphabetical heuristics to extract names, father's names, dates of birth, and card numbers, auto-filling input fields in real time.
*   **Visual PDF & Image Previews**: View decrypted image scans or native visual previews of PDF files (rendering Page 1 inline) instantly upon opening. PDF attachments can be decrypted on-the-fly and launched in external system viewers.
*   **Dynamic Obsidian Theme**: Styled in a modern glassmorphic theme featuring deep obsidian black surfaces, cyberpunk cyan accents, and glowing, borderless status cards.

---

## Tech Stack & Architecture

*   **UI Framework**: Jetpack Compose (Kotlin-first declarative UI)
*   **Database / Storage**: Encrypted File System JSON Serialization & Binary File Storage
*   **Cryptography**: `androidx.security:security-crypto` & Android Keystore API (AES-GCM-256)
*   **Machine Learning (OCR)**: Google ML Kit Text Recognition (`com.google.mlkit:text-recognition`)
*   **Scanning UI**: Google Play services Document Scanner (`play-services-mlkit-document-scanner`)
*   **Build System**: Gradle Kotlin DSL (`.gradle.kts`) targeting Android SDK 35 (Java 17)

---

## Installation & Setup

1.  **Clone the Repository**:
    ```bash
    git clone https://github.com/mohd-shariq-osmani/Vault.git
    cd Vault
    ```
2.  **Open in Android Studio**:
    *   Select **File > Open** and choose the `Vault` folder.
    *   Allow Gradle to sync and download the Play Services dependencies.
3.  **Run the Application**:
    *   Connect your Android device via USB/Wi-Fi debugging.
    *   Build and deploy the debug package (`app-debug.apk`).
    *   Ensure your device has at least one fingerprint or screen lock set up for biometric authentication.

---

## Project Structure

```text
app/src/main/
├── AndroidManifest.xml          # Sandboxed manifest requesting biometric & camera access
├── java/com/shariq/vault/
│   ├── MainActivity.kt          # Biometric lifecycle integration & screen navigation router
│   ├── data/
│   │   └── VaultRepository.kt   # Local load/save repository for encrypted files
│   ├── model/
│   │   └── Document.kt          # Data models mapping card details and attachments
│   ├── security/
│   │   └── CryptoManager.kt     # Hardware keystore wrapper for AES-GCM encryption
│   └── ui/
│       ├── theme/               # Obsidian Black and Cyber Cyan colors & typography
│       ├── components/          # Custom Glassmorphic container widget
│       └── screens/
│           ├── MainScreen.kt    # Dashboard grid with search and category filters
│           ├── AddDocumentScreen.kt # Scan inputs, OCR prefill heuristics, scanner launchers
│           └── ViewDocumentScreen.kt # Details listing, decrypted previews, PDF viewer launcher
└── res/
    ├── mipmap-*/                # Custom Launcher icons
    └── xml/                     # FileProvider sharing configs and secure backup rules
```

---

## Security & Privacy Policy

*   **Zero Internet Permission**: The `AndroidManifest.xml` declares **no network permission** (`android.permission.INTERNET`). The app cannot communicate with any remote servers, preventing telemetry or credentials leak.
*   **Decryption-in-Memory**: Encrypted scans and PDF attachments are decrypted on-the-fly inside local volatile RAM, and temporary files generated for system previews are instantly marked to delete on exit.
