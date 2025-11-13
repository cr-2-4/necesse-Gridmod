package colox.gridmod.overlay;

import java.awt.Rectangle;

import colox.gridmod.config.GridConfig;
import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.gfx.GameResources;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.Drawable;
import necesse.level.maps.Level;
import necesse.level.maps.levelData.settlementData.SettlementBoundsManager;

public class SettlementBoundsOverlay implements Drawable {

    @SuppressWarnings("unused")
    private final Level level;
    private final GameCamera camera;

    public SettlementBoundsOverlay(Level level, GameCamera camera) {
        this.level = level;
        this.camera = camera;
    }

    @Override
    public void draw(TickManager tickManager) {
        if (!GridConfig.settlementEnabled) return;

        final int tilePx = GridConfig.tileSize;

        // Camera rect
        final int camX = camera.getX();
        final int camY = camera.getY();
        final int camW = camera.getWidth();
        final int camH = camera.getHeight();

        Rectangle tileRect = getSettlementTileRectangle();
        if (tileRect == null) return;
        final int sideTiles = tileRect.width;
        final int ox = tileRect.x;
        final int oy = tileRect.y;

        // Convert to pixel rect relative to camera
        final int px = ox * tilePx - camX;
        final int py = oy * tilePx - camY;
        final int pw = sideTiles * tilePx;
        final int ph = sideTiles * tilePx;

        // Cull if completely off-screen
        if (px > camW || py > camH || px + pw < -1 || py + ph < -1) return;

        // Colors
        final float r = GridConfig.sbr;
        final float g = GridConfig.sbg;
        final float b = GridConfig.sbb;

        // Fill
        final float fa = clamp01(GridConfig.settlementFillAlpha);
        if (fa > 0.001f) {
            GameResources.empty.initDraw()
                .size(Math.max(1, pw), Math.max(1, ph))
                .pos(px, py, false)
                .color(r, g, b, fa)
                .draw();
        }

        // Outline
        final float oa = clamp01(GridConfig.settlementOutlineAlpha);
        final int thick = Math.max(1, GridConfig.settlementOutlineThickness);
        if (oa > 0.001f) {
            // top
            GameResources.empty.initDraw().size(Math.max(1, pw), thick).pos(px, py, false).color(r, g, b, oa).draw();
            // bottom
            GameResources.empty.initDraw().size(Math.max(1, pw), thick).pos(px, py + ph - thick, false).color(r, g, b, oa).draw();
            // left
            GameResources.empty.initDraw().size(thick, Math.max(1, ph)).pos(px, py, false).color(r, g, b, oa).draw();
            // right
            GameResources.empty.initDraw().size(thick, Math.max(1, ph)).pos(px + pw - thick, py, false).color(r, g, b, oa).draw();
        }
    }

    private Rectangle getSettlementTileRectangle() {
        if (!GridConfig.hasSettlementFlag()) return null;
        if ("builtin".equalsIgnoreCase(GridConfig.settlementMode)) {
            int tier = Math.max(1, Math.min(GridConfig.maxTier(), GridConfig.settlementTier));
            int flagTier = Math.max(0, Math.min(SettlementBoundsManager.flagTiers.length - 1, tier - 1));
            return SettlementBoundsManager.getTileRectangleFromTier(GridConfig.settlementFlagTx, GridConfig.settlementFlagTy, flagTier);
        }
        int sideTiles = GridConfig.currentTierSideTiles();
        int half = sideTiles / 2;
        return new Rectangle(GridConfig.settlementFlagTx - half, GridConfig.settlementFlagTy - half, sideTiles, sideTiles);
    }

    private static float clamp01(float v) {
        return v < 0f ? 0f : (v > 1f ? 1f : v);
    }
}
