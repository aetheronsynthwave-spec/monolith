package dev.monolith.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

/** Projects world positions onto the (GUI-scaled) screen. Camera basis is computed once per frame and shared by every ESP-style module. */
public final class Projector {
    private Projector() {}
    public static boolean valid;
    public static double cx, cy, cz, fx, fy, fz, rx, ry, rz, ux, uy, uz, focal;
    public static int W, H;

    public static void update(DrawContext c, float delta) {
        MinecraftClient mc = MinecraftClient.getInstance();
        valid = mc.player != null && mc.world != null;
        if (!valid) return;
        Vec3d cam = mc.player.getCameraPosVec(delta);
        cx = cam.x; cy = cam.y; cz = cam.z;
        double yaw = Math.toRadians(mc.player.getYaw(delta)), pitch = Math.toRadians(mc.player.getPitch(delta));
        fx = -Math.sin(yaw) * Math.cos(pitch); fy = -Math.sin(pitch); fz = Math.cos(yaw) * Math.cos(pitch);
        rx = -Math.cos(yaw); ry = 0; rz = -Math.sin(yaw);
        ux = ry * fz - rz * fy; uy = rz * fx - rx * fz; uz = rx * fy - ry * fx;
        W = c.getScaledWindowWidth(); H = c.getScaledWindowHeight();
        focal = (H / 2.0) / Math.tan(Math.toRadians(mc.options.getFov().getValue()) / 2.0);
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
    public static boolean box(Box b, int[] out) {
        double mx = (b.minX + b.maxX) / 2 - cx, my = (b.minY + b.maxY) / 2 - cy, mz = (b.minZ + b.maxZ) / 2 - cz;
        if (mx * fx + my * fy + mz * fz < 0.1) return false;
        double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
        for (int i = 0; i < 8; i++) {
            double dx = ((i & 1) == 0 ? b.minX : b.maxX) - cx, dy = ((i & 2) == 0 ? b.minY : b.maxY) - cy, dz = ((i & 4) == 0 ? b.minZ : b.maxZ) - cz;
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
