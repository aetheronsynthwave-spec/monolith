package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.class_1297;
import org.lwjgl.glfw.GLFW;

/**
 * Hold the zoom key to smoothly narrow the FOV.
 *
 * WHY THE OLD VERSION BROKE: it lowered the saved "Mouse Sensitivity" option while zooming and put it back afterwards. If Minecraft
 * wrote options.txt in between (changing any setting, F11, a crash, closing the game from the launcher...) the LOW value became the
 * new permanent sensitivity. Now the sensitivity option is never touched: the mouse movement itself is scaled by EntityLookMixin.
 *
 * FOV is still changed through the option (the whole client reads it), but safely:
 *  - it is restored instantly whenever a menu opens, you leave the world, or the module is disabled/the game closes;
 *  - while zooming, the original FOV is written to config/monolith/zoom.lock, so even after a crash it is restored on next start.
 */
public class Zoom extends Module {
    private static Zoom INSTANCE;
    public final KeybindSetting zoomKey = add(new KeybindSetting("Zoom Key", GLFW.GLFW_KEY_C));
    public final NumberSetting zoomFov = add(new NumberSetting("Zoom FOV", 30, 30, 60, 1));
    public final NumberSetting speed = add(new NumberSetting("Smoothing", 12, 3, 30, 1));
    public final BoolSetting reduceSens = add(new BoolSetting("Reduce Sensitivity", true));
    private double baseFov = -1, cur;
    private long last = System.nanoTime();
    private boolean checkedLock;

    public Zoom() {
        super("Zoom", "Hold a key to zoom in smoothly.", Category.RENDER);
        INSTANCE = this;
    }

    /** Factor (0.12..1) applied to mouse movement while zoomed; 1 when not zooming. Read by EntityLookMixin. */
    public static double lookScale() {
        Zoom z = INSTANCE;
        if (z == null || !z.isEnabled() || z.baseFov <= 0 || !z.reduceSens.get()) return 1.0;
        return Math.max(0.12, Math.min(1.0, z.cur / z.baseFov));
    }

    /** Called by the mixin for every mouse look; only the local player is affected. */
    public static double scaleLook(class_1297 self, double delta) {
        return self == mc.field_1724 ? delta * lookScale() : delta;
    }

    private static Path lock() { return FabricLoader.getInstance().getConfigDir().resolve("monolith").resolve("zoom.lock"); }

    /** Crash recovery: if the game died while zoomed, put the FOV back. */
    private void recoverAfterCrash() {
        checkedLock = true;
        try {
            Path p = lock();
            if (Files.exists(p)) {
                mc.field_1690.method_41808().method_41748((int) Math.round(Double.parseDouble(Files.readString(p).trim())));
                Files.deleteIfExists(p);
            }
        } catch (Throwable ignored) { }
    }

    @Override public void onFrame(float delta) {
        long now = System.nanoTime(); float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;
        if (!checkedLock) recoverAfterCrash();
        int k = zoomKey.get();
        boolean screen = mc.field_1755 != null;
        boolean held = !screen && k > 0 && k <= GLFW.GLFW_KEY_LAST
                && GLFW.glfwGetKey(mc.method_22683().method_4490(), k) == GLFW.GLFW_PRESS;
        var fov = mc.field_1690.method_41808();

        if (baseFov < 0) {
            if (!held) return;
            baseFov = fov.method_41753(); cur = baseFov;
            try { Files.createDirectories(lock().getParent()); Files.writeString(lock(), Double.toString(baseFov)); } catch (Throwable ignored) { }
        } else if (screen) {                                                    // never leave the option changed while a menu is open
            restore(); return;
        }

        double target = held ? zoomFov.get() : baseFov;
        cur += (target - cur) * Math.min(1.0, dt * speed.get());
        int v = (int) Math.round(cur);
        if (v != fov.method_41753()) fov.method_41748(v);                       // only write when the value actually changes
        if (!held && Math.abs(cur - baseFov) < 0.6) restore();
    }

    private void restore() {
        if (baseFov < 0) return;
        try { mc.field_1690.method_41808().method_41748((int) Math.round(baseFov)); } catch (Throwable ignored) { }
        baseFov = -1;
        try { Files.deleteIfExists(lock()); } catch (Throwable ignored) { }
    }

    @Override public void onDisable() { restore(); }
    @Override public void onDisconnect() { restore(); }
}
