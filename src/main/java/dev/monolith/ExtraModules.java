package dev.monolith;

import dev.monolith.hud.Radar;
import dev.monolith.module.impl.*;
import net.fabricmc.api.ClientModInitializer;

/**
 * Second client entrypoint (runs right after Monolith): registers the modules added in 1.0.3 without touching ModuleManager,
 * then reloads the saved profile so their settings and on/off state are restored.
 */
public class ExtraModules implements ClientModInitializer {
    @Override public void onInitializeClient() {
        var m = Monolith.modules();
        ESPStyle style = new ESPStyle();
        m.register(style);
        style.setEnabled(true);                    // filled boxes by default; a saved profile can switch it off
        m.register(new Tracers());
        m.register(new BlockHighlight());
        m.register(new ChunkBorders());
        m.register(new Breadcrumbs());
        m.register(new Radar());
        Monolith.config().loadActive();
        Monolith.LOGGER.info("Monolith 1.0.3 extras ready: {} modules", m.all().size());
    }
}
