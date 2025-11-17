# Privacy Policy for NexPass

**Last Updated:** November 16, 2024

## Introduction

NexPass ("we", "our", or "the app") is committed to protecting your privacy. This Privacy Policy explains how NexPass handles your data as a zero-knowledge, open-source password manager.

## Our Privacy Commitment

**NexPass is designed with a zero-knowledge architecture. We cannot access your master password, vault contents, or any sensitive data stored in the app.**

## Data Collection and Storage

### Data We Store Locally on Your Device

NexPass stores the following data **exclusively on your device**:

1. **Master Password Derivative**
   - Your master password is never stored in plaintext
   - Only a cryptographically hashed derivative (PBKDF2, 100,000 iterations) is stored
   - Used to unlock your encrypted vault

2. **Encrypted Password Vault**
   - All your passwords, usernames, URLs, and notes are encrypted with AES-256-GCM
   - Stored in an encrypted SQLCipher database on your device
   - Encryption keys are protected by Android Keystore (hardware-backed when available)

3. **App Settings**
   - Theme preferences (dark/light mode)
   - Auto-lock timeout settings
   - Nextcloud server configuration (URL, username)
   - Biometric authentication preferences
   - Stored in encrypted SharedPreferences

4. **Folder and Tag Data**
   - Organization metadata for your passwords
   - Encrypted in the same SQLCipher database

### Data We Do NOT Collect

NexPass does **NOT** collect, transmit, or have access to:

- Your master password (plaintext)
- Your decrypted passwords or vault contents
- Usage analytics or telemetry
- Crash reports (unless you manually choose to share them)
- Device information beyond what's required for Android functionality
- Location data
- Contacts or other personal information
- Advertising identifiers

## Third-Party Services

### Nextcloud Passwords (Optional)

If you choose to enable Nextcloud synchronization:

1. **What is Transmitted**
   - Encrypted password data is transmitted to **your own Nextcloud server**
   - All passwords are encrypted **before** transmission using AES-256-GCM
   - Server configuration (URL, username, app password)

2. **Zero-Knowledge Sync**
   - Your Nextcloud server receives **only encrypted data**
   - Your Nextcloud server **cannot** decrypt your passwords
   - Decryption happens exclusively on your device after download

3. **Your Control**
   - You control the Nextcloud server (self-hosted or managed)
   - You can disable sync at any time
   - Refer to your Nextcloud provider's privacy policy for server-side data handling

4. **Network Security**
   - All communication uses HTTPS (TLS 1.2+)
   - Certificate validation is enforced
   - No fallback to insecure protocols

### Android System Services

NexPass integrates with standard Android system services:

1. **AutofillService**
   - Uses Android's AutofillFramework API
   - Credential matching happens locally on your device
   - No data sent to Google or third parties

2. **BiometricPrompt**
   - Uses Android's BiometricPrompt API
   - Biometric data never leaves your device's secure hardware
   - We do not access or store biometric information

3. **Android Keystore**
   - Encryption keys are protected by Android Keystore
   - Hardware-backed security when available
   - Keys are device-bound and cannot be extracted

## Data Retention

- **Local Data**: Stored on your device until you delete the app or manually clear data
- **Nextcloud Data**: Retained on your Nextcloud server according to your server's policies
- **No Cloud Backup**: NexPass data is excluded from Android cloud backups for security

## Data Export and Deletion

### Export Your Data

You can export your entire vault:
- Navigate to Settings → Export Vault
- Exports are encrypted with AES-256-GCM
- Requires a strong export password (separate from master password)
- Export files are saved to your device storage

### Delete Your Data

To permanently delete all data:

1. **Local Data**: Uninstall the app or use Settings → Clear All Data
2. **Nextcloud Data**: Delete passwords from your Nextcloud server web interface
3. **Export Files**: Manually delete export files from device storage

## Children's Privacy

NexPass is not directed to children under 13. We do not knowingly collect data from children. If you believe a child has provided data to NexPass, please contact us at [privacy@daguva.com](mailto:privacy@daguva.com).

## Security Measures

We implement industry-standard security practices:

- **AES-256-GCM encryption** for all sensitive data
- **PBKDF2** key derivation (100,000 iterations)
- **Android Keystore** for key protection
- **SQLCipher** for database encryption
- **HTTPS-only** network communication
- **ProGuard obfuscation** in release builds
- **Secure memory wiping** for sensitive data in RAM
- **Biometric authentication** with hardware backing

## Open Source Transparency

NexPass is **100% open source**:

- Source code: [https://github.com/codegax/nexpass](https://github.com/codegax/nexpass)
- You can audit our code to verify our privacy claims
- Licensed under GNU General Public License v3.0
- Community contributions welcome

## Changes to This Privacy Policy

We may update this Privacy Policy periodically. Changes will be posted:

- In this document with an updated "Last Updated" date
- In the app's About screen
- On our GitHub repository

Continued use of NexPass after changes constitutes acceptance of the updated policy.

## International Users

NexPass is designed for worldwide use:

- Data is stored **exclusively on your device**
- If using Nextcloud sync, data is transmitted to **your chosen server location**
- We do not control where your Nextcloud server is located
- Ensure your Nextcloud server complies with applicable data protection laws (GDPR, CCPA, etc.)

## Your Rights

Depending on your jurisdiction (e.g., GDPR, CCPA), you may have rights including:

- **Right to Access**: You can export your vault at any time
- **Right to Deletion**: You can delete all data by uninstalling the app
- **Right to Portability**: Export your data in encrypted JSON format
- **Right to Rectification**: Edit or update passwords in the app

Since all data is stored locally on your device, you have complete control.

## Contact Us

For privacy-related questions or concerns:

- **Email**: [privacy@daguva.com](mailto:privacy@daguva.com)
- **GitHub Issues**: [https://github.com/codegax/nexpass/issues](https://github.com/codegax/nexpass/issues)
- **Security Issues**: [security@daguva.com](mailto:security@daguva.com) (see [SECURITY.md](SECURITY.md))

## Legal Basis for Processing (GDPR)

For users in the European Economic Area:

- **Consent**: You provide consent by installing and using the app
- **Legitimate Interest**: Providing password management functionality
- **Data Controller**: You are the data controller of your own data
- **Data Processor**: Your Nextcloud server (if sync enabled) acts as a data processor

## California Privacy Rights (CCPA)

For California residents:

- **No Sale of Data**: We do not sell your personal information
- **No Sharing for Cross-Context Behavioral Advertising**: We do not share data for advertising
- **Right to Know**: You can export your vault to see all stored data
- **Right to Delete**: Uninstall the app to delete all local data

## Compliance

NexPass is designed to comply with:

- General Data Protection Regulation (GDPR)
- California Consumer Privacy Act (CCPA)
- Android Privacy Guidelines
- Google Play Store Privacy Requirements

---

**Summary**: NexPass is a zero-knowledge password manager. Your master password and decrypted vault contents never leave your device. Optional Nextcloud sync transmits only encrypted data to your own server. We collect no analytics, telemetry, or personal information. You have complete control over your data.
