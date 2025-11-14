package colox.gridmod.overlay;

/*
// ===========================================================================
// PURPOSE: Handle the main show/hide toggle of the grid. Single source of
//          truth is GridConfig.gridEnabled so UI & hotkey stay in sync.
// ===========================================================================
*/

public final class GridToggle {
    private GridToggle() {}

    public static boolean isEnabled() {
        return colox.gridmod.config.GridConfig.gridEnabled;
    }

    public static void setEnabled(boolean enabled) {
        colox.gridmod.config.GridConfig.gridEnabled = enabled;
        colox.gridmod.config.GridConfig.markDirty();
        colox.gridmod.config.GridConfig.saveIfDirty();
    }

    public static void tick() {
        try {
            if (colox.gridmod.input.GridKeybinds.GRID_TOGGLE != null
                && colox.gridmod.input.GridKeybinds.GRID_TOGGLE.isPressed()) {
                setEnabled(!isEnabled());
            }
        } catch (Throwable ignored) {}
    }
}
