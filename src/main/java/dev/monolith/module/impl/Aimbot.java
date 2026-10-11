package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;

/**
 * Smooth aim assist. Rotation is applied every rendered frame with frame-rate independent easing and a turn-speed cap,
 * so it glides instead of snapping. Uses the same path as mouse movement (changeLookDirection), so the camera stays smooth.
 */
public class Aimbot extends Module {
    public enum Targets { PLAYERS, MOBS, BOTH }
    public enum Part { HEAD, CHEST, FEET }
    public final EnumSetting<Targets> targets = add(new EnumSetting<>("Targets", Targets.BOTH));
    public final BoolSetting hostileOnly = add(new BoolSetting("Hostile Mobs Only", false));
    public final EnumSetting<Part> part = add(new EnumSetting<>("Aim At", Part.CHEST));
    public final NumberSetting range = add(new NumberSetting("Range", 6, 2, 32, 0.5));
    public final NumberSetting fov = add(new NumberSetting("FOV", 90, 10, 360, 5));
    public final NumberSetting speed = add(new NumberSetting("Smoothness", 8, 1, 30, 0.5));
    public final NumberSetting maxTurn = add(new NumberSetting("Max Turn (deg/s)", 360, 60, 1200, 20));
    public final BoolSetting onlyAttacking = add(new BoolSetting("Only While Attacking", false));
    public final BoolSetting visibleOnly = add(new BoolSetting("Visible Only", true));
    private class_1309 target;
    private long last = System.nanoTime();

    public Aimbot() {
        super("Aimbot", "Smoothly aims at the nearest valid target.", Category.COMBAT);
        hostileOnly.visibleWhen(() -> targets.get() != Targets.PLAYERS);
        speed.tooltip = "Higher = snappier. Lower = smoother and slower.";
        fov.tooltip = "Only targets within this angle of your crosshair.";
    }

    @Override public void onDisable() { target = null; }

    @Override public void onFrame(float delta) {
        long now = System.nanoTime(); float dt = Math.min(0.05f, (now - last) / 1e9f); last = now;
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1755 != null || (onlyAttacking.get() && !mc.field_1690.field_1886.method_1434())) { target = null; return; }

        if (target != null && !valid(target, delta, 1.3)) target = null;    // keep the same target while it stays reasonable (no flicker)
        if (target == null) target = pick(delta);
        if (target == null) return;

        class_243 aim = aimPoint(target, delta), eye = mc.field_1724.method_33571();
        double dx = aim.field_1352 - eye.field_1352, dy = aim.field_1351 - eye.field_1351, dz = aim.field_1350 - eye.field_1350;
        float yawT = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitchT = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        float dYaw = class_3532.method_15393(yawT - mc.field_1724.method_36454()), dPitch = pitchT - mc.field_1724.method_36455();
        if (Math.hypot(dYaw, dPitch) < 0.12) return;                         // already on target

        float t = 1f - (float) Math.exp(-speed.get() * dt);                  // frame-rate independent easing
        float sy = dYaw * t, sp = dPitch * t;
        float cap = maxTurn.asFloat() * dt, mag = (float) Math.hypot(sy, sp);
        if (mag > cap && mag > 0) { sy *= cap / mag; sp *= cap / mag; }
        mc.field_1724.method_5872(sy / 0.15, sp / 0.15);
    }

    private class_243 aimPoint(class_1309 e, float delta) {
        class_243 lp = e.method_30950(delta);
        double k = part.get() == Part.HEAD ? 0.9 : part.get() == Part.CHEST ? 0.65 : 0.15;
        return new class_243(lp.field_1352, lp.field_1351 + e.method_17682() * k, lp.field_1350);
    }

    private boolean typeOk(class_1297 e) {
        if (e == mc.field_1724 || !(e instanceof class_1309 le) || e instanceof class_1531 || !le.method_5805() || e.method_7325()) return false;
        boolean pl = e instanceof class_1657;
        return switch (targets.get()) {
            case PLAYERS -> pl;
            case MOBS -> !pl && (!hostileOnly.get() || e instanceof class_1588);
            case BOTH -> pl || !hostileOnly.get() || e instanceof class_1588;
        };
    }

    /** Angular distance (degrees) from the crosshair to the entity's aim point. */
    private double angle(class_1309 e, float delta) {
        class_243 aim = aimPoint(e, delta), eye = mc.field_1724.method_33571();
        double dx = aim.field_1352 - eye.field_1352, dy = aim.field_1351 - eye.field_1351, dz = aim.field_1350 - eye.field_1350;
        float yawT = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitchT = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return Math.hypot(class_3532.method_15393(yawT - mc.field_1724.method_36454()), pitchT - mc.field_1724.method_36455());
    }

    private boolean valid(class_1309 e, float delta, double fovScale) {
        if (!typeOk(e)) return false;
        if (mc.field_1724.method_5858(e) > range.get() * range.get() * fovScale) return false;
        if (visibleOnly.get() && !mc.field_1724.method_6057(e)) return false;
        return angle(e, delta) <= fov.get() / 2.0 * fovScale;
    }

    private class_1309 pick(float delta) {
        class_1309 best = null; double bestAngle = 1e9;
        for (class_1297 e : mc.field_1687.method_18112()) {
            if (!(e instanceof class_1309 le) || !valid(le, delta, 1.0)) continue;
            double a = angle(le, delta);
            if (a < bestAngle) { bestAngle = a; best = le; }
        }
        return best;
    }
}
