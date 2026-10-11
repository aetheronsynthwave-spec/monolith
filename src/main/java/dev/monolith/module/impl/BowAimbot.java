package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1802;
import net.minecraft.class_243;
import net.minecraft.class_3532;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;

/**
 * While drawing a bow, aims at the best target with arrow drop and target movement accounted for.
 * Aims as if the bow is fully drawn, so release at full draw. Uses the same smooth rotation as the Aimbot.
 */
public class BowAimbot extends Module {
    private static final double V = 3.0;                     // full-draw arrow speed (blocks/tick)
    public final EnumSetting<Aimbot.Targets> targets = add(new EnumSetting<>("Targets", Aimbot.Targets.BOTH));
    public final BoolSetting hostileOnly = add(new BoolSetting("Hostile Mobs Only", false));
    public final EnumSetting<Aimbot.Part> part = add(new EnumSetting<>("Aim At", Aimbot.Part.CHEST));
    public final NumberSetting range = add(new NumberSetting("Range", 40, 8, 100, 1));
    public final NumberSetting fov = add(new NumberSetting("FOV", 140, 20, 360, 5));
    public final NumberSetting speed = add(new NumberSetting("Smoothness", 10, 1, 30, 0.5));
    public final BoolSetting predict = add(new BoolSetting("Lead Moving Targets", true));
    public final BoolSetting visibleOnly = add(new BoolSetting("Visible Only", true));
    private class_1309 target;
    private class_243 lastPos; private class_243 vel = class_243.field_1353;
    private long last = System.nanoTime();
    private static double lastTicks;

    public BowAimbot() {
        super("BowAimbot", "Aims your bow with drop and lead compensation.", Category.COMBAT);
        hostileOnly.visibleWhen(() -> targets.get() != Aimbot.Targets.PLAYERS);
    }

    @Override public void onDisable() { target = null; lastPos = null; vel = class_243.field_1353; }

    /** Track the target's real speed per tick (client-side velocity is unreliable for other players). */
    @Override public void onTick() {
        if (target == null) { lastPos = null; vel = class_243.field_1353; return; }
        class_243 p = new class_243(target.method_23317(), target.method_23318(), target.method_23321());
        if (lastPos != null) vel = p.method_1020(lastPos);
        lastPos = p;
    }

    @Override public void onFrame(float delta) {
        long now = System.nanoTime(); float dt = Math.min(0.05f, (now - last) / 1e9f); last = now;
        if (mc.field_1724 == null || mc.field_1687 == null || mc.field_1755 != null || !mc.field_1724.method_6115() || !mc.field_1724.method_6030().method_31574(class_1802.field_8102)) { target = null; return; }
        class_243 eye = mc.field_1724.method_33571();
        double sx = eye.field_1352, sy = eye.field_1351 - 0.1, sz = eye.field_1350;           // arrows leave slightly below the eyes

        if (target != null && !valid(target, delta, 1.25)) { target = null; lastPos = null; vel = class_243.field_1353; }
        if (target == null) target = pick(delta);
        if (target == null) return;

        class_243 lp = target.method_30950(delta);
        double k = part.get() == Aimbot.Part.HEAD ? 0.9 : part.get() == Aimbot.Part.CHEST ? 0.65 : 0.15;
        double tx = lp.field_1352, ty = lp.field_1351 + target.method_17682() * k, tz = lp.field_1350;
        double px = tx, py = ty, pz = tz; double[] sol = null;
        for (int it = 0; it < 3; it++) {                           // refine: flight time depends on where the target will be
            sol = solve(Math.hypot(px - sx, pz - sz), py - sy);
            if (!predict.get()) break;
            px = tx + vel.field_1352 * sol[1]; py = ty + vel.field_1351 * sol[1] * 0.5; pz = tz + vel.field_1350 * sol[1];
        }
        float yawT = (float) Math.toDegrees(Math.atan2(-(px - sx), pz - sz)), pitchT = (float) -sol[0];
        AimHelper.rotate(class_3532.method_15393(yawT - mc.field_1724.method_36454()), pitchT - mc.field_1724.method_36455(), dt, speed.asFloat(), 500f);
    }

    private boolean valid(class_1309 e, float delta, double scale) {
        if (!AimHelper.typeOk(e, targets.get(), hostileOnly.get())) return false;
        if (mc.field_1724.method_5858(e) > range.get() * range.get() * scale * scale) return false;
        if (visibleOnly.get() && !mc.field_1724.method_6057(e)) return false;
        return Math.abs(yawError(e, delta)) <= fov.get() / 2.0 * scale;
    }
    private double yawError(class_1309 e, float delta) {
        class_243 lp = e.method_30950(delta), eye = mc.field_1724.method_33571();
        return class_3532.method_15393((float) Math.toDegrees(Math.atan2(-(lp.field_1352 - eye.field_1352), lp.field_1350 - eye.field_1350)) - mc.field_1724.method_36454());
    }
    private class_1309 pick(float delta) {
        class_1309 best = null; double bestErr = 1e9;
        for (class_1297 e : mc.field_1687.method_18112()) {
            if (!(e instanceof class_1309 le) || !valid(le, delta, 1.0)) continue;
            double err = Math.abs(yawError(le, delta));
            if (err < bestErr) { bestErr = err; best = le; }
        }
        return best;
    }

    /** Launch angle (degrees, up = positive) to hit a point d blocks away and dy higher; also stores flight ticks. */
    private static double[] solve(double d, double dy) {
        d = Math.max(0.5, d);
        double lo = -60, hi = 45;
        if (height(hi, d) < dy) return new double[]{hi, lastTicks};         // out of reach: best effort
        for (int i = 0; i < 18; i++) { double mid = (lo + hi) / 2; if (height(mid, d) < dy) lo = mid; else hi = mid; }
        double th = (lo + hi) / 2; height(th, d);
        return new double[]{th, lastTicks};
    }

    /** Simulates an arrow (drag 0.99, gravity 0.05/tick) and returns its height when it has travelled d blocks. */
    private static double height(double thetaDeg, double d) {
        double th = Math.toRadians(thetaDeg), vx = V * Math.cos(th), vy = V * Math.sin(th), x = 0, y = 0;
        for (int t = 1; t <= 300; t++) {
            double nx = x + vx, ny = y + vy;
            if (nx >= d) { double f = (d - x) / (nx - x); lastTicks = t - 1 + f; return y + (ny - y) * f; }
            x = nx; y = ny; vx *= 0.99; vy = vy * 0.99 - 0.05;
        }
        lastTicks = 300; return -1e9;
    }
}
