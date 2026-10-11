package dev.monolith.module.impl;

import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_310;
import net.minecraft.entity.*;

/** Small helpers shared by the aim modules. */
final class AimHelper {
    private AimHelper() {}

    static boolean typeOk(class_1297 e, Aimbot.Targets t, boolean hostileOnly) {
        class_310 mc = class_310.method_1551();
        if (e == mc.field_1724 || !(e instanceof class_1309 le) || e instanceof class_1531 || !le.method_5805() || e.method_7325()) return false;
        boolean pl = e instanceof class_1657;
        return switch (t) {
            case PLAYERS -> pl;
            case MOBS -> !pl && (!hostileOnly || e instanceof class_1588);
            case BOTH -> pl || !hostileOnly || e instanceof class_1588;
        };
    }

    /** Eased, speed-capped rotation using the same path as mouse movement (keeps the camera smooth). */
    static void rotate(float dYaw, float dPitch, float dt, float speed, float maxTurn) {
        class_310 mc = class_310.method_1551();
        float t = 1f - (float) Math.exp(-speed * dt);
        float sy = dYaw * t, sp = dPitch * t;
        float cap = maxTurn * dt, mag = (float) Math.hypot(sy, sp);
        if (mag > cap && mag > 0) { sy *= cap / mag; sp *= cap / mag; }
        mc.field_1724.method_5872(sy / 0.15, sp / 0.15);
    }
}
