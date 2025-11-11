package colox.gridmod.paint;

import colox.gridmod.config.GridConfig;

public final class Painter {
    private Painter() {}

    public static void applyAt(int cx, int cy, boolean erase, String categoryId) {
        int s = PaintState.getBrush();               // side length
        int half = (s - 1) / 2;                      // integer floor
        int startX = cx - half;
        int startY = cy - half;
        String cat = categoryId;
        if (cat == null || cat.isBlank()) {
            cat = GridConfig.getActivePaintCategory().id();
        }
        for (int y = startY; y < startY + s; y++) {
            for (int x = startX; x < startX + s; x++) {
                if (erase) PaintState.remove(x, y);
                else       PaintState.add(x, y, cat);
            }
        }
    }

}
