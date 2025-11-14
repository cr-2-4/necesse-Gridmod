# Vanilla reference index

This quick map highlights the Necesse engine classes the mod touches; each path is the expected engine package path (the actual sources live in the Necesse runtime, not in this repo).

## UI bases
- [necesse.engine.window.WindowManager](necesse/engine/window/WindowManager.java) – singleton entry point for the current HUD, input stream, and camera-aware sizing; we call this to center forms, read mouse state, and gate UI-safe input.
- [necesse.engine.window.GameWindow](necesse/engine/window/GameWindow.java) – per-screen window that exposes the active `GameCamera` and raw `Input`; our paint/blueprint loops query it for mouse clicks and to sync cursor position.
- [necesse.engine.input.Input](necesse/engine/input/Input.java) – raw keyboard/mouse state for the current window; the paint hotkeys and quick palette consume it to drive painting, selection, and blueprint placement.

## Registries
- [necesse.engine.save.LoadData](necesse/engine/save/LoadData.java) – engine helper for reading structured data; we rely on it when loading `grid_settings.txt`, `paint_state.txt`, and blueprint snapshots.
- [necesse.engine.save.SaveData](necesse/engine/save/SaveData.java) – counterpart to `LoadData` that serializes configs, paint layers, and blueprints back to the mods-data folder.

## World / tile
- [necesse.level.maps.Level](necesse/level/maps/Level.java) – the active world map; overlays, painting, and blueprint placement all operate relative to the current `Level`.
- [necesse.gfx.camera.GameCamera](necesse/gfx/camera/GameCamera.java) – camera used to translate between tiles/screen coords; we store it in `MouseTileUtil` so paints and blueprints track the camera view.
- [necesse.entity.mobs.PlayerMob](necesse/entity/mobs/PlayerMob.java) – the player's mob; the ByteBuddy hook targets the local player so overlays follow their draw cycle.
- [necesse.engine.world.worldData.SettlementsWorldData](necesse/engine/world/worldData/SettlementsWorldData.java) – settlement metadata provider; we query it to draw configurable settlement bounds when that option is enabled.
