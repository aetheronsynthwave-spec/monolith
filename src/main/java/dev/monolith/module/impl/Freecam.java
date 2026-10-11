package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

public class Freecam extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed (blocks/s)", 14, 1, 80, 1));
    public final NumberSetting sprintMult = add(new NumberSetting("Sprint Multiplier", 2.0, 1, 5, 0.1));
    public final NumberSetting vertical = add(new NumberSetting("Vertical Speed %", 100, 25, 200, 5));
    public final NumberSetting smoothing = add(new NumberSetting("Smoothing (0 = off)", 14, 0, 40, 1));
    public final NumberSetting returnMs = add(new NumberSetting("Return Time (ms)", 250, 0, 1000, 25));
    public final BoolSetting offOnHurt = add(new BoolSetting("Disable On Damage", true));

    private ArmorStandEntity cam;
    private PlayerEntity bound;
    private double ex, ey, ez, vx, vy, vz;
    private float camYaw, camPitch, realYaw, realPitch;
    private long last = System.nanoTime();
    private boolean returning;
    private double sx, sy, sz; private float sYaw, sPitch; private float t;

    public Freecam() { super("Freecam", "Fly the camera while your body stays still.", Category.RENDER); }

    private boolean init() {
        if (mc.player == null || mc.world == null) return false;
        bound = mc.player;
        cam = new ArmorStandEntity(EntityType.ARMOR_STAND, mc.world);
        ex = mc.player.getX(); ey = mc.player.getEyeY(); ez = mc.player.getZ();
        camYaw = realYaw = mc.player.getYaw(); camPitch = realPitch = mc.player.getPitch();
        vx = vy = vz = 0; returning = false;
        place();
        mc.setCameraEntity(cam);
        return true;
    }

    private void place() {
        cam.refreshPositionAndAngles(ex, ey, ez, camYaw, camPitch);
        double off = cam.getEyeY() - cam.getY();
        cam.refreshPositionAndAngles(ex, ey - off, ez, camYaw, camPitch);
    }

    @Override public void onEnable() { init(); }

    @Override public void onDisable() {
        KeyBinding.updatePressedStates();
        if (mc.player == null || cam == null || returnMs.get() <= 0) { finish(); return; }
        mc.player.setYaw(realYaw); mc.player.setPitch(realPitch);
        sx = ex; sy = ey; sz = ez; sYaw = camYaw; sPitch = camPitch; t = 0; returning = true;
        last = System.nanoTime();
    }

    private void finish() {
        returning = false; cam = null; bound = null;
        if (mc.player != null) mc.setCameraEntity(mc.player);
    }

    @Override public void onDisconnect() { returning = false; cam = null; bound = null; setEnabled(false); }

    @Override public void onPreTick() {
        if (mc.player == null) return;
        var o = mc.options;
        for (KeyBinding k : new KeyBinding[]{o.forwardKey, o.backKey, o.leftKey, o.rightKey, o.jumpKey, o.sneakKey, o.sprintKey, o.attackKey, o.useKey}) k.setPressed(false);
        mc.player.setYaw(realYaw); mc.player.setPitch(realPitch);
    }

    @Override public void onFrame(float delta) {
        if (mc.player == null || mc.world == null) return;
        if (cam == null && !init()) return;
        if (mc.player != bound || cam.getEntityWorld() != mc.world) { setEnabled(false); return; }
        if (offOnHurt.get() && mc.player.hurtTime > 0) { setEnabled(false); return; }

        long now = System.nanoTime(); float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;

        camYaw += mc.player.getYaw() - realYaw;
        camPitch = MathHelper.clamp(camPitch + (mc.player.getPitch() - realPitch), -90f, 90f);
        mc.player.setYaw(realYaw); mc.player.setPitch(realPitch);

        double tx = 0, ty = 0, tz = 0;
        if (mc.currentScreen == null) {
            long h = mc.getWindow().getHandle();
            var o = mc.options;
            double f = axis(h, o.forwardKey, o.backKey), s = axis(h, o.leftKey, o.rightKey), u = axis(h, o.jumpKey, o.sneakKey);
            double yaw = Math.toRadians(camYaw), pitch = Math.toRadians(camPitch);
            double dx = (-Math.sin(yaw) * Math.cos(pitch)) * f + Math.cos(yaw) * s;
            double dy = -Math.sin(pitch) * f;
            double dz = (Math.cos(yaw) * Math.cos(pitch)) * f + Math.sin(yaw) * s;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 1e-6) { dx /= len; dy /= len; dz /= len; }
            double v = speed.get() * (down(h, o.sprintKey) ? sprintMult.get() : 1.0);
            tx = dx * v; ty = dy * v + u * v * vertical.get() / 100.0; tz = dz * v;
        }
        double a = smoothing.get() <= 0 ? 1 : 1 - Math.exp(-smoothing.get() * dt);
        vx += (tx - vx) * a; vy += (ty - vy) * a; vz += (tz - vz) * a;
        ex += vx * dt; ey += vy * dt; ez += vz * dt;

        place();
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
    }

    public void returnFrame() {
        if (!returning) return;
        if (cam == null || mc.player == null || mc.world == null || mc.player != bound) { finish(); return; }
        long now = System.nanoTime(); float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;
        t = Math.min(1f, t + dt * 1000f / returnMs.get().floatValue());
        float e = t * t * (3 - 2 * t);
        ex = sx + (mc.player.getX() - sx) * e;
        ey = sy + (mc.player.getEyeY() - sy) * e;
        ez = sz + (mc.player.getZ() - sz) * e;
        camYaw = sYaw + MathHelper.wrapDegrees(mc.player.getYaw() - sYaw) * e;
        camPitch = sPitch + (mc.player.getPitch() - sPitch) * e;
        place();
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
        if (t >= 1f) finish();
    }

    private static double axis(long w, KeyBinding pos, KeyBinding neg) { return (down(w, pos) ? 1 : 0) - (down(w, neg) ? 1 : 0); }

    private static boolean down(long w, KeyBinding k) {
        InputUtil.Key key = KeyBindingHelper.getBoundKeyOf(k);
        int c = key.getCode();
        if (c < 0) return false;
        return key.getCategory() == InputUtil.Type.MOUSE
            ? GLFW.glfwGetMouseButton(w, c) == GLFW.GLFW_PRESS
            : GLFW.glfwGetKey(w, c) == GLFW.GLFW_PRESS;
    }
}
