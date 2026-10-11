package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;

/** Draws chunk boundaries (16x16) and optional 16-block height lines around you. Handy for farms, spawn chunks and builds. */
public class ChunkBorders extends Module {
    public final NumberSetting radius = add(new NumberSetting("Chunk Radius", 1, 0, 3, 1));
    public final NumberSetting height = add(new NumberSetting("Vertical Range", 32, 16, 96, 16));
    public final BoolSetting levels = add(new BoolSetting("16-Block Levels", true));
    public final NumberSetting width = add(new NumberSetting("Line Width", 1, 1, 4, 1));
    public final ColorSetting edge = add(new ColorSetting("Chunk Color", 0xFF38D9E8));
    public final ColorSetting level = add(new ColorSetting("Level Color", 0x88FFFFFF));
    public final BoolSetting walls = add(new BoolSetting("Through Walls", false));

    public ChunkBorders() { super("Chunk Borders", "Chunk edges and 16-block levels around you.", Category.RENDER); }

    @Override public void onRender3D(float delta) {
        if (mc.field_1724 == null) return;
        int R = radius.asInt(), px = (int) Math.floor(Render3D.cx) >> 4, pz = (int) Math.floor(Render3D.cz) >> 4;
        double span = height.get();
        double y0 = Math.floor((Render3D.cy - span) / 16.0) * 16.0, y1 = Math.floor((Render3D.cy + span) / 16.0) * 16.0 + 16.0;
        float w = ESPStyle.pixels(width.asInt());
        boolean prev = Render3D.xray();
        Render3D.xray(walls.get());
        int ec = edge.get(), lc = level.get();
        double minX = (px - R) * 16.0, maxX = (px + R + 1) * 16.0, minZ = (pz - R) * 16.0, maxZ = (pz + R + 1) * 16.0;

        // vertical lines on every chunk corner; the current chunk's corners are drawn full strength, the rest dimmer
        for (int i = -R; i <= R + 1; i++) {
            for (int j = -R; j <= R + 1; j++) {
                double x = (px + i) * 16.0, z = (pz + j) * 16.0;
                boolean own = (i == 0 || i == 1) && (j == 0 || j == 1);
                Render3D.line(x, y0, z, x, y1, z, own ? ec : Render2D.withAlpha(ec, 0.45f), w);
            }
        }
        // horizontal grid on each 16-block level (only the current chunk's square when 16-Block Levels is off)
        for (double y = y0; y <= y1; y += 16.0) {
            boolean here = Render3D.cy >= y && Render3D.cy < y + 16.0;
            if (!levels.get() && !here) continue;
            int c = here ? ec : lc;
            for (int i = -R; i <= R + 1; i++) {
                double x = (px + i) * 16.0, z = (pz + i) * 16.0;
                Render3D.line(x, y, minZ, x, y, maxZ, c, w);
                Render3D.line(minX, y, z, maxX, y, z, c, w);
            }
        }
        Render3D.xray(prev);
    }
}
