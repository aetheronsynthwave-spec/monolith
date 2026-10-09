package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared logic for block-entity ESP modules. Block entities are cached via Fabric client events (see Monolith.java),
 * so there is no chunk scanning. Boxes are true 3D wireframes (clipped lines), visible through walls.
 */
public abstract class ESPModule extends Module {
    public enum Origin { BOTTOM, CROSSHAIR }
    protected final Set<BlockEntity> cache = ConcurrentHashMap.newKeySet();
    public final NumberSetting range = add(new NumberSetting("Range", 64, 8, 256, 1));
    public final BoolSetting fadeWithDistance = add(new BoolSetting("Distance Fade", true));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1, 1, 4, 1));
    public final BoolSetting glow = add(new BoolSetting("Glow", true));
    public final BoolSetting showDistance = add(new BoolSetting("Show Distance", false));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    public final EnumSetting<Origin> tracerOrigin = add(new EnumSetting<>("Tracer Origin", Origin.BOTTOM));
    public final NumberSetting tracerWidth = add(new NumberSetting("Tracer Width", 1, 1, 3, 1));
    private final double[] pt = new double[3];

    protected ESPModule(String n, String d) {
        super(n, d, Category.RENDER);
        range.tooltip = "Maximum distance in blocks.";
        glow.tooltip = "Soft halo behind the outline (first 40 targets only).";
    }

    public void track(BlockEntity be) { if (accepts(be)) cache.add(be); }
    public void untrack(BlockEntity be) { cache.remove(be); }
    public void clear() { cache.clear(); }
    protected abstract boolean accepts(BlockEntity be);
    /** ARGB colour for this block entity; 0 means "do not draw". */
    protected abstract int colorFor(BlockEntity be);

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid) return;
        double r2 = range.get() * range.get();
        int drawn = 0, th = lineWidth.asInt();
        for (BlockEntity be : cache) {
            if (be.isRemoved()) continue;
            BlockPos p = be.getPos();
            double dx = p.getX() + 0.5 - Projector.cx, dy = p.getY() + 0.5 - Projector.cy, dz = p.getZ() + 0.5 - Projector.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            int col = colorFor(be);
            if (col == 0) continue;
            float fade = fadeWithDistance.get() ? (float) (1.0 - Math.sqrt(d2 / r2) * 0.65) : 1f;
            if (tracers.get()) Wire.tracer(c, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, Render2D.withAlpha(col, 0.75f * fade), tracerOrigin.get() == Origin.CROSSHAIR, tracerWidth.asInt());

            Box box = new Box(p);
            var shape = be.getCachedState().getOutlineShape(mc.world, p);
            if (!shape.isEmpty()) box = shape.getBoundingBox().offset(p);
            if (glow.get() && drawn < 40) Wire.box(c, box, Render2D.withAlpha(col, 0.2f * fade), th + 2);
            if (Wire.box(c, box, Render2D.withAlpha(col, fade), th)) {
                if (showDistance.get() && Projector.point(p.getX() + 0.5, box.maxY + 0.35, p.getZ() + 0.5, pt))
                    Render2D.centered(c, (int) Math.sqrt(d2) + "m", (float) pt[0], (float) pt[1] - 4, Render2D.withAlpha(col, fade));
            }
            if (++drawn >= 150) break;                    // hard cap keeps FPS stable in huge storage rooms
        }
    }
}
