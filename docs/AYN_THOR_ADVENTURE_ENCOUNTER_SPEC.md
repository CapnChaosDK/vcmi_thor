# Lower-screen Adventure encounter decisions

Implementation handoff for one integrated feature candidate. Start from the current
`ayn-thor-dual-screen` product tree. The reference checkout was `ed3aa05be`.
Follow [AGENTS.md](../AGENTS.md) and the [Thor build playbook](AYN_THOR_BUILD_PLAYBOOK.md).

## Player outcome

After an Adventure visit opens a decision, the player can read the relevant cost
or reward, select an option with a thumb, and confirm or decline on the lower
touchscreen. The upper screen retains the map or native dialog for context. The
lower screen presents a short question, large choices, and the next available
action. It returns to the current Adventure or Town controls when the native
sequence ends. The player should be able to complete each supported sequence
without touching the upper display.

## Scope and native owners

| Journey | Required lower-screen behavior | Start here |
| --- | --- | --- |
| Reward and other Adventure component choices | Show the question, each offered component and its native amount or description. Select a choice, inspect its details, confirm, or decline when offered. | `CPlayerInterface::showBlockingDialog` with `selection`, then `CSelWindow` in `client/windows/InfoWindows.cpp`. Reuse its current component selection and button paths. |
| Creature joining | Put the offered creature, quantity, price or free offer, and native Yes/No actions within easy reach. Present any subsequent native shared confirmation or notice in sequence. | `lib/mapObjects/CGCreature.cpp` emits the existing blocking question. `CPlayerInterface::showBlockingDialog` and the existing `CInfoWindow` Yes/No path already handle it. Verify the full journey and fix encounter-specific presentation gaps in that path. |
| Native destination and map-object choices | Show the actual offered destinations in native order. Tap a large row to select it and use a separate action to travel or confirm. Preserve native cancellation and map centering where available. | `CPlayerInterface::showMapObjectSelectDialog` and the Castle Gate list in `client/windows/CCastleInterface.cpp` both use `CObjectListWindow`. Opt in at these call sites. Reuse native selection, accept and exit behavior. |

Ordinary monoliths and whirlpools use automatic path-based or random answers in
`HeroMovementController::showTeleportDialog`. They stay automatic. Destination
support applies when the game actually opens an interactive picker. Battle
results, level-up choices, paid services, and unrelated object-list callers such
as lobby selectors are separate work.

If accepting a creature offer opens a dedicated army rearrangement window,
hand off to that native window and restore the lower controls when it closes.
The creature-joining scope covers the offer and its shared decision dialogs.

## Touch interaction

- Put the question and primary action on the first view. Fit up to four large,
  full-width choice rows per page. Reduce the row count if needed to keep touch
  targets at least 48 dp in the supported layout. Keep paging, Confirm and
  Cancel in separate, fixed touch regions. Show the current page and selection.
- A row tap selects once and highlights the current option. Tapping a row again
  does not confirm. Use the native selection as the initial value and keep upper
  and lower selection synchronized. Confirm acts on the visible selection. A
  native Yes/No prompt has two directly actionable buttons without a second confirm.
- Show native names and amounts in the rows. Allow paging through long questions
  and selected-option details when needed. Keep the selection while browsing
  pages and identify it beside Confirm. Use native labels or localized action
  strings. Text alone must support every choice.
- Render only available native actions. An empty list shows a clear empty state
  with Confirm disabled and the native exit where available. Missing or oversized
  publication data leaves the native upper dialog usable.
- Once an action is submitted, suppress repeated input until native progress or
  rejection resolves it. Restore input only for a still-current prompt. A new
  prompt starts from its own native selection, including when it looks identical
  to the previous one. Return to the live parent deck after the last dialog.

## Integration constraints

- Reuse the shared dialog publication and Android dialog/paging paths where they
  fit. Inspect `CInfoWindow` and the Settings use of `CObjectListWindow` and
  `SETTINGS_SELECTION` before adding another transport or list renderer. Keep
  generic window opt-in explicit so unrelated contexts keep their current UI.
- Keep the native window and MainGUI authoritative. Publish bounded, immutable
  snapshots of the active player's visible question and choices. Use stable
  object IDs for destinations and owner-bound option identities for component
  choices. Android sends intent against the current revision, never a server
  answer value. Re-resolve the owner, player/interface, prompt identity, parent,
  row membership and enabled action before using the same native path as upper
  input. Keep native question numbering, cancellation and callback ordering.
- Cover parent/child dialogs, upper selection or close while a lower touch is
  held, duplicate releases, rapid changes between questions, multitouch, display
  toggle or reconnect, and background/resume. Clear held and pending input when
  the applicable context, owner, revision or presentation session changes. Keep
  native upper touch, controller, focus, and single-display behavior intact.
- Keep engine and server rules, save and network formats, and movement behavior
  on the existing upstream paths. Preserve question auto-accept and cancellation
  semantics. Keep the overlay limited to the presentation, transport and native
  owner adapters required for these journeys.

## Acceptance and delivery

1. **Reward choice.** Choose a non-default reward with lower touch, confirm it,
   and observe exactly one native result. Exercise a cancellable and a mandatory
   prompt. Verify long localized text, native selection parity, paging beyond the
   first list page and readable targets on the device.
2. **Creature joining.** Accept and decline free and paid offers on disposable
   saves. Verify the displayed offer, resulting army and gold, and any native
   follow-up when the army is full or funds are insufficient. Complete shared
   prompts on the lower screen and verify handoff and recovery for a dedicated
   army window if one opens.
3. **Destinations.** Exercise the server-driven object picker and Castle Gate
   with multiple destinations, cancel, and confirm a non-default destination.
   Exercise no valid destinations and an invalidated target. Verify the native
   outcome and parent restoration. Check ordinary automatic teleport behavior.
4. **Input and lifecycle.** Confirm from the upper screen while holding a lower
   touch, replace the prompt, rotate or recreate the presentation where supported,
   toggle or reconnect the lower display, and background/resume. Verify no stale
   or duplicate action executes and no prompt leaves the player stuck. Recheck
   upper touch/controller input and single-display play.

Add focused native tests for action availability, option mapping and stale
owner/revision rejection, and JVM tests for paging, large disjoint touch regions,
selection and gesture cancellation. Run the upstream boundary and validation-plan
checks. Record the scope and truthful status in the backlog, and put the integrated
device checklist in `CI/thor-validation-plan.json`. Complete the repository's
review gates and publish one candidate for the complete feature. Hardware
acceptance requires the checksum-verified APK on a real AYN Thor.
