# Security Policy

## Reporting Security Vulnerabilities

**Please do not report security vulnerabilities through public GitHub issues.**

We take security seriously. If you discover a security vulnerability in NexPass, please report it responsibly.

### How to Report

Send details to **[security@daguva.com](mailto:security@daguva.com)** with:

1. **Description** of the vulnerability
2. **Steps to reproduce** the issue
3. **Potential impact** (what could an attacker do?)
4. **Suggested fix** (if you have one)
5. **Your contact information** for follow-up

### What to Expect

- **Acknowledgment**: Within 48 hours
- **Initial assessment**: Within 7 days
- **Status updates**: Every 7-14 days until resolved
- **Fix timeline**: Depends on severity (see below)
- **Public disclosure**: Coordinated with reporter after fix is released

### Severity Levels

| Severity | Description | Fix Timeline |
|----------|-------------|--------------|
| **Critical** | Remote code execution, data breach, master password exposure | 1-7 days |
| **High** | Privilege escalation, authentication bypass, encryption weakness | 7-14 days |
| **Medium** | Information disclosure, DoS | 14-30 days |
| **Low** | Minor security issues, hardening opportunities | 30-90 days |

## Security Best Practices for Users

### Master Password

- Use a **strong, unique** master password (12+ characters)
- Include uppercase, lowercase, numbers, and symbols
- **Never reuse** your master password elsewhere
- Consider using a **passphrase** (e.g., "correct-horse-battery-staple")
- **Never share** your master password with anyone

### Device Security

- Enable **device encryption** (Android 10+ has this by default)
- Use a **secure lock screen** (PIN, pattern, or biometric)
- Keep your **Android OS updated**
- Install apps only from **trusted sources** (Google Play, F-Droid)
- Enable **biometric unlock** for convenience (fingerprint/face)

### Nextcloud Sync Security

- Use **app passwords** (not your main Nextcloud password)
- Enable **Two-Factor Authentication (2FA)** on your Nextcloud account
- Use **HTTPS** for Nextcloud server connections (enforced by NexPass)
- Ensure your Nextcloud server is **kept up to date**
- Use a **trusted Nextcloud provider** or self-host securely

### App Security

- Download NexPass only from **official sources**:
  - GitHub Releases: [https://github.com/codegax/nexpass/releases](https://github.com/codegax/nexpass/releases)
  - Google Play Store (when available)
  - F-Droid (when available)
- **Verify signatures** of APK files
- Enable **auto-lock** (Settings → Auto-Lock Timeout)
- Regularly **export encrypted backups** of your vault
- **Review app permissions** - NexPass only requests necessary permissions

### Data Backup

- Regularly **export your vault** (Settings → Export Vault)
- Use a **strong export password** (different from master password)
- Store backups in a **secure location** (encrypted USB, offline storage)
- **Test your backups** periodically by importing them
- Keep backups **separate from your device** (in case of loss/theft)

## Supported Versions

| Version | Supported | Notes |
|---------|-----------|-------|
| 1.0.x (Latest) | ✅ Yes | Active development and security updates |
| < 1.0.0 | ❌ No | Beta/alpha versions - upgrade immediately |

**Always use the latest version** to ensure you have the most recent security fixes.

## Security Architecture

### Encryption

- **Algorithm**: AES-256-GCM (Galois/Counter Mode)
- **Key Size**: 256 bits
- **Mode**: Authenticated encryption with associated data (AEAD)
- **IV**: 12 bytes, randomly generated per encryption
- **Tag**: 128 bits for authentication

### Key Derivation

- **Algorithm**: PBKDF2 (Password-Based Key Derivation Function 2)
- **Hash Function**: HMAC-SHA256
- **Iterations**: 100,000 (resistant to brute-force)
- **Salt**: 16 bytes, randomly generated per vault
- **Output**: 256-bit vault encryption key

### Key Storage

- **Android Keystore**: Hardware-backed when available (TEE/StrongBox)
- **Master Key**: Used to encrypt the vault key
- **Vault Key**: Stored encrypted in EncryptedSharedPreferences
- **In-Memory**: Vault key held in memory only when vault is unlocked
- **Memory Wiping**: Sensitive data zeroed after use

### Database

- **Engine**: SQLCipher (encrypted SQLite)
- **Cipher**: AES-256-CBC
- **KDF**: PBKDF2-HMAC-SHA512 (256,000 iterations)
- **Page Size**: 4096 bytes
- **Backups**: Excluded from Android cloud backups

### Network Security

- **Protocol**: HTTPS only (TLS 1.2+)
- **Certificate Validation**: Enforced (no self-signed certs accepted)
- **Certificate Pinning**: Not implemented (to allow self-hosted Nextcloud)
- **Zero-Knowledge**: Passwords encrypted before transmission
- **Authentication**: HTTP Basic Auth with Nextcloud app password

### Biometric Security

- **API**: Android BiometricPrompt (API 28+)
- **Fallback**: Password unlock always available
- **Crypto Object**: Tied to Keystore key when supported
- **Biometric Data**: Never accessed by app (handled by Android system)

### Memory Security

- **Sensitive Data**: Wiped from memory after use
- **String Handling**: Converted to ByteArray for wiping
- **Garbage Collection**: Explicit zeroing before GC
- **Logging**: No sensitive data logged (passwords, keys, PII)

## Known Security Considerations

### Current Limitations

1. **Folders Don't Sync with Nextcloud**
   - Folders are currently local-only
   - Risk: Folder organization lost if device is lost
   - Mitigation: Use export/import to transfer folder structure
   - Status: Planned for v1.1.0

2. **No Background Sync Worker**
   - Sync only occurs manually (tap "Sync Now")
   - Risk: Changes may not sync if you forget
   - Mitigation: Manual sync before/after making changes
   - Status: Planned for v1.1.0

3. **No Password History**
   - Previous password versions not tracked
   - Risk: Can't recover if you accidentally change a password
   - Mitigation: Export vault before making bulk changes
   - Status: Planned for v1.1.0

4. **No Screenshot Prevention**
   - Screenshots are not blocked
   - Risk: Passwords visible in screenshots
   - Mitigation: User awareness; avoid screenshotting passwords
   - Status: Considering for future release

5. **No Clipboard Auto-Clear**
   - Copied passwords remain in clipboard until overwritten
   - Risk: Passwords accessible via clipboard history
   - Mitigation: Manually clear clipboard after use
   - Status: Planned for v1.1.0

### Defense in Depth

Even with these limitations, NexPass maintains strong security:

- **Zero-knowledge encryption**: Server never sees plaintext
- **Local-first**: Works offline; network outages don't lock you out
- **Hardware-backed keys**: Encryption keys protected by Android Keystore
- **Auto-lock**: Vault locks after timeout
- **Biometric unlock**: Fast access without typing password repeatedly

## Security Audits

- **Internal Reviews**: Code reviewed by maintainers for security issues
- **Open Source**: Code publicly auditable on GitHub
- **External Audits**: None yet (planned for future)

If you'd like to sponsor a professional security audit, please contact [security@daguva.com](mailto:security@daguva.com).

## Security Hall of Fame

We recognize security researchers who responsibly disclose vulnerabilities:

*No vulnerabilities reported yet.*

---

Thank you for helping keep NexPass and our users secure!
