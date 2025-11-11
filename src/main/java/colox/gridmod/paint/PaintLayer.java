package colox.gridmod.paint;

/**
 * Logical layer that a paint category belongs to. Layers determine both
 * placement priority and render ordering (low values draw first).
 */
public enum PaintLayer {
    TERRAIN(0),          // floors / natural tiles
    OBJECT(10),          // placeables that sit on top of terrain
    WALL(20),            // background walls, windows, wall mounts
    TABLETOP(30);        // table decorations / micro objects

    private final int drawOrder;

    PaintLayer(int drawOrder) {
        this.drawOrder = drawOrder;
    }

    public int drawOrder() {
        return drawOrder;
    }
}
