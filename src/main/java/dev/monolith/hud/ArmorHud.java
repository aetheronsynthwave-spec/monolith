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
public class ArmorHud extends HudModule {
    public ArmorHud() { super("Armor HUD", "Equipped armor and durability.", 0.01, 0.40); }
    @Override protected void draw(DrawContext c) {
        if (mc.player == null) return;
        EquipmentSlot[] slots = { EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET, EquipmentSlot.MAINHAND };
        panel(c, 20 + 30, slots.length * 18 + 2);
        int y = 1;
        for (EquipmentSlot s : slots) {
            ItemStack st = mc.player.getEquippedStack(s);
            if (!st.isEmpty()) {
                c.drawItem(st, 2, y); c.drawStackOverlay(mc.textRenderer, st, 2, y);
                if (st.isDamageable()) Render2D.text(c, (st.getMaxDamage() - st.getDamage()) + "", 22, y + 4, textColor.get());
            }
            y += 18;
        }
    }
}
