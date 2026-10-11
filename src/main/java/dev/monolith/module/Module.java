package dev.monolith.module;

import dev.monolith.setting.*;
import java.util.*;
import net.minecraft.class_310;
import net.minecraft.class_332;

/** Base class for all features. Add settings in the constructor via {@link #add}; the GUI picks them up automatically. */
public abstract class Module {
    protected static final class_310 mc = class_310.method_1551();
    public final String name, description;
    public final Category category;
    public final KeybindSetting keybind = new KeybindSetting("Keybind", -1);
    private final List<Setting<?>> settings = new ArrayList<>();
    private boolean enabled;

    protected Module(String name, String description, Category category) {
        this.name = name; this.description = description; this.category = category;
        settings.add(keybind);
    }

    protected <S extends Setting<?>> S add(S s) { settings.add(s); return s; }
    public List<Setting<?>> settings() { return settings; }

    public boolean isEnabled() { return enabled; }
    public void toggle() { setEnabled(!enabled); }
    public void setEnabled(boolean e) {
        if (e == enabled) return;
        enabled = e;
        if (e) onEnable(); else onDisable();
    }

    public void onEnable() {}
    public void onDisable() {}
    public void onTick() {}
    /** Called at the START of every client tick, before the player processes input. */
    public void onPreTick() {}
    /** Called when leaving a world/server. */
    public void onDisconnect() {}
    /** Called every rendered frame (even when the HUD is hidden) while enabled and in a world. Use for smooth animation. */
    public void onFrame(float delta) {}
    /** 2D overlay; only called while enabled and in a world. */
    public void onHud(class_332 ctx, float delta) {}
    /** World-space drawing (Render3D.*). Called once per frame, after entities are drawn, only while enabled and in a world. */
    public void onRender3D(float delta) {}
}
