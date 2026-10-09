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
public class KeystrokesHud extends HudModule {
    public KeystrokesHud() { super("Keystrokes", "WASD, space and mouse overlay.", 0.01, 0.60); }
    private void key(DrawContext c, String l, boolean down, int x, int y, int w, int h) {
        Render2D.roundedBordered(c, x, y, w, h, 3, down ? 0xE6FFFFFF : Theme.HUD_BG, down ? 0xFFFFFFFF : Theme.BORDER);
        Render2D.centered(c, l, x + w / 2f, y + (h - 8) / 2f, down ? 0xFF000000 : Theme.TEXT);
    }
    @Override protected void draw(DrawContext c) {
        contentW = 59; contentH = 76; var o = mc.options;
        key(c, "W", o.forwardKey.isPressed(), 20, 0, 19, 19);
        key(c, "A", o.leftKey.isPressed(), 0, 20, 19, 19);
        key(c, "S", o.backKey.isPressed(), 20, 20, 19, 19);
        key(c, "D", o.rightKey.isPressed(), 40, 20, 19, 19);
        key(c, "LMB", o.attackKey.isPressed(), 0, 40, 29, 17);
        key(c, "RMB", o.useKey.isPressed(), 30, 40, 29, 17);
        key(c, "SPACE", o.jumpKey.isPressed(), 0, 58, 59, 17);
    }
}
