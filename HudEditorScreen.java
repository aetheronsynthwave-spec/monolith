package dev.monolith.gui;

import dev.monolith.Monolith;
import dev.monolith.hud.HudModule;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/** Drag HUD elements; scroll over one to scale it. Elements are shown even if their module is off (dimmed) so they can be placed. */
public class HudEditorScreen extends Screen {
    private final Screen parent; private HudModule drag; private double ox, oy;
    public HudEditorScreen(Screen parent) { super(Text.literal("HUD Editor")); this.parent = parent; }
    @Override public boolean shouldPause() { return false; }
    @Override public void renderBackground(DrawContext c, int mx, int my, float d) {}

    @Override public void render(DrawContext c, int mx, int my, float d) {
        Render2D.rect(c, 0, 0, width, height, 0x70000000);
        Render2D.rect(c, width / 2, 0, 1, height, 0x22FFFFFF); Render2D.rect(c, 0, height / 2, width, 1, 0x22FFFFFF);   // centre guides
        Render2D.centered(c, "HUD Editor  -  drag to move, scroll to scale, Esc to finish", width / 2f, 8, Theme.TEXT_DIM);
        for (Module m : Monolith.modules().all()) if (m instanceof HudModule h && h.isEnabled()) {
            h.onHud(c, d);
            Render2D.outline(c, h.px(width), h.py(height), Math.max(8, h.w()), Math.max(8, h.h()), h == drag ? 0xFFFFFFFF : 0x55FFFFFF);
        }
    }
    private HudModule at(double mx, double my) {
        HudModule f = null;
        for (Module m : Monolith.modules().all()) if (m instanceof HudModule h && h.isEnabled() && Render2D.hover(mx, my, h.px(width), h.py(height), Math.max(8, h.w()), Math.max(8, h.h()))) f = h;
        return f;
    }
    @Override public boolean mouseClicked(Click e, boolean dbl) { drag = at(e.x(), e.y()); if (drag != null) { ox = e.x() - drag.px(width); oy = e.y() - drag.py(height); } return true; }
    @Override public boolean mouseDragged(Click e, double dx, double dy) {
        if (drag == null) return false;
        double nx = e.x() - ox, ny = e.y() - oy;
        // snap to screen centre lines
        if (Math.abs(nx + drag.w() / 2.0 - width / 2.0) < 4) nx = width / 2.0 - drag.w() / 2.0;
        drag.x.set(Math.max(0, nx) / width); drag.y.set(Math.max(0, ny) / height); return true;
    }
    @Override public boolean mouseReleased(Click e) { drag = null; return true; }
    @Override public boolean mouseScrolled(double mx, double my, double h, double v) { HudModule t = at(mx, my); if (t != null) t.scale.set(t.scale.get() + v * 0.05); return true; }
    @Override public boolean keyPressed(KeyInput in) { if (in.key() == 256) { Monolith.config().save(Monolith.config().active()); client.setScreen(parent); return true; } return false; }
}
