package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.Render2D;
import dev.monolith.setting.*;
import net.minecraft.class_332;

/** Replaces the vanilla crosshair (see HudElementRegistry hook in Monolith.java). */
public class CrosshairModule extends Module {
    public enum Shape { CROSS, PLUS_DOT, T_SHAPE, DOT }
    public final EnumSetting<Shape> shape = add(new EnumSetting<>("Shape", Shape.CROSS));
    public final NumberSetting size = add(new NumberSetting("Size", 5, 1, 20, 1));
    public final NumberSetting gap = add(new NumberSetting("Gap", 3, 0, 15, 1));
    public final NumberSetting thickness = add(new NumberSetting("Thickness", 1, 1, 5, 1));
    public final BoolSetting outline = add(new BoolSetting("Outline", true));
    public final BoolSetting dynamic = add(new BoolSetting("Dynamic Gap", false));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFFFF));

    public CrosshairModule() { super("Crosshair", "Custom monochrome crosshair.", Category.RENDER); }

    public void draw(class_332 c) {
        if (mc.field_1755 != null || mc.field_1724 == null) return;
        int cx = c.method_51421() / 2, cy = c.method_51443() / 2;
        int s = size.asInt(), t = thickness.asInt(), col = color.get(), o = 0xB0000000;
        int g = gap.asInt();
        if (dynamic.get()) g += Math.min(8, (int) (mc.field_1724.method_18798().method_37267() * 20));
        int h = t / 2;
        Shape sh = shape.get();
        boolean bars = sh != Shape.DOT;
        if (bars) {
            if (sh != Shape.T_SHAPE) bar(c, cx - h, cy - g - s, t, s, col, o);
            bar(c, cx - h, cy + g + 1, t, s, col, o);
            bar(c, cx - g - s, cy - h, s, t, col, o);
            bar(c, cx + g + 1, cy - h, s, t, col, o);
        }
        if (sh == Shape.DOT || sh == Shape.PLUS_DOT) bar(c, cx - h, cy - h, t, t, col, o);
    }

    private void bar(class_332 c, int x, int y, int w, int h, int col, int outCol) {
        if (outline.get()) Render2D.rect(c, x - 1, y - 1, w + 2, h + 2, outCol);
        Render2D.rect(c, x, y, w, h, col);
    }
}
