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
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.*;

/** Entity ESP: 3D outlines, health bars, names, equipment icons and dropped items, through walls, with per-type colours and tracers. */
public class HitboxESP extends Module {
    private static final EquipmentSlot[] GEAR = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND};
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

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid) return;
        double r2 = range.get() * range.get();
        int drawn = 0, itemsDrawn = 0, gearDrawn = 0, th = lineWidth.asInt();
        boolean fromCross = tracerOrigin.get() == ESPModule.Origin.CROSSHAIR;
        for (Entity e : mc.world.getEntities()) {
            if (e == mc.player) continue;
            double dx = e.getX() - Projector.cx, dy = e.getY() - Projector.cy, dz = e.getZ() - Projector.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            float fade = (float) (1.0 - Math.sqrt(d2 / r2) * 0.6);

            if (e instanceof ItemEntity ie) {                                // ---- dropped items
                if (!items.get() || itemsDrawn >= 60) continue;
                itemsDrawn++;
                Vec3d lp = e.getLerpedPos(delta);
                Box b = e.getDimensions(e.getPose()).getBoxAt(lp.x, lp.y, lp.z);
                int col = cItem.get();
                if (tracers.get()) Wire.tracer(c, lp.x, lp.y + 0.2, lp.z, Render2D.withAlpha(col, 0.6f * fade), fromCross, 1);
                if (Wire.box(c, b, Render2D.withAlpha(col, fade), th) && names.get() && Projector.box(b, rect)) {
                    ItemStack st = ie.getStack();
                    Render2D.centered(c, st.getName().getString() + (st.getCount() > 1 ? " x" + st.getCount() : ""), rect[0] + rect[2] / 2f, rect[1] - 10, Render2D.withAlpha(col, fade));
                }
                continue;
            }

            if (!(e instanceof LivingEntity le) || e instanceof ArmorStandEntity || !le.isAlive()) continue;
            int col; boolean pl = e instanceof PlayerEntity;
            if (pl) { if (!players.get()) continue; col = cPlayer.get(); }
            else if (e instanceof HostileEntity) { if (!hostiles.get()) continue; col = cHostile.get(); }
            else { if (!passives.get()) continue; col = cOther.get(); }
            Vec3d lp = e.getLerpedPos(delta);                               // interpolated: smooth between ticks
            Box b = e.getDimensions(e.getPose()).getBoxAt(lp.x, lp.y, lp.z);
            if (tracers.get()) Wire.tracer(c, lp.x, lp.y + e.getHeight() * 0.5, lp.z, Render2D.withAlpha(col, 0.75f * fade), fromCross, 1);
            if (!Wire.box(c, b, Render2D.withAlpha(col, fade), th)) continue;
            if ((names.get() || health.get() || equipment.get()) && Projector.box(b, rect)) {
                int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
                if (names.get()) Render2D.centered(c, e.getName().getString(), x + w / 2f, y - 10, Render2D.withAlpha(col, fade));
                if (health.get()) {
                    float f = Math.max(0, Math.min(1, le.getHealth() / le.getMaxHealth()));
                    Render2D.rect(c, x - 4, y, 2, h, 0x80000000);
                    Render2D.rect(c, x - 4, y + h * (1 - f), 2, h * f, Render2D.withAlpha(col, fade));
                }
                if (pl && equipment.get() && d2 < 32 * 32 && gearDrawn < 10) {
                    gearDrawn++;
                    float scale = 0.6f, cell = 16 * scale, gx = x + w / 2f - cell * GEAR.length / 2f, gy = y - 10 - cell - 2;
                    var m = c.getMatrices();
                    for (int i = 0; i < GEAR.length; i++) {
                        ItemStack st = le.getEquippedStack(GEAR[i]);
                        if (st.isEmpty()) continue;
                        m.pushMatrix(); m.translate(gx + i * cell, gy); m.scale(scale, scale);
                        c.drawItem(st, 0, 0);
                        m.popMatrix();
                    }
                }
            }
            if (++drawn >= 120) break;                                      // cap protects FPS in mob farms
        }
    }
}
