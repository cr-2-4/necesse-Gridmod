package colox.gridmod.input;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.PlayerInventorySlot;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(
        target = PlayerMob.class,
        name = "runClientControllerAttack",
        arguments = {
                PlayerInventorySlot.class
        }
)
public final class RunClientControllerAttackBlockPatch {
    private RunClientControllerAttackBlockPatch() {}

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    static boolean onEnter() {
        return PaintModeInputGate.shouldBlockActions();
    }
}
