# GridMod Controls & Usage Guide

Everything you can do in GridMod, how the inputs behave, and where to find the relevant toggles.

## 1. Getting started

1. **Bind the essentials** under `Controls → Mods → GridMod`:
   - `Open Grid UI`, `Toggle Grid`, `Paint: Toggle`, `Paint: Clear`, `Paint: Toggle overlay`.
   - Add `Brush size ±`, `Paint: Erase (hold)`, `Blueprint: Quick load/place`, `Blueprint: Rotate CW/CCW/Flip`, and the three settlement binds once you get comfortable.
2. **Toggle Grid** or **Paint: Toggle** – either action spawns the quick palette panel on the left edge of the screen. Expand the tabs you need (Grid, Paint, Blueprints, Settlement).
3. **Open Grid UI** for the full-screen configuration window. Everything in the quick palette also mirrors into the Grid UI for players who prefer a single menu.

## 2. Interface overview

- **Quick palette overlay** (left sidebar) – fast access to:
  - Grid toggles/alpha sliders.
  - Paint category/layer selectors, brush size, erase, and color swatches.
  - Blueprint picker (per-save, global, read-only defaults) with load/save buttons, rotate/flip controls, and selection filters.
  - Settlement panel with overlay toggle, tier picker, and placement buttons.
  - Any click inside these panels pauses painting until you release the mouse so you don’t accidentally draw while pushing buttons.
- **Grid UI** (full window) – duplicates all quick palette controls plus:
  - Precise color pickers for grid lines, chunk lines, paint, selection, blueprint ghost, settlement outline/fill.
  - Opacity sliders, default presets, and paint-category color reset buttons.
  - Status text showing the active world, active blueprint, and brush state.

## 3. Grid overlay controls

- `Toggle Grid` shows/hides base tile lines. When on, the quick palette “Grid” tab appears.
- Inside the panel (or Grid UI) you can:
  - Enable **Chunk lines** and **Sub-chunk lines** independently.
  - Adjust **alpha**, **thickness**, and **span** (8/16/32/64) for chunks; sub-chunks automatically track a quarter of that span.
  - Use the sliders or the keybinds (`Grid alpha ±`, `Chunk alpha ±`, `Sub alpha ±`) to tweak opacity while playing.
- Grid colors default to white tile lines, orange chunk lines, and cyan sub-chunk lines. Change them in `Grid UI → Colors → Grid`.

## 4. Painting fundamentals

- **Enable painting** with `Paint: Toggle`. Painting stays per-world; the data lives in `mods-data/colox.gridmod/worlds/<worldID>/paint_state.txt`.
- **Left click (hold)** paints the currently selected category on the current layer.
- **Right click (hold)** erases instead of painting. You can also hold the `Paint: Erase (hold)` modifier while left-clicking.
- **Right click (press)** also:
  - Cancels blueprint placement.
  - Clears the active selection and returns to `Selection mode: None`.
- **Brush size** 1–32 is controlled via the quick palette slider or the `Brush size ±` keybinds.
- **Paint overlay visibility:** `Paint: Toggle overlay` hides the render but keeps the data. You can paint “blind” this way and bring the overlay back later.
- **Categories:** Choose floors, walls, doors, lighting, furniture, etc. via the quick palette dropdown; categories also change the color swatch.
- **Layers:** Every stroke goes to a layer (Bottom/Floors, Middle/Objects+Walls, Top/Tabletop & attachments).
  - The quick palette shows the active **Paint Layer** for painting.
  - Layer filters apply to erase/selection operations so you can target specific content.
- **Clear paint** wipes the entire world’s overlay.
- **Autosave:** GridMod saves dirty paint every time you release the mouse or exit placement/selection.

## 5. Selection workflow

- Choose a mode in the quick palette or Grid UI:
  - `Rect` – click/drag a rectangle.
  - `Edge` – draw a path; GridMod selects tiles intersected by the stroke.
  - `Edge + Fill` – same as Edge, plus fills the polygon traced by the stroke.
  - `All` – instantly selects every painted tile on the active filter.
- While a selection mode is active:
  - Left-click drag performs the selection; painting and blueprint placement are paused.
  - `Right click` exits selection (switches mode to `None`).
  - `Selection layer` dropdown decides which layers will be considered (All/Bottom/Middle/Top/Wall-only/etc.).
- Use selections to save blueprints, delete specific tiles, or just count how many cells are painted (the counter is shown near the minimap/overlay).

## 6. Blueprint workflow

### Saving & loading

- **Relative (per-save)** blueprints live under `mods-data/colox.gridmod/worlds/<worldID>/blueprints/`.
  1. Make a selection (Rect/Edge/etc.).
  2. In the Blueprint panel, pick a slot from the dropdown or type a new name.
  3. Use **Save (dbl-click)** to overwrite the selected entry (requires two clicks) or **Save As** to create a new entry.
  4. `Blueprint: Quick save` keybind overwrites the currently highlighted blueprint without touching the UI.
- **Global** blueprints live under `mods-data/colox.gridmod/global-blueprints/`. Use the “Global BPs” mini panel to manage them just like per-save ones.
- **Default** blueprints come bundled in the jar (`resources/defaults/**`). They show up in the read-only dropdown; hit **Load default** to spawn their ghost instantly.

### Placement

- Load a blueprint (per-save, global, or default) – it spawns a **ghost** preview you can move with the mouse.
- **Transformations:**
  - Quick palette buttons or keybinds: `Rotate CW`, `Rotate CCW`, `Flip`.
  - You can transform repeatedly before stamping.
- **Left click** stamps the transformed blueprint into paint data (respecting categories/layers stored in the blueprint).
- **Right click** cancels placement and exits the ghost.
- After stamping, GridMod waits for you to release the left mouse button before normal painting resumes, preventing accidental brush strokes.
- You can paint while the overlay is hidden; the ghost still appears even if `Paint: Toggle overlay` is off.

## 7. Color management

- **Paint colors:** Each category has a swatch in the Paint panel. Click it to open the color picker in the Grid UI, or use the `Reset` buttons to return to curated defaults.
- **Erase, selection, and blueprint ghost** colors/alpha live in `Grid UI → Colors`.
- **Grid/chunk/sub-chunk** line colors live in their own section of the Grid UI; sliders next to each color control the opacity (alpha) defaults.
- **UI opacity** (for the quick palette) is under Grid UI → General.

## 8. Settlement helpers

- The Settlement panel (quick palette or Grid UI) controls the bounds overlay:
  - **Show settlement bounds** turns the rectangle on/off.
  - **Tier dropdown / Cycle tier** lets you preview tiers 1–5 (size shown in tiles).
  - **Place at flag** snaps the overlay to the current world settlement flag if one exists.
  - **Place here** (keybind: `Settlement: Place here`) stores your current tile as the flag and enables the overlay immediately.
  - **Toggle overlay** keybind matches the checkbox.
- GridMod keeps track of the flag coordinates in `GridConfig`; settlement data is saved with other settings.

## 9. Keybind recap

| Category | Bind | Effect |
| --- | --- | --- |
| UI | Open Grid UI | Toggle the full configuration window. |
| Grid | Toggle Grid, Grid alpha ±, Chunk/Sub-chunk toggles & alpha ±, Chunk span | Control the line overlays without leaving gameplay. |
| Paint | Paint: Toggle, Paint: Clear, Paint: Toggle overlay, Brush size ± | Gate the entire paint system and its visibility. |
| Paint extras | Paint: Erase (hold), Paint: Toggle overlay, Paint: Toggle | Erase modifier, overlay visibility, enable/disable logic. |
| Blueprint | Blueprint: Quick save, Blueprint: Quick load/place, Rotate CW/CCW, Flip | Manage ghosts without touching the UI. |
| Settlement | Settlement: Place here, Settlement: Toggle overlay, Settlement: Cycle tier | Operate the settlement bounds overlay from the keyboard. |

Bind everything under `Controls → Mods → GridMod`. Controls default to “Unbound” so they never conflict with vanilla keys.

## 10. Data & troubleshooting

- **Per-world saves:** `mods-data/colox.gridmod/worlds/<worldID>/grid_settings.txt` and `paint_state.txt`.
- **Blueprint storage:**
  - Per-save: `mods-data/colox.gridmod/worlds/<worldID>/blueprints/`.
  - Global: `mods-data/colox.gridmod/global-blueprints/`.
- **Clearing data:** Delete those files/folders (game closed) to reset everything, or use `Paint: Clear` for the current world.
- **Crash logs:** Necesse writes `latest-crash.log` in the mod folder root. Attach it to bug reports along with your `mods-data/colox.gridmod/` directory if something goes wrong.

Need more help? Open an issue on GitHub with the log and reproduction steps: https://github.com/cr-2-4/necesse-Gridmod
