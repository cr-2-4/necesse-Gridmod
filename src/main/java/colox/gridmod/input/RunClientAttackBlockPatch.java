package colox.gridmod.input;

import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.entity.mobs.PlayerMob;
import necesse.inventory.PlayerInventorySlot;
import net.bytebuddy.asm.Advice;

@ModMethodPatch(
        target = PlayerMob.class,
        name = "runClientAttack",
        arguments = {
                PlayerInventorySlot.class,
                int.class,
                int.class
        }
)
public final class RunClientAttackBlockPatch {
    private RunClientAttackBlockPatch() {}

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    static boolean onEnter() {
        return PaintModeInputGate.shouldBlockActions();
    }
}
