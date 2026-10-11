package dev.monolith.hud;
import net.minecraft.util.math.*;
import net.minecraft.class_1304;
import net.minecraft.class_1799;
import net.minecraft.class_332;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.*;
public class ArmorHud extends HudModule {
    public ArmorHud() { super("Armor HUD", "Equipped armor and durability.", 0.01, 0.40); }
    @Override protected void draw(class_332 c) {
        if (mc.field_1724 == null) return;
        class_1304[] slots = { class_1304.field_6169, class_1304.field_6174, class_1304.field_6172, class_1304.field_6166, class_1304.field_6173 };
        panel(c, 20 + 30, slots.length * 18 + 2);
        int y = 1;
        for (class_1304 s : slots) {
            class_1799 st = mc.field_1724.method_6118(s);
            if (!st.method_7960()) {
                c.method_51427(st, 2, y); c.method_51431(mc.field_1772, st, 2, y);
                if (st.method_7963()) Render2D.text(c, (st.method_7936() - st.method_7919()) + "", 22, y + 4, textColor.get());
            }
            y += 18;
        }
    }
}
