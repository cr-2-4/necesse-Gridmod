# GridMod Workshop Description

GridMod is a QoL overlay that makes Necesse's construction tools feel as precise as level design software. It injects a clean grid, chunk indicators, settlement bounds, and the familiar paint/blueprint workflow entirely client-side, so players can layout floors, walls, and decorations with surgical accuracy before committing to the world.

## Highlights
- **Grid & brush controls:** Toggle visibility of chunk/sub-chunk lines, tweak opacity, and paint with brush size 1–32 while toggling the paint overlay independently of the paint logic.
- **Blueprint palette:** Save/load both regular and global blueprints, step through defaults directly in the quick palette, and keep selections scoped per world so your builds stay organized.
- **Default templates:** The new “Default blueprints” dropdown exposes curated layouts (Guidehouse, crafting bounds) that you can select for placement or import into your personal library—defaults are read-only and never mixed into the user list.
- **Selection & placement helpers:** Drag-select paint, highlight specific layers, and use quick palette controls to transform blueprints (rotate, flip, move) without leaving the overlay.

## Quick start
1. Open the quick palette (default key bind from Controls menu) and expand the “Blueprints” panel.
2. Choose a blueprint from the user list or select one of the bundled defaults, then hit “Load default” or “Load” to begin placement.
3. Use the paint tools to sketch new layouts; toggles in the Grid UI let you enable/disable painting and show/hide the overlay independently.
4. Save your creations via the “Save selection as” input or the Save button, then reuse them across worlds.

## Controls table
| Action | Description |
| --- | --- |
| `gridmod.toggle` | Toggle the grid overlay on/off. |
| `gridmod.chunk.span` | Cycle chunk display spans (8 / 16 / 32). |
| `gridmod.paint.toggle` | Enable/disable paint logic so the system ignores input. |
| `gridmod.paint.overlay.toggle` | Toggle the visual paint overlay without affecting the active brush. |
| `gridmod.paint.clear` | Clear the entire paint canvas for the current selection scope. |
| `gridmod.brush.plus` / `gridmod.brush.minus` | Increase/decrease brush size. |
| `gridmod.paint.erase` | Hold to switch to erase mode temporarily. |
| `gridmod.paint.bpsave` | Quick-save the currently selected blueprint. |
| `gridmod.paint.bpload` | Quick-load/place the current blueprint without opening the UI. |
| `gridmod.bp.rotatecw` / `gridmod.bp.rotateccw` / `gridmod.bp.flip` | Rotate and flip blueprints while placing. |
| `gridmod.openui` | Open the full Grid UI form (for config, colors, settlement controls). |

Bind these controls in Necesse's Controls menu for instant access; the defaults are unbound so you can tailor the shortcuts to your workflow.
