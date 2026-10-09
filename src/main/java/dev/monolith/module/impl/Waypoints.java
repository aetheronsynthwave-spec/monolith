package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import java.util.*;

/**
 * Waypoints saved inside the profile (the hidden "Data" setting). Press the Add key to drop one at your feet,
 * the Remove key to delete the nearest one within 8 blocks. Rebind both in the GUI.
 */
public class Waypoints extends Module {
    private record WP(String name, String dim, int x, int y, int z) {}
    public final KeybindSetting addKey = add(new KeybindSetting("Add Here Key", GLFW.GLFW_KEY_B));
    public final KeybindSetting removeKey = add(new KeybindSetting("Remove Nearest Key", GLFW.GLFW_KEY_N));
    public final NumberSetting maxRange = add(new NumberSetting("Max Range (0 = any)", 0, 0, 5000, 50));
    public final BoolSetting showDistance = add(new BoolSetting("Show Distance", true));
    public final ColorSetting color = add(new ColorSetting("Color", 0xFFFFFFFF));
    private final StringSetting data = add(new StringSetting("Data", ""));
    private final List<WP> list = new ArrayList<>();
    private String parsed = null;
    private boolean addDown, removeDown;
    private final double[] pt = new double[3];

    public Waypoints() { super("Waypoints", "Markers you can see from anywhere.", Category.WORLD); data.visibleWhen(() -> false); }

    private void sync() {
        String raw = data.get();
        if (raw.equals(parsed)) return;
        parsed = raw; list.clear();
        for (String e : raw.split(";")) {
            String[] f = e.split("\\|");
            if (f.length != 5) continue;
            try { list.add(new WP(f[0], f[1], Integer.parseInt(f[2]), Integer.parseInt(f[3]), Integer.parseInt(f[4]))); } catch (NumberFormatException ignored) {}
        }
    }
    private void save() {
        StringBuilder sb = new StringBuilder();
        for (WP w : list) sb.append(w.name).append('|').append(w.dim).append('|').append(w.x).append('|').append(w.y).append('|').append(w.z).append(';');
        data.set(sb.toString()); parsed = data.get();
    }
    private String dim() { return mc.world.getRegistryKey().getValue().toString(); }
    private static boolean down(int key) { return key > 0 && key <= GLFW.GLFW_KEY_LAST && GLFW.glfwGetKey(net.minecraft.client.MinecraftClient.getInstance().getWindow().getHandle(), key) == GLFW.GLFW_PRESS; }

    @Override public void onTick() {
        sync();
        if (mc.currentScreen != null) return;
        boolean a = down(addKey.get()), r = down(removeKey.get());
        if (a && !addDown) {
            list.add(new WP("Waypoint " + (list.size() + 1), dim(), (int) Math.floor(mc.player.getX()), (int) Math.floor(mc.player.getY()), (int) Math.floor(mc.player.getZ())));
            save(); mc.player.sendMessage(Text.literal("Waypoint added"), true);
        }
        if (r && !removeDown) {
            WP best = null; double bd = 64;
            for (WP w : list) { if (!w.dim.equals(dim())) continue;
                double dx = w.x + 0.5 - mc.player.getX(), dy = w.y - mc.player.getY(), dz = w.z + 0.5 - mc.player.getZ(); double d = dx * dx + dy * dy + dz * dz;
                if (d < bd) { bd = d; best = w; } }
            if (best != null) { list.remove(best); save(); mc.player.sendMessage(Text.literal("Waypoint removed"), true); }
        }
        addDown = a; removeDown = r;
    }

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid) return;
        sync();
        String dim = dim(); double max = maxRange.get(); int col = color.get();
        for (WP w : list) {
            if (!w.dim.equals(dim)) continue;
            double dx = w.x + 0.5 - Projector.cx, dy = w.y + 0.5 - Projector.cy, dz = w.z + 0.5 - Projector.cz;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (max > 0 && dist > max) continue;
            if (!Projector.point(w.x + 0.5, w.y + 1.0, w.z + 0.5, pt)) continue;
            float x = (float) pt[0], y = (float) pt[1];
            if (x < -20 || x > Projector.W + 20 || y < -20 || y > Projector.H + 20) continue;
            Render2D.rounded(c, x - 3, y - 3, 7, 7, 3, 0xFF000000);
            Render2D.rounded(c, x - 2, y - 2, 5, 5, 2, col);
            Render2D.centered(c, showDistance.get() ? w.name + "  " + (int) dist + "m" : w.name, x, y - 14, col);
        }
    }
}
