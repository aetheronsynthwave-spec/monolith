package dev.monolith.module.impl;

import dev.monolith.module.*;
import dev.monolith.render.*;
import dev.monolith.setting.*;
import net.minecraft.client.gui.DrawContext;
import java.util.Random;

public class ParticleAura extends Module {
    public enum Mode { ORBIT, RISING, TRAIL }
    public final EnumSetting<Mode> mode = add(new EnumSetting<>("Mode", Mode.ORBIT));
    public final NumberSetting count = add(new NumberSetting("Particles", 60, 10, 200, 5));
    public final NumberSetting size = add(new NumberSetting("Size", 1.0, 0.3, 3, 0.1));
    public final NumberSetting radius = add(new NumberSetting("Radius", 1.1, 0.6, 3, 0.1));
    public final NumberSetting glow = add(new NumberSetting("Glow Layers", 2, 0, 3, 1));
    public final NumberSetting lifeSec = add(new NumberSetting("Lifetime (s)", 2.0, 0.5, 5, 0.1));
    public final ColorSetting colorA = add(new ColorSetting("Color A", 0xFF8AB4FF));
    public final ColorSetting colorB = add(new ColorSetting("Color B", 0xFFC58BFF));
    public final BoolSetting firstPerson = add(new BoolSetting("Show In First Person", true));

    private static final int MAX = 200;
    private final double[] x = new double[MAX], y = new double[MAX], z = new double[MAX];
    private final double[] vx = new double[MAX], vy = new double[MAX], vz = new double[MAX];
    private final float[] life = new float[MAX], maxLife = new float[MAX], mix = new float[MAX];
    private final Random rng = new Random();
    private final double[] out = new double[3];
    private long last = System.nanoTime();
    private double lastPx, lastPz;

    public ParticleAura() { super("Particle Aura", "Floating glowing particles around you.", Category.RENDER); }

    @Override public void onDisable() { for (int i = 0; i < MAX; i++) life[i] = 0; }

    private void spawn(int i) {
        var p = mc.player;
        double a = rng.nextDouble() * Math.PI * 2, r = radius.get() * (0.6 + rng.nextDouble() * 0.4);
        switch (mode.get()) {
            case TRAIL -> { x[i] = p.getX() + (rng.nextDouble() - .5) * .4; y[i] = p.getY() + rng.nextDouble() * 1.6; z[i] = p.getZ() + (rng.nextDouble() - .5) * .4;
                vx[i] = (rng.nextDouble() - .5) * .2; vy[i] = .05 + rng.nextDouble() * .15; vz[i] = (rng.nextDouble() - .5) * .2; }
            case RISING -> { x[i] = p.getX() + Math.cos(a) * r; y[i] = p.getY() + rng.nextDouble() * .3; z[i] = p.getZ() + Math.sin(a) * r;
                vx[i] = 0; vy[i] = .4 + rng.nextDouble() * .5; vz[i] = 0; }
            default -> { x[i] = p.getX() + Math.cos(a) * r; y[i] = p.getY() + rng.nextDouble() * 2.0; z[i] = p.getZ() + Math.sin(a) * r;
                vx[i] = 0; vy[i] = (rng.nextDouble() - .5) * .15; vz[i] = 0; }
        }
        maxLife[i] = life[i] = lifeSec.get().floatValue() * (0.6f + rng.nextFloat() * 0.4f);
        mix[i] = rng.nextFloat();
    }

    @Override public void onFrame(float delta) {
        if (mc.player == null) return;
        long now = System.nanoTime(); float dt = Math.min(0.1f, (now - last) / 1e9f); last = now;
        boolean moving = Math.abs(mc.player.getX() - lastPx) + Math.abs(mc.player.getZ() - lastPz) > 0.02;
        lastPx = mc.player.getX(); lastPz = mc.player.getZ();
        int n = Math.min(MAX, count.asInt());
        int spawnBudget = Math.max(1, (int) (n * dt / (lifeSec.get() * 0.7)) + 1);
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0) {
                if (i < n && spawnBudget > 0 && (mode.get() != Mode.TRAIL || moving)) { spawn(i); spawnBudget--; }
                continue;
            }
            life[i] -= dt;
            if (mode.get() == Mode.ORBIT) {
                double dx = x[i] - mc.player.getX(), dz = z[i] - mc.player.getZ(), w = 1.2 * dt;
                double c = Math.cos(w), s = Math.sin(w);
                x[i] = mc.player.getX() + dx * c - dz * s; z[i] = mc.player.getZ() + dx * s + dz * c;
            }
            x[i] += vx[i] * dt; y[i] += vy[i] * dt; z[i] += vz[i] * dt;
        }
    }

    @Override public void onHud(DrawContext c, float delta) {
        if (!Projector.valid || mc.player == null) return;
        if (!firstPerson.get() && mc.options.getPerspective().isFirstPerson()) return;
        int layers = glow.asInt();
        for (int i = 0; i < MAX; i++) {
            if (life[i] <= 0 || !Projector.point(x[i], y[i], z[i], out)) continue;
            float f = life[i] / maxLife[i];
            float fade = Math.min(1f, Math.min(f * 4f, (1f - f) * 6f + 0.15f));
            float s = (float) Math.max(1.0, Math.min(8.0, size.get() * 0.12 * Projector.focal / out[2]));
            int col = Render2D.lerpColor(colorA.get(), colorB.get(), mix[i]);
            float cx = (float) out[0], cy = (float) out[1];
            for (int l = layers; l >= 1; l--) {
                float gs = s * (1f + l * 1.2f);
                Render2D.rect(c, cx - gs / 2, cy - gs / 2, gs, gs, Render2D.withAlpha(col, fade * 0.10f * (4 - l)));
            }
            Render2D.rect(c, cx - s / 2, cy - s / 2, s, s, Render2D.withAlpha(col, fade));
            if (s >= 3) Render2D.rect(c, cx - s / 6, cy - s / 6, s / 3, s / 3, Render2D.withAlpha(0xFFFFFFFF, fade * 0.9f));
        }
    }
}
