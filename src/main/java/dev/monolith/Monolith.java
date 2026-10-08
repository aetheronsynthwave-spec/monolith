package dev.monolith;
import dev.monolith.module.Module;

import dev.monolith.config.ConfigManager;
import dev.monolith.gui.ClickGuiScreen;
import dev.monolith.hud.HudModule;
import dev.monolith.module.*;
import dev.monolith.module.impl.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.MinecraftClient;
import org.lwjgl.glfw.GLFW;
import org.slf4j.*;

public class Monolith implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("Monolith");
    private static ModuleManager modules; private static ConfigManager config;
    public static ModuleManager modules() { return modules; }
    public static ConfigManager config() { return config; }
    /** Right Shift opens the Click GUI. Rebind in the GUI header. */
    public static int guiKey = GLFW.GLFW_KEY_RIGHT_SHIFT;
    private boolean guiKeyDown;

    @Override public void onInitializeClient() {
        modules = new ModuleManager();
        config = new ConfigManager();
        // First-run defaults: a sensible HUD.
        for (String n : new String[]{"FPS", "Coordinates", "Keystrokes", "CPS", "Armor HUD"}) modules.all().stream().filter(m -> m.name.equals(n)).forEach(m -> m.setEnabled(true));
        config.loadActive();

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            modules.tick();
            boolean down = mc.getWindow() != null && GLFW.glfwGetKey(mc.getWindow().getHandle(), guiKey) == GLFW.GLFW_PRESS;
            if (down && !guiKeyDown && mc.currentScreen == null && mc.world != null) mc.setScreen(new ClickGuiScreen());
            guiKeyDown = down;
        });

        // HUD overlay.
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.world == null || mc.options.hudHidden) return;
            for (Module m : modules.all()) if (m.isEnabled()) m.onHud(ctx, tick.getTickProgress(false));
        });

        // Block-entity cache for the finders (no per-frame chunk scanning).
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, w) -> {
            for (ESPModule f : finders()) f.track(be); });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, w) -> {
            for (ESPModule f : finders()) f.untrack(be); });
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> { for (ESPModule f : finders()) f.clear(); });

        LOGGER.info("Monolith ready: {} modules", modules.all().size());
    }

    private static java.util.List<ESPModule> finders() {
        return modules.all().stream().filter(m -> m instanceof ESPModule).map(m -> (ESPModule) m).toList();
    }
}
