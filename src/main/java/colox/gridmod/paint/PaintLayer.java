package colox.gridmod.paint;

/**
 * Logical layer that a paint category belongs to. Layers determine both
 * placement priority and render ordering (low values draw first).
 */
public enum PaintLayer {
    TERRAIN(0, 0.35f),          // floors / natural tiles
    OBJECT(10, 0.55f),          // placeables that sit on top of terrain
    WALL(20, 0.7f),             // background walls, windows, wall mounts
    TABLETOP(30, 0.9f);         // table decorations / micro objects

    private final int drawOrder;
    private final float alphaScale;

    PaintLayer(int drawOrder, float alphaScale) {
        this.drawOrder = drawOrder;
        this.alphaScale = clamp(alphaScale);
    }

    public int drawOrder() {
        return drawOrder;
    }

    public float alphaScale() {
        return alphaScale;
    }

    private static float clamp(float value) {
        if (value < 0f) return 0f;
        if (value > 1f) return 1f;
        return value;
    }
}
