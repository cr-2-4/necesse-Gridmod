package colox.gridmod.paint;

import colox.gridmod.config.GridConfig;
import colox.gridmod.input.GridKeybinds;
import colox.gridmod.ui.GridUI; // UI-safe gate
import colox.gridmod.ui.PaintQuickPaletteOverlay;
import necesse.engine.GlobalData;
import necesse.engine.input.Input;
import necesse.engine.input.InputPosition;
import necesse.engine.state.State;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.forms.FormManager;
import necesse.level.maps.Level;

import java.lang.reflect.Method;
import java.util.List;

public final class PaintControls {
    private PaintControls() {}

    // After stamping a blueprint, block normal brush painting until LMB goes up
    private static boolean suppressPaintUntilLmbUp = false;

    private static String currentBlueprintName() {
        String n = colox.gridmod.config.GridConfig.selectedBlueprint;
        return (n == null || n.isBlank()) ? "quick" : n.trim();
    }

    public static void tick(Level level, GameCamera camera) {
        PaintQuickPaletteOverlay.tick(PaintState.enabled);
        // UI-safe: If Grid UI is open, ignore all input (paint/placement/selection)
        if (GridUI.isOpen()) return;

        // Pause-safe: ignore ALL while the game is paused
        if (isGamePaused()) {
            try {
                Input in = WindowManager.getWindow().getInput();
                if (in != null && in.isKeyDown(-100)) {
                    suppressPaintUntilLmbUp = true;
                }
            } catch (Throwable ignored) {}
            return;
        }

        // Sync camera for mouse->tile conversion
        MouseTileUtil.setCamera(camera);
        Input input = WindowManager.getWindow().getInput();

        // ===== Settlement overlay keys (chunk-based) =====
        handleSettlementKeys(input);

        // Reset suppression once LMB is released
        if (!input.isKeyDown(-100)) {
            suppressPaintUntilLmbUp = false;
        }

        // Selection gate: when active, selection owns the mouse
        if (SelectionState.isActive()) {
            handleSelectionInput(input);
            return; // Selection suppresses placement & paint while active
        }

        // Blueprint quick save/load
        String bpName = currentBlueprintName();

        if (GridKeybinds.PAINT_BP_SAVE != null && GridKeybinds.PAINT_BP_SAVE.isPressed()) {
            PaintBlueprints.saveBlueprint(bpName);
        }
        if (GridKeybinds.PAINT_BP_LOAD != null && GridKeybinds.PAINT_BP_LOAD.isPressed()) {
            List<BlueprintPlacement.BlueprintTile> rel = PaintBlueprints.loadRelative(bpName);
            if (!rel.isEmpty()) {
                BlueprintPlacement.begin(rel);
                if (input.isKeyDown(-100)) suppressPaintUntilLmbUp = true;
                System.out.println("[GridMod] Placement mode ON for '" + bpName + "'");
            }
        }

        boolean toggleClicked = PaintQuickPaletteOverlay.consumeToggleClick();
        boolean uiHover = PaintQuickPaletteOverlay.isMouseOverUi() || isMouseOverFormManager();
        boolean paletteExpanded = PaintQuickPaletteOverlay.isPaletteExpanded();
        boolean uiBlock = toggleClicked || uiHover || paletteExpanded;

        // Placement mode
        boolean rightClickPress = input.isPressed(-99); // RIGHT-CLICK edge
        if (BlueprintPlacement.active) {
            if (rightClickPress) {
                BlueprintPlacement.cancel();
                if (input.isKeyDown(-100)) suppressPaintUntilLmbUp = true;
                System.out.println("[GridMod] Placement cancelled (RMB).");
                return;
            }

            if (GridKeybinds.BP_ROTATE_CW != null && GridKeybinds.BP_ROTATE_CW.isPressed()) {
                BlueprintPlacement.rotateCW();
            }
            if (GridKeybinds.BP_ROTATE_CCW != null && GridKeybinds.BP_ROTATE_CCW.isPressed()) {
                BlueprintPlacement.rotateCCW();
            }
            if (GridKeybinds.BP_FLIP != null && GridKeybinds.BP_FLIP.isPressed()) {
                BlueprintPlacement.toggleFlip();
            }

            if (input.isPressed(-100)) {
                if (uiBlock) {
                    suppressPaintUntilLmbUp = true;
                    return;
                }
                int tileSize = GridConfig.tileSize;
                int[] tile = MouseTileUtil.getMouseTile(tileSize);
                if (tile != null) {
                    List<BlueprintPlacement.BlueprintTile> abs = BlueprintPlacement.transformedAt(tile[0], tile[1]);
                    for (BlueprintPlacement.BlueprintTile p : abs) PaintState.add(p.dx, p.dy, p.categoryId);
                    PaintState.markDirty();
                    PaintState.saveIfDirty();
                    suppressPaintUntilLmbUp = true;
                    System.out.println("[GridMod] Blueprint placed at " + tile[0] + "," + tile[1]
                            + " tiles=" + abs.size() + " (multi-stamp active; RMB to cancel)");
                }
            }
            if (uiBlock) {
                suppressPaintUntilLmbUp = true;
            }
            return;
        }

        if (uiBlock) {
            suppressPaintUntilLmbUp = true;
            return;
        }

        // Painting (NOTE: visibility is handled in GridOverlayHook; we still allow painting
        // while overlay is hidden, so you can “paint blind” if you want.)
        boolean leftHeld  = input.isKeyDown(-100);
        boolean rightHeld = safeIsRightHeld(input);
        boolean anyHeld   = leftHeld || rightHeld;

        if (suppressPaintUntilLmbUp) {
            PaintState.saveIfDirty();
            return;
        }

        int eraseKey = (GridKeybinds.PAINT_ERASE_MOD != null) ? GridKeybinds.PAINT_ERASE_MOD.getKey() : -1;
        boolean eraseModHeld = eraseKey != -1 && input.isKeyDown(eraseKey);

        int tileSize = GridConfig.tileSize;
        int[] tile = MouseTileUtil.getMouseTile(tileSize);

        if (!PaintState.enabled) {
            PaintState.saveIfDirty();
            return;
        }

        if (anyHeld && tile != null) {
            boolean doErase = rightHeld || eraseModHeld;
            String catId = GridConfig.getActivePaintCategory().id();
            Painter.applyAt(tile[0], tile[1], doErase, catId);
        }

        PaintState.saveIfDirty();
    }

    // -------------------------------------------------------------------
    // Settlement helpers callable from UI (unchanged)
    // -------------------------------------------------------------------
    public static void placeHere() {
        try {
            // Ensure MouseTileUtil has a camera
            try {
                Object window = necesse.engine.window.WindowManager.getWindow();
                if (window != null) {
                    try {
                        java.lang.reflect.Method getCamera = window.getClass().getMethod("getCamera");
                        Object cam = getCamera.invoke(window);
                        if (cam instanceof necesse.gfx.camera.GameCamera) {
                            MouseTileUtil.setCamera((necesse.gfx.camera.GameCamera) cam);
                        }
                    } catch (NoSuchMethodException ignored) {}
                }
            } catch (Throwable ignored) {}

            int[] mouse = MouseTileUtil.getMouseTile(GridConfig.tileSize);

            int tx, ty;
            if (mouse != null) {
                tx = mouse[0];
                ty = mouse[1];
            } else {
                // Fallback: camera center (in tiles)
                Object window = necesse.engine.window.WindowManager.getWindow();
                int cx = 0, cy = 0, cw = 0, ch = 0;
                if (window != null) {
                    try {
                        java.lang.reflect.Method getCamera = window.getClass().getMethod("getCamera");
                        Object cam = getCamera.invoke(window);
                        if (cam instanceof necesse.gfx.camera.GameCamera) {
                            necesse.gfx.camera.GameCamera c = (necesse.gfx.camera.GameCamera) cam;
                            cx = c.getX(); cy = c.getY(); cw = c.getWidth(); ch = c.getHeight();
                        }
                    } catch (Throwable ignored) {}
                }
                int px = cx + cw / 2;
                int py = cy + ch / 2;
                tx = Math.max(0, px / GridConfig.tileSize);
                ty = Math.max(0, py / GridConfig.tileSize);
            }

            int chunkOriginTx = Math.floorDiv(tx, 16) * 16;
            int chunkOriginTy = Math.floorDiv(ty, 16) * 16;

            int sideTiles = GridConfig.currentTierSideTiles();
            int centerTx = chunkOriginTx + 8;
            int centerTy = chunkOriginTy + 8;

            GridConfig.settlementAnchorTx = centerTx - (sideTiles / 2);
            GridConfig.settlementAnchorTy = centerTy - (sideTiles / 2);

            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        } catch (Throwable ignored) {}
    }

    public static void recenterHere() {
        try {
            placeHere();
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
        } catch (Throwable ignored) {}
    }

    public static void placeHereAndEnable() {
        placeHere();
        GridConfig.settlementEnabled = true;
        GridConfig.markDirty();
        GridConfig.saveIfDirty();
        System.out.println("[GridMod] Settlement placed+enabled via UI at topLeft=("
                + GridConfig.settlementAnchorTx + "," + GridConfig.settlementAnchorTy + ")");
    }

    private static void handleSettlementKeys(Input input) {
        if (colox.gridmod.input.GridKeybinds.SETTLEMENT_PLACE != null
                && colox.gridmod.input.GridKeybinds.SETTLEMENT_PLACE.isPressed()) {
            placeHereAndEnable();
            return;
        }

        if (colox.gridmod.input.GridKeybinds.SETTLEMENT_TOGGLE != null
                && colox.gridmod.input.GridKeybinds.SETTLEMENT_TOGGLE.isPressed()) {
            boolean prev = GridConfig.settlementEnabled;
            boolean next = !prev;
            GridConfig.settlementEnabled = next;
            if (next && !GridConfig.hasSettlementAnchor()) {
                placeHere();
            }
            GridConfig.markDirty();
            GridConfig.saveIfDirty();
            System.out.println("[GridMod] Settlement overlay = " + GridConfig.settlementEnabled
                    + " anchorTopLeft=(" + GridConfig.settlementAnchorTx + "," + GridConfig.settlementAnchorTy + ")");
        }

        if (colox.gridmod.input.GridKeybinds.SETTLEMENT_TIER_CYCLE != null
                && colox.gridmod.input.GridKeybinds.SETTLEMENT_TIER_CYCLE.isPressed()) {
            GridConfig.cycleSettlementTier();
            System.out.println("[GridMod] Settlement tier -> " + GridConfig.settlementTier
                    + " (chunks per side=" + GridConfig.currentTierChunks() + ")");
        }
    }

    private static void handleSelectionInput(Input input) {
        int tileSize = GridConfig.tileSize;
        int[] tile = MouseTileUtil.getMouseTile(tileSize);

        if (input.isPressed(-99)) {
            SelectionState.clear();
            SelectionState.setMode(SelectionState.Mode.NONE);
            return;
        }

        boolean lmbPressed = input.isPressed(-100);
        boolean lmbDown    = input.isKeyDown(-100);
        boolean lmbUpEdge  = (!lmbDown && !lmbPressed);

        if (lmbPressed && tile != null && !SelectionState.isDragging()) {
            SelectionState.beginDrag(tile[0], tile[1]);
        }

        if (SelectionState.isDragging() && lmbDown && tile != null) {
            SelectionState.updateDrag(tile[0], tile[1]);
        }

        if (SelectionState.isDragging() && lmbUpEdge) {
            SelectionState.endDrag();
        }
    }

    private static boolean safeIsRightHeld(Input input) {
        try { if (input.isKeyDown(-99)) return true; } catch (Throwable ignored) {}
        try { if (input.isPressed(-99)) return true; } catch (Throwable ignored) {}
        return false;
    }

    private static FormManager getCurrentFormManager() {
        State state = GlobalData.getCurrentState();
        if (state == null) return null;
        return state.getFormManager();
    }

    private static boolean isMouseOverFormManager() {
        GameWindow window = WindowManager.getWindow();
        if (window == null) return false;
        InputPosition pos = window.mousePos();
        if (pos == null) return false;
        FormManager manager = getCurrentFormManager();
        return manager != null && manager.isMouseOver(pos);
    }

    // --- Pause detection via reflection (safe fallback = false) ---
    private static boolean isGamePaused() {
        try {
            Class<?> gd = Class.forName("necesse.engine.GlobalData");
            Method getCurrentState = gd.getMethod("getCurrentState");
            Object state = getCurrentState.invoke(null);
            if (state != null) {
                try {
                    Method getTickManager = state.getClass().getMethod("getTickManager");
                    Object tm = getTickManager.invoke(state);
                    if (tm != null) {
                        try {
                            Method isPaused = tm.getClass().getMethod("isPaused");
                            Object res = isPaused.invoke(tm);
                            if (res instanceof Boolean && (Boolean) res) return true;
                        } catch (NoSuchMethodException ignored) {}
                    }
                } catch (NoSuchMethodException ignored) {}
                try {
                    Method isPaused = state.getClass().getMethod("isPaused");
                    Object res = isPaused.invoke(state);
                    if (res instanceof Boolean && (Boolean) res) return true;
                } catch (NoSuchMethodException ignored) {}
                try {
                    Method isRunning = state.getClass().getMethod("isRunning");
                    Object res = isRunning.invoke(state);
                    if (res instanceof Boolean && !((Boolean) res)) return true;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}

        try {
            Class<?> wm = Class.forName("necesse.engine.window.WindowManager");
            Method getWindow = wm.getMethod("getWindow");
            Object window = getWindow.invoke(null);
            if (window != null) {
                try {
                    Method isGamePaused = window.getClass().getMethod("isGamePaused");
                    Object res = isGamePaused.invoke(window);
                    if (res instanceof Boolean && (Boolean) res) return true;
                } catch (NoSuchMethodException ignored) {}
            }
        } catch (Throwable ignored) {}

        return false;
    }
}
