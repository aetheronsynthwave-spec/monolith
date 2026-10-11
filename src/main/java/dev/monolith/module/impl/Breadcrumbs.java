package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.class_243;

/** Leaves a glowing 3D trail where you have walked, so you can find your way back in caves, the Nether or a maze. */
public class Breadcrumbs extends Module {
    public final NumberSetting length = add(new NumberSetting("Trail Length", 240, 40, 1200, 20));
    public final NumberSetting width = add(new NumberSetting("Line Width", 2, 1, 4, 1));
    public final ColorSetting start = add(new ColorSetting("Newest Color", 0xFF38D9E8));
    public final ColorSetting end = add(new ColorSetting("Oldest Color", 0xFF7C4DFF));
    public final BoolSetting walls = add(new BoolSetting("Through Walls", true));

    private double[] pts = new double[3 * 1300];
    private int head, count, tick;

    public Breadcrumbs() { super("Breadcrumbs", "3D trail of where you have been.", Category.RENDER); }

    @Override public void onDisable() { count = 0; }
    @Override public void onDisconnect() { count = 0; }

    @Override public void onTick() {
        if (mc.field_1724 == null || (tick++ & 1) != 0) return;                  // one point every 2 ticks
        double x = mc.field_1724.method_23317(), y = mc.field_1724.method_23318() + 0.15, z = mc.field_1724.method_23321();
        if (count > 0) {
            int last = ((head - 1 + capacity()) % capacity()) * 3;
            double dx = x - pts[last], dy = y - pts[last + 1], dz = z - pts[last + 2];
            if (dx * dx + dy * dy + dz * dz < 0.04) return;                       // standing still: add nothing
            if (dx * dx + dy * dy + dz * dz > 64 * 64) count = 0;                 // teleported / changed dimension: start a new trail
        }
        pts[head * 3] = x; pts[head * 3 + 1] = y; pts[head * 3 + 2] = z;
        head = (head + 1) % capacity();
        if (count < capacity()) count++;
    }

    private int capacity() { return pts.length / 3; }

    @Override public void onRender3D(float delta) {
        int n = Math.min(count, length.asInt());
        if (n < 2 || mc.field_1724 == null) return;
        float w = ESPStyle.pixels(width.asInt());
        boolean prev = Render3D.xray();
        Render3D.xray(walls.get());
        class_243 now = mc.field_1724.method_30950(delta);
        double ax = now.field_1352, ay = now.field_1351 + 0.15, az = now.field_1350;   // smooth end point: no stutter between ticks
        for (int k = 0; k < n; k++) {
            int idx = ((head - 1 - k) % capacity() + capacity()) % capacity() * 3;
            double bx = pts[idx], by = pts[idx + 1], bz = pts[idx + 2];
            float t = k / (float) (n - 1);
            int col = Render2D.lerpColor(start.get(), end.get(), t);
            Render3D.line(ax, ay, az, bx, by, bz, Render2D.withAlpha(col, 1f - t * 0.85f), w);
            ax = bx; ay = by; az = bz;
        }
        Render3D.xray(prev);
    }
}
