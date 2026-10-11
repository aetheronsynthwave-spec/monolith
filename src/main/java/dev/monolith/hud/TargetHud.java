package dev.monolith.hud;
import net.minecraft.util.math.*;
import net.minecraft.class_1309;
import net.minecraft.class_332;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.*;
public class TargetHud extends HudModule {
    public TargetHud() { super("Target Info", "Health of the entity you are looking at.", 0.42, 0.62); }
    @Override protected void draw(class_332 c) {
        if (!(mc.field_1692 instanceof class_1309 t)) { contentW = 0; contentH = 0; return; }
        panel(c, 110, 28);
        Render2D.text(c, t.method_5477().getString(), 6, 4, textColor.get());
        float f = Math.max(0, Math.min(1, t.method_6032() / t.method_6063()));
        Render2D.rounded(c, 6, 18, 98, 4, 2, Theme.TRACK);
        Render2D.rounded(c, 6, 18, Math.max(3, 98 * f), 4, 2, Theme.ACCENT);
    }
}
