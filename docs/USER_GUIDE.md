# NexPass User Guide

Welcome to NexPass! This guide will help you get started with your secure, privacy-focused password manager.

## Table of Contents

1. [Installation](#installation)
2. [First-Time Setup](#first-time-setup)
3. [Creating Your First Password](#creating-your-first-password)
4. [Managing Passwords](#managing-passwords)
5. [Using Autofill](#using-autofill)
6. [Organizing with Folders and Tags](#organizing-with-folders-and-tags)
7. [Nextcloud Synchronization](#nextcloud-synchronization)
8. [Password Generator](#password-generator)
9. [Export and Import](#export-and-import)
10. [Settings](#settings)
11. [Tips and Best Practices](#tips-and-best-practices)

## Installation

### From GitHub Releases

1. Visit [https://github.com/codegax/nexpass/releases](https://github.com/codegax/nexpass/releases)
2. Download the latest `.apk` file
3. Open the downloaded file on your Android device
4. Tap "Install" (you may need to allow installation from unknown sources)

### From Google Play Store

*Coming soon*

### From F-Droid

*Coming soon*

### Requirements

- Android 10 (API 29) or higher
- 50 MB of free storage
- Internet connection (optional, for Nextcloud sync)

## First-Time Setup

### Creating Your Master Password

When you first open NexPass, you'll be guided through the onboarding process:

1. **Launch NexPass** - Tap the app icon
2. **Welcome Screen** - Read about NexPass's features
3. **Create Master Password**:
   - Enter a strong, memorable password
   - This password encrypts ALL your data
   - You cannot recover it if forgotten
   - Password strength indicator will help you choose a secure password

**Important**: Your master password:
- Is NEVER stored or transmitted anywhere
- Cannot be recovered if forgotten
- Should be at least 12 characters
- Should include uppercase, lowercase, numbers, and symbols
- Should NOT be used anywhere else

4. **Confirm Master Password** - Re-type to confirm
5. **Enable Biometric Unlock** (optional):
   - Use fingerprint or face unlock for convenience
   - Your master password is still required occasionally
   - You can enable this later in Settings

6. **Setup Complete** - You're ready to use NexPass!

## Creating Your First Password

### Quick Start

1. **Unlock your vault** with master password or biometric
2. **Tap the + button** (bottom-right corner)
3. **Fill in the details**:
   - **Title**: Name for this password (e.g., "Gmail")
   - **Username**: Your email or username
   - **Password**: Your password (or tap 🎲 to generate one)
   - **URL**: Website address (e.g., "https://gmail.com")
   - **Notes**: Optional additional information
4. **Tap Save**

Your password is now encrypted and stored securely!

## Managing Passwords

### Viewing Passwords

1. **Unlock your vault**
2. **Browse the list** - Scroll through all saved passwords
3. **Search** - Use the search bar at the top
4. **Filter by favorites** - Tap the ⭐ icon to show only favorites

### Viewing Password Details

1. **Tap a password** in the list
2. **View information**:
   - Username
   - Password (tap 👁️ to show/hide)
   - URL
   - Notes
3. **Copy to clipboard**:
   - Tap 📋 next to username or password
   - Paste anywhere you need it

### Editing a Password

1. **Open password details**
2. **Tap the Edit button** (pencil icon)
3. **Make your changes**
4. **Tap Save**

### Deleting a Password

1. **Open password details**
2. **Tap the Delete button** (trash icon)
3. **Confirm deletion**

**Warning**: Deleted passwords cannot be recovered unless you have an export backup!

### Marking Favorites

1. **Open password details**
2. **Tap the Star icon** (⭐) in the top-right
3. **Star turns yellow** - Password is now a favorite
4. **Filter favorites** - Tap ⭐ in vault list to show only favorites

## Using Autofill

NexPass integrates with Android's Autofill Framework to automatically fill passwords in apps and browsers.

### Enabling Autofill

1. **Open NexPass Settings**
2. **Tap "Enable Autofill Service"**
3. **Android Settings opens**
4. **Select NexPass** as your autofill service
5. **Grant permission**

### Using Autofill

1. **Open any app** or website with a login form
2. **Tap on the username or password field**
3. **Autofill popup appears** with matching passwords
4. **Tap a password** to fill it
5. **If vault is locked**:
   - Unlock with master password or biometric
   - Autofill will proceed after unlock

### Auto-Save New Passwords

When you log in with new credentials:

1. **NexPass detects** the new username/password
2. **Save prompt appears**
3. **Tap "Save"** to add it to your vault
4. **Edit details** if needed (add title, URL)
5. **Saved automatically**

### Autofill Not Working?

See the [Troubleshooting Guide](TROUBLESHOOTING.md#autofill-issues).

## Organizing with Folders and Tags

### Creating Folders

1. **Open Settings** → **Manage Folders**
2. **Tap + button**
3. **Enter folder name** (e.g., "Work", "Personal", "Banking")
4. **Tap Save**

### Assigning Passwords to Folders

1. **Edit a password**
2. **Tap "Folder" field**
3. **Select a folder**
4. **Tap Save**

### Filtering by Folder

1. **Vault list screen**
2. **Tap the folder icon** (📁)
3. **Select a folder**
4. **View only passwords in that folder**

### Creating Tags

1. **Open Settings** → **Manage Tags**
2. **Tap + button**
3. **Enter tag name** (e.g., "Important", "Shared", "Old")
4. **Choose a color** (16 options)
5. **Tap Save**

### Assigning Tags to Passwords

1. **Edit a password**
2. **Tap "Tags" field**
3. **Select one or more tags**
4. **Tap Save**

**Note**: Folders and tags are currently local-only and don't sync with Nextcloud.

## Nextcloud Synchronization

NexPass can sync with your Nextcloud Passwords server for backup and multi-device access.

### Prerequisites

- A Nextcloud server with Passwords app installed
- Nextcloud app password (see [Nextcloud Setup Guide](NEXTCLOUD_SETUP.md))

### Connecting to Nextcloud

1. **Open Settings** → **Nextcloud Sync**
2. **Enter your details**:
   - **Server URL**: Your Nextcloud URL (e.g., "https://cloud.example.com")
   - **Username**: Your Nextcloud username
   - **App Password**: Generated from Nextcloud (NOT your main password)
3. **Tap "Test Connection"**
4. **If successful**, tap "Save"

### Syncing Your Vault

1. **Open Settings** → **Nextcloud Sync**
2. **Tap "Sync Now"**
3. **Wait for sync to complete**
4. **Check status**: Last sync time and success/error message

### How Sync Works

- **Two-way sync**: Changes upload to server, server changes download to device
- **Zero-knowledge**: Passwords are encrypted BEFORE upload
- **Conflict resolution**: Last-write-wins (most recent change kept)
- **Offline queue**: Changes made offline sync when connection restored

### Sync Best Practices

- **Sync before and after** making changes
- **Verify sync status** after important changes
- **Enable Nextcloud 2FA** for extra security
- **Use app passwords** (not your main Nextcloud password)

For detailed setup instructions, see [Nextcloud Setup Guide](NEXTCLOUD_SETUP.md).

## Password Generator

NexPass includes a powerful password generator with two modes.

### Accessing the Generator

- **When creating a password**: Tap the 🎲 icon next to password field
- **From main menu**: Settings → Password Generator

### Character-Based Mode

Generates random passwords with customizable character sets:

1. **Select Character-Based Mode**
2. **Adjust settings**:
   - **Length**: 8-64 characters (default: 16)
   - **Uppercase**: Include A-Z
   - **Lowercase**: Include a-z
   - **Numbers**: Include 0-9
   - **Symbols**: Include !@#$%^&*
3. **Tap "Generate"**
4. **Copy** or **Use** the generated password

**Example**: `kR7$mPq2nX9@vL4s`

### Passphrase Mode

Generates memorable passphrases using words:

1. **Select Passphrase Mode**
2. **Adjust settings**:
   - **Words**: 3-8 words (default: 4)
   - **Separator**: - _ . (space)
   - **Capitalize**: First letter of each word
   - **Include Number**: Add number at end
3. **Tap "Generate"**
4. **Copy** or **Use** the generated passphrase

**Example**: `Correct-Horse-Battery-Staple-7`

### Password Strength

The generator shows strength indicators:
- **Weak**: < 60 bits of entropy (red)
- **Medium**: 60-80 bits (orange)
- **Strong**: 80-100 bits (yellow)
- **Very Strong**: > 100 bits (green)

## Export and Import

Backup your vault with encrypted exports.

### Exporting Your Vault

1. **Open Settings** → **Export Vault**
2. **Choose export password**:
   - Use a STRONG password (different from master password)
   - You'll need this to import the vault later
3. **Confirm export password**
4. **Tap "Export"**
5. **Choose save location** (Downloads folder recommended)
6. **File saved**: `nexpass_backup_YYYY-MM-DD.nxp`

**Important**:
- Export files are encrypted with AES-256-GCM
- Store exports in a secure location
- Test your export by importing it
- Create regular backups (weekly recommended)

### Importing a Vault

1. **Open Settings** → **Import Vault**
2. **Tap "Select File"**
3. **Navigate to your `.nxp` export file**
4. **Select the file**
5. **Enter export password**
6. **Choose import mode**:
   - **Merge**: Add to existing passwords (duplicates skipped)
   - **Replace**: Delete all existing passwords first
7. **Tap "Import"**
8. **Wait for completion**

**Warning**: "Replace" mode deletes ALL existing passwords before import!

## Settings

### Security Settings

- **Auto-Lock Timeout**: 1, 5, 15, 30 minutes, or Never
- **Biometric Unlock**: Enable/disable fingerprint/face unlock
- **Change Master Password**: Update your master password

### Appearance

- **Theme**: Light, Dark, or System Auto
- **Material You**: Dynamic colors (Android 12+)

### Nextcloud Sync

- **Server URL**: Your Nextcloud server address
- **Username**: Your Nextcloud username
- **App Password**: Nextcloud app password
- **Test Connection**: Verify server connectivity
- **Sync Now**: Manual synchronization
- **Last Sync**: Status of most recent sync

### Organization

- **Manage Folders**: Create, rename, delete folders
- **Manage Tags**: Create, edit, delete tags

### Data Management

- **Export Vault**: Create encrypted backup
- **Import Vault**: Restore from backup
- **Clear All Data**: Delete everything (requires confirmation)

### About

- **Version**: App version number
- **License**: GPLv3 open source license
- **Source Code**: Link to GitHub repository
- **Privacy Policy**: How we handle your data
- **Contact**: Support email

## Tips and Best Practices

### Master Password

✅ **Do**:
- Use a passphrase (e.g., "correct-horse-battery-staple")
- Make it at least 12 characters
- Include variety (upper, lower, numbers, symbols)
- Write it down and store it securely (offline)
- Change it if you suspect compromise

❌ **Don't**:
- Reuse it from other services
- Share it with anyone
- Store it in plaintext on your device
- Use simple patterns (123456, password)
- Forget it (there's no recovery!)

### Password Hygiene

- **Use unique passwords** for every service
- **Use the password generator** for new accounts
- **Update old passwords** to generated ones
- **Enable 2FA** where available (in addition to strong passwords)
- **Review passwords** quarterly for old/weak ones

### Backup Strategy

- **Export weekly** to create backups
- **Store exports** in 2+ secure locations:
  - Encrypted USB drive
  - Offline storage (safe, lockbox)
  - Separate encrypted cloud storage
- **Test imports** to verify backups work
- **Keep export passwords** safe and separate

### Device Security

- **Use device encryption** (Android 10+ default)
- **Enable lock screen** (PIN, pattern, or biometric)
- **Keep Android updated** for security patches
- **Install apps carefully** from trusted sources only
- **Review app permissions** periodically

### Nextcloud Sync

- **Use HTTPS** (enforced by NexPass)
- **Enable Nextcloud 2FA** for account protection
- **Use app passwords** instead of main password
- **Sync regularly** before/after changes
- **Monitor sync status** for errors

### Folder Organization Ideas

- **By Category**: Work, Personal, Banking, Shopping
- **By Importance**: Critical, Important, Optional
- **By Frequency**: Daily, Weekly, Rarely Used
- **By Type**: Email, Social Media, Finance, Entertainment

### Tag Usage Ideas

- **Status**: Active, Inactive, Archived
- **Security**: 2FA Enabled, Needs Update, Compromised
- **Sharing**: Shared, Personal
- **Priority**: High, Medium, Low

---

## Getting Help

### Still Have Questions?

- **Troubleshooting**: See [TROUBLESHOOTING.md](TROUBLESHOOTING.md)
- **Nextcloud Setup**: See [NEXTCLOUD_SETUP.md](NEXTCLOUD_SETUP.md)
- **GitHub Issues**: [https://github.com/codegax/nexpass/issues](https://github.com/codegax/nexpass/issues)
- **Email Support**: [support@daguva.com](mailto:support@daguva.com)

### Report Bugs

Found a bug? Please report it on [GitHub Issues](https://github.com/codegax/nexpass/issues) with:
- Android version
- Device model
- NexPass version
- Steps to reproduce
- Screenshots (if applicable)

---

**Welcome to secure password management with NexPass!** 🔐
