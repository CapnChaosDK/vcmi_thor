# AYN Thor dual-screen Heroes III backlog

This is the maintained planning and validation record for the AYN Thor fork. Do not remove deferred work when implementing an earlier slice. Before each implementation slice, record the proposed behavior, implementation boundary, native and Android responsibilities, focused acceptance tests, and regression risks, then wait for user approval.

Status values: `planned`, `proposed`, `approved`, `in progress`, `awaiting hardware validation`, `hardware validated`, `blocked`, `deferred`.

## Current state

- Phase: first-slice approval.
- Status: `proposed`.
- Upstream reference: `https://github.com/vcmi/vcmi.git`, default branch `develop`.
- Baseline: upstream commit `819259d97f1de9262b97811ccb081346c20ffef2`.
- Fork: `https://github.com/CapnChaosDK/vcmi_thor`, public.
- Working branch: `ayn-thor-dual-screen`, published from the clean upstream baseline.
- Remote safety: `origin` points to the CapnChaosDK fork; the local `upstream` push URL is disabled.
- Repository discovery: complete; see `docs/AYN_THOR_DISCOVERY.md`.
- Build readiness: source inspection is complete, but the checkout's submodules are not initialized and the local Windows environment has no VCMI CMake, Ninja, Conan, Qt, or Java tools on `PATH`. Android SDK platform tools and NDK `27.0.12077973` are installed; JDK 17 candidates exist outside `PATH`. Resolve the supported Windows Android toolchain before the first candidate build.
- Product code changed: no.
- Hardware validation claimed: no.
- Approval to implement a feature slice: no.

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

Status: `proposed, pending user approval`

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

Await explicit approval or requested changes to Proposed Slice 1. Do not implement it before approval.
