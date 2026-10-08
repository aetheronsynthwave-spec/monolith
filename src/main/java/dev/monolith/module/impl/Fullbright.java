package dev.monolith.module.impl;
import dev.monolith.module.Module;
import dev.monolith.module.*;
import net.minecraft.entity.effect.*;
/** Client-side night vision. (A gamma mixin would avoid the effect icon; this needs no mixin.) */
public class Fullbright extends Module {
    public Fullbright() { super("Fullbright", "See clearly in the dark.", Category.RENDER); }
    @Override public void onTick() {
        if (mc.player.hasStatusEffect(StatusEffects.NIGHT_VISION)) {
            StatusEffectInstance i = mc.player.getStatusEffect(StatusEffects.NIGHT_VISION);
            if (i.getDuration() > 400) return;
        }
        mc.player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, 1000, 0, false, false, false));
    }
    @Override public void onDisable() { if (mc.player != null) mc.player.removeStatusEffect(StatusEffects.NIGHT_VISION); }
}
