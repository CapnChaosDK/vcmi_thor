# Second-screen fork scope audit

Current reconciliation work uses pinned fresh upstream with a minimal second-screen
layer. See [upstream overlay](AYN_THOR_UPSTREAM_OVERLAY.md) for its contract, intentional
compatibility changes and validation gates. Earlier assessments below are historical
for their pinned trees; they do not authorize engine feature adaptations.

## Decision and evidence

On 2026-10-25 the user clarified that this fork is for second-screen additions.
Everything else should follow the upstream base and remain easy to update. This
supersedes the earlier selective engine-migration roadmap. This audit changes
documentation only; it does not revert code, merge upstream, change save/network
formats, alter candidate refs or invalidate historical device receipts.

Inspected the clean product checkout, first-parent and subsystem history, current
Thor owner/transport paths, callback request adapters, army packet application,
serialization declarations and registrations, and the current upstream equivalents.
Fetched upstream without recursing into submodules; upstream push remains disabled.

| Reference | Pinned revision |
| --- | --- |
| Audited product | `009a209ce45a3ab2fe4419de3502fb728bb4a8e1` |
| Fetched upstream `develop` | `46f2e15ec69e0507e6d0195d6357df5a2813dde1` |
| Common ancestor / original fork base | `819259d97f1de9262b97811ccb081346c20ffef2` |

The product has 336 commits outside upstream ancestry; upstream has 809 outside
product ancestry. These are ancestry counts, not counts of unique behavior: the
fork selectively adapted some upstream changes without importing their history.
Against the common ancestor the fork changes 451 paths, with 45,797 added and 4,584
removed lines, including tests/docs/tools. Upstream changes 1,333 paths; 258 paths
overlap the fork's changed-path set. These numbers measure update surface, not
semantic conflicts or Thor-only production size. No new merge simulation or full
runtime/compatibility test was performed in this documentation pass.

## Classification and disposition

Paths below are representative owners of each change family, not a claim that every
line in a mixed file is Thor code. No complete file should be reverted solely on
this table. Tests and build registration travel with their owning behavior.

| Existing change family | Evidence / purpose | Disposition under the clarified scope |
| --- | --- | --- |
| Android lower presentation and lifecycle | `android/vcmi-app/.../Thor*.java`, `VcmiSDLActivity`, `NativeMethods`: display selection, view state, gestures, bounded snapshots, lifecycle cancellation | Retain as the core overlay, disabled outside Thor. Preserve upper focus/input. |
| Native transport and adapters | `lib/thor/`, `lib/CAndroidVMHelper.*`, `client/thor/`, guarded `GameEngine` dispatch and window publication/action hooks | Retain minimal bounded data transport, visibility checks, revisions and MainGUI routing. Reconnect adapters to the chosen upstream owners rather than carrying old owner implementations. |
| Thor information, menus, movement and services | Hero/Town/Meeting/recruitment/lobby/Adventure/Battle owner hooks; minimap color sharing and lower route snapshots | Retain lower-screen journeys through upstream APIs. Shared read-only helpers can be justified; new gameplay rules cannot. Window activation/deactivation remains upstream-owned. |
| Request acknowledgement adapters | `CCallback::*Request`, `CExchangeController`, artifact/army pending-request tracking | Directly supports lower input suppression and acceptance feedback, but modifies shared APIs. Reuse upstream request IDs/acknowledgements where possible; otherwise keep only minimal additive adapters with unchanged upstream operation semantics. |
| Multi-slot redistribution (Slice 25) | `RedistributeArmyStack`, `ArmyStackRedistribution.h`, `CCallback::redistributeStack`, server handler and registration 270 | A lower feature with a custom shared gameplay transaction/wire extension. It fails the target upstream-only engine boundary. Must map to a suitable upstream transaction or explicitly resolve the feature limitation before removal; independent requests are not automatically equivalent to one atomic transaction. |
| General staged animation fix (Slice 51) | `BattleAnimationWait.h`, `BattleInterface::waitForAnimations`, commit `49d4dd6da` | General client behavior, shared by all builds; receive through upstream base. Do not retain a second implementation alongside upstream's equivalent. |
| Army authorization/integrity (Slice 52) | `ArmyTransferValidation.h`, `CGameHandler`, `NetPacksServer`, packet application, commit `88aa4f412` | General server rules plus fork transaction integration. Use upstream's rules after reconciliation; preserve rejection/acknowledgement and required-last-stack behavior. Do not blindly revert to older unsafe handlers. |
| Native battle controller navigation (Slice 53) | `BattleControllerInput.h`, BattleActions/Field/Window and input routing, default bindings, commit `27c3a84c6` and follow-ups | Primary-screen/controller functionality is outside second-screen scope even though useful on Thor. Follow chosen upstream behavior; retain lower Battle controls independently. Any changed physical bindings must be documented/tested during reconciliation. |
| Strict/optional callbacks (Slice 54) | `CGameInfoCallback`, AI/editor/Lua/pathfinder consumers, commit `e2a93b699` | General callback API migration. Use upstream's API as delivered; adapt only Thor consumers. Keep stale-ID and fog/ownership checks at the Thor boundary without maintaining a separate engine-wide migration. |
| Query isolation, answers and activities (Slice 55 and prerequisites) | `server/activities/`, removed `server/queries/`, visits/rewards/turn/battle routines, client/AI/Lua/spell consumers, `QuestionID`, serialization; commit `09d279e14` and fixes | Largest general engine adaptation; receive the complete subsystem from the chosen upstream base. Reapply only necessary lower-screen acknowledgements and owner adapters. Do not revert individual runtime files or treat adaptation as an upstream-history merge. |
| Packaging, tests and candidate companion | Thor flavor/package, workflow, focused tests, `tools/vcmi-builds/` | Retain support needed to build/test/install the second-screen product. Keep upstream toolchain updates in an explicit base update; companion is development support, not an engine feature. |

The full Slice 55 merge alone changes 151 paths, with 6,669 additions and 3,760
removals against its first parent `06ff1c27c`. This includes tests/documentation;
it is evidence of a broad subsystem migration, not a measure of required Thor code.
The existing architecture audit remains a historical complexity analysis; this
audit answers the different question of whether each family belongs in the fork.

## Concrete compatibility blockers

1. **Packet registration collision.** Product `RegisterTypes.h` assigns type 270 to
   `RedistributeArmyStack`; fetched upstream assigns 270 to `SimturnsContactStatus`
   (and uses 271/272 for other types). Retaining the custom registration on an
   updated base would collide. Removing or renumbering it without a migration can
   also affect old replay/network decoding. Do not claim mixed-version multiplayer
   or replay compatibility from a clean compile. Prefer eliminating the custom wire
   extension through upstream operations; any unsupported legacy replay case must
   be identified and explicitly resolved before promotion.
2. **Serialization version divergence.** Product appends
   `CREATURE_REFUSED_JOINING_REMOVED` directly after `RECORD_TEXTS_METASTRING`;
   upstream inserts five other version entries before that feature. The enum uses
   implicit incrementing values, so matching names have different numeric versions.
   Product also retains the sequential save format while current upstream includes
   `SAVE_CHUNKS`. Copying the upstream enum or loader is not proof that existing
   Thor saves will load correctly. Preserve real existing saves and test their
   interpretation; do not overwrite them or silently classify them by a mismatched
   version number. Any conversion/legacy support decision must be explicit and
   bounded rather than becoming a new independently maintained save engine.
3. **Runtime/API coupling.** Activities, questions, reward JSON, battle results,
   teleport movement and callback consumers were migrated together. Current
   upstream contains the corresponding broader evolution. Whole-base alignment
   must replace these adaptations coherently and reconnect Thor owners. A blanket
   revert of Slices 51–55 would also discard tested safety fixes and can break
   lower-screen actions that now depend on these APIs.

These are source-confirmed update hazards, not claims that the currently validated
APK is broken. Existing hardware acceptance applies to its exact product tree.

## Recommended reconciliation

The next maintenance task should align an explicit upstream base with a minimal
second-screen overlay. It is separate from a new Thor feature. The fetched develop
SHA above is an assessment reference, not an automatically approved release base.

1. Select and pin the intended upstream release/commit in that task. Preserve the
   current validated product and candidate refs. Prepare a normal review branch;
   do not rewrite product history or perform an in-place destructive reset.
2. Review a three-way integration against that upstream base. Let upstream own
   generic engine/server/AI/callback/controller/serializer code. Reconcile Thor
   publication and requests through current upstream owners. Inventory each retained
   shared hunk with its concrete lower-screen dependency and update cost; do not
   rely on successful automatic merging as evidence of necessity or correctness.
3. Resolve the custom redistribution dependency and packet collision as one complete
   boundary. Preserve equivalent atomic semantics where upstream supports them;
   surface any capability change for a concrete product decision before implementing
   a loss of existing lower-screen functionality.
4. Resolve existing Thor saves/replays against upstream versioning using preserved
   representative files. Test old/current saves, campaigns, browser metadata,
   autosaves and multiplayer/replay expectations. Report unavailable fixtures and
   incompatible cases; never infer compatibility from version names or header load.
5. Keep existing Thor tests and adapt assertions to upstream owners. Run focused
   contracts plus integrated client/server/AI and non-Thor build checks, independent
   diff review and the permanent candidate gate. Reuse the established caches and
   signing path. Do not reduce regression gates merely because a fix is upstream.
6. Package one comprehensive device checklist in `CI/thor-validation-plan.json`:
   all existing lower decks, movement/services/army/artifacts, context restoration,
   stale/rapid input, reconnect/toggle/resume, upper controller/touch behavior and
   save/load. Promote only after exact-candidate acceptance and explicit merge
   authorization. Record resulting overlay inventory and upstream base together.

Completion means generic subsystems follow the selected upstream implementation,
remaining shared changes have narrow second-screen justifications, identified
packet/save compatibility issues are resolved, and existing Thor journeys pass
the required CI/device gates. No standalone chunked-save or other engine feature
should be selected to accomplish this piecemeal.

## Validation of this audit

Reproducible source checks: `git merge-base`, `git rev-list --left-right --count`,
baseline-to-tip `git diff --shortstat`/`--name-only`, subsystem history and direct
product/upstream file reads. File-set overlap counts are distinct path intersections.
Documentation review and `git diff --check` are the applicable gates. No native,
Gradle, ARM64 build or device cycle is required for this documentation-only change.

Completeness checkpoint: scope rule, subsystem classification, concrete compatibility
hazards, reconciliation acceptance and current roadmap are covered. A separate
critical documentation review checked the proposed dispositions against current
code and upstream declarations, distinguished historical receipts from current
instructions, and corrected stale Slice 55 current-state/prerequisite wording.
The review does not establish a line-by-line minimal overlay or compatibility pass;
those are explicit deliverables of the reconciliation task.
