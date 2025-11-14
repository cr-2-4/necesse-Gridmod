package colox.gridmod.paint;

import java.util.ArrayList;
import java.util.List;

public final class BlueprintPlacement {
    private BlueprintPlacement() {}

    public static final class BlueprintTile {
        public final int dx;
        public final int dy;
        public final String categoryId;
        public BlueprintTile(int dx, int dy, String categoryId) {
            this.dx = dx;
            this.dy = dy;
            this.categoryId = categoryId;
        }
    }

    // Whether we are currently placing a blueprint
    public static boolean active = false;

    private static final List<BlueprintTile> rel = new ArrayList<>();

    // Transform state
    private static int rot = 0;           // 0,90,180,270 (clockwise)
    private static boolean flip = false;  // horizontal flip before rotate

    public static void begin(List<BlueprintTile> relativeTiles) {
        rel.clear();
        rel.addAll(relativeTiles);
        rot = 0;
        flip = false;
        active = true;
        SelectionState.deactivate();
    }

    public static void cancel() {
        active = false;
        rel.clear();
    }

    public static void rotateCW() {
        if (!active) return;
        rot = (rot + 90) % 360;
    }

    // NEW for Phase 3
    public static void rotateCCW() {
        if (!active) return;
        rot -= 90;
        if (rot < 0) rot += 360;
    }

    public static void toggleFlip() {
        if (!active) return;
        flip = !flip;
    }

    // Compute transformed absolute tiles when anchoring at (ax, ay)
    public static List<BlueprintTile> transformedAt(int ax, int ay) {
        List<BlueprintTile> out = new ArrayList<>(rel.size());
        for (BlueprintTile p : rel) {
            int dx = p.dx, dy = p.dy;

            // flip (mirror X) first
            if (flip) dx = -dx;

            // rotate around (0,0) clockwise
            int rx = dx, ry = dy;
            switch (rot) {
                case 90:  rx =  dy; ry = -dx; break;
                case 180: rx = -dx; ry = -dy; break;
                case 270: rx = -dy; ry =  dx; break;
                default:  rx =  dx; ry =  dy; break; // 0°
            }

            out.add(new BlueprintTile(ax + rx, ay + ry, p.categoryId));
        }
        return out;
    }

    public static boolean isFlipped() {
        return flip;
    }

    public static int getBlueprintTileCount() {
        return rel.size();
    }

    public static List<BlueprintTile> snapshotRelativeTiles() {
        return new ArrayList<>(rel);
    }
}
