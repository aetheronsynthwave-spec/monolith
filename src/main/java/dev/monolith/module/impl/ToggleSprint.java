package dev.monolith.module.impl;
import dev.monolith.module.*;
public class ToggleSprint extends Module {
    public ToggleSprint() { super("ToggleSprint", "Hold sprint automatically while moving forward.", Category.MOVEMENT); }
    @Override public void onTick() {
        if (mc.options.forwardKey.isPressed() && !mc.player.isSneaking() && !mc.player.horizontalCollision
            && mc.player.getHungerManager().getFoodLevel() > 6) mc.player.setSprinting(true);
    }
}
