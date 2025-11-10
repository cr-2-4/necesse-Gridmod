package colox.gridmod;

import colox.gridmod.config.GridConfig;
import colox.gridmod.input.GridKeybinds;
import colox.gridmod.paint.PaintState;
import necesse.engine.modLoader.annotations.ModEntry;

// ===========================================================================
// ********** THIS IS TOP OF CLASS (VERY IMPORTANT) ***************************
// PURPOSE: Mod entry — register keybinds & load configs/state.
// HOW TO JUMP AROUND:
//   - Search: [init] for startup wiring.
// STAR LEGEND:
//   ***  = critical
//   **   = common
//   *    = helpful
// ===========================================================================

@ModEntry
public class GridMod {

    // *** Called once on mod load
    public void init() {
        // *** Keybinds: single source of truth
        GridKeybinds.register();

        // ** Config / state
        GridConfig.load();
        PaintState.load();

        // * Optional: background hotkey ticker (not needed if you poll in overlay hook)
        // GridUI.startHotkeyTicker();
    }

    public void postInit() {
        // hooks for later
    }
}
