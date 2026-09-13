# AYN Thor build and device-validation playbook

This is the concise hand-off for creating and validating future AYN Thor Android slices. Read it together with `AYN_THOR_BACKLOG.md`; that file is the approval and product-scope record.

## Validated starting point

- Repository: `https://github.com/CapnChaosDK/vcmi_thor`
- Branch: `ayn-thor-dual-screen`
- Upstream baseline: `819259d97f1de9262b97811ccb081346c20ffef2`
- Thor package: `is.xyz.vcmi.thor`
- Slice 1: `ed8e57130` — inert lower-screen command-deck foundation.
- Slice 2: `9300bcc59` — read-only `MAIN_MENU` context publication.
- Both slices were built by Linux/JDK 17 CI, installed on an AYN Thor, and manually hardware-validated on 2026-09-13.

The normal fork remote is `origin`. Never push to upstream; its push URL is intentionally disabled. Temporary CI validation branches have been deleted after use and must not be recreated without a focused, approved validation need.

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
4. Do not merge the validation workflow into `ayn-thor-dual-screen`. After validation, commit only the approved product/documentation changes and delete the temporary local and remote CI branch.
5. Download the artifact manually from the GitHub Actions run when browser download permissions prevent automation.

Artifacts have two useful digests: GitHub's ZIP artifact digest and the APK SHA-256 written inside the ZIP. Verify both before installation. The Slice 2 reference values are ZIP `bf26453182d82cf5ef6d9f7ad7a4c454e98f28f0ace9182dd311caa661d25f83` and APK `683c8213256d0e18277569dfa1f2972827654b9cdbd6b15b2681ccf27d3d672c`.

## AYN Thor installation and smoke check

Use the Android SDK platform tools at `C:\Users\steen\AppData\Local\Android\Sdk\platform-tools\adb.exe`. The Thor was previously reachable at `192.168.68.76:43311`; reconnect when needed, then confirm its model before installing.

```powershell
adb connect 192.168.68.76:43311
adb -s 192.168.68.76:43311 devices
adb -s 192.168.68.76:43311 shell getprop ro.product.model
adb -s 192.168.68.76:43311 install -r -d <verified-apk-path>
adb -s 192.168.68.76:43311 shell monkey -p is.xyz.vcmi.thor 1
```

`-r -d` updates the compatible package while preserving the user's installed game data. Launch through the launcher or `monkey`: `VcmiSDLActivity` is not exported, so direct shell launching is not supported.

Keep automated device work limited to connection, install, launch, process/display/log checks, and state checks. The user performs touch, controller, focus, panel-toggle, pause/resume, and visual checks.

For the validated lower deck, check that:

- the game remains on the upper display and one deck appears on the lower display;
- the lower deck is non-focus-stealing and inert;
- lower-panel toggle and app pause/resume do not duplicate or strand the presentation;
- Slice 2 shows `Main menu / Choose a game mode` at the main menu and logs monotonic `MAIN_MENU` revisions.

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
