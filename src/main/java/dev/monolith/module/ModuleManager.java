package dev.monolith.module;

import dev.monolith.module.impl.*;
import dev.monolith.hud.*;
import org.lwjgl.glfw.GLFW;
import java.util.*;
import net.minecraft.class_310;

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
        register(new ESPStyle());
        register(new Tracers());
        register(new BlockHighlight());
        register(new ChunkBorders());
        register(new Breadcrumbs());
        register(new Aimbot());
        register(new BowAimbot());
        register(new AutoTotem());
        register(new Criticals());
        register(new Speed());
        register(new XRay());
        register(new Freecam());
        register(new ParticleAura());
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
        register(new dev.monolith.hud.Radar());
    }

    public void register(Module m) { modules.add(m); }
    public List<Module> all() { return modules; }
    public List<Module> byCategory(Category c) { return modules.stream().filter(m -> m.category == c).toList(); }
    @SuppressWarnings("unchecked") public <T extends Module> T get(Class<T> c) {
        for (Module m : modules) if (c.isInstance(m)) return (T) m; return null; }

    public void preTick() {
        if (class_310.method_1551().field_1724 == null) return;
        for (Module m : modules) if (m.isEnabled()) m.onPreTick();
    }

    public void tick() {
        class_310 mc = class_310.method_1551();
        if (mc.method_22683() == null) return;
        long h = mc.method_22683().method_4490();
        for (Module m : modules) {
            int k = m.keybind.get();
            if (k > 0 && k <= GLFW.GLFW_KEY_LAST && mc.field_1755 == null) {
                boolean down = GLFW.glfwGetKey(h, k) == GLFW.GLFW_PRESS;
                if (down && !keyState[k]) m.toggle();
                keyState[k] = down;
            }
            if (m.isEnabled() && mc.field_1724 != null) m.onTick();
        }
    }
}
