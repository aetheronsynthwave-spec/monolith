package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.class_1297;
import net.minecraft.class_1304;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_1542;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_1799;
import net.minecraft.class_238;
import net.minecraft.class_243;
import net.minecraft.class_332;
import net.minecraft.entity.*;
import net.minecraft.util.math.*;

/** Entity ESP: 3D outlines, health bars, names, equipment icons and dropped items, through walls, with per-type colours and tracers. */
public class HitboxESP extends Module {
    private static final class_1304[] GEAR = {class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166, class_1304.field_6173};
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Other Mobs", false));
    public final BoolSetting items = add(new BoolSetting("Dropped Items", true));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 8, 128, 1));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1, 1, 3, 1));
    public final BoolSetting names = add(new BoolSetting("Show Names", true));
    public final BoolSetting health = add(new BoolSetting("Show Health", true));
    public final BoolSetting equipment = add(new BoolSetting("Equipment Icons", true));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", true));
    public final EnumSetting<ESPModule.Origin> tracerOrigin = add(new EnumSetting<>("Tracer Origin", ESPModule.Origin.BOTTOM));
    public final ColorSetting cPlayer = add(new ColorSetting("Player Color", 0xFFFFFFFF));
    public final ColorSetting cHostile = add(new ColorSetting("Hostile Color", 0xFFFF4D4D));
    public final ColorSetting cOther = add(new ColorSetting("Other Color", 0xFF5EE08A));
    public final ColorSetting cItem = add(new ColorSetting("Item Color", 0xFFFFD93D));
    private final int[] rect = new int[4];

    public HitboxESP() {
        super("ESP", "Boxes, health, gear and dropped items through walls.", Category.COMBAT);
        equipment.tooltip = "Armor and held item icons above nearby players (closest 10).";
    }

    @Override public void onHud(class_332 c, float delta) {
        if (!Projector.valid) return;
        double r2 = range.get() * range.get();
        int drawn = 0, itemsDrawn = 0, gearDrawn = 0, th = lineWidth.asInt();
        boolean fromCross = tracerOrigin.get() == ESPModule.Origin.CROSSHAIR;
        for (class_1297 e : mc.field_1687.method_18112()) {
            if (e == mc.field_1724) continue;
            double dx = e.method_23317() - Projector.cx, dy = e.method_23318() - Projector.cy, dz = e.method_23321() - Projector.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            float fade = (float) (1.0 - Math.sqrt(d2 / r2) * 0.6);

            if (e instanceof class_1542 ie) {                                // ---- dropped items
                if (!items.get() || itemsDrawn >= 60) continue;
                itemsDrawn++;
                class_243 lp = e.method_30950(delta);
                class_238 b = e.method_18377(e.method_18376()).method_30231(lp.field_1352, lp.field_1351, lp.field_1350);
                int col = cItem.get();
                if (tracers.get()) Wire.tracer(c, lp.field_1352, lp.field_1351 + 0.2, lp.field_1350, Render2D.withAlpha(col, 0.6f * fade), fromCross, 1);
                if (Wire.box(c, b, Render2D.withAlpha(col, fade), th) && names.get() && Projector.box(b, rect)) {
                    class_1799 st = ie.method_6983();
                    Render2D.centered(c, st.method_7964().getString() + (st.method_7947() > 1 ? " x" + st.method_7947() : ""), rect[0] + rect[2] / 2f, rect[1] - 10, Render2D.withAlpha(col, fade));
                }
                continue;
            }

            if (!(e instanceof class_1309 le) || e instanceof class_1531 || !le.method_5805()) continue;
            int col; boolean pl = e instanceof class_1657;
            if (pl) { if (!players.get()) continue; col = cPlayer.get(); }
            else if (e instanceof class_1588) { if (!hostiles.get()) continue; col = cHostile.get(); }
            else { if (!passives.get()) continue; col = cOther.get(); }
            class_243 lp = e.method_30950(delta);                               // interpolated: smooth between ticks
            class_238 b = e.method_18377(e.method_18376()).method_30231(lp.field_1352, lp.field_1351, lp.field_1350);
            if (tracers.get()) Wire.tracer(c, lp.field_1352, lp.field_1351 + e.method_17682() * 0.5, lp.field_1350, Render2D.withAlpha(col, 0.75f * fade), fromCross, 1);
            if (!Wire.box(c, b, Render2D.withAlpha(col, fade), th)) continue;
            if ((names.get() || health.get() || equipment.get()) && Projector.box(b, rect)) {
                int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
                if (names.get()) Render2D.centered(c, e.method_5477().getString(), x + w / 2f, y - 10, Render2D.withAlpha(col, fade));
                if (health.get()) {
                    float f = Math.max(0, Math.min(1, le.method_6032() / le.method_6063()));
                    Render2D.rect(c, x - 4, y, 2, h, 0x80000000);
                    Render2D.rect(c, x - 4, y + h * (1 - f), 2, h * f, Render2D.withAlpha(col, fade));
                }
                if (pl && equipment.get() && d2 < 32 * 32 && gearDrawn < 10) {
                    gearDrawn++;
                    float scale = 0.6f, cell = 16 * scale, gx = x + w / 2f - cell * GEAR.length / 2f, gy = y - 10 - cell - 2;
                    var m = c.method_51448();
                    for (int i = 0; i < GEAR.length; i++) {
                        class_1799 st = le.method_6118(GEAR[i]);
                        if (st.method_7960()) continue;
                        m.pushMatrix(); m.translate(gx + i * cell, gy); m.scale(scale, scale);
                        c.method_51427(st, 0, 0);
                        m.popMatrix();
                    }
                }
            }
            if (++drawn >= 120) break;                                      // cap protects FPS in mob farms
        }
    }
}
