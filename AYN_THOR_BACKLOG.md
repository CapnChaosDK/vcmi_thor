# AYN Thor dual-screen Heroes III backlog

This is the maintained planning and validation record for the AYN Thor fork. Do not remove deferred work when implementing an earlier slice. Before each implementation slice, record the proposed behavior, implementation boundary, native and Android responsibilities, focused acceptance tests, and regression risks, then wait for user approval.

Status values: `planned`, `proposed`, `approved`, `in progress`, `awaiting hardware validation`, `hardware validated`, `blocked`, `deferred`.

## Current state

- Phase: Slices 1 through 16 are implemented, CI-built, and hardware-validated.
- Status: `hardware validated` through Slice 16.
- Upstream reference: `https://github.com/vcmi/vcmi.git`, default branch `develop`.
- Baseline: upstream commit `819259d97f1de9262b97811ccb081346c20ffef2`.
- Fork: `https://github.com/CapnChaosDK/vcmi_thor`, public.
- Working branch: `ayn-thor-dual-screen`, published from the clean upstream baseline.
- Remote safety: `origin` points to the CapnChaosDK fork; the local `upstream` push URL is disabled.
- Repository discovery: complete; see `docs/AYN_THOR_DISCOVERY.md`.
- Reproducible build/device procedure: `docs/AYN_THOR_BUILD_PLAYBOOK.md`.
- Build readiness: use Linux/JDK 17 CI for a complete ARM64 APK. This Windows host is useful for focused source checks, but the official dependency cache contains Linux-host Qt generators and Android Studio's JDK 25 has a Gradle cache-close limitation.
- Published implementation: Slice 1 commit `ed8e57130`; Slice 2 commit `9300bcc59`; Slice 3 promoted after hardware validation; Slice 4 commit `17fbdfbb9`; Slice 5 commit `9b8664177`; Slice 6 promoted after hardware validation; Slice 7 commit `981d2b65a`; Slice 8 promoted after hardware validation; Slice 10 product commit `8bd603d6684d107e6f72e48fbb9e04eaa4b38293`; Slice 11 product commit `873faeaedc153f400d39c2677cfe495bf3d07bd9`; Slice 12 product commit `221a9f9eba3614665bcce8e84c86868f1d252d20`; Slice 13 product commit `f12865c7fd4dd9208d14c928402702fffcc719bc`; Slice 14 product commit `e60e7e1051d08c5fae07be207182c3f175047429`; Slice 15 product commit `df97a1899dd9deb300a3b6b5fe21803e1e4bfcec`.
- Hardware validation: the lower command deck is visible and inert; it preserves upper-screen focus through resume/toggle checks. Slice 2 additionally shows the localized `Main menu / Choose a game mode` context and logs monotonic `MAIN_MENU` revisions. Slice 3 shows the localized New Game card, clears it safely on unsupported tabs, and passes panel-toggle, pause/resume, and input-regression checks. Slice 4 adds the localized Load Game card and restores the root card on Back. Slice 5 adds the localized Campaign card and restores New Game on Back. Slice 6 adds the localized Credits card and restores Main Menu on exit. Slice 7 adds localized lobby/setup cards and restores them after unsupported children close. Slice 8 adds localized Adventure Map, Hero, and Town cards, restores approved parents through normal activation, and clears unsupported child/battle contexts safely. Slice 9 adds Hero Meeting and Battle lifecycle cards, including the corrected Battle Result restoration path. Slice 10 adds Kingdom Overview, Quest Log, Scenario Journal, Puzzle Map, and Save Game cards through their concrete native owners; every focused manual check passed after the CI-built APK was installed on an AYN Thor. Slice 12 adds the native Next Hero, Move Hero, Sleep/Wake, and End Turn routes; its focused checks passed on the CI-verified APK, including ordinary End Turn confirmation, rapid/stale input safety, and upper-input regressions. All validated contexts pass transition, lifecycle, inertness, and upper-input checks; malformed and mod-added tabs continue to fail closed.
- Slice 13 promotion: product commit `f12865c7fd4dd9208d14c928402702fffcc719bc` adds the selected hero's localized name and movement snapshot to the Adventure Map header. The CI-built artifact passed all focused hardware checks, including no-selection fallback, movement/selection refresh, all eight existing commands, lifecycle/display recreation, and upper-input regressions.
- Slice 14 promotion: product commit `e60e7e1051d08c5fae07be207182c3f175047429` adds the read-only Hero Window dashboard. The CI-built artifact passed the complete Hero accuracy, hero-switching, long-text, child/modal restoration, lower-display recreation, inert-touch, Adventure command, and upper-input regression checklist on an AYN Thor.
- Slice 15 promotion: product commit `df97a1899dd9deb300a3b6b5fe21803e1e4bfcec` adds the read-only Town Window dashboard. The CI-built artifact passed the complete Town accuracy, construction/hero refresh, town switching, child/modal restoration, lower-display recreation, inert-touch, Adventure command, and upper-input regression checklist on an AYN Thor.
- Slice 16 validation: candidate commit `f4df29a2eaa0b64a42faae5ceac3d56e1549d270` adds the context-aware Battle command deck. The CI-built artifact passed focused native and Android tests, and the final AYN Thor hardware checklist passed after the post-opening publication refresh. The candidate remains on `ci/thor-slice16-validation`; promotion is a separate explicit operation.
- Approval to implement feature slices: yes; Slices 1 through 3 were approved by the user on 2026-09-13, Slice 4 on 2026-09-14, Slice 5 on 2026-09-15, Slice 6 on 2026-09-15, Slices 7 and 8 on 2026-09-17, and Slice 10 through the approved implementation brief on 2026-09-17.

## Working rules

- The native engine remains authoritative for gameplay and information visibility.
- The lower display supplements the primary game; it never replaces or captures its input paths.
- Use Android public multi-display APIs and select a presentation display dynamically. Never hardcode a transient display ID or rely only on a device model.
- Preserve upstream behavior on single-display devices and guard Thor-only work with a build flavor or equivalent compile-time boundary.
- Use a distinct application ID so the Thor fork has separate assets, settings, and saves and can coexist with the standard app.
- Process game mutations on the existing game/SDL thread through native operations.
- Treat all Android requests as untrusted: revision-lock them, consume once, and revalidate context, identity, bounds, ownership, destination, and enabled state natively.
- Invalidate queued input, gestures, selection, information, and visuals on context or lifecycle changes.
- Preserve VCMI's complete owner lifecycle when closing or replacing windows. A Thor context optimization must not batch-remove an inactive parent window if its normal reactivation/deactivation performs visual, input, audio, or restoration cleanup.
- Never bundle proprietary Heroes III artwork. Decode only player-installed assets and always retain a generic/text fallback.
- Keep automated device interaction brief: connection, installation, explicit activity launch, process/display/log/state checks. Manual hardware behavior is user-validated.
- Do not commit or push a slice until the user reports its focused hardware checks passed.
- Never push to the official upstream remote or rewrite validated history without explicit approval.
- Preserve unrelated working-tree changes.

## Discovery checklist

Status: `done`

Completed on 2026-09-13 and recorded in `docs/AYN_THOR_DISCOVERY.md`:

1. Repository identity, remotes, branch, revision, dirty state, project instructions, and fork/upstream relationship.
2. License obligations, attribution/notices, redistribution rules, Android package/release signing, and the project's documented policy for original Heroes III assets.
3. Engine entry point, main/game thread, event loop, screen/view stack, modal ownership, and transition/restoration behavior.
4. Android application modules, activity classes, JNI/native loading, SDL version and integration, surface lifecycle, rendering thread, pause/resume/recreation paths, and manifest declarations.
5. Touch, mouse, keyboard, hotkey, SDL controller, and device-controller paths, including focus behavior.
6. Existing display enumeration, `Presentation`, virtual/external display, display-listener, or secondary-surface support.
7. Concrete native owners and entry paths for main/new/load/settings/campaign/scenario menus, Adventure Map, heroes, towns/castles, meetings, battles, dialogs, save/load, rewards/level-up, editor, and any standalone battle mode.
8. Safest locations to publish stable contexts, enabled actions, bounded state snapshots, bounded visual snapshots, and one-shot semantic requests without duplicating rules.
9. Native build systems, Android Gradle/NDK configuration, formatting, lint, static analysis, unit/integration tests, packaging, CI workflows, release/tagging, and artifact naming.
10. Existing identifier-generation or contract-test facilities suitable for native/Android parity and collision checks.

### Discovery deliverable

The architecture report identifies the client/server threading model, Android Qt-launcher and SDL-game activities, rendering/input paths, absent multi-display support, real screen hierarchy, safe integration points, build/test/release gates, and licensing/asset constraints. It also records that the map editor is a separate Qt activity/process and therefore needs a later editor-specific bridge rather than being treated as an SDL context.

## Proposed Slice 1: inert dual-display presentation foundation

Status: `hardware validated`

This is deliberately smaller than a context bridge or command implementation. It proves packaging, display choice, lifecycle safety, and upper-screen independence before gameplay state crosses the boundary.

### 1. Proposed user-visible behavior

- A Thor-specific Android build installs alongside the standard build with separate app-owned data.
- The normal game launches and renders on the primary display exactly as upstream does.
- When Android exposes an eligible secondary presentation display, that display shows a non-interactive Heroes III-inspired generic frame with clearly separated information and future command regions.
- The frame contains generic/localizable text only and no proprietary artwork or gameplay controls.
- Connecting, enabling, disabling, removing, reordering, or recreating displays safely dismisses and recreates the presentation as needed.
- Pause, resume, activity recreation, and relaunch do not leave a leaked window, blank blocking surface, duplicated presentation, or stuck input.
- With no eligible secondary display, the app behaves like upstream and creates no companion surface.
- Upper touchscreen, mouse, keyboard, hotkeys, and physical controller continue to operate independently.

### 2. Implementation boundary

- Add a Thor build property/preset layered on VCMI's existing CMake-generated Gradle properties. Use a distinct release application ID such as `is.xyz.vcmi.thor` (and the normal debug suffix for debug packages) plus a generated `BuildConfig` feature flag; do not rename the Java namespace `eu.vcmi.vcmi`.
- Add an Android lifecycle controller owned by `VcmiSDLActivity`, using `DisplayManager` callbacks and an Android `Presentation`.
- Select from currently valid presentation-capable displays by capability/state rather than a fixed ID; keep selection policy deterministic and replace the presentation safely when the chosen display disappears.
- Add a bounded, static, non-focus-stealing companion view with localizable fallback strings and generic drawn styling.
- Add no JNI state bridge, semantic actions, snapshot protocol, gameplay data, asset decoding, coordinate injection, or changes to native rules in this slice.
- Do not modify the Qt `ActivityLauncher`, separate-process `ActivityMapEditor`, SDL event sources, `GameEngine`, window stack, callbacks, or server/network packets in this slice.

### 3. Native and Android responsibilities

Native responsibilities:

- Continue normal startup, rendering, event processing, persistence, and controller handling unchanged.
- Provide no new gameplay state or mutation entry point in Slice 1.
- No native source modification is planned for Slice 1.

Android responsibilities:

- Own display discovery, deterministic eligibility/selection, listener registration, presentation construction/dismissal, and lifecycle cleanup on the Android main thread.
- Keep the companion window non-focus-stealing and non-authoritative.
- Render only bounded generic UI resources included by the fork.
- Fail closed: exceptions, absent displays, unsupported APIs, or stale callbacks result in no lower presentation and do not disturb the game.

### 4. Focused acceptance tests

Automated/local:

1. The standard and Thor variants have distinct application IDs and data namespaces; the standard variant contains no active Thor presentation path.
2. The Thor CMake/Gradle generation produces the intended package without changing the standard package or generated provider authority; APK manifest/package inspection passes.
3. Display-selection tests cover zero, one, and multiple candidates; changed IDs/order; removed or disabled selected display; and deterministic replacement.
4. Lifecycle tests cover repeated start/resume/pause/stop/destroy and display callbacks without duplicate presentation creation, leaked windows, or operations after teardown.
5. The fallback view uses bounded/localizable text, safe dimensions, and no proprietary asset.
6. Android compilation and lint, targeted Java tests, existing native tests available in the configured build, JSON/include/Markdown validation, and packaging checks pass.

Focused Thor hardware checklist after build/install/explicit launch:

1. With both panels active, the game remains on the upper display and exactly one inert framed companion appears on the lower display.
2. Upper touch, mouse/keyboard where available, and physical controller still operate the game; touching the inert lower frame causes no game action or upper focus loss.
3. Toggle the lower panel off and on once: the presentation disappears and returns without relaunch, duplication, or a frozen upper game.
4. Background and resume once, then rotate/recreate only if the app normally permits it: both displays recover and the upper game remains usable.
5. Launch on a single-display Android configuration/device if practical: upstream behavior is unchanged and no error UI appears.

### 5. Important regression risks

- SDL activity subclass or library-version assumptions may make an apparently small lifecycle hook unsafe.
- A `Presentation` can steal focus, redirect controller/key events, leak its window, or outlive the activity if flags and teardown ordering are wrong.
- Android may expose several eligible displays or change IDs/order after a panel toggle; an underspecified policy can select the wrong surface or duplicate windows.
- Flavor/application-ID changes may break JNI library loading, file-provider authorities, save/asset paths, manifest placeholders, signing, deep links, or CI packaging.
- VCMI's Android package is generated by CMake/Qt before Gradle runs; treating `android/` as a standalone Gradle project would bypass required native libraries and generated properties.
- Activity recreation and display callbacks can race, producing stale callbacks or `WindowManager` failures.
- Styling based on density assumptions may clip or become unreadable on the actual lower panel.
- `VcmiSDLActivity.onDestroy()` terminates the process, so the presentation must be dismissed before delegating to the existing shutdown path.

## Roadmap

Each milestone must be decomposed further after repository discovery. No item below is approved merely by appearing here.

## Proposed next slice: measured deck shell and diagnostics

Status: `hardware validated`

With Slice 1 hardware-validated, the next bounded step is to turn the generic companion into a measured, read-only deck shell without crossing into gameplay mutation or input injection.

- Record the Thor secondary panel's observed metrics (1080 x 1240, presentation-capable display) and define density-safe margins, typography tiers, and region bounds.
- Keep the existing inert presentation and add only lifecycle/display diagnostics that can identify selection, replacement, dismissal, and recreation in logs.
- Define a stable shell contract for a future read-only context card: title, status, revision, and bounded content regions; do not publish gameplay state yet.
- Add focused layout/diagnostic tests and repeat the existing standard-build regression checks.

Acceptance requires the shell to remain inert, upper-screen input to remain unchanged, no stale presentation after lifecycle/display changes, and no proprietary artwork or game-data dependency.

## Proposed Slice 2: read-only context publication seam

Status: `hardware validated`

The first gameplay-facing increment should publish a bounded, revisioned read-only context record to the Android deck while leaving all native input and rules unchanged. The initial context is limited to a safe transitional/main-menu state so no hero, town, battle, or save data is exposed prematurely.

- Define a small native-owned record: context identifier, monotonically increasing revision, title/status strings, and an explicit `UNKNOWN` fallback.
- Publish immutable records through a thread-safe handoff; Android consumes the latest complete revision on the main thread and drops stale revisions.
- Render only the title/status in the existing deck shell; no buttons, hit regions, commands, JNI mutation calls, or coordinate injection.
- Add parity tests for revision ordering, invalidation to `UNKNOWN`, lifecycle restart, and standard-build isolation.

This slice required explicit approval before native gameplay code was modified.

Implementation started: `lib/thor/ThorContextStore` provides a mutex-protected latest-record handoff with monotonic revisions and an explicit `UNKNOWN` fallback. `CMainMenu::activate()` now publishes the first safe `MAIN_MENU` record through the existing Android VM helper; Java forwards it to the activity main thread, drops stale revisions, and renders localized read-only title/status text. Focused native store tests cover initial fallback, revision ordering, and empty identifiers. No command or mutable gameplay path exists.

Local validation: the new native store compiles with the Android NDK C++20 compiler, Android resource processing succeeds, and the modified Java sources compile without source errors. Gradle cannot mark the Java task successful on this host because Android Studio currently supplies JDK 25, whose compiler fails while closing an Android dependency cache file; Linux/JDK 17 CI remains the authoritative APK gate.

Hardware validation: the CI-built ARM64 APK was installed over the existing Thor app, preserving game data. The user confirmed the deck changes to “Main menu / Choose a game mode,” and device logs recorded `MAIN_MENU` context revisions 1–3 with no fatal exception.

## Approved Slice 3: read-only New Game submenu context

Status: `hardware validated`

Approved by the user on 2026-09-13. This slice adds one new read-only context, `MAIN_MENU_NEW_GAME`, within the existing main-menu owner.

### 1. Approved user-visible behavior

- Entering New Game changes the lower deck to `New game / Choose single-player, multiplayer, campaign, or tutorial`.
- Returning to the root menu restores the existing `Main menu / Choose a game mode` card.
- Load, Campaign, Credits, malformed, and other unsupported tabs publish `UNKNOWN` so the New Game label cannot remain stale.
- The lower deck remains inert and cannot change gameplay or upper-screen UI state.

### 2. Implementation boundary and responsibilities

- Native code maps configured main-menu tab names to stable context identifiers and owns the monotonic revision.
- Android maps the approved identifier to bounded localizable text and drops stale revisions as before.
- Thor-only behavior remains behind the existing build boundary.
- No action identifiers, buttons, hit regions, Java-to-native calls, gameplay data, coordinate injection, or mutable state are added.

### 3. Focused acceptance tests

- Native mapping covers `main`, `new`, every currently unsupported tab, and malformed input.
- Native and Android identifiers remain in parity; revisions remain monotonic and stale records remain rejected.
- Standard-build isolation, Android compilation/resources/tests/lint, focused native tests, and ARM64 packaging pass.

### 4. Focused Thor hardware checklist

1. The root menu shows the existing Main menu card; New Game shows the new card; Back restores the root card.
2. Load, Campaign, and Credits never retain the New Game card and instead show the safe fallback.
3. Panel toggle and pause/resume while on New Game restore exactly one presentation with the latest card.
4. Lower touch remains inert, and upper touch/controller focus and operation remain unchanged.

### 5. Important regression risks

- Configurable or mod-added tabs may have unexpected names or ordering; mapping must use names and fail to `UNKNOWN`.
- Activation and tab switching can publish close together; revisions must remain monotonic without leaving stale text.
- Presentation recreation must render the controller's latest complete record.
- New context rendering must not change focus, lifecycle cleanup, package identity, or standard-build behavior.

### Milestone 1: dual-screen foundation

- Slice 1: inert presentation foundation (proposed above).
- Define measured lower-panel layout metrics, safe margins, localization behavior, and resilient generic styling.
- Introduce only the minimum lifecycle diagnostics needed for brief device verification.

### Milestone 2: context publication and decks

- Define stable context identifiers and parity tests.
- Add revisioned native context publication and explicit invalidation.
- Add data-driven decks incrementally: safe unknown/transitional state, Main Menu, nested menus/dialogs, Adventure, Hero, Town/Castle, Hero Meeting, Battle, scenario/battle setup, save/load, campaigns, and editor.
- Require exact immediate-parent restoration and rapid-input suppression for each context slice.

### Milestone 3: semantic commands

- Define stable action identifiers, collision checks, native enabled-action representation, and one-shot revisioned requests.
- Consume and revalidate commands on the game thread.
- Migrate one context at a time to native semantic operations, retaining independent original input paths.

### Milestone 4: information cards

- Add bounded, revisioned, visibility-safe snapshots one context at a time: Adventure, Hero, Town/Castle, Battle, scenario/setup, and editor.
- Clear stale or unauthorized state immediately.

### Milestone 5: complete menus and dialogs

- Mirror actual menu hierarchy and native availability for new/load/settings/campaign/scenario/save/load/reward/recruitment/marketplace and modal workflows.
- Use generic navigation only for explicitly classified safe screens.

### Milestone 6: scenario and battle setup

- Mirror native player, faction, handicap/difficulty, hot-seat, readiness, terrain, reset/start/back rules.
- Keep complex loadout editing on the upper display pending separate approval.

### Milestone 7: Adventure maps

- Compact engine-owned minimap with revisioned, clamped viewport navigation.
- Expanded lower-only map navigation, fog-safe markers, relationship filters, fixed zoom levels, deterministic clustering and labels, and authorized stationary long-press information.
- Split these into separately approved slices with transformed hit-testing and gesture cancellation tests.

### Milestone 8: quick-selection lists

- Revisioned owned-hero and town lists in native order, bounded paging, stable identity, game-thread revalidation, focus/center through native paths, and exact Adventure restoration.

### Milestone 9: Hero Meeting armies

- Army snapshot and fixed-slot identity.
- Tap select/move/merge/swap.
- Touch-slop drag-and-drop.
- Exact native-validated long-press splitting.
- Existing whole-army operations.
- Multi-slot redistribution remains explicitly deferred.

### Milestone 10: Hero Meeting artifacts

- Artifact/Army mode switching with exact context retention.
- Stable slots, names/scroll identity, locked state, tap transfers/swaps, touch-slop drag-and-drop, and existing whole-inventory operations.
- Preserve native assembly and all derived-state updates.

### Milestone 11: player-installed visuals

- Bounded revision-locked portraits, creature sprites, artifact icons, and other approved engine-owned imagery decoded from player-installed assets.
- Preserve aspect ratio, clear on invalidation, and always provide generic/text fallbacks.
- Measure memory, frame pacing, battery, and lifecycle behavior before any large or continuous visual transport.

### Milestone 12: haptics

- Persistent setting that respects Android system touch feedback.
- Exactly one tick for an accepted lower-screen semantic action or approved local transition; no feedback for invalid, stale, cancelled, selection-only, inspection, navigation gestures, or non-lower-screen inputs.
- Test duplicate suppression across Android acceptance and native completion.

## Cross-cutting automated contract backlog

- Native/Android context and action identifier parity.
- Action-mask collision and representation limits.
- Snapshot text, count, dimension, and payload bounds; malformed payload rejection.
- One-shot consumption and duplicate-pending rejection.
- Stale-revision, wrong-context, invalid-index/identity, ownership, destination, and enabled-state rejection.
- Modal and lifecycle invalidation.
- Fog/authorization withdrawal.
- Army matching/nonmatching, last-stack, and source/destination validation.
- Artifact locked-slot, full-inventory, same-slot, identical/no-op, and scroll-spell identity cases.
- Visual payload bounds, revision parity, malformed input, and fallback.

## Build, device, validation, and release record

Populate repository-specific commands and paths only after discovery.

### Slice 1 automated validation — 2026-09-13

- Source baseline: `819259d97f1de9262b97811ccb081346c20ffef2`; planning checkpoint: `2c936cb37`.
- Pinned repository submodules initialized successfully.
- CMake preset JSON parsing and source whitespace checks pass.
- Conan ARM64 dependency resolution succeeds with VCMI's `dependencies-android-arm64-v8a` cache and NDK r29.
- The Thor CMake preset reaches generation on Windows. Full generation is blocked because VCMI's official Android dependency bundle carries Linux-host Qt tools (`moc`/later `androiddeployqt`), which cannot execute on Windows.
- The new Android display classes compile against Android API 35.
- The modified `VcmiSDLActivity` and new display classes compile against the real SDL 3.4.14 Android library.
- The real Android Gradle module compiles the Thor Java/resources and passes all five `ThorDisplaySelectorTest` tests in a generated validation copy.
- Generated Thor debug identity verified: application ID `is.xyz.vcmi.thor.debug`, `BuildConfig.AYN_THOR_BUILD=true`, label `VCMI Thor debug`, and provider authorities follow the Thor application ID.
- Standard debug generation verified: application ID `is.xyz.vcmi.debug` and `BuildConfig.AYN_THOR_BUILD=false`.
- Linux/JDK 17 CI built the complete ARM64 candidate; GitHub Actions run `34763508118` produced the verified artifact.
- The APK was installed and explicitly launched on the connected AYN Thor. The user confirmed the lower inert deck, independent upper-screen input, panel toggle, and pause/resume behavior.
- The validated implementation was committed as `ed8e57130` and pushed to `origin/ayn-thor-dual-screen`.

### Slice 2 automated validation — 2026-09-13

- Source checkpoint: `ed8e57130`; implementation commit: `9300bcc59`.
- The native context-store source passed an Android NDK C++20 syntax check. Android resources and modified Java sources compiled without source errors.
- The Windows Gradle task could not close an Android dependency-cache JAR when run under Android Studio's JDK 25; this is a host/JDK limitation, not a source failure. Linux/JDK 17 CI is the authoritative package gate.
- Linux/JDK 17 CI run `34774388683` built and tested the ARM64 APK. The downloaded ZIP digest was `bf26453182d82cf5ef6d9f7ad7a4c454e98f28f0ace9182dd311caa661d25f83`; the embedded APK SHA-256 was `683c8213256d0e18277569dfa1f2972827654b9cdbd6b15b2681ccf27d3d672c`.
- The verified APK was installed over the existing Thor package with data preserved. The user confirmed `Main menu / Choose a game mode` on the lower deck; ADB logs recorded `MAIN_MENU` revisions 1 through 3 and no fatal exception.
- The implementation was pushed to `origin/ayn-thor-dual-screen`.

### Slice 3 automated and hardware validation — 2026-09-14

- Source checkpoint: `43dd715ae`; temporary CI candidate: `c57f34123`.
- The native Thor context module passed an ARM64 Android NDK C++20 syntax check. The Android source and resources compiled under JDK 17; local cache cleanup continued to show the known Windows JAR-close limitation after successful compilation.
- Linux/JDK 17 CI run `34777738191` passed focused native context tests, complete ARM64 packaging, package identity and checksum verification, artifact upload, and Android unit tests.
- The GitHub artifact ZIP SHA-256 was `b74383997da509410629d74b300cc72a8fd3a86ef376ac616ba059c92e9665fc`; the embedded and locally verified APK SHA-256 was `6f18f1c54b408eeb6b1b10a64eb5afd1ec189135d055cdbb3eb48056469632b9`.
- The verified APK was installed over `is.xyz.vcmi.thor` with data preserved and launched on an AYN Thor. Automated checks confirmed both displays were on, the presentation-capable 1080 x 1240 lower display was available, the process was running, `MAIN_MENU` revisions were monotonic, and no fatal exception was present.
- The user confirmed the entire focused manual checklist passed: root/New Game/Back context changes, unsupported-tab fallback, panel-toggle and pause/resume recovery, lower-screen inertness, and upper-screen touch/controller operation.

### Slice 4 automated and hardware validation — 2026-09-14/15

- Source checkpoint: `1777ae44150ec9097ad41f032b6f6812b271b966`; temporary CI candidate: `e6a0f682d`; promoted implementation: `17fbdfbb9`.
- Linux/JDK 17 CI run `34885114429` passed focused native context tests, complete ARM64 packaging, package identity and checksum verification, Android unit tests, artifact upload, Markdown validation, and the Slice 4 Android lint delta.
- Full-project Android lint still reports the established project-wide baseline (including English-only `MissingTranslation` diagnostics and an untouched `NewApi` finding). The candidate retained the full report but failed only on a new non-translation error in a Slice 4 Android file; this keeps the gate meaningful without treating the baseline as a Slice 4 regression.
- The GitHub artifact ZIP SHA-256 was `ecda46f91b5cdc93e67eed229cc74dda8b80b09a09b7dd2ec52bfaae3748e103`; the embedded and locally verified APK SHA-256 was `23fdc4b88421a623a5ebabb27c483d5babdb1a13a473f3d303e92b97e7cbe03c`. The independently inspected package ID was `is.xyz.vcmi.thor`.
- The artifact was installed with data preserved and launched on an AYN Thor at a current wireless-debugging endpoint. ADB confirmed the `is.xyz.vcmi.thor` package path and a running process.
- The user confirmed the full focused manual checklist passed: Main Menu, New Game, and Load Game cards; Back/root restoration; Campaign/Credits fallback; lower-panel toggle; pause/resume; lower-deck inertness; and unaffected upper touch and physical controls.

### Slice 6 automated and hardware validation — 2026-09-16/17

- Source baseline: `b994b7d9a`; product candidate: `299e18223`; final implementation: the promoted Slice 6 commit on `ayn-thor-dual-screen`.
- Temporary validation branch `ci/thor-slice6-validation` used Ubuntu 24.04, Temurin JDK 17, recursive submodules, an isolated Gradle home, VCMI's official Conan ARM64 dependency bundle, and the `android-thor-release` preset.
- Qt 5.15.19 adds the removed `android.bundle.enableUncompressedNativeLibs` property immediately before invoking Gradle. The successful candidate injected a cleanup into the copied Gradle wrapper and cleared inherited `--aab` deployment options so CI produced an installable APK. Earlier attempts to clean global/generated properties before `androiddeployqt`, isolate Gradle alone, patch a nonexistent template, or use `--no-build` did not solve the generation timing problem.
- Linux/JDK 17 CI run `35142531415` passed focused native context tests, the complete ARM64 APK build, focused Android tests, package-ID verification, checksum generation, and artifact upload.
- The downloaded APK SHA-256 was independently verified as `672db851f231f312614096169a8c9cfa00858fd6b3db63f06a49574777dd4388`; package ID was `is.xyz.vcmi.thor`, version `1.8.0` (`1800`).
- The verified APK was installed with data preserved and launched on an AYN Thor. The user explicitly confirmed all focused Credits, restoration, transition, lifecycle, inertness, touchscreen, and controller checks passed on 2026-09-17.
- The exact reusable GitHub build recipe and failure-avoidance notes are maintained in `docs/AYN_THOR_BUILD_PLAYBOOK.md`.

For each approved slice:

1. Record source revision and pre-existing changes.
2. Run focused native tests plus the complete relevant Android build/lint/static and identifier-contract gates.
3. Use a short temporary Windows drive mapping when NDK path length requires it.
4. Record the exact APK path and SHA-256.
5. Install that APK on the connected Thor and explicitly launch the verified package/activity.
6. Perform only brief connection, process, display, log, and state checks.
7. Give the user a short checklist containing the new behavior and a few high-risk earlier regressions.
8. Record the user's hardware result. Do not mark validation passed before the user reports it.
9. Commit and push only after validation and approval under the verified fork remote policy.

For releases, first approve a cleanup-only scope, reconcile documentation/history, run all gates, rebuild and hash the APK, install and smoke-test that exact artifact, record user validation, then commit documentation, create an annotated tag at the validated checkpoint, publish a prerelease with a versioned APK name, verify remote tag/release/asset metadata and digest, and confirm hosted CI.

## Deferred ideas

- Multi-slot army redistribution.
- Continuous or high-volume rendered-frame transport before performance evidence exists.
- Any lower-screen rule engine or hidden-information inference.
- Additional layouts, handedness modes, battery-saving modes, advanced haptic customization, and extra long-press actions unless separately proposed and approved.

## Approved Slice 8: read-only primary in-game context family

Status: hardware validated
Approved by the user on 2026-09-17.

### User-visible behavior

- The lower, inert deck shows `Adventure map / Explore the world` while the Adventure Map is active.
- It shows `Hero / Review hero details` while `CHeroWindow` is the active top gameplay screen, and `Town / Review town details` while `CCastleInterface` is active.
- Opening an unsupported child or major context clears the deck to the safe `UNKNOWN` fallback. Closing that child restores the exact active approved parent card through normal VCMI activation.

### Implementation boundary and responsibilities

- Add only `ADVENTURE_MAP`, `HERO_WINDOW`, and `TOWN_WINDOW` to the stable native/Android context contract, with a pure fail-closed native in-game mapping.
- Publish only from the existing Adventure Map, Hero Window, and Castle Interface lifecycle owners, using the shared `ThorContextStore::publishNext()` revision stream and existing Thor compile guard.
- Android owns bounded title/status resources and rendering for recognized IDs. Native sends no known-context UI text and no hero, town, gameplay, or visual data.
- No controls, touch behavior, Java-to-native calls, SDL input synthesis, gameplay/network changes, or context classification beyond these three owners are included.

### Automated acceptance tests

- Test each in-game enum mapping, unknown/invalid fail-closed behavior, unchanged main-menu/lobby mappings, sequential cross-family revisions, and a fresh revision for `UNKNOWN`.
- Extend Android identifier parity tests and verify all three recognized IDs render bounded local resources.
- Run focused native/Android checks before the Linux/JDK 17 ARM64 candidate workflow, then verify package ID, APK checksum, and uploaded artifact.

### Hardware checklist

- Verify Adventure, Hero, and Town cards; parent restoration after Back; and safe fallback for reachable unsupported children and battle.
- Verify rapid transitions, lower-panel recreation, background/resume, lower-panel inertness, and unchanged upper touch/controller behavior.
- Recheck the established main-menu, New Game, and lobby cards.

### CI and hardware validation

- Candidate CI: GitHub Actions run `35197790453` at commit `4fca0af47d6783203b2aa99cb40406282991446c` passed 11 focused native tests, Android identifier parity tests, complete ARM64 APK build, package verification, checksum verification, and artifact upload.
- Candidate artifact: `thor-slice8-candidate-arm64.apk` (artifact ID `10487421830`), package `is.xyz.vcmi.thor`, version `1.8.0` (version code `1800`), APK SHA-256 `f3e43413d87437a4c5722e38ca58072afd2bbecf8680018ae4e453e4a8e6e46f`.
- The candidate APK was installed over the existing Thor application with data preserved. On 2026-09-17, the user reported every Slice 8 hardware check passed, including the three cards, parent restoration, safe fallback, rapid transitions, lower-panel lifecycle, resume, lower-panel inertness, and unchanged upper touchscreen/controller behavior.

### Regression risks

- Incorrect lifecycle ordering could retain a stale approved parent card instead of publishing a newer `UNKNOWN` record.
- Duplicate publication must still use the shared monotonic store so Android can reject stale callbacks safely.
- Android rendering/resources must remain local, inert, package-compatible, and isolated from standard builds.

## Next action

Slices 1 through 9 are hardware-validated. Propose and obtain approval for any future read-only context before implementation; native commands, coordinate injection, and mutable gameplay state remain separately scoped.

## Approved Slice 9: read-only Hero Meeting and Battle lifecycle contexts

Status: `hardware validated`

### User-visible behavior

- The inert lower deck shows `Hero meeting / Review hero exchange` while `CExchangeWindow` is active, and restores it when an approved nested Hero window closes.
- It shows `Battle / Combat in progress` during normal battle, `Battle tactics / Arrange your forces` during the real tactics phase, and `Battle result / Review the outcome` only while `BattleResultWindow` is active.
- Unsupported children clear to `UNKNOWN`; normal parent activation restores approved contexts. Battle Result dismissal preserves VCMI's normal result-close, BattleWindow reactivation/deactivation, and Adventure Map restoration lifecycle.

### Implementation boundary and responsibilities

- Add only `HERO_MEETING`, `BATTLE`, `BATTLE_TACTICS`, and `BATTLE_RESULT` to the stable native/Android contract.
- Publish from the normal CExchangeWindow, BattleWindow, and BattleResultWindow lifecycle owners using the existing monotonic store and Thor compile boundary.
- Android renders only bounded local title/status resources. No native gameplay data, controls, input paths, or mutable requests are added.

### Automated acceptance tests

- Extend native mapping and revision tests for all four IDs, invalid-value fallback, and the tactics-to-battle revision transition.
- Extend Android identifier parity coverage and local-resource rendering branches for all four IDs.

### Hardware checklist

- Verify Hero Meeting, nested Hero restoration, and Adventure Map restoration.
- Verify normal battle, tactics-to-battle transition, result visibility, unsupported-child fallback, and result dismissal back to Adventure Map.
- Recheck rapid lifecycle transitions, panel recreation, resume, lower-panel inertness, and unchanged upper touchscreen/controller behavior.

### CI and hardware validation

- Candidate CI: GitHub Actions run `35227471813` at commit `16d3c44ed432839369ce2d394b8c257b41a697a8` passed focused native and Android Thor tests, the ARM64 APK build, package verification, checksum verification, validation receipt creation, and artifact upload.
- Candidate artifact: `thor-candidate-arm64-35227471813`, package `is.xyz.vcmi.thor`, APK SHA-256 `9514f37cd807d961f50aafa07a38ac67862b306bfff147ad43d97ad804af6064`.
- The user reported all Slice 9 AYN Thor hardware checks passed, including Hero Meeting and Battle lifecycle cards, Battle Result dismissal back to Adventure Map, lower-panel inertness, and upper-screen behavior.

### Lifecycle regression lesson

- An initial Battle Result optimization removed the result and inactive BattleWindow together. It skipped BattleWindow's normal activation/deactivation lifecycle and left the last battle frame above Adventure Map on physical hardware.
- The correction restored the engine's normal close sequence. Future Thor work must treat existing window activation/deactivation as functional behavior, not merely context publication: preserve the exact stack transition unless a full equivalent cleanup path is proven and hardware-validated.
- Battle-result regression coverage must explicitly dismiss the result and verify that Adventure Map is both visible and interactive, with no residual battle frame, before promotion.

## Approved Slice 5: read-only Campaign submenu context

Status: `hardware validated`

Approved by the user on 2026-09-15. This slice adds the `MAIN_MENU_CAMPAIGN` read-only context for the exact existing `campaign` main-menu tab.

### 1. Approved user-visible behavior

- Opening Campaign shows the localized `Campaign / Choose a campaign` card on the lower deck.
- Back from Campaign returns to the existing `New game / Choose single-player, multiplayer, campaign, or tutorial` card.
- Main Menu, New Game, and Load Game remain unchanged.
- Credits, empty, case-variant, malformed, unknown, and mod-added tab names fail closed to `UNKNOWN`; the lower deck remains inert.

### 2. Implementation boundary and responsibilities

- Use the existing `CMenuScreen` publication path and map only the exact, case-sensitive tab name `campaign` to `MAIN_MENU_CAMPAIGN`.
- Native code owns the stable identifier and exact mapping; Android owns parity and localized title/status rendering.
- No buttons, touch regions, commands, coordinate input, gameplay or campaign data, mutable behavior, artwork, networking, or broad context architecture changes.

### 3. Focused acceptance tests

- Native mapping covers `main`, `new`, `load`, `campaign`, credits, empty, case-variant, and malformed names; context-store tests remain intact.
- Java/native identifier parity includes `MAIN_MENU_CAMPAIGN`; Android sources/resources and unit tests pass; stale-revision handling is unchanged.
- Source validity, focused Thor checks, Markdown validation, relevant lint/static checks, Thor-only/standard-build isolation, and the established Linux/JDK 17 build route are checked where available.

### 4. Focused Thor hardware checklist

1. Main Menu, New Game, Campaign, and Load Game show their expected localized cards.
2. Back from Campaign restores New Game.
3. Rapid `main → new → campaign → new → load → main` transitions do not leave stale text; Credits shows the safe fallback.
4. Toggling the lower panel and pausing/resuming on Campaign restores exactly one deck with the latest context.
5. Lower touch remains inert; upper touchscreen and physical controls remain unaffected.

### 5. Important regression risks

- An imprecise tab-name match could recognize malformed or mod-added entries instead of failing closed.
- Rapid transitions or lifecycle recreation could retain stale Campaign text or duplicate the presentation.
- Rendering or resource changes could affect lower-panel inertness, focus, or standard builds.

### Hardware validation

The Linux/JDK 17 candidate build passed CI run `35003735325`. The verified APK SHA-256 was `fc9a520ad032e03ac545328e4e7f6855a753ef9545f2c151116080a1e9626696`, package ID `is.xyz.vcmi.thor`, and it was installed and launched successfully on an AYN Thor. The user confirmed all focused hardware checks passed: Campaign card, Back-to-New restoration, rapid transitions, Credits fallback, panel toggle, pause/resume, lower-touch inertness, and upper touchscreen/controller behavior.

## Approved Slice 4: read-only Load Game submenu context

Status: `hardware validated`

Approved by the user on 2026-09-14. This slice adds the `MAIN_MENU_LOAD_GAME` read-only context within the existing main-menu owner.

### 1. Approved user-visible behavior

- Entering Load Game shows `Load game / Choose single-player, multiplayer, campaign, or tutorial` on the lower deck.
- Returning to the root menu restores `Main menu / Choose a game mode`.
- New Game continues to show its existing context unchanged.
- Campaign, Credits, malformed names, mod-added tabs, and all other unsupported tabs fail closed to `UNKNOWN`; the lower deck remains inert.

### 2. Implementation boundary and responsibilities

- Use the existing `CMenuScreen` publication path and map only the exact configured tab name `load` to `MAIN_MENU_LOAD_GAME`.
- Native code owns the new stable identifier, exact mapping, and existing monotonic context publication.
- Android owns the parity identifier and localized bounded title/status rendering; it does not render arbitrary native text for this known context.
- No buttons, touch regions, action identifiers, Java-to-native commands, coordinate input, game or save data, mutable behavior, artwork, or server/network changes are permitted.

### 3. Focused acceptance tests

- Native mapping covers `main`, `new`, `load`, campaign, credits, empty, and malformed names; existing context-store tests continue to pass.
- Java/native identifier parity includes `MAIN_MENU_LOAD_GAME`; Android sources and resources compile, and stale-revision handling is unchanged.
- Thor-only and standard-build isolation, focused native tests, Android checks, Markdown validation, packaging verification, and Linux/JDK 17 ARM64 CI pass.

### 4. Focused Thor hardware checklist

1. Main Menu retains the existing Main Menu card, New Game retains its existing card, and Load Game shows the new Load Game card.
2. Back from Load Game restores Main Menu; Campaign and Credits show the safe fallback rather than retaining Load Game.
3. Toggling the lower panel and pausing/resuming while on Load Game restores exactly one deck with the latest context.
4. Lower touch remains inert; upper touch and physical controls remain unaffected.

### 5. Important regression risks

- An imprecise tab-name match could recognize malformed or mod-added menu entries instead of failing closed.
- Rapid tab changes could expose a stale card if revision filtering or presentation restoration changes.
- A context-rendering addition could inadvertently affect focus, lower-panel inertness, lifecycle behavior, or standard builds.

### Hardware validation

The user confirmed every focused hardware check passed on 2026-09-15. Slice 4 was promoted as commit `17fbdfbb9` on `ayn-thor-dual-screen`.

## Approved Slice 6: read-only Credits main-menu context

Status: `hardware validated`

Approved by the user on 2026-09-15. This slice adds the `MAIN_MENU_CREDITS` read-only context for the exact existing `credits` main-menu tab.

### 1. Approved user-visible behavior

- Opening Credits keeps the normal Credits screen on the upper display and shows the localized `Credits / View contributors and acknowledgements` card on the lower deck.
- Returning from Credits to the main menu restores the existing `Main menu / Choose a game mode` card through the revisioned publication path.
- Main Menu, New Game, Load Game, and Campaign remain unchanged; the lower deck remains inert.

### 2. Implementation boundary and responsibilities

- Use the existing `CMenuScreen` publication path and map only the exact, case-sensitive tab name `credits` to `MAIN_MENU_CREDITS`.
- Native code owns the stable identifier and exact mapping; Android owns parity and localized title/status rendering for the known context.
- No buttons, touch regions, commands, JNI mutation entry points, coordinate input, gameplay or credits data, mutable behavior, artwork, networking, or context architecture changes.

### 3. Focused acceptance tests

- Native mapping covers `main`, `new`, `load`, `campaign`, exact `credits`, case variants, empty, and malformed names; context-store revision tests remain intact.
- Java/native identifier parity includes `MAIN_MENU_CREDITS`; Android sources/resources and unit tests pass, and the known context uses local resources rather than arbitrary native text.
- Source validity, focused Thor checks, Markdown validation, relevant lint/static checks, Thor-only/standard-build isolation, and the established Linux/JDK 17 ARM64 build route are checked where available.

### 4. Focused Thor hardware checklist

1. Start on Main Menu: lower deck shows `Main menu / Choose a game mode`.
2. Open Credits: upper display shows normal Credits; lower deck shows `Credits / View contributors and acknowledgements`.
3. Exit Credits: lower deck immediately restores `Main menu / Choose a game mode`.
4. Run `Main → New → Campaign → New → Main → Load → Main → Credits → Main`; every card follows the upper context without stale text.
5. Toggle the lower panel once and background/resume once while Credits is open; exactly one correct card returns each time.
6. Touching the lower display produces no game action and does not steal upper focus; upper touchscreen and physical controller remain functional.

### 5. Important regression risks

- A loosened tab-name comparison could recognize malformed or mod-added entries instead of failing closed.
- Rapid transitions or lifecycle recreation could retain stale Credits text or duplicate the presentation.
- Resource or rendering changes could affect lower-panel inertness, focus, or standard builds.

### Hardware validation

Linux/JDK 17 CI run `35142531415` passed the focused native context tests, complete ARM64 APK build, focused Android tests, package-ID verification, checksum generation, and artifact upload. The downloaded APK SHA-256 was independently verified as `672db851f231f312614096169a8c9cfa00858fd6b3db63f06a49574777dd4388`, with package ID `is.xyz.vcmi.thor` and version `1.8.0` (`1800`). The verified APK was installed with data preserved and launched on an AYN Thor; ADB confirmed the installed package and running process. On 2026-09-17, the user explicitly reported that all focused hardware checks passed, including the Credits card, Main Menu restoration, regression route, panel toggle, pause/resume, lower-deck inertness, and unaffected upper touchscreen/controller input.

## Approved Slice 7: read-only pre-game Lobby/Setup context family

Status: `hardware validated`

Approved by the user on 2026-09-17. This slice extends the bounded Thor context bridge to the existing `CLobbyScreen` pre-game setup owner.

### 1. Approved user-visible behavior

- New Game setup shows the following local lower-deck cards: scenario selection (`New game / Select a scenario`), options, random-map options, turn options, extra options, battle-only mode, and the no-active-tab fallback (`New game / Choose a setup panel`).
- Load Game setup shows bounded cards for save selection (`Load game / Select a saved game`), options, turn options, extra options, and the no-active-tab fallback (`Load game / Choose a setup panel`). Unsupported Load Game random-map and battle-only combinations fail closed.
- Campaign-list selection shows `Campaign / Select a campaign`, with no campaign name, description, map, player, save, or other game data exposed.
- Leaving a lobby or temporarily opening an unsupported child clears the card safely; normal parent activation republishes its own existing card. The lower deck remains inert.

### 2. Implementation boundary and responsibilities

- A pure `lib/thor` mapping converts bounded lobby mode and tab enums into stable `LOBBY_*` identifiers. It rejects unknown modes/tabs and invalid combinations.
- `CLobbyScreen` maps its authoritative `screenType` and exact existing tab pointer identity only after `CSelectionBase::toggleTab()` has changed `curTab`; activation republishes the current state and deactivation clears it.
- The shared Thor context store now assigns revisions for all publishers, preventing a lobby update from being rejected after a main-menu revision.
- Android adds exact identifier parity and renders every recognised lobby card from local default resources through the existing inert presentation.
- No lower-screen buttons, Java-to-native API, coordinate/event injection, game/server state mutation, networking, map/player/save transfer, second SDL renderer, or proprietary assets are added.

### 3. Automated acceptance tests

- Native pure-mapping tests cover every approved New Game, Load Game, and campaign-list combination; no-tab fallbacks; unknown mode/tab; invalid campaign tabs; and unsupported Load Game Random Map/Battle-only combinations. Existing main-menu and context-store tests remain covered.
- Java/native parity tests cover all `LOBBY_*` identifiers. The known Android contexts select local resources, while unknown records retain the existing safe fallback behavior.
- Focused host-native tests, Android unit/resource checks, source validation, Markdown validation, and one Linux/JDK 17 ARM64 candidate build passed before hardware testing.

### 4. Focused Thor hardware checklist

1. From Main Menu, verify `Main menu / Choose a game mode`; then open New Game and confirm its existing card.
2. Enter Single Player/New Game scenario selection and confirm `New game / Select a scenario`.
3. Open Options, Random Map, Turn Options, Extra Options, and Battle-only Mode where available; each card must match its approved title/status. Return to scenario selection and confirm immediate restoration.
4. Back to New Game and confirm its existing parent card is restored. Enter Load Game → Single Player and verify save selection, Options, Turn Options, and Extra Options each follow the upper UI and restore correctly.
5. Enter the campaign-list setup path and verify `Campaign / Select a campaign`; back out through the hierarchy without a stale Lobby card.
6. Rapidly exercise `Main → New → Single → Options → Scenario → Random → Scenario → Back → Load → Single → Options → Back → Main`; no deck card may lag or remain stale.
7. While a Lobby card is visible, toggle the lower panel, background/resume once, and touch the lower display. Exactly one latest card must return; lower touch remains inert and upper touchscreen/controller input remains normal.
8. Recheck previously validated Main Menu, New Game, Load Game, Campaign, and Credits cards.

### 5. Important regression risks

- A pointer comparison that accepts an absent or future tab could classify unsupported/mod-added UI instead of failing closed.
- Activation/deactivation and rapid server-driven tab updates could leave stale text if the shared revision stream or parent restoration regresses.
- Android resource/rendering additions must not affect lower-panel focus/inertness, package identity, standard builds, or established cards.

### Hardware validation

Linux/JDK 17 CI run `35193073049` passed the focused native tests (8 tests), complete ARM64 APK build, focused Android unit test, package verification, checksum generation, and artifact upload. The candidate commit was `9a5d3a8ed75c9b4aa3c9ec0c07502b3c1eefe5fa`; package ID is `is.xyz.vcmi.thor`, version `1.8.0` (`1800`), APK SHA-256 is `3f2d573b31f87a82342486ffd303e7a359ec8b25061a26d4eda65016c8c07f56`, and the uploaded artifact ZIP digest is `d7fd7c4e5fec2c9ec800e1e5a473703e0e887bcb94dfc4c2d99410e0a609e60f`. The APK was installed with data preserved and launched on an AYN Thor on 2026-09-17; ADB confirmed model `AYN Thor`, package installation, and a running process. On 2026-09-17, the user explicitly confirmed every Slice 7 hardware check passed, including all New Game, Load Game, Campaign, transition, lifecycle, inertness, and upper-input checks.

## Approved Slice 10: read-only Adventure utility/modal context family

Status: `hardware validated`

### Scope and behaviour

- Add only the stable identifiers `KINGDOM_OVERVIEW`, `QUEST_LOG`, `SCENARIO_EVENT_JOURNAL`, `PUZZLE_MAP`, and `SAVE_GAME` to the shared native/Android contract.
- The lower deck renders bounded Android-localized cards: `Kingdom overview / Review your kingdom`, `Quest log / Review active quests`, `Scenario journal / Review scenario events`, `Puzzle map / Review the Grail map`, and `Save game / Choose a save slot`.
- Native publishes these IDs only from `CKingdomInterface`, `CQuestLog`, `ScenarioEventJournal`, `CPuzzleWindow`, and `CSavingScreen`. Each owner clears to `UNKNOWN` when genuinely deactivated; the normal immediate parent then republishes itself on its existing activation path.
- `CPlayerInterface::showQuestLog()` retains its existing decision: a missing displayable quest opens `ScenarioEventJournal`, which has its own distinct Thor context.
- Save Game remains the existing functional upper-screen window. Its selection, filename, overwrite, callback, pause, and close behavior are untouched.

### Boundaries and tests

- Thor publication is observational only. No WindowHandler stack shortcut, lifecycle optimization, lower-deck input, action request, gameplay data, journal/quest content, puzzle image, save-slot data, or state mutation is introduced.
- Native tests cover all approved in-game mappings, unknown/invalid fallback, unchanged earlier mappings, and increasing revisions across Adventure Map utility transitions including fresh `UNKNOWN` publications.
- Android parity tests include all five identifiers and verify their title/status resource IDs. The existing presentation remains non-focusable, non-clickable, and has no new listener/action path.

### Risks

- The main regression risk is bypassing an established parent restoration lifecycle. These observers deliberately call the appropriate base activation/deactivation before publishing and never manually substitute `ADVENTURE_MAP`.
- Nested standard dialogs can temporarily clear a utility card; the owner or its immediate native parent remains responsible for restoration through the normal stack lifecycle.

### CI candidate

- Candidate branch: `ci/thor-slice10-validation`.
- Exact product commit: `8bd603d6684d107e6f72e48fbb9e04eaa4b38293` (`Add Thor adventure utility contexts`).
- Permanent workflow: GitHub Actions run `35247079222` ([run details](https://github.com/CapnChaosDK/vcmi_thor/actions/runs/35247079222)); preflight and ARM64 candidate both passed.
- Receipt: package `is.xyz.vcmi.thor`, `android-thor-release`, `arm64-v8a`, focused native and Android Thor tests passed, and package verification passed.
- Artifact: `thor-candidate-arm64-35247079222`, containing `thor-candidate-arm64.apk`, `thor-candidate-arm64.apk.sha256`, `thor-validation-receipt.json`, and `thor-ccache-stats.txt`.
- APK SHA-256: `01f577694631b0c6739938c5a5c83382736ea7451915ecc86e4b7dfd5770b05d` (downloaded artifact independently verified locally).

### Focused hardware checklist

1. On Adventure Map, open and close Kingdom Overview; verify its card and a fully visible, interactive Adventure Map afterward.
2. Open Quest Log where quests exist; verify its card and Adventure Map restoration. With no displayable quests, verify the distinct Scenario Journal card and correct parent restoration.
3. Open and close Puzzle Map; verify its card, no residual puzzle graphics, and Adventure Map interaction.
4. Open Save Game; verify its card, cancel, then perform a normal save if practical. Confirm existing save behavior and Adventure Map restoration.
5. Rapidly open/close at least two new windows, toggle the lower panel during one, and background/resume during one. Exactly one latest correct deck card must return.
6. Confirm lower touch remains inert while upper touchscreen and physical controller remain functional. Smoke-test Adventure Map, Hero, Town, a battle, and Battle Result restoration.

### Final validation

- On 2026-09-17, the user reported that every focused manual AYN Thor check passed after installing the CI-verified candidate APK. This includes the five new cards, normal parent restoration, rapid transitions, panel and app lifecycle behavior, lower-panel inertness, upper touchscreen/controller input, and the earlier Adventure Map, Hero, Town, Battle, and Battle Result smoke checks.
- The exact hardware-tested product tree is commit `8bd603d6684d107e6f72e48fbb9e04eaa4b38293`. The separate validation-record commits contain documentation only.

## Approved Slice 11: Adventure Map semantic utility actions

Status: `hardware validated`

### Scope and command contract

- The Adventure Map deck exposes four stable semantic commands only: `OPEN_KINGDOM_OVERVIEW = 1`, `OPEN_QUEST_LOG = 2`, `OPEN_PUZZLE_MAP = 3`, and `OPEN_SAVE_GAME = 4`; `NONE = 0` is never executable.
- Android renders a stable 2x2 command deck with local labels, enabled/disabled treatment, a revision-bound action mask, and accessible deck text. It submits only a snapshot revision plus one semantic ID; it never sends screen coordinates or synthetic input.
- The Java/JNI boundary accepts only a known ID and enqueues a small request. A fixed 16-entry, mutex-protected native queue drops the newest request on overflow and is cleared when the lower presentation pauses.
- `GameEngine::updateFrame()` consumes the queue on `MainGUI`. It rechecks the stable ID, current revision, `ADVENTURE_MAP` context, active Adventure Map owner, allow-list, published enabled mask, and the existing shortcut predicate immediately before execution. Rejected input is a debug-logged no-op; at most one accepted command runs per frame.

### Existing VCMI semantics reused

- `AdventureMapShortcuts` remains the single source of truth for availability and invokes its existing Kingdom Overview, Quest Log/journal fallback, Puzzle Map, and Save Game callbacks. No modal is created by Thor-specific code.
- Normal upper-screen window transitions remain authoritative. Slice 10's concrete utility owners publish their existing contexts after the normal action runs; returning through normal lifecycle restoration returns the deck to Adventure Map.
- Availability is stored with each `ThorContextRecord`. Identical semantic state returns the existing record without a revision, while an availability change creates one new revision and bounded Android update.

### Deliberately out of scope

- Movement, end turn, hero/town selection, transfers, recruitment, construction, market, battle, Main Menu, Load, Quit, direct saving, coordinate injection, synthetic touch, or a second renderer/camera.

### Automated validation and candidate hand-off

- Native tests cover ID mapping, allow-list, fixed-capacity FIFO/overflow, concurrent submissions, and revision-bound action-state no-churn. Android tests cover the explicit Java IDs and masks. The permanent candidate preflight compiles and runs both native Thor test sets; the ARM64 candidate runs the Android Thor unit test.
- Implementation commit and candidate branch SHA: `873faeaedc153f400d39c2677cfe495bf3d07bd9` (`thor: add adventure map semantic utility actions`); the candidate points directly at the product commit, with no validation-only product commit in between.
- Candidate branch: `ci/thor-slice11-validation`.
- GitHub Actions run `35256657759` succeeded: the preflight and full ARM64 candidate build passed. Artifact: `thor-candidate-arm64-35256657759`; artifact ZIP SHA-256: `354302b412dab6d99a0869dd67f02589007006283b2e8e0d73c8ca0d453a31c6`; APK SHA-256: `30adbcff7c4a8082e373abf6f7038378f00e40c012ab8829b8115189a060f4dc`.
- On 2026-09-17, the verified artifact was installed on a physical AYN Thor with existing application data preserved. The user reported that all focused Slice 11 hardware checks passed: the four Adventure Map commands, normal upper/lower context round trips, rapid/stale input safety, lifecycle behavior, and Hero/Town/Battle/Main Menu regression checks. No duplicate modal, delayed replay, stale lower context, crash, or upper-input regression was observed.

### Hardware checklist

1. Install the verified CI artifact; start or load an Adventure Map and confirm the four lower commands and normal upper input.
2. Exercise Kingdom, Quest Log (including normal Scenario Journal fallback), Puzzle Map, and Save Game. For each, confirm its normal upper modal, matching existing lower context, normal close/cancel, and Adventure Map restoration.
3. Test unavailable commands where practical, double taps, rapid mixed taps, taps during transitions, repeated open/close, Home/resume, lower-display detach/reattach, and normal Hero/Town/Battle/Main Menu regression paths.
4. Confirm no duplicate modal, delayed replay, stale lower context, crash, or loss of upper touchscreen/controller behavior.

## Approved Slice 12: Context-aware Adventure Map gameplay controls

Status: `hardware validated`

### Scope and command contract

- The existing Slice 11 IDs remain unchanged: `OPEN_KINGDOM_OVERVIEW = 1`, `OPEN_QUEST_LOG = 2`, `OPEN_PUZZLE_MAP = 3`, and `OPEN_SAVE_GAME = 4`. Slice 12 appends `NEXT_HERO = 5`, `MOVE_HERO = 6`, `TOGGLE_HERO_SLEEP = 7`, and `END_TURN = 8`; `NONE = 0` remains non-executable.
- The Adventure Map allow-list contains exactly these eight semantic commands. Android submits only a rendered revision and one ID through the existing fixed 16-entry queue; it does not send a hero, path, tile, pointer event, or confirmation response.
- Native availability is sourced only from `AdventureMapShortcuts`: `optionHasNextHero()`, `optionHeroCanMove()`, `optionHeroSelected()`, and `optionCanEndTurn()`. The sleep toggle uses the native `ADVENTURE_TOGGLE_SLEEP` action and reports the existing `optionHeroSleeping()` state as a checked/action-state bit so the deck says `Sleep Hero` or `Wake Hero` correctly.

### Implementation boundary and lifecycle safety

- `GameEngine::updateFrame()` remains the sole consumer on `MainGUI`. Before dispatch, it validates a known ID, current revision, `ADVENTURE_MAP` context, current active owner, allow-list membership, native published enablement, then re-runs the normal shortcut predicate through `executeThorAction()`.
- Each action maps to its normal VCMI shortcut: `ADVENTURE_NEXT_HERO`, `ADVENTURE_MOVE_HERO`, `ADVENTURE_TOGGLE_SLEEP`, or `ADVENTURE_END_TURN`. Thus selection, path movement, sleep state, end-turn warnings/confirmation, autosave, and server interaction retain their upstream behavior.
- An accepted command immediately refreshes the bounded state, consumes its action epoch, and clears queued taps. The consumption revision invalidates a rapid second tap even when movement has not yet produced a visible state change; normal context deactivation and Android pause continue to invalidate queued actions. No native owner is retained by Android or the queue.
- `ThorContextRecord` now includes an active-action mask alongside enablement, selected-hero identity, and a consumed-action epoch. Unchanged published state does not churn revisions; availability, sleep state, selected hero, and one-time accepted-command consumption each create one new revision and lower-panel update.

### Lower-screen presentation

- The existing Adventure Map deck is retained and expanded to eight touch targets in two sections: Gameplay (Next Hero, Move Hero, Sleep/Wake Hero, End Turn) and Utilities (Kingdom, Quest Log, Puzzle Map, Save Game). Buttons retain enabled/disabled treatment, local labels, and accessible deck text without taking focus from the upper display.

### Automated validation

- Focused native tests cover stable ID values, all eight allow-listed actions, gameplay action masks, revision/context/availability rejection, active sleep-state revision/no-churn behavior, selected-hero and consumed-action stale rejection, and the pre-existing bounded FIFO/overflow/concurrency queue behavior.
- Focused Android tests verify the explicit native-parity IDs and masks for all eight actions. The permanent Thor workflow remains the candidate gate for the native and Android test runs.

### Regression risks and focused hardware checklist

1. Load or start an Adventure Map with multiple heroes and verify Next Hero matches the ordinary shortcut exactly once, including upper-map selection/camera and lower Move/Sleep state refresh.
2. With and without a same-turn path, verify Move Hero enablement and ordinary movement behavior. Rapid/double taps, a changed selection, a modal, and a paused/detached lower display must not start delayed or duplicate movement.
3. Verify Sleep Hero changes to Wake Hero; wake it again and verify ordinary hero selection behavior. Change heroes and confirm the label always reflects the selected hero.
4. Verify End Turn both with and without the ordinary hero-reminder warning. The usual warning/confirmation must remain on the upper display and no lower queued action may run into the next turn.
5. Recheck all four utility commands, normal modal restoration, rapid context changes, Home/resume, lower-panel detach/reattach, upper touchscreen, and physical controller input.

### Candidate hand-off

- Candidate branch: `ci/thor-slice12-validation`, pointing directly at product commit `221a9f9eba3614665bcce8e84c86868f1d252d20`.
- GitHub Actions run `35263114653` passed its focused native tests, ARM64 APK build, focused Android test, package verification, and artifact upload. Artifact: `thor-candidate-arm64-35263114653`; GitHub artifact ZIP SHA-256: `211d1dd9cf0443ab9f4d60d11188626d267ea30c39f9a9217a27a79c4ffb41da`; APK SHA-256: `f18621aaaf1fbded950112a52f5e1f41f44a5b9900d7ec988447f2940c2609cc`.
- The downloaded receipt independently confirms the same product commit, candidate ref, `is.xyz.vcmi.thor` package, `arm64-v8a` ABI, and passed focused native/Android tests. The completed hardware validation is recorded below.

### Final hardware validation

- On 2026-09-17, the user reported that every focused Slice 12 check passed on the AYN Thor after the checksum-verified candidate APK was installed in place. This includes Next Hero selection/camera refresh, valid and invalid Move Hero behavior, Sleep/Wake state changes, End Turn including its normal reminder/confirmation flow, rapid/double-tap safety, all four Slice 11 utility commands, lifecycle/display changes, and upper touchscreen/controller regression checks.
- The hardware-tested product tree is exactly `221a9f9eba3614665bcce8e84c86868f1d252d20`. The later CI and hardware validation commits contain documentation only.

## Slice 13: selected-hero Adventure information card

Status: `hardware validated`

### Scope and behavior

- The Adventure Map header now shows the selected hero's translated display name and current/maximum movement points. With no selected hero it retains the existing generic `Explore the world` status.
- The native Adventure Map owner produces the visibility-safe snapshot from the local player's existing selection. The payload contains no map position, path, army, enemy, fogged, or server-private data.
- Hero information is refreshed through the existing per-frame Thor state publication, shares the action revision, and does not churn revisions while name, movement, selection, action availability, and action state are unchanged.
- Dynamic text is limited to 128 UTF-8 bytes without splitting a code point. Android supplies the local `Movement` label, renders the snapshot in the existing header, and retains the existing eight semantic controls.

### Boundaries, validation, and risks

- This is the first bounded read-only information-card slice from Milestone 4. It deliberately excludes portraits, attributes, armies, resources, quick selection, map navigation, and new commands.
- Native tests cover payload bounds, UTF-8 truncation, movement revision updates, and unchanged-state suppression. The Android resource test covers the new local format string.
- Hardware validation must check long/localized hero names, selection and movement refresh, no-selection fallback, rapid commands, lifecycle/display recreation, all eight existing commands, and unchanged upper-screen input.
- Remaining risks are header clipping on unusually long translated names, movement values changing during animation more often than expected, and device-only timing across presentation recreation. GitHub Actions Android CI is the authoritative compile gate.

### CI and hardware validation

- GitHub Actions run `35504023315` passed the focused preflight and full ARM64 candidate pipeline: Android package build, focused native and Android Thor tests, package verification, checksum generation, and artifact upload.
- Artifact: `thor-candidate-arm64-35504023315`; package `is.xyz.vcmi.thor`; APK SHA-256 `f2af72c9024ce6fb4c8e70c7775d0fde60cd463a97306b31f85b6f9b60675f24`.
- The validation ref `ci/thor-slice13-rebuild-validation` at `caa05489a30d80cb26e89c5faae8b1499986240d` differs from the Slice 13 product tree only by a CI-comment trigger and contains no product-code changes.
- On 2026-09-20, the user reported every focused Slice 13 hardware check passed on an AYN Thor after the verified artifact was installed in place, preserving app data.
- The promoted hardware-tested product tree is `f12865c7fd4dd9208d14c928402702fffcc719bc` (`thor: add selected hero information card`).

## Slice 14: Hero Window information dashboard

Status: `hardware validated`

### Scope and information boundary

- When a normal `CHeroWindow` is the active upper-screen owner, the lower deck renders a read-only dashboard for that exact window's `curHero`: translated name; level and translated class; Attack and Defense; Spell Power and Knowledge; current/max mana; and current/next-level experience.
- The native source of truth is only the `CGHeroInstance * curHero` already displayed by `CHeroWindow`. It reuses the same translated name/class, primary-skill accessors, mana limit, experience, and `CHeroHandler::reqExp()` next-level calculation shown by the upper Hero Window. It performs no global hero lookup and broadens no visibility.
- This slice excludes commands and all lower-display controls, army contents, artifacts, secondary skills, specialty/biography text, morale/luck, map position, resources, enemy/fogged information, quick hero switching, Town/Battle/Hero Meeting cards, and proprietary Heroes III imagery.

### Native payload and lifecycle

- `ThorContextRecord` now carries four generic bounded detail lines for this and later information cards. Context title, status, and every detail line are UTF-8-safe bounded to 128 bytes through `thorBoundedText()`; `UNKNOWN` explicitly clears detail lines.
- Semantic comparison includes detail lines. An unchanged Hero snapshot does not advance the revision; one rendered Hero-information change advances it once. Replacing one Hero snapshot with another and clearing to `UNKNOWN` both receive new revisions, without retaining stale Hero values.
- `CHeroWindow::activate()` publishes its complete snapshot after the normal base activation. `updateArtifacts()` refreshes only when the window is already active, so constructor-time setup cannot expose `HERO_WINDOW`. `deactivate()` retains the established `UNKNOWN` invalidation. Normal `WindowHandler` ownership and hero-switcher close/recreate behavior are unchanged.

### Android responsibility

- The existing atomic context publication path now carries all four detail lines through JNI, `NativeMethods`, `VcmiSDLActivity`, `ThorSecondScreenController`, and `ThorSecondScreenPresentation`.
- The controller caches the complete latest snapshot, including detail lines, so a recreated lower `Presentation` restores the Hero card without another native publication.
- `HERO_WINDOW` has a dedicated parchment/stone drawing treatment that fitted-renders the Hero label, name, level/class, primary-skill rows, and mana/experience row on the 1080 x 1240 reference layout. It adds no hitboxes and does not submit Thor actions. Adventure Map rendering, all eight existing commands, state, and touch behavior remain unchanged.

### Automated validation

- Focused native Thor tests cover detail-line no-churn, single-line revision changes, replacement snapshots, `UNKNOWN` clearing, every-line bounds, and UTF-8 truncation. Existing context and action tests remain in the same focused suite.
- Focused Android tests cover the fixed four-line model/resource contract; the permanent Thor CI compiles the extended Java/JNI publication path and runs the focused Android test target.

### Required AYN Thor hardware checklist

1. Start/load an Adventure Map and confirm Slice 13 still shows selected hero name/movement and all eight Adventure commands still work.
2. Open a Hero Window and verify the lower dashboard matches the upper Hero Window for hero name, level/class, four primary skills, mana, and experience.
3. Test at least two substantially different heroes and verify no stale values survive hero switching.
4. Rapidly switch between several heroes using the normal upper-screen hero list.
5. Verify a long/localized hero name and class fit without overlap or unreadable clipping where a suitable save/language is available.
6. Open a supported child such as Quest Log and verify its existing Thor context appears; closing it must restore the exact Hero dashboard.
7. Open an unsupported Hero child/modal where applicable and verify the deck fails closed rather than leaving stale Hero information; closing it must restore the Hero dashboard.
8. Toggle/recreate the lower display or pause/resume while a Hero Window is active and verify the latest complete Hero card returns.
9. Verify lower-screen touches in Hero Window perform no gameplay action and do not focus/control the upper display.
10. Verify upper touchscreen and physical controller behavior remain unchanged.
11. Return to Adventure Map and exercise all eight existing lower-screen Adventure commands, including rapid/double-tap stale-input checks.
12. Confirm no crash, stale card, revision churn symptom, duplicate window, delayed action, or lifecycle regression.

### Regression risks and CI procedure

- Check Hero detail text in long/localized languages, lifecycle restore after child/modal windows, Hero switcher replacement, display recreation cache restore, and lower-display inertness. The principal regressions to guard are stale details, duplicate revisions, clipped text, JNI signature drift, and any change to established upper-window or Adventure input behavior.
- Before hardware validation, inspect the final diff and run `git diff --check`, the focused native Thor suite, and any inexpensive affected Android unit checks. Create `ci/thor-slice14-validation` from the exact product commit and push it to trigger the permanent `Thor CI`: preflight, focused native tests, complete ARM64 configuration/package, focused Android tests, package/`is.xyz.vcmi.thor` verification, checksum, receipt, and candidate artifact. Do not promote this slice or mark it hardware validated until the CI-built candidate passes the checklist above.

### Final validation and next handover

- GitHub Actions run `35519819523` passed `Thor preflight` and `Thor ARM64 candidate` for `e60e7e1051d08c5fae07be207182c3f175047429`. The focused native and Android Thor tests passed; the candidate package was verified as `is.xyz.vcmi.thor`.
- Candidate artifact: `thor-candidate-arm64-35519819523`; artifact ZIP SHA-256 `52888aa096054de6f4d57df3ce9d0533ab3459e60a06136038070452531f812f`; APK SHA-256 `4e8158af4543e74922458608460738be331d3b726fed0ac880f202c9243a8cac`.
- The artifact checksum and embedded validation receipt were verified before installation. The APK was installed in place on the AYN Thor, preserving Thor package data, and launched successfully.
- On 2026-09-20, the user reported that all twelve Slice 14 hardware checks passed. Slice 14 is therefore hardware validated; the exact tested product tree is `e60e7e1051d08c5fae07be207182c3f175047429` (`thor: add hero window information dashboard`).
- Future slices must retain the generic bounded detail-line contract, no-churn revisions, authoritative Hero Window lifecycle, inert `HERO_WINDOW` lower display, Slice 13 Adventure snapshot, and all eight Adventure commands. Start a new slice only with a separately approved scope and validation record.

## Slice 15: Town Window live information dashboard

Status: `hardware validated`

### Behavior, scope, and information boundary

- When `CCastleInterface` is the active upper-screen owner, the lower deck displays the exact visible `CGTownInstance`: translated town name and faction, daily gold income, buildings constructed this turn versus the configured cap, visiting hero, and garrison hero.
- The sole native source is `CCastleInterface::town`. The snapshot uses only the Town interface's existing translated name/faction, `dailyIncome()[EGameResID::GOLD]`, `built`, the configured `TOWNS_BUILDINGS_PER_TURN_CAP`, and visiting/garrison hero display names. It performs no global town lookup and exposes no army, resources, buildings list, spells, market, map, or fogged/enemy information.
- The lower Town card remains completely read-only and inert. It adds no action IDs, buttons, gestures, hitboxes, coordinate injection, focus changes, building/recruitment actions, hero switching, or army manipulation. Only `ADVENTURE_MAP` accepts lower-display action touches; its existing eight actions and masks are unchanged.

### Native responsibility and lifecycle

- `CCastleInterface::activate()` keeps its early return and base activation, then publishes a complete `TOWN_WINDOW` record. The record reuses the four generic bounded details: income; `built / cap`; visiting hero name or empty; and garrison hero name or empty. It sends the JNI snapshot only when the semantic revision changes.
- `updateGarrisons()` republishes after the normal Town UI update only while the Town window is active, so visiting/garrison changes refresh without artificial traffic. Building construction/removal retains its deactivate, update/recreate, activate lifecycle; the final activation naturally supplies the fresh dashboard. Town switching remains normal close-and-create ownership behavior, so the old context invalidates and the replacement publishes a full independent snapshot.
- The confirmed `ChangeTownName` client callback refreshes the active matching Town interface after game state has applied the new name; it updates the normal name widget and, when active, republishes the snapshot. No polling and no editable widget text is used as a data source.
- `deactivate()` retains `UNKNOWN` invalidation and does not guess or manually publish a parent. `UNKNOWN` clears all details through the existing context store.

### Android responsibility

- `TOWN_WINDOW` consumes the native title and status when present, retaining existing Town resources only as fail-safe fallbacks. A dedicated parchment/stone Town renderer presents the Town label, fitted name/faction header, and all four fields simultaneously in a fitted two-by-two layout.
- Android resources provide the static labels Income, Buildings this turn, Visiting hero, Garrison hero, and None. The command-deck content description now includes the complete Town snapshot with localized labels and empty-hero fallback. Presentation-cache restoration continues to use the existing complete context snapshot.

### Automated validation

- Focused native Thor tests cover all four Town details, unchanged-snapshot no-churn, exactly one revision for individual income/build/visiting/garrison changes, replacement without stale details, `UNKNOWN` detail clearing, and UTF-8-safe bounds for long Town fields.
- Focused Android tests confirm the existing fixed four-line contract, required Town resources, and the unchanged Adventure action identifier/mask contract. The permanent Thor CI remains the authoritative Android/native integration and ARM64 package gate.

### Required AYN Thor hardware checklist

1. Start/load an Adventure Map and confirm the Slice 13 hero header and all eight Adventure commands still work.
2. Open a Town and verify the lower display shows the exact upper-screen town name and faction.
3. Verify displayed daily gold income against the Town interface.
4. Verify `Buildings this turn` against the current Town state/configured limit.
5. Construct a building where practical and verify the Town dashboard returns with updated income and/or build-count information without stale data.
6. Test visiting/garrison hero state. Where the save permits, verify empty, visiting-hero, and garrison-hero states and confirm the correct translated hero names/fallbacks.
7. Switch between at least two substantially different towns using the normal upper-screen town navigation and verify name, faction, income, build progress, and hero occupancy never leak from the previous town.
8. From Town, open a Hero Window. Confirm the Slice 14 Hero dashboard appears. Close it and verify the exact Town dashboard is restored.
9. Open at least one Town child/modal, such as Town Hall, Fort/recruitment, Marketplace, Tavern, or another available child. Unsupported children must fail closed rather than leaving stale Town data. Closing the child must restore the exact current Town dashboard.
10. Toggle/recreate the lower display and pause/resume while Town is active. The complete latest Town snapshot must return from the Android controller cache.
11. Touch multiple areas of the lower Town dashboard and confirm absolutely no gameplay action occurs and upper-screen focus remains unaffected.
12. Exercise upper touchscreen and physical controller Town interaction and confirm behavior is unchanged.
13. Return to Adventure Map and exercise all eight existing lower-screen commands, including rapid/double-tap stale-input checks.
14. Confirm there is no crash, stale Town card, duplicate presentation, revision-churn symptom, delayed command, residual modal frame, or lifecycle regression.

### Regression risks and CI procedure

- Hardware validation must watch for long/localized field fitting, stale values after Town/hero changes, lifecycle restoration after child/modal windows, display recreation cache restoration, lower-screen inertness, and unchanged upper touchscreen/controller behavior. Preserve Slice 13 Adventure information and commands plus Slice 14 Hero behavior.
- Before hardware validation, inspect the final diff and run `git diff --check`, focused native Thor tests, and inexpensive Android/resource checks. Create and push `ci/thor-slice15-validation` from the approved implementation to trigger only `.github/workflows/thor-ci.yml`: preflight, focused native tests, full ARM64 configuration/package, focused Android tests, `is.xyz.vcmi.thor` verification, checksum/receipt generation, and candidate artifact upload. Do not merge, promote, or mark this slice hardware validated until the CI-built candidate passes every checklist item.

### Final validation and next handover

- GitHub Actions run `35528064000` passed `Thor preflight` and `Thor ARM64 candidate` for `df97a1899dd9deb300a3b6b5fe21803e1e4bfcec`. The focused native and Android Thor tests passed; the candidate package was verified as `is.xyz.vcmi.thor`.
- Candidate artifact: `thor-candidate-arm64-35528064000`; artifact ZIP SHA-256 `29dc8ad80cb7fa95386eef30baa4e5c3ca9b8c3c625b30e0639474885d95d0e3`; APK SHA-256 `c3491ee34b11f962fd4700031d450195120003c11d0cc3123042785b1419be12`.
- The artifact checksum and embedded validation receipt were verified before installation. The APK was installed in place on the AYN Thor, preserving Thor package data, and launched successfully.
- On 2026-09-20, the user reported that all fourteen Slice 15 hardware checks passed. Slice 15 is therefore hardware validated; the exact tested product tree is `df97a1899dd9deb300a3b6b5fe21803e1e4bfcec` (`Candidate Thor Slice 15 build`).
- Future slices must retain the generic bounded detail-line contract, no-churn revisions, exact Town Window ownership and visibility boundary, inert `TOWN_WINDOW` lower display, Slice 13 Adventure snapshot and commands, and Slice 14 Hero dashboard behavior. Start a new slice only with separately approved scope and validation record.

## Slice 16: Context-aware Battle command deck

Status: `hardware validated`

### User-visible behavior and action contract

- The lower display offers exactly **Wait** and **Defend** when the exact active upper owner is the ordinary `BattleWindow`. The controls use the native enabled mask and are disabled whenever the corresponding battle operation is not currently valid.
- During an active tactics phase owned by the same `BattleWindow`, the deck instead offers exactly **Next Unit** and **Start Battle**. Start Battle invokes VCMI's existing end-tactics operation.
- `BATTLE_RESULT`, battle children/modals, menus, dashboards, and unknown contexts remain inert. Existing Adventure Map presentation retains the unchanged eight-control layout and permanent IDs 1 through 8.
- New stable action IDs are `BATTLE_WAIT = 9`, `BATTLE_DEFEND = 10`, `BATTLE_TACTICS_NEXT = 11`, and `BATTLE_TACTICS_END = 12`; `NONE = 0` remains non-executable. The 32-bit action mask remains sufficient.

### Implementation boundary and native responsibility

- The existing single revision-bound native queue now allow-lists actions by exact context: Adventure Map permits 1–8, Battle permits 9–10, Battle Tactics permits 11–12, and all other contexts permit nothing.
- `BattleWindow` remains the sole battle command authority. It publishes Battle/Tactics state only while it is the exact active top window, derives masks from the same current stack, tactics, UI-block, opening, waited, and spell-targeting semantics used by the upper battle UI, and invokes its existing Wait, Defend, Tactics Next, and Tactics End paths. No synthetic input, coordinate path, separate battle rules engine, or direct state/network mutation is added.
- Publication is refreshed at battle activation/deactivation, tactics transitions, active-stack changes through `blockUI`, UI block/unblock, spell-target entry/cancel, and accepted-command consumption. Normal deactivation still publishes `UNKNOWN`; Battle Result retains its complete native lifecycle and never receives action availability.
- `ThorContextRecord` carries a native-only signed 64-bit `actionSubjectId`, populated from the stable VCMI stack `unitId()` only in Battle/Tactics contexts. It is part of semantic equality, so an active-stack change always creates a revision even when the mask is identical. Unknown/non-battle contexts clear it; Adventure's `selectedHeroId` remains unchanged.
- Before execution, the GUI thread rechecks the request revision/context/mask, resolves the live `CPlayerInterface::battleInt`/`BattleWindow`, requires it to still be the exact active top owner, republishes current state, and rechecks the native predicate immediately before calling the existing semantic operation. Accepted commands increment the existing action epoch, publish an immediate zero-mask stale boundary, clear queued input, and execute at most one command per frame.

### Android responsibility

- Android retains the existing Adventure layout unchanged. It renders two large context-specific controls for Battle or Battle Tactics only, uses native enabled bits for appearance and submission, and performs explicit matching hit tests. Disabled controls do not submit.
- Local resources provide Wait, Defend, Next Unit, and Start Battle labels. The command-deck content description includes the relevant Battle controls. The presentation remains not focusable and does not take upper-display input.

### Automated acceptance coverage

- Focused native tests preserve IDs 1–8, verify IDs 9–12 and their masks, reject unknown IDs, test exact allow-lists for Adventure/Battle/Tactics and inert Battle Result, reject disabled/stale/wrong-context requests, verify action-subject revision changes without churn, verify non-battle subject clearing, and verify accepted-command epoch invalidation. Existing bounded FIFO/concurrency queue tests remain in the suite.
- Focused Android tests verify IDs/masks 0–12, distinct Battle masks, and Battle command resources while retaining the Adventure, Hero, and Town contract tests. The permanent Thor CI runs the focused native suite and Android `ThorContextIdsTest` before the ARM64 candidate package.

### Required AYN Thor hardware checklist

1. Start an ordinary battle and verify the lower deck shows exactly Wait and Defend.
2. On a valid active stack, tap Wait once. Confirm ordinary VCMI Wait behavior and that no rapid second tap acts on the next stack.
3. On another active stack, tap Defend once and confirm ordinary Defend behavior.
4. Exercise a stack that has already waited and verify Wait is disabled/unavailable as appropriate.
5. During enemy movement/animations or another blocked state, verify lower actions cannot create a delayed command.
6. With a hero that has a tactics phase, verify the deck switches to Next Unit and Start Battle.
7. Use Next Unit repeatedly and confirm normal tactics selection/cycling with no duplicate movement/action.
8. Use Start Battle and confirm normal transition from tactics into battle and the deck changes to Wait/Defend.
9. Exercise rapid/double taps while the active stack changes; no command may leak to the next stack.
10. Open/close any reachable battle child/modal and verify safe fallback/restoration.
11. Finish a battle, reach Battle Result, confirm it is inert, dismiss it, and verify Adventure Map is visible and interactive with no residual battle frame.
12. Toggle the lower panel off/on and background/resume during battle; restore exactly one correct deck.
13. Verify upper touchscreen and physical controller battle input still works.
14. After returning to Adventure Map, recheck representative Slice 12 commands and verify Hero/Town dashboards still work.
15. Confirm no crash, duplicate presentation, stale command, revision churn symptom, delayed action, or lifecycle regression.

### Regression risks and candidate procedure

- Main risks remain a stale active-stack command, release of a battle child/modal that skips the normal owner lifecycle, Battle Result restoration, blocked/spell-targeted action availability, and any regression to upper touchscreen/controller or the existing Adventure/Hero/Town deck behavior. The completed validation found and corrected a post-opening publication gap: Battle/Tactics controls must be republished after `battleOpeningDelayActive` clears.
- Candidate `f4df29a2eaa0b64a42faae5ceac3d56e1549d270` on `ci/thor-slice16-validation` passed the permanent `Thor CI` in run `35577409407`: `Thor preflight` and `Thor ARM64 candidate`, including focused native and Android tests and package verification. The verified artifact was `thor-candidate-arm64-35577409407`; its receipt records package `is.xyz.vcmi.thor` and APK SHA-256 `d1ca061bd346e9022801d78fd0ef69cb70da3b1c902662868670b4a20acc65f3`.

### Final hardware validation and next-slice handover

- The CI-built candidate was checksum-verified, installed over the existing Thor package, and launched on an AYN Thor.
- The user reported the complete Slice 16 checklist passed: ordinary Wait/Defend, disabled/blocking states, tactics Next Unit/Start Battle after the opening refresh, rapid stack-change protection, child/modal fallback, Battle Result dismissal/restoration, panel and resume behavior, upper touchscreen/controller input, and Adventure/Hero/Town regressions.
- Slice 16 is hardware validated. Retain its exact context allow-list, action IDs 0–12, action-subject revision boundary, native owner/top-window checks, one-shot action epoch, Battle Result lifecycle, and Adventure/Hero/Town behavior in the next approved slice.
- The tested candidate has not been promoted to `ayn-thor-dual-screen`. Promotion remains a separate explicit user-authorized step; start the next slice only after its scope and candidate/promotion strategy are agreed.

## Slice 17: Live Battle information dashboard

Status: `awaiting hardware validation`

### User-visible behavior and information boundary

- With the exact active top owner `BattleWindow`, the lower display now combines the Slice 16 controls with a read-only active-unit dashboard: Battle identity, round, translated active-creature name, count, effective Attack, effective Defense, and current top-creature HP / maximum HP.
- In tactics, the same dashboard shows the selected tactics stack and retains exactly Next Unit and Start Battle. Normal Battle retains exactly Wait and Defend. Battle Result and every non-Battle owner remain inert and cannot retain an active-unit snapshot.
- The sole source is the active stack already owned by `BattleInterface::stacksController`. This slice adds no army list, turn queue, target, hidden/fogged state, range, spell list/effects, hero data, prediction, quick selection, creature art, or battle action.

### Native responsibilities and refresh/invalidation rules

- `BattleWindow::updateThorActionState()` clears title, status, and all four detail slots before rebuilding its atomic Battle/Tactics record. It uses `CStack::getName()`, count, the same effective `getAttack()`/`getDefense()` values as `StackInfoBasicPanel`, and that panel's top-creature remaining-health formula. Normal Battle status is the numeric round; Tactics leaves it empty for Android's local identity.
- The existing bounded UTF-8 `ThorContextRecord.details` transport is reused: Count, Attack, Defense, HP. `actionSubjectId` remains the active stack's stable unit ID or invalid. Equality therefore advances once for every display change or stack identity change, but does not churn for an identical snapshot.
- Battle refresh follows state changes after the normal UI update: activation/opening completion, tactics start/end, UI block/unblock, spell-target entry/cancel, accepted-action invalidation, active-stack activation/reset/add/removal, attacks/effects, round changes, direct command handoff, autofight handoff, tactics end, and battle finish. Publication still fails closed unless this is the exact active top owner; it does not change WindowHandler lifecycle.
- A missing active stack clears title/details and action subject immediately. `UNKNOWN` already clears details, and the existing Battle Result publication starts from a fresh inert record. Slice 16 action IDs 0–12, masks, ownership resolution, revision validation, one-shot epoch, zero-mask accepted-action boundary, and upper-input independence are unchanged.

### Android responsibilities

- `ThorSecondScreenPresentation` now has a dedicated Battle layout on the 1080 x 1240 reference panel: identity/status, fitted unit name, a compact two-by-two Count/Attack/Defense/HP grid, and two large context-specific controls. Hero, Town, and Adventure layouts are unchanged.
- Android resources own the labels, round format, no-active-unit, and unavailable fallbacks. Empty native values never render as `null`, raw IDs, or stale text. Fitted text is used for long translated creature names.
- Only the two rendered action rectangles are actionable. Unit information, headings, round, stats, and empty dashboard space resolve to no action; disabled buttons do not submit. The accessibility description now includes the unit information and the applicable Battle or tactics controls.

### Automated validation

- Focused native coverage now checks bounded Battle details, unchanged-state suppression, one revision each for Count/Attack/Defense/HP/round/subject changes, missing-stack clearing, `UNKNOWN` clearing, and clean Battle-to-Tactics transition. Existing action ID/mask, stale/wrong-context/disabled-action, and accepted-action invalidation tests remain in the same suite.
- Focused Android coverage preserves all IDs and masks 0–12 and verifies the fixed four-detail contract, dashboard labels/fallbacks, and the distinct normal-Battle versus tactics command masks. Existing Adventure, Hero, and Town resource contracts remain covered.

### Required AYN Thor hardware checklist

1. Start a normal battle; verify heading, round, active creature, Count, Attack, Defense, and HP against the upper Battle UI.
2. Move through friendly stacks and observe an enemy stack; each switch must be immediate and expose only the active stack.
3. Damage an active stack where practical; confirm Count/HP refresh, then advance a round and confirm the displayed round changes.
4. Tap Wait, then Defend, once each; verify normal behavior, the already-waited disabled state, immediate stale-action protection, and no rapid/double-tap command leak.
5. Enter tactics; verify selected-stack information plus exactly Next Unit/Start Battle. Cycle Next Unit, then Start Battle, and confirm both upper selection and lower deck convert correctly.
6. During opening, animations, blocked UI, spell targeting, enemy activity, and rapid stack changes, verify no delayed lower command executes.
7. Open/close a reachable Battle child/modal; it must fail closed while covered and restore the exact current dashboard afterward.
8. Finish a battle; Battle Result must be inert and have no stale unit frame. Dismiss it and verify Adventure Map is interactive with no residual battle frame.
9. Toggle the lower panel, then background/resume during battle; exactly one complete current dashboard must restore.
10. Verify upper touchscreen/controller input is unchanged, then recheck representative Adventure commands and Hero/Town dashboards.
11. Confirm no crash, duplicate presentation, stale creature, stale command, revision churn, delayed action, focus change, or lifecycle regression.

### Regression risks and candidate procedure

- Primary risks are stale snapshots during an active-stack handoff, effects changing current values, animation/spell blocking, Battle Result lifecycle restoration, long localized names, and regression to the validated Slice 16 command safety or Adventure/Hero/Town cards.
- Before candidate creation, review the complete diff, run `git diff --check`, focused native Thor tests, and the inexpensive Android source/resource test. Create `ci/thor-slice17-validation` from the complete candidate, commit it, and push only that branch to trigger the existing `.github/workflows/thor-ci.yml` preflight and dependent ARM64 candidate job. Do not install an APK, promote, merge, or mark Slice 17 hardware validated until CI artifact verification and every checklist item above are completed.
