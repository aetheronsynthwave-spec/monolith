package dev.monolith.render;

import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_310;
import net.minecraft.class_332;

/** Projects world positions onto the (GUI-scaled) screen. Camera basis is computed once per frame and shared by every ESP-style module. */
public final class Projector {
    private Projector() {}
    public static boolean valid;
    public static double cx, cy, cz, fx, fy, fz, rx, ry, rz, ux, uy, uz, focal;
    public static int W, H;

    public static void update(class_332 c, float delta) {
        class_310 mc = class_310.method_1551();
        valid = mc.field_1724 != null && mc.field_1687 != null;
        if (!valid) return;
        net.minecraft.class_1297 ce = mc.method_1560() != null ? mc.method_1560() : mc.field_1724;
        class_243 cam = ce.method_5836(delta);
        cx = cam.field_1352; cy = cam.field_1351; cz = cam.field_1350;
        double yaw = Math.toRadians(ce.method_5705(delta)), pitch = Math.toRadians(ce.method_5695(delta));
        fx = -Math.sin(yaw) * Math.cos(pitch); fy = -Math.sin(pitch); fz = Math.cos(yaw) * Math.cos(pitch);
        rx = -Math.cos(yaw); ry = 0; rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy; uy = rz * fx - rx * fz; uz = rx * fy - ry * fx;
        W = c.method_51421(); H = c.method_51443();
        focal = (H / 2.0) / Math.tan(Math.toRadians(mc.field_1690.method_41808().method_41753()) / 2.0);
    }

    /** out = {screenX, screenY, depth}. Returns false when the point is behind the camera. */
    public static boolean point(double x, double y, double z, double[] out) {
        double dx = x - cx, dy = y - cy, dz = z - cz;
        double depth = dx * fx + dy * fy + dz * fz;
        if (depth < 0.05) return false;
        out[0] = W / 2.0 + (dx * rx + dy * ry + dz * rz) / depth * focal;
        out[1] = H / 2.0 - (dx * ux + dy * uy + dz * uz) / depth * focal;
        out[2] = depth;
        return true;
    }

    /** Screen rectangle {x, y, w, h} around a world box. False if behind the camera or fully off-screen. */
    public static boolean box(class_238 b, int[] out) {
        double mx = (b.field_1323 + b.field_1320) / 2 - cx, my = (b.field_1322 + b.field_1325) / 2 - cy, mz = (b.field_1321 + b.field_1324) / 2 - cz;
        if (mx * fx + my * fy + mz * fz < 0.1) return false;
        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
        for (int i = 0; i < 8; i++) {
            double dx = ((i & 1) == 0 ? b.field_1323 : b.field_1320) - cx, dy = ((i & 2) == 0 ? b.field_1322 : b.field_1325) - cy, dz = ((i & 4) == 0 ? b.field_1321 : b.field_1324) - cz;
            double z = Math.max(0.1, dx * fx + dy * fy + dz * fz);
            double sx = W / 2.0 + (dx * rx + dy * ry + dz * rz) / z * focal, sy = H / 2.0 - (dx * ux + dy * uy + dz * uz) / z * focal;
            if (sx < minX) minX = sx; if (sx > maxX) maxX = sx; if (sy < minY) minY = sy; if (sy > maxY) maxY = sy;
        }
        if (maxX < 0 || minX > W || maxY < 0 || minY > H) return false;
        out[0] = (int) minX; out[1] = (int) minY; out[2] = Math.max(3, (int) (maxX - minX)); out[3] = Math.max(3, (int) (maxY - minY));
        return true;
    }

    /** Camera-space coordinates {right, up, depth} written into out[off..off+2]. */
    public static void toCam(double wx, double wy, double wz, double[] out, int off) {
        double dx = wx - cx, dy = wy - cy, dz = wz - cz;
        out[off] = dx * rx + dy * ry + dz * rz;
        out[off + 1] = dx * ux + dy * uy + dz * uz;
        out[off + 2] = dx * fx + dy * fy + dz * fz;
    }
}
