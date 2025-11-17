# NexPass

**Secure, Open-Source Android Password Manager with Nextcloud Sync**

[![License: GPLv3](https://img.shields.io/badge/License-GPLv3-blue.svg)](LICENSE)
[![Android](https://img.shields.io/badge/Platform-Android%2010+-green.svg)](https://android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)](https://kotlinlang.org)

NexPass is a privacy-focused Android password manager with zero-knowledge encryption, designed for users who self-host Nextcloud. It combines robust security with seamless OS-level autofill integration and offline-first architecture.

## Features

### Security & Privacy
- **Zero-Knowledge Encryption**: Master password never leaves your device
- **AES-256-GCM Encryption**: Military-grade encryption for all stored passwords
- **Hardware-Backed Security**: Android Keystore integration
- **Biometric Authentication**: Fingerprint/Face unlock support
- **Auto-Lock**: Configurable timeout (1/5/15/30 minutes)
- **Encrypted Database**: SQLCipher-encrypted local storage
- **PBKDF2 Key Derivation**: 100,000 iterations for master password

### Core Functionality
- **Full CRUD Operations**: Create, read, update, delete passwords
- **Intelligent Password Generator**: Character-based and passphrase modes
- **Folder Organization**: Categorize passwords into folders
- **Tag System**: 16-color tags for flexible organization
- **Favorites**: Quick access to frequently used passwords
- **Search**: Fast password search across all fields
- **Export/Import**: Encrypted backup and restore

### Nextcloud Integration
- **Two-Way Sync**: Seamless synchronization with Nextcloud Passwords
- **Offline-First**: Works without internet, syncs when available
- **Conflict Resolution**: Smart last-write-wins algorithm
- **Pending Queue**: Offline changes synced automatically
- **Zero-Knowledge Upload**: Passwords encrypted before transmission

### Android Integration
- **Autofill Service**: System-wide password autofill
- **Intelligent Matching**: Domain, package, and fuzzy matching
- **Auto-Save**: Capture new credentials automatically
- **Material 3 UI**: Modern, clean interface with dark/light themes
- **Deep Linking**: Direct navigation to specific passwords

## Quick Start

### Prerequisites

- **Android Studio**: Hedgehog (2023.1.1) or newer
- **JDK**: 17 or higher
- **Android SDK**: API 29+ (Android 10+)
- **Gradle**: 8.2+ (included via wrapper)

### Clone & Build

```bash
# Clone the repository
git clone https://github.com/codegax/nexpass.git
cd nexpass

# Build debug APK
./gradlew assembleDebug

# Run on connected device/emulator
./gradlew installDebug

# Run tests
./gradlew test
```

### Build Variants

- **Debug**: Development build with debugging enabled
  - Package: `com.nexpass.passwordmanager.debug`
  - Suffix: `-debug`

- **Release**: Production build with ProGuard optimization
  - Package: `com.nexpass.passwordmanager`
  - Requires signing configuration

## Architecture

### Tech Stack

- **Language**: 100% Kotlin
- **UI Framework**: Jetpack Compose + Material 3
- **Architecture**: Clean Architecture + MVVM
- **Database**: Room + SQLCipher (encrypted)
- **Network**: Ktor Client (HTTPS only)
- **Security**: Android Keystore + BiometricPrompt
- **Dependency Injection**: Koin
- **Testing**: JUnit, Coroutines Test

### Project Structure

```
app/src/main/java/com/nexpass/passwordmanager/
├── autofill/           # Android AutofillService implementation
│   ├── matcher/        # Domain/package matching logic
│   ├── service/        # AutofillService & response builder
│   └── ui/             # Autofill unlock prompt
├── data/               # Data layer (repositories, DAOs, network)
│   ├── local/          # Room database, DAOs, entities
│   ├── network/        # Nextcloud API client
│   └── repository/     # Repository implementations
├── di/                 # Koin dependency injection modules
├── domain/             # Business logic layer
│   ├── model/          # Domain models
│   ├── repository/     # Repository interfaces
│   └── usecase/        # Use cases (export/import)
├── security/           # Core security layer
│   ├── biometric/      # Biometric authentication
│   ├── encryption/     # AES-256-GCM crypto operations
│   ├── keystore/       # Android Keystore management
│   └── vault/          # In-memory vault key management
├── ui/                 # Presentation layer
│   ├── components/     # Reusable Compose components
│   ├── navigation/     # Navigation graph
│   ├── screens/        # Screen composables
│   ├── theme/          # Material 3 theme
│   ├── viewmodel/      # ViewModels
│   └── lifecycle/      # Auto-lock manager
└── util/               # Utilities (retry policy, network monitor)
```

### Security Architecture

```
Master Password
    ↓
PBKDF2 (100k iterations)
    ↓
Vault Key (AES-256)
    ↓
Encrypted by Keystore Master Key
    ↓
Stored in Encrypted SharedPreferences
    ↓
Used to encrypt/decrypt passwords
    ↓
SQLCipher Database
```

## Development

### Running Tests

```bash
# Unit tests
./gradlew test

# Unit tests with coverage
./gradlew testDebugUnitTest

# Lint checks
./gradlew lint

# All checks
./gradlew check
```

### Code Style

This project follows [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html):
- 4 spaces for indentation
- Max line length: 120 characters
- Explicit types for public APIs
- Meaningful variable names

### Git Workflow

1. Fork the repository
2. Create feature branch: `git checkout -b feature/amazing-feature`
3. Commit changes: `git commit -m 'Add amazing feature'`
4. Push to branch: `git push origin feature/amazing-feature`
5. Open a Pull Request

See [CONTRIBUTING.md](CONTRIBUTING.md) for detailed guidelines.

## Documentation

### User Documentation

- **[docs/USER_GUIDE.md](docs/USER_GUIDE.md)**: End-user installation and usage
- **[docs/NEXTCLOUD_SETUP.md](docs/NEXTCLOUD_SETUP.md)**: Nextcloud integration guide
- **[docs/TROUBLESHOOTING.md](docs/TROUBLESHOOTING.md)**: Common issues and fixes

### Developer Documentation

For detailed technical documentation, see the `../NexPassClaude/` directory:
- Architecture specifications
- API contracts
- Build instructions
- Setup guides

## Security

### Reporting Vulnerabilities

Please report security vulnerabilities to [security@daguva.com](mailto:security@daguva.com) or via GitHub Security Advisories.

See [SECURITY.md](SECURITY.md) for our security policy.

### Security Features

- Master password hashed with PBKDF2 (100,000 iterations)
- All passwords encrypted with AES-256-GCM
- Encryption keys protected by Android Keystore
- Database encrypted with SQLCipher
- HTTPS-only network communication
- Zero-knowledge architecture (server never sees plaintext)
- Biometric authentication with hardware backing
- Secure memory wiping for sensitive data
- ProGuard obfuscation in release builds

## Contributing

We welcome contributions! Please see [CONTRIBUTING.md](CONTRIBUTING.md) for:
- Code of conduct
- Development setup
- Testing requirements
- Pull request process
- Code review guidelines

## License

This project is licensed under the **GNU General Public License v3.0** - see the [LICENSE](LICENSE) file for details.

### Key Points
- ✅ Free to use, modify, and distribute
- ✅ Source code must remain open
- ✅ Changes must be documented
- ✅ Same license for derivatives

## Acknowledgments

- **Nextcloud Passwords**: Backend API integration
- **Android Open Source Project**: Platform foundation
- **Jetpack Compose**: Modern UI framework
- **SQLCipher**: Database encryption
- **Community Contributors**: Bug reports and feature requests

## Roadmap

### v1.0.0 (Current - Release Candidate)
- Core password management
- Nextcloud sync
- Autofill service
- Folder & tag organization

### v1.1.0 (Planned)
- Folder sync with Nextcloud
- Background sync worker
- Password history
- Biometric re-auth for sensitive actions

### v1.2.0 (Future)
- TOTP/2FA code generation
- Custom fields support
- Password sharing
- Breach monitoring

### v2.0.0 (Vision)
- Multi-account support
- Passkey/WebAuthn support
- Browser extension
- Desktop clients (Linux, Windows, macOS)

## Support

- **Documentation**: See `docs/` directory
- **Issues**: [GitHub Issues](https://github.com/codegax/nexpass/issues)
- **Discussions**: [GitHub Discussions](https://github.com/codegax/nexpass/discussions)
- **Contact**: [contact@daguva.com](mailto:contact@daguva.com)

---

**Built with ❤️ for privacy and security**
