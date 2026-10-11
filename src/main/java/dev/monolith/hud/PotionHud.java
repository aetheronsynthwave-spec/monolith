package dev.monolith.hud;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.util.Identifier;
import dev.monolith.render.*;
import java.util.*;

public class PotionHud extends HudModule {
    private record Info(Identifier icon, String name) {}
    private final Map<Object, Info> cache = new HashMap<>();
    public PotionHud() { super("Potion HUD", "Active effects with icons and timers.", 0.88, 0.40); }

    @Override protected void draw(DrawContext c) {
        if (mc.player == null) return;
        var fx = mc.player.getStatusEffects();
        panel(c, 110, Math.max(16, fx.size() * 20 + 4));
        int y = 3;
        for (StatusEffectInstance e : fx) {
            Info info = cache.computeIfAbsent(e.getEffectType(), k -> {
                var key = e.getEffectType().getKey().map(r -> r.getValue().getPath()).orElse("speed");
                return new Info(Identifier.ofVanilla("mob_effect/" + key), e.getEffectType().value().getName().getString());
            });
            c.drawGuiTexture(RenderPipelines.GUI_TEXTURED, info.icon(), 4, y, 16, 16);
            Render2D.text(c, info.name() + (e.getAmplifier() > 0 ? " " + (e.getAmplifier() + 1) : ""), 24, y, textColor.get());
            int s = e.getDuration() / 20;
            String t = e.isInfinite() ? "inf" : (s / 60) + ":" + (s % 60 < 10 ? "0" : "") + (s % 60);
            boolean flash = !e.isInfinite() && s < 10 && (System.currentTimeMillis() / 300 % 2 == 0);
            Render2D.text(c, t, 24, y + 9, flash ? 0xFFFF5555 : Theme.TEXT_DIM);
            y += 20;
        }
    }
}
