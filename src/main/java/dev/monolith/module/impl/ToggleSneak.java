package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;

/** Turns on vanilla's "Sneak: Toggle" mode while enabled. */
public class ToggleSneak extends Module {
    private boolean saved;
    public ToggleSneak() { super("ToggleSneak", "Press sneak once to stay sneaking.", Category.MOVEMENT); }
    @Override public void onEnable() { var o = mc.field_1690.method_42449(); saved = o.method_41753(); o.method_41748(true); }
    @Override public void onDisable() { mc.field_1690.method_42449().method_41748(saved); }
}
