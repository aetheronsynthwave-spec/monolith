package dev.monolith.module.impl;
import dev.monolith.setting.*;
import net.minecraft.block.entity.*;
import net.minecraft.class_2586;
import net.minecraft.class_2636;
public class SpawnerFinder extends ESPModule {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFB06CFF));
    public SpawnerFinder() { super("Spawner Finder", "Highlights mob spawners through walls."); showDistance.set(true); range.set(128.0); tracers.set(true); }
    @Override protected boolean accepts(class_2586 be) { return be instanceof class_2636; }
    @Override protected int colorFor(class_2586 be) { return color.get(); }
}
