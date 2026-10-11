package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.minecraft.class_243;

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
        var p = mc.field_1724;
        if (p.method_5715() || p.method_5799() || p.method_5771() || p.method_6101() || p.method_31549().field_7479) return;
        float f = (mc.field_1690.field_1894.method_1434() ? 1 : 0) - (mc.field_1690.field_1881.method_1434() ? 1 : 0);
        float s = (mc.field_1690.field_1913.method_1434() ? 1 : 0) - (mc.field_1690.field_1849.method_1434() ? 1 : 0);
        if (f == 0 && s == 0) return;
        double yaw = Math.toRadians(p.method_36454());
        double dx = -Math.sin(yaw) * f + Math.cos(yaw) * s, dz = Math.cos(yaw) * f + Math.sin(yaw) * s;
        double len = Math.hypot(dx, dz); dx /= len; dz /= len;
        class_243 v = p.method_18798();
        double target = BASE * multiplier.get();
        p.method_5728(true);
        if (mode.get() == Mode.GROUND) { if (p.method_24828()) p.method_18800(dx * target, v.field_1351, dz * target); return; }
        if (p.method_24828()) {
            p.method_18800(dx * target, autoJump.get() ? 0.42 : v.field_1351, dz * target);
        } else if (airControl.get()) {
            double sp = Math.max(Math.hypot(v.field_1352, v.field_1350) * 0.998, BASE);       // keep momentum, steer toward the keys
            p.method_18800(dx * sp, v.field_1351, dz * sp);
        }
    }
}
