package dev.monolith.module.impl;
import dev.monolith.setting.*;
import net.minecraft.block.entity.*;
public class SpawnerFinder extends ESPModule {
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFB06CFF));
    public SpawnerFinder() { super("Spawner Finder", "Highlights mob spawners through walls."); showDistance.set(true); range.set(128.0); tracers.set(true); }
    @Override protected boolean accepts(BlockEntity be) { return be instanceof MobSpawnerBlockEntity; }
    @Override protected int colorFor(BlockEntity be) { return color.get(); }
}
