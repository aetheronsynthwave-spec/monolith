package dev.monolith;
import dev.monolith.config.ConfigManager;
import dev.monolith.gui.ClickGuiScreen;
import dev.monolith.hud.HudModule;
import dev.monolith.module.*;
import dev.monolith.module.impl.*;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.*;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import dev.monolith.render.Projector;
import dev.monolith.render.Render3D;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.*;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.class_1269;
import net.minecraft.class_310;
import net.minecraft.class_332;
import net.minecraft.class_9779;
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
        for (String n : new String[]{"FPS", "Coordinates", "Keystrokes", "CPS", "Armor HUD", "ESP Style"}) modules.all().stream().filter(m -> m.name.equals(n)).forEach(m -> m.setEnabled(true));
        config.loadActive();

        ClientTickEvents.START_CLIENT_TICK.register(client -> modules.preTick());
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            modules.tick();
            boolean down = mc.method_22683() != null && GLFW.glfwGetKey(mc.method_22683().method_4490(), guiKey) == GLFW.GLFW_PRESS;
            if (down && !guiKeyDown && mc.field_1755 == null && mc.field_1687 != null) mc.method_1507(new ClickGuiScreen());
            guiKeyDown = down;
        });

        // Per-frame work: smooth animations, then the overlay. Projection maths is shared by all ESP-style modules.
        HudRenderCallback.EVENT.register((ctx, tick) -> {
            class_310 client = class_310.method_1551();
            if (client.field_1687 == null) return;
            float delta = tick.method_60637(false);
            for (Module m : modules.all()) if (m.isEnabled()) m.onFrame(delta);
            Freecam fc = modules.get(Freecam.class); if (fc != null) fc.returnFrame();
            if (client.field_1690.field_1842) return;
            Projector.update(ctx, delta);
            for (Module m : modules.all()) if (m.isEnabled()) m.onHud(ctx, delta);
        });


        // World-space pass (3D ESP boxes, tracers, trails). Render3D batches everything and draws it once in end().
        WorldRenderEvents.AFTER_ENTITIES.register(wrc -> {
            class_310 client = class_310.method_1551();
            if (client.field_1687 == null || client.field_1724 == null) return;
            if (!Render3D.begin(wrc)) return;
            try {
                float delta = client.method_61966().method_60637(false);
                for (Module m : modules.all()) if (m.isEnabled()) m.onRender3D(delta);
            } catch (Throwable t) {
                LOGGER.error("Render3D pass failed", t);
            } finally {
                Render3D.end();
            }
        });

        // Wrap vanilla HUD elements (crosshair, scoreboard, chat, player list). No mixins needed.
        HudElementRegistry.replaceElement(VanillaHudElements.CROSSHAIR, vanilla -> (ctx, tick) -> {
            CrosshairModule cm = modules.get(CrosshairModule.class);
            if (cm != null && cm.isEnabled()) cm.draw(ctx); else vanilla.render(ctx, tick);
        });
        HudElementRegistry.replaceElement(VanillaHudElements.SCOREBOARD, vanilla -> (ctx, tick) -> tweak(ScoreboardTweak.class, vanilla, ctx, tick));
        HudElementRegistry.replaceElement(VanillaHudElements.CHAT, vanilla -> (ctx, tick) -> tweak(ChatTweak.class, vanilla, ctx, tick));
        HudElementRegistry.replaceElement(VanillaHudElements.PLAYER_LIST, vanilla -> (ctx, tick) -> tweak(TabListTweak.class, vanilla, ctx, tick));

        // Save the active profile and undo option changes (zoom, damage tilt...) when the game closes.
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            config.save(config.active());
            for (Module m : modules.all()) if (m.isEnabled()) m.onDisable();
        });

        // Block-entity cache for the finders (no per-frame chunk scanning).
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, w) -> {
            for (ESPModule f : finders()) f.track(be); });
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, w) -> {
            for (ESPModule f : finders()) f.untrack(be); });
        ClientPlayConnectionEvents.DISCONNECT.register((h, mc) -> { for (ESPModule f : finders()) f.clear(); for (Module m : modules.all()) m.onDisconnect(); });

        // Criticals needs to act just before an attack is sent.
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hit) -> {
            Criticals cr = modules.get(Criticals.class);
            if (cr != null && player == class_310.method_1551().field_1724) cr.onAttack(entity);
            return class_1269.field_5811;
        });

        LOGGER.info("Monolith ready: {} modules", modules.all().size());
    }

    private static <T extends ElementTweak> void tweak(Class<T> type, HudElement vanilla, class_332 ctx, class_9779 tick) {
        T m = modules.get(type);
        if (m != null && m.isEnabled()) m.apply(ctx, vanilla, tick); else vanilla.render(ctx, tick);
    }

    private static java.util.List<ESPModule> finders() {
        return modules.all().stream().filter(m -> m instanceof ESPModule).map(m -> (ESPModule) m).toList();
    }
}
