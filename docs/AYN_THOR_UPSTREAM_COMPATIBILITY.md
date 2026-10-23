# Controlled upstream compatibility checkpoint

## Verified comparison — Slice 51, 2026-10-18

Fetched `origin` (CapnChaosDK/vcmi_thor) and `upstream` (vcmi/vcmi), without
changing product history. Upstream push URL remains `DISABLED`.

| Reference | Exact revision |
| --- | --- |
| Product `ayn-thor-dual-screen` / assessment input | `b4b7138c382956239c5b0633cb431e72df2b6a5a` |
| Upstream `develop` | `5881a6f93bc3006d982198df1bd3a0e982b49e80` |
| Actual common ancestor | `819259d97f1de9262b97811ccb081346c20ffef2` |
| #8014 upstream merge | `66fa496c6328a276c4eb14c0011b3a9d751dfac3` |

`git rev-list --left-right --count <product>...<upstream>` reports **274 fork-only
and 623 upstream-only commits**. Baseline-to-tip name sets contain 219 fork paths
and 1,145 upstream paths; **57 paths overlap**. These are file/commit counts, not
estimates of Thor-only code or behavior equivalence.

`git merge-tree --write-tree --name-only <product> <upstream>` exits 1 with ten
content-conflicted paths: `AGENTS.md`, `CI/install_conan_dependencies.sh`,
`client/CPlayerInterface.cpp`, `client/adventureMap/CMinimap.cpp`,
`client/mainmenu/CMainMenu.h`, `client/windows/CExchangeWindow.cpp` and `.h`,
`client/windows/CreaturePurchaseCard.cpp`, `client/windows/InfoWindows.h`, and
`lib/serializer/RegisterTypes.h`. The simulation writes Git objects only; no merge,
index update, or product rewrite occurred. Its tree is diagnostic, never a candidate.
Automatically merged files are **not evidence of runtime compatibility**.

Major overlapping owners also include `GameEngine`, `HeroMovementController`,
Adventure interfaces/shortcuts, BattleInterface/actions/window/result, WindowHandler,
lobby options/selection, Hero/Town/Meeting/recruitment windows, callbacks, packets
and server handlers. Preserve their Thor publication, request acknowledgement,
stable IDs, epoch validation and ordinary owner restoration when integrating.

## First adopted correction: staged battle animations

[#8014](https://github.com/vcmi/vcmi/pull/8014) changes only
`BattleInterface::waitForAnimations()`: after the current animation wait, dispatch
the earliest pending stage and wait for any animation it starts, repeating until
the queue is empty. The fork's existing `endAction()` kick starts only one stage;
it does not guarantee later synchronous stages or completion-queued reactions drain.

Slice 51 adopts this behavior through the small `BattleAnimationWait.h` orchestration
seam. Existing `executeStagedAnimations()` still selects/dispatches stages; existing
MainGUI frame ticks still advance animations. The caller retains the interface lock
for queue access/dispatch, releases it only around `ConditionalWait::waitWhileBusy()`,
and reacquires it before returning or propagating termination. No new thread, JNI,
command, server mutation or Thor publication route is introduced. The endAction
kick and all Thor hooks remain. The helper is shared by all client builds.

No arbitrary iteration cap discards valid chained events. Each existing stage
dispatcher removes its selected events before invoking callbacks; synchronous
stages advance immediately, asynchronous stages yield the interface lock while
waiting. A callback that indefinitely requeues itself remains an invalid producer,
not a condition to conceal by dropping gameplay events.

Six deterministic tests exercise idle/running completion, synchronous drain,
multiple animated stages, stage-added events, completion-added reactions and
termination propagation. They test scheduling and callback lock boundaries, not
real rendering or OS thread synchronization. Permanent Thor preflight runs them;
normal project test registration includes them independently of Thor.

## Army-transfer assessment (Slice 51 historical input)

The table below records Slice 51 findings before Slice 52; see the next section for current implementation status.

[#8035](https://github.com/vcmi/vcmi/pull/8035), merge `005a888b7`
(`c72621eab` implementation), changes **54 files, +1,045/-292 lines** relative to
its first parent. It is not imported. Its parent already has the activity/question
system and strict callback getters; direct cherry-picking would mix those API
assumptions with this fork's query system.

| Boundary | Fork evidence and remaining work |
| --- | --- |
| Hero Meeting quick/exact transfer | CExchangeWindow and CGarrisonInt validate current owner, stack, destination, last-stack/locked state; Thor requests bind live identities and revision. Preserve source-only action 15 and exact-pair action 16. UI checks alone do not secure arbitrary network requests. |
| Atomic redistribution | `redistributeArmyStack()` already checks both hero owners, exchange context, expected source identity/count, positive bounded unique destinations, matching creatures, overflow and retention of at least one source creature before one BulkRebalanceStacks packet. Upstream #8035 has no equivalent Thor packet; retain its visitor/serializer registration and acknowledgements. |
| Town/visiting/garrison | Current Thor Town dispatch uses the live CGarrisonInt route; server arrangeStacks checks exchange relationship, ownership and last-stack through existing stack operations. Upstream's generalized `isLocked()` and player-bound exchange checks are absent. Preserve town-hero transitions and modal ownership. |
| Locked armies | Fork CGarrisonInt blocks taking from its non-removable upper army; arrangeStacks checks CGGarrison::removableUnits. `bulkMoveArmy()` lacks an equivalent lock check. Reverse split lock validation uses `s2->getStackCount(p1)` instead of direction-based source/destination validation. #8035 corrects this. |
| Authorization | Bulk split/merge/rebalance visitors check connection player and active turn, **not source ownership**; handlers dereference an army without a null guard. `isAllowedExchange(id,id)` returns true and its dialog search is not tied to the requesting player. #8035 introduces `canTakeTroopsFrom`, player-bound exchange and validated army lookup. These are priority safety gaps, even though Thor UI rejects such requests. |
| Integrity/minimum stack | Existing move/erase paths retain a hero's last stack and redistribution conservatively keeps one source creature. Bulk move always retains one even for armies not requiring it; smart rebalance can enqueue zero moves. Upstream also validates positive counts, same-slot/non-army requests, packet application, rewards, recruitment and market shapes. Existing Thor bounds do not cover all these paths. |

**Immediate triage priority:** server bulk ownership/non-army rejection and locked,
reverse-direction transfer validation require correction in the next army safety
milestone. No additional army production change is made here: these are independent
of the animation boundary, need authoritative server fixtures, and must preserve
neutral reward/allied exchange behavior. No exploit execution or exhaustive server
security audit is claimed. Do not treat this checkpoint as closing these issues.

Recommended coherent milestone, without requiring the full activity migration:

1. Characterize current query-bound Hero Meeting, neutral reward, allied garrison
   and Town exchange permissions with server tests derived from upstream
   `test/server/ArmyManagementTest.cpp` (16 scenarios).
2. Adapt player-bound source authorization, safe army lookup and lock direction
   checks to current queries. Reject invalid/non-army/same-slot/zero/negative and
   overflow requests before changes; preserve last-stack/count conservation and
   all-destination validation of Thor redistribution. Test rejected requests leave
   both armies unchanged and acknowledgements release pending UI.
3. Integrate necessary packet-application checks with producers together; audit
   bulk notification ordering and preserve the custom redistribution registration.
   Decide separately whether generalized isLocked/GarrisonDialog schema migration
   is necessary. Keep the current wire format if it is not.
4. Validate normal and Thor paths: Hero Meeting quick/exact/bulk/redistribution,
   Town garrison/visitor, locked and allied/neutral armies, stale/rapid requests,
   save/load and multiplayer rejection. Then one reviewed exact-SHA CI candidate
   and physical Thor checklist under the build playbook.

## Slice 52 — Adapted army-transfer boundary

Freshly fetched product `d05c1df0291b663c07866b1c59fbfe7b948b8362` and the
unchanged pinned upstream ref underpin the targeted #8035 adaptation. Source
permission and exchange authorization now bind to the authenticated player and
their exposed query. Owned Town/visitor relationships, allied deposits and neutral
reward collection remain supported. Actual transfer direction determines removal
permission; the server retains the existing dialog flag without a wire change.

Every bulk plan is simulated before publication and before mutation, including
all destinations, count/type/overflow checks and required last-stack retention.
Single moves share that integrity boundary. Empty/self/zero moves fail, bulk
retention is conditional on needsLastStack, and smart rebalance emits positive
moves with bounded intermediate totals. Existing artifact processing is retained,
including safe non-Hero conflict cleanup. Thor redistribution keeps its packet
registration/expected-state contract and matching-query authorization; actual
PackageApplied results reach its existing pending-release/revision-refresh path.
Java/UI/JNI/action contracts and the query architecture remain. Legacy requests
are revalidated against live state; no exactly-once/nonce guarantee is added.

Twenty-one server scenarios use real handlers, queries, state application and results
through in-memory transport. Standard vcmitest and a focused non-Thor native target
register them; the existing Thor preflight must build/run them before packaging.
Local ARM64 syntax checks and 149 Android JVM tests passed during implementation.
PR #43 follow-up also builds the focused target and passes all 21 server scenarios
on Linux after correcting development-directory detection for its executable.
Corrected candidate `078a28c0a` passed Thor CI 39217037034, including the real
non-Thor server suite and ARM64 packaging. Archive/APK/receipt/package verification
passed. The exact APK was installed on the AYN Thor and the user confirmed all
seven physical checks passed on 2026-10-19. PR #43 is promoted as `fddb90d09`,
with the exact tested product/build tree preserved; full desktop rendering/runtime
is unverified.
Current review, candidate receipts and the complete hardware checklist belong in
[Slice 52](../AYN_THOR_BACKLOG.md#slice-52--complete-army-transfer-authorization-and-integrity).

## Priority and dependency-aware synchronization plan

| Priority | Candidate / evidence | Value, risk and decision |
| --- | --- | --- |
| 1 | #8014 staged completion | High battle reliability value, low integration risk; adopted here. |
| 2 | #8035 army authorization/integrity | Adapted in Slice 52 above; CI/artifact and user-confirmed hardware acceptance passed; promoted through PR #43, no wholesale cherry-pick. |
| 3 | Controller cancellation/navigation (#7772, `dfe0d541c`, `6aa1bf766`) | Targeted native adaptation in Slice 53 below; exact integrated candidate passed CI/hardware and was promoted through PR #44. |
| 4 | Strict callbacks (#8018, `e23240336`) | Adapted in Slice 54 below: strict object/tile/player lookups, optional stale/hidden identities, fork-specific server/Thor/AI callers and focused regressions. CI succeeded and the user confirmed hardware acceptance on the exact candidate. |
| 5 | Activity/question server migration (`54a6fc585` and `server/activities/`) | Slice 55 is in progress. The covered-reply player-isolation prerequisite and legacy answer-validation stage passed CI and user-reported hardware checks, then were promoted through PRs #52 and #53. The full migration must still replace queries, QueryID/reply routes and exchange/battle continuation ownership together; acknowledgement, result dismissal and movement stop/resume remain high-risk acceptance paths. |
| 6 | Chunked saves (#8022, `f6b5eb30c`) | New SaveFileFormat/legacy loader, SAVE_CHUNKS and subsequent serialization versions. Migrate serializer, lobby save browser and custom packet registration together; test old saves and multiplayer compatibility explicitly. |
| 7 | Download/build/SDL changes | Defer dependency release 2026-10-13, libcurl downloader/Android CA JNI, optional LTO/section GC and SDL3 rendering changes to a dedicated toolchain checkpoint. Preserve Thor package ID, Linux Qt deployment hook, caches and signing gates. |

Android tracked build/Gradle/profile files do not show a new NDK or Qt requirement
in this baseline comparison. This does **not** validate a replacement dependency
bundle: upstream's installer release changed, launcher now finds CURL instead of
Qt Network, and NativeMethods adds applicationId/system CA access for native TLS.
SDL2/SDL3 renderer/input changes also exist; do not migrate them to obtain #8014.
The current Thor workflow's NDK r29/Linux/JDK17 and Qt packaging remain authoritative;
the older generic Android developer guide's r25c is not the candidate recipe.

Cadence: assess freshly fetched upstream every two weeks or before three further
gameplay slices, whichever comes first; triage security/serialization/build changes
immediately. Pin both SHAs, compare changed path sets and subsystem history, rerun
merge-tree, classify semantic dependencies, then adopt one coherent dependency
group per reviewed PR/candidate. Keep this assessment current at each checkpoint.
Avoid per-commit archaeology, automatic bulk updates and new permanent CI workflows.

## Validation and physical acceptance

Use [build playbook](AYN_THOR_BUILD_PLAYBOOK.md) for exact candidate publication,
artifact digest/APK checksum verification and promotion. CI/device receipts belong
in the Slice 51 backlog record. Local ARM64 syntax checks and 149 Android JVM tests
do not prove full native execution, desktop builds, actual spells, or physical UI.
Full non-Thor build/runtime compatibility is not yet verified.

After the exact candidate passes native preflight and ARM64 packaging, verify its
artifact, receipt, package `is.xyz.vcmi.thor` and APK checksum before this checklist:

1. Play manual battles with ordinary attacks and chained/multi-target spell-hit
   reactions (e.g. Chain Lightning), at normal/fast animation speeds. All effects
   finish, the next unit activates, and neither frozen waits nor skipped reactions
   occur; also test a spell ending the battle and return to Adventure.
2. Use Wait/Defend and Auto-combat start/stop, upper and lower controls, plus Battle
   Quick Controls. Confirm legacy/extended commands remain available only in the
   right state, rapid/stale taps do not double-act, and result dismissal restores
   the visible, interactive parent without a residual battle frame.
3. During effects and between turns, toggle/reconnect the second display and
   background/resume. Confirm one responsive current lower deck, no old hit regions,
   correct restored modal/context, and no deadlock/crash on battle exit.
4. Verify upper touch/controller targeting and focus, battle settings/spellbook
   restoration, Adventure movement and Hero/Town/Meeting controls. Test lower display
   disabled/single-screen operation; separately build/run a non-Thor client before
   claiming that platform gate passed.

Exact-SHA CI, installation and user-confirmed hardware receipts are recorded in
the Slice 51 backlog record; Slice 51 is hardware validated and promoted via PR #41.
Full non-Thor build/runtime compatibility remains unverified. Do not merge automatically.
Slice 52 has since passed its exact-candidate CI and hardware gates and was promoted
through PR #43. Slice 53 below passed its integrated CI and hardware gates and was
promoted through PR #44; the full activity/query migration was deferred at that
checkpoint and is now tracked as Slice 55 in the backlog.


## Slice 53 — Native controller navigation and cancellation

Product base `dc637214c0ac68a7057965948302a40597618a97` and PR #7772 head
`6aa1bf766d54089017d7a638ba547c09dcef5467` were freshly fetched on 2026-10-20.
The fork had cursor/swipe emulation and Thor Battle Quick Controls but neither
native grid/unit navigation nor prerequisite cancellation from `dfe0d541c`.

Adapted: dispatcher press ownership/cancellation, CKeyShortcut and gesture-owner
cleanup, SDL2/SDL3 reset/device handling, battlefield directional focus, native
preview/action legality, native melee-approach geometry and turn notifications.
Fork-specific integration preserves GPU/software field rendering, Thor lower
commands, availability publication and immediate-parent window restoration.

Corrections beyond the reference: A invokes the native action owner directly;
confirmation binds stable actor/target IDs, action/spell, hex and approach; canceled
sticks require neutral; newly connected/remapped held controls are suppressed;
queued device/pause/focus boundaries preserve input order on MainGUI. Dispatcher
cancellation marks presses before callbacks, supporting reentrant window cleanup.
Mouse shortcuts cannot continue into a window opened by their own dispatch.
Back Auto remains bound; X/Y expose native Defend/Wait while melee shoulders
select approaches. Tactics movement stays on native legality and right shoulder
can end tactics. Ordinary native inspect windows already support both face buttons,
so no creature-window shortcut/constructor migration is needed.

Intentionally excluded: upstream controller-family binding schema, new shoulder
glyph images, floating prompt layout, side-panel geometry and settings UI changes.
Existing native action cursor/highlights/status provide feedback; left-stick click
exposes existing cursor-mode advanced interfaces. No activity/query, wire/JNI,
Android presentation, proprietary asset or unrelated upstream migration.

Local tests/syntax checks and independent review are recorded in
[Slice 53](../AYN_THOR_BACKLOG.md#slice-53--complete-controller-driven-battlefield-navigation-and-safe-cancellation).
Exact integrated candidate `9e5fa181e4b9193100724ca647629302d05c23c5` passed
Thor CI 39824620408 and user-confirmed hardware acceptance; PR #44 promoted its
product/build tree. Detailed receipts are in the backlog.


## Slice 54 — Strict and optional callback boundary

Adapt `e2324033600611a182cfb10b6ae4db024ccc7d18` from product `b98058529`.
Origin and upstream were fetched on 2026-10-22; upstream develop was `610c3cbec`.
That fetch does not replace the full Slice 51 divergence assessment above.

Required object, const tile and player-state getters throw for unavailable data;
optional `tryGet` counterparts return null without changing visibility/access rules.
Hero/Town typed getters retain nullable semantics, and Lua `Game:getObj` still
returns nil for absence. Missing-player status remains `WRONG`.

Fork-specific adaptation preserves Slice 52's server rejection/acknowledgement path,
lower-map fog filtering, minimap marker safety, current native window ownership and
nullable top-object lookup. AI startup, threat scans, partially visible teleport
exits, non-coastal shipyards and delayed object-selection callbacks handle absence
explicitly. No activity/query, network packet, serialization, Java/JNI or build
migration is imported. The native callback tests share the existing non-Thor server
boundary target; normal tests retain the AI exploration coverage. See the
[Slice 54 contract](../AYN_THOR_BACKLOG.md#slice-54--safe-callback-lookups-and-stale-action-handling)
for scope, validation and exact candidate/hardware state.
