package colox.gridmod.paint;

import java.util.EnumSet;

/**
 * Shared filter for erase/selection tools.
 */
public enum PaintLayerFilter {
    ALL("All layers", EnumSet.allOf(PaintLayer.class)),
    BOTTOM("Bottom (floors/terrain)", EnumSet.of(PaintLayer.TERRAIN)),
    MIDDLE("Middle (objects)", EnumSet.of(PaintLayer.OBJECT)),
    WALL("Walls / background", EnumSet.of(PaintLayer.WALL)),
    TOP("Top (table & decor)", EnumSet.of(PaintLayer.TABLETOP));

    private final String label;
    private final EnumSet<PaintLayer> layers;

    PaintLayerFilter(String label, EnumSet<PaintLayer> layers) {
        this.label = label;
        this.layers = layers;
    }

    public String id() {
        return name().toLowerCase();
    }

    public String label() {
        return label;
    }

    public boolean matches(PaintLayer layer) {
        if (this == ALL || layer == null) return true;
        return layers.contains(layer);
    }

    public static PaintLayerFilter byId(String id) {
        if (id != null) {
            String needle = id.trim().toLowerCase();
            for (PaintLayerFilter filter : values()) {
                if (filter.id().equals(needle)) return filter;
            }
        }
        return ALL;
    }
}
