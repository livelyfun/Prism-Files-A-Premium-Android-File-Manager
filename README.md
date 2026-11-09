# Prism Files – Premium Android File Manager

**Prism Files** is a next-generation Android file manager designed with a sophisticated blend of three distinct design styles: **Skeuomorphism**, **Neumorphism**, and **Glassmorphism**, accompanied by an impenetrable **Hardware-backed Encrypted Vault** (AES-256-GCM).

---

## 🎨 The Three Design Styles

1. **Skeuomorphism – Warmth & Intuition**
   - **Applied to**: Folder and file icons throughout the Home Dashboard and File Browser.
   - **Technique**: Custom layered Canvas rendering featuring physical manila folder tabs, paper sheets with dog-ear folds, vinyl record grooves for audio, film slates for videos, and titanium safe badges for encrypted files.
   - **Goal**: Emotional warmth and tactile recognizability—organizing files feels like arranging physical objects.

2. **Neumorphism (Soft UI) – Tactile Elegance**
   - **Applied to**: Toggles, switches, sliders, soft cards, and the Vault PIN keypad in Security Controls and Settings.
   - **Technique**: Dual-shadow light modeling with ambient top-left specular highlights and bottom-right shadow extrusions, combined with smooth inset animations when pressed.
   - **Goal**: Satisfying, pressable, tactile feedback for privacy toggles and biometric credentials.

3. **Glassmorphism – Depth & Modernity**
   - **Applied to**: Overlays, modal dialogs, the Vault unlock screen, media preview controls, and ambient backdrops.
   - **Technique**: Frosted glass blur, semi-transparent gradients (`Color.White.copy(alpha = 0.15f)`), fine 1.2dp iridescent specular borders, and chromatic prismatic light mesh gradients.
   - **Goal**: Distinct separation between public browsing and confidential vault actions.

---

## 🛡️ The Prism Vault (Hardware-Backed Security)

- **Cryptography**: Uses standard Android KeyStore with 256-bit AES-GCM (`AES/GCM/NoPadding`) authenticated encryption. Keys are generated directly within the device's hardware-backed Secure Element / TEE and never leave the chip.
- **Obfuscation**: Encrypted files are assigned random UUIDs with a `.pvf` (Prism Vault File) extension.
- **Isolation**: Stored inside the app's private internal storage (`context.filesDir/vault/`).
- **Anti-Indexing**: Protected with an empty `.nomedia` file to prevent the Android MediaStore and third-party gallery apps from discovering the files.
- **Authentication**: Strong 4-digit PIN (SHA-256 with 16-byte random salt, stored in protected preferences) alongside Android `BiometricPrompt` (Fingerprint & Face unlock).
- **Panic Mode**: If an incorrect PIN is entered 5 consecutive times, Panic Mode activates, concealing the vault and locking it down until reset.
- **Zero Cloud Leakage**: Vault directories and preference files are explicitly excluded from Android Auto-Backup and Google Cloud data extraction rules.
- **Decryption on-the-fly**: When viewing an item, the file is temporarily decrypted to private cache with `deleteOnExit()` flags, opened in the immersive viewer, and wiped upon exit.

---

## 🚀 Core Features

- **Home Dashboard**:
  - Interactive Storage Ring Chart with animated color-coded segments (Images, Videos, Audio, Documents, Apps, Vault, Other, Free space).
  - Smart Clean engine identifying cache files, logs, temp items, and duplicates with a 1-tap reclamation dialog.
  - Parallax horizontal recent files carousel.
  - Category shortcuts grid (Images, Videos, Audio, Documents, Downloads, Archives, Apps, Vault).
- **File Browser**:
  - Interactive breadcrumb navigation (`Internal Storage > DCIM > Camera`) for path navigation.
  - Dynamic List and Grid views with large image/video previews.
  - Multi-select batch actions (Share, Delete, Move, and Hide in Vault).
  - Swipe right to share, swipe left to delete with Undo snackbar.
  - Real-time search and sort chips (Name, Date, Size, Type).
- **File Preview Screen**:
  - Immersive viewer with pinch-to-zoom and pan for images.
  - Text document reader for Markdown, plain text, and code files.
  - Glassmorphic top and bottom overlay sheets with metadata inspector.
- **Settings**:
  - Theme selection: Light, Dark, and System default.
  - Security controls: Biometric authentication toggle, Auto-lock timers (Immediate, 1 min, 5 min), Panic Mode reset.
  - Cache maintenance: 1-tap wipe for decrypted preview cache.

---

## 🛠️ Architecture & Tech Stack

- **UI Framework**: 100% Jetpack Compose with Material 3 Expressive.
- **Architecture**: Clean Architecture + MVVM (`ViewModel`, `StateFlow`, `collectAsState`).
- **Image Pipeline**: Coil Compose.
- **Security**: Android KeyStore, `javax.crypto.Cipher`, `androidx.biometric.BiometricPrompt`.
- **Storage Access**: MediaStore API + FileProvider + Storage Access Framework (SAF).
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`).
