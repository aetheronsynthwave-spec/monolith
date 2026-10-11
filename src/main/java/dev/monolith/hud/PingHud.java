package dev.monolith.hud;
import net.minecraft.util.math.*;
import net.minecraft.class_332;
import net.minecraft.entity.*;
import net.minecraft.entity.effect.*;
import net.minecraft.entity.player.*;
import net.minecraft.item.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import java.util.*;
public class PingHud extends HudModule {
    public PingHud() { super("Ping", "Latency to the server.", 0.01, 0.07); }
    @Override protected void draw(class_332 c) {
        int ping = 0;
        if (mc.field_1724 != null && mc.method_1562() != null) { var e = mc.method_1562().method_2871(mc.field_1724.method_5667()); if (e != null) ping = e.method_2959(); }
        line(c, "PING", ping + "ms");
    }
}
