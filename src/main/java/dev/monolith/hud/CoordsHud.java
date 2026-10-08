package dev.monolith.hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.*;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.*;
public class CoordsHud extends HudModule {
    public CoordsHud() { super("Coordinates", "Your XYZ position.", 0.01, 0.12); }
    @Override protected void draw(DrawContext c) {
        if (mc.player == null) return;
        line(c, "XYZ", String.format("%.0f  %.0f  %.0f", mc.player.getX(), mc.player.getY(), mc.player.getZ()));
    }
}
