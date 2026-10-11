package dev.monolith.render;

import dev.monolith.module.impl.ESPStyle;
import net.minecraft.class_238;
import net.minecraft.class_332;

/**
 * Compatibility layer. Older modules (HitboxESP, XRay, Waypoints...) still call Wire.box()/Wire.tracer() from their onHud code.
 * These calls used to paint 2D lines on the screen (the "rough" look). They now hand the shape to Render3D, which draws it as
 * real world-space geometry (filled faces + outlines) in the next world pass. Signatures are unchanged, so no module needs editing.
 */
public final class Wire {
    private Wire() {}
    private static final double NEAR = 0.05;
    private static final double[] cam = new double[3];

    /** Returns false when the whole box is behind the camera (callers use that to skip labels). */
    public static boolean box(class_332 g, class_238 b, int col, int th) {
        if (!Projector.valid || (col >>> 24) == 0) return false;
        boolean front = false;
        for (int i = 0; i < 8 && !front; i++) {
            Projector.toCam((i & 1) == 0 ? b.field_1323 : b.field_1320, (i & 2) == 0 ? b.field_1322 : b.field_1325, (i & 4) == 0 ? b.field_1321 : b.field_1324, cam, 0);
            front = cam[2] >= NEAR;
        }
        if (!front) return false;
        Render3D.queueBox(b, col, ESPStyle.pixels(th));
        return true;
    }

    /** Line from the bottom-centre (or crosshair) to a world point. */
    public static void tracer(class_332 g, double wx, double wy, double wz, int col, boolean fromCrosshair, int th) {
        if (!Projector.valid || (col >>> 24) == 0) return;
        Render3D.queueTracer(wx, wy, wz, col, fromCrosshair, ESPStyle.pixels(th));
    }
}
