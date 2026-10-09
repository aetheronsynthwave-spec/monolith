package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;

/** Uses the vanilla "Damage Tilt" accessibility option, set to 0 while enabled. */
public class NoHurtCam extends Module {
    private double saved = -1;
    public NoHurtCam() { super("No Hurt Cam", "Removes the camera shake when hit.", Category.RENDER); }
    @Override public void onEnable() { var o = mc.options.getDamageTiltStrength(); saved = o.getValue(); o.setValue(0.0); }
    @Override public void onDisable() { if (saved >= 0) { mc.options.getDamageTiltStrength().setValue(saved); saved = -1; } }
}
