package colox.gridmod;

import necesse.engine.gameLoop.tickManager.TickManager;

public final class ClientOverlay {
    private ClientOverlay() {}

    // Call this once per client frame (from your drawable hook)
    public static void onFrame(TickManager tm) {
        // Poll the UI hotkey each frame
        colox.gridmod.input.GridKeybinds.poll();

        // (optional) If you want the “toggle UI” hotkey instead of “always open”
        // use GridUI.toggle() when the key is pressed, but keep it inside pollOpenUI().
    }
}
