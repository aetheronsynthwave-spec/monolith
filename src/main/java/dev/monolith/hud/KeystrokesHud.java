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
public class KeystrokesHud extends HudModule {
    public KeystrokesHud() { super("Keystrokes", "WASD, space and mouse overlay.", 0.01, 0.60); }
    private void key(class_332 c, String l, boolean down, int x, int y, int w, int h) {
        Render2D.roundedBordered(c, x, y, w, h, 3, down ? 0xE6FFFFFF : Theme.HUD_BG, down ? 0xFFFFFFFF : Theme.BORDER);
        Render2D.centered(c, l, x + w / 2f, y + (h - 8) / 2f, down ? 0xFF000000 : Theme.TEXT);
    }
    @Override protected void draw(class_332 c) {
        contentW = 59; contentH = 76; var o = mc.field_1690;
        key(c, "W", o.field_1894.method_1434(), 20, 0, 19, 19);
        key(c, "A", o.field_1913.method_1434(), 0, 20, 19, 19);
        key(c, "S", o.field_1881.method_1434(), 20, 20, 19, 19);
        key(c, "D", o.field_1849.method_1434(), 40, 20, 19, 19);
        key(c, "LMB", o.field_1886.method_1434(), 0, 40, 29, 17);
        key(c, "RMB", o.field_1904.method_1434(), 30, 40, 29, 17);
        key(c, "SPACE", o.field_1903.method_1434(), 0, 58, 59, 17);
    }
}
