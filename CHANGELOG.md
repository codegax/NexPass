# Changelog

All notable changes to NexPass will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

### Planned for v1.1.0
- Folder synchronization with Nextcloud
- Background sync worker (WorkManager)
- Password history tracking
- Clipboard auto-clear after 30 seconds
- Biometric re-authentication for sensitive operations

### Planned for v1.2.0
- TOTP/2FA code generation
- Custom fields support
- Password sharing capabilities
- Breach monitoring integration

## [1.0.0-rc1] - 2024-11-16

### Added
- Initial release candidate
- Core password management (CRUD operations)
- Zero-knowledge encryption (AES-256-GCM)
- Master password with PBKDF2 key derivation (100,000 iterations)
- Android Keystore integration for key protection
- Biometric authentication (fingerprint/face unlock)
- SQLCipher encrypted database
- Material 3 UI with dark/light theme support
- Nextcloud Passwords synchronization (two-way sync)
- Android AutofillService integration
- Intelligent autofill matching (domain, package, fuzzy)
- Auto-save for new credentials
- Password generator (character-based and passphrase modes)
- Folder organization for passwords
- Tag system with 16 color options
- Favorites filtering
- Fast password search
- Encrypted vault export/import
- Auto-lock with configurable timeout (1/5/15/30 min, Never)
- Offline-first architecture with pending sync queue
- Conflict resolution for sync (last-write-wins)
- Error handling with user-friendly messages
- Retry logic with exponential backoff
- Network connectivity monitoring

### Security
- AES-256-GCM encryption for all passwords
- PBKDF2-HMAC-SHA256 (100,000 iterations) for master password
- Hardware-backed Android Keystore when available
- SQLCipher database encryption (AES-256-CBC, 256,000 iterations)
- HTTPS-only network communication
- Zero-knowledge sync (server never sees plaintext)
- Secure memory wiping for sensitive data
- ProGuard obfuscation for release builds
- No analytics or telemetry

### Known Limitations
- Folders currently don't sync with Nextcloud (local-only)
- No background sync worker (manual sync only)
- No password history tracking
- No clipboard auto-clear
- No screenshot prevention

## [0.9.0] - 2024-11-16

### Added
- Phase 9 completion: Advanced features
- Export/Import UI with file picker integration
- Folder management (create, rename, delete, assign)
- Tag management (create with colors, assign to passwords)
- Auto-lock implementation with background timeout
- Production-ready build configuration

## [0.8.0] - 2024-11-16

### Added
- Phase 8 completion: Settings and extras
- Password generator screen (dual-mode)
- Theme selection (light, dark, system auto)
- Auto-lock timeout settings
- Favorites filter for vault
- About screen with version info
- Export/import backend infrastructure

## [0.7.0] - 2024-11-16

### Added
- Phase 7 completion: Polish and error handling
- Comprehensive error handling system
- Retry logic with exponential backoff
- Network connectivity monitoring
- User-friendly error messages
- State components (EmptyState, LoadingState, ErrorState)
- Error UI components (ErrorCard, ErrorMessage)

## [0.6.0] - 2024-11-16

### Added
- Phase 6 completion: AutofillService
- Full Android AutofillService implementation
- Intelligent credential matching (domain, package, subdomain, fuzzy)
- Scoring algorithm for best match prioritization
- Vault unlock integration for autofill
- Auto-save for capturing new credentials
- Material 3 autofill UI

## [0.5.0] - 2024-11-16

### Added
- Phase 5 completion: Nextcloud integration
- NextcloudApiClient with full REST API support
- Two-way synchronization (upload/download)
- Conflict resolution algorithm (last-write-wins)
- Offline queue for pending operations
- Settings UI for server configuration
- Connection testing and status indicators
- Zero-knowledge sync architecture

## [0.4.5] - 2024-11-16

### Added
- Phase 4.5 completion: Vault management polish
- Password favorites functionality
- Enhanced password detail screen
- Improved vault list UI

## [0.4.0] - 2024-11-16

### Added
- Phase 4 completion: Vault management
- Onboarding flow with master password creation
- Unlock flow (password + biometric)
- Password CRUD operations
- Password generator
- Password strength indicator
- Search functionality

## [0.3.0] - 2024-11-16

### Added
- Phase 3 completion: UI foundation
- Material 3 theme implementation
- 5 core screens (Onboarding, Unlock, Vault, Detail, Settings)
- Navigation with Compose Navigation
- Reusable UI components
- Dark mode support

## [0.2.0] - 2024-11-16

### Added
- Phase 2 completion: Local storage
- Room database with SQLCipher encryption
- 4 DAOs (Password, Folder, Tag, SyncOperation)
- 4 repositories with full CRUD
- Encrypted SharedPreferences
- VaultKeyManager for in-memory key management

## [0.1.0] - 2024-11-16

### Added
- Phase 1 completion: Core security layer
- CryptoManager (AES-256-GCM encryption/decryption)
- KeystoreManager (Android Keystore integration)
- BiometricManager (BiometricPrompt authentication)
- MemoryUtils (secure memory wiping)
- PBKDF2 key derivation
- 93% test coverage on security layer

## [0.0.1] - 2024-11-16

### Added
- Phase 0 completion: Project setup
- Android project initialization (API 29+, target SDK 34)
- Clean Architecture structure
- All dependencies configured
- ProGuard security rules
- Build system ready

---

## Version History Summary

- **1.0.0-rc1**: Release candidate - feature complete MVP
- **0.9.0**: Advanced features (folders, tags, auto-lock)
- **0.8.0**: Settings and extras
- **0.7.0**: Polish and error handling
- **0.6.0**: AutofillService integration
- **0.5.0**: Nextcloud synchronization
- **0.4.5**: Vault management polish
- **0.4.0**: Vault management
- **0.3.0**: UI foundation
- **0.2.0**: Local storage
- **0.1.0**: Core security layer
- **0.0.1**: Project setup

[Unreleased]: https://github.com/codegax/nexpass/compare/v1.0.0-rc1...HEAD
[1.0.0-rc1]: https://github.com/codegax/nexpass/releases/tag/v1.0.0-rc1
