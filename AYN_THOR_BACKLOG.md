# AYN Thor dual-screen Heroes III backlog

This is the maintained planning and validation record for the AYN Thor fork. Do not remove deferred work when implementing an earlier slice. Before each implementation slice, record the proposed behavior, implementation boundary, native and Android responsibilities, focused acceptance tests, and regression risks, then wait for user approval.

Status values: `planned`, `proposed`, `approved`, `in progress`, `awaiting hardware validation`, `hardware validated`, `blocked`, `deferred`.

## Current state

- Phase: Slices 1 through 4 are implemented, CI-built, and hardware-validated; Slice 5 is implemented and awaiting hardware validation.
- Status: `awaiting hardware validation`.
- Upstream reference: `https://github.com/vcmi/vcmi.git`, default branch `develop`.
- Baseline: upstream commit `819259d97f1de9262b97811ccb081346c20ffef2`.
- Fork: `https://github.com/CapnChaosDK/vcmi_thor`, public.
- Working branch: `ayn-thor-dual-screen`, published from the clean upstream baseline.
- Remote safety: `origin` points to the CapnChaosDK fork; the local `upstream` push URL is disabled.
- Repository discovery: complete; see `docs/AYN_THOR_DISCOVERY.md`.
- Reproducible build/device procedure: `docs/AYN_THOR_BUILD_PLAYBOOK.md`.
- Build readiness: use Linux/JDK 17 CI for a complete ARM64 APK. This Windows host is useful for focused source checks, but the official dependency cache contains Linux-host Qt generators and Android Studio's JDK 25 has a Gradle cache-close limitation.
- Published implementation: Slice 1 commit `ed8e57130`; Slice 2 commit `9300bcc59`; Slice 3 promoted after hardware validation; Slice 4 commit `17fbdfbb9`.
- Hardware validation: the lower command deck is visible and inert; it preserves upper-screen focus through resume/toggle checks. Slice 2 additionally shows the localized `Main menu / Choose a game mode` context and logs monotonic `MAIN_MENU` revisions. Slice 3 shows the localized New Game card, clears it safely on unsupported tabs, and passes panel-toggle, pause/resume, and input-regression checks. Slice 4 adds the localized Load Game card, restores the root card on Back, and fails closed on Campaign, Credits, malformed, and mod-added tabs. Slice 5 is awaiting hardware validation; automated checks cover the localized Campaign card, exact mapping, New Game restoration, and fail-closed unsupported names.
- Approval to implement feature slices: yes; Slices 1 through 3 were approved by the user on 2026-09-13 and Slice 4 on 2026-09-14.

## Working rules

- The native engine remains authoritative for gameplay and information visibility.
- The lower display supplements the primary game; it never replaces or captures its input paths.
- Use Android public multi-display APIs and select a presentation display dynamically. Never hardcode a transient display ID or rely only on a device model.
- Preserve upstream behavior on single-display devices and guard Thor-only work with a build flavor or equivalent compile-time boundary.
- Use a distinct application ID so the Thor fork has separate assets, settings, and saves and can coexist with the standard app.
- Process game mutations on the existing game/SDL thread through native operations.
- Treat all Android requests as untrusted: revision-lock them, consume once, and revalidate context, identity, bounds, ownership, destination, and enabled state natively.
- Invalidate queued input, gestures, selection, information, and visuals on context or lifecycle changes.
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

## Next action

Run the focused Slice 5 hardware checklist and report PASS or any regression. Do not add native commands, coordinate injection, or mutable gameplay state without a separately approved design.

## Approved Slice 5: read-only Campaign submenu context

Status: `awaiting hardware validation`

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
