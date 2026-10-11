package dev.monolith.mixin;

import dev.monolith.module.impl.Zoom;
import net.minecraft.class_1297;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Scales mouse look while Zoom is active, instead of rewriting the saved sensitivity option.
 * method_5872 = Entity#changeLookDirection(double, double). require = 0: if a future Minecraft version renames it,
 * this injection is skipped (zoom just won't slow the mouse) instead of crashing the game on startup.
 */
@Mixin(class_1297.class)
public abstract class EntityLookMixin {
    @ModifyVariable(method = "method_5872", at = @At("HEAD"), argsOnly = true, ordinal = 0, require = 0)
    private double monolith$lookX(double dx) { return Zoom.scaleLook((class_1297) (Object) this, dx); }

    @ModifyVariable(method = "method_5872", at = @At("HEAD"), argsOnly = true, ordinal = 1, require = 0)
    private double monolith$lookY(double dy) { return Zoom.scaleLook((class_1297) (Object) this, dy); }
}
