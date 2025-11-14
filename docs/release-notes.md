# Release Notes

## GridMod 1.1

### Highlights
- **Defaults dropdown:** Official `.gridpaint` templates live in a dedicated dropdown so defaults never mix into your personal library and can be previewed without editing.
- **Per-world persistence:** `paint_state.txt` and `grid_settings.txt` now live under `mods-data/colox.gridmod/worlds/<world GUID>/…`, so each save keeps its own brush, selection, and settlement settings.
- **Quick palette polish:** Blueprint controls live entirely inside the quick palette overlay now, the main UI simply shows paint controls and status, and the global blueprint mini-panel gained fully editable name input plus a “New” button.
- **Selection highlighting:** Selected tiles now glow like hover highlights, honoring the active paint layers so you can see exactly what will move or fill.
- **Blueprint placement fixes:** Default blueprints load from the bundled manifest and are treated as read-only so they can’t be accidentally deleted, and painting/placement no longer stay locked when the quick palette is open.

### Bug fixes
- Prevented the paint/blueprint loop from suppressing placement when the palette was expanded.
- Made the top layer filter cover wall-mounted lighting so selection can target wall lights explicitly.
- Reworked quick palette/global blueprint controls to avoid stale text refreshes, ensuring name inputs stay writable.
