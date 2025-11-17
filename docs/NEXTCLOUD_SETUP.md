# Nextcloud Setup Guide

This guide will walk you through setting up Nextcloud Passwords synchronization with NexPass.

## Table of Contents

1. [What You Need](#what-you-need)
2. [Installing Nextcloud Passwords](#installing-nextcloud-passwords)
3. [Creating an App Password](#creating-an-app-password)
4. [Configuring NexPass](#configuring-nexpass)
5. [First Sync](#first-sync)
6. [Troubleshooting](#troubleshooting)
7. [Advanced Configuration](#advanced-configuration)

## What You Need

### Prerequisites

- **Nextcloud Server**: Version 25 or higher
  - Self-hosted instance, OR
  - Managed hosting (e.g., Hetzner, Nextcloud providers)
- **Nextcloud Passwords App**: Installed on your server
- **HTTPS**: Your Nextcloud server must use HTTPS (required for security)
- **NexPass App**: Installed on your Android device

### Why App Passwords?

App passwords are more secure than using your main Nextcloud password because:
- Limited scope (only access Passwords app)
- Revocable without changing main password
- Required if you use Two-Factor Authentication (2FA)
- Best practice for third-party apps

## Installing Nextcloud Passwords

### If You Self-Host Nextcloud

1. **Log in to Nextcloud** web interface as admin
2. **Click your profile** (top-right) → **Apps**
3. **Search for "Passwords"**
4. **Click "Download and enable"**
5. **Wait for installation** to complete
6. **Passwords app** now appears in your app menu

### If You Use Managed Nextcloud

1. **Contact your provider** or check their app catalog
2. **Install Passwords app** from available apps
3. Some providers pre-install it for you

### Verify Installation

1. **Open Nextcloud**
2. **Look for Passwords** in the top app menu
3. **Click it** - you should see the Passwords interface
4. **If not visible**, check Apps → Enabled apps

## Creating an App Password

### Step-by-Step Guide

1. **Log in to Nextcloud** web interface

2. **Open Settings**:
   - Click your **profile picture** (top-right)
   - Select **Personal settings** or **Settings**

3. **Navigate to Security**:
   - In the left sidebar
   - Click **Security**

4. **Scroll to "Devices & sessions"** section

5. **Create App Password**:
   - Look for **"App name"** or **"Create new app password"** field
   - Enter a name: `NexPass` or `NexPass Android`
   - Click **"Create new app password"**

6. **Copy the App Password**:
   - A long password appears (e.g., `xxxxx-xxxxx-xxxxx-xxxxx-xxxxx`)
   - **Copy it immediately** - you won't see it again!
   - Store it temporarily in a note or clipboard

7. **Important**:
   - Do NOT share this password
   - Do NOT use your main Nextcloud password in NexPass
   - Keep the app password secure

### Screenshot Reference

```
┌─────────────────────────────────────────┐
│ Security                                │
├─────────────────────────────────────────┤
│ Devices & sessions                      │
│                                         │
│ App name:                               │
│ ┌─────────────────────────────────────┐ │
│ │ NexPass                             │ │
│ └─────────────────────────────────────┘ │
│ [Create new app password]               │
│                                         │
│ ✅ App password created!                │
│                                         │
│ xxxxx-xxxxx-xxxxx-xxxxx-xxxxx          │
│ [Copy]                                  │
└─────────────────────────────────────────┘
```

## Configuring NexPass

### Entering Connection Details

1. **Open NexPass** on your Android device

2. **Unlock your vault** with master password or biometric

3. **Open Settings**:
   - Tap the ⋮ menu (top-right)
   - Select **Settings**

4. **Navigate to Nextcloud Sync**:
   - Scroll down
   - Tap **Nextcloud Sync**

5. **Enter Server URL**:
   - **Format**: `https://your-domain.com`
   - **Examples**:
     - Self-hosted: `https://cloud.example.com`
     - Hetzner: `https://your-account.on-hetzner.cloud`
     - Generic: `https://nextcloud.yourdomain.com`
   - **Important**: Must start with `https://`
   - **No trailing slash**: ❌ `https://cloud.example.com/` (wrong)
   - **No path**: ❌ `https://cloud.example.com/nextcloud` (wrong)

6. **Enter Username**:
   - Your Nextcloud username (same as web login)
   - Usually your email or a custom username
   - Case-sensitive

7. **Enter App Password**:
   - Paste the app password you created earlier
   - Include the dashes: `xxxxx-xxxxx-xxxxx-xxxxx-xxxxx`
   - Or paste without dashes (both work)

8. **Test Connection**:
   - Tap **"Test Connection"** button
   - Wait for verification (may take 5-10 seconds)
   - **Success**: ✅ "Connection successful"
   - **Failure**: See [Troubleshooting](#troubleshooting)

9. **Save Configuration**:
   - If test succeeded, tap **"Save"**
   - Settings are stored securely (encrypted)

### Configuration Example

```
Server URL:     https://cloud.example.com
Username:       john.doe
App Password:   xxxxx-xxxxx-xxxxx-xxxxx-xxxxx

[Test Connection] → ✅ Connection successful!
[Save]
```

## First Sync

### Initial Synchronization

After configuring Nextcloud:

1. **Navigate to Sync Settings**:
   - Settings → Nextcloud Sync

2. **Tap "Sync Now"**:
   - Button at the bottom of the screen

3. **Wait for Sync**:
   - Progress indicator appears
   - May take 10-30 seconds depending on vault size

4. **Check Results**:
   - **Success**: "Last sync: Just now" with ✅
   - **Failure**: Error message (see [Troubleshooting](#troubleshooting))

### What Happens During First Sync?

#### If Your NexPass Vault is Empty

- **Downloads** all passwords from Nextcloud server
- **Decrypts** them locally on your device
- **Imports** them into your vault

#### If Your Nextcloud Passwords is Empty

- **Uploads** all passwords from NexPass vault
- **Encrypts** them before transmission
- **Creates** them on Nextcloud server

#### If Both Have Passwords

- **Two-way sync**:
  - Passwords on NexPass → uploaded to Nextcloud
  - Passwords on Nextcloud → downloaded to NexPass
- **Duplicates** are merged (based on UUID)
- **Conflicts** resolved using last-write-wins

### Verify Sync Worked

1. **Check NexPass**:
   - Open vault list
   - Verify all expected passwords are present

2. **Check Nextcloud Web**:
   - Log in to Nextcloud
   - Open Passwords app
   - Verify passwords appear there

3. **Both Should Match**:
   - Same passwords in NexPass and Nextcloud
   - Same usernames, URLs, and notes

## Troubleshooting

### Common Issues

#### ❌ "Connection failed: Invalid URL"

**Problem**: Server URL format is incorrect

**Solutions**:
- ✅ Use `https://` prefix
- ✅ Remove trailing slash
- ✅ Remove any paths (like `/index.php`)
- ✅ Example: `https://cloud.example.com`

#### ❌ "Authentication failed"

**Problem**: Username or app password is wrong

**Solutions**:
- Verify username matches Nextcloud login
- Double-check app password (copy/paste to avoid typos)
- Recreate app password in Nextcloud settings
- Ensure username is case-sensitive correct

#### ❌ "SSL/TLS error"

**Problem**: HTTPS certificate is invalid or self-signed

**Solutions**:
- Ensure server has valid SSL certificate (Let's Encrypt is free)
- NexPass does NOT support self-signed certificates (security)
- Contact your hosting provider to fix SSL

#### ❌ "Nextcloud Passwords app not found"

**Problem**: Passwords app not installed on server

**Solutions**:
- Log in to Nextcloud web
- Go to Apps
- Search and install "Passwords" app
- Refresh and try again

#### ❌ "Network error"

**Problem**: Internet connection or server unreachable

**Solutions**:
- Check device has internet connection
- Verify server URL is correct and accessible in browser
- Check if server is online
- Try again with better network (Wi-Fi recommended)

#### ❌ "Sync conflict"

**Problem**: Same password modified on multiple devices

**Solutions**:
- NexPass uses **last-write-wins** conflict resolution
- Most recent change is kept
- Older change is overwritten
- Avoid editing same password on multiple devices simultaneously

### Testing Checklist

If sync isn't working, verify:

- [ ] Server URL starts with `https://`
- [ ] No trailing slash in server URL
- [ ] Username is correct (case-sensitive)
- [ ] App password copied correctly (all characters)
- [ ] Nextcloud Passwords app is installed
- [ ] Your device has internet connection
- [ ] Nextcloud server is reachable (test in browser)
- [ ] SSL certificate is valid (not self-signed)

### Viewing Detailed Errors

If you get an error:

1. **Note the exact error message**
2. **Check logs** (if available in Settings → About)
3. **Report issue** with:
   - Error message
   - Nextcloud version
   - NexPass version
   - Server type (self-hosted/managed)

Contact: [support@daguva.com](mailto:support@daguva.com)

## Advanced Configuration

### Manual Sync vs. Auto Sync

**Current**: Manual sync only
- Tap "Sync Now" to synchronize
- No background sync (planned for v1.1.0)

**Future** (v1.1.0+):
- Background sync with WorkManager
- Sync every 6-12 hours automatically
- User can still manually sync anytime

### Sync Frequency Recommendations

- **After creating passwords**: Sync immediately
- **Before editing**: Sync to get latest changes
- **After editing**: Sync to upload changes
- **Daily**: At least once per day if actively using
- **Before switching devices**: Sync on old device, then on new device

### Offline Changes

NexPass supports **offline mode**:

1. **No internet**: Create/edit passwords as normal
2. **Changes queued**: Stored in pending sync operations
3. **When online**: Sync automatically uploads queued changes

### Folder and Tag Sync

**Important Limitation**: Folders and tags currently do NOT sync with Nextcloud

- **Folders**: Local to each device (planned for v1.1.0)
- **Tags**: Local to each device
- **Workaround**: Use export/import to transfer folder structure

### Two-Factor Authentication (2FA)

If you use Nextcloud 2FA:

- **App passwords bypass 2FA** (by design)
- This is normal and secure
- Main Nextcloud login still requires 2FA
- App passwords have limited scope

### Multiple Devices

To use NexPass on multiple devices:

1. **Setup on first device**:
   - Configure Nextcloud sync
   - Sync your passwords

2. **Setup on second device**:
   - Install NexPass
   - Create same master password (or different if preferred)
   - Configure Nextcloud sync (same server, user, app password)
   - Sync to download passwords

3. **Keep in sync**:
   - Sync before making changes
   - Sync after making changes
   - Conflicts resolved automatically (last-write-wins)

### Revoking App Password

If you need to remove NexPass access:

1. **Log in to Nextcloud** web
2. **Settings** → **Security**
3. **Find NexPass** in Devices & sessions
4. **Click delete/revoke** icon (🗑️)
5. **NexPass can no longer sync** until you create a new app password

### Security Notes

- **Zero-knowledge**: Nextcloud server never sees plaintext passwords
- **Encryption**: Passwords encrypted before upload (AES-256-GCM)
- **Transport**: HTTPS (TLS 1.2+) for all communication
- **Authentication**: App passwords are hashed, not stored
- **Server storage**: Nextcloud stores encrypted passwords only

---

## Quick Reference Card

```
┌──────────────────────────────────────────────┐
│ Nextcloud Setup Quick Reference             │
├──────────────────────────────────────────────┤
│                                              │
│ 1. Install Nextcloud Passwords App          │
│    Nextcloud Web → Apps → Passwords         │
│                                              │
│ 2. Create App Password                      │
│    Settings → Security → App passwords      │
│    Name: NexPass                            │
│                                              │
│ 3. Configure NexPass                        │
│    Settings → Nextcloud Sync                │
│    - Server URL: https://your-server.com    │
│    - Username: your-username                │
│    - App Password: xxxxx-xxxxx-xxxxx...     │
│                                              │
│ 4. Test & Save                              │
│    Test Connection → ✅ → Save              │
│                                              │
│ 5. First Sync                               │
│    Tap "Sync Now" → Wait → Verify           │
│                                              │
└──────────────────────────────────────────────┘
```

---

**Need more help?** See [TROUBLESHOOTING.md](TROUBLESHOOTING.md) or contact [support@daguva.com](mailto:support@daguva.com)
