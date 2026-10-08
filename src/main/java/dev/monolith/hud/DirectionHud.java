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
public class DirectionHud extends HudModule {
    public DirectionHud() { super("Direction", "Facing direction.", 0.01, 0.17); }
    @Override protected void draw(DrawContext c) {
        if (mc.player == null) return;
        Direction d = Direction.fromHorizontalDegrees(mc.player.getYaw());
        String n = d.asString(); line(c, "DIR", Character.toUpperCase(n.charAt(0)) + n.substring(1));
    }
}
