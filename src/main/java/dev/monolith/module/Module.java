package dev.monolith.module;

import dev.monolith.setting.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import java.util.*;

/** Base class for all features. Add settings in the constructor via {@link #add}; the GUI picks them up automatically. */
public abstract class Module {
    protected static final MinecraftClient mc = MinecraftClient.getInstance();
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
    /** Called every rendered frame (even when the HUD is hidden) while enabled and in a world. Use for smooth animation. */
    public void onFrame(float delta) {}
    /** 2D overlay; only called while enabled and in a world. */
    public void onHud(DrawContext ctx, float delta) {}
}
