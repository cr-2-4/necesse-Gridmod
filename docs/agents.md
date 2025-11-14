# Agent context

This file documents the expectations and public surface area of GridMod so future Codex agents have a stable context and know where to look before making changes.

## Goals
- Keep a responsive grid overlay that can show chunks, sub-chunks, and settlement bounds without forcing players into external tools.
- Offer a paint/blueprint workflow (toggleable paint logic, quick palette controls, and blueprint save/load) that mirrors Necesse’s own placement feel.
- Surface an in-game UI (Grid UI form + quick palette sidebar) so users can tweak config, manage paint layers, and trigger settlement checkpoints without leaving the game.

## Constraints and accepted patterns
- Persist all user data inside `ConfigPaths.modDataDir()` (typically `…/mods-data/colox.gridmod`). `GridConfig` owns grid/settlement settings while `PaintState` owns tile/brush data; don’t scatter config elsewhere.
- Hook into Necesse only via supported extension points (registered controls, ByteBuddy patches, `FormManager`/`WindowManager`) and keep additional reflection centralized in helper classes (`GridUI`, `PaintQuickPaletteOverlay`). Avoid inventing new reflection-heavy plumbing unless there’s no public API.
- Respect Necesse UI state: the paint tick loop immediately returns if `GridUI.isOpen()` or the game is paused, and input is gate-checked via `SelectionState`, `PaintQuickPaletteOverlay`, and the render overlay toggle system.
- Register all keybinds in `GridMod.init()` by calling `GridKeybinds.register()` once; rely on the shared `Control` fields for polling so that hotkeys stay synchronized with the Quick Palette buttons.
- When pausing gameplay for the overlay, gate actions at the `PlayerMob.runClientAttack`/`runClientControllerAttack` entry points rather than mutating individual `Item` behavior, letting movement run unimpeded while attacks/tools remain blocked.

## Public API surface

### Key classes
- `colox.gridmod.GridMod` – mod entry annotated with `@ModEntry`; initializes keybinds plus config/paint persistence, and exposes `postInit()` for future wiring.
- `colox.gridmod.input.GridKeybinds` – single source of truth for every control (grid toggles, paint brushes, settlement shortcuts, blueprint transforms, UI open). Keep it in sync with the Controls menu and only mutate via its helpers.
- `colox.gridmod.ui.GridUI` / `GridUIForm` / `UiParts` – reflection-backed helpers for creating, showing, and positioning the Grid UI form through `FormManager`; `GridUI$$` is the “UI server” that ensures only one form per state manager exists.
- `colox.gridmod.ui.PaintQuickPaletteOverlay` – sidebar overlay built with Necesse `Form` components; it aims to mimic Necesse menus and exposes helpers like `tick()`, `isMouseOverUi()`, and `consumeToggleClick()` for the paint loop.
- `colox.gridmod.paint.PaintControls` – per-frame tick called from `GridOverlayHook`; contains painting, selection, blueprint placement, and settlement shortcut logic plus UI-safe gates.
- `colox.gridmod.util.WorldKeyProvider` – computes the active world GUID used to scope paint data (via `PaintState`) under `mods-data/colox.gridmod/worlds/<worldID>/`; it falls back to `"global"` until a world is loaded.
- `colox.gridmod.util.WorldKeyProvider` – computes the world GUID key used to locate `paint_state.txt`/`grid_settings.txt` under `mods-data/colox.gridmod/worlds/<worldID>/`, so each save gets its own paint/settlement data while legacy files stay untouched.
- `colox.gridmod.input.PaintModeInputGate` – centralized helper that reports whether paint, selection, or blueprint controls are active so other patches can reuse the “overlay is active” check.
- `colox.gridmod.input.RunClientAttackBlockPatch` / `colox.gridmod.input.RunClientControllerAttackBlockPatch` – ByteBuddy hooks on the client attack entry points that skip the original implementations whenever `PaintModeInputGate` says the overlay is active, ensuring no weapon/item use slips past the guard.
- `colox.gridmod.overlay.GridOverlayHook` – ByteBuddy patch on `Mob.addDrawables` that adds `GridDrawable`, `SettlementBoundsOverlay`, and `PaintDrawable` for the local player, and that calls `PaintControls.tick(...)`.
- `colox.gridmod.config.GridConfig` & `colox.gridmod.paint.PaintState` – the in-memory config/state models. Both expose `load()`/`saveIfDirty()` and static getters/setters, so other modules consume the shared state.
- `colox.gridmod.util.ConfigPaths` – centralized knowledge of `mods-data/colox.gridmod`; exposes `modDataDir()`, `settingsFile()`, `paintFile()`, `blueprintsDir()`, and `globalBlueprintsDir()`.

### Event hooks
- `GridKeybinds.poll()` is invoked in the UI loop (currently from `GridDrawable`) to handle toggle-style actions, mode switches, and to open/close `GridUI`.
- `GridOverlayHook.onEnter()` patches `necesse.entity.mobs.Mob.addDrawables(...)`, ensuring overlays/rendering side effects occur exactly once per local-player frame, with `TickManager`, `Level`, `GameCamera`, and `PlayerMob` passed through.
- `PaintControls.tick()` orchestrates input, selection, blueprint placement, and quick palette interaction. It asks `GridUI.isOpen()`, `WindowManager.getWindow()`, and `Input` for gating before mutating `PaintState`.
- UI panels/add buttons rely on `GlobalData.getCurrentState().getFormManager()` either directly (Quick Palette) or indirectly (Grid UI form builder) and invoke form helpers (e.g., `Form.setHidden`, `Form.makeCurrent`) to control visibility.

### Config & data locations
- Grid and settlement settings: `ConfigPaths.settingsFile()` (`grid_settings.txt`).
- Paint persistence: `ConfigPaths.paintFile()` (`paint_state.txt`).
- Blueprint storage: `ConfigPaths.blueprintsDir()` for local saves and `ConfigPaths.globalBlueprintsDir()` for global variants.
- Controls configuration lives in Necesse’s regular control registry once `GridKeybinds.register()` runs; the mod does not serialize controls itself.

## Vanilla mirrors
- `necesse.entity.mobs.Mob.addDrawables(List<OrderableDrawables>, Level, TickManager, GameCamera, PlayerMob)` – patched via `GridOverlayHook` to inject overlays for the local player.
- `necesse.gfx.forms.FormManager` (`FormManager.addComponent`, `setCurrent`, `removeComponent`, etc.) – we insert, toggle, and dispose of `GridUIForm` and the quick palette panels through this manager, often via reflection to stay compatible with Necesse’s private APIs.
- `necesse.engine.window.WindowManager` / `necesse.engine.window.GameWindow` – used to poll raw mouse/keyboard input, read camera references, and center UI elements; we treat them as the “window” mirror for our overlays.
- `necesse.engine.world.worldData.SettlementsWorldData` – this is the authorative settlement metadata provider we query before drawing bounds, so the overlay mirrors Necesse’s own settlement rendering rules.
