# Thor command protocol

## Legacy compatibility

`ThorAction` IDs 0–64, `submitThorActionNative(revision, action, target)` and
`publishThorActionState(revision, enabledMask, activeMask)` retain their contracts.
ID 0 has no mask bit; IDs 1–64 use `uint64_t{1} << (id - 1)`.
ID 64 remains Java `Long.MIN_VALUE`; negative masks are valid bit patterns.
Never append action 65 or pass an extended ID to the legacy mask helper.

## Extended V1

The additive JNI request is `submitThorCommandNative(long revision, int[] payload)`.
The payload is **exactly three integers**: `[version, command, target]`.
V1 has version `1`; all current commands require target `-1`.
JNI checks positive revision, non-null/exact array length, version, known command
and target **before** submitting to the existing 16-entry `ThorActionQueue`.
Wrong versions, unknown IDs, extra/missing words, and any other target fail closed.
The return boolean means queued, never native execution or gameplay success.

| V1 extended ID | Meaning | Native route |
| --- | --- | --- |
| 1 | Start auto-combat | `BattleWindow::bAutofightf` |
| 2 | Stop auto-combat | `BattleWindow::bAutofightf`, only when currently stoppable |
| 3 | Cast Spell | `BattleWindow::bSpellf` |
| 4 | Retreat | `BattleWindow::bFleef` |
| 5 | Surrender | `BattleWindow::bSurrenderf` |

These IDs form a separate namespace; they are **not** new legacy mask bits.
Start/Stop are distinct idempotent intents, never a remotely requested toggle.

Availability is `publishThorCommands(revision, version, int[] enabled, int[] active)`.
Each array has at most 16 unique, known IDs; invalid version, null/oversized arrays,
unknown IDs or duplicates yield an empty state. Android copies arrays before UI
posting and applies them only at the exact current Battle revision. Active ID 1
means auto-combat is on; availability of ID 2 independently means it can be stopped.
A disabled Stop label can therefore represent running but non-stoppable auto-combat.
Active IDs need not be enabled. No target collection or arbitrary strings/JSON exist.

`ThorExtendedCommand.h` owns bounded protocol values/decoding. Battle eligibility
is separate in `ThorBattleControls.h`. The internal queue envelope has legacy
`action = NONE`, `commandVersion = 1`, `commandId` and all legacy target/payload
fields at their no-target defaults. Mixed legacy/extended envelopes are rejected.
Version/ID zero defaults preserve all existing aggregate callers.

## Native authority and lifecycle

The normalized context includes the lists in semantic equality; a list, owner,
participant, active-unit, turn/targeting or action-epoch change invalidates the
rendered revision. Non-Battle contexts clear extended availability. Battle keeps
native-only tokens for the exact BattleWindow and current player interface.
Nothing sends pointers to Java.

MainGUI refreshes the top BattleWindow each frame (including AI state changes),
then validates revision, context and availability. It resolves the exact top
window, refreshes/revalidates again, and checks window/participant identity,
activation, tactics, opening, blocked UI, targeting, local side and current native
eligibility before calling the original method. An accepted command consumes the
action epoch and clears queued duplicates across the existing queues. Stop refreshes
immediately even when no child/server callback occurs; a still-existing AI prevents
starting a second AI while the original winds down.

Normal auto-combat can be stopped while the AI blocks UI, including its opponent
turn. Starting and other new actions require an unblocked local active turn.
Configured end-with-auto-combat uses the original upper confirmation and is never
advertised as stoppable. Spell readiness includes the native magic-blocked
explanation path. Retreat/surrender require native eligibility and exclude critical
hero loss; surrender affordability and optional marketplace handling remain entirely
in the original confirmation/callback route.

Spellbook, targeting and confirmation windows retain normal push/pop ordering.
The lower deck disables/clears while they own input and restores from a fresh
native snapshot. No synthetic input, direct battle action or targeting is added.
Android's shared gesture guard requires matching down/up control, pointer, revision
and presentation session. Context/revision changes, resize, multi-touch, cancel,
pause and display replacement clear pending gestures; existing lifecycle queue
clearing also applies to the extended envelope.

Feedback reuses the existing accepted-action delivery/token path. Negative keys
`-command` are **feedback-only** tags and never legacy IDs or mask inputs. Only a
successful native owner dispatch acknowledges a command; queue acceptance never
vibrates. Child-context changes and lifecycle invalidation may suppress feedback,
as they do for existing actions. Retreat/surrender feedback acknowledges opening
the native route, not confirmation or server success.

## Adding another family

Allocate stable IDs in this extended namespace and update both native and Java
known-ID tables. Keep old IDs/targets immutable. Define the family's exact context,
owner and typed bounded target contract, normalization and live MainGUI validation;
add parity, malformed/version/target, stale/duplicate and lifecycle tests. Update
the decoder only for explicitly supported targets. A change to envelope shape or
incompatible target semantics requires another explicitly supported protocol version,
never reinterpretation of V1. The 16-entry bound is a simultaneous availability
budget, not a bit-mask ID limit. Do not migrate working legacy callers unnecessarily.

## Slice 49 hardware acceptance

Use the exact candidate SHA and checksum in the backlog receipt after successful
Thor CI. Install/checksum as in the build playbook. No emulator or CI proves these:

1. In a locally controlled battle compare active-unit name/count/attack/defense/HP
   and round with the upper display. Wait/Defend act once and update normally;
   waited units cannot wait again. On opponent turns information remains and manual
   commands disable. In tactics only original Next/End remain usable.
2. Start normal auto-combat once and rapidly tap again: one AI only. Verify active
   Stop label; stop once while AI/animation/opponent turn blocks manual controls.
   Verify it winds down safely and becomes startable only after native AI cleanup.
   With end-with-auto-combat configured, cancel and accept its original upper
   confirmation; never advertise an unavailable stop or change the native mode.
3. Cast Spell opens the original upper spellbook. Cancel, reopen, enter targeting,
   cancel targeting, and cast normally. Lower inappropriate controls disable and
   restore with the current unit. Check no-hero/already-cast/magic-blocked cases.
4. Retreat and Surrender open original upper confirmations; cancel each and verify
   both displays restore and remain interactive, with no residual frame. Where
   practical confirm each in a disposable battle. Verify flee-blocking effects,
   unavailable surrender, critical hero/scenario loss, insufficient gold and the
   native marketplace branch where available. Check multiplayer ownership.
5. Hold a button while the unit/turn/context changes; rapid alternating/double taps,
   drag-off, second pointer and cancel never execute stale/wrong-owner commands.
   Repeat over spellbook, confirmation and Battle Result transitions.
6. Toggle/reconnect the lower display and background/resume during a held tap,
   auto-combat, spellbook/targeting and a confirmation. Verify one current deck,
   no delayed command and correct parent restoration. Recheck upper touch,
   keyboard/controller, ordinary single-display play, Adventure and Hero/Town decks.
