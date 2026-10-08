package dev.monolith.module.impl;

import dev.monolith.module.*;
import dev.monolith.render.Render3D;
import dev.monolith.setting.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/** Shared logic for block-entity ESP modules. Block entities are cached via Fabric client events (see Monolith.java), so no chunk scanning per frame. */
public abstract class ESPModule extends Module {
    public enum Style { OUTLINE, FILLED, BOTH }
    protected final Set<BlockEntity> cache = ConcurrentHashMap.newKeySet();
    public final EnumSetting<Style> style = add(new EnumSetting<>("Style", Style.BOTH));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));
    public final NumberSetting range = add(new NumberSetting("Range", 64, 8, 256, 1).tip("Max distance in blocks."));
    public final BoolSetting fadeWithDistance = add(new BoolSetting("Distance Fade", true));
    public final NumberSetting fillAlpha = add(new NumberSetting("Fill Opacity", 0.18, 0.02, 1, 0.01));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1.5, 0.5, 5, 0.1));
    public final BoolSetting glow = add(new BoolSetting("Glow", true).tip("Draws a soft expanded shell around the box."));
    public final BoolSetting showDistance = add(new BoolSetting("Show Distance", false));

    protected ESPModule(String n, String d) { super(n, d, Category.RENDER); }

    public void track(BlockEntity be) { if (accepts(be)) cache.add(be); }
    public void untrack(BlockEntity be) { cache.remove(be); }
    public void clear() { cache.clear(); }
    protected abstract boolean accepts(BlockEntity be);
    /** ARGB colour for this block entity. */
    protected abstract int colorFor(BlockEntity be);

    @Override public void onWorld(MatrixStack m, float delta) {
        Vec3d cam = mc.gameRenderer.getCamera().getPos();
        double r2 = range.get() * range.get();
        for (BlockEntity be : cache) {
            if (be.isRemoved()) continue;
            BlockPos p = be.getPos();
            double d2 = p.getSquaredDistance(cam);
            if (d2 > r2) continue;
            int col = colorFor(be); if (col == 0) continue;
            float fade = fadeWithDistance.get() ? (float) (1.0 - Math.sqrt(d2 / r2) * 0.7) : 1f;
            Box box = Box.enclosing(p, p).expand(0.002);
            // Use the real collision shape so double chests/hoppers look right.
            var shape = be.getCachedState().getOutlineShape(mc.world, p);
            if (!shape.isEmpty()) box = shape.getBoundingBox().offset(p).expand(0.002);

            if (style.get() != Style.OUTLINE) Render3D.box(m, box, Render3D.alpha(col, fillAlpha.asFloat() * fade), true, throughWalls.get());
            if (style.get() != Style.FILLED) Render3D.box(m, box, Render3D.alpha(col, fade), false, throughWalls.get(), lineWidth.asFloat());
            if (glow.get()) Render3D.box(m, box.expand(0.06), Render3D.alpha(col, 0.07f * fade), true, throughWalls.get());
            if (showDistance.get()) Render3D.label(m, Vec3d.ofCenter(p).add(0, 0.9, 0), String.format("%.0fm", Math.sqrt(d2)), 0xFFFFFFFF);
        }
    }
}
