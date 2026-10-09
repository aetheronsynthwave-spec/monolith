package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.Render2D;
import dev.monolith.render.Projector;
import dev.monolith.setting.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.*;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared logic for block-entity ESP modules.
 * Block entities are cached through Fabric client events (see Monolith.java), so no chunk scanning per frame.
 * Drawing is screen-space: each block's 8 corners are projected onto the screen and a box is drawn around them.
 * Because it is just a 2D overlay, it is visible through walls and needs no fragile 3D render-pipeline code.
 */
public abstract class ESPModule extends Module {
    public enum Style { OUTLINE, FILLED, BOTH }
    protected final Set<BlockEntity> cache = ConcurrentHashMap.newKeySet();
    public final EnumSetting<Style> style = add(new EnumSetting<>("Style", Style.BOTH));
    public final NumberSetting range = add(new NumberSetting("Range", 64, 8, 256, 1));
    public final BoolSetting fadeWithDistance = add(new BoolSetting("Distance Fade", true));
    public final NumberSetting fillAlpha = add(new NumberSetting("Fill Opacity", 0.18, 0.02, 1, 0.01));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1.0, 1, 4, 1));
    public final BoolSetting glow = add(new BoolSetting("Glow", true));
    public final BoolSetting showDistance = add(new BoolSetting("Show Distance", false));

    protected ESPModule(String n, String d) {
        super(n, d, Category.RENDER);
        range.tooltip = "Maximum distance in blocks.";
        glow.tooltip = "Soft light halo around each box.";
    }

    public void track(BlockEntity be) { if (accepts(be)) cache.add(be); }
    public void untrack(BlockEntity be) { cache.remove(be); }
    public void clear() { cache.clear(); }
    protected abstract boolean accepts(BlockEntity be);
    /** ARGB colour for this block entity; 0 means "do not draw". */
    protected abstract int colorFor(BlockEntity be);

    private final int[] rect = new int[4];

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid) return;
        double r2 = range.get() * range.get();
        for (BlockEntity be : cache) {
            if (be.isRemoved()) continue;
            BlockPos p = be.getPos();
            double dx = p.getX() + 0.5 - Projector.cx, dy = p.getY() + 0.5 - Projector.cy, dz = p.getZ() + 0.5 - Projector.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;                       // cheapest test first
            int col = colorFor(be);
            if (col == 0) continue;
            Box box = new Box(p);
            var shape = be.getCachedState().getOutlineShape(mc.world, p);
            if (!shape.isEmpty()) box = shape.getBoundingBox().offset(p);
            if (!Projector.box(box, rect)) continue;
            int x = rect[0], y = rect[1], w = rect[2], h = rect[3];
            float fade = fadeWithDistance.get() ? (float) (1.0 - Math.sqrt(d2 / r2) * 0.7) : 1f;

            if (glow.get()) Render2D.glow(c, x, y, w, h, 2, 6, Render2D.withAlpha(col, fade));
            if (style.get() != Style.OUTLINE) Render2D.rect(c, x, y, w, h, Render2D.withAlpha(col, fillAlpha.asFloat() * fade));
            if (style.get() != Style.FILLED) {
                int t = lineWidth.asInt();
                for (int i = 0; i < t; i++) Render2D.outline(c, x + i, y + i, w - i * 2, h - i * 2, Render2D.withAlpha(col, fade));
            }
            if (showDistance.get()) Render2D.centered(c, (int) Math.sqrt(d2) + "m", x + w / 2f, y - 10, Render2D.withAlpha(0xFFFFFFFF, fade));
        }
    }
}
