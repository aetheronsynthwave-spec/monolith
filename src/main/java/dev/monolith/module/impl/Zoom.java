package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import org.lwjgl.glfw.GLFW;

/** Hold the zoom key to smoothly narrow the FOV. Vanilla clamps FOV to 30..110, so 30 is the strongest zoom. */
public class Zoom extends Module {
    public final KeybindSetting zoomKey = add(new KeybindSetting("Zoom Key", GLFW.GLFW_KEY_C));
    public final NumberSetting zoomFov = add(new NumberSetting("Zoom FOV", 30, 30, 60, 1));
    public final NumberSetting speed = add(new NumberSetting("Smoothing", 12, 3, 30, 1));
    public final BoolSetting reduceSens = add(new BoolSetting("Reduce Sensitivity", true));
    private double baseFov = -1, baseSens, cur;
    private long last = System.nanoTime();

    public Zoom() { super("Zoom", "Hold a key to zoom in smoothly.", Category.RENDER); }

    @Override public void onFrame(float delta) {
        long now = System.nanoTime(); float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;
        int k = zoomKey.get();
        boolean held = mc.currentScreen == null && k > 0 && k <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(mc.getWindow().getHandle(), k) == GLFW.GLFW_PRESS;
        var fov = mc.options.getFov();
        if (baseFov < 0) {
            if (!held) return;
            baseFov = fov.getValue(); cur = baseFov; baseSens = mc.options.getMouseSensitivity().getValue();
        }
        double target = held ? zoomFov.get() : baseFov;
        cur += (target - cur) * Math.min(1.0, dt * speed.get());
        fov.setValue((int) Math.round(cur));
        if (reduceSens.get()) mc.options.getMouseSensitivity().setValue(held ? baseSens * (cur / baseFov) : baseSens);
        if (!held && Math.abs(cur - baseFov) < 0.6) restore();
    }

    private void restore() {
        if (baseFov < 0) return;
        mc.options.getFov().setValue((int) Math.round(baseFov));
        mc.options.getMouseSensitivity().setValue(baseSens);
        baseFov = -1;
    }
    @Override public void onDisable() { restore(); }
}
