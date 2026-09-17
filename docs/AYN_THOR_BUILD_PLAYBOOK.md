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
- Slice 6: read-only `MAIN_MENU_CREDITS` context; candidate CI run `35142531415`, APK SHA-256 `672db851f231f312614096169a8c9cfa00858fd6b3db63f06a49574777dd4388`.
- Slices 1 through 6 were built by Linux/JDK 17 CI, installed on an AYN Thor, and manually hardware-validated. Slice 6 was validated on 2026-09-17.
- Slice 7: read-only Lobby/Setup context family; candidate CI run `35193073049`, candidate commit `9a5d3a8ed75c9b4aa3c9ec0c07502b3c1eefe5fa`, APK SHA-256 `3f2d573b31f87a82342486ffd303e7a359ec8b25061a26d4eda65016c8c07f56`, package `is.xyz.vcmi.thor` version `1.8.0` (`1800`). Slice 7 was installed and hardware-validated on 2026-09-17; the temporary candidate workflow was not promoted.

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

## Known-good GitHub ARM64 compile setup

Use the setup below as the baseline for future candidate branches. It is derived from successful run `35142531415` on 2026-09-16 and is intentionally branch-scoped; do not merge the temporary workflow into `ayn-thor-dual-screen`.

Key requirements:

- Ubuntu 24.04 and Temurin JDK 17.
- Recursive submodule checkout.
- A fresh `GRADLE_USER_HOME` under `RUNNER_TEMP`.
- VCMI's `dependencies-android-arm64-v8a` bundle and both official Conan profiles.
- `android-thor-release` with `arm64-v8a` and an empty `ANDROIDDEPLOYQT_OPTIONS` value, overriding the preset's `--aab` when an installable APK is required.
- `dailySigning` only for short-lived validation APKs. Production/release signing remains a separate release concern.
- The Qt 5.15 compatibility hook below must run before CMake configuration.

Qt 5.15.19 writes `android.bundle.enableUncompressedNativeLibs` into generated `gradle.properties` immediately before it invokes Gradle. Android Gradle Plugin 8.1 and newer reject that removed property. Inject the cleanup into the tracked wrapper because `androiddeployqt` copies that wrapper into the generated project and executes it after writing the property:

```sh
sed -i '/^CLASSPATH=/i sed -i "/android\\.bundle\\.enableUncompressedNativeLibs/d" "$APP_HOME/gradle.properties"' \
  android/gradlew
grep -F 'enableUncompressedNativeLibs/d' android/gradlew
```

The following workflow core is known good. Replace the branch, focused test filter/class, artifact name, and validation label for the approved slice while preserving the toolchain and compatibility steps:

```text
name: Thor candidate validation

on:
  push:
    branches:
      - ci/thor-sliceN-validation
  workflow_dispatch:

jobs:
  arm64:
    runs-on: ubuntu-24.04
    timeout-minutes: 90
    steps:
      - uses: actions/checkout@v7
        with:
          submodules: recursive
      - uses: actions/setup-java@v6
        with:
          distribution: temurin
          java-version: '17'
      - name: Isolate Gradle home
        run: echo "GRADLE_USER_HOME=$RUNNER_TEMP/gradle-home" >> "$GITHUB_ENV"
      - name: Install build dependencies
        run: |
          pipx install conan
          source CI/install_conan_dependencies.sh dependencies-android-arm64-v8a
          conan profile detect
          conan install . --output-folder=conan-generated --build=never \
            --profile=dependencies/conan_profiles/base/android-system \
            --profile=dependencies/conan_profiles/android-64-ndk \
            --conf='tools.cmake.cmaketoolchain:generator=Ninja'
      - name: Add Android Gradle compatibility hook
        run: |
          sed -i '/^CLASSPATH=/i sed -i "/android\\.bundle\\.enableUncompressedNativeLibs/d" "$APP_HOME/gradle.properties"' \
            android/gradlew
          grep -F 'enableUncompressedNativeLibs/d' android/gradlew
      - name: Run focused native context tests
        run: |
          boost_version="$(find "$HOME/.conan2/p" -path '*/p/include/boost/version.hpp' -print -quit)"
          test -n "$boost_version"
          boost_include="$(dirname "$(dirname "$boost_version")")"
          g++ -std=c++20 -pthread \
            -I. -Iinclude -Itest \
            -Itest/googletest/googletest \
            -Itest/googletest/googletest/include \
            -Itest/googletest/googlemock/include \
            -I"$boost_include" \
            lib/thor/ThorContext.cpp \
            test/thor/ThorContextTest.cpp \
            test/googletest/googletest/src/gtest-all.cc \
            test/googletest/googletest/src/gtest_main.cc \
            -o "$RUNNER_TEMP/thor-context-tests"
          "$RUNNER_TEMP/thor-context-tests" --gtest_filter='ThorContext*'
      - name: Configure Thor ARM64
        run: |
          cmake --preset android-thor-release \
            -DANDROID_ABI=arm64-v8a \
            -DANDROIDDEPLOYQT_OPTIONS= \
            '-DANDROID_GRADLE_PROPERTIES=signingConfig=dailySigning;applicationVariant=thor-ci'
      - name: Build Thor ARM64 APK
        run: cmake --build --preset android-thor-release
        env:
          ANDROID_STORE_PASSWORD: ${{ secrets.ANDROID_STORE_PASSWORD }}
          ANDROID_KEY_PASSWORD: ${{ secrets.ANDROID_KEY_PASSWORD }}
      - name: Run focused Android tests
        working-directory: out/build/android-thor-release/android-build
        run: |
          sdl_java_src_dir="$(cmake -N -LA .. | sed -n 's/^SDL_JAVA_SRC_DIR:STRING=//p')"
          ORG_GRADLE_PROJECT_SDL_JAVA_SRC_DIR="$sdl_java_src_dir" \
            ./gradlew --no-daemon :vcmi-app:testReleaseUnitTest \
              --tests eu.vcmi.vcmi.ThorContextIdsTest
      - name: Verify and checksum APK
        run: |
          apk="$(find out/build/android-thor-release/android-build/vcmi-app/build/outputs/apk/release \
            -type f -name '*.apk' -print -quit)"
          test -n "$apk"
          build_tools="$(find "$ANDROID_HOME/build-tools" -mindepth 1 -maxdepth 1 -type d \
            -printf '%f\n' | sort -V | tail -n 1)"
          "$ANDROID_HOME/build-tools/$build_tools/aapt" dump badging "$apk" \
            | grep "package: name='is.xyz.vcmi.thor'"
          cp "$apk" thor-candidate-arm64.apk
          sha256sum thor-candidate-arm64.apk | tee thor-candidate-arm64.apk.sha256
      - uses: actions/upload-artifact@v4
        with:
          name: thor-candidate-arm64
          path: |
            thor-candidate-arm64.apk
            thor-candidate-arm64.apk.sha256
          if-no-files-found: error
          retention-days: 7
```

Add focused native tests before configuration. Slice 6 compiled `lib/thor/ThorContext.cpp`, its GoogleTest file, and GoogleTest itself with the runner's `g++`, then ran the `ThorContext*` filter. This host-native method is deliberate: Android cross-compiled test executables cannot run directly on the x86-64 CI host.

Do not repeat these failed approaches:

- Removing the property from `$HOME/.gradle` or generated files before the CMake build: `androiddeployqt` writes it later.
- Isolating `GRADLE_USER_HOME` alone: useful for reproducibility, but it cannot suppress Qt's generated property.
- Patching a Qt template file: Qt 5.15.19 generates this property in `androiddeployqt`; the expected standalone template is not present in the Conan package layout.
- Passing `--no-build`: Qt then skips creation/copying of the Gradle wrapper needed for a separate packaging step.
- Using a job-level `${{ runner.temp }}` expression for Gradle home: write it to `GITHUB_ENV` from a shell step, where `RUNNER_TEMP` is available.
- Leaving the preset's `--aab` active when the deliverable must be installed with ADB: explicitly set `-DANDROIDDEPLOYQT_OPTIONS=` for an APK candidate.

The complete audited Slice 6 workflow remains on `origin/ci/thor-slice6-validation`; use it as the Git reference, but copy only the required branch-scoped workflow into a new candidate branch.

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

Artifacts have two useful digests: GitHub's ZIP artifact digest and the APK SHA-256 written inside the ZIP. Verify both before installation. Slice 4 reference values are ZIP `ecda46f91b5cdc93e67eed229cc74dda8b80b09a09b7dd2ec52bfaae3748e103` and APK `23fdc4b88421a623a5ebabb27c483d5babdb1a13a473f3d303e92b97e7cbe03c`. Slice 5 APK SHA-256 is `fc9a520ad032e03ac545328e4e7f6855a753ef9545f2c151116080a1e9626696`. Slice 6 APK SHA-256 is `672db851f231f312614096169a8c9cfa00858fd6b3db63f06a49574777dd4388` from successful run `35142531415`.

## AYN Thor installation and smoke check

Use the Android SDK platform tools at `C:\Users\steen\AppData\Local\Android\Sdk\platform-tools\adb.exe`. Wireless-debugging addresses and ports are transient: obtain the current value from the Thor before every session and always confirm the model before installing. If a direct address refuses the connection but the paired `_adb-tls-connect._tcp` device appears after restarting ADB, use that authenticated device serial.

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
