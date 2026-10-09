package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;

/** Turns on vanilla's "Sneak: Toggle" mode while enabled. */
public class ToggleSneak extends Module {
    private boolean saved;
    public ToggleSneak() { super("ToggleSneak", "Press sneak once to stay sneaking.", Category.MOVEMENT); }
    @Override public void onEnable() { var o = mc.options.getSneakToggled(); saved = o.getValue(); o.setValue(true); }
    @Override public void onDisable() { mc.options.getSneakToggled().setValue(saved); }
}
