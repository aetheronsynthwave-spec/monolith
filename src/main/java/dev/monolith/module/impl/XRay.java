package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.class_2246;
import net.minecraft.class_2248;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_332;
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
    private record Hit(class_238 box, Ore ore, double d2, double x, double y, double z) {}

    private final Map<class_2248, Ore> blocks = new HashMap<>();
    private final Map<Ore, BoolSetting> toggles = new EnumMap<>(Ore.class);
    public final NumberSetting range = add(new NumberSetting("Range", 20, 8, 48, 4));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1, 1, 3, 1));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    private final Map<Long, Ore> found = new HashMap<>();
    private final List<Hit> hits = new ArrayList<>();
    private final class_2338.class_2339 cursor = new class_2338.class_2339();
    private int layer, ticks;

    public XRay() {
        super("X-Ray", "Outlines ores through walls.", Category.RENDER);
        for (Ore o : Ore.values()) toggles.put(o, add(new BoolSetting(o.label, true)));
        put(Ore.COAL, class_2246.field_10418, class_2246.field_29219);
        put(Ore.IRON, class_2246.field_10212, class_2246.field_29027);
        put(Ore.COPPER, class_2246.field_27120, class_2246.field_29221);
        put(Ore.GOLD, class_2246.field_10571, class_2246.field_29026, class_2246.field_23077);
        put(Ore.REDSTONE, class_2246.field_10080, class_2246.field_29030);
        put(Ore.LAPIS, class_2246.field_10090, class_2246.field_29028);
        put(Ore.DIAMOND, class_2246.field_10442, class_2246.field_29029);
        put(Ore.EMERALD, class_2246.field_10013, class_2246.field_29220);
        put(Ore.QUARTZ, class_2246.field_10213);
        put(Ore.DEBRIS, class_2246.field_22109);
        range.tooltip = "Scan radius in blocks. Bigger = slower to refresh.";
    }
    private void put(Ore o, class_2248... bs) { for (class_2248 b : bs) blocks.put(b, o); }

    @Override public void onDisable() { found.clear(); hits.clear(); }
    @Override public void onDisconnect() { found.clear(); hits.clear(); }

    /** Scans ONE horizontal layer per tick (about 1,700 blocks) so there is no lag spike. */
    @Override public void onTick() {
        if (mc.field_1687 == null) return;
        int r = range.asInt(), span = 2 * r + 1;
        class_2338 c = mc.field_1724.method_24515();
        int y = c.method_10264() - r + (layer++ % span);
        found.entrySet().removeIf(en -> class_2338.method_10071(en.getKey()) == y);
        for (int x = c.method_10263() - r; x <= c.method_10263() + r; x++)
            for (int z = c.method_10260() - r; z <= c.method_10260() + r; z++) {
                Ore o = blocks.get(mc.field_1687.method_8320(cursor.method_10103(x, y, z)).method_26204());
                if (o != null) found.put(cursor.method_10063(), o);
            }
        if (ticks++ % 10 == 0) rebuild(c, r);
    }

    private void rebuild(class_2338 c, int r) {
        hits.clear();
        double max = (r + 6.0) * (r + 6.0);
        var it = found.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next(); class_2338 p = class_2338.method_10092(en.getKey());
            double dx = p.method_10263() + 0.5 - c.method_10263(), dy = p.method_10264() + 0.5 - c.method_10264(), dz = p.method_10260() + 0.5 - c.method_10260(), d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > max) { it.remove(); continue; }                       // drop stale far-away entries
            hits.add(new Hit(new class_238(p), en.getValue(), d2, p.method_10263() + 0.5, p.method_10264() + 0.5, p.method_10260() + 0.5));
        }
        hits.sort(Comparator.comparingDouble(Hit::d2));
    }

    @Override public void onHud(class_332 c, float delta) {
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
