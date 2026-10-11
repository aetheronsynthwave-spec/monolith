package dev.monolith.render;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.monolith.module.impl.ESPStyle;
import java.util.Arrays;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.class_10789;
import net.minecraft.class_10799;
import net.minecraft.class_12245;
import net.minecraft.class_12246;
import net.minecraft.class_1921;
import net.minecraft.class_238;
import net.minecraft.class_290;
import net.minecraft.class_2960;
import net.minecraft.class_310;
import net.minecraft.class_4184;
import net.minecraft.class_4587;
import net.minecraft.class_4588;
import net.minecraft.class_4597;
import net.minecraft.class_9799;

/**
 * World-space drawing for every ESP-style module: real 3D lines AND real filled faces, depth-tested or through walls.
 *
 * How it works (this is also the main optimisation): nothing is sent to the GPU while modules call box()/line()/fillBox().
 * Each call only appends a few floats to a plain array. In end() the four batches (fill+walls, fill+depth, lines+walls,
 * lines+depth) are written into four separate buffers and drawn with four draw calls in total, no matter how many boxes exist.
 *
 * Modules that still draw in onHud (old code) go through {@link Wire}, which calls queue*(): those shapes are drawn in the
 * NEXT world pass (one frame later, invisible at normal frame rates).
 */
public final class Render3D {
    private Render3D() {}

    // ---- camera, shared with Projector / GlowRenderer (names and types are used by other classes: do not rename)
    public static double cx, cy, cz, fx, fy, fz, ux, uy, uz;

    // ---- pipelines / layers
    private static final RenderPipeline LINE_WALLS_P = linePipe("pipeline/esp_lines", DepthTestFunction.NO_DEPTH_TEST);
    private static final RenderPipeline LINE_DEPTH_P = linePipe("pipeline/esp_lines_depth", DepthTestFunction.LEQUAL_DEPTH_TEST);
    private static final RenderPipeline FILL_WALLS_P = fillPipe("pipeline/esp_fill", DepthTestFunction.NO_DEPTH_TEST);
    private static final RenderPipeline FILL_DEPTH_P = fillPipe("pipeline/esp_fill_depth", DepthTestFunction.LEQUAL_DEPTH_TEST);

    private static final class_1921 LINE_WALLS = layer("monolith_esp_lines", LINE_WALLS_P, true);
    private static final class_1921 LINE_DEPTH = layer("monolith_esp_lines_depth", LINE_DEPTH_P, true);
    private static final class_1921 FILL_WALLS = layer("monolith_esp_fill", FILL_WALLS_P, false);
    private static final class_1921 FILL_DEPTH = layer("monolith_esp_fill_depth", FILL_DEPTH_P, false);

    /** Kept for binary compatibility with 1.0.2 (the through-walls line layer). */
    public static final class_1921 LINES = LINE_WALLS;

    // One Immediate per layer: switching layers inside a single Immediate would flush it every time.
    private static final class_4597.class_4598 IMM_LINE_WALLS = class_4597.method_22991(new class_9799(0x10000));
    private static final class_4597.class_4598 IMM_LINE_DEPTH = class_4597.method_22991(new class_9799(0x10000));
    private static final class_4597.class_4598 IMM_FILL_WALLS = class_4597.method_22991(new class_9799(0x20000));
    private static final class_4597.class_4598 IMM_FILL_DEPTH = class_4597.method_22991(new class_9799(0x20000));

    private static RenderPipeline linePipe(String name, DepthTestFunction depth) {
        return RenderPipeline.builder(class_10799.field_56859)            // vanilla line shader + format
                .withLocation(class_2960.method_60655("monolith", name))
                .withDepthTestFunction(depth).withDepthWrite(false)
                .withBlend(BlendFunction.TRANSLUCENT).withCull(false).build();
    }

    private static RenderPipeline fillPipe(String name, DepthTestFunction depth) {
        return RenderPipeline.builder()                                   // own tiny shader: assets/monolith/shaders/esp_fill.*
                .withLocation(class_2960.method_60655("monolith", name))
                .withUniform("DynamicTransforms", class_10789.field_60031)
                .withUniform("Projection", class_10789.field_60031)
                .withVertexShader(class_2960.method_60655("monolith", "esp_fill"))
                .withFragmentShader(class_2960.method_60655("monolith", "esp_fill"))
                .withVertexFormat(class_290.field_1575, VertexFormat.class_5596.field_27382)
                .withBlend(BlendFunction.TRANSLUCENT).withDepthTestFunction(depth)
                .withDepthWrite(false).withCull(false).build();
    }

    private static class_1921 layer(String name, RenderPipeline p, boolean lines) {
        var b = net.minecraft.class_12247.method_75927(p);
        if (lines) b = b.method_75930(class_12245.field_63976);
        b = b.method_75931(class_12246.field_63980);
        if (!lines) b = b.method_75929(0x40000);
        return class_1921.method_75940(name, b.method_75938());
    }

    // ---- growable float batches
    private static final class Buf {
        float[] a = new float[2048]; int n;
        int reserve(int k) { if (n + k > a.length) a = Arrays.copyOf(a, Math.max(a.length * 2, n + k)); int s = n; n += k; return s; }
    }
    private static final int LINE_STRIDE = 8, FILL_STRIDE = 7, Q_STRIDE = 9;
    private static final int MAX_LINES = 30000, MAX_FILLS = 6000, MAX_QUEUE = 4000;
    private static final Buf LW = new Buf(), LD = new Buf(), FW = new Buf(), FD = new Buf();
    // Queue keeps full double precision (world coordinates), unlike the camera-relative float batches above.
    private static double[] q = new double[1024];
    private static int qn;

    private static class_4587.class_4665 entry;
    private static boolean active;
    private static boolean xray = true;

    /** Through-walls toggle for the shapes drawn after this call. begin() resets it from ESPStyle every frame. */
    public static void xray(boolean on) { xray = on; }
    public static boolean xray() { return xray; }

    // ======================================================================== frame control
    public static boolean begin(WorldRenderContext ctx) {
        active = false;
        try {
            if (ctx.matrices() == null || ctx.worldState() == null) return false;
            var pos = ctx.worldState().field_63082.field_63078;
            cx = pos.field_1352; cy = pos.field_1351; cz = pos.field_1350;
            class_4184 cam = class_310.method_1551().field_1773.method_19418();
            double yaw = Math.toRadians(cam.method_19330()), pitch = Math.toRadians(cam.method_19329());
            fx = -Math.sin(yaw) * Math.cos(pitch); fy = -Math.sin(pitch); fz = Math.cos(yaw) * Math.cos(pitch);
            double rx = -Math.cos(yaw), rz = -Math.sin(yaw);                   // right vector (y = 0)
            ux = -rz * fy; uy = rz * fx - rx * fz; uz = rx * fy;               // up = right x forward
            entry = ctx.matrices().method_23760();
            LW.n = LD.n = FW.n = FD.n = 0;
            xray = ESPStyle.walls();
            active = true;
            drainQueue();                                                      // shapes queued by Wire during last frame's HUD pass
            return true;
        } catch (Throwable t) { active = false; return false; }
    }

    public static void end() {
        if (!active) { qn = 0; return; }
        active = false;
        try {
            drawFills(FD, IMM_FILL_DEPTH, FILL_DEPTH);
            drawFills(FW, IMM_FILL_WALLS, FILL_WALLS);
            drawLines(LD, IMM_LINE_DEPTH, LINE_DEPTH);
            drawLines(LW, IMM_LINE_WALLS, LINE_WALLS);
        } finally { entry = null; }
    }

    // ======================================================================== public drawing API (call from Module.onRender3D)
    /** Outline only. width is in screen pixels. */
    public static void box(class_238 b, int argb, float width) {
        if (!active || (argb >>> 24) == 0) return;
        double x1 = b.field_1323, y1 = b.field_1322, z1 = b.field_1321, x2 = b.field_1320, y2 = b.field_1325, z2 = b.field_1324;
        line(x1, y1, z1, x2, y1, z1, argb, width); line(x1, y1, z2, x2, y1, z2, argb, width);
        line(x1, y2, z1, x2, y2, z1, argb, width); line(x1, y2, z2, x2, y2, z2, argb, width);
        line(x1, y1, z1, x1, y1, z2, argb, width); line(x2, y1, z1, x2, y1, z2, argb, width);
        line(x1, y2, z1, x1, y2, z2, argb, width); line(x2, y2, z1, x2, y2, z2, argb, width);
        line(x1, y1, z1, x1, y2, z1, argb, width); line(x2, y1, z1, x2, y2, z1, argb, width);
        line(x1, y1, z2, x1, y2, z2, argb, width); line(x2, y1, z2, x2, y2, z2, argb, width);
    }

    /** Six translucent faces. Alpha comes from argb. Faces are shaded (top bright, bottom dark) when ESPStyle says so. */
    public static void fillBox(class_238 b, int argb) {
        if (!active || (argb >>> 24) == 0) return;
        Buf buf = xray ? FW : FD;
        if (buf.n / FILL_STRIDE >= MAX_FILLS) return;
        int i = buf.reserve(FILL_STRIDE);
        float[] a = buf.a;
        a[i] = (float) (b.field_1323 - cx); a[i + 1] = (float) (b.field_1322 - cy); a[i + 2] = (float) (b.field_1321 - cz);
        a[i + 3] = (float) (b.field_1320 - cx); a[i + 4] = (float) (b.field_1325 - cy); a[i + 5] = (float) (b.field_1324 - cz);
        a[i + 6] = Float.intBitsToFloat(argb);
    }

    /** The style-aware box every ESP uses: honours ESPStyle (filled glass / outline / both, opacity, thickness, walls). */
    public static void espBox(class_238 b, int argb, float widthPx) {
        if (!active) return;
        int alpha = argb >>> 24;
        if (ESPStyle.fill()) fillBox(b, ((int) (alpha * ESPStyle.fillAlpha()) << 24) | (argb & 0xFFFFFF));
        if (ESPStyle.outline()) box(b, ((int) (alpha * ESPStyle.outlineAlpha()) << 24) | (argb & 0xFFFFFF), widthPx * ESPStyle.thickness());
    }

    /** One 3D line between two world points. width is in screen pixels. */
    public static void line(double x1, double y1, double z1, double x2, double y2, double z2, int argb, float width) {
        if (!active || (argb >>> 24) == 0) return;
        Buf buf = xray ? LW : LD;
        if (buf.n / LINE_STRIDE >= MAX_LINES) return;
        int i = buf.reserve(LINE_STRIDE);
        float[] a = buf.a;
        a[i] = (float) (x1 - cx); a[i + 1] = (float) (y1 - cy); a[i + 2] = (float) (z1 - cz);
        a[i + 3] = (float) (x2 - cx); a[i + 4] = (float) (y2 - cy); a[i + 5] = (float) (z2 - cz);
        a[i + 6] = Float.intBitsToFloat(argb); a[i + 7] = width;
    }

    /** Line from just in front of the camera (bottom of the screen, or the crosshair) to a world point. */
    public static void tracer(double wx, double wy, double wz, int argb, boolean fromCrosshair, float width) {
        if (!active) return;
        double sx = cx + fx * 2.0, sy = cy + fy * 2.0, sz = cz + fz * 2.0;
        if (!fromCrosshair) { sx -= ux * 1.1; sy -= uy * 1.1; sz -= uz * 1.1; }
        line(sx, sy, sz, wx, wy, wz, argb, width);
    }

    // ======================================================================== queue for modules that draw in onHud (via Wire)
    private static int qReserve() {
        if (qn + Q_STRIDE > q.length) q = Arrays.copyOf(q, q.length * 2);
        int i = qn; qn += Q_STRIDE; return i;
    }

    public static void queueBox(class_238 b, int argb, float width) {
        if (qn / Q_STRIDE >= MAX_QUEUE) return;
        int i = qReserve();
        q[i] = 0; q[i + 1] = b.field_1323; q[i + 2] = b.field_1322; q[i + 3] = b.field_1321;
        q[i + 4] = b.field_1320; q[i + 5] = b.field_1325; q[i + 6] = b.field_1324; q[i + 7] = argb; q[i + 8] = width;
    }

    public static void queueTracer(double wx, double wy, double wz, int argb, boolean fromCrosshair, float width) {
        if (qn / Q_STRIDE >= MAX_QUEUE) return;
        int i = qReserve();
        q[i] = 1; q[i + 1] = wx; q[i + 2] = wy; q[i + 3] = wz; q[i + 4] = fromCrosshair ? 1 : 0; q[i + 7] = argb; q[i + 8] = width;
    }

    private static void drainQueue() {
        for (int i = 0; i < qn; i += Q_STRIDE) {
            int col = (int) q[i + 7]; float w = (float) q[i + 8];
            if (q[i] == 0) espBox(new class_238(q[i + 1], q[i + 2], q[i + 3], q[i + 4], q[i + 5], q[i + 6]), col, w);
            else tracer(q[i + 1], q[i + 2], q[i + 3], col, q[i + 4] > 0.5, w);
        }
        qn = 0;
    }

    // ======================================================================== flush
    private static void drawFills(Buf buf, class_4597.class_4598 imm, class_1921 layer) {
        if (buf.n == 0) return;
        class_4588 v = imm.method_73477(layer);
        boolean shade = ESPStyle.shade();
        float[] a = buf.a;
        final float g = 0.002f;                                                // tiny grow: stops z-fighting with the block faces
        for (int i = 0; i < buf.n; i += FILL_STRIDE) {
            float x1 = a[i] - g, y1 = a[i + 1] - g, z1 = a[i + 2] - g, x2 = a[i + 3] + g, y2 = a[i + 4] + g, z2 = a[i + 5] + g;
            int c = Float.floatToRawIntBits(a[i + 6]);
            int top = c, bot = shade ? dim(c, 0.55f) : c, sx = shade ? dim(c, 0.80f) : c, sz = shade ? dim(c, 0.68f) : c;
            quad(v, x1, y2, z1, x2, y2, z1, x2, y2, z2, x1, y2, z2, top);      // +Y
            quad(v, x1, y1, z1, x1, y1, z2, x2, y1, z2, x2, y1, z1, bot);      // -Y
            quad(v, x1, y1, z1, x1, y2, z1, x1, y2, z2, x1, y1, z2, sx);       // -X
            quad(v, x2, y1, z1, x2, y1, z2, x2, y2, z2, x2, y2, z1, sx);       // +X
            quad(v, x1, y1, z1, x2, y1, z1, x2, y2, z1, x1, y2, z1, sz);       // -Z
            quad(v, x1, y1, z2, x1, y2, z2, x2, y2, z2, x2, y1, z2, sz);       // +Z
        }
        imm.method_22993();
    }

    private static void quad(class_4588 v, float ax, float ay, float az, float bx, float by, float bz,
                             float cx_, float cy_, float cz_, float dx, float dy, float dz, int c) {
        class_4587.class_4665 e = entry;
        v.method_56824(e, ax, ay, az).method_22913(0f, 0f).method_39415(c);
        v.method_56824(e, bx, by, bz).method_22913(0f, 0f).method_39415(c);
        v.method_56824(e, cx_, cy_, cz_).method_22913(0f, 0f).method_39415(c);
        v.method_56824(e, dx, dy, dz).method_22913(0f, 0f).method_39415(c);
    }

    private static int dim(int argb, float k) {
        return (argb & 0xFF000000) | ((int) (((argb >> 16) & 255) * k) << 16) | ((int) (((argb >> 8) & 255) * k) << 8) | (int) ((argb & 255) * k);
    }

    private static void drawLines(Buf buf, class_4597.class_4598 imm, class_1921 layer) {
        if (buf.n == 0) return;
        class_4588 v = imm.method_73477(layer);
        float[] a = buf.a;
        for (int i = 0; i < buf.n; i += LINE_STRIDE) {
            float x1 = a[i], y1 = a[i + 1], z1 = a[i + 2], x2 = a[i + 3], y2 = a[i + 4], z2 = a[i + 5];
            float dx = x2 - x1, dy = y2 - y1, dz = z2 - z1, len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 1e-5f) continue;
            dx /= len; dy /= len; dz /= len;
            int c = Float.floatToRawIntBits(a[i + 6]); float w = a[i + 7];
            v.method_56824(entry, x1, y1, z1).method_39415(c).method_60831(entry, dx, dy, dz).method_75298(w);
            v.method_56824(entry, x2, y2, z2).method_39415(c).method_60831(entry, dx, dy, dz).method_75298(w);
        }
        imm.method_22993();
    }
}
