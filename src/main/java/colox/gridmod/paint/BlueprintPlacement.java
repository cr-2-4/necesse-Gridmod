package colox.gridmod.paint;

import java.util.ArrayList;
import java.util.List;

public final class BlueprintPlacement {
    private BlueprintPlacement() {}

    // Whether we are currently placing a blueprint
    public static boolean active = false;

    // Tiles relative to pivot (0,0). Each entry is int[2] {dx, dy}
    private static final List<int[]> rel = new ArrayList<>();

    // Transform state
    private static int rot = 0;           // 0,90,180,270 (clockwise)
    private static boolean flip = false;  // horizontal flip before rotate

    public static void begin(List<int[]> relativeTiles) {
        rel.clear();
        rel.addAll(relativeTiles);
        rot = 0;
        flip = false;
        active = true;
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
    public static List<int[]> transformedAt(int ax, int ay) {
        List<int[]> out = new ArrayList<>(rel.size());
        for (int[] p : rel) {
            int dx = p[0], dy = p[1];

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

            out.add(new int[] { ax + rx, ay + ry });
        }
        return out;
    }
}
