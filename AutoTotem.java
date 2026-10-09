package dev.monolith.module.impl;

import dev.monolith.module.Category;
import dev.monolith.module.Module;
import dev.monolith.setting.*;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;

/** Keeps a Totem of Undying in your off-hand by swapping one in from your inventory. */
public class AutoTotem extends Module {
    public final NumberSetting delay = add(new NumberSetting("Delay (ticks)", 1, 0, 10, 1));
    public final NumberSetting belowHealth = add(new NumberSetting("Only Below Health", 20, 2, 20, 1));
    private int cooldown;

    public AutoTotem() {
        super("AutoTotem", "Refills your off-hand with a totem.", Category.COMBAT);
        belowHealth.tooltip = "20 = always keep a totem in the off-hand.";
    }

    @Override public void onTick() {
        if (mc.interactionManager == null) return;
        if (cooldown > 0) { cooldown--; return; }
        if (mc.currentScreen != null && !(mc.currentScreen instanceof InventoryScreen)) return;   // not while another container is open
        if (mc.player.getOffHandStack().isOf(Items.TOTEM_OF_UNDYING)) return;
        if (mc.player.getHealth() + mc.player.getAbsorptionAmount() > belowHealth.get()) return;
        int slot = -1;
        for (int i = 0; i < 36; i++) if (mc.player.getInventory().getStack(i).isOf(Items.TOTEM_OF_UNDYING)) { slot = i; break; }   // hotbar first
        if (slot < 0) return;
        int handlerSlot = slot < 9 ? 36 + slot : slot;           // inventory index -> player screen handler slot
        mc.interactionManager.clickSlot(mc.player.playerScreenHandler.syncId, handlerSlot, 40, SlotActionType.SWAP, mc.player);   // button 40 = swap with off-hand
        cooldown = delay.asInt();
    }
}
