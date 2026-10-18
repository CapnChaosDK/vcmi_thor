# Slice 46 — Thor architecture and targeted simplification

## Scope, baseline and verdict

Audited on 2026-10-18 from freshly fetched product tip
`b1f7bbf59c9b6a1e2ccb81ca34464c57a3b2f486`, on
`codex/slice-46-architecture-simplification`; the initial working tree was clean.
Historical upstream baseline: `819259d97f1de9262b97811ccb081346c20ffef2`.
Fresh upstream `develop` was `66fa496c6`; its merge base with the product is still
the baseline. Its 619 subsequent commits are **not** included in this fork delta.
PR [#35](https://github.com/CapnChaosDK/vcmi_thor/pull/35), its diff and merge
`8476249f8` were inspected: shared movement continuation and `ThorMapControls`
authorization are already implemented, validated and excluded from this refactor.

Verdict: substantial integration complexity, with localized accidental duplication;
the evidence does not support a wholesale rewrite or a claim that the entire fork
is materially overengineered. Visibility filtering, bounded snapshots, revision
epochs, live owner validation and server requests serve different safety boundaries.
The principal maintenance risks are concentrated presentation responsibilities,
repeated adapters, and the exhausted action namespace, rather than raw line count.

## Verified footprint before Slice 46

Physical lines, including comments/blanks, from `git diff --numstat <baseline> <tip>`.
New files are counted directly, including the native files omitted by earlier
GitHub summaries. Categories are disjoint; tests take precedence over language.

| Category | New files / lines | Modified original files / added / removed lines | Net lines |
| --- | ---: | ---: | ---: |
| Android production (`android/` Java/XML) | 51 / 9,584 | 3 / 710 / 0 | 10,294 |
| Native production (C++ source/headers) | 14 / 3,505 | 82 / 6,723 / 164 | 10,064 |
| Tests (native + Android, including registration) | 29 / 7,317 | 1 / 5 / 0 | 7,322 |
| Documentation (Markdown) | 4 / 2,195 | 1 / 392 / 220 | 2,367 |
| CI (`.github/`, `CI/`) | 1 / 370 | 2 / 17 / 1 | 386 |
| Build/configuration/other | 0 / 0 | 6 / 47 / 4 | 43 |
| **Total** | **99 / 22,971** | **95 / 7,894 / 389** | **30,476** |

No files were deleted. Total additions are 30,865, removals 389; production net is
20,358 lines. Of 95 original upstream files modified, 85 are production files
(82 native, two Java, one resource XML). These are historical fork changes, not
all changes exclusively guarded by Thor: e.g. callback request IDs and the bounded
`RedistributeArmyStack` packet are shared engine extensions.

Largest changed production files (full file lengths, **not** Thor attribution):
`ThorSecondScreenPresentation.java` 5,169; `server/CGameHandler.cpp` 4,791;
`client/windows/CCastleInterface.cpp` 3,040; `client/windows/GUIClasses.cpp` 2,381;
`client/CPlayerInterface.cpp` 2,273. Largest Thor-only native source is
`lib/thor/ThorAction.cpp` 1,183; `ThorContext.cpp` is 582.

The outer presentation class spans lines 19–5169 (5,151); its nested
`ThorFoundationView` spans 193–5168 (4,976). Controller class spans 13–507 (495).
Largest measured function: `CBonusSelection::createBonusesIcons` 194–575 (382).
Other long spans, measured from signature through closing brace:
`GameEngine::updateFrame` 160–538 (379), presentation `updateContext` 374–707 (334),
`boundTextFields` 56–386 (331), `validateThorActionRequest` 734–999 (266), and
presentation `onTouchEvent` 1011–1218 (208). Full inherited functions are included;
these spans are size indicators, not AST cyclomatic-complexity scores.

## Modules and ownership

- `lib/thor/`: eight files / 3,200 lines: action/queue validation, context store,
  Adventure map/route values and visual cache. Depends on engine constants and
  shared bounded army values; no Android rendering or live window ownership.
  `sameSemanticState` excludes revision; normalized payload equality controls epochs.
  Its explicit field list and `boundTextFields` must evolve together; bounds and
  equality are distinct responsibilities, not duplicate game-state ownership.
- `client/thor/ThorVisualAssetPublisher.*` and
  `client/windows/ThorRecruitmentSupport.*`: four files / 253 lines, adapting
  installed resources and recruitment snapshots through the established JNI bridge.
  Other new native files are shared minimap color and army redistribution headers.
- Android has 51 new Java production files at the audited tip.
  Most are small value, geometry or gesture helpers; the presentation is the broad
  renderer/input owner. Controller retains snapshots for presentation recreation;
  this is a different lifetime from view-local selection and pending gestures.
- State path: native owner → normalized `ThorContextStore` → `CAndroidVMHelper`
  → `NativeMethods` UI-thread callbacks → controller → presentation.
  Return path: revision-bound plain command → bounded queue → `MainGUI`
  `GameEngine::updateFrame` → exact active owner/live legality → existing callbacks
  → authoritative server. Revalidation along this path is intentional.

## Five ranked findings

1. **Medium — presentation concentration (accidental coupling).**
   `android/vcmi-app/src/main/java/eu/vcmi/vcmi/ThorSecondScreenPresentation.java`,
   especially `updateContext`, `onDraw`, `onTouchEvent`, `commandDeckDescription`:
   one nested view coordinates every deck, geometry, gestures, rendering and resets.
   Five frame-margin implementations (`onDraw`, `recruitmentFrame`, `adventureFrame`,
   `lobbyScenarioControlAt`, `actionAt`) repeat the same formula. Incremental slices
   explain the accumulation; context-specific interaction is essential, repeated
   geometry is not. Remedy: first share shell geometry; consider one cohesive deck
   extraction only after lifecycle characterization. Indicator: five formulas → one,
   fewer context branches per owner. Moderate risk/effort; a broad split is high risk.

2. **Medium — repeated tap state machines (accidental duplication; selected).**
   Android `ThorLobbyScenarioGesture`, `ThorMainMenuGesture`, `ThorBrowserGesture`,
   `ThorWindowNavigation`, `ThorTownServices`: `begin`, `finish`, `cancel` separately
   maintain pointer, revision, session, control and one-shot acceptance. Separate
   feature delivery copied the same safety mechanism. Those checks are essential;
   five implementations are unnecessary. Remedy: one composed `ThorControlGesture`,
   retaining semantic facades, distinct NONE sentinels and browser target/Back rules.
   Indicator: five acceptance/reset implementations → one. Low/moderate risk, small
   effort; existing screen tests plus shared guard tests cover the boundary.

3. **Medium — repeated publication sequencing (accidental adapter work).**
   `client/windows/CHeroWindow.cpp::publishThorHeroContext`,
   `CCastleInterface.cpp::publishThorTownContext`,
   `CExchangeWindow.cpp::updateThorActionState`, and
   `client/adventureMap/AdventureMapInterface.cpp::publishThorInGameContext` repeat
   publish-next/change detection/context/action/payload steps. Recruitment already
   shares `ThorRecruitmentSupport.cpp::publishThorRecruitmentContext`. Specialization
   grew with payloads. Remedy: investigate one post-normalization publisher; keep
   snapshot construction and owner checks local. Indicator: fewer duplicated JNI
   call sequences and one place to add a payload. Moderate effort/risk: callback
   order matters (zero masks and Load Back); do not batch or redesign JNI here.

4. **Medium — upstream merge surface (mostly essential integration).**
   The 85 original production files include `client/GameEngine.cpp::updateFrame`,
   concrete window activation/deactivation and `CExchangeController` routes, plus
   callback/network/server redistribution integration. Exact owners and result
   acknowledgements require hooks; the 379-line frame function also carries many
   repeated refresh/revalidate/execute branches. Remedy: cautiously centralize
   dispatch mechanics later, retaining distinct authority, pending-result and window
   lifecycle rules. Indicator: fewer repeated branches/upstream changed lines;
   moving code alone will not reduce the 85-file merge surface. Moderate/high
   effort and risk; never remove live validation as redundant snapshot validation.

5. **High for expansion, no demonstrated current failure — saturated protocol.**
   `lib/thor/ThorAction.{h,cpp}` (`thorActionMask`, `validateThorActionRequest`) and
   Android `ThorActionIds.java::maskFor` consume all IDs 1–64. JNI correctly carries
   unsigned native masks as signed Java bit patterns; ID 64 is valid. Scarcity has
   encouraged context-qualified commands, which are appropriate when semantics match.
   Preserve IDs and meanings. Before the next new action family, specify a versioned
   capability/command extension alongside the legacy mask with explicit rejection of
   unknown versions and bounded typed targets. Never append ID 65, shift by 64, or
   overload unrelated commands. Indicator: explicit future budget and parity tests,
   rather than more special cases. Protocol migration is moderate/high effort/risk
   and deferred; no new transport is necessary for this slice.

## Tests and development overhead

The initial suite has 24 Java test classes and five native Thor test files.
Repeated gesture rejection cases protect distinct semantic facades; retain them.
No additional parallel test framework is warranted. The permanent workflow compiles
focused native sources directly and runs Android Gradle tests; normal
`test/CMakeLists.txt` registers the nearby component tests. Local generated resource/activity stubs remain uncommitted workstation aids.
The backlog's current state begins after two historical slices, and historical
handoffs repeat old pending statements. Use `Current state`, the latest slice, this
audit and the playbook for routine discovery; preserve the original receipts.
Slice 46 adds a current-state pointer and corrects stale branch guidance.

## Ranked simplification options and implemented boundary

1. Shared tap identity/consumption: selected; immediate five-to-one maintenance gain,
   low/moderate risk, no caller or transport migration.
2. Shared presentation shell geometry: useful follow-up; five-to-one formula gain,
   moderate visual/hit-region regression risk, smaller benefit than the tap contract.
3. Shared normalized native publisher: broader future benefit; moderate risk/effort
   because callback ordering and per-context payload withdrawal must be characterized.

Implementation composes one pure-Java `ThorControlGesture` inside the five existing
facades. Their public/package APIs, screen/control validation, enabled-at-down
semantics, browser target matching and Back revision exception remain in place.
Lobby and build-confirmation already share the lobby gesture facade. No drawing,
display selection, lifecycle caller, action ID, JNI signature or native code changes.
Dragging, recruitment commands and Hero/Town management touch sequences retain
their distinct contracts. No external dependency is introduced.

Before/after: affected production sources 412 → 367 lines including the new helper
(45 fewer); five temporal acceptance/reset implementations → one; five semantic
facades and their independent gesture instances remain. This reduces repeated
safety maintenance, not the number of runtime snapshots or the presentation's size.

## Validation and exact-candidate handoff

- 135 pure-Java JUnit tests pass (132 existing, three new shared/facade guard tests).
- Java 17 source compilation against Android API 35 passes for Thor presentation
  and helpers, using local resource/native stubs; this is not APK packaging.
- Native local syntax/test execution is unavailable: installed NDK host clang lacks
  the host C++ standard-library headers (`algorithm` not found). No native source
  changed. Native preflight and ARM64 packaging remain CI gates; Thor-off build was
  not run locally (native/build paths are unchanged).
- Action/JNI files are unchanged; existing Java stable-ID, signed-mask and browser
  row/footer-boundary tests pass. Separate complete diff self-review found no
  remaining behavior issue; `git diff --check` passed before publication.
- Publish this branch/PR, then advance the permanent `ci/thor-candidate-validation`
  to that exact SHA per the playbook. CI and device receipts belong in Slice 46 of
  the backlog. No new hardware pass is claimed.

Focused checksum-verified AYN Thor checklist:
1. Main menu/New Game/campaign and confirmation taps execute once; cancel/back works.
2. Load rows/pages/Load work; hold Back during a native list refresh and release;
   Back remains usable, while stale rows/changed targets cannot execute.
3. Hero/Town Previous/Next/Close and each available Town service use normal upper
   routes; closing children restores the visible, interactive parent.
4. Cross-control release, rapid/double taps, held input during revisions and extra
   pointers cancel safely; repeat during display toggle/reconnect and pause/resume.
5. Upper touchscreen/controller and single-display play retain normal behavior;
   Adventure route/army dragging and recruitment retain their existing interactions.

Next recommendation: characterize and centralize the repeated shell geometry;
design the protocol extension before adding another action family. Do not broaden
this PR into presentation extraction or native dispatch replacement.
