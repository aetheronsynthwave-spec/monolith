package dev.monolith.hud;

import dev.monolith.module.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.client.gui.DrawContext;

/** A module with a draggable on-screen element. x/y are stored as fractions of the screen so layouts survive resolution changes. */
public abstract class HudModule extends Module {
    public final NumberSetting x, y, scale;
    public final BoolSetting background = add(new BoolSetting("Background", true));
    public final ColorSetting textColor = add(new ColorSetting("Text Color", 0xFFFFFFFF));
    /** Unscaled content size, written by draw(). */
    public int contentW = 60, contentH = 10;

    protected HudModule(String name, String desc, double dx, double dy) {
        super(name, desc, Category.HUD);
        x = add(new NumberSetting("X", dx, 0, 1, 0.001)); y = add(new NumberSetting("Y", dy, 0, 1, 0.001));
        scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.5, 0.05));
    }

    public int px(int sw) { return (int) (x.get() * sw); }
    public int py(int sh) { return (int) (y.get() * sh); }
    public int w() { return (int) (contentW * scale.get()); }
    public int h() { return (int) (contentH * scale.get()); }

    /** Draw content at (0,0); set contentW/contentH. */
    protected abstract void draw(DrawContext ctx);

    @Override public void onHud(DrawContext ctx, float delta) {
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        var mat = ctx.getMatrices();
        mat.pushMatrix();
        mat.translate(px(sw), py(sh));
        mat.scale(scale.asFloat(), scale.asFloat());
        draw(ctx);
        mat.popMatrix();
    }

    protected void panel(DrawContext ctx, int w, int h) {
        contentW = w; contentH = h;
        if (background.get()) Render2D.rounded(ctx, 0, 0, w, h, 3, Theme.HUD_BG);
    }

    /** Convenience for single-line elements. */
    protected void line(DrawContext ctx, String label, String value) {
        String s = label + "  " + value;
        int w = Render2D.width(s) + 10;
        panel(ctx, w, 16);
        Render2D.text(ctx, label, 5, 4, Theme.TEXT_DIM);
        Render2D.text(ctx, value, 5 + Render2D.width(label + "  "), 4, textColor.get());
    }
}
