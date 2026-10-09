package dev.monolith.hud;
import net.minecraft.client.gui.DrawContext;
public class CoordsHud extends HudModule {
    private int lx = Integer.MIN_VALUE, ly, lz; private String txt = "";
    public CoordsHud() { super("Coordinates", "Your XYZ position.", 0.01, 0.12); }
    @Override protected void draw(DrawContext c) {
        if (mc.player == null) return;
        int x = (int) Math.floor(mc.player.getX()), y = (int) Math.floor(mc.player.getY()), z = (int) Math.floor(mc.player.getZ());
        if (x != lx || y != ly || z != lz) { lx = x; ly = y; lz = z; txt = x + "  " + y + "  " + z; }   // only rebuild the string when a block changes
        line(c, "XYZ", txt);
    }
}
