# Upstream base and second-screen overlay

## Integration contract — 2026-10-25

Use upstream `46f2e15ec69e0507e6d0195d6357df5a2813dde1` as the engine base.
The user approved removing custom multi-slot redistribution and isolating new saves
on 2026-10-25 to simplify future upstream updates. No old save conversion is attempted.

A direct three-way merge retains the fork's general engine adaptations and conflicts
across AI, activities, callbacks, serialization and input. This implementation starts
from the exact upstream tree and ports the lower-screen overlay from the validated
pre-engine-migration product `b4b7138c382956239c5b0633cb431e72df2b6a5a`, then adapts
its client-side hooks to upstream's current APIs. The initial port had eight conflicted
owners; automatically merged owners also require source review and compilation.
The review branch will record both the old product and pinned upstream as parents,
preserving validated history and making this upstream commit a real merge ancestor.
It does not reset product history; the permanent candidate ref advances only after the reviewed implementation is published.

## Retained integration

| Area | Overlay responsibility |
| --- | --- |
| `android/vcmi-app/Thor*.java`, resources and activity hooks | Lower presentation, public display discovery, rendering, local UI state, touch cancellation and system-respecting haptics |
| `lib/thor/`, `CAndroidVMHelper`, `clientapp/EntryPoint` | Bounded revision-bound data and JNI transport; no gameplay packets or Java game-state mutation |
| `GameEngine`, window/lobby/Adventure/Battle adapters | MainGUI dispatch, exact active owner, current visibility/identity/availability and ordinary native routes |
| `client/thor/ThorArmyController` | Lower-only slot validation and calls to upstream callbacks/controllers; bulk behavior delegates to the unchanged `CExchangeController` |
| `client/thor/ThorRequestTracker`, `Client::sendRequest` | Observe the synchronous upstream request ID through a scoped thread-local capture; retain callback signatures, packet construction, request delivery and server rules |
| Artifact/window adapters | Pending-action suppression and acknowledgements across modal/lifecycle transitions; upstream exchange initiator remains the closure owner |
| Android `VCMIDirs::userSavePath` override | Mount `vcmi-data/Saves-upstream` as logical `SAVES/` only for Thor; keep the browser on this dedicated loader through unrestricted root refresh; leave serializers upstream-owned |
| Build, CI, tests and candidate companion | Thor package/preset, focused contracts, upstream native regressions, exact candidate receipts and installation support |

The pinned base and allowed shared-library seams are executable policy in
[`CI/thor_upstream_base.json`](../CI/thor_upstream_base.json). The validator rejects
tracked or untracked changes to protected engine/server/AI/packet/save/rules/input
paths outside these seams. An allowance documents integration cost; it is not
permission to add gameplay rules within an allowed file. Changes to that policy
require review with their actual diff. The lower owner hooks still need human review;
the policy does not prove their behavior equivalent to upstream.

## Intentional compatibility changes

- Manual saves, autosaves, campaigns and replay data under the save mount use the
  upstream implementation and new physical `Saves-upstream` location. Existing
  `vcmi-data/Saves` is untouched and absent from the new save browser. Assets, maps,
  mods and application ID `is.xyz.vcmi.thor` remain in the same app data namespace.
  Keep old saves for the old APK; do not manually copy them into the new directory.
  In-place APK installation preserves files; uninstalling the app may delete them.
- The lower custom allocation editor and its JNI/native queue are removed. Action
  ID 22 is reserved, decodes as invalid and has no enabled mask or haptic response;
  later IDs are unchanged. Standard lower single-stack move/merge/swap/split and
  upstream whole-army/artifact operations remain. There is no custom army packet,
  registration 270, server handler or separately maintained transfer rule.
- Primary-screen and controller behavior follows this upstream base. Fork-specific
  native battlefield navigation/default bindings from Slice 53 are not carried.
  The lower Battle deck retains its own controls, using upstream actions and dialogs.
- The callback/activity/army/animation/save changes included in upstream are obtained
  as part of its complete base, with no independent adaptation of Slices 51–55.
  Stock upstream behavior may differ from the previous APK. No mixed-version or
  old-fork save/replay compatibility is claimed.

## Validation and maintenance

Run `python3 CI/thor_upstream_base.py`, the scope/checklist Python tests, focused
native/JVM contracts and the normal Thor pipeline. Preflight now builds and runs
upstream's `vcmitest` plus the registered Thor contracts; no fork-specific server
test target is retained. Use upstream's 2026-10-13 dependency archive with the
existing bounded archive/Gradle/compiler caches and clean generated build trees.
ARM64 still uses the permanent candidate ref, Linux/JDK 17, signing and package gates.

The current complete device checklist is
[`CI/thor-validation-plan.json`](../CI/thor-validation-plan.json). New saves and
ordinary gameplay, every lower deck, stale input, display/modal restoration and
upstream primary input must pass on the exact candidate. Host syntax checks and JVM
tests are implementation evidence; native runtime, packaging and device behavior
are separate gates. Preserve the old APK and files until new-candidate acceptance.

For a later base update, fetch and pin upstream, merge it normally on a review
branch, update the policy's base SHA and reconcile the retained hooks against actual
upstream owners. Compare protected code to the new base, close review findings,
then use the existing candidate/device/promotion gates. Do not cherry-pick a growing
list of general engine fixes or replace upstream handlers with older fork copies.
