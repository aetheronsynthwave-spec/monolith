package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
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
    private LivingEntity target;
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
        if (mc.player == null || mc.world == null || mc.currentScreen != null || (onlyAttacking.get() && !mc.options.attackKey.isPressed())) { target = null; return; }

        if (target != null && !valid(target, delta, 1.3)) target = null;    // keep the same target while it stays reasonable (no flicker)
        if (target == null) target = pick(delta);
        if (target == null) return;

        Vec3d aim = aimPoint(target, delta), eye = mc.player.getEyePos();
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        float yawT = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitchT = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        float dYaw = MathHelper.wrapDegrees(yawT - mc.player.getYaw()), dPitch = pitchT - mc.player.getPitch();
        if (Math.hypot(dYaw, dPitch) < 0.12) return;                         // already on target

        float t = 1f - (float) Math.exp(-speed.get() * dt);                  // frame-rate independent easing
        float sy = dYaw * t, sp = dPitch * t;
        float cap = maxTurn.asFloat() * dt, mag = (float) Math.hypot(sy, sp);
        if (mag > cap && mag > 0) { sy *= cap / mag; sp *= cap / mag; }
        mc.player.changeLookDirection(sy / 0.15, sp / 0.15);
    }

    private Vec3d aimPoint(LivingEntity e, float delta) {
        Vec3d lp = e.getLerpedPos(delta);
        double k = part.get() == Part.HEAD ? 0.9 : part.get() == Part.CHEST ? 0.65 : 0.15;
        return new Vec3d(lp.x, lp.y + e.getHeight() * k, lp.z);
    }

    private boolean typeOk(Entity e) {
        if (e == mc.player || !(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive() || e.isSpectator()) return false;
        boolean pl = e instanceof PlayerEntity;
        return switch (targets.get()) {
            case PLAYERS -> pl;
            case MOBS -> !pl && (!hostileOnly.get() || e instanceof HostileEntity);
            case BOTH -> pl || !hostileOnly.get() || e instanceof HostileEntity;
        };
    }

    /** Angular distance (degrees) from the crosshair to the entity's aim point. */
    private double angle(LivingEntity e, float delta) {
        Vec3d aim = aimPoint(e, delta), eye = mc.player.getEyePos();
        double dx = aim.x - eye.x, dy = aim.y - eye.y, dz = aim.z - eye.z;
        float yawT = (float) Math.toDegrees(Math.atan2(-dx, dz)), pitchT = (float) -Math.toDegrees(Math.atan2(dy, Math.hypot(dx, dz)));
        return Math.hypot(MathHelper.wrapDegrees(yawT - mc.player.getYaw()), pitchT - mc.player.getPitch());
    }

    private boolean valid(LivingEntity e, float delta, double fovScale) {
        if (!typeOk(e)) return false;
        if (mc.player.squaredDistanceTo(e) > range.get() * range.get() * fovScale) return false;
        if (visibleOnly.get() && !mc.player.canSee(e)) return false;
        return angle(e, delta) <= fov.get() / 2.0 * fovScale;
    }

    private LivingEntity pick(float delta) {
        LivingEntity best = null; double bestAngle = 1e9;
        for (Entity e : mc.world.getEntities()) {
            if (!(e instanceof LivingEntity le) || !valid(le, delta, 1.0)) continue;
            double a = angle(le, delta);
            if (a < bestAngle) { bestAngle = a; best = le; }
        }
        return best;
    }
}
