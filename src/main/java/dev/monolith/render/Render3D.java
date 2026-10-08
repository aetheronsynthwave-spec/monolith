package dev.monolith.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.*;

/**
 * 3D primitives. ALL world-space drawing goes through this class so that when Mojang/Fabric change
 * the render pipeline again, only this file needs touching.
 *
 * !! Highest compile-risk file in the project. The 1.21.6-1.21.11 rendering rewrite moved to RenderPipeline/RenderLayer.
 * !! The WALL_* layers below must be built from a pipeline with depth test disabled; see buildLayers().
 */
public final class Render3D {
    private Render3D() {}
    /** Set each frame by Monolith's world-render hook. */
    public static VertexConsumerProvider consumers;

    public static int alpha(int c, float a) { return Render2D.withAlpha(c, Math.max(0, Math.min(1, a))); }

    public static void box(MatrixStack m, Box b, int argb, boolean filled, boolean walls) { box(m, b, argb, filled, walls, 1.5f); }

    public static void box(MatrixStack m, Box b, int argb, boolean filled, boolean walls, float width) {
        if (consumers == null || (argb >>> 24) == 0 || (argb & 0xFFFFFF) == 0 && (argb >>> 24) == 0) return;
        float a = (argb >>> 24) / 255f, r = ((argb >> 16) & 255) / 255f, g = ((argb >> 8) & 255) / 255f, bl = (argb & 255) / 255f;
        Vec3d cam = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        Box o = b.offset(-cam.x, -cam.y, -cam.z);
        RenderLayer layer = filled ? (walls ? Layers.FILL_WALLS : RenderLayer.getDebugFilledBox())
                                   : (walls ? Layers.LINE_WALLS : RenderLayer.getLines());
        VertexConsumer vc = consumers.getBuffer(layer);
        if (filled) VertexRendering.drawFilledBox(m, vc, o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ, r, g, bl, a);
        else VertexRendering.drawBox(m, vc, o.minX, o.minY, o.minZ, o.maxX, o.maxY, o.maxZ, r, g, bl, a);
    }

    /** Camera-facing text label (distance readout). Uses vanilla text rendering with see-through layer. */
    public static void label(MatrixStack m, Vec3d pos, String text, int argb) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (consumers == null) return;
        Camera cam = mc.gameRenderer.getCamera();
        m.push();
        m.translate(pos.x - cam.getPos().x, pos.y - cam.getPos().y, pos.z - cam.getPos().z);
        m.multiply(cam.getRotation());
        float s = 0.025f; m.scale(s, -s, s);
        float w = mc.textRenderer.getWidth(text);
        mc.textRenderer.draw(text, -w / 2f, 0, argb, false, m.peek().getPositionMatrix(), consumers,
            net.minecraft.client.font.TextRenderer.TextLayerType.SEE_THROUGH, 0x66000000, 0xF000F0);
        m.pop();
    }

    /** Through-wall layers. TODO(verify on 1.21.11): construct with RenderPipeline.builder(...).withDepthTestFunction(NO_DEPTH_TEST)
     *  and RenderLayer.of(...) (may need an access widener for RenderLayer.of / MultiPhaseParameters). */
    private static final class Layers {
        static final RenderLayer FILL_WALLS = RenderLayer.getDebugFilledBox();
        static final RenderLayer LINE_WALLS = RenderLayer.getLines();
    }
}
