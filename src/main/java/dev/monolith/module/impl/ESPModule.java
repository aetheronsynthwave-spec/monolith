package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.Render2D;
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

    @Override public void onHud(DrawContext c, float delta) {
        if (mc.player == null || mc.world == null) return;
        Vec3d cam = mc.player.getCameraPosVec(delta);
        double yaw = Math.toRadians(mc.player.getYaw(delta)), pitch = Math.toRadians(mc.player.getPitch(delta));
        // Camera basis vectors (forward / right / up).
        double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
        double rx = -Math.cos(yaw), ry = 0, rz = -Math.sin(yaw);
        double ux = ry * fz - rz * fy, uy = rz * fx - rx * fz, uz = rx * fy - ry * fx;
        int W = c.getScaledWindowWidth(), H = c.getScaledWindowHeight();
        double fov = Math.toRadians(mc.options.getFov().getValue());
        double focal = (H / 2.0) / Math.tan(fov / 2.0);
        double r2 = range.get() * range.get();

        for (BlockEntity be : cache) {
            if (be.isRemoved()) continue;
            BlockPos p = be.getPos();
            double d2 = p.getSquaredDistance(cam);
            if (d2 > r2) continue;
            int col = colorFor(be);
            if (col == 0) continue;

            // Centre must be in front of the camera.
            double cdx = p.getX() + 0.5 - cam.x, cdy = p.getY() + 0.5 - cam.y, cdz = p.getZ() + 0.5 - cam.z;
            if (cdx * fx + cdy * fy + cdz * fz < 0.1) continue;

            double minX = 1e9, minY = 1e9, maxX = -1e9, maxY = -1e9;
            for (int i = 0; i < 8; i++) {
                double dx = p.getX() + (i & 1) - cam.x, dy = p.getY() + ((i >> 1) & 1) - cam.y, dz = p.getZ() + ((i >> 2) & 1) - cam.z;
                double z = Math.max(0.1, dx * fx + dy * fy + dz * fz);
                double sx = W / 2.0 + (dx * rx + dy * ry + dz * rz) / z * focal;
                double sy = H / 2.0 - (dx * ux + dy * uy + dz * uz) / z * focal;
                minX = Math.min(minX, sx); maxX = Math.max(maxX, sx); minY = Math.min(minY, sy); maxY = Math.max(maxY, sy);
            }
            if (maxX < 0 || minX > W || maxY < 0 || minY > H) continue;

            int x = (int) minX, y = (int) minY, w = Math.max(3, (int) (maxX - minX)), h = Math.max(3, (int) (maxY - minY));
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
