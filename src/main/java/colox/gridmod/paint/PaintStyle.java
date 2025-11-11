package colox.gridmod.paint;

/**
 * Visual footprint used when rendering a paint category.
 */
public enum PaintStyle {
    FULL_TILE,     // fills the entire tile
    INSET_RECT,    // centered rectangle smaller than the tile (approx half)
    OUTLINE,       // only draws the tile outline
    TOP_STRIP,     // horizontal strip across the top (wall / window hint)
    QUARTER_CORNER,// small square in one corner (table decor)
    CENTER_DOT     // compact square in the tile center (traps/seeds)
}
