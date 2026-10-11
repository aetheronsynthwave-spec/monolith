package dev.monolith.hud;

import dev.monolith.module.impl.ESPModule;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_1531;
import net.minecraft.class_1588;
import net.minecraft.class_1657;
import net.minecraft.class_243;
import net.minecraft.class_332;

/** Top-down radar: you in the centre, nearby players/mobs as dots. Up = where you look (or north, if North Lock is on). */
public class Radar extends HudModule {
    public final NumberSetting size = add(new NumberSetting("Size", 96, 64, 160, 8));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 16, 128, 4));
    public final BoolSetting northLock = add(new BoolSetting("North Lock", false));
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Other Mobs", false));
    public final ColorSetting accent = add(new ColorSetting("Accent", 0xFF38D9E8));
    public final ColorSetting cPlayer = add(new ColorSetting("Player Color", 0xFFFFFFFF));
    public final ColorSetting cHostile = add(new ColorSetting("Hostile Color", 0xFFFF4D4D));
    public final ColorSetting cOther = add(new ColorSetting("Other Color", 0xFF5EE08A));

    public Radar() { super("Radar", "Top-down map of nearby entities.", 0.01, 0.58); }

    @Override protected void draw(class_332 c) {
        if (mc.field_1687 == null || mc.field_1724 == null) return;
        int S = size.asInt(); float half = S / 2f, cx = half, cy = half;
        contentW = S; contentH = S;
        if (background.get()) {
            Render2D.shadow(c, 0, 0, S, S, 8, 6, 0.5f);
            Render2D.roundedBordered(c, 0, 0, S, S, 8, 0xD00E1014, 0xFF262B33);
        }
        int ac = accent.get();
        Render2D.rect(c, cx, 4, 1, S - 8, Render2D.withAlpha(0xFFFFFFFF, 0.06f));   // crosshair guides
        Render2D.rect(c, 4, cy, S - 8, 1, Render2D.withAlpha(0xFFFFFFFF, 0.06f));

        float partial = 1f;
        double yaw = Math.toRadians(mc.field_1724.method_5705(partial));
        double fxw = -Math.sin(yaw), fzw = Math.cos(yaw), rxw = -Math.cos(yaw), rzw = -Math.sin(yaw);
        class_243 me = mc.field_1724.method_30950(partial);
        double r = range.get(), r2 = r * r;
        boolean lock = northLock.get();
        int drawn = 0;
        for (class_1297 e : mc.field_1687.method_18112()) {
            if (e == mc.field_1724 || !(e instanceof class_1309 l) || e instanceof class_1531 || !l.method_5805()) continue;
            int col;
            if (e instanceof class_1657) { if (!players.get()) continue; col = cPlayer.get(); }
            else if (e instanceof class_1588) { if (!hostiles.get()) continue; col = cHostile.get(); }
            else { if (!passives.get()) continue; col = cOther.get(); }
            double dx = e.method_23317() - me.field_1352, dz = e.method_23321() - me.field_1350;
            double d2 = dx * dx + dz * dz;
            double sx, sy;
            if (lock) { sx = dx; sy = dz; }                                           // east = right, south = down
            else { sx = dx * rxw + dz * rzw; sy = -(dx * fxw + dz * fzw); }          // facing direction = up
            double len = Math.sqrt(sx * sx + sy * sy);
            boolean edge = d2 > r2;
            double k = edge ? (half - 5) / len : (half - 5) / r;                       // far targets stick to the rim, dimmed
            float px = cx + (float) (sx * k), py = cy + (float) (sy * k);
            Render2D.rounded(c, px - 2, py - 2, 4, 4, 2, edge ? Render2D.withAlpha(col, 0.45f) : col);
            if (++drawn >= 60) break;
        }
        Render2D.rounded(c, cx - 2, cy - 2, 4, 4, 2, ac);                              // you
        if (!lock) {                                                                   // view cone
            Render2D.line(c, cx, cy, cx - 14, cy - 18, Render2D.withAlpha(ac, 0.5f), 1);
            Render2D.line(c, cx, cy, cx + 14, cy - 18, Render2D.withAlpha(ac, 0.5f), 1);
        }
    }
}
