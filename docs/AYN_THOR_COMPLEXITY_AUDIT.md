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
- Exact candidate `de244e5600b41a717f195fdfc2798a073ec02531` passed Thor CI
  run `39132783698`; downloaded checksums were verified and the user confirmed all
  five device checks passed on 2026-10-18. See the Slice 46 backlog receipt. PR #36
  merged as `4e89f517b2608ffa3070b120ed03571f4c172c79`; subsequent changes
  are validation documentation only. The merged development branch was removed.

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

Historical Slice 46 recommendation: characterize and centralize the repeated shell geometry;
design the protocol extension before adding another action family. Do not broaden
this PR into presentation extraction or native dispatch replacement.

## Slice 47 — Presentation geometry and Main Menu boundary

Based on freshly fetched product `1731a3f11` (2026-10-18), with a clean working
tree. Slice 46's shared `ThorControlGesture` was inspected and retained unchanged.
The historical measurements/findings above describe the pre-Slice 47 tree.

Selected boundary: one immutable shell geometry value plus the complete existing
Main Menu presentation family. `ThorPresentationLayout` is pure Java; its sole
formula supplies drawing, recruitment, Adventure/Battle and lobby hit detection.
It retains both the old raw content extent and RectF subtraction order because
rewriting these as equivalent algebra can change a float by a fraction of a pixel.
Geometry is recomputed from current dimensions/density, so resize/recreation needs
no new cache or reset protocol.

`ThorMainMenuDeck` owns menu rendering, control hit detection, accessibility text
and its existing semantic gesture facade. It receives current values per call,
retaining only the gesture. The view keeps native submission and its captured
revision, click notification, session checks and all existing cancellation sites.
The extracted component returns an accepted action only after the unchanged
`ThorMainMenuGesture`/`ThorControlGesture` accepts it. No snapshot subscription,
controller reference, callback interface or framework was added. The component
knows neither `ThorSecondScreenPresentation` nor `NativeMethods`, and can be tested
without constructing an Android Presentation/View. Main Menu no longer reaches
through the Adventure frame helper. `ThorDeckDrawing` shares the existing palette,
fitted text and choice-button primitives with the view; it retains no Paint, view,
resources, snapshot or lifecycle state. Its primitive bodies match the original
operations exactly, with Paint made an explicit parameter.

| Indicator | Before | After |
| --- | ---: | ---: |
| Shell margin formulas | 5 | 1 |
| Presentation physical lines | 5,169 | 5,044 |
| Presentation direct MainMenuState references | 14 | 4 (routing/cancellation only) |
| Menu rendering/hit/description/gesture owners | Broad view | One 112-line menu component |
| Geometry / shared drawing helper lines | Embedded | 32 / 58 |
| Combined affected production lines | 5,169 | 5,246 (+77) |
| Independent menu event-adapter test boundary | None | Plain events and snapshot values → accepted action |

This is a responsibility reduction, not a claim of large file-size savings. The
view's lifecycle/snapshot/native dependencies do not flow into the new menu owner;
there are no backwards references or forwarding calls for each menu row. Hero,
Town, Adventure, Battle, recruitment and specialized gestures remain in their
existing owners. The broad view is still large; extracting more screen families
without their own coupling analysis is deferred. Native publication duplication
and the exhausted protocol namespace remain separate future work.

Validation: 140 JUnit tests pass, including five new tests covering 32 size/density
shell combinations, known reference/density-floor dimensions, eight menu contexts
across 12 display configurations, rendered row centers and inert margins/header,
move/one-shot acceptance, cancellation, extra pointers, changed revisions/sessions,
availability and invalid session rejection. Full Thor Java source compilation with
Java 17 compatibility against API 35 passes using local resource/native/activity
stubs. Native checking is unavailable locally (`algorithm` absent in the NDK host
toolchain); existing CI gates remain required. `git diff --check` passes. Exact CI
and hardware evidence belongs in the Slice 47 backlog receipt; no device result is
inferred from these tests. The checklist covers all shared-frame consumers as well
as menu layout/input, reconnect, resume, restoration and upper-input regressions.

Slice 47 validation receipt (2026-10-18): exact candidate
`f9ba6e6d8124cec6eeae418f2cd7bcac4380069c` passed both jobs in
[Thor run 39134929591](https://github.com/CapnChaosDK/vcmi_thor/actions/runs/39134929591).
The artifact and installed APK were independently checksum-verified; APK SHA-256
is `9f0b528bfd1918df37fa926575ca884ef328395a2c729de643314fa8bab619fa`.
The user confirmed all five hardware checks passed on the installed AYN Thor APK.
Status is `hardware validated`; PR #37 merged at the user's request as
`332ec554311dcee78d765dbcb265a5099bdeb524`, preserving the tested product/build
tree. See the backlog for validation and branch-cleanup receipts. These receipt-only updates do not change the tested build.


## Slice 48 — Native window publication boundary

Baseline: freshly fetched product `ee900792eab5c10e706da3fd5b2fb8a96efdeae7`
(2026-10-18). Inspection covered Hero, Town, Hero Meeting, Adventure, recruitment,
context normalization, JNI and the visual publisher. Slice 46–47 remains intact.
Duplication is post-construction adapter work: store update/revision gating and
context/action JNI sequences. Snapshot construction, owner checks, pending requests
and server authority remain necessary specialization at their original sites.

`ThorWindowPublisher` shares these mechanics for four window families (five IDs),
with explicit Meeting payload-before-actions ordering and Hero/Town zero active
masks. The store retains normalization and semantic equality; only its returned
snapshot is transported. Recruitment retains its epoch and conditional payload/
visual publication, including unsupported-owner `UNKNOWN` withdrawal. Other forced
publications, clearing and normal parent activation are unchanged. The concrete
adapter is Android/Thor guarded; a template seam records actual transport calls in
host-native tests without a VM, event framework or new interface hierarchy.

| Indicator | Before | After |
| --- | ---: | ---: |
| Window transaction/revision gates | 4 | 1 |
| Window epoch carry/reset/invalidate expressions | 3 | 1 |
| Common context/action call sites in these transactions | 4 each | 1 each |
| Snapshot construction owner files | 4 | 4 |
| Affected owner physical lines | 4,824 | 4,778 |
| New publisher source/header lines | 0 | 89 |
| Combined affected production lines | 4,824 | 4,867 (+43) |
| Recruitment support direct JNI dependency | Yes | No |

This reduces distributed maintenance and adds a testable sequencing boundary; it
does not claim net line savings or eliminate upstream window integration. Empty
Meeting artifact fallback, optional Town management and visual ordering remain
explicit. No Java/JNI contract, action IDs, 64-bit representation, native authority
or normalization changes are made.

Adventure stays specialized: refresh/coalescing timestamps, map RGB/viewport reuse,
route and movement publication belong to its owner. Its forced publisher also
intentionally sends unchanged revisions. Moving these into an ordinary changed-
window transaction would require more policy/indirection than this slice warrants.
Other modal/menu publications remain explicit.

Validation: 140 Java contract tests pass. Six new native recording tests are
registered in CMake and permanent preflight/filter. Local native execution is
unavailable; Android-target syntax checking stops at missing Boost headers.
Separate diff/caller review caught and fixed unsupported recruitment withdrawal.
No local full non-Thor build is claimed. Exact CI/candidate receipts and the five-
check hardware gate belong in the Slice 48 backlog record. Resume feature work
after validation; further architectural cleanup needs concrete evidence. A versioned
protocol design is required before a new global action family.

Slice 48 handoff: [PR #38](https://github.com/CapnChaosDK/vcmi_thor/pull/38),
exact candidate `97cdb79832fcb943692f9afae312fd15c7a132f8`,
[Thor run 39138368858](https://github.com/CapnChaosDK/vcmi_thor/actions/runs/39138368858).
The permanent candidate advanced normally after fetch/ancestry verification. Status
is `awaiting CI`; no package/checksum/device result is claimed. This receipt-only
follow-up does not change the candidate product/build tree.

Slice 48 CI completion (2026-10-18): run `39138368858` passed native preflight
(149 tests, including six publisher tests), Android contracts and ARM64 packaging
for exact candidate `97cdb79832fcb943692f9afae312fd15c7a132f8`. The downloaded
APK matches receipt/checksum SHA-256
`a99d26f234cf409271584cd5f1c29d87cf933ff0d44198c51d38c3c6af3ee833`.
Warm compiler-cache hits: 833/846 (98.46%); ARM64 build: 131 seconds.
Status is now `awaiting hardware validation`; earlier pending CI text is historical.
No device pass, merge or promotion is claimed. See the backlog receipt/checklist.

Slice 48 hardware confirmation (2026-10-18): the user confirmed all five device
checks passed on installed candidate `97cdb79832fcb943692f9afae312fd15c7a132f8`,
APK SHA-256 `a99d26f234cf409271584cd5f1c29d87cf933ff0d44198c51d38c3c6af3ee833`.
Installed bytes were independently verified; behavior is user-reported. Status is
`hardware validated`; earlier pending statements are historical. Merge and branch
cleanup are explicitly authorized; their completion receipt belongs in the backlog.
