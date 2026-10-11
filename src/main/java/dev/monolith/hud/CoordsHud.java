package dev.monolith.hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.BlockPos;
import dev.monolith.render.*;

public class CoordsHud extends HudModule {
    private int lx = Integer.MIN_VALUE, ly, lz; private String xyz = "", biome = "";
    private final BlockPos.Mutable pos = new BlockPos.Mutable();
    public CoordsHud() { super("Coordinates", "XYZ and biome.", 0.01, 0.12); }

    @Override protected void draw(DrawContext c) {
        if (mc.player == null || mc.world == null) return;
        int x = (int) Math.floor(mc.player.getX()), y = (int) Math.floor(mc.player.getY()), z = (int) Math.floor(mc.player.getZ());
        if (x != lx || y != ly || z != lz) {
            lx = x; ly = y; lz = z; xyz = x + " " + y + " " + z;
            String id = mc.world.getBiome(pos.set(x, y, z)).getKey().map(k -> k.getValue().getPath()).orElse("unknown");
            StringBuilder sb = new StringBuilder(id.length());
            boolean up = true;
            for (char ch : id.toCharArray()) { if (ch == '_') { sb.append(' '); up = true; } else { sb.append(up ? Character.toUpperCase(ch) : ch); up = false; } }
            biome = sb.toString();
        }
        int w = Math.max(Render2D.width("XYZ " + xyz), Render2D.width("Biome " + biome)) + 10;
        panel(c, w, 28);
        Render2D.text(c, "XYZ", 5, 4, Theme.TEXT_DIM); Render2D.text(c, xyz, 5 + Render2D.width("XYZ "), 4, textColor.get());
        Render2D.text(c, "Biome", 5, 16, Theme.TEXT_DIM); Render2D.text(c, biome, 5 + Render2D.width("Biome "), 16, textColor.get());
    }
}
