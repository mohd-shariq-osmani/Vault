# Secure Document Vault 🔒

Vault is a modern, premium, secure local document vault application built with **Flutter**. It allows you to store private personal documents (Payment Cards, Aadhaar Cards, PAN Cards, Driving Licenses, Vehicle RCs, and Generic ID Cards) locally on your device with hardware-backed, military-grade AES-256-GCM local encryption.

Vault is designed for **100% offline privacy**—it requires zero network permissions, ensuring your sensitive data never leaves your device.

---

## Key Features

*   **AES-256-GCM Local Encryption**: Serializes all document details and local scans using synchronous AES-256-GCM encryption. Cryptographic keys are managed securely by the local platform secure storage (Android Keystore / iOS Keychain).
*   **Biometric Access Control**: Strict biometric (fingerprint/FaceID) lock screen on app startup. Safe bypass fallback triggers if device-level lock credentials are completely turned off or disabled.
*   **Integrated Multi-Page Document Scanner (Mobile)**: Native integration with the Google Play services Document Scanner API. It automatically detects edges, crops, aligns, and scans multiple pages into high-res PDF attachments.
*   **Multi-Format File Picker (PDFs & Images)**: Supports uploading multiple images and PDF files. PDFs are rendered page-by-page as visual previews and can be re-compiled into single attachments.
*   **Heuristic OCR & Autofill**: Offline text parser powered by Google ML Kit. Automatically extracts cardholders, card numbers, CVVs, names, father's names, dates of birth, and expiry dates to auto-fill forms.
*   **Interactive Date Selectors**: Input fields for dates (DOBs, Expiries) are read-only to avoid typos, and open neat native visual Date Picker dialog overlays.
*   **Decrypted Previews & Share Option**: View visual pages of attachments, or decrypt them on-the-fly to open in native external system viewers. Automatic clipboard copy + system sharing copies all document fields in one click.
*   **Premium Obsidian Theme**: Styled in a dark glassmorphic cinema theme featuring deep obsidian black surfaces, indigo highlights, and custom categories navigation.

---

## Project Structure

```text
lib/
├── main.dart                    # Biometric lock gate, app theme, and screen router
├── data/
│   ├── crypto_manager.dart      # AES-GCM secure storage wrapper
│   └── vault_repository.dart    # Encrypted files database (load/save/delete/reorder)
├── models/
│   └── document.dart            # JSON Serializable document models
├── providers/
│   ├── auth_provider.dart       # Enforced LocalAuthentication state controller
│   └── vault_provider.dart      # Vault documents Riverpod state notifier
├── utils/
│   ├── number_formatters.dart   # Credit Card, Expiry, and Aadhaar input formatters
│   └── ocr_autofill.dart        # Text recognition line filters and regex heuristics
├── ui/
│   ├── theme/                   # Cinema colors palette & Inter typography
│   ├── widgets/                 # Glassmorphic containers, details row, and items list
│   └── screens/
│       ├── lock_screen.dart     # Pulsing Fingerprint scanner lock gate
│       ├── main_screen.dart     # Obsidian dashboard with chip filters and reorder buttons
│       ├── add_document_screen.dart # Document attachments scanner, PDF splitted picker, forms
│       └── view_document_screen.dart # Decrypted PDF viewer, details copier, system sharing
```

---

## Installation & Setup

Ensure you have your Flutter environment configured (`flutter --version` >= 3.4.0).

### 1. Resolve Dependencies
From the project root directory, run:
```bash
flutter pub get
```

### 2. Android Setup & Launch
1. Ensure USB Debugging or WiFi debugging is enabled on your device.
2. Build and run:
   ```bash
   flutter run
   ```
   Or build a debug APK:
   ```bash
   flutter build apk --debug
   ```

### 3. iOS Setup & Launch (Xcode)
1. Install iOS pod dependencies:
   ```bash
   cd ios
   pod install
   ```
2. Open the project workspace in Xcode:
   ```bash
   open ios/Runner.xcworkspace
   ```
3. Set your Development Team under **Runner > Signing & Capabilities**.
4. Run the app on your iPhone target.

ℹ️ **iOS 14+ JIT Launch Warning**:
If you build in **Debug** mode and try to open the app directly from your iPhone home screen, iOS security will show a JIT startup screen.
- To test the debug build, run it attached to your debugger via Xcode or Terminal (`flutter run`).
- To launch the app standalone from your iPhone home screen, build it in **Release Mode** (In Xcode: Set **Product > Scheme > Edit Scheme... > Run > Build Configuration** to `Release` / In Terminal: `flutter run --release`).

⚠️ **Xcode Build Error: `Command PhaseScriptExecution failed with a nonzero exit code`**
If you encounter this error during compilation:
*   **Disable User Script Sandboxing (Xcode 15+)**:
    1. Select the **Runner** project in the Xcode sidebar.
    2. Go to the **Build Settings** tab.
    3. Search for **User Script Sandboxing** (`ENABLE_USER_SCRIPT_SANDBOXING`).
    4. Set it to **No** (this is already configured in the code, but verify if overridden).
*   **Configure Local Flutter Path**:
    If Xcode cannot locate your Flutter path, create `ios/.xcode.env.local` and add:
    ```bash
    export FLUTTER_ROOT="/opt/homebrew/share/flutter" # Adjust to your Flutter SDK path
    ```

### 4. macOS Setup & Launch
1. Enable macOS desktop support:
   ```bash
   flutter config --enable-macos-desktop
   ```
2. Build and run:
   ```bash
   flutter run -d macos
   ```

---

## Security & Privacy Policy

*   **Zero Internet Permission**: The app declares no network permissions, preventing any telemetry, tracking, or credentials leakage.
*   **Decryption-in-Memory**: Documents are decrypted on-the-fly inside local volatile RAM, and temporary files generated for preview are deleted immediately upon exit.
