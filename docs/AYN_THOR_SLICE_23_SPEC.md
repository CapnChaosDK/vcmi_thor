# AYN Thor Slice 23 — Hero Meeting artifact deck foundation

Status: implementation specification.

## Goal

Add a lower-screen **Artifacts** mode to the existing Hero Meeting deck while preserving the hardware-validated Slice 20–22 Army mode unchanged. This slice establishes a bounded, revision-scoped, read-only artifact inventory and local Army/Artifacts mode switching. Artifact mutation is deliberately deferred to the next slice so stable slot identity, backpack visibility, lifecycle, and layout can be validated first.

## User-visible behavior

- In `HERO_MEETING`, the lower display gains local-only **Army** and **Artifacts** tabs/modes.
- Army remains the default and must preserve all Slice 20–22 behavior exactly: quick transfer, exact drag/drop, whole-army controls, and long-press split.
- Artifacts shows both heroes by name and a bounded snapshot of each hero's currently visible artifact inventory.
- Show equipped slots and the currently visible backpack window in stable native order. Each occupied entry shows a translated artifact name and whether the slot is locked. Spell scrolls must remain distinguishable by their translated artifact/scroll identity if the existing native artifact API exposes it without new global lookup.
- Empty positions are represented as empty slots; locked positions are visibly marked and inert.
- Switching Army/Artifacts is Android-local only and never submits gameplay input.
- Leaving Hero Meeting, replacing the presentation, display detach/toggle, pause/resume, or context revision replacement must cancel local selection/gesture/editor state and restore a valid mode without stale hit regions.

## Native boundary

- `CExchangeWindow` remains the sole source of truth. Build the artifact snapshot from the exact two heroes/artifact widgets already owned by the active exchange window; do not perform global hero lookup.
- Publish only bounded plain data: stable hero IDs/names, artifact-position identity, translated display name, occupied/locked flags, and enough backpack-window identity to distinguish the currently visible backpack positions.
- The snapshot is read-only in Slice 23. Do not add artifact action IDs and do not change action IDs 0–20.
- Include artifact state in semantic revision equality so changes to equipped artifacts, visible backpack contents/scroll position, hero replacement, or lock state advance the Thor revision once; unchanged frames do not churn.
- Invalid/oversized/incomplete artifact payloads fail closed and are cleared outside `HERO_MEETING`.
- `updateArtifacts()` / the existing exchange refresh path must republish while the exact `CExchangeWindow` is active. Do not alter normal VCMI artifact/window lifecycle.
- Preserve Qt-macro-safe native naming and existing Thor compile guards.

## Android/JNI boundary

- Add a dedicated immutable Hero Meeting artifact snapshot model; do not overload the army arrays.
- Carry the snapshot through `CAndroidVMHelper` -> `NativeMethods` -> `VcmiSDLActivity` -> `ThorSecondScreenController` -> `ThorSecondScreenPresentation`.
- Controller caching must restore the latest artifact snapshot when the lower presentation is recreated.
- Artifacts mode must have no gameplay hitboxes in this slice except the Android-local Army/Artifacts mode selector and any local-only paging needed to fit the bounded snapshot.
- Switching back to Army must restore the existing army touch regions and gestures exactly.
- All local artifact-mode state must be revision/context scoped and cleared on lifecycle/display changes.

## Scope constraints

- No artifact transfer/swap action.
- No artifact drag/drop.
- No whole-inventory move/swap buttons on the lower display yet.
- No artifact assembly/disassembly, equip/unequip, backpack mutation, spell casting, popup, or description window.
- No proprietary artwork transport. Text/generic shapes only.
- Do not change upper-screen mouse/touch/controller/keyboard behavior.
- Do not alter non-Thor or single-display behavior.

## Automated acceptance

Add focused native and JVM tests covering at least:

1. snapshot completeness/bounds and stable slot identity;
2. equipped and visible-backpack occupied/empty/locked state;
3. translated/bounded artifact names;
4. semantic no-churn and one-revision refresh on artifact/backpack change;
5. clearing outside Hero Meeting and malformed payload rejection;
6. JNI/model array-shape validation;
7. controller cache/recreation behavior;
8. local Army/Artifacts mode retention within one valid Hero Meeting revision and reset on context/lifecycle replacement;
9. existing Slice 20–22 action IDs/masks and Army gesture tests remain unchanged and green.

Register new focused JVM tests in the existing Thor CI invocation. Do not create another workflow.

## Hardware checklist

1. Open Hero Meeting and confirm Army mode is unchanged; retest quick tap, exact drag, long-press split, and whole-army controls.
2. Switch to Artifacts and verify both hero names plus equipped and visible backpack artifact names against the upper screen.
3. Verify empty and locked positions are represented correctly and that touching artifact rows performs no gameplay mutation.
4. Scroll/change the upper backpack where practical and verify the lower snapshot refreshes without stale entries or revision churn.
5. Equip/unequip/move an artifact using the normal upper UI and verify the lower read-only view refreshes.
6. Switch repeatedly between Army and Artifacts; verify no stale army highlight/split editor, no artifact hitbox leakage, and no duplicate native commands.
7. Open/close Hero/Quest/other children, toggle the lower display, and background/resume; verify correct parent restoration and no stale inventory.
8. Recheck upper artifact interaction, keyboard/controller/mouse/touch behavior and Adventure/Battle decks.
9. Watch for crashes, stale names, wrong hero inventory, duplicate presentation, focus theft, or update loops.

## Delivery workflow

Read and follow `AGENTS.md`, `AYN_THOR_BACKLOG.md`, and `docs/AYN_THOR_BUILD_PLAYBOOK.md`. Inspect existing artifact ownership/API before coding.

Implement the complete slice on this existing PR branch. Add/update the maintained backlog with the final Slice 23 contract and candidate hardware checklist. Run practical cheap/static/focused validation in Codex Cloud, including `git diff --check`, and review the complete diff.

Do **not** run the full Android ARM64 package build in Codex Cloud. After the implementation commit is pushed to this PR branch, update the permanent `ci/thor-candidate-validation` transport branch to the exact PR HEAD according to the playbook so the authoritative Thor CI runs. Do not create a per-slice candidate branch or another PR.

Report the implementation commit, changed files, focused validation, candidate branch SHA, and Thor CI run URL/status if available.
