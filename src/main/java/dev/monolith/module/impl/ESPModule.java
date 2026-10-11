package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.class_2338;
import net.minecraft.class_238;
import net.minecraft.class_2586;
import net.minecraft.class_332;

/**
 * Shared logic for block-entity ESP modules (Storage Finder, Spawner Finder...). Public API is unchanged, so subclasses need no edits.
 *
 * Optimisations over the old version:
 *  - the expensive part (distance test, collision-shape lookup, colour, sorting) runs once every 2 ticks, not every frame;
 *  - every frame only walks a small pre-built array and appends floats to Render3D's batch (no allocation, no shape lookups);
 *  - nearest targets are drawn first and the total is capped, so a huge storage hall cannot tank the frame rate.
 */
public abstract class ESPModule extends Module {
    public enum Origin { BOTTOM, CROSSHAIR }
    protected final Set<class_2586> cache = ConcurrentHashMap.newKeySet();
    public final NumberSetting range = add(new NumberSetting("Range", 64, 8, 256, 1));
    public final BoolSetting fadeWithDistance = add(new BoolSetting("Distance Fade", true));
    public final NumberSetting lineWidth = add(new NumberSetting("Line Width", 1, 1, 4, 1));
    public final BoolSetting glow = add(new BoolSetting("Glow", true));
    public final BoolSetting showDistance = add(new BoolSetting("Show Distance", false));
    public final BoolSetting tracers = add(new BoolSetting("Tracers", false));
    public final EnumSetting<Origin> tracerOrigin = add(new EnumSetting<>("Tracer Origin", Origin.BOTTOM));
    public final NumberSetting tracerWidth = add(new NumberSetting("Tracer Width", 1, 1, 3, 1));

    private static final int MAX_DRAWN = 150, MAX_GLOW = 40;
    private static final class Hit { class_238 box; double x, y, z; int color; double d2; }
    private final ArrayList<Hit> hits = new ArrayList<>();
    private final double[] pt = new double[3];
    private int tick;

    protected ESPModule(String n, String d) {
        super(n, d, Category.RENDER);
        range.tooltip = "Maximum distance in blocks.";
        glow.tooltip = "Soft halo behind the outline (first 40 targets only).";
    }

    public void track(class_2586 be) { if (accepts(be)) cache.add(be); }
    public void untrack(class_2586 be) { cache.remove(be); }
    public void clear() { cache.clear(); hits.clear(); }
    protected abstract boolean accepts(class_2586 be);
    /** ARGB colour for this block entity; 0 means "do not draw". */
    protected abstract int colorFor(class_2586 be);

    @Override public void onDisable() { hits.clear(); }
    @Override public void onDisconnect() { hits.clear(); }

    @Override public void onTick() { if ((tick++ & 1) == 0) rebuild(); }

    private void rebuild() {
        hits.clear();
        if (mc.field_1687 == null || mc.field_1724 == null) return;
        double px = mc.field_1724.method_23317(), py = mc.field_1724.method_23318(), pz = mc.field_1724.method_23321();
        double r = range.get(), r2 = r * r;
        for (class_2586 be : cache) {
            if (be.method_11015()) continue;                                   // block entity was removed
            class_2338 p = be.method_11016();
            double dx = p.method_10263() + 0.5 - px, dy = p.method_10264() + 0.5 - py, dz = p.method_10260() + 0.5 - pz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            int col = colorFor(be);
            if (col == 0) continue;
            class_238 box = new class_238(p);
            var shape = be.method_11010().method_26218(mc.field_1687, p);
            if (!shape.method_1110()) box = shape.method_1107().method_996(p);
            Hit h = new Hit();
            h.box = box; h.color = col; h.d2 = d2;
            h.x = p.method_10263() + 0.5; h.y = p.method_10264() + 0.5; h.z = p.method_10260() + 0.5;
            hits.add(h);
        }
        hits.sort(Comparator.comparingDouble(h -> h.d2));
        while (hits.size() > MAX_DRAWN) hits.remove(hits.size() - 1);
    }

    @Override public void onRender3D(float delta) {
        if (hits.isEmpty()) return;
        double r = range.get(), r2 = r * r;
        boolean fade = fadeWithDistance.get(), gl = glow.get(), tr = tracers.get();
        float w = ESPStyle.pixels(lineWidth.asInt()), tw = ESPStyle.pixels(tracerWidth.asInt());
        boolean cross = tracerOrigin.get() == Origin.CROSSHAIR;
        int drawn = 0;
        for (Hit h : hits) {
            double dx = h.x - Render3D.cx, dy = h.y - Render3D.cy, dz = h.z - Render3D.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            float f = fade ? (float) (1.0 - Math.sqrt(d2 / r2) * 0.65) : 1f;
            if (tr) Render3D.tracer(h.x, h.y, h.z, Render2D.withAlpha(h.color, 0.75f * f), cross, tw);
            if (gl && drawn < MAX_GLOW) Render3D.box(h.box, Render2D.withAlpha(h.color, 0.2f * f), w + 2.2f);
            Render3D.espBox(h.box, Render2D.withAlpha(h.color, f), w);
            drawn++;
        }
    }

    /** Distance labels stay 2D text (crisp at any range); everything else is drawn in 3D above. */
    @Override public void onHud(class_332 c, float delta) {
        if (!showDistance.get() || !Projector.valid || hits.isEmpty()) return;
        double r = range.get(), r2 = r * r;
        for (Hit h : hits) {
            double dx = h.x - Projector.cx, dy = h.y - Projector.cy, dz = h.z - Projector.cz;
            double d2 = dx * dx + dy * dy + dz * dz;
            if (d2 > r2) continue;
            float f = fadeWithDistance.get() ? (float) (1.0 - Math.sqrt(d2 / r2) * 0.65) : 1f;
            if (Projector.point(h.x, h.box.field_1325 + 0.35, h.z, pt))
                Render2D.centered(c, (int) Math.sqrt(d2) + "m", (float) pt[0], (float) pt[1] - 4, Render2D.withAlpha(h.color, f));
        }
    }
}
