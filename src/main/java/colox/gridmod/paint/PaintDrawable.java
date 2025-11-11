package colox.gridmod.paint;

import colox.gridmod.config.GridConfig;
import colox.gridmod.input.GridKeybinds;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.window.WindowManager;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.Renderer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.Level;

import java.awt.Point;
import java.util.List;

public class PaintDrawable implements necesse.gfx.drawables.Drawable {
    @SuppressWarnings("unused")
    private final Level level;
    private final GameCamera camera;
    private static final FontOptions HOVER_FONT = new FontOptions(18)
            .outline()
            .colorf(1f, 1f, 1f, 1f)
            .shadow(0f, 0f, 0f, 0.85f, 2, 2);
    private static final int TOOLTIP_PADDING = 5;

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
        GridConfig.PaintColor paintColor = GridConfig.getPaintColor(GridConfig.getActivePaintCategory());
        final float pA = paintColor.a;
        final float pR = paintColor.r, pG = paintColor.g, pB = paintColor.b;
        final float eA = GridConfig.eraseAlpha;
        final float eR = GridConfig.eraseR, eG = GridConfig.eraseG, eB = GridConfig.eraseB;
        final float sA = GridConfig.selectionAlpha;
        final float sR = GridConfig.selectionR, sG = GridConfig.selectionG, sB = GridConfig.selectionB;

        int camX = camera.getX();
        int camY = camera.getY();
        int viewW = camera.getWidth();
        int viewH = camera.getHeight();
        int startTileX = camX / tileSize - 1;
        int startTileY = camY / tileSize - 1;
        int endTileX   = (camX + viewW) / tileSize + 1;
        int endTileY   = (camY + viewH) / tileSize + 1;

        List<PaintState.PaintEntry> snapshot = PaintState.iterateSnapshot();
        int[] mouseTile = MouseTileUtil.getMouseTile(tileSize);
        PaintCategory hoverCategory = null;
        int hoverPx = 0;
        int hoverPy = 0;
        boolean hoverMasterEnabled = GridConfig.isHoverLabelsEnabled();
        if (mouseTile != null) {
            PaintState.PaintEntry hovered = PaintState.getPaintEntry(mouseTile[0], mouseTile[1]);
            if (hovered != null) {
                hoverCategory = PaintCategory.byId(hovered.categoryId);
                hoverPx = mouseTile[0] * tileSize - camX;
                hoverPy = mouseTile[1] * tileSize - camY;
            }
        }
        if (hoverCategory != null && (!hoverMasterEnabled || !GridConfig.isHoverCategoryAllowed(hoverCategory))) {
            hoverCategory = null;
        }

        // committed paint tiles
        for (PaintState.PaintEntry p : snapshot) {
            int tx = p.x;
            int ty = p.y;
            if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;

            int px = tx * tileSize - camX;
            int py = ty * tileSize - camY;
            PaintCategory cat = PaintCategory.byId(p.categoryId);
            GridConfig.PaintColor color = GridConfig.getPaintColor(cat);
            GameResources.empty.initDraw()
                .size(tileSize, tileSize)
                .pos(px, py, false)
                .color(color.r, color.g, color.b, color.a)
                .draw();
        }

        if (hoverCategory != null) {
            drawHoverCategoryHighlight(snapshot, hoverCategory, camX, camY, tileSize,
                    startTileX, endTileX, startTileY, endTileY);
        }

        // brush preview (only when painting and not placing/choosing selection)
        if (PaintState.enabled && !BlueprintPlacement.active && !SelectionState.isActive()) {
            int[] tile = mouseTile;
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
            int[] anchor = mouseTile;
            if (anchor != null) {
                List<BlueprintPlacement.BlueprintTile> ghost = BlueprintPlacement.transformedAt(anchor[0], anchor[1]);
                for (BlueprintPlacement.BlueprintTile t : ghost) {
                    int tx = t.dx, ty = t.dy;
                    if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;
                    int px = tx * tileSize - camX;
                    int py = ty * tileSize - camY;
                    PaintCategory cat = PaintCategory.byId(t.categoryId);
                    GridConfig.PaintColor color = GridConfig.getPaintColor(cat);
                    float ga = Math.min(1f, color.a + 0.25f);
                    GameResources.empty.initDraw()
                        .size(tileSize, tileSize)
                        .pos(px, py, false)
                        .color(color.r, color.g, color.b, ga)
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
                String catId = PaintState.getCategory(tx, ty);
                PaintCategory colorCat = PaintCategory.byId(catId);
                GridConfig.PaintColor color = GridConfig.getPaintColor(colorCat);
                GameResources.empty.initDraw()
                    .size(tileSize, tileSize)
                    .pos(px, py, false)
                    .color(color.r, color.g, color.b, color.a)
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

        if (hoverCategory != null) {
            drawHoverTooltip(hoverCategory, hoverPx, hoverPy, tileSize, viewW, viewH);
        }
    }

    private void drawHoverCategoryHighlight(List<PaintState.PaintEntry> snapshot,
                                            PaintCategory category,
                                            int camX, int camY, int tileSize,
                                            int startTileX, int endTileX,
                                            int startTileY, int endTileY) {
        GridConfig.PaintColor base = GridConfig.getPaintColor(category);
        float hiR = Math.min(1f, base.r + 0.15f);
        float hiG = Math.min(1f, base.g + 0.15f);
        float hiB = Math.min(1f, base.b + 0.15f);
        float hiA = Math.min(1f, base.a + 0.35f);
        float edgeA = Math.min(1f, hiA + 0.15f);
        String targetId = category.id();

        for (PaintState.PaintEntry entry : snapshot) {
            if (!targetId.equals(entry.categoryId)) continue;
            int tx = entry.x;
            int ty = entry.y;
            if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;
            int px = tx * tileSize - camX;
            int py = ty * tileSize - camY;
            GameResources.empty.initDraw()
                    .size(tileSize, tileSize)
                    .pos(px, py, false)
                    .color(hiR, hiG, hiB, hiA)
                    .draw();
            drawCellEdges(px, py, tileSize, hiR, hiG, hiB, edgeA);
        }
    }

    private void drawHoverTooltip(PaintCategory category,
                                  int hoverPx, int hoverPy,
                                  int tileSize, int viewW, int viewH) {
        if (FontManager.bit == null) return;
        String label = category.label();
        FontOptions fo = HOVER_FONT;
        int textW = FontManager.bit.getWidthCeil(label, fo);
        int textH = FontManager.bit.getHeightCeil(label, fo);
        int pad = TOOLTIP_PADDING;

        int labelX = hoverPx + tileSize / 2 - textW / 2;
        int labelY = hoverPy - textH - 6;
        if (labelX < pad) labelX = pad;
        if (labelX + textW > viewW - pad) labelX = Math.max(pad, viewW - textW - pad);
        if (labelY < pad) labelY = hoverPy + tileSize + 6;
        if (labelY + textH > viewH - pad) labelY = Math.max(pad, viewH - textH - pad);

        int boxX = labelX - pad;
        int boxY = labelY - pad;
        int boxW = textW + pad * 2;
        int boxH = textH + pad * 2;

        Renderer.initQuadDraw(boxW, boxH)
                .color(0f, 0f, 0f, 0.7f)
                .draw(boxX, boxY);

        GridConfig.PaintColor color = GridConfig.getPaintColor(category);
        float borderA = Math.min(1f, color.a + 0.3f);
        Renderer.initQuadDraw(boxW, 1).color(color.r, color.g, color.b, borderA).draw(boxX, boxY);
        Renderer.initQuadDraw(boxW, 1).color(color.r, color.g, color.b, borderA).draw(boxX, boxY + boxH - 1);
        Renderer.initQuadDraw(1, boxH).color(color.r, color.g, color.b, borderA).draw(boxX, boxY);
        Renderer.initQuadDraw(1, boxH).color(color.r, color.g, color.b, borderA).draw(boxX + boxW - 1, boxY);

        FontManager.bit.drawString((float)labelX, (float)labelY, label, fo);
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
