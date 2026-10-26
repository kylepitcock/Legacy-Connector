package legacyconnector.v1_7_10.net.minecraftforge.fluids;

import legacyconnector.v1_7_10.cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.RenderBlocks;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.util.IIcon;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/**
 * Draws fluid blocks (Forge's fluid renderer): a column whose height is the fluid level, still texture on the
 * top and bottom, flowing texture on the sides, sides hidden against the same fluid.
 */
public class RenderBlockFluid implements ISimpleBlockRenderingHandler {
    /** Forge: the one registered instance (mods draw fluids in their own handlers through it). */
    public static RenderBlockFluid instance = new RenderBlockFluid();

    @Override
    public void renderInventoryBlock(Block block, int metadata, int modelId, RenderBlocks renderer) {
    }

    @Override
    public boolean shouldRender3DInInventory(int modelId) {
        return false;
    }

    @Override
    public int getRenderId() {
        return FluidRegistry.renderIdFluid;
    }

    @Override
    public boolean renderWorldBlock(IBlockAccess world, int x, int y, int z, Block block, int modelId, RenderBlocks renderer) {
        if (!(block instanceof BlockFluidBase fluidBlock)) return false;
        Fluid fluid = fluidBlock.getFluid();
        IIcon still = fluid != null ? fluid.getStillIcon() : null;
        IIcon flowing = fluid != null && fluid.getFlowingIcon() != null ? fluid.getFlowingIcon() : still;
        if (still == null) still = block.getIcon(1, 0);
        if (flowing == null) flowing = block.getIcon(2, 0);
        if (still == null) return false;

        Tessellator t = Tessellator.instance;
        int color = fluid != null ? fluid.getColor() : 0xFFFFFF;
        float r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
        int dir = fluidBlock.densityDir;
        boolean flip = dir > 0;

        int quanta = fluidBlock.getQuantaValue(world, x, y, z);
        float height = Math.min(1.0F, Math.max(0.125F, quanta / fluidBlock.quantaPerBlockFloat));
        Block above = world.getBlock(x, y - dir, z);
        if (above == block) height = 1.0F;
        if (fluidBlock instanceof BlockFluidClassic && world.getBlockMetadata(x, y, z) == 0) height = Math.max(height, 0.875F);
        if (above == block) height = 1.0F;
        float top = flip ? 1.0F : height;
        float bottom = flip ? 1.0F - height : 0.0F;
        if (above != block && !flip) top = height;

        boolean any = false;
        int brightness = block.getMixedBrightnessForBlock(world, x, y, z);
        // top
        if (world.getBlock(x, y + 1, z) != block) {
            t.setBrightness(brightness);
            t.setColorOpaque_F(r, g, b);
            double u0 = still.getMinU(), u1 = still.getMaxU(), v0 = still.getMinV(), v1 = still.getMaxV();
            t.addVertexWithUV(x, y + top, z + 1, u0, v1);
            t.addVertexWithUV(x + 1, y + top, z + 1, u1, v1);
            t.addVertexWithUV(x + 1, y + top, z, u1, v0);
            t.addVertexWithUV(x, y + top, z, u0, v0);
            any = true;
        }
        // bottom
        if (world.getBlock(x, y - 1, z) != block && !world.getBlock(x, y - 1, z).isOpaqueCube()) {
            t.setBrightness(block.getMixedBrightnessForBlock(world, x, y - 1, z));
            t.setColorOpaque_F(r * 0.5F, g * 0.5F, b * 0.5F);
            double u0 = still.getMinU(), u1 = still.getMaxU(), v0 = still.getMinV(), v1 = still.getMaxV();
            t.addVertexWithUV(x, y + bottom, z, u0, v0);
            t.addVertexWithUV(x + 1, y + bottom, z, u1, v0);
            t.addVertexWithUV(x + 1, y + bottom, z + 1, u1, v1);
            t.addVertexWithUV(x, y + bottom, z + 1, u0, v1);
            any = true;
        }
        // sides
        int[][] sides = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}};
        for (int[] s : sides) {
            int nx = x + s[0], nz = z + s[1];
            Block n = world.getBlock(nx, y, nz);
            if (n == block || n.isOpaqueCube()) continue;
            t.setBrightness(block.getMixedBrightnessForBlock(world, nx, y, nz));
            float shade = s[0] != 0 ? 0.6F : 0.8F;
            t.setColorOpaque_F(r * shade, g * shade, b * shade);
            double u0 = flowing.getMinU(), u1 = flowing.getMaxU();
            double v0 = flowing.getInterpolatedV((1.0F - (top - bottom)) * 16.0F), v1 = flowing.getMaxV();
            double y0 = y + bottom, y1 = y + top;
            if (s[1] == -1) {
                t.addVertexWithUV(x, y1, z, u0, v0);
                t.addVertexWithUV(x + 1, y1, z, u1, v0);
                t.addVertexWithUV(x + 1, y0, z, u1, v1);
                t.addVertexWithUV(x, y0, z, u0, v1);
            } else if (s[1] == 1) {
                t.addVertexWithUV(x + 1, y1, z + 1, u0, v0);
                t.addVertexWithUV(x, y1, z + 1, u1, v0);
                t.addVertexWithUV(x, y0, z + 1, u1, v1);
                t.addVertexWithUV(x + 1, y0, z + 1, u0, v1);
            } else if (s[0] == -1) {
                t.addVertexWithUV(x, y1, z + 1, u0, v0);
                t.addVertexWithUV(x, y1, z, u1, v0);
                t.addVertexWithUV(x, y0, z, u1, v1);
                t.addVertexWithUV(x, y0, z + 1, u0, v1);
            } else {
                t.addVertexWithUV(x + 1, y1, z, u0, v0);
                t.addVertexWithUV(x + 1, y1, z + 1, u1, v0);
                t.addVertexWithUV(x + 1, y0, z + 1, u1, v1);
                t.addVertexWithUV(x + 1, y0, z, u0, v1);
            }
            any = true;
        }
        return any;
    }
}
