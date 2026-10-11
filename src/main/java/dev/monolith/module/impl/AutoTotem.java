package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.minecraft.class_1713;
import net.minecraft.class_1802;
import net.minecraft.class_490;

/** Keeps a Totem of Undying in your off-hand by swapping one in from your inventory. */
public class AutoTotem extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 10, 1));
    public final NumberSetting belowHealth = add(new NumberSetting("Only Below Health", 20, 2, 20, 1));
    private int cooldown;

    public AutoTotem() {
        super("AutoTotem", "Refills your off-hand with a totem.", Category.COMBAT);
        belowHealth.tooltip = "20 = always keep a totem in the off-hand.";
    }

    @Override public void onTick() {
        if (mc.field_1761 == null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (mc.field_1755 != null && !(mc.field_1755 instanceof class_490)) return;   // not while another container is open
        if (mc.field_1724.method_6079().method_31574(class_1802.field_8288)) return;
        if (mc.field_1724.method_6032() + mc.field_1724.method_6067() > belowHealth.get()) return;
        int slot = -1;
        for (int i = 0; i < 36; i++) if (mc.field_1724.method_31548().method_5438(i).method_31574(class_1802.field_8288)) { slot = i; break; }   // hotbar first
        if (slot < 0) return;
        int handlerSlot = slot < 9 ? 36 + slot : slot;           // inventory index -> player screen handler slot
        mc.field_1761.method_2906(mc.field_1724.field_7498.field_7763, handlerSlot, 40, class_1713.field_7791, mc.field_1724);   // button 40 = swap with off-hand
        cooldown = delay.asInt();
    }
}
