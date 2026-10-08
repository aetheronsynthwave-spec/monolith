package dev.monolith.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * Reusable 2D primitives built only on DrawContext.fill (batched by vanilla, so cheap).
 * Rounded corners are drawn as row spans: no textures, no shaders, resolution independent.
 * NOTE: true gaussian blur needs a post-process shader; "blur()" here is a frosted-glass approximation
 * (layered translucent fills) which costs almost nothing.
 */
public final class Render2D {
    private Render2D() {}

    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0, Math.min(1, t));
        int r = 0;
        for (int s = 24; s >= 0; s -= 8) { int ca = (a >> s) & 255, cb = (b >> s) & 255; r |= ((int) (ca + (cb - ca) * t) & 255) << s; }
        return r;
    }
    public static int withAlpha(int c, float a) { return ((int) (((c >>> 24) * a)) << 24) | (c & 0xFFFFFF); }

    public static void rect(DrawContext c, float x, float y, float w, float h, int col) { c.fill((int) x, (int) y, (int) (x + w), (int) (y + h), col); }

    public static void rounded(DrawContext c, float fx, float fy, float fw, float fh, float radius, int col) {
        int x = (int) fx, y = (int) fy, w = (int) fw, h = (int) fh;
        int r = (int) Math.min(radius, Math.min(w, h) / 2f);
        if (r <= 0) { c.fill(x, y, x + w, y + h, col); return; }
        c.fill(x, y + r, x + w, y + h - r, col);
        for (int i = 0; i < r; i++) {
            double dy = r - i - 0.5;
            int inset = (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
            c.fill(x + inset, y + i, x + w - inset, y + i + 1, col);
            c.fill(x + inset, y + h - i - 1, x + w - inset, y + h - i, col);
        }
    }

    /** 1px rounded border: outer rounded fill in border colour, inner rounded fill in fill colour. */
    public static void roundedBordered(DrawContext c, float x, float y, float w, float h, float r, int fill, int border) {
        rounded(c, x, y, w, h, r, border);
        rounded(c, x + 1, y + 1, w - 2, h - 2, Math.max(0, r - 1), fill);
    }

    public static void outline(DrawContext c, int x, int y, int w, int h, int col) {
        c.fill(x, y, x + w, y + 1, col); c.fill(x, y + h - 1, x + w, y + h, col);
        c.fill(x, y, x + 1, y + h, col); c.fill(x + w - 1, y, x + w, y + h, col);
    }

    public static void gradientV(DrawContext c, int x, int y, int w, int h, int top, int bottom) { c.fillGradient(x, y, x + w, y + h, top, bottom); }

    /** Soft drop shadow: stacked expanding translucent rounded rects. */
    public static void shadow(DrawContext c, float x, float y, float w, float h, float r, int spread, float strength) {
        for (int i = spread; i > 0; i--) {
            float a = strength * (1f - i / (float) (spread + 1)) * 0.25f;
            rounded(c, x - i, y - i + 2, w + i * 2, h + i * 2, r + i, ((int) (a * 255) << 24));
        }
    }

    /** Glow = shadow in a light colour. */
    public static void glow(DrawContext c, float x, float y, float w, float h, float r, int spread, int col) {
        for (int i = spread; i > 0; i--) {
            float a = (1f - i / (float) (spread + 1)) * 0.12f;
            rounded(c, x - i, y - i, w + i * 2, h + i * 2, r + i, withAlpha(col, a));
        }
    }

    /** Frosted-glass approximation. */
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
}
