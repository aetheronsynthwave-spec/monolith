package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import net.minecraft.class_1297;
import net.minecraft.class_1309;
import net.minecraft.class_2828;

/** Sends a tiny fake hop just before each hit so the server counts it as a critical. Strict anti-cheats will flag this. */
public class Criticals extends Module {
    public Criticals() { super("Criticals", "Hits count as critical hits while on the ground.", Category.COMBAT); }

    public void onAttack(class_1297 target) {
        if (!isEnabled() || mc.field_1724 == null || !(target instanceof class_1309)) return;
        if (!mc.field_1724.method_24828() || mc.field_1724.method_5799() || mc.field_1724.method_5771() || mc.field_1724.method_6101()) return;
        var net = mc.method_1562();
        if (net == null) return;
        double x = mc.field_1724.method_23317(), y = mc.field_1724.method_23318(), z = mc.field_1724.method_23321();
        net.method_52787(new class_2828.class_2829(x, y + 0.0625, z, false, false));
        net.method_52787(new class_2828.class_2829(x, y, z, false, false));
    }
}
