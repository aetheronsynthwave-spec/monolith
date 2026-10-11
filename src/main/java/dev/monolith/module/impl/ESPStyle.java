package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;

/**
 * One switch for the look of every ESP box in the client (Storage Finder, Spawner Finder, ESP, X-Ray, Block Highlight...).
 * ON  = the filled "glass" boxes from the screenshot (translucent faces + crisp outline, optionally through walls).
 * OFF = thin 3D outlines only.
 */
public class ESPStyle extends Module {
    public enum Mode { FILL_OUTLINE, OUTLINE, FILL }
    private static ESPStyle I;

    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Style", Mode.FILL_OUTLINE));
    public final NumberSetting fillOpacity = add(new NumberSetting("Fill Opacity", 0.22, 0.02, 0.8, 0.02));
    public final NumberSetting outlineOpacity = add(new NumberSetting("Outline Opacity", 1.0, 0.2, 1.0, 0.05));
    public final NumberSetting thickness = add(new NumberSetting("Line Thickness", 1.0, 0.5, 4.0, 0.1));
    public final BoolSetting throughWalls = add(new BoolSetting("Through Walls", true));
    public final BoolSetting faceShading = add(new BoolSetting("Face Shading", true));

    public ESPStyle() {
        super("ESP Style", "Filled glass boxes or thin outlines for every ESP.", Category.RENDER);
        I = this;
        throughWalls.tooltip = "Off = boxes are hidden behind blocks, like normal geometry.";
        faceShading.tooltip = "Darker bottom and sides so a filled box looks solid instead of flat.";
    }

    private static boolean on() { return I != null && I.isEnabled(); }
    public static boolean fill() { return on() && I.mode.get() != Mode.OUTLINE; }
    public static boolean outline() { return !on() || I.mode.get() != Mode.FILL; }
    public static float fillAlpha() { return on() ? I.fillOpacity.asFloat() : 0f; }
    public static float outlineAlpha() { return on() ? I.outlineOpacity.asFloat() : 1f; }
    public static float thickness() { return on() ? I.thickness.asFloat() : 1f; }
    public static boolean walls() { return on() ? I.throughWalls.get() : true; }
    public static boolean shade() { return on() ? I.faceShading.get() : true; }
    /** Old 1..4 "pixel" line settings to a sensible 3D line width. */
    public static float pixels(int th) { return 1.6f + 0.8f * (Math.max(1, th) - 1); }
}
