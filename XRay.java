package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.*;
import java.util.*;

/**
 * X-Ray (ore highlight). Scans the blocks around you one horizontal layer per tick and outlines valuable blocks through walls.
 * It does NOT hide the stone/dirt itself: that needs changes deep inside Minecraft's chunk renderer.
 */
public class XRay extends Module {
    private enum Ore {
        COAL("Coal", 0xFF8C8C8C), IRON("Iron", 0xFFE2B48C), COPPER("Copper", 0xFFFF9F43), GOLD("Gold", 0xFFFFD93D), REDSTONE("Redstone", 0xFFFF4D4D),
        LAPIS("Lapis", 0xFF5C7CFF), DIAMOND("Diamond", 0xFF4FD8FF), EMERALD("Emerald", 0xFF5EE08A), QUARTZ("Quartz", 0xFFF2F2F2), DEBRIS("Ancient Debris", 0xFFB06CFF);
        final String label; final int color;
        Ore(String l, int c) { label = l; color = c; }
    }
    private record Hit(Box box, Ore ore, double d2, double x, double y, double z) {}

    private final Map<Block, Ore> blocks = new HashMap<>();
    private final Map<Ore, BoolSetting> toggles = new EnumMap<>(Ore.class);
    public final NumberSetting range = add(new NumberSetting("Range", 20, 8, 48, 4));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1, 1, 3, 1));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    private final Map<Long, Ore> found = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final BlockPos.Mutable cursor = new BlockPos.Mutable();
    private int layer, ticks;

    public XRay() {
        super("X-Ray", "Outlines ores through walls.", Category.RENDER);
        for (Ore o : Ore.values()) toggles.put(o, add(new BoolSetting(o.label, true)));
        put(Ore.COAL, Blocks.COAL_ORE, Blocks.DEEPSLATE_COAL_ORE);
        put(Ore.IRON, Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE);
        put(Ore.COPPER, Blocks.COPPER_ORE, Blocks.DEEPSLATE_COPPER_ORE);
        put(Ore.GOLD, Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE);
        put(Ore.REDSTONE, Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);
        put(Ore.LAPIS, Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE);
        put(Ore.DIAMOND, Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE);
        put(Ore.EMERALD, Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);
        put(Ore.QUARTZ, Blocks.NETHER_QUARTZ_ORE);
        put(Ore.DEBRIS, Blocks.ANCIENT_DEBRIS);
        range.tooltip = "Scan radius in blocks. Bigger = slower to refresh.";
    }
    private void put(Ore o, Block... bs) { for (Block b : bs) blocks.put(b, o); }

    @Override public void onDisable() { found.clear(); hits.clear(); }
    @Override public void onDisconnect() { found.clear(); hits.clear(); }

    /** Scans ONE horizontal layer per tick (about 1,700 blocks) so there is no lag spike. */
    @Override public void onTick() {
        if (mc.world == null) return;
        int r = range.asInt(), span = 2 * r + 1;
        BlockPos c = mc.player.getBlockPos();
        int y = c.getY() - r + (layer++ % span);
        found.entrySet().removeIf(en -> BlockPos.unpackLongY(en.getKey()) == y);
        for (int x = c.getX() - r; x <= c.getX() + r; x++)
            for (int z = c.getZ() - r; z <= c.getZ() + r; z++) {
                Ore o = blocks.get(mc.world.getBlockState(cursor.set(x, y, z)).getBlock());
                if (o != null) found.put(cursor.asLong(), o);
            }
        if (ticks++ % 10 == 0) rebuild(c, r);
    }

    private void rebuild(BlockPos c, int r) {
        hits.clear();
        double max = (r + 6.0) * (r + 6.0);
        var it = found.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next(); BlockPos p = BlockPos.fromLong(en.getKey());
            double dx = p.getX() + 0.5 - c.getX(), dy = p.getY() + 0.5 - c.getY(), dz = p.getZ() + 0.5 - c.getZ(), d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > max) { it.remove(); continue; }                       // drop stale far-away entries
            hits.add(new Hit(new Box(p), en.getValue(), d2, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5));
        }
        hits.sort(Comparator.comparingDouble(Hit::d2));
    }

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid) return;
        double r2 = range.get() * range.get(); int drawn = 0, th = lineWidth.asInt();
        for (Hit h : hits) {
            if (h.d2 > r2) break;                                           // list is sorted by distance
            if (!toggles.get(h.ore).get()) continue;
            float fade = (float) (1.0 - Math.sqrt(h.d2 / r2) * 0.6);
            int col = Render2D.withAlpha(h.ore.color, fade);
            if (tracers.get()) Wire.tracer(c, h.x, h.y, h.z, Render2D.withAlpha(h.ore.color, 0.6f * fade), false, 1);
            Wire.box(c, h.box, col, th);
            if (++drawn >= 200) break;
        }
    }
}
