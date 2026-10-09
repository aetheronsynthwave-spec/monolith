package dev.monolith.module.impl;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;

/** Small helpers shared by the aim modules. */
final class AimHelper {
    private AimHelper() {}

    static boolean typeOk(Entity e, Aimbot.Targets t, boolean hostileOnly) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (e == mc.player || !(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive() || e.isSpectator()) return false;
        boolean pl = e instanceof PlayerEntity;
        return switch (t) {
            case PLAYERS -> pl;
            case MOBS -> !pl && (!hostileOnly || e instanceof HostileEntity);
            case BOTH -> pl || !hostileOnly || e instanceof HostileEntity;
        };
    }

    /** Eased, speed-capped rotation using the same path as mouse movement (keeps the camera smooth). */
    static void rotate(float dYaw, float dPitch, float dt, float speed, float maxTurn) {
        MinecraftClient mc = MinecraftClient.getInstance();
        float t = 1f - (float) Math.exp(-speed * dt);
        float sy = dYaw * t, sp = dPitch * t;
        float cap = maxTurn * dt, mag = (float) Math.hypot(sy, sp);
        if (mag > cap && mag > 0) { sy *= cap / mag; sp *= cap / mag; }
        mc.player.changeLookDirection(sy / 0.15, sp / 0.15);
    }
}
