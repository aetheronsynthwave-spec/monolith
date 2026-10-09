package dev.monolith.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Reusable 2D primitives built only on DrawContext.fill (batched by vanilla, so cheap).
 * Rounded corners are drawn as row spans using cached corner tables: no textures, no shaders, no per-call sqrt.
 * NOTE: true gaussian blur needs a post-process shader; "blur()" is a frosted-glass approximation.
 */
public final class Render2D {
    private Render2D() {}
    private static final int[][] INSETS = new int[48][];

    /** Horizontal inset of each corner row for radius r (cached). */
    private static int[] insets(int r) {
        int[] t = r < INSETS.length ? INSETS[r] : null;
        if (t != null) return t;
        t = new int[r];
        for (int i = 0; i < r; i++) { double dy = r - i - 0.5; t[i] = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy))); }
        if (r < INSETS.length) INSETS[r] = t;
        return t;
    }

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) { int ca = (a >> s) & 255, cb = (b >> s) & 255; r |= ((int) (ca + (cb - ca) * t) & 255) << s; }
        return r;
    }
    public static int withAlpha(int c, float a) { return ((int) (((c >>> 24) * Math.max(0, Math.min(1, a)))) << 24) | (c & 0xFFFFFF); }

    public static void rect(DrawContext c, float x, float y, float w, float h, int col) { c.fill((int) x, (int) y, (int) (x + w), (int) (y + h), col); }

    public static void rounded(DrawContext c, float fx, float fy, float fw, float fh, float radius, int col) {
        if ((col >>> 24) == 0) return;
        int x = (int) fx, y = (int) fy, w = (int) fw, h = (int) fh;
        int r = (int) Math.min(radius, Math.min(w, h) / 2f);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        int[] in = insets(r);
        for (int i = 0; i < r; i++) {
            c.fill(x + in[i], y + i, x + w - in[i], y + i + 1, col);
            c.fill(x + in[i], y + h - i - 1, x + w - in[i], y + h - i, col);
        }
    }

    public static void roundedBordered(DrawContext c, float x, float y, float w, float h, float r, int fill, int border) {
        rounded(c, x, y, w, h, r, border);
        rounded(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col); c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col); c.fill(x + w - 1, y, x + w, y + h, col);
    }

    public static void gradientV(DrawContext c, int x, int y, int w, int h, int top, int bottom) { c.fillGradient(x, y, x + w, y + h, top, bottom); }

    /** Soft drop shadow: expanding translucent rounded rects (every 2nd ring, so half the draw calls). */
    public static void shadow(DrawContext c, float x, float y, float w, float h, float r, int spread, float strength) {
        for (int i = spread; i > 0; i -= 2) {
            float a = strength * (1f - i / (float) (spread + 1)) * 0.5f;
            rounded(c, x - i, y - i + 2, w + i * 2, h + i * 2, r + i, ((int) (Math.min(1f, a) * 255) << 24) & 0x7F000000);
        }
    }

    public static void glow(DrawContext c, float x, float y, float w, float h, float r, int spread, int col) {
        for (int i = spread; i > 0; i -= 2) {
            float a = (1f - i / (float) (spread + 1)) * 0.2f;
            rounded(c, x - i, y - i, w + i * 2, h + i * 2, r + i, withAlpha(col, a));
        }
    }

    public static void blur(DrawContext c, float x, float y, float w, float h, float r) {
        rounded(c, x, y, w, h, r, 0xB0101010);
        rounded(c, x, y + 1, w, h * 0.5f, r, 0x10FFFFFF);
    }

    public static void text(DrawContext c, String s, float x, float y, int col) { c.drawText(MinecraftClient.getInstance().textRenderer, s, (int) x, (int) y, col, false); }
    public static void textShadow(DrawContext c, String s, float x, float y, int col) { c.drawText(MinecraftClient.getInstance().textRenderer, s, (int) x, (int) y, col, true); }
    public static int width(String s) { return MinecraftClient.getInstance().textRenderer.getWidth(s); }
    public static void centered(DrawContext c, String s, float cx, float y, int col) { text(c, s, cx - width(s) / 2f, y, col); }
    public static boolean hover(double mx, double my, float x, float y, float w, float h) { return mx >= x && mx <= x + w && my >= y && my <= y + h; }
    public static float approach(float cur, float target, float dt, float speed) { return cur + (target - cur) * Math.min(1f, dt * speed); }

    // ---------------- lines ----------------
    /**
     * Anti-gap line using axis-aligned runs: cost is min(|dx|,|dy|)+1 fills, so vertical/horizontal lines cost 1 fill.
     * Fully clipped to the screen, so huge off-screen coordinates are safe.
     */
    public static void line(DrawContext c, double x0, double y0, double x1, double y1, int col, int th) {
        if ((col >>> 24) == 0 || !Double.isFinite(x0 + y0 + x1 + y1)) return;
        int W = c.getScaledWindowWidth(), H = c.getScaledWindowHeight();
        if ((x0 < 0 && x1 < 0) || (x0 > W && x1 > W) || (y0 < 0 && y1 < 0) || (y0 > H && y1 > H)) return;
        if (Math.abs(x1 - x0) >= Math.abs(y1 - y0)) runs(c, x0, y0, x1, y1, col, th, W, H, false);
        else runs(c, y0, x0, y1, x1, col, th, H, W, true);
    }

    private static void runs(DrawContext c, double a0, double b0, double a1, double b1, int col, int th, int maxA, int maxB, boolean swap) {
        double da = a1 - a0, db = b1 - b0;
        int rowA = (int) Math.floor(b0), rowB = (int) Math.floor(b1);
        if (rowA == rowB) {
            if (rowA >= -1 && rowA <= maxB) fillRun(c, (int) Math.floor(Math.min(a0, a1)), (int) Math.floor(Math.max(a0, a1)), rowA, th, swap, maxA, col);
            return;
        }
        int step = rowB > rowA ? 1 : -1, start = rowA, end = rowB;
        if (step > 0) { start = Math.max(start, -1); end = Math.min(end, maxB); } else { start = Math.min(start, maxB); end = Math.max(end, -1); }
        if ((step > 0 && start > end) || (step < 0 && start < end)) return;
        double lo = Math.min(b0, b1), hi = Math.max(b0, b1);
        for (int r = start; ; r += step) {
            double ta = (Math.max(lo, Math.min(hi, r)) - b0) / db, tb = (Math.max(lo, Math.min(hi, r + 1)) - b0) / db;
            double aa = a0 + da * ta, ab = a0 + da * tb;
            fillRun(c, (int) Math.floor(Math.min(aa, ab)), (int) Math.floor(Math.max(aa, ab)), r, th, swap, maxA, col);
            if (r == end) break;
        }
    }

    private static void fillRun(DrawContext c, int lo, int hi, int row, int th, boolean swap, int maxA, int col) {
        lo = Math.max(lo, -1); hi = Math.min(hi, maxA + 1);
        if (lo > hi) return;
        if (swap) c.fill(row, lo, row + th, hi + 1, col); else c.fill(lo, row, hi + 1, row + th, col);
    }
}
