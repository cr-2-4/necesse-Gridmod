package colox.gridmod.overlay;

/*
// ===========================================================================
 // PURPOSE: Handle style hotkeys and mutate GridConfig.
 // ===========================================================================
*/

public final class GridStyleControls {
    public static void tick() {
        try {
            if (colox.gridmod.input.GridKeybinds.GRID_ALPHA_PLUS != null
                && colox.gridmod.input.GridKeybinds.GRID_ALPHA_PLUS.isPressed()) {
                colox.gridmod.config.GridConfig.addAlpha(+0.05f);
            }
            if (colox.gridmod.input.GridKeybinds.GRID_ALPHA_MINUS != null
                && colox.gridmod.input.GridKeybinds.GRID_ALPHA_MINUS.isPressed()) {
                colox.gridmod.config.GridConfig.addAlpha(-0.05f);
            }

            if (colox.gridmod.input.GridKeybinds.GRID_CHUNK_TOGGLE != null
                && colox.gridmod.input.GridKeybinds.GRID_CHUNK_TOGGLE.isPressed()) {
                colox.gridmod.config.GridConfig.showChunkLines = !colox.gridmod.config.GridConfig.showChunkLines;
            }
            if (colox.gridmod.input.GridKeybinds.GRID_CHUNK_SPAN != null
                && colox.gridmod.input.GridKeybinds.GRID_CHUNK_SPAN.isPressed()) {
                colox.gridmod.config.GridConfig.cycleChunkSpan();
            }
            if (colox.gridmod.input.GridKeybinds.GRID_CHUNK_ALPHA_PLUS != null
                && colox.gridmod.input.GridKeybinds.GRID_CHUNK_ALPHA_PLUS.isPressed()) {
                colox.gridmod.config.GridConfig.addChunkAlpha(+0.05f);
            }
            if (colox.gridmod.input.GridKeybinds.GRID_CHUNK_ALPHA_MINUS != null
                && colox.gridmod.input.GridKeybinds.GRID_CHUNK_ALPHA_MINUS.isPressed()) {
                colox.gridmod.config.GridConfig.addChunkAlpha(-0.05f);
            }

            if (colox.gridmod.input.GridKeybinds.GRID_SUBCHUNK_TOGGLE != null
                && colox.gridmod.input.GridKeybinds.GRID_SUBCHUNK_TOGGLE.isPressed()) {
                colox.gridmod.config.GridConfig.showSubChunkLines = !colox.gridmod.config.GridConfig.showSubChunkLines;
            }
            if (colox.gridmod.input.GridKeybinds.GRID_SUBCHUNK_ALPHA_PLUS != null
                && colox.gridmod.input.GridKeybinds.GRID_SUBCHUNK_ALPHA_PLUS.isPressed()) {
                colox.gridmod.config.GridConfig.addSubAlpha(+0.05f);
            }
            if (colox.gridmod.input.GridKeybinds.GRID_SUBCHUNK_ALPHA_MINUS != null
                && colox.gridmod.input.GridKeybinds.GRID_SUBCHUNK_ALPHA_MINUS.isPressed()) {
                colox.gridmod.config.GridConfig.addSubAlpha(-0.05f);
            }
        } catch (Throwable ignored) {}
    }
}
