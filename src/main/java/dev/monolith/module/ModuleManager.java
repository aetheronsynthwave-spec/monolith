package dev.monolith.module;

import dev.monolith.module.impl.*;
import dev.monolith.hud.*;
import org.lwjgl.glfw.GLFW;
import net.minecraft.client.MinecraftClient;
import java.util.*;

public class ModuleManager {
    private final List<Module> modules = new ArrayList<>();
    private final boolean[] keyState = new boolean[GLFW.GLFW_KEY_LAST + 1];

    public ModuleManager() {
        register(new StorageFinder());
        register(new SpawnerFinder());
        register(new Fullbright());
        register(new ToggleSprint());
        register(new ToggleSneak());
        register(new Zoom());
        register(new CrosshairModule());
        register(new NoHurtCam());
        register(new HitboxESP());
        register(new Aimbot());
        register(new Waypoints());
        register(new ScoreboardTweak());
        register(new ChatTweak());
        register(new TabListTweak());
        register(new FpsHud());
        register(new PingHud());
        register(new CoordsHud());
        register(new DirectionHud());
        register(new CpsHud());
        register(new KeystrokesHud());
        register(new ArmorHud());
        register(new PotionHud());
        register(new ActiveModulesHud());
        register(new SessionHud());
        register(new TargetHud());
    }

    public void register(Module m) { modules.add(m); }
    public List<Module> all() { return modules; }
    public List<Module> byCategory(Category c) { return modules.stream().filter(m -> m.category == c).toList(); }
    @SuppressWarnings("unchecked") public <T extends Module> T get(Class<T> c) {
        for (Module m : modules) if (c.isInstance(m)) return (T) m; return null; }

    public void tick() {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getWindow() == null) return;
        long h = mc.getWindow().getHandle();
        for (Module m : modules) {
            int k = m.keybind.get();
            if (k > 0 && k <= GLFW.GLFW_KEY_LAST && mc.currentScreen == null) {
                boolean down = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
                if (down && !keyState[k]) m.toggle();
                keyState[k] = down;
            }
            if (m.isEnabled() && mc.player != null) m.onTick();
        }
    }
}
