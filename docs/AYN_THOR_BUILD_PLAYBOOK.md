# AYN Thor build and device-validation playbook

This is the concise hand-off for creating and validating future AYN Thor Android slices. Read it together with `AYN_THOR_BACKLOG.md`; that file is the approval and product-scope record.

## Validated starting point

- Repository: `https://github.com/CapnChaosDK/vcmi_thor`
- Branch: `ayn-thor-dual-screen`
- Upstream baseline: `819259d97f1de9262b97811ccb081346c20ffef2`
- Thor package: `is.xyz.vcmi.thor`
- Slice 1: `ed8e57130` — inert lower-screen command-deck foundation.
- Slice 2: `9300bcc59` — read-only `MAIN_MENU` context publication.
- Slice 3: promoted read-only `MAIN_MENU_NEW_GAME` context.
- Slice 4: `17fbdfbb9` — read-only `MAIN_MENU_LOAD_GAME` context.
- Slice 5: read-only `MAIN_MENU_CAMPAIGN` context; candidate CI run `35003735325`.
- Slices 1 through 5 were built by Linux/JDK 17 CI, installed on an AYN Thor, and manually hardware-validated. Slice 5 was validated on 2026-09-15.

The normal fork remote is `origin`. Never push to upstream; its push URL is intentionally disabled. A temporary CI validation branch is permitted only for a focused, approved candidate and its workflow must never be merged into `ayn-thor-dual-screen`.

## Git setup and compile best practice

Keep product work, CI-only packaging changes, and generated output separate. The implementation branch is `ayn-thor-dual-screen`; `origin` is the CapnChaosDK fork and `upstream` is read-only. Start every slice by recording the state and confirming that unrelated work is either absent or understood:

```powershell
git fetch origin
git status --short
git branch --show-current
git log --oneline -5
git remote -v
```

Make approved source, test, and documentation changes on `ayn-thor-dual-screen`. Before compiling, run `git diff --check` and review `git diff --stat`. Do not build from a dirty checkout containing unrelated changes, and do not use `git reset --hard` or `git checkout --` to clean it.

For a complete Thor APK, create a temporary candidate branch from the exact implementation state, commit only the approved slice, and push it to `origin`:

```powershell
git switch -c ci/thor-sliceN-validation
git add -- <approved slice files>
git commit -m "Candidate Thor Slice N build"
git push -u origin ci/thor-sliceN-validation
```

The candidate workflow must be branch-scoped, use Linux/JDK 17, configure the `android-thor-release` preset with `TARGET_AYN_THOR`, run focused native and Android tests, build the ARM64 APK, verify `is.xyz.vcmi.thor`, calculate the APK SHA-256, and upload the APK plus checksum. Keep that workflow only on the candidate branch; never merge it into `ayn-thor-dual-screen` or push it to `upstream`.

After CI succeeds, download the artifact into an ignored or explicitly excluded output directory. Compare the local APK SHA-256 with the CI checksum, inspect the package ID, and record the source revision, CI run, APK path, checksum, and package ID before installing. Install with `adb install -r -d` to preserve the Thor package data. The user performs visual, focus, touch, controller, toggle, and pause/resume validation.

After hardware PASS, switch back to `ayn-thor-dual-screen`, keep the approved product/test/documentation changes, remove any candidate-only workflow from the implementation checkout, review the complete diff, and create one focused local implementation commit. Confirm `git status --short` is clean and do not push the implementation branch unless explicitly requested. Candidate branches and artifacts may be retained for audit or removed separately after the validated commit; never mix them into the product commit.

## Build facts that matter

VCMI's Android application is assembled in stages: CMake configures the Qt/Android project, Qt deployment creates the Gradle project, then Gradle packages the APK. Building `android/` alone is not a substitute because the native libraries and generated properties originate earlier.

The Thor-specific build boundary is the `TARGET_AYN_THOR` definition and `android-thor-release` CMake preset. Keep Thor-only behavior behind that boundary and preserve the normal package behavior.

On this Windows workstation, use local checks for fast feedback only:

- The official `dependencies-android-arm64-v8a` bundle contains Linux-host Qt tools such as `moc` and `androiddeployqt`, so it cannot complete a Windows-host APK build.
- Android Studio's JDK 25 can compile the changed Java sources but may fail while closing an Android dependency-cache JAR. Treat that as a host/JDK limitation after confirming source compilation; do not mislabel it as a product failure.
- Linux with JDK 17 is the authoritative full-package and test gate.
- Keep Conan, CMake, Ninja, NDK, and transient build output isolated from tracked source. Use a short temporary drive mapping only when Windows path length requires it.

## CI candidate protocol

1. Confirm a clean working tree and record the current commit before a new slice.
2. Obtain explicit approval for the proposed slice and its hardware checklist before modifying native/gameplay code.
3. Use a temporary, branch-scoped Linux/JDK 17 workflow to produce an ARM64 candidate. It should configure the Thor CMake preset, run focused tests, package the APK, write an APK SHA-256 file, and upload the APK plus checksum as a short-retention artifact.
4. Do not merge the validation workflow into `ayn-thor-dual-screen`. After validation, commit only the approved product/documentation changes; retain or delete the temporary candidate branch according to the needed audit history.
5. Download the artifact manually from the GitHub Actions run when browser download permissions prevent automation or anonymous GitHub API rate limits prevent retrieval.
6. Upload the validation artifact before any known-baseline lint step. Keep the complete lint report, then enforce a focused delta that rejects new non-baseline diagnostics in the changed Slice files.

Artifacts have two useful digests: GitHub's ZIP artifact digest and the APK SHA-256 written inside the ZIP. Verify both before installation. Slice 4 reference values are ZIP `ecda46f91b5cdc93e67eed229cc74dda8b80b09a09b7dd2ec52bfaae3748e103` and APK `23fdc4b88421a623a5ebabb27c483d5babdb1a13a473f3d303e92b97e7cbe03c`. Slice 5 APK SHA-256 is `fc9a520ad032e03ac545328e4e7f6855a753ef9545f2c151116080a1e9626696`.

## AYN Thor installation and smoke check

Use the Android SDK platform tools at `C:\Users\steen\AppData\Local\Android\Sdk\platform-tools\adb.exe`. Wireless-debugging addresses and ports are transient: obtain the current value from the Thor before every session. The most recently validated endpoint was `192.168.68.61:40141`; always confirm the model before installing.

```powershell
$device = '192.168.68.61:40141' # Replace with the current Wireless debugging address.
adb connect $device
adb -s $device get-state
adb -s $device shell getprop ro.product.model
adb -s $device install -r -d <verified-apk-path>
adb -s $device shell monkey -p is.xyz.vcmi.thor 1
adb -s $device shell pm path is.xyz.vcmi.thor
adb -s $device shell pidof is.xyz.vcmi.thor
```

`-r -d` updates the compatible package while preserving the user's installed game data. Launch through the launcher or `monkey`: `VcmiSDLActivity` is not exported, so direct shell launching is not supported.

Keep automated device work limited to connection, install, launch, process/display/log checks, and state checks. The user performs touch, controller, focus, panel-toggle, pause/resume, and visual checks.

For the validated lower deck, check that:

- the game remains on the upper display and one deck appears on the lower display;
- the lower deck is non-focus-stealing and inert;
- lower-panel toggle and app pause/resume do not duplicate or strand the presentation;
- Main Menu shows `Main menu / Choose a game mode`; New Game, Campaign, Load Game, and Credits each show their approved local card.
- Back from Campaign restores New Game; Back from Credits restores Main Menu; malformed and mod-added menu names must fail closed rather than retaining a prior card.

## Game data notes

The Thor package has its own app data, so standard VCMI assets/saves are not automatically shared. Import the user's legitimate GOG Heroes III installation through VCMI's normal import flow. Android's picker can hide `.exe` files; when needed, a byte-identical `.zip` copy of the installer is a practical picker-visible source because VCMI identifies the file contents. Retain the matching data files. Do not add proprietary game files to this repository or an APK.

## Fresh-chat hand-off checklist

Before further work, run or inspect:

```powershell
git status --short
git branch --show-current
git log --oneline -3
git remote -v
```

Then read this playbook and the current-state/next-action sections of `AYN_THOR_BACKLOG.md`. Propose one small, read-only context increment and wait for approval. Native commands, coordinate injection, hidden-information inference, and any mutable gameplay behavior remain outside the approved scope until separately designed and approved.
