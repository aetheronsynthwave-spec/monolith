package dev.monolith.hud;
import net.minecraft.util.math.*;
import net.minecraft.class_2350;
import net.minecraft.class_332;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.*;
public class DirectionHud extends HudModule {
    public DirectionHud() { super("Direction", "Facing direction.", 0.01, 0.17); }
    @Override protected void draw(class_332 c) {
        if (mc.field_1724 == null) return;
        class_2350 d = class_2350.method_10150(mc.field_1724.method_36454());
        String n = d.method_15434(); line(c, "DIR", Character.toUpperCase(n.charAt(0)) + n.substring(1));
    }
}
