# GridMod module map

This document inventories the Java sources under `src/main/java/colox/gridmod`, with a focus on the `config`, `input`, `overlay`, `paint`, `ui`, and `util` packages. For each file it records the class it declares, the responsibilities it handles, and the notable collaborators it depends on or surfaces for other code.

## config

### GridConfig.java
- **Role:** Central repository for all configurable grid, paint, and settlement settings, plus persistence helpers for loading/saving them on disk. Handles clamping, defaults, and derived settlement metrics.【F:src/main/java/colox/gridmod/config/GridConfig.java†L1-L210】【F:src/main/java/colox/gridmod/config/GridConfig.java†L210-L330】
- **Declares:** `colox.gridmod.config.GridConfig` (final class with static state and helpers).
- **Key dependencies:** Reads/writes via `necesse.engine.save.LoadData` and `SaveData`; resolves file locations through `colox.gridmod.util.ConfigPaths`; exposes data consumed by UI, overlays, paint, and settlement features.

## input

### GridKeybinds.java
- **Role:** Single source of truth for all GridMod keybind declarations and per-frame polling of toggle-style actions (UI open, paint visibility, paint logic controls).【F:src/main/java/colox/gridmod/input/GridKeybinds.java†L1-L166】
- **Declares:** `colox.gridmod.input.GridKeybinds` with static `Control` fields and helper methods.
- **Key dependencies:** Config toggles via `GridConfig`; paint state mutations via `colox.gridmod.paint.PaintState`; UI toggling through `colox.gridmod.ui.GridUI`; relies on Necesse `Control` and `StaticMessage` APIs.

## overlay

### GridToggle.java
- **Role:** Wraps the primary grid visibility flag with helper methods for UI and hotkey sync, including a per-frame toggle check that watches `GridKeybinds.GRID_TOGGLE`.【F:src/main/java/colox/gridmod/overlay/GridToggle.java†L1-L29】
- **Declares:** `colox.gridmod.overlay.GridToggle`.
- **Key dependencies:** Reads/writes `GridConfig.gridEnabled`; uses `GridKeybinds` to drive toggle polling.

### GridStyleControls.java
- **Role:** Applies grid style hotkeys every frame, adjusting alpha, chunk visibility, span, and sub-chunk settings within `GridConfig`.【F:src/main/java/colox/gridmod/overlay/GridStyleControls.java†L1-L37】
- **Declares:** `colox.gridmod.overlay.GridStyleControls`.
- **Key dependencies:** Reads hotkeys from `GridKeybinds`; mutates `GridConfig` properties and persistence flags.

### SettlementBoundsOverlay.java
- **Role:** Drawable overlay that renders the configurable settlement bounding box (fill plus outline) relative to the camera when settlement visualization is enabled.【F:src/main/java/colox/gridmod/overlay/SettlementBoundsOverlay.java†L1-L83】
- **Declares:** `colox.gridmod.overlay.SettlementBoundsOverlay` implementing Necesse `Drawable`.
- **Key dependencies:** Pulls rendering values from `GridConfig`; draws through `GameResources.empty`; needs level and `GameCamera` references supplied by the overlay hook.

### GridDrawable.java
- **Role:** Main grid renderer and per-frame hotkey poller; draws base grid, chunk, and sub-chunk lines respecting `GridConfig`, after ticking `GridToggle` and `GridStyleControls`.【F:src/main/java/colox/gridmod/overlay/GridDrawable.java†L1-L92】
- **Declares:** `colox.gridmod.overlay.GridDrawable` implementing Necesse `Drawable`.
- **Key dependencies:** Invokes `GridKeybinds.poll()`, `GridToggle`, and `GridStyleControls`; reads styling from `GridConfig`; draws via `GameResources.empty`.

### GridOverlayHook.java
- **Role:** ByteBuddy patch that injects GridMod overlays into `Mob.addDrawables`, wiring per-frame paint controls plus grid, settlement, and paint drawables for the local player perspective.【F:src/main/java/colox/gridmod/overlay/GridOverlayHook.java†L1-L53】
- **Declares:** `colox.gridmod.overlay.GridOverlayHook` with a `@ModMethodPatch` entrypoint.
- **Key dependencies:** Calls `colox.gridmod.paint.PaintControls`, `GridDrawable`, `SettlementBoundsOverlay`, and `colox.gridmod.paint.PaintDrawable`; respects `GridConfig.paintVisible` and settlement flags.

## paint

### PaintState.java
- **Role:** Persistent model of painted tiles, brush size, and paint colors, offering mutation helpers and serialization to the GridMod data directory.【F:src/main/java/colox/gridmod/paint/PaintState.java†L1-L120】【F:src/main/java/colox/gridmod/paint/PaintState.java†L120-L190】
- **Declares:** `colox.gridmod.paint.PaintState` (static state manager).
- **Key dependencies:** File path resolution through `ConfigPaths`; persistence via `LoadData`/`SaveData`; consumed by paint controls, drawables, selection, and blueprint logic.

### SelectionState.java
- **Role:** Tracks selection modes (rectangle, edge, lasso), drag gestures, and resulting tile sets derived from the painted layer, including polygon math for advanced modes.【F:src/main/java/colox/gridmod/paint/SelectionState.java†L1-L120】【F:src/main/java/colox/gridmod/paint/SelectionState.java†L120-L218】
- **Declares:** `colox.gridmod.paint.SelectionState` with nested `Mode` enum.
- **Key dependencies:** Consumes paint snapshots from `PaintState`; exposes selection data to `PaintDrawable`, `PaintControls`, and blueprint export routines.

### Painter.java
- **Role:** Applies square brush stamps (paint or erase) centered on a tile, delegating persistence to `PaintState`.【F:src/main/java/colox/gridmod/paint/Painter.java†L1-L16】
- **Declares:** `colox.gridmod.paint.Painter`.
- **Key dependencies:** Reads brush size from and writes tile updates to `PaintState`.

### MouseTileUtil.java
- **Role:** Stores the active `GameCamera` and converts the mouse pointer to tile coordinates for paint and selection logic.【F:src/main/java/colox/gridmod/paint/MouseTileUtil.java†L1-L18】
- **Declares:** `colox.gridmod.paint.MouseTileUtil`.
- **Key dependencies:** Necesse `GameCamera`; camera reference supplied by `PaintControls` and other callers.

### BlueprintPlacement.java
- **Role:** Tracks the relative tiles of a blueprint being placed, handles rotation/flip transforms, and produces absolute tile positions anchored to the cursor.【F:src/main/java/colox/gridmod/paint/BlueprintPlacement.java†L1-L58】
- **Declares:** `colox.gridmod.paint.BlueprintPlacement`.
- **Key dependencies:** Operates on raw coordinate lists supplied by `PaintBlueprints`; consumed by `PaintControls` and `PaintDrawable`.

### PaintDrawable.java
- **Role:** Renders painted tiles, brush previews, blueprint ghosts, and selection overlays, honoring visibility gating and camera culling.【F:src/main/java/colox/gridmod/paint/PaintDrawable.java†L1-L108】【F:src/main/java/colox/gridmod/paint/PaintDrawable.java†L108-L206】
- **Declares:** `colox.gridmod.paint.PaintDrawable` implementing Necesse `Drawable`.
- **Key dependencies:** Pulls alpha/color/visibility from `GridConfig` and `PaintState`; references `MouseTileUtil`, `BlueprintPlacement`, and `SelectionState`; uses `GameResources.empty` for drawing.

### PaintControls.java
- **Role:** Per-frame input coordinator for painting, blueprint placement, selection, and settlement hotkeys. Integrates UI gating, pause detection, blueprint IO, and paint persistence management.【F:src/main/java/colox/gridmod/paint/PaintControls.java†L1-L120】【F:src/main/java/colox/gridmod/paint/PaintControls.java†L120-L240】
- **Declares:** `colox.gridmod.paint.PaintControls`.
- **Key dependencies:** Uses `GridConfig`, `GridKeybinds`, `GridUI`, `PaintState`, `PaintBlueprints`, `BlueprintPlacement`, `SelectionState`, and `MouseTileUtil`; interacts with Necesse input/window APIs via reflection for pause detection.

### PaintBlueprints.java
- **Role:** Loads and saves blueprint files (relative, selection-derived, global, legacy) and coordinates directory management for blueprint storage.【F:src/main/java/colox/gridmod/paint/PaintBlueprints.java†L1-L160】【F:src/main/java/colox/gridmod/paint/PaintBlueprints.java†L160-L320】
- **Declares:** `colox.gridmod.paint.PaintBlueprints`.
- **Key dependencies:** Persistence through `LoadData`/`SaveData`; path resolution via `ConfigPaths`; consumes/updates tile data from `PaintState` and selection snapshots provided by `SelectionState` callers.

## ui

### GridUIForm.java
- **Role:** Builds the Grid Mod settings window, including tabbed content areas, close handling, and wiring to `GridTab`, `PaintTab`, and settlement/color sections. Updates base form opacity from config.【F:src/main/java/colox/gridmod/ui/GridUIForm.java†L1-L138】【F:src/main/java/colox/gridmod/ui/GridUIForm.java†L138-L246】
- **Declares:** `colox.gridmod.ui.GridUIForm` extending Necesse `Form`, plus inner `SectionCard` helper.
- **Key dependencies:** Reads/saves settings via `GridConfig`; delegates tab construction to `GridTab` and `PaintTab`; triggers settlement helpers in `PaintControls`; uses numerous Necesse UI components.

### GridTab.java
- **Role:** Provides the grid settings tab contents (grid enable, opacity, alpha, chunk/sub-chunk options) with UI controls bound to `GridConfig` and the runtime toggle helper.【F:src/main/java/colox/gridmod/ui/GridTab.java†L1-L126】
- **Declares:** package-private `colox.gridmod.ui.GridTab`.
- **Key dependencies:** Mutates `GridConfig`; toggles grid via `GridToggle`; reuses `UiParts.SectionCard` for layout.

### PaintTab.java
- **Role:** Assembles paint-related UI, including paint toggles, overlay visibility, brush sizing, blueprint management (relative/global), placement transforms, selection tools, and status messaging.【F:src/main/java/colox/gridmod/ui/PaintTab.java†L1-L136】【F:src/main/java/colox/gridmod/ui/PaintTab.java†L136-L236】
- **Declares:** `colox.gridmod.ui.PaintTab`.
- **Key dependencies:** Reads/writes `GridConfig.paintVisible`; interacts with `PaintState`, `PaintBlueprints`, `BlueprintPlacement`, and `SelectionState`; uses `GridKeybinds` to display shortcuts; leverages Necesse UI components.

### UiParts.java
- **Role:** Shared UI helpers for section cards and tab button layout reused across tabs to keep `GridUIForm`/`GridTab` concise.【F:src/main/java/colox/gridmod/ui/UiParts.java†L1-L70】
- **Declares:** package-private `colox.gridmod.ui.UiParts` with nested `SectionCard`.
- **Key dependencies:** Necesse `Renderer`, `Form`, `FormTextButton`, and font utilities.

### GridUI.java
- **Role:** Manages lifecycle of `GridUIForm` instances per active `FormManager`, exposing open/toggle/close helpers and reflective fallbacks for the Necesse UI system.【F:src/main/java/colox/gridmod/ui/GridUI.java†L1-L120】【F:src/main/java/colox/gridmod/ui/GridUI.java†L120-L220】
- **Declares:** `colox.gridmod.ui.GridUI`.
- **Key dependencies:** Constructs `GridUIForm`; reflects into `necesse.engine.GlobalData`, `WindowManager`, and `FormManager` types to add, center, and manage forms; used by keybinds and paint controls for gating.

### PaintQuickPaletteOverlay.java
- **Role:** HUD-side “quick control” stack composed of `SidePanelForm` subclasses (Paints, Blueprints, Grid, Settlement). Each panel collapses to a button when inactive and expands into a mini form when toggled; panels appear only when their feature is enabled (e.g., paint mode, grid overlay) and collapse automatically when that feature toggles off so no blank boxes remain onscreen.
- **Key details:** Paint/blueprint/settlement panels expose the same controls found in the settings UI (paint category toggles, blueprint save/load with double-click safeguard, selection mode buttons, settlement placement/tier actions). The grid panel mirrors `GridTab` alpha sliders/toggles. `PanelsHost` attaches/detaches forms per `FormManager`, with reflection fallbacks (`addComponent`, `setTimeout`, component-list injection). `isMouseOverUi()` and `consumeToggleClick()` integrate with `PaintControls` so mouse clicks over the HUD controls never reach the world.

## util

### ConfigPaths.java
- **Role:** Computes OS-specific directories for GridMod data (settings, paint state, blueprints) and ensures they exist on disk.【F:src/main/java/colox/gridmod/util/ConfigPaths.java†L1-L37】
- **Declares:** `colox.gridmod.util.ConfigPaths`.
- **Key dependencies:** Java NIO `Path`, `Paths`, and `Files`; consumed by `GridConfig`, `PaintState`, and `PaintBlueprints`.

## Additional entry points

### GridMod.java
- **Role:** Mod entry annotated with `@ModEntry`; registers keybinds and loads configuration/paint state on initialization.【F:src/main/java/colox/gridmod/GridMod.java†L1-L32】
- **Declares:** `colox.gridmod.GridMod`.
- **Key dependencies:** Calls `GridKeybinds.register()`, `GridConfig.load()`, and `PaintState.load()`.

### ClientOverlay.java
- **Role:** Legacy helper that polls `GridKeybinds` each client frame when invoked from a drawable hook.【F:src/main/java/colox/gridmod/ClientOverlay.java†L1-L13】
- **Declares:** `colox.gridmod.ClientOverlay`.
- **Key dependencies:** Uses `GridKeybinds` to process UI hotkeys.

## Coverage checklist

All Java files under `src/main/java/colox/gridmod/config`, `input`, `overlay`, `paint`, `ui`, and `util` are documented above. Files listed by `find` in these packages: GridConfig, GridKeybinds, GridToggle, GridStyleControls, SettlementBoundsOverlay, GridDrawable, GridOverlayHook, PaintState, SelectionState, Painter, MouseTileUtil, BlueprintPlacement, PaintDrawable, PaintControls, PaintBlueprints, GridUIForm, UiParts, GridTab, GridUI, PaintTab, ConfigPaths. Each entry is described in its respective section to ensure no class from the targeted packages is omitted.【F:src/main/java/colox/gridmod/config/GridConfig.java†L1-L330】【F:src/main/java/colox/gridmod/util/ConfigPaths.java†L1-L37】
