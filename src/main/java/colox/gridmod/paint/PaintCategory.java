package colox.gridmod.paint;

import java.util.Locale;

/**
 * Defines the selectable paint categories and their default colors.
 */
public enum PaintCategory {
    FLOORS("floors", "Floors", 0.82f, 0.67f, 0.45f, 0.50f, PaintLayer.TERRAIN, PaintStyle.FULL_TILE),
    TERRAIN("terrain", "Terrain", 0.40f, 0.60f, 0.25f, 0.50f, PaintLayer.TERRAIN, PaintStyle.FULL_TILE),
    LIQUIDS("liquids", "Liquids", 0.20f, 0.45f, 0.90f, 0.50f, PaintLayer.TERRAIN, PaintStyle.FULL_TILE),
    OTHER_TILES("other_tiles", "Other Tiles (Farmland/Landfill)", 0.90f, 0.80f, 0.55f, 0.50f, PaintLayer.TERRAIN, PaintStyle.OUTLINE),

    OBJECTS("objects", "Objects", 0.95f, 0.35f, 0.35f, 0.50f, PaintLayer.OBJECT, PaintStyle.INSET_RECT),
    SEEDS("seeds", "Seeds / Crops", 0.35f, 0.80f, 0.30f, 0.50f, PaintLayer.OBJECT, PaintStyle.CENTER_DOT),
    CRAFTING("crafting", "Crafting Stations", 0.95f, 0.65f, 0.25f, 0.50f, PaintLayer.OBJECT, PaintStyle.PLUS_SIGN),
    LIGHTING_FLOOR("lighting_floor", "Lighting (Floor)", 1.00f, 0.90f, 0.55f, 0.50f, PaintLayer.OBJECT, PaintStyle.INSET_RECT),
    LIGHTING_WALL("lighting_wall", "Lighting (Wall/Decor)", 1.00f, 0.90f, 0.55f, 0.50f, PaintLayer.WALL_LIGHTING, PaintStyle.TOP_STRIP),
    FURNITURE("furniture", "Furniture", 0.75f, 0.55f, 0.35f, 0.50f, PaintLayer.OBJECT, PaintStyle.INSET_RECT),
    DECOR("decor", "Decorations", 0.90f, 0.40f, 0.80f, 0.50f, PaintLayer.OBJECT, PaintStyle.INSET_RECT),
    WALLS("walls", "Walls (Background)", 0.65f, 0.65f, 0.65f, 0.50f, PaintLayer.WALL, PaintStyle.TRIANGLE),
    WALL_FIXTURES("wall_fixtures", "Wall Fixtures", 0.95f, 0.55f, 0.15f, 0.65f, PaintLayer.WALL_ATTACHMENT, PaintStyle.TOP_STRIP),
    DOORS("doors", "Doors", 0.70f, 0.45f, 0.25f, 0.50f, PaintLayer.WALL, PaintStyle.DOOR_ICON),
    FENCES("fences", "Fences", 0.75f, 0.75f, 0.35f, 0.50f, PaintLayer.WALL, PaintStyle.TRIANGLE),
    FENCE_GATES("fence_gates", "Fence Gates", 0.90f, 0.75f, 0.40f, 0.50f, PaintLayer.WALL, PaintStyle.DOOR_ICON),
    TRAPS("traps", "Traps", 0.95f, 0.20f, 0.20f, 0.50f, PaintLayer.OBJECT, PaintStyle.CENTER_DOT),
    LANDSCAPING("landscaping", "Landscaping Objects", 0.30f, 0.60f, 0.30f, 0.50f, PaintLayer.OBJECT, PaintStyle.INSET_RECT),
    TABLE_DECOR("table_decor", "Table Decorations", 0.95f, 0.75f, 0.90f, 0.50f, PaintLayer.TABLETOP, PaintStyle.QUARTER_CORNER),
    MASONRY("masonry", "Masonry / Structural", 0.80f, 0.80f, 0.85f, 0.50f, PaintLayer.OBJECT, PaintStyle.INSET_RECT),
    OTHER_OBJECTS("other_objects", "Other Objects", 0.50f, 0.50f, 0.95f, 0.50f, PaintLayer.OBJECT, PaintStyle.INSET_RECT);

    private final String id;
    private final String label;
    private final float defR, defG, defB, defA;
    private final PaintLayer layer;
    private final PaintStyle style;

    PaintCategory(String id, String label, float defR, float defG, float defB, float defA,
                  PaintLayer layer, PaintStyle style) {
        this.id = id;
        this.label = label;
        this.defR = clamp(defR);
        this.defG = clamp(defG);
        this.defB = clamp(defB);
        this.defA = clamp(defA);
        this.layer = layer;
        this.style = style;
    }

    private static float clamp(float v) {
        if (v < 0f) return 0f;
        if (v > 1f) return 1f;
        return v;
    }

    public String id() {
        return id;
    }

    public String label() {
        return label;
    }

    public float defaultR() { return defR; }
    public float defaultG() { return defG; }
    public float defaultB() { return defB; }
    public float defaultA() { return defA; }
    public PaintLayer layer() { return layer; }
    public PaintStyle style() { return style; }

    public static PaintCategory defaultCategory() {
        return FLOORS;
    }

    public static PaintCategory byId(String id) {
        if (id == null || id.isEmpty()) return defaultCategory();
        String needle = id.trim().toLowerCase(Locale.ROOT);
        if ("tiles".equals(needle)) return FLOORS; // legacy alias
        if ("lighting".equals(needle)) return LIGHTING_FLOOR; // legacy single lighting entry
        for (PaintCategory cat : values()) {
            if (cat.id.equals(needle)) return cat;
        }
        return defaultCategory();
    }
}
