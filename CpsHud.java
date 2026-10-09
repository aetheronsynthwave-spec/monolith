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
public class CpsHud extends HudModule {
    private final ArrayDeque<Long> left = new ArrayDeque<>(), right = new ArrayDeque<>();
    private boolean ld, rd;
    public CpsHud() { super("CPS", "Clicks per second.", 0.01, 0.22); }
    /** Polled each tick (no mixin needed). */
    @Override public void onTick() {
        long h = mc.getWindow().getHandle(); long now = System.currentTimeMillis();
        boolean l = org.lwjgl.glfw.GLFW.glfwGetMouseButton(h, 0) == 1, r = org.lwjgl.glfw.GLFW.glfwGetMouseButton(h, 1) == 1;
        if (l && !ld) left.add(now); if (r && !rd) right.add(now); ld = l; rd = r;
        while (!left.isEmpty() && now - left.peek() > 1000) left.poll();
        while (!right.isEmpty() && now - right.peek() > 1000) right.poll();
    }
    @Override protected void draw(DrawContext c) { line(c, "CPS", left.size() + " | " + right.size()); }
}
