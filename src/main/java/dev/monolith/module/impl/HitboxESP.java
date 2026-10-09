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

/** 3D hitbox outlines for living entities, visible through walls, with per-type colours and optional tracers. */
public class HitboxESP extends Module {
    public final BoolSetting players = add(new BoolSetting("Players", true));
    public final BoolSetting hostiles = add(new BoolSetting("Hostile Mobs", true));
    public final BoolSetting passives = add(new BoolSetting("Other Mobs", false));
    public final NumberSetting range = add(new NumberSetting("Range", 48, 8, 128, 1));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1, 1, 3, 1));
    public final BoolSetting names = add(new BoolSetting("Show Names", true));
    public final BoolSetting health = add(new BoolSetting("Show Health", true));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", true));
    public final EnumSetting<ESPModule.Origin> tracerOrigin = add(new EnumSetting<>("Tracer Origin", ESPModule.Origin.BOTTOM));
    public final ColorSetting cPlayer = add(new ColorSetting("Player Color", 0xFFFFFFFF));
    public final ColorSetting cHostile = add(new ColorSetting("Hostile Color", 0xFFFF4D4D));
    public final ColorSetting cOther = add(new ColorSetting("Other Color", 0xFF5EE08A));
    private final int[] rect = new int[4];

    public HitboxESP() { super("Hitboxes", "Shows entity hitboxes through walls.", Category.COMBAT); }

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid) return;
        double r2 = range.get() * range.get();
        int drawn = 0;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player || !(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            int col;
            if (e instanceof PlayerEntity) { if (!players.get()) continue; col = cPlayer.get(); }
            else if (e instanceof HostileEntity) { if (!hostiles.get()) continue; col = cHostile.get(); }
            else { if (!passives.get()) continue; col = cOther.get(); }
            double dx = e.getX() - Projector.cx, dy = e.getY() - Projector.cy, dz = e.getZ() - Projector.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            Vec3d lp = e.getLerpedPos(delta);                       // interpolated: smooth between ticks
            Box b = e.getDimensions(e.getPose()).getBoxAt(lp.x, lp.y, lp.z);
            float fade = (float) (1.0 - Math.sqrt(d2 / r2) * 0.6);
            if (tracers.get()) Wire.tracer(c, lp.x, lp.y + e.getHeight() * 0.5, lp.z, Render2D.withAlpha(col, 0.75f * fade), tracerOrigin.get() == ESPModule.Origin.CROSSHAIR, 1);
            if (!Wire.box(c, b, Render2D.withAlpha(col, fade), lineWidth.asInt())) continue;
            if ((names.get() || health.get()) && Projector.box(b, rect)) {
                int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
                if (names.get()) Render2D.centered(c, e.getName().getString(), x + w / 2f, y - 10, Render2D.withAlpha(col, fade));
                if (health.get()) {
                    float f = Math.max(0, Math.min(1, le.getHealth() / le.getMaxHealth()));
                    Render2D.rect(c, x - 4, y, 2, h, 0x80000000);
                    Render2D.rect(c, x - 4, y + h * (1 - f), 2, h * f, Render2D.withAlpha(col, fade));
                }
            }
            if (++drawn >= 120) break;                              // cap protects FPS in mob farms
        }
    }
}
