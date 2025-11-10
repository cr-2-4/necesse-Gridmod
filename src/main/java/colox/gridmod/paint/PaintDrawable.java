package colox.gridmod.paint;

import colox.gridmod.config.GridConfig;
import colox.gridmod.input.GridKeybinds;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.window.WindowManager;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.level.maps.Level;

import java.awt.Point;
import java.util.List;

public class PaintDrawable implements necesse.gfx.drawables.Drawable {
    private final Level level;
    private final GameCamera camera;

    public PaintDrawable(Level level, GameCamera camera) {
        this.level = level;
        this.camera = camera;
    }

    @Override
    public void draw(TickManager tickManager) {
        // Safety: visibility gate (also gated in hook)
        if (!GridConfig.paintVisible) return;

        final int tileSize = GridConfig.tileSize;

        // Colors from config
        final float pA = GridConfig.paintAlpha;
        final float pR = GridConfig.paintR, pG = GridConfig.paintG, pB = GridConfig.paintB;
        final float eA = GridConfig.eraseAlpha;
        final float eR = GridConfig.eraseR, eG = GridConfig.eraseG, eB = GridConfig.eraseB;
        final float sA = GridConfig.selectionAlpha;
        final float sR = GridConfig.selectionR, sG = GridConfig.selectionG, sB = GridConfig.selectionB;
        final float gA = GridConfig.bpGhostAlpha;
        final float gR = GridConfig.bpGhostR, gG = GridConfig.bpGhostG, gB = GridConfig.bpGhostB;

        int camX = camera.getX();
        int camY = camera.getY();
        int viewW = camera.getWidth();
        int viewH = camera.getHeight();
        int startTileX = camX / tileSize - 1;
        int startTileY = camY / tileSize - 1;
        int endTileX   = (camX + viewW) / tileSize + 1;
        int endTileY   = (camY + viewH) / tileSize + 1;

        // committed paint tiles
        for (long[] p : PaintState.iterateSnapshot()) {
            int tx = (int)p[0];
            int ty = (int)p[1];
            if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;

            int px = tx * tileSize - camX;
            int py = ty * tileSize - camY;
            GameResources.empty.initDraw()
                .size(tileSize, tileSize)
                .pos(px, py, false)
                .color(pR, pG, pB, pA)
                .draw();
        }

        // brush preview (only when painting and not placing/choosing selection)
        if (PaintState.enabled && !BlueprintPlacement.active && !SelectionState.isActive()) {
            int[] tile = MouseTileUtil.getMouseTile(tileSize);
            if (tile != null) {
                int s = PaintState.getBrush();
                int half = (s - 1) / 2;
                int tx0 = tile[0] - half;
                int ty0 = tile[1] - half;
                int x = tx0 * tileSize - camX;
                int y = ty0 * tileSize - camY;
                int w = s * tileSize;
                int h = s * tileSize;

                // If erase modifier or RMB is held, preview in erase color
                boolean erasePreview = false;
                try {
                    Input input = WindowManager.getWindow() != null ? WindowManager.getWindow().getInput() : null;
                    if (input != null) {
                        // Right mouse held
                        try { if (input.isKeyDown(-99) || input.isPressed(-99)) erasePreview = true; } catch (Throwable ignored) {}
                        // Erase mod key held
                        int eraseKey = (GridKeybinds.PAINT_ERASE_MOD != null) ? GridKeybinds.PAINT_ERASE_MOD.getKey() : -1;
                        if (eraseKey != -1) {
                            try { if (input.isKeyDown(eraseKey)) erasePreview = true; } catch (Throwable ignored) {}
                        }
                    }
                } catch (Throwable ignored) {}

                float rr = erasePreview ? eR : pR;
                float gg = erasePreview ? eG : pG;
                float bb = erasePreview ? eB : pB;
                float aa = erasePreview ? eA : pA;
                float oa = Math.min(1f, aa + 0.2f);

                GameResources.empty.initDraw().size(w, 2).pos(x, y, false).color(rr, gg, bb, oa).draw();
                GameResources.empty.initDraw().size(w, 2).pos(x, y + h - 2, false).color(rr, gg, bb, oa).draw();
                GameResources.empty.initDraw().size(2, h).pos(x, y, false).color(rr, gg, bb, oa).draw();
                GameResources.empty.initDraw().size(2, h).pos(x + w - 2, y, false).color(rr, gg, bb, oa).draw();
            }
        }

        // blueprint ghost while placing
        if (BlueprintPlacement.active) {
            int[] anchor = MouseTileUtil.getMouseTile(tileSize);
            if (anchor != null) {
                List<int[]> ghost = BlueprintPlacement.transformedAt(anchor[0], anchor[1]);
                for (int[] t : ghost) {
                    int tx = t[0], ty = t[1];
                    if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;
                    int px = tx * tileSize - camX;
                    int py = ty * tileSize - camY;
                    GameResources.empty.initDraw()
                        .size(tileSize, tileSize)
                        .pos(px, py, false)
                        .color(gR, gG, gB, gA)
                        .draw();
                }
            }
        }

        // selection highlight (selected cells after release)
        if (SelectionState.getSelectedCount() > 0) {
            for (long[] p : SelectionState.getSelectedPoints()) {
                int tx = (int)p[0], ty = (int)p[1];
                if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;
                int px = tx * tileSize - camX;
                int py = ty * tileSize - camY;
                GameResources.empty.initDraw()
                    .size(tileSize, tileSize)
                    .pos(px, py, false)
                    .color(sR, sG, sB, sA)
                    .draw();
            }
        }

        // selection outline while dragging
        if (SelectionState.isActive() && SelectionState.isDragging()) {
            float oa = sA; // use configured selection alpha
            switch (SelectionState.getMode()) {
                case RECT: {
                    int tx0 = SelectionState.getDragStartTx();
                    int ty0 = SelectionState.getDragStartTy();
                    int tx1 = SelectionState.getDragEndTx();
                    int ty1 = SelectionState.getDragEndTy();
                    int xmin = Math.min(tx0, tx1);
                    int ymin = Math.min(ty0, ty1);
                    int xmax = Math.max(tx0, tx1);
                    int ymax = Math.max(ty0, ty1);
                    int x = xmin * tileSize - camX;
                    int y = ymin * tileSize - camY;
                    int w = (xmax - xmin + 1) * tileSize;
                    int h = (ymax - ymin + 1) * tileSize;

                    GameResources.empty.initDraw().size(w, 2).pos(x, y, false).color(sR, sG, sB, oa).draw();
                    GameResources.empty.initDraw().size(w, 2).pos(x, y + h - 2, false).color(sR, sG, sB, oa).draw();
                    GameResources.empty.initDraw().size(2, h).pos(x, y, false).color(sR, sG, sB, oa).draw();
                    GameResources.empty.initDraw().size(2, h).pos(x + w - 2, y, false).color(sR, sG, sB, oa).draw();
                    break;
                }
                case EDGE:
                case EDGE_FILL: {
                    for (long[] p : SelectionState.getHoverStrokePoints()) {
                        int tx = (int)p[0], ty = (int)p[1];
                        if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;
                        int x = tx * tileSize - camX;
                        int y = ty * tileSize - camY;
                        drawCellEdges(x, y, tileSize, sR, sG, sB, oa);
                    }
                    break;
                }
                case LASSO_FILL: {
                    List<Point> pts = SelectionState.getLassoPath();
                    for (int i = 1; i < pts.size(); i++) {
                        Point aP = pts.get(i - 1);
                        Point bP = pts.get(i);
                        int ax = aP.x * tileSize - camX + tileSize / 2;
                        int ay = aP.y * tileSize - camY + tileSize / 2;
                        int bx = bP.x * tileSize - camX + tileSize / 2;
                        int by = bP.y * tileSize - camY + tileSize / 2;
                        drawThickLine(ax, ay, bx, by, 2, sR, sG, sB, oa);
                    }
                    break;
                }
                default: break;
            }
        }
    }

    private void drawCellEdges(int x, int y, int size, float r, float g, float b, float a) {
        GameResources.empty.initDraw().size(size, 2).pos(x, y, false).color(r, g, b, a).draw();
        GameResources.empty.initDraw().size(size, 2).pos(x, y + size - 2, false).color(r, g, b, a).draw();
        GameResources.empty.initDraw().size(2, size).pos(x, y, false).color(r, g, b, a).draw();
        GameResources.empty.initDraw().size(2, size).pos(x + size - 2, y, false).color(r, g, b, a).draw();
    }

    private void drawThickLine(int x0, int y0, int x1, int y1, int thickness, float r, float g, float b, float a) {
        int dx = x1 - x0;
        int dy = y1 - y0;
        if (Math.abs(dx) > Math.abs(dy)) {
            if (x0 > x1) { int t = x0; x0 = x1; x1 = t; }
            int w = Math.max(2, x1 - x0 + 1);
            int h = Math.max(2, thickness);
            GameResources.empty.initDraw().size(w, h).pos(x0, y0 - h / 2, false).color(r, g, b, a).draw();
        } else {
            if (y0 > y1) { int t = y0; y0 = y1; y1 = t; }
            int w = Math.max(2, thickness);
            int h = Math.max(2, y1 - y0 + 1);
            GameResources.empty.initDraw().size(w, h).pos(x0 - w / 2, y0, false).color(r, g, b, a).draw();
        }
    }
}
