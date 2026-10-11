package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_1542;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_243;

/** Stand-alone tracer lines to entities (players, hostile mobs, other mobs, dropped items). Drawn as real 3D lines. */
public class Tracers extends Module {
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Other Mobs", false));
    public final BoolSetting items = add(new BoolSetting("Dropped Items", false));
    public final NumberSetting range = add(new NumberSetting("Range", 96, 8, 256, 1));
    public final NumberSetting width = add(new NumberSetting("Line Width", 1, 1, 4, 1));
    public final EnumSetting<ESPModule.Origin> origin = add(new EnumSetting<>("Origin", ESPModule.Origin.BOTTOM));
    public final BoolSetting fade = add(new BoolSetting("Distance Fade", true));
    public final BoolSetting walls = add(new BoolSetting("Through Walls", true));
    public final ColorSetting cPlayer = add(new ColorSetting("Player Color", 0xFFFFFFFF));
    public final ColorSetting cHostile = add(new ColorSetting("Hostile Color", 0xFFFF4D4D));
    public final ColorSetting cOther = add(new ColorSetting("Other Color", 0xFF5EE08A));
    public final ColorSetting cItem = add(new ColorSetting("Item Color", 0xFFFFD93D));

    public Tracers() { super("Tracers", "Lines pointing at nearby entities.", Category.RENDER); }

    @Override public void onRender3D(float delta) {
        if (mc.field_1687 == null || mc.field_1724 == null) return;
        double r = range.get(), r2 = r * r;
        float w = ESPStyle.pixels(width.asInt());
        boolean cross = origin.get() == ESPModule.Origin.CROSSHAIR;
        boolean prev = Render3D.xray();
        Render3D.xray(walls.get());
        int drawn = 0;
        for (class_1297 e : mc.field_1687.method_18112()) {
            if (e == mc.field_1724) continue;
            int col;
            if (e instanceof class_1657) { if (!players.get()) continue; col = cPlayer.get(); }
            else if (e instanceof class_1542) { if (!items.get()) continue; col = cItem.get(); }
            else if (e instanceof class_1309 && !(e instanceof class_1531)) {
                if (e instanceof class_1588) { if (!hostiles.get()) continue; col = cHostile.get(); }
                else { if (!passives.get()) continue; col = cOther.get(); }
            } else continue;
            if (e instanceof class_1309 l && !l.method_5805()) continue;
            double d2 = e.method_5649(Render3D.cx, Render3D.cy, Render3D.cz);
            if (d2 > r2) continue;
            float f = fade.get() ? (float) (1.0 - Math.sqrt(d2 / r2) * 0.7) : 1f;
            class_243 p = e.method_30950(delta);
            Render3D.tracer(p.field_1352, p.field_1351 + e.method_17682() * 0.5, p.field_1350, Render2D.withAlpha(col, 0.85f * f), cross, w);
            if (++drawn >= 120) break;                                          // hard cap for mob farms
        }
        Render3D.xray(prev);
    }
}
