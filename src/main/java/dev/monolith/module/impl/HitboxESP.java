package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.*;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.*;

/** Hitbox overlay: projects each living entity's box onto the screen (visible through walls). */
public class HitboxESP extends Module {
    public enum Style { OUTLINE, FILLED, BOTH }
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Other Mobs", false));
    public final EnumSetting<Style> style = add(new EnumSetting<>("Style", Style.OUTLINE));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 8, 128, 1));
    public final BoolSetting names = add(new BoolSetting("Show Names", true));
    public final BoolSetting health = add(new BoolSetting("Show Health", true));
    public final ColorSetting cPlayer = add(new ColorSetting("Player Color", 0xFFFFFFFF));
    public final ColorSetting cHostile = add(new ColorSetting("Hostile Color", 0xFFBDBDBD));
    public final ColorSetting cOther = add(new ColorSetting("Other Color", 0xFF757575));
    private final int[] rect = new int[4];

    public HitboxESP() { super("Hitboxes", "Shows entity hitboxes through walls.", Category.COMBAT); }

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid) return;
        double r2 = range.get() * range.get();
        int drawn = 0;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            int col; boolean pl = e instanceof PlayerEntity;
            if (pl) { if (!players.get()) continue; col = cPlayer.get(); }
            else if (e instanceof HostileEntity) { if (!hostiles.get()) continue; col = cHostile.get(); }
            else { if (!passives.get()) continue; col = cOther.get(); }
            double dx = e.getX() - Projector.cx, dy = e.getY() - Projector.cy, dz = e.getZ() - Projector.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            Vec3d lp = e.getLerpedPos(delta);                      // interpolated, so boxes move smoothly between ticks
            Box b = e.getDimensions(e.getPose()).getBoxAt(lp.x, lp.y, lp.z);
            if (!Projector.box(b, rect)) continue;
            int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
            float fade = (float) (1.0 - Math.sqrt(d2 / r2) * 0.6);
            if (style.get() != Style.OUTLINE) Render2D.rect(c, x, y, w, h, Render2D.withAlpha(col, 0.15f * fade));
            if (style.get() != Style.FILLED) Render2D.outline(c, x, y, w, h, Render2D.withAlpha(col, fade));
            if (names.get()) Render2D.centered(c, e.getName().getString(), x + w / 2f, y - 10, Render2D.withAlpha(0xFFFFFFFF, fade));
            if (health.get()) {
                float f = Math.max(0, Math.min(1, le.getHealth() / le.getMaxHealth()));
                Render2D.rect(c, x - 3, y, 2, h, 0x80000000);
                Render2D.rect(c, x - 3, y + h * (1 - f), 2, h * f, Render2D.withAlpha(0xFFFFFFFF, fade));
            }
            if (++drawn >= 150) break;                              // hard cap protects FPS in mob farms
        }
    }
}
