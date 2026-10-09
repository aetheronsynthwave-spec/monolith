package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.minecraft.util.math.Vec3d;

/** BHOP: auto-timed jumps with boosted ground speed and air control. GROUND: just boosts walking speed. */
public class Speed extends Module {
    public enum Mode { BHOP, GROUND }
    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.BHOP));
    public final NumberSetting multiplier = add(new NumberSetting("Speed", 1.5, 1.0, 3.0, 0.05));
    public final BoolSetting autoJump = add(new BoolSetting("Auto Jump", true));
    public final BoolSetting airControl = add(new BoolSetting("Air Control", true));
    private static final double BASE = 0.2806;               // vanilla sprint speed per tick

    public Speed() { super("Speed", "Bunny-hop and ground speed boost.", Category.MOVEMENT); autoJump.visibleWhen(() -> mode.get() == Mode.BHOP); airControl.visibleWhen(() -> mode.get() == Mode.BHOP); }

    @Override public void onTick() {
        var p = mc.player;
        if (p.isSneaking() || p.isTouchingWater() || p.isInLava() || p.isClimbing() || p.getAbilities().flying) return;
        float f = (mc.options.forwardKey.isPressed() ? 1 : 0) - (mc.options.backKey.isPressed() ? 1 : 0);
        float s = (mc.options.leftKey.isPressed() ? 1 : 0) - (mc.options.rightKey.isPressed() ? 1 : 0);
        if (f == 0 && s == 0) return;
        double yaw = Math.toRadians(p.getYaw());
        double dx = -Math.sin(yaw) * f + Math.cos(yaw) * s, dz = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.hypot(dx, dz); dx /= len; dz /= len;
        Vec3d v = p.getVelocity();
        double target = BASE * multiplier.get();
        p.setSprinting(true);
        if (mode.get() == Mode.GROUND) { if (p.isOnGround()) p.setVelocity(dx * target, v.y, dz * target); return; }
        if (p.isOnGround()) {
            p.setVelocity(dx * target, autoJump.get() ? 0.42 : v.y, dz * target);
        } else if (airControl.get()) {
            double sp = Math.max(Math.hypot(v.x, v.z) * 0.998, BASE);       // keep momentum, steer toward the keys
            p.setVelocity(dx * sp, v.y, dz * sp);
        }
    }
}
