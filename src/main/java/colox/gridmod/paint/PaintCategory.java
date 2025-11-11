package colox.gridmod.paint;

import java.util.Locale;

/**
 * Defines the selectable paint categories and their default colors.
 */
public enum PaintCategory {
    FLOORS("floors", "Floors", 0.82f, 0.67f, 0.45f, 0.50f),
    TERRAIN("terrain", "Terrain", 0.40f, 0.60f, 0.25f, 0.50f),
    LIQUIDS("liquids", "Liquids", 0.20f, 0.45f, 0.90f, 0.50f),
    OTHER_TILES("other_tiles", "Other Tiles (Farmland/Landfill)", 0.90f, 0.80f, 0.55f, 0.50f),

    OBJECTS("objects", "Objects", 0.95f, 0.35f, 0.35f, 0.50f),
    SEEDS("seeds", "Seeds / Crops", 0.35f, 0.80f, 0.30f, 0.50f),
    CRAFTING("crafting", "Crafting Stations", 0.95f, 0.65f, 0.25f, 0.50f),
    LIGHTING("lighting", "Lighting", 1.00f, 0.90f, 0.55f, 0.50f),
    FURNITURE("furniture", "Furniture", 0.75f, 0.55f, 0.35f, 0.50f),
    DECOR("decor", "Decorations", 0.90f, 0.40f, 0.80f, 0.50f),
    WALLS("walls", "Walls (Background)", 0.65f, 0.65f, 0.65f, 0.50f),
    DOORS("doors", "Doors", 0.70f, 0.45f, 0.25f, 0.50f),
    FENCES("fences", "Fences", 0.75f, 0.75f, 0.35f, 0.50f),
    FENCE_GATES("fence_gates", "Fence Gates", 0.90f, 0.75f, 0.40f, 0.50f),
    TRAPS("traps", "Traps", 0.95f, 0.20f, 0.20f, 0.50f),
    LANDSCAPING("landscaping", "Landscaping Objects", 0.30f, 0.60f, 0.30f, 0.50f),
    TABLE_DECOR("table_decor", "Table Decorations", 0.95f, 0.75f, 0.90f, 0.50f),
    MASONRY("masonry", "Masonry / Structural", 0.80f, 0.80f, 0.85f, 0.50f),
    OTHER_OBJECTS("other_objects", "Other Objects", 0.50f, 0.50f, 0.95f, 0.50f);

    private final String id;
    private final String label;
    private final float defR, defG, defB, defA;

    PaintCategory(String id, String label, float defR, float defG, float defB, float defA) {
        this.id = id;
        this.label = label;
        this.defR = clamp(defR);
        this.defG = clamp(defG);
        this.defB = clamp(defB);
        this.defA = clamp(defA);
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

    public static PaintCategory defaultCategory() {
        return FLOORS;
    }

    public static PaintCategory byId(String id) {
        if (id == null || id.isEmpty()) return defaultCategory();
        String needle = id.trim().toLowerCase(Locale.ROOT);
        if ("tiles".equals(needle)) return FLOORS; // legacy alias
        for (PaintCategory cat : values()) {
            if (cat.id.equals(needle)) return cat;
        }
        return defaultCategory();
    }
}
