# AYN Thor VCMI repository discovery

Discovery date: 2026-09-13

Baseline: VCMI `develop` at `819259d97f1de9262b97811ccb081346c20ffef2`

This report records the repository-specific evidence used to plan the AYN Thor dual-screen fork. It is a design document, not approval to implement a feature slice.

## Repository and fork state

- Official reference: `https://github.com/vcmi/vcmi.git`.
- New public fork: `https://github.com/CapnChaosDK/vcmi_thor`.
- Local and fork branch: `ayn-thor-dual-screen`.
- `origin` is the CapnChaosDK fork. `upstream` fetches from official VCMI and has its push URL disabled locally.
- The fork branch currently matches the clean upstream baseline. `AYN_THOR_BACKLOG.md` and this report are local planning additions and have not been committed.
- Repository guidance is in `AGENTS.md`; C++ changes must follow `docs/developers/Coding_Guidelines.md`, use established constants, and preserve the server-authoritative state model.

## Engine and thread architecture

VCMI is not a single-process game-state engine like fheroes2. Its architecture separates presentation from authoritative mechanics:

- `clientapp/EntryPoint.cpp` supplies `SDL_main` on mobile, initializes the library, creates `GameEngine` and `GameInstance`, opens the main menu, and enters `GameEngine::mainLoop()`.
- `client/GameEngine.cpp` runs the `MainGUI` loop. Each frame fetches SDL events, acquires `interfaceMutex`, updates the client, processes input, redraws the active window stack, updates the screen texture, and presents it through the selected SDL backend.
- `client/gui/WindowHandler.*` owns an ordered `windowsStack`. `pushWindow()` deactivates the current top and activates the new top; `popWindows()` deactivates/removes the top and reactivates its immediate parent. This is the primary source of visible client context and exact modal restoration. Do not collapse multiple stack transitions merely to suppress a transient Thor context: parent activation/deactivation can perform essential upper-screen cleanup, as demonstrated by Battle Result dismissal restoring Adventure Map.
- `client/gui/CIntObject.*` defines the common activatable UI base. A later context bridge should expose an explicit stable context identity from screen/window owners rather than rely on Android labels or coordinates.
- `runNetwork` processes incoming packets and client feedback. `runServer` is the authoritative server thread for local/hosted games. The server validates client requests and sends state changes back to clients.
- `lib/gameState/CGameState.h` owns authoritative map, player, hero, army, town, and game-option state. Client-side visibility must be read through `CPlayerSpecificInfoCallback`/related callbacks.
- `lib/callback/CCallback.*` is the safe mutation boundary. Existing methods create network packs for movement, turn completion, stack arrangement, artifact exchange, hero dismissal, construction, recruitment, and other actions. A Thor action must re-resolve current client-visible objects, validate current UI/context, then invoke these existing client request paths. It must never mutate `CGameState` directly.

### Safe future game-thread dispatch

`GameEngine::mainLoop()` and `GameEngine::updateFrame()` are the only safe consumer location for one-shot Android requests that affect gameplay UI or issue client/server requests. The bridge should enqueue bounded plain data from the Android main thread and consume it under the normal `MainGUI` frame path. Direct JNI calls from a lower-screen touch handler into gameplay code would violate thread ownership.

## Android application architecture

VCMI combines Qt and SDL in one Android package:

- `android/AndroidManifest.xml` declares `ActivityLauncher` as the launcher, `VcmiSDLActivity` as the game activity, and `ActivityMapEditor` as a separate `:editor` process.
- `android/vcmi-app/src/main/java/eu/vcmi/vcmi/ActivityLauncher.java` extends Qt's `QtActivity`. It manages setup/import/mods and launches `VcmiSDLActivity` for the game or `ActivityMapEditor` for the editor. Its controller handling is limited to Android key events because SDL cannot read gamepads while the Qt launcher owns the foreground.
- `VcmiSDLActivity.java` extends `org.libsdl.app.SDLActivity`. It loads the VCMI client shared library, moves SDL's `mSurface` into `activity_game.xml`, starts background services, restores immersive mode and SDL focus after resume, and shuts the game process down in `onDestroy()`.
- `ActivityMapEditor.java` extends Qt's `QtActivity`; `AndroidManifest.xml` assigns it to `:editor`, and launcher startup selects the editor through `VCMI_LAUNCH_MAP_EDITOR`. It does not share the SDL game process or its static native state.
- `ServerService` also runs in its own process (`eu.vcmi.vcmi.srv`) and communicates with the game activity via Android `Messenger` while the native client/server layer handles game requests.
- `lib/CAndroidVMHelper.*` already supports native-to-Java static calls and caches the Android VM/class loader. It is suitable infrastructure for later bounded state notification, but no generic Java-to-native Thor action entry point exists yet.
- `NativeMethods.java` is the current Java helper surface for paths, services, progress display, and haptics. Thor methods should live in a separate narrowly named bridge class or a clearly isolated section, guarded from standard builds.
- The validated main-menu slices show that `CMenuScreen` already publishes configured tab names. Map only exact approved names (`main`, `new`, `load`, `campaign`, and `credits`) to stable context IDs; every other name, including malformed and mod-added names, must publish `UNKNOWN`. Android must render known IDs from bounded local resources rather than from arbitrary native title/status text.

### Rendering lifecycle

- The upper game remains a single SDL surface owned by `VcmiSDLActivity`; SDL2 and SDL3 backends live under `clientsdl2/` and `clientsdl3/`.
- `GameEngine::mainLoop()` presents the normal screen texture. The lower deck should be an Android `Presentation` with its own Android `View`; it must not request a second SDL renderer or redirect the primary surface.
- `VcmiSDLActivity` already repairs input focus after resume/window-focus changes. The presentation must be non-focus-stealing and must not interfere with this logic.
- The presentation controller should register display callbacks only while its activity is alive/visible, dismiss before the existing `onDestroy()` process exit, and ignore late callbacks after teardown.

### Player-installed visual assets in hero-centric Thor contexts

- Visual keys are decorative and revision-bound. Creature keys use CreatureID, artifact keys use artifact type ID (never artifact instance ID), and HERO keys use the stable portrait-source HeroTypeID returned by `CGHeroInstance::getPortraitSource()` (never a hero instance ID or pointer). Adventure and Hero Window carry the exact selected/active portrait key in their context; Hero Meeting carries ordered left/right keys beside its Army and Artifact snapshots.
- Native lookup uses the existing render handler and player-installed `CPRSMALL`, `Artifact`, and `PortraitsSmall` frames. The `PortraitsSmall` path matches the portrait resource used by VCMI's Adventure hero list and honors map-specific portrait sources. Images are transported only when referenced by the current semantic context.
- A Hero Meeting context may reference at most 64 distinct assets: 14 creature rows, 48 artifact slots, and two hero portraits. Each PNG is at most 64×64 pixels and 32 KiB. Native and Android caches retain at most 64 entries and 1 MiB each. Native checks the Android cache before sending; Android rejects stale/unreferenced keys and preserves existing text and interaction behavior if a resource is unavailable or fails decoding.
- Context/revision changes replace rendered references and clear transient gestures. The bounded activity-owned image cache may reuse type assets across revisions and presentation recreation. This path stays separate from SDL frame publication and adds no gameplay action, hit target, or game-state mutation.

## Existing multi-display support

A repository-wide Android/client search found no use of `DisplayManager`, `Presentation`, display-listener callbacks, virtual displays, or secondary Android surfaces. VCMI currently has no Android multi-display support.

The proven fheroes2 policy is applicable: compare candidates with the activity's current display, prefer `DISPLAY_CATEGORY_PRESENTATION`, accept a valid active non-activity display only as a compatibility fallback, never persist display IDs, and rebuild when Android reports add/remove/change. VCMI-specific lifecycle and Qt/SDL packaging still require independent tests.

## Input paths

- `clientsdl2/events/InputHandler.*` and its SDL3 counterpart fetch SDL events into a queue and process them on `MainGUI`.
- Mouse, keyboard/text, touch, and controller sources are separated in `clientsdl2/events/InputSource*` and mirrored under `clientsdl3/events/`.
- Touch input has native tap, long-press, panning, pinch, and cancellation state in `InputSourceTouch`; lower-screen gestures must stay in the Android presentation and must not leak into this upper-screen SDL touch source.
- `InputSourceGameController` opens SDL game controllers, tracks stable joystick instance IDs, and dispatches configured semantic shortcuts through `EventDispatcher`.
- `config/keyBindingsConfig.json` maps physical controls to `EShortcut` values. Defaults include left-stick cursor, right-stick map swipe, A/B accept/cancel, X/Y mouse clicks, shoulders for next hero/town or battle defend/wait, triggers for visit/spell, Back for end turn/autocombat, Start for options, and D-pad navigation/context shortcuts.
- `client/gui/ShortcutHandler.*` already has a broad stable `EShortcut` vocabulary for menus, Adventure, battle, towns, hero exchange, recruitment, settings, and lists. Shortcuts are useful compatibility entry points, but native semantic callbacks are safer where availability and object identity matter.

Slice 1 must leave every existing input source untouched. Later Android touches should enqueue separate semantic requests rather than synthesize coordinates. Key/shortcut reuse should be limited to actions whose existing shortcut path already expresses the exact semantic operation and availability.

## Visible screens and workflow owners

The real VCMI hierarchy differs from the fheroes2 fork and must drive context names:

- Main menu: `client/mainmenu/CMainMenu.*`, configured by `config/mainmenu.json`. The SDL menu contains Main, New, Load, and Campaign submenus. Main exposes New Game, Load Game, High Scores, Credits, and Quit. New exposes Single Player, Multiplayer, Campaign, Tutorial, and Back. Load exposes single, multiplayer, campaign, tutorial, and Back. Campaign selection includes SoD, RoE, Armageddon's Blade, custom campaigns, and Back, with mod/config-dependent additions.
- High scores and campaigns: `client/mainmenu/CHighScoreScreen.*`, `CCampaignScreen.*`, `CPrologEpilogVideo.*`, and `client/lobby/CBonusSelection.*`.
- Scenario/load/campaign setup: `client/lobby/CLobbyScreen.*`, `CSelectionBase.*`, `SelectionTab.*`, `OptionsTab.*`, `ExtraOptionsTab.*`, `TurnOptionsTab.*`, `RandomMapTab.*`, `CScenarioInfoScreen.*`, `CCampaignInfoScreen.*`, `CSavingScreen.*`, and `BattleOnlyModeTab.*`.
- Adventure Map: `client/adventureMap/AdventureMapInterface.*`; commands are centralized substantially in `AdventureMapShortcuts.*`. `CList.*`, `CMinimap.*`, `CInfoBar.*`, and `CResDataBar.*` expose focused hero/town, minimap, date, and resources. `client/windows/CMapOverview.*`, `CKingdomInterface.*`, `CPuzzleWindow.*`, and `CQuestLog.*` own related views.
- Hero: `client/windows/CHeroWindow.*` with reusable garrison and artifact widgets under `client/widgets/`.
- Town/castle: `client/windows/CCastleInterface.*`, with native callbacks for building, recruitment, upgrades, visiting/garrison heroes, markets, guilds, shipyards, and related nested windows.
- Hero meeting/exchange: `client/windows/CExchangeWindow.*`, `client/widgets/CGarrisonInt.*`, and artifact widgets. Existing callbacks already represent exact move/merge/split/swap and bulk army/artifact operations.
- Battle: `client/battle/BattleInterface.*`, `BattleWindow.*`, `BattleActionsController.*`, `BattleFieldController.*`, `UnitActionPanel.*`, `StackQueue.*`, and `BattleResultWindow.*`. Battle availability depends on active side/unit, tactics, spell targeting, and server battle state.
- Dialogs and choices: `client/windows/CMessage.*`, `InfoWindows.*`, recruitment/market/spell/level/reward windows, plus numerous object-specific windows. The `WindowHandler` top and parent stack are essential to avoid stale controls and stacked dialogs.
- Settings: `client/windows/settings/SettingsMainWindow.*` and its General, Adventure, Battle, Other, and key-binding tabs, configured under `config/widgets/settings/`.
- Map editor: Qt code under `mapeditor/`, launched through `ActivityMapEditor` in a different process. It cannot be modeled as another SDL `WindowHandler` context. A later editor milestone needs a separately approved Qt/editor publication and action path.

## Recommended context and state integration points

1. Add a small Thor UI-state service owned by the client `GameEngine`, created only in Thor builds.
2. Publish visible context changes from the concrete owner that already owns the visible window lifecycle. Call its established base activation/deactivation first, then publish a stable identifier or `UNKNOWN`; do not alter `WindowHandler::pushWindow()`, `popWindows()`, or close behavior to optimize deck transitions. Avoid a large RTTI classifier and do not derive context from rendered pixels.
3. Treat every top-window transition as a new context revision and clear the pending request slot immediately. Nested windows naturally restore their exact immediate parent through the existing stack.
4. Publish action availability from the owning screen/controller after its native state checks. Do not let Android infer availability from labels or visible widgets.
5. Use `CPlayerSpecificInfoCallback` and battle callbacks for visible state. Bind entries to stable VCMI object IDs plus a revision; never expose raw pointers.
6. Consume one pending Android request in `GameEngine::updateFrame()` on `MainGUI`, re-resolve the current top context and IDs, and call existing UI/controller or `CCallback` request paths.
7. Let the server perform its existing final gameplay validation. Client-side revalidation is still required to reject stale/incorrect lower-screen requests before a packet is issued.
8. Use `CAndroidVMHelper` only for bounded transfer/notification. Define hard limits before adding text arrays or pixels. Do not poll or stream full SDL frames.

## Build, test, packaging, CI, and release

- Root build: CMake, C++20, with Conan on Windows and CMake presets in `CMakePresets.json`.
- Android is a two-stage generated build: CMake compiles native shared libraries and invokes Qt's `androiddeployqt`; Gradle then packages `android/vcmi-app`. A standalone Gradle build from the source `android/` tree is not a complete VCMI build.
- Android requirements documented in `docs/developers/Building_Android.md`: CMake 3.26+, JDK 17, Android SDK, NDK (documented r25c or compatible), Ninja optionally, Qt Android, and Conan dependencies. CI currently builds ARMv7, ARM64, and x86_64; Thor should target ARM64 while leaving upstream variants untouched.
- The app namespace is `eu.vcmi.vcmi`; the production application ID is `is.xyz.vcmi`, debug adds `.debug`, and release builds already accept an `applicationIdSuffix` generated through CMake/Gradle properties. The document provider authority uses `${applicationId}`, which naturally follows the fork package.
- Existing unit tests use GoogleTest under `test/`, enabled by `ENABLE_TEST`, registered through `gtest_discover_tests`, and run with CTest. New bridge contract tests should be independent of proprietary game data where possible.
- Hosted CI is `.github/workflows/github.yml`. It builds packages across desktop/mobile platforms, builds/tests multiple Linux compiler combinations, validates server startup, validates JSON, checks relative includes, enforces LF line endings, and runs Markdown lint. Android packaging produces APK and AAB artifacts.
- Official release flow builds from `master`, stages versioned platform artifacts, source archives including submodules, and creates a draft GitHub release. The Thor fork uses its permanent branch-scoped ARM64 workflow only through `ci/thor-candidate-validation`; it never publishes to VCMI infrastructure and later publishes prereleases only from hardware-validated checkpoints.
- Repository submodules are GoogleTest, VCMI innoextract, VCMI dependencies, and Discord presence. They were not initialized during read-only discovery.
- Current local readiness: Android SDK and ADB exist; an isolated Conan/CMake/Ninja toolchain and NDK r29 are available. Windows is suitable for focused source checks, but the official ARM64 dependency cache contains Linux-host Qt generators and Android Studio's JDK 25 has a Gradle cache-close limitation. Use Linux/JDK 17 CI for a complete candidate APK. The exact repeatable procedure is in `docs/AYN_THOR_BUILD_PLAYBOOK.md`.
- The known-good full build is GitHub Actions run `35142531415`: Ubuntu 24.04, Temurin JDK 17, recursive submodules, isolated `GRADLE_USER_HOME`, the official Conan ARM64 dependency bundle/profiles, `android-thor-release`, and an installable release APK. The permanent-candidate validation workflow injects a one-line cleanup into `android/gradlew` before configuration because Qt 5.15.19 writes the removed `android.bundle.enableUncompressedNativeLibs` property immediately before starting Gradle. See the playbook for the exact commands and why earlier cleanup attempts failed.

## Licensing and original assets

- VCMI source files and `license.txt` state GNU GPL version 2 or later. Distributed modified binaries must remain GPL-compatible, retain notices/license, and make the corresponding source and build scripts available under GPL terms. Dependency and bundled-asset licenses still need to be preserved in packaged notices.
- VCMI does not provide the proprietary Heroes III game data. `docs/players/Installation_Android.md` requires the player to supply data from Heroes III: Shadow of Death or Complete and imports `Data`, `Maps`, and `Mp3` into app storage. Restoration of Erathia alone and the HD Edition are rejected by current startup/import checks.
- A distinct Android application ID yields a distinct app-specific external files directory through `Storage.getVcmiDataDir()`, so Thor assets, settings, mods, and saves remain separate from standard VCMI.
- The Thor APK must contain only VCMI/fork-created generic framing and appropriately licensed resources. Heroes III portraits, creatures, artifacts, town art, fonts, or other original assets may be decoded only from the player's imported data at runtime. Missing, malformed, or oversized resources must fall back to bounded generic/text UI.

## Discovery conclusions

- The smallest safe foundation is Android-only: fork identity plus a lifecycle-safe inert `Presentation` owned by `VcmiSDLActivity`.
- No native bridge belongs in Slice 1. This isolates multi-display, focus, package/data separation, and lifecycle behavior before exposing gameplay state.
- `WindowHandler` plus explicit per-window context descriptors is the preferred later context source.
- `GameEngine::updateFrame()` is the preferred one-shot request consumer on `MainGUI`.
- `CCallback` and existing controllers are the preferred semantic action executors; the server remains authoritative.
- The Qt launcher and separate Qt map editor require their own future designs. They must not be forced through the SDL bridge.
- Slices 1 through 19 have been implemented, CI-built, and hardware-validated. Slice 9 confirmed that a context-only change must preserve VCMI's full native window lifecycle: bypassing BattleWindow's normal restoration during Battle Result dismissal left a stale battle frame above Adventure Map until the original close sequence was restored. Slice 10 extended that lesson to Adventure utility/modal owners: `CKingdomInterface`, `CQuestLog`, `ScenarioEventJournal`, `CPuzzleWindow`, and `CSavingScreen` publish only after their real lifecycle transition; deactivation publishes `UNKNOWN`, while immediate-parent reactivation restores the correct newer context. `CPlayerInterface::showQuestLog()` may open Scenario Journal instead of Quest Log, so those windows remain distinct contexts. Slice 11 adds the first lower-display command path: revision-bound semantic actions travel from the Android deck through a bounded JNI queue and are revalidated on `MainGUI` before the existing Adventure Map shortcut executes. Slice 12 adds two reusable stale-action rules: selected-hero identity is revision-bound even when visible button state is unchanged, and every accepted command consumes its revision before later lower-screen input can replay it. Slice 13 extends the bounded Adventure Map snapshot with only the local selected hero's translated name and movement points; it adds no action, map, army, fogged, or server-private data. Slice 14 extends the same atomic context payload with four UTF-8-safe bounded generic detail lines and renders the active `CHeroWindow`'s exact `curHero` name, level/class, primary skills, mana, and experience without adding controls, images, or a global lookup. Slice 15 reuses that bounded payload for the active `CCastleInterface::town` only: translated town/faction, daily gold income, build progress, visiting hero, and garrison hero. Slice 16 provides the exact Battle-owner command deck with revision-bound Wait/Defend and tactics controls. Slice 17 adds the current active-unit dashboard; during normal opponent turns `battleActiveUnit()` supplies only the exact read-only unit while the local controller exclusively determines whether commands are enabled. Slice 18 adds the validated Adventure hero quick selector and its final spacing/contrast corrections; Slice 19 adds the validated revision-bound Adventure town quick selector. The lower display never injects coordinates or opens windows directly. Future candidates use the permanent `ci/thor-candidate-validation` branch so ccache reuse carries across slices while generated build trees remain clean. `AGENTS.md` governs autonomous selection and workflow; acceptance history and remaining roadmap are maintained in `AYN_THOR_BACKLOG.md`; the build/device hand-off and known-good GitHub candidate procedure are in `docs/AYN_THOR_BUILD_PLAYBOOK.md`.
