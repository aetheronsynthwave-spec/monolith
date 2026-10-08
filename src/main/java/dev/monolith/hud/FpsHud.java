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
public class FpsHud extends HudModule {
    public FpsHud() { super("FPS", "Frames per second.", 0.01, 0.02); }
    @Override protected void draw(DrawContext c) { line(c, "FPS", String.valueOf(mc.getCurrentFps())); }
}
