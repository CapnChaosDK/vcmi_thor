# VCMI Builds

Small native Android app (Android 8 or later) for downloading APKs from GitHub Releases or GitHub Actions artifacts and opening Android's installer.

## Produce an APK on GitHub

1. Keep this project under `tools/vcmi-builds` with `.github/workflows/vcmi-builds-helper.yml` at the repository root.
2. Push helper changes to `codex/vcmi-builds-helper` to run **Build VCMI helper APK**.
3. Once the build succeeds, download the **vcmi-builds-installer** artifact.
4. Verify the included checksum for `vcmi-builds.apk`, transfer it to your phone, and open it to install.

From 0.2.0, the workflow builds a release APK with a dedicated stable helper signing key. The repository secrets `VCMI_BUILDS_KEYSTORE_BASE64` and `VCMI_BUILDS_KEYSTORE_PASSWORD` must be configured; missing secrets fail the build rather than falling back to a disposable key. Signing material is decoded only into the runner's temporary directory and removed after the job; it is never uploaded as an artifact. Only trusted helper-branch pushes build signed packages, never pull requests. Keep the key backup and its password outside the repository. Earlier debug-signed versions require one uninstall/reinstall of the helper; this does not uninstall VCMI. Later updates signed with this key preserve helper data.

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
- Paste the token and tap **Save token** or **Check newest builds**. The token is encrypted with AES-GCM using a non-exportable Android Keystore key and stored in app-private preferences. It is restored after app/process restarts and device reboots, and retained across helper updates signed with the same key. App backups are disabled; uninstalling/clearing helper data removes it. GitHub expiration/revocation still requires a replacement token.
- **Forget token** removes the saved ciphertext and key, clears the field and download buttons, and does not revoke the token on GitHub. Token controls are disabled during requests. A missing/invalid key or damaged ciphertext fails closed with a recovery message; use Forget and save again. No plaintext fallback is used.
- Check newest builds, choose a download, then tap Install downloaded APK. For archives containing several APKs, choose the APK matching your device ABI shown on screen.
- Android asks you to allow installation from this app and confirm each install. Updating VCMI requires compatible package IDs and signing certificates. The app cannot silently install or bypass Android's signature/version checks.

## Limits and validation

- 0.2.0 includes device instrumentation using synthetic tokens in a separate record/key. Build `:app:assembleDebug :app:assembleDebugAndroidTest`, install both debug/test APKs, and run `adb shell am instrument -w dev.vcmi.builds.test/dev.vcmi.builds.TokenStoreInstrumentation`. It checks empty/replaced/restored tokens, nonce uniqueness, ciphertext tampering, corrupt records, missing keys, recovery and Forget. With both debug/release APKs signed by the stable key, run with `-e phase seed`, force-stop the helper, install the release APK with `adb install -r`, then run with `-e phase verify` to check storage across process restart and APK replacement. Remove the test package afterward. These checks never read the user's token.
- On device, save a real scoped token, list/download an Actions artifact, close/reopen the helper, reboot and reopen, then verify builds still load. Check Forget clears saved access, and saving a replacement restores it. Actual reboot and real-token download behavior require the user's confirmation; instrumentation is not a substitute.

- Version 0.1.1 corrects HTTP 415 when downloading Actions artifacts: the archive API request uses `application/vnd.github+json`, while Release asset downloads retain `application/octet-stream`. A live authenticated comparison on 2026-10-21 reproduced HTTP 415 with the old header and HTTP 302 with the corrected header. Complete download/install behavior on the Thor still requires device validation.

- Defaults to `CapnChaosDK/vcmi_thor`, Actions artifacts, and `ci/thor-candidate-validation`.
- Discovers the 30 most recent releases or successful Actions runs. Artifacts may be expired, or may contain no APK. Split APK sets and Android App Bundles are unsupported.
- Downloads require keeping the app open; background/resumable downloads are not implemented. Interrupting a download may require retrying.
- Version 0.1.0 passed GitHub assemble/lint/package checks and was installed and launched on an AYN Thor. Token-authenticated build listing worked on device; artifact downloading exposed the HTTP 415 fixed in 0.1.1. This does not establish a complete download/install device pass.
