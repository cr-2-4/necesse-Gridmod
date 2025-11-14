package colox.gridmod.input;

import colox.gridmod.paint.BlueprintPlacement;
import colox.gridmod.paint.PaintState;
import colox.gridmod.paint.SelectionState;

/** Helper that decides whether paint/selection/blueprint input modes should suppress actions. */
public final class PaintModeInputGate {
    private PaintModeInputGate() {}

    public static boolean shouldBlockActions() {
        if (BlueprintPlacement.active) return true;
        if (PaintState.enabled) return true;
        return SelectionState.isActive();
    }
}
