# Troubleshooting Guide

Common issues and solutions for NexPass.

## Table of Contents

1. [Unlock Issues](#unlock-issues)
2. [Autofill Issues](#autofill-issues)
3. [Sync Issues](#sync-issues)
4. [Password Generator Issues](#password-generator-issues)
5. [Import/Export Issues](#import-export-issues)
6. [Performance Issues](#performance-issues)
7. [App Crashes](#app-crashes)
8. [Data Loss Prevention](#data-loss-prevention)

---

## Unlock Issues

### Cannot Remember Master Password

**Problem**: Forgot master password and cannot unlock vault

**Bad News**: There is NO password recovery mechanism by design (zero-knowledge security)

**Your Options**:

1. **If you have an export backup**:
   - Uninstall and reinstall NexPass
   - Create new master password
   - Import your backup file
   - Enter export password (hopefully different from master password!)

2. **If you don't have a backup**:
   - Your data is **permanently inaccessible**
   - This is by design for security
   - You'll need to start fresh

**Prevention**:
- Write down master password and store securely (safe, lockbox)
- Create regular export backups with different password
- Use a memorable passphrase instead of random characters

### Biometric Unlock Not Working

**Problem**: Fingerprint/Face unlock fails or not available

**Solutions**:

1. **Re-enable biometric**:
   - Settings → Security → Biometric Unlock
   - Toggle OFF then ON
   - Follow setup prompts

2. **Check device biometric**:
   - Android Settings → Security → Biometric
   - Ensure fingerprint/face is registered
   - Test in other apps

3. **Fallback to password**:
   - Biometric unlock always has password fallback
   - Enter master password if biometric fails

4. **Device limitations**:
   - Some devices don't support biometric crypto
   - NexPass will disable biometric on these devices
   - Password unlock only

### "Decryption Failed" Error

**Problem**: Vault unlocks but passwords show decryption errors

**Possible Causes**:
- Database corruption
- Interrupted encryption process
- Device storage issues

**Solutions**:

1. **Restart the app**:
   - Force close NexPass
   - Clear from recent apps
   - Reopen and unlock

2. **Restore from backup**:
   - If you have an export, import it
   - This may recover corrupted entries

3. **Check device storage**:
   - Ensure device has free space (>100MB)
   - Clear cache if needed

4. **Last resort**:
   - Report issue to [support@daguva.com](mailto:support@daguva.com)
   - Include Android version, device model

---

## Autofill Issues

### Autofill Not Appearing

**Problem**: Autofill suggestions don't show up in apps/browsers

**Solutions**:

1. **Enable Autofill Service**:
   - NexPass Settings → Enable Autofill
   - Android Settings opens
   - Select NexPass as autofill service
   - Grant permissions

2. **Verify autofill is active**:
   - Android Settings → System → Languages & input
   - Autofill service → Should show "NexPass"

3. **Restart the app**:
   - Force close the app where autofill should appear
   - Reopen it
   - Try login form again

4. **Check Android version**:
   - Autofill requires Android 8.0+ (API 26+)
   - NexPass requires Android 10+ (API 29+)

### No Matching Passwords

**Problem**: Autofill says "No passwords found"

**Solutions**:

1. **Add URL to password**:
   - Edit the password in NexPass
   - Add website URL (e.g., "https://gmail.com")
   - Save and try autofill again

2. **Check package name** (for apps):
   - Autofill matches app package name
   - Some apps use non-standard packages
   - Try creating password with app-specific name

3. **Manual selection**:
   - Tap "Show all passwords" in autofill popup
   - Manually select the correct one

4. **Fuzzy matching**:
   - NexPass uses fuzzy matching
   - Should match subdomains (login.example.com → example.com)
   - If not working, check URL exactly matches

### Autofill Requires Unlock Every Time

**Problem**: Must unlock vault for every autofill

**Cause**: This is normal behavior when vault is locked

**Solutions**:

1. **Adjust auto-lock timeout**:
   - Settings → Auto-Lock Timeout
   - Set to 15 or 30 minutes
   - Vault stays unlocked longer

2. **Use biometric unlock**:
   - Much faster than typing master password
   - Settings → Biometric Unlock → Enable

3. **Keep vault unlocked**:
   - Set Auto-Lock to "Never" (⚠️ less secure)
   - Only recommended if device is always in your possession

### Autofill Overlaps with Keyboard

**Problem**: Autofill dropdown blocked by keyboard

**Solutions**:

1. **Dismiss keyboard**:
   - Tap back button or hide keyboard
   - Autofill suggestions should remain visible

2. **Scroll the form**:
   - Sometimes scrolling reveals autofill dropdown

3. **Android limitation**:
   - Some apps have autofill display issues
   - Use manual copy/paste as workaround

---

## Sync Issues

### "Connection Failed" Error

**Problem**: Cannot connect to Nextcloud server

**Solutions**:

See [Nextcloud Setup Guide - Troubleshooting](NEXTCLOUD_SETUP.md#troubleshooting) for detailed solutions.

Quick checks:
- Verify server URL format: `https://domain.com`
- Check internet connection
- Test URL in browser
- Verify SSL certificate is valid

### "Authentication Failed" Error

**Problem**: Username or password rejected

**Solutions**:

1. **Recreate app password**:
   - Log in to Nextcloud web
   - Settings → Security
   - Delete old app password
   - Create new one
   - Update in NexPass

2. **Check username**:
   - Must match Nextcloud login username exactly
   - Case-sensitive

3. **Don't use main password**:
   - Use app password, NOT Nextcloud main password

### Sync Never Completes

**Problem**: Sync hangs at "Syncing..."

**Solutions**:

1. **Check network**:
   - Ensure stable internet (Wi-Fi recommended)
   - Mobile data may be slow/unreliable

2. **Large vault**:
   - First sync with 100+ passwords can take time
   - Wait up to 2-3 minutes

3. **Restart sync**:
   - Force close NexPass
   - Reopen and try sync again

4. **Check server**:
   - Nextcloud server may be slow/overloaded
   - Try again later

### Passwords Not Syncing

**Problem**: Changes don't appear on other devices

**Solutions**:

1. **Manual sync required**:
   - Currently no background sync
   - Must tap "Sync Now" manually
   - Do this on BOTH devices

2. **Sync workflow**:
   - Device A: Make changes → Sync Now
   - Device B: Sync Now → See changes

3. **Check sync status**:
   - Settings → Nextcloud Sync
   - Look for "Last sync: ..." timestamp
   - If old, sync again

4. **Offline changes**:
   - Changes made offline queue for sync
   - Must sync once online to upload

### Folders Not Syncing

**Known Limitation**: Folders and tags do NOT sync with Nextcloud in v1.0.0

**Workaround**:
1. Export vault on device A (includes folders/tags)
2. Import on device B
3. Folder structure transfers via export/import

**Future**: Folder sync planned for v1.1.0

---

## Password Generator Issues

### Generator Creates Weak Passwords

**Problem**: Generated password seems weak

**Solutions**:

1. **Increase length**:
   - Character mode: Use 16+ characters
   - Passphrase mode: Use 4+ words

2. **Enable all character types**:
   - Uppercase: ✅
   - Lowercase: ✅
   - Numbers: ✅
   - Symbols: ✅

3. **Check strength indicator**:
   - Green = Very Strong (>100 bits entropy)
   - Aim for green always

### Cannot Use Generated Password

**Problem**: Website rejects generated password

**Cause**: Some websites have password restrictions

**Solutions**:

1. **Disable symbols**:
   - Some sites don't allow !@#$%
   - Turn off symbol characters

2. **Reduce length**:
   - Some sites have max length (e.g., 20 chars)
   - Adjust length slider

3. **Use passphrase mode**:
   - More compatible with restrictive sites
   - Still very secure (4+ words)

---

## Import/Export Issues

### "Export Failed" Error

**Problem**: Cannot export vault

**Solutions**:

1. **Check storage permission**:
   - Android Settings → Apps → NexPass → Permissions
   - Storage: Allowed

2. **Free up space**:
   - Ensure device has >50MB free
   - Delete old files if needed

3. **Try different location**:
   - Export to Downloads folder
   - Try internal storage vs. SD card

### "Import Failed - Invalid File" Error

**Problem**: Cannot import export file

**Solutions**:

1. **Verify file format**:
   - Must be `.nxp` file from NexPass export
   - Not compatible with other password managers

2. **Check export password**:
   - Must match password used during export
   - Case-sensitive

3. **File not corrupted**:
   - Try re-downloading/transferring export file
   - Ensure complete file transfer

4. **Wrong import mode**:
   - Choose "Merge" for adding to existing vault
   - Choose "Replace" for fresh start (⚠️ deletes current vault)

### "Import Failed - Decryption Error"

**Problem**: Wrong export password entered

**Solutions**:

1. **Verify export password**:
   - Not the same as master password
   - Password you chose during export

2. **Try other passwords**:
   - Check if you used different password
   - Check password manager (if you saved it)

3. **Contact support**:
   - If genuinely forgot export password
   - Data in export file is **permanently inaccessible**

---

## Performance Issues

### App is Slow/Laggy

**Problem**: NexPass runs slowly

**Solutions**:

1. **Restart app**:
   - Force close and reopen
   - Clears temporary memory

2. **Clear app cache**:
   - Android Settings → Apps → NexPass → Storage
   - Clear Cache (NOT Clear Data!)

3. **Large vault**:
   - Vaults with 500+ passwords may be slower
   - Consider organizing into folders
   - Filter/search instead of scrolling

4. **Device resources**:
   - Close other apps
   - Restart device
   - Check available RAM

### Search is Slow

**Problem**: Password search takes time

**Solutions**:

1. **Database indexing**:
   - First search may be slower
   - Subsequent searches are faster

2. **Be specific**:
   - Type more characters for faster results
   - Use exact matches when possible

---

## App Crashes

### App Crashes on Launch

**Problem**: NexPass immediately crashes when opened

**Solutions**:

1. **Update Android**:
   - Ensure running Android 10+ (API 29+)
   - Update to latest security patch

2. **Reinstall app**:
   - ⚠️ **EXPORT VAULT FIRST if possible**
   - Uninstall NexPass
   - Reinstall from source
   - Import vault

3. **Clear app data**:
   - ⚠️ **This deletes ALL data - export first!**
   - Android Settings → Apps → NexPass → Storage
   - Clear Data

4. **Report crash**:
   - Email [support@daguva.com](mailto:support@daguva.com)
   - Include:
     - Android version
     - Device model
     - NexPass version
     - Steps to reproduce

### App Crashes During Sync

**Problem**: NexPass crashes when syncing

**Solutions**:

1. **Check network stability**:
   - Use Wi-Fi instead of mobile data
   - Ensure strong signal

2. **Reduce vault size**:
   - Large vaults (500+ passwords) may cause issues
   - Try syncing in batches (export/import subsets)

3. **Update Nextcloud**:
   - Ensure server runs latest Nextcloud version
   - Update Passwords app

4. **Report crash**:
   - Include sync logs if available
   - Note vault size (approx. number of passwords)

---

## Data Loss Prevention

### How to Avoid Losing Data

**Best Practices**:

1. **Export regularly**:
   - Weekly exports recommended
   - Store in 2+ secure locations
   - Test imports periodically

2. **Sync frequently**:
   - Before and after making changes
   - Provides cloud backup

3. **Write down master password**:
   - Store in physical safe/lockbox
   - Separate from device

4. **Test backups**:
   - Periodically import an export
   - Verify it works BEFORE you need it

### What to Do If Data is Lost

**If you have an export backup**:
1. Reinstall NexPass (if needed)
2. Create new master password
3. Import export file
4. Enter export password
5. Data restored ✅

**If you have Nextcloud sync**:
1. Reinstall NexPass (if needed)
2. Create new master password (can be different)
3. Configure Nextcloud sync (same server/credentials)
4. Sync Now → Downloads all passwords
5. Data restored ✅

**If you have neither**:
- Data is permanently lost
- This is by design (zero-knowledge security)
- Start fresh and create backups going forward

---

## Getting More Help

### Contact Support

If you've tried everything above and still have issues:

- **Email**: [support@daguva.com](mailto:support@daguva.com)
- **GitHub Issues**: [https://github.com/codegax/nexpass/issues](https://github.com/codegax/nexpass/issues)

### Information to Include

When reporting issues, please provide:

- **Android version** (e.g., Android 13)
- **Device model** (e.g., Samsung Galaxy S21)
- **NexPass version** (Settings → About → Version)
- **Steps to reproduce** the problem
- **Error messages** (exact text)
- **Screenshots** (if applicable)
- **Recent changes** (new install, update, etc.)

### Before Contacting Support

Please:
- [ ] Check this troubleshooting guide
- [ ] Search GitHub Issues for similar problems
- [ ] Try restarting the app
- [ ] Export your vault (if possible) as backup
- [ ] Note exact error messages

---

**Most issues can be resolved quickly!** If stuck, we're here to help at [support@daguva.com](mailto:support@daguva.com) 🔧
