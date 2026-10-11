package dev.monolith.module.impl;
import dev.monolith.module.*;
public class ToggleSprint extends Module {
    public ToggleSprint() { super("ToggleSprint", "Hold sprint automatically while moving forward.", Category.MOVEMENT); }
    @Override public void onTick() {
        if (mc.field_1690.field_1894.method_1434() && !mc.field_1724.method_5715() && !mc.field_1724.field_5976
            && mc.field_1724.method_7344().method_7586() > 6) mc.field_1724.method_5728(true);
    }
}
