package dev.monolith.hud;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.*;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.*;
public class PingHud extends HudModule {
    public PingHud() { super("Ping", "Latency to the server.", 0.01, 0.07); }
    @Override protected void draw(DrawContext c) {
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null) { var e = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()); if (e != null) ping = e.getLatency(); }
        line(c, "PING", ping + "ms");
    }
}
