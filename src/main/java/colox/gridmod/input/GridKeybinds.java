package colox.gridmod.input;

import colox.gridmod.config.GridConfig;
import colox.gridmod.paint.PaintState;
import necesse.engine.input.Control;
import necesse.engine.localization.message.StaticMessage;

// ===========================================================================
// PURPOSE: Single source of truth for ALL mod keybinds + per-frame polling.
// Adds paint overlay toggle key:
//   - PAINT_VIS_TOGGLE: toggles GridConfig.paintVisible (draw only).
// ===========================================================================
public final class GridKeybinds {

    // Debounce: prevents “hold key = multiple opens”
    private static boolean openLatch = false;

    private GridKeybinds() {}

    // ** UI
    public static Control GRID_OPEN_UI;

    // * Grid toggles
    public static Control GRID_TOGGLE;
    public static Control GRID_ALPHA_PLUS;
    public static Control GRID_ALPHA_MINUS;
    public static Control GRID_CHUNK_TOGGLE;
    public static Control GRID_CHUNK_SPAN;
    public static Control GRID_CHUNK_ALPHA_PLUS;
    public static Control GRID_CHUNK_ALPHA_MINUS;
    public static Control GRID_SUBCHUNK_TOGGLE;
    public static Control GRID_SUBCHUNK_ALPHA_PLUS;
    public static Control GRID_SUBCHUNK_ALPHA_MINUS;

    // * Paint mode (universal)
    public static Control PAINT_TOGGLE;      // enable/disable painting logic (input gate)
    public static Control PAINT_CLEAR;
    public static Control BRUSH_PLUS;
    public static Control BRUSH_MINUS;

    // ** Paint overlay visibility (draw gate)
    public static Control PAINT_VIS_TOGGLE;  // <--- NEW

    // ** Paint extras
    public static Control PAINT_ERASE_MOD; // hold to erase while painting
    public static Control PAINT_BP_SAVE;   // quick blueprint save
    public static Control PAINT_BP_LOAD;   // quick blueprint load/placement

    // ** Blueprint transform controls
    public static Control BP_ROTATE_CW;    // Rotate clockwise
    public static Control BP_ROTATE_CCW;   // Counter-clockwise
    public static Control BP_FLIP;         // Flip mirror (horizontal pre-rotate)

    // ** Settlement overlay controls
    public static Control SETTLEMENT_PLACE;
    public static Control SETTLEMENT_TOGGLE;
    public static Control SETTLEMENT_TIER_CYCLE;

    // -----------------------------------------------------------------------
    // [REGISTER] (call once in @ModEntry.init)
    // DEFAULTS: 0 means "NOT SET" – bind them in the Controls menu.
    // -----------------------------------------------------------------------
    public static void register() {
        // UI
        GRID_OPEN_UI = Control.addModControl(
            new Control(0, "gridmod.openui", new StaticMessage("Open Grid UI"), "gridmod")
        );

        // Grid
        GRID_TOGGLE      = Control.addModControl(new Control(0, "gridmod.toggle",            new StaticMessage("Toggle Grid"),          "gridmod"));
        GRID_ALPHA_PLUS  = Control.addModControl(new Control(0, "gridmod.alpha.plus",        new StaticMessage("Grid alpha +"),         "gridmod"));
        GRID_ALPHA_MINUS = Control.addModControl(new Control(0, "gridmod.alpha.minus",       new StaticMessage("Grid alpha -"),         "gridmod"));

        GRID_CHUNK_TOGGLE      = Control.addModControl(new Control(0, "gridmod.chunk.toggle",     new StaticMessage("Chunk lines"),        "gridmod"));
        GRID_CHUNK_SPAN        = Control.addModControl(new Control(0, "gridmod.chunk.span",       new StaticMessage("Chunk span 8/16/32"), "gridmod"));
        GRID_CHUNK_ALPHA_PLUS  = Control.addModControl(new Control(0, "gridmod.chunkalpha.plus",  new StaticMessage("Chunk alpha +"),      "gridmod"));
        GRID_CHUNK_ALPHA_MINUS = Control.addModControl(new Control(0, "gridmod.chunkalpha.minus", new StaticMessage("Chunk alpha -"),      "gridmod"));

        GRID_SUBCHUNK_TOGGLE      = Control.addModControl(new Control(0, "gridmod.subchunk.toggle",     new StaticMessage("Sub-chunk lines"), "gridmod"));
        GRID_SUBCHUNK_ALPHA_PLUS  = Control.addModControl(new Control(0, "gridmod.subchunkalpha.plus",  new StaticMessage("Sub alpha +"),     "gridmod"));
        GRID_SUBCHUNK_ALPHA_MINUS = Control.addModControl(new Control(0, "gridmod.subchunkalpha.minus", new StaticMessage("Sub alpha -"),     "gridmod"));

        // Paint (toggle/clear/brush)
        PAINT_TOGGLE     = Control.addModControl(new Control(0, "gridmod.paint.toggle", new StaticMessage("Paint: Toggle"), "gridmod"));
        PAINT_CLEAR      = Control.addModControl(new Control(0, "gridmod.paint.clear",  new StaticMessage("Paint: Clear"),  "gridmod"));
        BRUSH_PLUS       = Control.addModControl(new Control(0, "gridmod.brush.plus",   new StaticMessage("Brush size +"),  "gridmod"));
        BRUSH_MINUS      = Control.addModControl(new Control(0, "gridmod.brush.minus",  new StaticMessage("Brush size -"),  "gridmod"));

        // Paint overlay visibility (draw gate)
        PAINT_VIS_TOGGLE = Control.addModControl(new Control(0, "gridmod.paint.overlay.toggle", new StaticMessage("Paint: Toggle overlay"), "gridmod"));

        // Paint (erase + blueprint quick actions)
        PAINT_ERASE_MOD = Control.addModControl(new Control(0, "gridmod.paint.erase",   new StaticMessage("Paint: Erase (hold)"), "gridmod"));
        PAINT_BP_SAVE   = Control.addModControl(new Control(0, "gridmod.paint.bpsave",  new StaticMessage("Blueprint: Quick save"), "gridmod"));
        PAINT_BP_LOAD   = Control.addModControl(new Control(0, "gridmod.paint.bpload",  new StaticMessage("Blueprint: Quick load/place"), "gridmod"));

        // Blueprint transforms
        BP_ROTATE_CW   = Control.addModControl(new Control(0, "gridmod.bp.rotatecw",  new StaticMessage("Blueprint: Rotate CW"),  "gridmod"));
        BP_ROTATE_CCW  = Control.addModControl(new Control(0, "gridmod.bp.rotateccw", new StaticMessage("Blueprint: Rotate CCW"), "gridmod"));
        BP_FLIP        = Control.addModControl(new Control(0, "gridmod.bp.flip",      new StaticMessage("Blueprint: Flip"),       "gridmod"));

        // Settlement overlay
        SETTLEMENT_PLACE      = Control.addModControl(new Control(0, "gridmod.settlement.place",      new StaticMessage("Settlement: Place here"),     "gridmod"));
        SETTLEMENT_TOGGLE     = Control.addModControl(new Control(0, "gridmod.settlement.toggle",     new StaticMessage("Settlement: Toggle overlay"), "gridmod"));
        SETTLEMENT_TIER_CYCLE = Control.addModControl(new Control(0, "gridmod.settlement.tier.cycle", new StaticMessage("Settlement: Cycle tier"),     "gridmod"));
    }

    // -----------------------------------------------------------------------
    // [POLL METHOD] – central place for toggle style actions
    // -----------------------------------------------------------------------
    public static void poll() {
        try {
            // UI TOGGLE (debounced, using isDown for reliability)
            if (GRID_OPEN_UI != null) {
                boolean down = GRID_OPEN_UI.isDown();
                if (down && !openLatch) {
                    openLatch = true;
                    colox.gridmod.ui.GridUI.toggle();
                }
                if (!down) {
                    openLatch = false;
                }
            }

            // Paint visibility toggle (draw only)
            if (PAINT_VIS_TOGGLE != null && PAINT_VIS_TOGGLE.isPressed()) {
                GridConfig.togglePaintVisible();
                System.out.println("[GridMod] Paint overlay visible = " + GridConfig.paintVisible);
            }

            // Paint enable/clear/brush size (input gate + helpers)
            if (PAINT_TOGGLE != null && PAINT_TOGGLE.isPressed()) {
                PaintState.toggle();
                System.out.println("[GridMod] Paint enabled = " + PaintState.enabled);
            }
            if (PAINT_CLEAR != null && PAINT_CLEAR.isPressed()) {
                PaintState.clear();
                System.out.println("[GridMod] Cleared paint points.");
            }
            if (BRUSH_PLUS != null && BRUSH_PLUS.isPressed()) {
                PaintState.incBrush();
                System.out.println("[GridMod] Brush size -> " + PaintState.getBrush());
            }
            if (BRUSH_MINUS != null && BRUSH_MINUS.isPressed()) {
                PaintState.decBrush();
                System.out.println("[GridMod] Brush size -> " + PaintState.getBrush());
            }

            // (Other grid/settlement style keys remain in their existing places.)
        } catch (Throwable t) {
            System.out.println("[GridUI] poll error: " + t);
        }
    }
}
