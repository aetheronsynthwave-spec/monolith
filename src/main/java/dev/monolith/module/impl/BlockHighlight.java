package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.class_239;
import net.minecraft.class_243;
import net.minecraft.class_238;
import net.minecraft.class_2338;
import net.minecraft.class_2680;

/** Coloured filled highlight on the block you are looking at (replaces squinting at the thin vanilla outline). */
public class BlockHighlight extends Module {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFF38D9E8));
    public final NumberSetting fillOpacity = add(new NumberSetting("Fill Opacity", 0.25, 0.02, 0.8, 0.02));
    public final NumberSetting width = add(new NumberSetting("Line Width", 2, 1, 4, 1));
    public final BoolSetting pulse = add(new BoolSetting("Pulse", true));
    public final NumberSetting reach = add(new NumberSetting("Max Distance", 6, 2, 12, 1));

    public BlockHighlight() { super("Block Highlight", "Filled highlight on the targeted block.", Category.RENDER); }

    @Override public void onRender3D(float delta) {
        class_239 h = mc.field_1765;
        if (h == null || mc.field_1687 == null || h.method_17783().ordinal() != 1) return;   // 1 = BLOCK (MISS, BLOCK, ENTITY)
        class_243 p = h.method_17784();
        if (p.method_1022(new class_243(Render3D.cx, Render3D.cy, Render3D.cz)) > reach.get()) return;
        // The hit point lies ON the face; nudge it along the view direction so it falls inside the block that was hit.
        class_2338 bp = new class_2338((int) Math.floor(p.field_1352 + Render3D.fx * 0.01),
                                       (int) Math.floor(p.field_1351 + Render3D.fy * 0.01),
                                       (int) Math.floor(p.field_1350 + Render3D.fz * 0.01));
        class_2680 st = mc.field_1687.method_8320(bp);
        var shape = st.method_26218(mc.field_1687, bp);
        class_238 box = shape.method_1110() ? new class_238(bp) : shape.method_1107().method_996(bp);

        float a = pulse.get() ? 0.75f + 0.25f * (float) Math.sin(System.nanoTime() / 1e9 * 4.0) : 1f;
        int c = color.get();
        boolean prev = Render3D.xray();
        Render3D.xray(false);                                                    // a targeted block is never behind a wall
        Render3D.fillBox(box, Render2D.withAlpha(c, fillOpacity.asFloat() * a));
        Render3D.box(box, Render2D.withAlpha(c, a), 1.5f + width.asInt() * 0.8f);
        Render3D.xray(prev);
    }
}
