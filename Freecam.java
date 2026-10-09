package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

/**
 * Detached camera. A hidden stand-in entity becomes the camera while your real body stays put:
 * movement/attack keys are blanked for the body and your mouse + keys fly the camera through blocks.
 */
public class Freecam extends Module {
    public final NumberSetting speed = add(new NumberSetting("Speed (blocks/s)", 14, 2, 60, 1));
    private ArmorStandEntity cam;
    private double ex, ey, ez;
    private float camYaw, camPitch, realYaw, realPitch;
    private long last = System.nanoTime();

    public Freecam() { super("Freecam", "Fly the camera while your body stays still.", Category.RENDER); }

    private void init() {
        if (mc.player == null || mc.world == null) return;
        cam = new ArmorStandEntity(EntityType.ARMOR_STAND, mc.world);
        Vec3d eye = mc.player.getEyePos();
        ex = eye.x; ey = eye.y; ez = eye.z;
        camYaw = realYaw = mc.player.getYaw(); camPitch = realPitch = mc.player.getPitch();
        place();
        mc.setCameraEntity(cam);
    }

    private void place() {
        cam.refreshPositionAndAngles(ex, ey, ez, camYaw, camPitch);
        double eyeOffset = cam.getCameraPosVec(1f).y - cam.getY();           // put the camera's EYES at (ex, ey, ez)
        cam.refreshPositionAndAngles(ex, ey - eyeOffset, ez, camYaw, camPitch);
    }

    @Override public void onEnable() { init(); }

    @Override public void onDisable() {
        if (mc.player != null) { mc.setCameraEntity(mc.player); mc.player.setYaw(realYaw); mc.player.setPitch(realPitch); }
        cam = null;
        KeyBinding.updatePressedStates();                                    // hand the keys back to the real keyboard state
    }
    @Override public void onDisconnect() { cam = null; setEnabled(false); }

    /** Runs before the body reads input: blank the keys and undo mouse turning so the body does nothing. */
    @Override public void onPreTick() {
        var o = mc.options;
        for (KeyBinding k : new KeyBinding[]{o.forwardKey, o.backKey, o.leftKey, o.rightKey, o.jumpKey, o.sneakKey, o.sprintKey, o.attackKey, o.useKey}) k.setPressed(false);
        mc.player.setYaw(realYaw); mc.player.setPitch(realPitch);
    }

    @Override public void onFrame(float delta) {
        if (mc.player == null || mc.world == null) return;
        if (cam == null) { init(); if (cam == null) return; }
        long now = System.nanoTime(); float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;

        // The mouse still turns the real player; move that turn onto the camera and put the body back.
        camYaw += mc.player.getYaw() - realYaw;
        camPitch = Math.max(-90f, Math.min(90f, camPitch + (mc.player.getPitch() - realPitch)));
        mc.player.setYaw(realYaw); mc.player.setPitch(realPitch);

        if (mc.currentScreen == null) {
            long h = mc.getWindow().getHandle();
            double f = axis(h, mc.options.forwardKey, mc.options.backKey), s = axis(h, mc.options.leftKey, mc.options.rightKey), u = axis(h, mc.options.jumpKey, mc.options.sneakKey);
            double yaw = Math.toRadians(camYaw), pitch = Math.toRadians(camPitch);
            double fx = -Math.sin(yaw) * Math.cos(pitch), fy = -Math.sin(pitch), fz = Math.cos(yaw) * Math.cos(pitch);
            double lx = Math.cos(yaw), lz = Math.sin(yaw);
            double dx = fx * f + lx * s, dy = fy * f + u, dz = fz * f + lz * s;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len > 1e-6) { double k = speed.get() * dt / Math.max(1.0, len); ex += dx * k; ey += dy * k; ez += dz * k; }
        }
        place();
        if (mc.getCameraEntity() != cam) mc.setCameraEntity(cam);
    }

    /** Reads the physical key state (the game's own key state is blanked while Freecam is on). */
    private static double axis(long window, KeyBinding pos, KeyBinding neg) {
        return (down(window, pos) ? 1 : 0) - (down(window, neg) ? 1 : 0);
    }
    private static boolean down(long window, KeyBinding k) {
        int code = KeyBindingHelper.getBoundKeyOf(k).getCode();
        return code > 0 && GLFW.glfwGetKey(window, code) == GLFW.GLFW_PRESS;
    }
}
