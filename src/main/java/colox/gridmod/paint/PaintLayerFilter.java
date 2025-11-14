package colox.gridmod.paint;

import java.util.EnumSet;

/**
 * Shared filter for erase/selection tools.
 */
public enum PaintLayerFilter {
    ALL("All layers", EnumSet.allOf(PaintLayer.class)),
    BOTTOM("Bottom (floors / terrain)", EnumSet.of(PaintLayer.TERRAIN)),
    MIDDLE("Middle (objects + wall background + lighting)", EnumSet.of(PaintLayer.OBJECT, PaintLayer.WALL, PaintLayer.WALL_LIGHTING)),
    WALL_BACKGROUND("Wall background only", EnumSet.of(PaintLayer.WALL)),
    WALL_ATTACHMENTS("Wall attachments only", EnumSet.of(PaintLayer.WALL_ATTACHMENT)),
    TOP("Top (table decor + wall attachments + lighting)", EnumSet.of(PaintLayer.TABLETOP, PaintLayer.WALL_ATTACHMENT, PaintLayer.WALL_LIGHTING));

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
            if ("wall".equals(needle)) return WALL_BACKGROUND;
            for (PaintLayerFilter filter : values()) {
                if (filter.id().equals(needle)) return filter;
            }
        }
        return ALL;
    }

    public static PaintLayerFilter forLayer(PaintLayer layer) {
        if (layer == null) return ALL;
        switch (layer) {
            case TERRAIN: return BOTTOM;
            case OBJECT: return MIDDLE;
            case WALL: return MIDDLE;
            case WALL_LIGHTING: return MIDDLE;
            case WALL_ATTACHMENT: return TOP;
            case TABLETOP: return TOP;
            default: return ALL;
        }
    }
}
