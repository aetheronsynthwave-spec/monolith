package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;

/** Sends a tiny fake hop just before each hit so the server counts it as a critical. Strict anti-cheats will flag this. */
public class Criticals extends Module {
    public Criticals() { super("Criticals", "Hits count as critical hits while on the ground.", Category.COMBAT); }

    public void onAttack(Entity target) {
        if (!isEnabled() || mc.player == null || !(target instanceof LivingEntity)) return;
        if (!mc.player.isOnGround() || mc.player.isTouchingWater() || mc.player.isInLava() || mc.player.isClimbing()) return;
        var net = mc.getNetworkHandler();
        if (net == null) return;
        double x = mc.player.getX(), y = mc.player.getY(), z = mc.player.getZ();
        net.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y + 0.0625, z, false, false));
        net.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, false, false));
    }
}
