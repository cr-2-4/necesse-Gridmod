package colox.gridmod.overlay;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;

/*
// ===========================================================================
 // PURPOSE: Render all grid lines. Also the ONE place we poll hotkeys per-frame.
 // NOTES:
 //   *** We call GridKeybinds.poll() at the very top (main thread, every frame).
 // ===========================================================================
*/

public class GridDrawable implements necesse.gfx.drawables.Drawable {
    @SuppressWarnings("unused")
    private final Level level;
    private final GameCamera camera;

    public GridDrawable(Level level, GameCamera camera) {
        this.level = level;
        this.camera = camera;
    }

    @Override
    public void draw(TickManager tickManager) {
        // *** ALWAYS poll hotkeys once per frame on the main/game thread
        //     This guarantees the UI open key works consistently.
        colox.gridmod.input.GridKeybinds.poll();

        // toggle + style hotkeys (your existing logic)
        GridToggle.tick();
        GridStyleControls.tick();

        if (!GridToggle.isEnabled()) return;

        int camX = camera.getX();
        int camY = camera.getY();
        int viewW = camera.getWidth();
        int viewH = camera.getHeight();

        final int tileSize = colox.gridmod.config.GridConfig.tileSize;
        int startTileX = camX / tileSize - 1;
        int startTileY = camY / tileSize - 1;
        int endTileX   = (camX + viewW) / tileSize + 1;
        int endTileY   = (camY + viewH) / tileSize + 1;

        final float baseA = colox.gridmod.config.GridConfig.lineAlpha;
        final float r = colox.gridmod.config.GridConfig.r;
        final float g = colox.gridmod.config.GridConfig.g;
        final float b = colox.gridmod.config.GridConfig.b;

        final boolean showChunk = colox.gridmod.config.GridConfig.showChunkLines;
        final int chunkEvery    = colox.gridmod.config.GridConfig.chunkSpanTiles;
        final int chunkThick    = colox.gridmod.config.GridConfig.chunkThickness;
        final float chunkAlpha  = colox.gridmod.config.GridConfig.chunkAlpha;
        final float cr = colox.gridmod.config.GridConfig.cr;
        final float cg = colox.gridmod.config.GridConfig.cg;
        final float cb = colox.gridmod.config.GridConfig.cb;

        final boolean showSub   = colox.gridmod.config.GridConfig.showSubChunkLines;
        final int subEvery      = Math.max(2, colox.gridmod.config.GridConfig.subChunkSpanTiles);
        final int subThick      = colox.gridmod.config.GridConfig.subChunkThickness;
        final float subAlpha    = colox.gridmod.config.GridConfig.subChunkAlpha;
        final float scr = colox.gridmod.config.GridConfig.scr;
        final float scg = colox.gridmod.config.GridConfig.scg;
        final float scb = colox.gridmod.config.GridConfig.scb;

        // vertical lines
        for (int x = startTileX; x <= endTileX; x++) {
            int sx = x * tileSize - camX;
            boolean isChunk = showChunk && (x % chunkEvery == 0);
            boolean isSub   = !isChunk && showSub && (x % subEvery == 0);

            int   useThick = isChunk ? chunkThick : (isSub ? subThick : 1);
            float useA     = isChunk ? chunkAlpha  : (isSub ? subAlpha : baseA);
            float rr       = isChunk ? cr : (isSub ? scr : r);
            float gg       = isChunk ? cg : (isSub ? scg : g);
            float bb       = isChunk ? cb : (isSub ? scb : b);

            GameResources.empty.initDraw()
                .size(useThick, viewH)
                .pos(sx, 0, false)
                .color(rr, gg, bb, useA)
                .draw();
        }

        // horizontal lines
        for (int y = startTileY; y <= endTileY; y++) {
            int sy = y * tileSize - camY;
            boolean isChunk = showChunk && (y % chunkEvery == 0);
            boolean isSub   = !isChunk && showSub && (y % subEvery == 0);

            int   useThick = isChunk ? chunkThick : (isSub ? subThick : 1);
            float useA     = isChunk ? chunkAlpha  : (isSub ? subAlpha : baseA);
            float rr       = isChunk ? cr : (isSub ? scr : r);
            float gg       = isChunk ? cg : (isSub ? scg : g);
            float bb       = isChunk ? cb : (isSub ? scb : b);

            GameResources.empty.initDraw()
                .size(viewW, useThick)
                .pos(0, sy, false)
                .color(rr, gg, bb, useA)
                .draw();
        }
    }
}
