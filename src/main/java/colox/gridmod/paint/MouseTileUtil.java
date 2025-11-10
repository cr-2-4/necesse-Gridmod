package colox.gridmod.paint;

import necesse.gfx.camera.GameCamera;

public final class MouseTileUtil {
    private static GameCamera camera;

    private MouseTileUtil() {}

    public static void setCamera(GameCamera cam) {
        camera = cam;
    }

    /** Returns {tx, ty} or null if camera not set yet. */
    public static int[] getMouseTile(int tileSize) {
        if (camera == null) return null;
        int tx = camera.getMouseLevelTilePosX();
        int ty = camera.getMouseLevelTilePosY();
        return new int[]{tx, ty};
    }
}
