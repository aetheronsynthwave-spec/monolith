package dev.monolith.hud;
import net.minecraft.util.math.*;
import net.minecraft.class_332;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.*;
public class FpsHud extends HudModule {
    public FpsHud() { super("FPS", "Frames per second.", 0.01, 0.02); }
    @Override protected void draw(class_332 c) { line(c, "FPS", String.valueOf(mc.method_47599())); }
}
