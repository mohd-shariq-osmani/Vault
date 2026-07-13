# Secure Document Vault 🔒

This repository contains two implementations of the secure local document vault:
1. **Flutter Implementation (Multi-Platform)**: Located in the [`VaultFlutter/`](./VaultFlutter) folder. Supports Android, iOS, and macOS.
2. **Native Android Compose Implementation**: Located in the root folder. Supports Android.

Both applications are designed for **100% offline privacy**—requiring zero network permissions to guarantee that your sensitive documents never leave your device.

---

## 📱 Flutter Implementation (Multi-Platform)

The Flutter app is located under [`VaultFlutter/`](./VaultFlutter) and supports Android, iOS, and macOS with full AES-256-GCM local encryption.

### Key Features
*   **Biometric & Device Authentication**: Strictly enforces fingerprint/FaceID locks. If no device lock credentials are set up on the phone, it safely bypasses lockouts.
*   **Native ML Kit Document Scanner (Mobile)**: Multi-page scanning interface using the Google Play services Document Scanner API.
*   **Image & PDF Picker (Mobile/Desktop)**: Upload multi-page PDFs or images. PDFs are parsed page-by-page as image previews and can be re-compiled into single attachments.
*   **Offline Heuristic OCR**: Local OCR autofills card numbers, names, expiry dates, CVVs, and dates of birth.
*   **Platform File Previews**: Decrypts attachments on-the-fly and opens them using the native OS viewer under their custom titles (e.g. `[Title].pdf`).

### How to Install & Run

#### 1. Android Installation
1. Ensure your Flutter environment is set up.
2. Run `flutter pub get` in `VaultFlutter/`.
3. Connect your Android device (ensure Developer Options and USB debugging are enabled).
4. Run:
   ```bash
   flutter run -d <device_id>
   ```
   Or build the debug APK:
   ```bash
   flutter build apk --debug
   ```

#### 2. iOS Installation (Xcode)
1. Navigate to the `VaultFlutter/ios/` directory and install Cocoapods dependencies:
   ```bash
   cd VaultFlutter/ios
   pod install
   ```
2. Open the project workspace in Xcode:
   ```bash
   open Runner.xcworkspace
   ```
3. Connect your iPhone and select it as the run target.
4. Set your development team under **Runner > Signing & Capabilities**.
5. Build and run the app.

ℹ️ **iOS 14+ Debug JIT Launch Limit**:
If you build the app in **Debug** mode, iOS security policies block you from opening it directly from your iPhone home screen (it will show a JIT startup screen).
- To test the debug build, run it attached to your debugger via Xcode or Terminal (`flutter run`).
- To launch the app standalone from your iPhone home screen, build it in **Release Mode**:
  - **In Xcode**: Go to **Product > Scheme > Edit Scheme... > Run** and set **Build Configuration** to `Release`.
  - **In Terminal**: Run `flutter run --release`.

⚠️ **Xcode Build Error: `Command PhaseScriptExecution failed with a nonzero exit code`**
If you encounter this error during iOS compile:
*   **Disable User Script Sandboxing (Xcode 15+)**:
    1. Select the **Runner** project in the Xcode left navigation sidebar.
    2. Go to the **Build Settings** tab.
    3. Search for **User Script Sandboxing** (or `ENABLE_USER_SCRIPT_SANDBOXING`).
    4. Set it to **No**.
*   **Configure Local Flutter Path**:
    If Xcode cannot locate your Flutter binary, create `VaultFlutter/ios/.xcode.env.local` and add:
    ```bash
    export FLUTTER_ROOT="/opt/homebrew/share/flutter" # Adjust to your Flutter SDK path
    ```

#### 3. macOS Installation
1. Ensure macOS desktop support is enabled in Flutter:
   ```bash
   flutter config --enable-macos-desktop
   ```
2. From the `VaultFlutter` folder, run:
   ```bash
   flutter run -d macos
   ```

---

## 🤖 Native Compose Implementation (Android)

Located in the root of the repository. Styled in a glassmorphic cinema theme.

### How to Install & Run
1. Open the root `Vault` folder in Android Studio.
2. Let Gradle sync and download Play Services ML Kit dependencies.
3. Build and deploy the debug package to your device.

---

## Security & Privacy Policy

*   **No Internet Permission**: Neither the Compose app nor the Flutter app declares network permissions. They cannot communicate with any remote servers.
*   **Decryption-in-Memory**: Encrypted scans and PDF files are decrypted on-the-fly inside local volatile RAM, and temporary files generated for system previews are instantly marked to delete on exit.
