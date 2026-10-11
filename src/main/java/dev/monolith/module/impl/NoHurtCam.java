package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;

/** Uses the vanilla "Damage Tilt" accessibility option, set to 0 while enabled. */
public class NoHurtCam extends Module {
    private double saved = -1;
    public NoHurtCam() { super("No Hurt Cam", "Removes the camera shake when hit.", Category.RENDER); }
    @Override public void onEnable() { var o = mc.field_1690.method_48974(); saved = o.method_41753(); o.method_41748(0.0); }
    @Override public void onDisable() { if (saved >= 0) { mc.field_1690.method_48974().method_41748(saved); saved = -1; } }
}
