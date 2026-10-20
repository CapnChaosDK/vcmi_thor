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
V1 has version `1`; Battle IDs 1–5 require target `-1`. Player setup IDs 6–12
require an explicit PlayerColor target 0–7. These target contracts are immutable.
JNI checks positive revision, non-null/exact array length, version, known command
and the command-specific target **before** submitting to the existing 16-entry `ThorActionQueue`.
Wrong versions, unknown IDs, extra/missing words, and out-of-range/wrong-family targets fail closed.
The return boolean means queued, never native execution or gameplay success.

| V1 extended ID | Meaning | Native route |
| --- | --- | --- |
| 1 | Start auto-combat | `BattleWindow::bAutofightf` |
| 2 | Stop auto-combat | `BattleWindow::bAutofightf`, only when currently stoppable |
| 3 | Cast Spell | `BattleWindow::bSpellf` |
| 4 | Retreat | `BattleWindow::bFleef` |
| 5 | Surrender | `BattleWindow::bSurrenderf` |
| 6 | Cycle player assignment | `OptionsTab::onSetPlayerClicked` |
| 7 / 8 | Previous / next faction | `setPlayerOption(TOWN, -1/+1, color)` |
| 9 / 10 | Previous / next hero | `setPlayerOption(HERO, -1/+1, color)` |
| 11 / 12 | Previous / next bonus | `setPlayerOption(BONUS, -1/+1, color)` |

These IDs form a separate namespace; they are **not** new legacy mask bits.
Start/Stop are distinct idempotent intents, never a remotely requested toggle.

Availability is `publishThorCommands(revision, version, int[] enabled, int[] active)`.
Each array has at most 16 unique Battle IDs; invalid version, null/oversized arrays,
unknown IDs or duplicates yield an empty state. Android copies arrays before UI
posting and applies them only at the exact current Battle revision. Active ID 1
means auto-combat is on; availability of ID 2 independently means it can be stopped.
A disabled Stop label can therefore represent running but non-stoppable auto-combat.
Active IDs need not be enabled. No target collection or arbitrary strings/JSON exist.

`ThorExtendedCommand.h` owns bounded protocol values/decoding. Battle eligibility
is separate in `ThorBattleControls.h`. The internal queue envelope has legacy
`action = NONE`, `commandVersion = 1`, `commandId` and other legacy payload
fields at their no-target defaults. Mixed legacy/extended envelopes are rejected.
Version/ID zero defaults preserve all existing aggregate callers.

## Native authority and lifecycle

Slice 53 leaves these payloads, IDs, revisions and availability rules unchanged.
The existing `clearThorActions()` lifecycle callback additionally sets a bounded,
mutex-protected controller-reset flag alongside clearing its queues. MainGUI consumes
that flag before frame input/commands; ordinary queue clears cannot lose it.
Accepted lower Battle actions cancel pending upper controller intent before invoking
their existing native routes. No controller gameplay data travels through Java.

The normalized context includes the lists in semantic equality; a list, owner,
participant, active-unit, turn/targeting or action-epoch change invalidates the
rendered revision. Non-Battle contexts clear the Battle availability lists. Player setup capabilities
are carried per color in the atomic setup snapshot described below. Battle keeps
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

## New Game player setup (Slice 50)

`publishThorLobbyPlayers(revision, int[] data, String[] labels)` carries 0–8 unique
PlayerColors. Each row has two integers `[color, flags]` and five UTF-8-safe labels
of at most 256 bytes: color, assignment name, faction, hero, bonus. Flags are
1 assignment, 2 faction, 4 hero, 8 bonus, 16 human, 32 local, 64 awaiting lobby
refresh. Pending disables all player mutations. Null, malformed, duplicate,
oversized or out-of-range payloads fail closed. Java copies before UI posting and
only installs the exact current New Game scenario revision. Metadata and rows
travel in ordered callbacks; rows are withdrawn on revision changes until the
matching callback arrives. No row indexes, native identities, faction/hero rules,
map paths or private opponent game state cross JNI.

`OptionsTab` derives rows from current native StartInfo/PlayerInfo. Its existing
name helper provides translated faction/hero/bonus labels, including Random/None.
The host can configure AI slots. Guests can edit only their own color's options
and cannot cycle assignments. Assignment follows the existing flag semantics:
human-capable slots only, AI or more than one human. Fixed heroes, absent factions,
random-faction heroes and unavailable choices use native upper-arrow restrictions.
Server `optionNextCastle/Hero/Bonus` chooses legal values and applies random,
hero uniqueness and artifact/resource restrictions. Nonstandard battle/random-map
or campaign modes and non-lobby state are read-only on this surface.

MainGUI resolves the exact top lobby and refreshes the snapshot before validating
revision, epoch, current scenario/server map match, PlayerColor and per-row
capability. `OptionsTab` independently rechecks live rules before invoking the
original assignment callback or server option operation. One pending operation
blocks player edits, difficulty, scenario navigation and Start; Back remains usable.
The pending snapshot consumes an action epoch and invalidates queued duplicates.
Only a real lobby state refresh or server rejection releases pending; no timer or
optimistic option change is used. A queued command or dispatch feedback does not
mean server acceptance. Upper controls remain available through normal native UI.

Option/connection identities are native-only semantic revision inputs, so equal
translated names cannot conceal a state change. Identical snapshots reuse the
revision and skip the player callback. Scenario mismatch, modal ownership, leaving
New Game, Load or gameplay withdraw the rows. Context/revision, pointer, cancel,
resize, pause, detach and recreation clear held gestures. Local tabs/pages contain
no gameplay state. Players shows one color at a time with Previous/Next and
48dp-minimum buttons at the measured 1080×1240 / 369dpi lower-panel reference.
Scenario retains its existing controls; both views expose Start/Back.

### Slice 50 physical acceptance

Use the exact successful candidate SHA/checksum receipt and the build playbook.

1. Complete a standard local scenario → Players → permitted color assignment →
   faction → hero → bonus → Scenario difficulty → Start journey using only the
   lower display. Compare every acknowledged value with upper Options. Test both
   cycling directions, wrap/Random, None, fixed heroes and single-faction maps.
2. Check all playable colors, AI slots and at least two-human hot-seat assignment.
   Compare flag behavior with upper controls. In network host/guest lobbies verify
   host AI editing, guest-own options, locked remote slots and blocked guest
   assignment. Verify unavailable artifact/resource bonuses remain native-limited.
3. Rapid/double/alternating taps must apply one current request. Change scenario,
   assignment or upper options while holding a lower edit; no stale action applies.
   Exercise Back during pending, server rejection/disconnect and re-entry. Confirm
   current data returns after acknowledgement and no optimistic value appears.
4. Cancel/drag off/add a pointer. Toggle/reconnect the display, background/resume
   and recreate during held/pending edits and child modals. Restore one current
   deck with no replayed input, stale row or blocked-parent restoration.
5. Recheck Load Game, campaign and random-map/advanced setup on the upper display,
   Main Menu/confirmation restoration, upper touch/controller/keyboard and normal
   single-display behavior; smoke-test Battle, Adventure, Hero and Town controls.

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
