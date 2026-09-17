# AYN Thor build and device-validation playbook

This is the concise hand-off for creating and validating future AYN Thor Android slices. Read it with `AYN_THOR_BACKLOG.md`, which remains the approval and product-scope record.

## Validated starting point

- Repository: `https://github.com/CapnChaosDK/vcmi_thor`
- Implementation branch: `ayn-thor-dual-screen`
- Upstream baseline: `819259d97f1de9262b97811ccb081346c20ffef2`
- Thor package: `is.xyz.vcmi.thor`
- Slice 8 final commit: `ca3d7887d` — read-only Adventure Map, Hero, and Town contexts.
- Slice 8 candidate CI run: `35197790453`; Slice 8 is hardware validated.

`origin` is the Thor fork. Never push to `upstream`; its push URL is intentionally disabled.

## Permanent Thor CI

`.github/workflows/thor-ci.yml` is the one permanent Thor pipeline. Future slices must not create temporary `.github/workflows/thor-sliceN-validation.yml` files.

It has two validation levels:

1. Relevant pushes to `ayn-thor-dual-screen` and pull requests targeting it run a no-secret preflight. It restores the immutable Conan dependency archive when available and runs the focused host-native `ThorContext*` tests. Documentation-only changes do not trigger it.
2. A trusted push to `ci/thor-*-validation` runs that preflight, then a dependent full ARM64 candidate job. The full job uses Ubuntu 24.04, Temurin JDK 17, recursive submodules, the official Android Conan bundle and profiles, `android-thor-release`, `arm64-v8a`, the required Qt compatibility hook, validation signing, focused Android Thor tests, package verification, checksum generation, receipt generation, and artifact upload.

The full job cannot start until preflight succeeds. It never runs for pull requests, so signing secrets are not available to fork PRs or arbitrary branches. A manual dispatch from `ayn-thor-dual-screen` can request the same trusted full path with `full_arm64`.

The workflow concurrency group includes both workflow and ref. A new push cancels an obsolete run for that candidate branch without cancelling a different candidate or workflow.

### Caches and receipts

The dependency archive is cached separately from the Conan installation. Its key includes the runner, Android ARM64 identity, and the hash of `CI/install_conan_dependencies.sh`; changing the release definition therefore cannot reuse the old archive. The installer only downloads a missing archive and uses robust retrying downloads. Caches never contain credentials, signing material, APKs, or build directories.

The ARM64 compilation uses a 3 GB `ccache` directory cached by runner OS, Android NDK r29, ABI, schema version, and candidate ref. The broader restore prefixes let later commits on a candidate ref reuse safe compatible objects. `CCACHE_BASEDIR`, content-based compiler checking, and disabled directory hashing avoid ephemeral runner paths needlessly reducing reuse. The workflow passes the C and C++ launcher settings explicitly to CMake, proves the launchers are configured, verifies cacheable compiler calls, and includes the ccache statistics in the job summary and candidate artifact.

Gradle uses the checked-in wrapper and `gradle/actions/setup-gradle@v6` with its open/basic GitHub Actions cache provider. Do not set `GRADLE_USER_HOME` to `RUNNER_TEMP`; that would defeat the supported cache. Generated signed APKs and signing material are not cached.

Successful full candidates upload a seven-day artifact named `thor-candidate-arm64-<run-id>` containing:

- `thor-candidate-arm64.apk`
- `thor-candidate-arm64.apk.sha256`
- `thor-validation-receipt.json`
- `thor-ccache-stats.txt`

The JSON receipt records the real candidate commit, run ID and number, package ID, APK SHA-256, artifact name, Git ref, preset, ABI, and completed test/package gates. The GitHub job summary repeats the SHA, run ID, package, digest, artifact, test status, and ccache statistics.

## Candidate and promotion flow

```text
Implementation
    ↓
local cheap checks
    ↓
create ci/thor-sliceN-validation from ayn-thor-dual-screen
    ↓
push candidate branch
    ↓
permanent Thor CI preflight
    ↓
cached ARM64 build, Android tests, package/checksum/receipt
    ↓
manual AYN Thor hardware validation
    ↓
PASS
    ↓
promote exact validated product tree
```

Start a slice by confirming the repository and clean state:

```powershell
git fetch origin
git status --short
git branch --show-current
git log --oneline -5
git remote -v
```

Make approved product, test, and documentation changes on `ayn-thor-dual-screen`. Before candidate creation, run `git diff --check`, review the diff/stat, and do not discard unrelated work. Build an exact candidate from the implementation commit:

```powershell
git switch -c ci/thor-sliceN-validation
git add -- <approved slice files>
git commit -m "Candidate Thor Slice N build"
git push -u origin ci/thor-sliceN-validation
```

After CI succeeds, download the artifact into an ignored or explicitly excluded directory. Verify the GitHub artifact digest and the APK SHA-256 inside it, inspect the package ID, and retain the receipt with the candidate commit and run ID before installation. Install with `adb install -r -d` to preserve Thor package data.

Manual AYN Thor hardware validation remains mandatory. CI is not a substitute for visual, focus, touch, controller, panel-toggle, or pause/resume checks.

Before promotion, compare the candidate against the proposed implementation tip. This reports every product/build change after the hardware-tested commit while allowing only validation-documentation changes:

```powershell
$candidate = '<validated-candidate-sha>'
git diff --name-only "$candidate..ayn-thor-dual-screen" -- . `
  ':(exclude)AYN_THOR_BACKLOG.md' `
  ':(exclude)docs/**' `
  ':(exclude)**/*.md'
```

The command must produce no output before promoting. If it does, do not promote until the hardware-tested candidate and final product/build tree are reconciled. Then fast-forward or cherry-pick the exact validated product commit where possible; never recreate it manually. Documentation-only receipt updates are allowed after the candidate because they do not change the tested product/build tree. Confirm `git status --short` is clean and do not push `ayn-thor-dual-screen` unless explicitly asked.

## Build facts that matter

VCMI's Android application is assembled in stages: CMake configures the Qt/Android project, Qt deployment creates the Gradle project, then Gradle packages the APK. Building `android/` alone is not a substitute because native libraries and generated properties originate earlier.

The Thor-specific build boundary is `TARGET_AYN_THOR` and the `android-thor-release` preset. Preserve the normal package behavior and use `-DANDROIDDEPLOYQT_OPTIONS=` for an installable APK candidate rather than the preset's AAB default.

Qt 5.15.19 writes `android.bundle.enableUncompressedNativeLibs` into generated `gradle.properties` immediately before invoking Gradle. Android Gradle Plugin 8.1 and newer reject that removed property. The workflow injects this cleanup into the tracked wrapper before CMake configuration because `androiddeployqt` copies and runs that wrapper after generating the property:

```sh
sed -i '/^CLASSPATH=/i sed -i "/android\\.bundle\\.enableUncompressedNativeLibs/d" "$APP_HOME/gradle.properties"' android/gradlew
```

Do not remove this workaround without validating the same toolchain. Do not pass `--no-build`: Qt would then skip creating/copying the Gradle wrapper needed for packaging.

On this Windows workstation, use local checks for fast feedback only. The official `dependencies-android-arm64-v8a` bundle contains Linux-host Qt tools, and Android Studio's JDK 25 can have a Gradle cache-close limitation. Linux/JDK 17 CI is the authoritative full-package and test gate.

## AYN Thor installation and smoke check

Use the Android SDK platform tools at `C:\Users\steen\AppData\Local\Android\Sdk\platform-tools\adb.exe`. Wireless-debugging addresses and ports are transient: obtain the current value from the Thor before every session and confirm the model before installing.

```powershell
$device = '<current-ip:port-or-paired-adb-serial>'
adb kill-server
adb start-server
adb connect $device
adb -s $device get-state
adb -s $device shell getprop ro.product.model
adb -s $device install -r -d <verified-apk-path>
adb -s $device shell monkey -p is.xyz.vcmi.thor 1
adb -s $device shell pm path is.xyz.vcmi.thor
adb -s $device shell pidof is.xyz.vcmi.thor
```

Keep automated device work limited to connection, installation, launch, process/display/log checks, and state checks. The user performs the behavioral hardware validation.

## Game data notes

The Thor package has its own app data, so standard VCMI assets/saves are not automatically shared. Import the user's legitimate Heroes III installation through VCMI's normal import flow. Do not add proprietary game files to this repository or an APK.
