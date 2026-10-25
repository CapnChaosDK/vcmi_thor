# Autonomous Development Guide

This file is the repository-level operating manual for coding agents. It governs the
whole tree unless a more deeply nested `AGENTS.md` applies. Follow direct user and
system instructions first, then the nearest applicable `AGENTS.md`.

## Repository purpose and Thor mission

VCMI is a C++20, client/server reimplementation of Heroes of Might and Magic III.
This fork's project-specific focus is safe AYN Thor dual-screen Android support. The
product line is developed on `ayn-thor-dual-screen`; a checkout with another local
branch name may still represent that state, so verify the commit/tree rather than
renaming branches to imitate it. Preserve upstream VCMI behavior, architecture,
licensing, and single-display/platform compatibility. Keep Thor code behind the
existing `TARGET_AYN_THOR`/Android boundaries where appropriate, small enough to
maintain during upstream updates, and free of proprietary Heroes III assets.

### Product scope decision — 2026-10-25

The fork exists for second-screen additions. Engine, server, saves, AI, ordinary
gameplay and primary-screen/controller behavior must follow the chosen upstream
base. Do not select general upstream fixes, API migrations, save formats or
toolchain modernization as Thor feature slices, or maintain selective semantic
adaptations of them. Receive those changes through explicit upstream-base updates.
Earlier roadmap priorities and completed slice contracts do not authorize expanding
this scope. Preserve their validation history as history.

Keep fork additions in the Android presentation, bounded JNI/plain-data transport,
MainGUI owner adapters and necessary Thor packaging/lifecycle support. Reuse upstream
callbacks and rules. Any shared-code change must be directly necessary for an
identified lower-screen capability, minimal, and documented with its upstream-update
cost; prefer an adapter or an existing upstream operation. Do not introduce custom
gameplay rules or wire/save formats to complete a lower-screen feature. If upstream
cannot support a capability without those changes, report the concrete limitation
instead of broadening the engine. Existing shared extensions require reconciliation,
not automatic retention or blind removal; see
[`docs/AYN_THOR_FORK_SCOPE_AUDIT.md`](docs/AYN_THOR_FORK_SCOPE_AUDIT.md).

An upstream-base update is a distinct maintenance task, not an autonomous next
feature. Preserve product history and user data, review the Thor-only overlay and
save/network compatibility, and run the existing CI/device gates before promotion.

## Sources of truth

Use these sources by responsibility:

| Responsibility | Authority |
| --- | --- |
| Autonomous workflow, selection, gates, and completion | This `AGENTS.md` |
| Current status, remaining roadmap, slice contracts, validation history | [`AYN_THOR_BACKLOG.md`](AYN_THOR_BACKLOG.md) |
| Established Thor architecture and integration points | [`docs/AYN_THOR_DISCOVERY.md`](docs/AYN_THOR_DISCOVERY.md), then current code |
| Candidate build, CI, cache, artifact, promotion, and device procedure | [`docs/AYN_THOR_BUILD_PLAYBOOK.md`](docs/AYN_THOR_BUILD_PLAYBOOK.md) |
| Executable CI behavior | [`.github/workflows/thor-ci.yml`](.github/workflows/thor-ci.yml) |
| Focused automated coverage | `test/thor/`, `android/vcmi-app/test/`, and nearby component tests |
| General VCMI architecture/style/build rules | `docs/developers/`, especially `Coding_Guidelines.md`, `Code_Structure.md`, `Building_Android.md`, and the topic-specific document |
| Completed work | Git history plus the candidate/device receipts recorded in the backlog and playbook |

Before Thor work, read this file, the backlog's current state/roadmap/latest slice,
the playbook, and the relevant discovery sections. Start with task-relevant sections;
expand to complete detailed documents when selecting an unspecified slice, changing
architecture, preparing a candidate, or updating historical status requires it.
Avoid rereading unchanged history or re-investigating established architecture
without a concrete reason.

If sources disagree, do not silently choose convenient text. Check current code,
tests, workflow behavior, and recent history; prefer the newest explicit repository
decision and executable behavior. Preserve historical records as historical. Correct
stale current-state documentation in the same change, or report the unresolved
conflict and stop when it prevents a safe decision. This file controls process, but
does not override an explicit product contract recorded for a slice.

## Interpreting “Implement the next feature”

Unless the user identifies a slice, perform this decision procedure and normally
continue without asking them to choose among reasonable options:

1. Inspect the backlog, current-state hand-off, roadmap, latest slice records, and
   relevant Thor documents.
2. Establish what is already implemented and validated from Git history, tests, and
   the current tree. Inspect code before trusting status wording.
3. Identify incomplete prerequisites, dependencies, adjacent TODOs, and any partially
   implemented behavior. Never reimplement completed work.
4. Choose the next logical user-facing capability or coherent vertical slice. Follow
   explicit ordering/dependencies; otherwise choose the highest-priority ready item
   in the maintained roadmap.
5. Include all layers needed for the capability to work safely—native/UI ownership,
   bounded JNI transport, Android presentation, lifecycle handling, tests, and docs
   as applicable.
6. Prefer enough substance to justify a full candidate build and focused hardware
   test. Do not combine unrelated roadmap items merely to make a larger change.
7. Before editing, state the selected slice and a brief rationale/scope. This is a
   notification, not an approval request.

Ask the user only for a genuine product decision, destructive choice, materially
ambiguous behavior, unavailable required dependency/access, security concern, or a
conflict that repository evidence cannot resolve. Historical labels such as
“proposed” or “approved” document earlier development; they do not by themselves
require a new approval round for an otherwise unambiguous next slice.

## Slice sizing

Each implementation slice should normally deliver one significant, end-to-end
user-visible capability or one complete, coherent technical/safety boundary. Apply
this default when selecting the next feature, executing an existing feature prompt,
scoping maintenance or cleanup, planning validation, and recommending follow-up work.
Include the required supporting refactoring, focused automated coverage,
resulting-state documentation, and applicable candidate/hardware readiness. Substance
comes from completing a coherent capability, not from diff size.

Keep closely related buttons/menu actions, native/Android/JNI integration,
validation/error handling, required cleanup, tests, and documentation in the same
slice. For example, a Town service includes all actions, states, navigation,
confirmation, error handling, UI integration, and focused tests needed to complete
that service; an entry point alone is not a complete service.

Avoid artificial one-file or micro-slices, speculative future architecture,
unrelated cleanup, and enormous multi-feature rewrites. A larger slice is appropriate
when tightly coupled components have one contract and are safer to implement and test
together. Separate independent capabilities when their contracts and validation
justify it; distinct failure cases within one capability do not by themselves
justify micro-slices. If a genuine blocker forces separation, explain the dependency
and identify a smaller but still complete capability.

## Usage-efficient development

This is a personal project with a limited Codex usage allowance. Treat model
reasoning, tool calls, cloud execution, and delegated-agent work as costs.
Optimize for the lowest reasonable total usage that delivers a *complete*
feature without weakening quality, CI, safety, or hardware-validation gates.

- Keep slices feature-complete; reduce cost by reusing established owners,
  contracts, tests, and UI components rather than omitting necessary behavior.
  Group required implementation and cleanup in one coherent PR where practical.
- Use targeted discovery and a concise implementation plan. Expand investigation
  only for missing evidence or concrete risks; do not repeatedly read unrelated
  history or rediscover architecture already documented and verified.
- Prefer one primary agent. Launch additional subagents only for narrowly scoped,
  genuinely independent work whose benefit justifies duplicated context and cost.
  Never automatically create parallel architecture, Android, protocol, and review
  agents for each slice.
- Retain an independent, critical review of the finished diff. Prefer one separate
  review pass by the primary agent; use a delegated reviewer when material risk
  warrants it. Repeat review and tests when findings require remediation.
- Follow the validation ladder: focused checks during implementation, consolidated
  regressions before publication, and one reviewed, integrated candidate per slice
  where possible. Failed candidates still require correction and rebuilding;
  a one-build preference never overrides required gates or cache safety.
- Avoid speculative abstractions, unrelated refactoring, redundant documentation,
  and repeated tests that add no new evidence. Never hide a known failure to save
  time or usage.
- If a task's likely resource cost is unusually high, explain the tradeoff early
  and propose a smaller *complete* capability or an efficient validation sequence;
  never silently deliver a half-finished feature.
- When recommending a model, suggest GPT-6.1 Sol with Medium reasoning as the
  usual starting point for a well-specified substantial slice. Use High effort only
  for difficult architecture, concurrency, or debugging, and consider GPT-6 Astra
  for exceptional problems. These are preferences, not assumptions about available
  models, automated model switching, pricing, or enforceable usage caps.

## Mandatory Thor workflow

### Phase A — Discovery

Before editing:

- Read the sources required above and any nested `AGENTS.md` for files in scope.
- Check `git status`, current branch/HEAD, remotes, recent Thor history, and existing
  user changes. Never discard or overwrite unrelated work.
- Inspect the implementation, callers/owners, nearby tests, build registration, and
  relevant upstream-compatible path.
- Trace dependencies and state/thread/lifecycle ownership; determine whether some or
  all requested behavior already exists.
- Do not design from documentation alone.

### Phase B — Design

Establish internally: user-visible behavior; scope and non-goals; acceptance criteria;
affected components; state/data flow and ownership; Android/JNI/threading and activity
lifecycle; display attach/detach/recreation; controller/touch/upper-screen input;
failure, stale-request, overflow, and non-Thor fallback behavior; upstream
compatibility; test strategy; and hardware checks.

Before coding, review the complete user journey or technical boundary against the
current implementation. Identify closely related missing behavior, all required
integration work, and reusable components. Make acceptance criteria end-to-end and
include expected failures and regression risks.

For significant work, publish a concise implementation plan for the whole capability
before changes and keep it current. Anticipate likely omissions and follow-up work
so predictable parts are included initially. Do not wait for plan approval unless
a stop condition applies.

Thor invariants include:

- The native engine and server remain authoritative. Android input is untrusted,
  revision-bound plain data; consume it on the existing `MainGUI` path, re-resolve
  stable identities, validate current owner/context/bounds/availability, then use
  existing UI/controller or callback routes. Never mutate game state from Java.
- Keep Android control IDs distinct from payload row indexes. Bound row checks by
  the explicit row-control range, not by the browser's maximum row capacity; footer
  controls such as Load and Back must use their own action availability. Pin this
  boundary in focused tests.
- The lower `Presentation` supplements the SDL display and must not steal or replace
  upper-screen touch, keyboard, controller, rendering, or focus paths.
- Discover displays through public Android APIs; never hard-code transient display
  IDs or depend only on a model string. Clear pending state and hit regions across
  context, pointer, pause/resume, toggle, detach, and recreation transitions.
- Preserve the normal VCMI window stack and owner activation/deactivation sequence.
  Thor publication is observational and must not bypass lifecycle cleanup.
- Publish only bounded, currently visible player information. Fail closed for unknown
  contexts, invalid/oversized payloads, stale revisions, or unsupported devices.
- Keep `is.xyz.vcmi.thor` data separate and decode proprietary visuals only from
  player-installed data with a generic/text fallback.

### Phase C — Implementation

Implement the complete coherent slice using the existing architecture and minimal,
targeted changes. Follow project conventions, preserve upstream behavior outside the
Thor requirement, and do not create parallel abstractions where an established owner
or semantic route exists. Do not knowingly leave incomplete production paths. Add or
adjust focused tests at the contract boundary and keep the diff reviewable. Avoid
drive-by formatting, generated output, secrets, binaries, and unrelated cleanup.

### Phase D — Independent self-review

Before candidate publication or declaring implementation complete, explicitly record
a feature-completeness checkpoint against the planned scope:

- the intended journey or technical boundary is complete end-to-end, with all
  closely related actions and enabled, disabled, pending, and error states;
- navigation, cancellation, confirmation, context/revision changes, and lifecycle
  transitions are handled, with native ownership and server authority preserved;
- existing related behavior is preserved and necessary cleanup is incorporated;
- tests cover complete behavior rather than only helpers, and documentation and
  applicable hardware-validation instructions are ready.

Close important gaps within the same slice; do not turn predictable omissions into
the next micro-slice. Distinguish implementation evidence from CI/device behavior
still awaiting validation; the checkpoint does not establish an unobserved pass.

After implementation and focused tests, perform one separate, critical review of
the entire diff as a release gate (a separate self-review pass is sufficient). Check:

- functional correctness, acceptance criteria, edge cases, and regressions;
- architecture/thread ownership, bounded transport, revalidation, and duplicated logic;
- window/activity/display lifecycle and state restoration;
- dual-screen layout, focus, touch/gesture cancellation, and controller behavior;
- stale revisions, double input, display changes, and non-Thor/single-display fallback;
- tests that assert behavior rather than only implementation details;
- unnecessary complexity, dead code, missing docs, and unrelated files.

Fix meaningful findings, rerun affected checks, and repeat review as necessary. A
summary of the initial diff is not an independent review.

## Delegation

Delegation is optional, not the default. Use one primary agent for discovery,
design, implementation, focused tests, and remediation. Conduct a separate,
critical review of the completed diff before delivery; a well-separated self-review
is acceptable. Delegate a focused independent architecture or release review when
complexity or failure risk justifies its additional usage, rather than automatically
spawning multiple investigators and reviewers.

When delegation clearly helps, give agents distinct, bounded scopes, avoid repeated
repository discovery and duplicated context, and prefer sequential handoffs. Do not
let agents concurrently edit shared mutable files unless isolated worktrees and
deliberate reconciliation are available. Never assume a named model will exist.

## Validation ladder

During implementation, reuse established test infrastructure and run the cheapest
meaningful checks after relevant changes, not the entire suite after every edit:

1. Syntax/resource/contract/static checks relevant to changed files.
2. Focused native tests in `test/thor/`, focused Android JVM tests in
   `android/vcmi-app/test/`, and nearby component tests.
3. Relevant project-level configuration/build validation when the environment makes
   it practical.
4. `git diff --check`.
5. Diff stat, changed-file inventory, and complete diff review.

Before candidate publication, complete the Phase D checkpoint, close known gaps,
perform the separate full-diff review, and run the appropriate consolidated regression
checks. Reuse valid earlier results; rerun affected checks after fixes or when new
evidence warrants it. Reserve full ARM64 builds for reviewed, integrated candidates,
not small intermediate changes that focused checks can validate. The CI policy's
failure/correction/rebuild gates remain mandatory.

For documentation-only changes, check scope, consistency, duplication, and the full
diff; do not launch native/Gradle builds, ARM64 candidates, or hardware cycles.

Use existing build directories and safe dependency/compiler/Gradle caches when
compatible. Do not casually clear caches, rebuild dependencies, cache generated
CMake/Qt/Gradle/APK trees, or weaken tests to accommodate the environment.

Local or Codex Cloud validation stops after practical focused checks. Do not attempt
to reproduce the complete Android ARM64 package when the documented Linux/JDK 17
toolchain, dependency bundle, signing secret, or remote access is unavailable. The
authoritative full package gate is the existing Thor workflow; state exactly which
checks were not run and the next candidate action.

## CI and candidate policy

`.github/workflows/thor-ci.yml` is the sole permanent Thor workflow:

- Relevant pushes to `ayn-thor-dual-screen` and pull requests targeting it run the
  no-secret preflight; Markdown-only changes are path-ignored.
- A trusted push to `ci/thor-candidate-validation` runs preflight and then the full
  Ubuntu 24.04/JDK 17 ARM64 candidate job. A manual full dispatch is valid only from
  that ref. Pull requests and arbitrary branches cannot use signing secrets.
- Reuse this one transport branch. Never create per-slice candidate branches or
  workflows. Prefer fast-forward; only the guarded, verified `--force-with-lease`
  procedure in the playbook may reset this disposable branch to an exact candidate.
- A normal implementation branch or Codex-created `codex/...` branch/PR is the
  review and delivery branch; it does not replace the permanent candidate handoff.
  Once a coherent implementation commit has passed focused validation and independent
  review and has been successfully published to the fork, advance
  `ci/thor-candidate-validation` to that exact published SHA when authenticated
  remote write access is available. Fetch and verify the current candidate ref first,
  use a normal fast-forward whenever possible, and use only the playbook's guarded
  `--force-with-lease` procedure when a reset is genuinely required.
- The candidate-branch push is part of normal feature completion when remote access
  permits it and must trigger the existing Thor workflow. Do not wait for the full
  ARM64 run to finish; report the exact candidate SHA and GitHub Actions run/link and
  stop at `awaiting CI` or `awaiting hardware validation` as appropriate. If remote
  write access is unavailable, report the exact published/local implementation SHA
  and the precise candidate handoff that remains; do not silently stop after opening
  an implementation PR.
- Preserve the Conan archive cache, supported Gradle cache, and same-ref 3 GB
  `ccache`. Each run must create clean generated build/output trees.
- Success means preflight and ARM64 jobs pass, focused tests pass, package ID is
  `is.xyz.vcmi.thor`, and the seven-day artifact contains the APK, SHA-256, validation
  receipt, and ccache statistics for the exact candidate SHA.
- After failure, diagnose the first real failure, fix the implementation/workflow,
  rerun focused checks, and submit a new exact candidate. Do not bypass a gate or
  describe failed/partial CI as successful. Escalate an infrastructure failure that
  cannot be resolved safely.

Before using remote operations, inspect `git remote -v`, fetch, and confirm the real
fork/ref and a clean intended tree. A cloud snapshot may use a detached or `work`
branch and have no remotes. In that case, finish local changes and checks, provide the
exact next CI command/action, and do not fabricate a branch, remote, run, URL, or
status. Never add an arbitrary remote merely to satisfy expected naming. Never expose
or reproduce signing credentials outside GitHub Actions, push upstream, or rewrite
validated product history.

## Hardware gate

Passing CI proves build/package/contracts, not physical dual-screen behavior. Any
change to presentation UI, display selection, lifecycle, focus, touch/gesture,
controller input, live game-state publication/actions, window transitions,
performance, or player-installed visuals requires a checksum-verified APK test on a
real AYN Thor.

For such slices, prepare one comprehensive but concise checklist for the complete
capability and relevant regressions. Consolidate closely related behavior into one
candidate/device cycle where possible, avoiding consecutive APK installations for
predictable fragments of the same feature. Cover happy and failure paths plus
display toggle/reconnect, background/resume, context/modal
restoration, stale/rapid input, and upper touch/controller regressions as applicable.
Report `awaiting hardware validation` until the user confirms the checklist for the
exact candidate SHA and checksum. Never infer, fabricate, or mark a device pass from
CI, emulator, source review, or an older APK. Promote only the exact hardware-tested
product/build tree as described in the playbook.

## Documentation policy

Documentation is part of a slice:

- Maintain `CI/thor-validation-plan.json` with the current candidate's feature title,
  change summary, setup, explicit test steps and expected outcomes before publication.
  This is the single structured checklist source consumed by the Thor companion;
  link it from the current backlog contract instead of maintaining a second new checklist.
  Retain historical checklist records. CI validates its schema and binds the packaged
  copy to the exact candidate commit and APK checksum. See
  `docs/THOR_CANDIDATE_COMPANION.md` for the contract and result handoff.

- Before/during implementation, add or refine the backlog contract when needed:
  behavior, boundary, native/Android responsibilities, acceptance tests, and risks.
- After implementation/review, update its truthful status and resulting behavior.
  Mark CI/device validation only from actual receipts/user confirmation.
- Update discovery/architecture, playbook/build/test guidance, or CI comments only
  when their enduring behavior changed. Correct stale current guidance encountered.
- Record completed slices and exact validation evidence in the backlog's established
  format; retain useful history, but label superseded procedures as historical.
- Link to one detailed authority instead of duplicating instructions across files.
  Describe resulting behavior, not merely that files changed.

## Git hygiene and delivery

Before completion:

1. Inspect `git status --short --branch` and preserve pre-existing changes.
2. Run `git diff --check`.
3. Inspect `git diff --stat`, the changed-file list, and the complete diff (including
   staged changes).
4. Confirm no unrelated, generated, temporary, credential, artifact, or binary files
   were added and tests/docs match the implementation.
5. Follow the active task's commit/PR instructions. A normal development commit may
   precede CI/device validation, but it is not a validated promotion. Keep receipt-only
   documentation commits distinct where useful.
6. For a hardware-relevant Thor feature, if the implementation commit has been
   published and authenticated remote access permits it, complete the permanent
   candidate handoff to that exact SHA before declaring the coding task complete.
   Opening a PR alone is not the candidate handoff.

Do not claim to have committed, pushed, opened a PR, triggered CI, merged, promoted,
or modified a remote unless that action actually succeeded. Do not push or merge
unless instructions and available authenticated remotes permit it. Preserve PR
review and obtain explicit user authorization before merging; feature implementation
or PR creation alone does not authorize a merge. Never rewrite
`ayn-thor-dual-screen`, releases, tags, or validated history. The candidate branch is
the only narrowly resettable ref, under the playbook procedure.

## Stop and escalate only when

- product behavior is materially ambiguous and repository evidence cannot decide it;
- a destructive migration or choice between incompatible architectures has meaningful
  product/user consequences;
- required credentials, device, dependency, repository state, or access is missing
  and no useful local work remains;
- repository state is inconsistent enough that edits would risk user work or history;
- a security/signing concern or broader CI/infrastructure failure cannot be resolved;
- applicable requirements conflict and precedence/evidence does not resolve them.

Routine naming, file placement, test design, bounded implementation choices, and
selection among equally safe technical details do not require user approval.

## Completion contract

A substantial Thor feature is done only when the Phase D feature-completeness
checkpoint passes and the coherent implementation and focused tests are complete;
independent review findings are fixed; relevant documentation is
current; `git diff --check` passes; status/stat/full diff have been inspected; and CI
status is reported or the exact candidate action is supplied. When the implementation
has been published and authenticated remote access is available, "CI status reported"
includes advancing `ci/thor-candidate-validation` to the exact implementation SHA and
reporting the triggered Thor workflow run; do not stop merely because a Codex branch
or PR exists. Hardware-relevant work must include its checklist and honest
`awaiting hardware validation` state until the exact candidate passes on-device.

Keep the final response concise and operational: selected/implemented feature,
important architectural decisions, validation commands/results, CI run/link or next
action, hardware checklist/status, and unresolved issues.

## Short-prompt semantics

- **“Implement the next feature.”** Discover the next logical backlog slice, announce
  it briefly, design, implement, test, independently review, remediate, document,
  publish the normal implementation branch/PR when supported, then hand the exact
  reviewed implementation SHA to `ci/thor-candidate-validation` and report the
  triggered Thor CI run when authenticated remote access permits it. Do not wait for
  the full ARM64 run or hardware test.
- **“Implement slice 21.”** Locate its current specification and status, verify both
  against code/history, then run the same complete workflow without duplicating work.
- **“Review the current Thor implementation.”** Do not choose a new feature. Inspect
  current changes/state independently, run relevant checks, and report or fix findings
  under the review and hygiene gates.
- **“Resume.”** Inspect Git/task/docs/CI state and continue at the first incomplete
  workflow gate; do not restart completed work or assume prior conversation context.

## General VCMI conventions

- Follow [`docs/developers/Coding_Guidelines.md`](docs/developers/Coding_Guidelines.md).
- Use `DLL_LINKAGE` for serialized or cross-library classes/structs.
- Prefer identifiers/constants from `lib/constants/EntityIdentifiers.h`,
  `StringConstants.h`, `NumericConstants.h`, and `Enumerations.h` over literals.
- Use the `lib/serializer/` `h & object` framework for serialized state and preserve
  compatibility where required.
- Only the server changes game state; clients issue validated requests through
  existing callbacks/network packets.
- Use callbacks rather than direct state access. Respect `MainGUI`, `runNetwork`,
  `runServer`, and TBB task ownership.
- Build consumers against the `vcmi` facade, not `vcmiMain`; AI and `vcmiLua` are
  static components aggregated by `libFacade/`.
- Never put `try`/`catch` around imports/includes.
