package io.github.scuba10steve.s3.advanced.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.scuba10steve.s3.advanced.blockentity.TankBlockEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;
import net.neoforged.neoforge.client.extensions.common.IClientFluidTypeExtensions;
import net.neoforged.neoforge.fluids.FluidStack;

public class TankBlockEntityRenderer implements BlockEntityRenderer<TankBlockEntity> {

    // Inset past the 1-pixel frame so the fluid never z-fights the glass.
    private static final float MIN = 1.1f / 16f;
    private static final float MAX = 14.9f / 16f;

    public TankBlockEntityRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(TankBlockEntity be, float partialTick, PoseStack pose, MultiBufferSource buffers,
                       int light, int overlay) {
        FluidStack fluid = be.getTank().getFluid();
        int capacity = be.getTank().getCapacity();
        if (fluid.isEmpty() || capacity <= 0) {
            return;
        }
        float fill = Math.min(1f, fluid.getAmount() / (float) capacity);
        IClientFluidTypeExtensions ext = IClientFluidTypeExtensions.of(fluid.getFluid());
        TextureAtlasSprite sprite = Minecraft.getInstance()
            .getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(ext.getStillTexture(fluid));
        int color = ext.getTintColor(fluid);
        if ((color >>> 24) == 0) {
            color |= 0xFF000000;
        }

        float height = (MAX - MIN) * fill;
        boolean gas = fluid.getFluid().getFluidType().isLighterThanAir();
        float y0 = gas ? MAX - height : MIN;
        float y1 = gas ? MAX : MIN + height;

        VertexConsumer vc = buffers.getBuffer(Sheets.translucentCullBlockSheet());
        PoseStack.Pose p = pose.last();
        float u0 = sprite.getU(MIN);
        float u1 = sprite.getU(MAX);
        float v0 = sprite.getV(1f - (y1 - MIN));
        float v1 = sprite.getV(1f - (y0 - MIN));
        float vTop0 = sprite.getV(MIN);
        float vTop1 = sprite.getV(MAX);

        // top
        quad(vc, p, color, light, overlay, MIN, y1, MIN, MIN, y1, MAX, MAX, y1, MAX, MAX, y1, MIN, u0, vTop0, u1, vTop1, 0, 1, 0);
        // bottom
        quad(vc, p, color, light, overlay, MIN, y0, MAX, MIN, y0, MIN, MAX, y0, MIN, MAX, y0, MAX, u0, vTop0, u1, vTop1, 0, -1, 0);
        // north (z = MIN)
        quad(vc, p, color, light, overlay, MAX, y1, MIN, MAX, y0, MIN, MIN, y0, MIN, MIN, y1, MIN, u0, v0, u1, v1, 0, 0, -1);
        // south (z = MAX)
        quad(vc, p, color, light, overlay, MIN, y1, MAX, MIN, y0, MAX, MAX, y0, MAX, MAX, y1, MAX, u0, v0, u1, v1, 0, 0, 1);
        // west (x = MIN)
        quad(vc, p, color, light, overlay, MIN, y1, MIN, MIN, y0, MIN, MIN, y0, MAX, MIN, y1, MAX, u0, v0, u1, v1, -1, 0, 0);
        // east (x = MAX)
        quad(vc, p, color, light, overlay, MAX, y1, MAX, MAX, y0, MAX, MAX, y0, MIN, MAX, y1, MIN, u0, v0, u1, v1, 1, 0, 0);
    }

    /** Vertices in order: top-left, bottom-left, bottom-right, top-right as seen from outside the face. */
    private static void quad(VertexConsumer vc, PoseStack.Pose p, int color, int light, int overlay,
                             float x0, float y0, float z0, float x1, float y1, float z1,
                             float x2, float y2, float z2, float x3, float y3, float z3,
                             float u0, float v0, float u1, float v1, float nx, float ny, float nz) {
        vertex(vc, p, color, light, overlay, x0, y0, z0, u0, v0, nx, ny, nz);
        vertex(vc, p, color, light, overlay, x1, y1, z1, u0, v1, nx, ny, nz);
        vertex(vc, p, color, light, overlay, x2, y2, z2, u1, v1, nx, ny, nz);
        vertex(vc, p, color, light, overlay, x3, y3, z3, u1, v0, nx, ny, nz);
    }

    private static void vertex(VertexConsumer vc, PoseStack.Pose p, int color, int light, int overlay,
                               float x, float y, float z, float u, float v, float nx, float ny, float nz) {
        vc.addVertex(p.pose(), x, y, z)
            .setColor(color)
            .setUv(u, v)
            .setOverlay(overlay)
            .setLight(light)
            .setNormal(p, nx, ny, nz);
    }
}
