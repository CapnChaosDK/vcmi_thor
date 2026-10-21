# VCMI Builds

Small native Android app (Android 8 or later) for downloading APKs from GitHub Releases or GitHub Actions artifacts and opening Android's installer.

## Produce an APK on GitHub

1. Put this directory's contents at the root of a GitHub repository, including `.github/workflows/android.yml`.
2. Open **Actions → Build installer APK → Run workflow** (pushing also triggers it).
3. Once the build succeeds, download the **vcmi-builds-installer** artifact.
4. Extract `app-debug.apk`, transfer it to your phone, and open it to install.

The workflow builds a debug-signed APK. GitHub-hosted runners may generate a new debug signing key on each run, so later installer versions may require uninstalling this installer app first. This does not uninstall VCMI. A stable release signing key is needed for repeatable installer upgrades.

## Local build

Open this folder in Android Studio and install Android SDK Platform 35 when prompted. With Gradle 8.9 and Java 17 or 21 installed:

```
gradle :app:assembleDebug
```

Output: `app/build/outputs/apk/debug/app-debug.apk`.

## Use

- Enter `owner/repository` or its GitHub URL.
- Choose Releases or Actions artifacts. Enter a branch to filter Actions runs if desired.
- Public Releases do not require a token. Actions artifact downloads require a token; for a fine-grained token, give the selected repository **Actions: read** access. Private Releases additionally need **Contents: read** access.
- Tokens are kept in memory, not saved. Reenter after restarting the app.
- Check newest builds, choose a download, then tap Install downloaded APK. For archives containing several APKs, choose the APK matching your device ABI shown on screen.
- Android asks you to allow installation from this app and confirm each install. Updating VCMI requires compatible package IDs and signing certificates. The app cannot silently install or bypass Android's signature/version checks.

## Limits and validation

- No repository URL was supplied, so there is no preset repository or verified VCMI Thor build layout.
- Discovers the 30 most recent releases or successful Actions runs. Artifacts may be expired, or may contain no APK. Split APK sets and Android App Bundles are unsupported.
- Downloads require keeping the app open; background/resumable downloads are not implemented. Interrupting a download may require retrying.
- This project has not been compiled or tested on a device. The provided cloud environment lacked SDK/Gradle and its configured proxy refused connections, preventing tool installation.
