package colox.gridmod.paint;

import colox.gridmod.config.GridConfig;
import colox.gridmod.input.GridKeybinds;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.input.Input;
import necesse.engine.window.GameWindow;
import necesse.engine.window.WindowManager;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.Renderer;
import necesse.gfx.gameFont.FontManager;
import necesse.gfx.gameFont.FontOptions;
import necesse.level.maps.Level;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class PaintDrawable implements necesse.gfx.drawables.Drawable {
    @SuppressWarnings("unused")
    private final Level level;
    private final GameCamera camera;
    private static final FontOptions HOVER_FONT = new FontOptions(18)
            .outline()
            .colorf(1f, 1f, 1f, 1f)
            .shadow(0f, 0f, 0f, 0.85f, 2, 2);
    private static final int TOOLTIP_PADDING = 5;
    private static final FontOptions HUD_FONT = new FontOptions(16)
            .outline()
            .colorf(1f, 1f, 1f, 1f)
            .shadow(0f, 0f, 0f, 0.9f, 2, 2);
    private static final int HUD_MARGIN = 12;
    private static final int HUD_PADDING = 10;
    private static final int HUD_LINE_GAP = 2;
    private static final int HUD_INDICATOR_SIZE = 10;
    private static final int HUD_SECTION_GAP = 6;

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
        PaintCategory activeCategory = GridConfig.getActivePaintCategory();
        GridConfig.PaintColor paintColor = GridConfig.getPaintColor(activeCategory);
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
        boolean hoverMasterEnabled = GridConfig.isHoverLabelsEnabled();
        List<PaintCategory> hoverCategories = Collections.emptyList();
        int hoverPx = 0;
        int hoverPy = 0;
        if (hoverMasterEnabled && mouseTile != null) {
            List<PaintState.PaintEntry> entries = PaintState.getPaintEntries(mouseTile[0], mouseTile[1]);
            if (!entries.isEmpty()) {
                List<PaintCategory> acceptedCats = new ArrayList<>(entries.size());
                for (PaintState.PaintEntry entry : entries) {
                    PaintCategory cat = PaintCategory.byId(entry.categoryId);
                    if (!GridConfig.isHoverCategoryAllowed(cat)) continue;
                    acceptedCats.add(cat);
                }
                if (!acceptedCats.isEmpty()) {
                    hoverCategories = acceptedCats;
                    hoverPx = mouseTile[0] * tileSize - camX;
                    hoverPy = mouseTile[1] * tileSize - camY;
                }
            }
        }
        PaintCategory hoverHighlight = hoverCategories.isEmpty() ? null : hoverCategories.get(0);
        // committed paint tiles
        for (PaintState.PaintEntry p : snapshot) {
            int tx = p.x;
            int ty = p.y;
            if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;

            int px = tx * tileSize - camX;
            int py = ty * tileSize - camY;
            PaintCategory cat = PaintCategory.byId(p.categoryId);
            GridConfig.PaintColor color = GridConfig.getPaintColor(cat);
            drawPaintMark(px, py, tileSize, cat, color.r, color.g, color.b, color.a);
        }

        if (hoverHighlight != null) {
            drawHoverCategoryHighlight(snapshot, hoverHighlight, camX, camY, tileSize,
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

        drawCategoryCounter(snapshot);

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
                    drawPaintMark(px, py, tileSize, cat, color.r, color.g, color.b, ga);
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

        if (!hoverCategories.isEmpty()) {
            drawHoverTooltip(hoverCategories, hoverPx, hoverPy, tileSize, viewW, viewH);
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
        float edgeA = Math.min(1f, hiA + 0.15f) * category.layer().alphaScale();
        String targetId = category.id();

        for (PaintState.PaintEntry entry : snapshot) {
            if (!targetId.equals(entry.categoryId)) continue;
            int tx = entry.x;
            int ty = entry.y;
            if (tx < startTileX || tx > endTileX || ty < startTileY || ty > endTileY) continue;
            int px = tx * tileSize - camX;
            int py = ty * tileSize - camY;
            drawPaintMark(px, py, tileSize, category, hiR, hiG, hiB, hiA);
            drawCellEdges(px, py, tileSize, hiR, hiG, hiB, edgeA);
        }
    }

    private void drawHoverTooltip(List<PaintCategory> categories,
                                  int hoverPx, int hoverPy,
                                  int tileSize, int viewW, int viewH) {
        if (categories == null || categories.isEmpty() || FontManager.bit == null) return;
        FontOptions fo = HOVER_FONT;
        int lineHeight = FontManager.bit.getHeightCeil("Ag", fo);
        int indicator = 10;
        int lineGap = 2;
        int pad = TOOLTIP_PADDING;

        int textW = 0;
        for (PaintCategory cat : categories) {
            int width = FontManager.bit.getWidthCeil(cat.label(), fo) + indicator + 6;
            if (width > textW) textW = width;
        }
        int textH = categories.size() * lineHeight + Math.max(0, categories.size() - 1) * lineGap;

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
                .color(0f, 0f, 0f, 0.75f)
                .draw(boxX, boxY);

        Renderer.initQuadDraw(boxW, 1).color(1f, 1f, 1f, 0.2f).draw(boxX, boxY);
        Renderer.initQuadDraw(boxW, 1).color(1f, 1f, 1f, 0.2f).draw(boxX, boxY + boxH - 1);
        Renderer.initQuadDraw(1, boxH).color(1f, 1f, 1f, 0.2f).draw(boxX, boxY);
        Renderer.initQuadDraw(1, boxH).color(1f, 1f, 1f, 0.2f).draw(boxX + boxW - 1, boxY);

        int rowY = labelY;
        for (PaintCategory cat : categories) {
            GridConfig.PaintColor color = GridConfig.getPaintColor(cat);
            Renderer.initQuadDraw(indicator, indicator)
                    .color(color.r, color.g, color.b, Math.min(1f, color.a + 0.2f))
                    .draw(labelX, rowY + (lineHeight - indicator) / 2);
            FontManager.bit.drawString((float)(labelX + indicator + 6), (float)rowY, cat.label(), fo);
            rowY += lineHeight + lineGap;
        }
    }

    private void drawCategoryCounter(List<PaintState.PaintEntry> snapshot) {
        GameWindow window = WindowManager.getWindow();
        if (window == null || FontManager.bit == null) return;

        HudCounterData data = resolveHudData(snapshot);
        if (data == null) return;
        if (data.rows.isEmpty()) return;

        FontOptions font = HUD_FONT;
        int lineHeight = FontManager.bit.getHeightCeil("Ag", font);
        int indicatorSize = Math.min(HUD_INDICATOR_SIZE, lineHeight);

        List<String> headerLines = new ArrayList<>();
        if (data.title != null && !data.title.isEmpty()) headerLines.add(data.title);
        if (data.subtitle != null && !data.subtitle.isEmpty()) headerLines.add(data.subtitle);

        int textWidth = 0;
        for (String header : headerLines) {
            textWidth = Math.max(textWidth, FontManager.bit.getWidthCeil(header, font));
        }
        for (CategoryCount row : data.rows) {
            String line = row.category.label() + ": " + row.count;
            int rowWidth = indicatorSize + 6 + FontManager.bit.getWidthCeil(line, font);
            textWidth = Math.max(textWidth, rowWidth);
        }

        int headerHeight = headerLines.isEmpty() ? 0
                : headerLines.size() * lineHeight + (headerLines.size() - 1) * HUD_LINE_GAP;
        int rowsHeight = data.rows.size() * lineHeight + (data.rows.size() - 1) * HUD_LINE_GAP;
        int innerHeight = headerHeight + rowsHeight;
        if (headerHeight > 0 && rowsHeight > 0) innerHeight += HUD_SECTION_GAP;

        int boxWidth = textWidth + HUD_PADDING * 2;
        int boxHeight = innerHeight + HUD_PADDING * 2;
        int boxX = window.getHudWidth() - HUD_MARGIN - boxWidth;
        int boxY = HUD_MARGIN;

        Renderer.initQuadDraw(boxWidth, boxHeight)
                .color(0f, 0f, 0f, 0.65f)
                .draw(boxX, boxY);
        Renderer.initQuadDraw(boxWidth, 1).color(1f, 1f, 1f, 0.3f).draw(boxX, boxY);
        Renderer.initQuadDraw(boxWidth, 1).color(1f, 1f, 1f, 0.3f).draw(boxX, boxY + boxHeight - 1);
        Renderer.initQuadDraw(1, boxHeight).color(1f, 1f, 1f, 0.3f).draw(boxX, boxY);
        Renderer.initQuadDraw(1, boxHeight).color(1f, 1f, 1f, 0.3f).draw(boxX + boxWidth - 1, boxY);

        float textY = boxY + HUD_PADDING;
        for (int i = 0; i < headerLines.size(); i++) {
            FontManager.bit.drawString(boxX + HUD_PADDING, textY, headerLines.get(i), font);
            textY += lineHeight;
            if (i < headerLines.size() - 1) textY += HUD_LINE_GAP;
        }
        if (headerHeight > 0 && !data.rows.isEmpty()) {
            textY += HUD_SECTION_GAP;
        }

        int indicatorX = boxX + HUD_PADDING;
        for (int i = 0; i < data.rows.size(); i++) {
            CategoryCount row = data.rows.get(i);
            GridConfig.PaintColor color = GridConfig.getPaintColor(row.category);
            int indicatorY = (int)(textY + (lineHeight - indicatorSize) / 2f);
            Renderer.initQuadDraw(indicatorSize, indicatorSize)
                    .color(color.r, color.g, color.b, Math.min(1f, color.a + 0.2f))
                    .draw(indicatorX, indicatorY);

            String line = row.category.label() + ": " + row.count;
            FontManager.bit.drawString(indicatorX + indicatorSize + 6, textY, line, font);
            textY += lineHeight;
            if (i < data.rows.size() - 1) textY += HUD_LINE_GAP;
        }
    }

    private HudCounterData resolveHudData(List<PaintState.PaintEntry> snapshot) {
        if (BlueprintPlacement.active) {
            HudCounterData bp = buildBlueprintHudData();
            if (bp != null) return bp;
        }
        if (SelectionState.isActive() && SelectionState.getSelectedCount() > 0) {
            HudCounterData sel = buildSelectionHudData(snapshot);
            if (sel != null) return sel;
        }
        return null;
    }

    private HudCounterData buildBlueprintHudData() {
        List<BlueprintPlacement.BlueprintTile> tiles = BlueprintPlacement.snapshotRelativeTiles();
        if (tiles.isEmpty()) return null;
        EnumMap<PaintCategory, Integer> counts = new EnumMap<>(PaintCategory.class);
        for (BlueprintPlacement.BlueprintTile tile : tiles) {
            PaintCategory category = PaintCategory.byId(tile.categoryId);
            counts.merge(category, 1, Integer::sum);
        }
        return buildHudDataFromCounts("Blueprint paints", tiles.size(), counts);
    }

    private HudCounterData buildSelectionHudData(List<PaintState.PaintEntry> snapshot) {
        List<long[]> selectedPoints = SelectionState.getSelectedPoints();
        if (selectedPoints.isEmpty()) return null;
        HashSet<Long> selected = new HashSet<>(selectedPoints.size());
        for (long[] pt : selectedPoints) {
            int x = (int) pt[0];
            int y = (int) pt[1];
            selected.add(tileKey(x, y));
        }
        EnumMap<PaintCategory, Integer> counts = new EnumMap<>(PaintCategory.class);
        int total = 0;
        for (PaintState.PaintEntry entry : snapshot) {
            long key = tileKey(entry.x, entry.y);
            if (!selected.contains(key)) continue;
            PaintCategory category = PaintCategory.byId(entry.categoryId);
            counts.merge(category, 1, Integer::sum);
            total++;
        }
        if (total == 0) return null;
        return buildHudDataFromCounts("Selection paints", total, counts);
    }

    private HudCounterData buildHudDataFromCounts(String title, int total, Map<PaintCategory, Integer> counts) {
        if (counts.isEmpty()) return null;
        List<CategoryCount> rows = new ArrayList<>();
        for (PaintCategory category : PaintCategory.values()) {
            Integer count = counts.get(category);
            if (count == null || count <= 0) continue;
            rows.add(new CategoryCount(category, count));
        }
        if (rows.isEmpty()) return null;
        String subtitle = "Layers: " + total;
        return new HudCounterData(title, subtitle, rows);
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

    private void drawPaintMark(int px, int py, int tileSize,
                               PaintCategory category,
                               float r, float g, float b, float a) {
        float finalAlpha = clamp01(a * category.layer().alphaScale());
        if (finalAlpha <= 0f) return;
        switch (category.style()) {
            case FULL_TILE:
                drawRect(px, py, tileSize, tileSize, r, g, b, finalAlpha);
                break;
            case INSET_RECT: {
                int margin = Math.max(2, tileSize / 6);
                int size = Math.max(2, tileSize - margin * 2);
                drawRect(px + margin, py + margin, size, size, r, g, b, finalAlpha);
                break;
            }
            case OUTLINE:
                drawCellEdges(px, py, tileSize, r, g, b, finalAlpha);
                break;
            case TOP_STRIP: {
                int height = Math.max(3, tileSize / 4);
                drawRect(px, py, tileSize, height, r, g, b, finalAlpha);
                break;
            }
            case TRIANGLE: {
                drawTriangle(px, py, tileSize, r, g, b, finalAlpha);
                break;
            }
            case QUARTER_CORNER: {
                int size = Math.max(4, tileSize / 2);
                int pad = Math.max(2, tileSize / 10);
                int drawX = px + tileSize - size - pad;
                int drawY = py + tileSize - size - pad;
                drawRect(drawX, drawY, size, size, r, g, b, finalAlpha);
                break;
            }
            case CENTER_DOT: {
                int size = Math.max(4, tileSize / 3);
                int drawX = px + (tileSize - size) / 2;
                int drawY = py + (tileSize - size) / 2;
                drawRect(drawX, drawY, size, size, r, g, b, finalAlpha);
                break;
            }
            case PLUS_SIGN: {
                drawPlus(px, py, tileSize, r, g, b, finalAlpha);
                break;
            }
            case DOOR_ICON: {
                int frameThickness = Math.max(2, tileSize / 8);
                int width = Math.max(6, tileSize / 2);
                int height = tileSize;
                int startX = px + (tileSize - width) / 2;
                drawRect(startX, py, frameThickness, height, r, g, b, finalAlpha); // left jamb
                drawRect(startX + width - frameThickness, py, frameThickness, height, r, g, b, finalAlpha); // right jamb
                int archHeight = Math.max(4, tileSize / 4);
                drawRect(startX, py, width, frameThickness, r, g, b, finalAlpha); // header
                int doorWidth = width - frameThickness * 2;
                int doorHeight = height - archHeight - frameThickness;
                int doorX = startX + frameThickness;
                int doorY = py + archHeight;
                drawRect(doorX, doorY, doorWidth, doorHeight, r, g, b, finalAlpha * 0.6f);
                break;
            }
            default:
                drawRect(px, py, tileSize, tileSize, r, g, b, finalAlpha);
        }
    }

    private void drawRect(int x, int y, int w, int h, float r, float g, float b, float a) {
        GameResources.empty.initDraw()
                .size(w, h)
                .pos(x, y, false)
                .color(r, g, b, a)
                .draw();
    }

    private void drawPlus(int px, int py, int tileSize, float r, float g, float b, float a) {
        int thickness = Math.max(2, tileSize / 5);
        int usable = tileSize - thickness * 2;
        int centerX = px + tileSize / 2;
        int centerY = py + tileSize / 2;

        int verticalX = centerX - thickness / 2;
        int verticalY = py + (tileSize - usable) / 2;
        drawRect(verticalX, verticalY, thickness, usable, r, g, b, a);

        int horizontalX = px + (tileSize - usable) / 2;
        int horizontalY = centerY - thickness / 2;
        drawRect(horizontalX, horizontalY, usable, thickness, r, g, b, a);
    }

    private void drawTriangle(int px, int py, int tileSize, float r, float g, float b, float a) {
        int triHeight = Math.max(4, tileSize / 2);
        int baseY = py + tileSize - 1;
        for (int row = 0; row < triHeight; row++) {
            int width = tileSize - (row * tileSize / triHeight);
            int startX = px + (tileSize - width) / 2;
            int y = baseY - row;
            GameResources.empty.initDraw()
                    .size(Math.max(1, width), 1)
                    .pos(startX, y, false)
                    .color(r, g, b, a)
                    .draw();
        }
    }

    private static float clamp01(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }

    private static long tileKey(int x, int y) {
        return ((long)x << 32) | (y & 0xffffffffL);
    }

    private static final class HudCounterData {
        final String title;
        final String subtitle;
        final List<CategoryCount> rows;

        HudCounterData(String title, String subtitle, List<CategoryCount> rows) {
            this.title = title;
            this.subtitle = subtitle;
            this.rows = rows;
        }
    }

    private static final class CategoryCount {
        final PaintCategory category;
        final int count;
        CategoryCount(PaintCategory category, int count) {
            this.category = category;
            this.count = count;
        }
    }
}
