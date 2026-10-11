package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.class_332;
import net.minecraft.class_9779;

/** Scales, moves or hides a vanilla HUD element by wrapping it (no mixins). Subclasses choose the scaling pivot. */
public abstract class ElementTweak extends Module {
    public final NumberSetting scale = add(new NumberSetting("Scale", 1.0, 0.5, 2.0, 0.05));
    public final NumberSetting offX = add(new NumberSetting("Offset X", 0, -300, 300, 1));
    public final NumberSetting offY = add(new NumberSetting("Offset Y", 0, -300, 300, 1));
    public final BoolSetting hide = add(new BoolSetting("Hide", false));

    protected ElementTweak(String name, String desc) { super(name, desc, Category.HUD); }
    protected abstract float pivotX(int w, int h);
    protected abstract float pivotY(int w, int h);

    public void apply(class_332 c, HudElement vanilla, class_9779 tick) {
        if (hide.get()) return;
        int w = c.method_51421(), h = c.method_51443();
        float px = pivotX(w, h), py = pivotY(w, h), s = scale.asFloat();
        var m = c.method_51448();
        m.pushMatrix();
        m.translate(offX.asFloat() + px, offY.asFloat() + py);
        m.scale(s, s);
        m.translate(-px, -py);
        vanilla.render(c, tick);
        m.popMatrix();
    }
}
