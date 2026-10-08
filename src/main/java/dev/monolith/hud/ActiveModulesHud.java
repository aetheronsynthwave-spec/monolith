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
public class ActiveModulesHud extends HudModule {
    public final BoolSetting alignRight = add(new BoolSetting("Align Right", false));
    public ActiveModulesHud() { super("Active Modules", "List of enabled modules.", 0.88, 0.02); }
    @Override protected void draw(DrawContext c) {
        List<String> names = new ArrayList<>();
        for (var m : dev.monolith.Monolith.modules().all()) if (m.isEnabled() && m.category != dev.monolith.module.Category.HUD) names.add(m.name);
        names.sort((a, b) -> Render2D.width(b) - Render2D.width(a));
        int w = 70; for (String n : names) w = Math.max(w, Render2D.width(n) + 10);
        panel(c, w, Math.max(16, names.size() * 12 + 6));
        int y = 4; for (String n : names) { Render2D.text(c, n, alignRight.get() ? w - 5 - Render2D.width(n) : 5, y, textColor.get()); y += 12; }
    }
}
