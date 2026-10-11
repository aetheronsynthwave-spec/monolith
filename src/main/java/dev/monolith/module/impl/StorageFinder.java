package dev.monolith.module.impl;

import dev.monolith.setting.*;
import net.minecraft.block.entity.*;
import net.minecraft.class_2586;
import net.minecraft.class_2595;
import net.minecraft.class_2601;
import net.minecraft.class_2608;
import net.minecraft.class_2614;
import net.minecraft.class_2627;
import net.minecraft.class_2646;
import net.minecraft.class_3719;

public class StorageFinder extends ESPModule {
    public final BoolSetting chests = add(new BoolSetting("Chests", true));
    public final BoolSetting trapped = add(new BoolSetting("Trapped Chests", true));
    public final BoolSetting barrels = add(new BoolSetting("Barrels", true));
    public final BoolSetting shulkers = add(new BoolSetting("Shulker Boxes", true));
    public final BoolSetting droppers = add(new BoolSetting("Droppers", true));
    public final BoolSetting dispensers = add(new BoolSetting("Dispensers", true));
    public final BoolSetting hoppers = add(new BoolSetting("Hoppers", true));
    // Each type has its own colour (change in the GUI).
    public final ColorSetting cChest = add(new ColorSetting("Chest Color", 0xFFFF9F43));
    public final ColorSetting cTrapped = add(new ColorSetting("Trapped Color", 0xFFFF4D4D));
    public final ColorSetting cBarrel = add(new ColorSetting("Barrel Color", 0xFFFFD93D));
    public final ColorSetting cShulker = add(new ColorSetting("Shulker Color", 0xFFFF6FB5));
    public final ColorSetting cDropper = add(new ColorSetting("Dropper Color", 0xFF4FD8FF));
    public final ColorSetting cDispenser = add(new ColorSetting("Dispenser Color", 0xFF5C7CFF));
    public final ColorSetting cHopper = add(new ColorSetting("Hopper Color", 0xFF5EE08A));

    public StorageFinder() { super("Storage Finder", "Highlights containers through walls."); }

    @Override protected boolean accepts(class_2586 be) {
        return be instanceof class_2595 || be instanceof class_3719 || be instanceof class_2627
            || be instanceof class_2608 || be instanceof class_2601 || be instanceof class_2614;
    }

    @Override protected int colorFor(class_2586 be) {
        // Order matters: Trapped is a subclass of Chest, Dropper a subclass of Dispenser.
        if (be instanceof class_2646) return trapped.get() ? cTrapped.get() : 0;
        if (be instanceof class_2595) return chests.get() ? cChest.get() : 0;
        if (be instanceof class_3719) return barrels.get() ? cBarrel.get() : 0;
        if (be instanceof class_2627) return shulkers.get() ? cShulker.get() : 0;
        if (be instanceof class_2608) return droppers.get() ? cDropper.get() : 0;
        if (be instanceof class_2601) return dispensers.get() ? cDispenser.get() : 0;
        if (be instanceof class_2614) return hoppers.get() ? cHopper.get() : 0;
        return 0;
    }
}
