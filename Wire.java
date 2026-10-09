package dev.monolith.render;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.Box;

/** True 3D wireframe boxes and tracers, drawn as clipped 2D lines (see Render2D.line). Works through walls. */
public final class Wire {
    private Wire() {}
    private static final double NEAR = 0.05;
    private static final double[] cam = new double[24];
    private static final double[] tmp = new double[3];
    // corner i: x=(i&1) y=(i&2) z=(i&4); every edge joins corners that differ in exactly one bit
    private static final int[][] EDGES = {{0,1},{2,3},{4,5},{6,7},{0,2},{1,3},{4,6},{5,7},{0,4},{1,5},{2,6},{3,7}};

    /** Draw the 12 edges of a box. Returns false when the whole box is behind the camera. */
    public static boolean box(DrawContext g, Box b, int col, int th) {
        if (!Projector.valid || (col >>> 24) == 0) return false;
        int front = 0;
        for (int i = 0; i < 8; i++) {
            Projector.toCam((i & 1) == 0 ? b.minX : b.maxX, (i & 2) == 0 ? b.minY : b.maxY, (i & 4) == 0 ? b.minZ : b.maxZ, cam, i * 3);
            if (cam[i * 3 + 2] >= NEAR) front++;
        }
        if (front == 0) return false;
        for (int[] e : EDGES) seg(g, e[0], e[1], col, th);
        return true;
    }

    private static void seg(DrawContext g, int a, int b, int col, int th) {
        double ax = cam[a * 3], ay = cam[a * 3 + 1], az = cam[a * 3 + 2], bx = cam[b * 3], by = cam[b * 3 + 1], bz = cam[b * 3 + 2];
        if (az < NEAR && bz < NEAR) return;
        if (az < NEAR) { double t = (NEAR - az) / (bz - az); ax += (bx - ax) * t; ay += (by - ay) * t; az = NEAR; }
        else if (bz < NEAR) { double t = (NEAR - bz) / (az - bz); bx += (ax - bx) * t; by += (ay - by) * t; bz = NEAR; }
        double f = Projector.focal, hw = Projector.W / 2.0, hh = Projector.H / 2.0;
        Render2D.line(g, hw + ax / az * f, hh - ay / az * f, hw + bx / bz * f, hh - by / bz * f, col, th);
    }

    /** Line from the bottom-centre (or crosshair) to a world point. Points behind you aim toward where they are. */
    public static void tracer(DrawContext g, double wx, double wy, double wz, int col, boolean fromCrosshair, int th) {
        if (!Projector.valid) return;
        Projector.toCam(wx, wy, wz, tmp, 0);
        double xc = tmp[0], yc = tmp[1], zc = tmp[2], sx, sy;
        double W = Projector.W, H = Projector.H;
        if (zc >= NEAR) { sx = W / 2 + xc / zc * Projector.focal; sy = H / 2 - yc / zc * Projector.focal; }
        else {
            double len = Math.hypot(xc, yc);
            if (len < 1e-6) { sx = W / 2; sy = H * 2; }
            else { double k = (W + H) / len; sx = W / 2 + xc * k; sy = H / 2 - yc * k; }
        }
        Render2D.line(g, W / 2, fromCrosshair ? H / 2 : H, sx, sy, col, th);
    }
}
