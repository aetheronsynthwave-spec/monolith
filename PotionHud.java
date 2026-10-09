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
public class PotionHud extends HudModule {
    public PotionHud() { super("Potion HUD", "Active status effects.", 0.88, 0.40); }
    @Override protected void draw(DrawContext c) {
        if (mc.player == null) return;
        var fx = new ArrayList<>(mc.player.getStatusEffects());
        panel(c, 100, Math.max(16, fx.size() * 12 + 6));
        int y = 4;
        for (StatusEffectInstance e : fx) {
            int s = e.getDuration() / 20;
            String t = e.isInfinite() ? "inf" : String.format("%d:%02d", s / 60, s % 60);
            Render2D.text(c, e.getEffectType().value().getName().getString() + " " + (e.getAmplifier() + 1), 5, y, textColor.get());
            Render2D.text(c, t, 100 - 5 - Render2D.width(t), y, Theme.TEXT_DIM);
            y += 12;
        }
    }
}
