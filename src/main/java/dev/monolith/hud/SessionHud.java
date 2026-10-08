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
public class SessionHud extends HudModule {
    private final long start = System.currentTimeMillis();
    public SessionHud() { super("Session", "Time played this session.", 0.01, 0.27); }
    @Override protected void draw(DrawContext c) {
        long s = (System.currentTimeMillis() - start) / 1000;
        line(c, "SESSION", String.format("%d:%02d:%02d", s / 3600, s / 60 % 60, s % 60));
    }
}
