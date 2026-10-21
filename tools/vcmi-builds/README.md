# VCMI Builds

Small native Android app (Android 8 or later) for downloading APKs from GitHub Releases or GitHub Actions artifacts and opening Android's installer.

## Produce an APK on GitHub

1. Keep this project under `tools/vcmi-builds` with `.github/workflows/vcmi-builds-helper.yml` at the repository root.
2. Push helper changes to `codex/vcmi-builds-helper` to run **Build VCMI helper APK**.
3. Once the build succeeds, download the **vcmi-builds-installer** artifact.
4. Verify the included checksum for `vcmi-builds.apk`, transfer it to your phone, and open it to install.

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

- Version 0.1.1 corrects HTTP 415 when downloading Actions artifacts: the archive API request uses `application/vnd.github+json`, while Release asset downloads retain `application/octet-stream`. A live authenticated comparison on 2026-10-21 reproduced HTTP 415 with the old header and HTTP 302 with the corrected header. Complete download/install behavior on the Thor still requires device validation.

- Defaults to `CapnChaosDK/vcmi_thor`, Actions artifacts, and `ci/thor-candidate-validation`.
- Discovers the 30 most recent releases or successful Actions runs. Artifacts may be expired, or may contain no APK. Split APK sets and Android App Bundles are unsupported.
- Downloads require keeping the app open; background/resumable downloads are not implemented. Interrupting a download may require retrying.
- Version 0.1.0 passed GitHub assemble/lint/package checks and was installed and launched on an AYN Thor. Token-authenticated build listing worked on device; artifact downloading exposed the HTTP 415 fixed in 0.1.1. This does not establish a complete download/install device pass.
