package colox.gridmod.overlay;

import java.util.List;

import necesse.engine.gameLoop.tickManager.TickManager;
import necesse.engine.modLoader.annotations.ModMethodPatch;
import necesse.entity.mobs.Mob;
import necesse.entity.mobs.PlayerMob;
import necesse.gfx.camera.GameCamera;
import necesse.gfx.drawables.OrderableDrawables;
import necesse.level.maps.Level;
import net.bytebuddy.asm.Advice;

import colox.gridmod.paint.PaintControls;
import colox.gridmod.paint.PaintDrawable;
import colox.gridmod.config.GridConfig;

@ModMethodPatch(
        target = Mob.class,
        name = "addDrawables",
        arguments = {
                List.class,
                OrderableDrawables.class,
                OrderableDrawables.class,
                OrderableDrawables.class,
                Level.class,
                TickManager.class,
                GameCamera.class,
                PlayerMob.class
        }
)
public class GridOverlayHook {

    @Advice.OnMethodEnter(skipOn = Advice.OnNonDefaultValue.class)
    static boolean onEnter(
            @Advice.This Mob mob,
            @Advice.Argument(3) OrderableDrawables overlayList,
            @Advice.Argument(4) Level level,
            @Advice.Argument(5) TickManager tickManager,
            @Advice.Argument(6) GameCamera camera,
            @Advice.Argument(7) PlayerMob perspective
    ) {
        if (mob == perspective) {
            // Per-frame input for painting/selection/placement
            PaintControls.tick(level, camera);

            // Grid
            overlayList.add(100_000, new GridDrawable(level, camera));

            // Settlement bounds
            if (GridConfig.settlementEnabled) {
                overlayList.add(100_005, new SettlementBoundsOverlay(level, camera));
            }

            // Paint overlay appears only if paintVisible == true
            if (GridConfig.paintVisible) {
                overlayList.add(100_010, new PaintDrawable(level, camera));
            }
        }
        return false;
    }
}
