package dev.monolith.gui;

import dev.monolith.Monolith;
import dev.monolith.hud.HudModule;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import net.minecraft.class_11908;
import net.minecraft.class_11909;
import net.minecraft.class_2561;
import net.minecraft.class_332;
import net.minecraft.class_437;

/** Drag HUD elements; scroll over one to scale it. Elements are shown even if their module is off (dimmed) so they can be placed. */
public class HudEditorScreen extends class_437 {
    private final class_437 parent; private HudModule drag; private double ox, oy;
    public HudEditorScreen(class_437 parent) { super(class_2561.method_43470("HUD Editor")); this.parent = parent; }
    @Override public boolean method_25421() { return false; }
    @Override public void method_25420(class_332 c, int mx, int my, float d) {}

    @Override public void method_25394(class_332 c, int mx, int my, float d) {
        Render2D.rect(c, 0, 0, field_22789, field_22790, 0x70000000);
        Render2D.rect(c, field_22789 / 2, 0, 1, field_22790, 0x22FFFFFF); Render2D.rect(c, 0, field_22790 / 2, field_22789, 1, 0x22FFFFFF);   // centre guides
        Render2D.centered(c, "HUD Editor  -  drag to move, scroll to scale, Esc to finish", field_22789 / 2f, 8, Theme.TEXT_DIM);
        for (Module m : Monolith.modules().all()) if (m instanceof HudModule h && h.isEnabled()) {
            h.onHud(c, d);
            Render2D.outline(c, h.px(field_22789), h.py(field_22790), Math.max(8, h.w()), Math.max(8, h.h()), h == drag ? 0xFFFFFFFF : 0x55FFFFFF);
        }
    }
    private HudModule at(double mx, double my) {
        HudModule f = null;
        for (Module m : Monolith.modules().all()) if (m instanceof HudModule h && h.isEnabled() && Render2D.hover(mx, my, h.px(field_22789), h.py(field_22790), Math.max(8, h.w()), Math.max(8, h.h()))) f = h;
        return f;
    }
    @Override public boolean method_25402(class_11909 e, boolean dbl) { drag = at(e.comp_4798(), e.comp_4799()); if (drag != null) { ox = e.comp_4798() - drag.px(field_22789); oy = e.comp_4799() - drag.py(field_22790); } return true; }
    @Override public boolean method_25403(class_11909 e, double dx, double dy) {
        if (drag == null) return false;
        double nx = e.comp_4798() - ox, ny = e.comp_4799() - oy;
        // snap to screen centre lines
        if (Math.abs(nx + drag.w() / 2.0 - field_22789 / 2.0) < 4) nx = field_22789 / 2.0 - drag.w() / 2.0;
        drag.x.set(Math.max(0, nx) / field_22789); drag.y.set(Math.max(0, ny) / field_22790); return true;
    }
    @Override public boolean method_25406(class_11909 e) { drag = null; return true; }
    @Override public boolean method_25401(double mx, double my, double h, double v) { HudModule t = at(mx, my); if (t != null) t.scale.set(t.scale.get() + v * 0.05); return true; }
    @Override public boolean method_25404(class_11908 in) { if (in.comp_4795() == 256) { Monolith.config().save(Monolith.config().active()); field_22787.method_1507(parent); return true; } return false; }
}
