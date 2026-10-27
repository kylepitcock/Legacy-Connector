package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.IBakedModel;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;
import legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline.VertexBufferConsumer;

/**
 * Writes a block model's quads into a buffer builder the way 1.12.2 meshed blocks (flat lighting from the world, tints from the block
 * colours), for code that draws blocks itself; and draws a model with one brightness through the Tessellator.
 */
public class BlockModelRenderer {
    private final BlockColors blockColors;

    public BlockModelRenderer(BlockColors blockColorsIn) {
        blockColors = blockColorsIn;
    }

    private static long positionRandom(BlockPos pos) {
        long i = (long) (pos.getX() * 3129871) ^ (long) pos.getZ() * 116129781L ^ (long) pos.getY();
        i = i * i * 42317861L + i * 11L;
        return i;
    }

    public boolean renderModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder buffer, boolean checkSides) {
        return renderModel(worldIn, modelIn, stateIn, posIn, buffer, checkSides, positionRandom(posIn));
    }

    public boolean renderModel(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder buffer, boolean checkSides, long rand) {
        try {
            return renderModelFlat(worldIn, modelIn, stateIn, posIn, buffer, checkSides, rand);
        } catch (RuntimeException e) {
            org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("meshing a block model into a legacy buffer", e);
            return false;
        }
    }

    /** Smooth lighting is the modern renderer's: legacy meshing uses flat lighting. */
    public boolean renderModelSmooth(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder buffer, boolean checkSides, long rand) {
        return renderModelFlat(worldIn, modelIn, stateIn, posIn, buffer, checkSides, rand);
    }

    public boolean renderModelFlat(IBlockAccess worldIn, IBakedModel modelIn, IBlockState stateIn, BlockPos posIn, BufferBuilder buffer, boolean checkSides, long rand) {
        boolean drawn = false;
        for (EnumFacing side : EnumFacing.values()) {
            List<BakedQuad> quads = modelIn.getQuads(stateIn, side, rand);
            if (quads.isEmpty()) continue;
            int light = light(worldIn, posIn.offset(side));
            renderQuadsFlat(worldIn, stateIn, posIn, light, buffer, quads);
            drawn = true;
        }
        List<BakedQuad> general = modelIn.getQuads(stateIn, null, rand);
        if (!general.isEmpty()) {
            renderQuadsFlat(worldIn, stateIn, posIn, light(worldIn, posIn), buffer, general);
            drawn = true;
        }
        return drawn;
    }

    private static int light(IBlockAccess world, BlockPos pos) {
        if (world == null) return 0xF000F0;
        try {
            return world.getCombinedLight(pos, 0);
        } catch (RuntimeException e) {
            return 0xF000F0;
        }
    }

    private void renderQuadsFlat(IBlockAccess world, IBlockState state, BlockPos pos, int light, BufferBuilder buffer, List<BakedQuad> quads) {
        boolean lightmap = buffer.getVertexFormat().hasUvOffset(1);
        for (BakedQuad quad : quads) {
            if (quad.getFormat().equals(buffer.getVertexFormat())) {
                buffer.addVertexData(quad.getVertexData());
                buffer.putPosition(pos.getX(), pos.getY(), pos.getZ());
            } else {
                VertexBufferConsumer consumer = new VertexBufferConsumer(buffer);
                consumer.setOffset(pos);
                quad.pipe(consumer);
            }
            if (lightmap) buffer.putBrightness4(light, light, light, light);
            if (quad.hasTintIndex() && buffer.getVertexFormat().hasColor()) {
                int color = blockColors.colorMultiplier(state, world, pos, quad.getTintIndex());
                float r = (color >> 16 & 255) / 255.0F, g = (color >> 8 & 255) / 255.0F, b = (color & 255) / 255.0F;
                for (int i = 4; i >= 1; i--) buffer.putColorMultiplier(r, g, b, i);
            }
        }
    }

    public void renderModelBrightnessColor(IBakedModel bakedModel, float brightness, float red, float green, float blue) {
        renderModelBrightnessColor(null, bakedModel, brightness, red, green, blue);
    }

    public void renderModelBrightnessColor(IBlockState state, IBakedModel modelIn, float brightness, float red, float green, float blue) {
        for (EnumFacing side : EnumFacing.values()) renderModelBrightnessColorQuads(brightness, red, green, blue, modelIn.getQuads(state, side, 0L));
        renderModelBrightnessColorQuads(brightness, red, green, blue, modelIn.getQuads(state, null, 0L));
    }

    public void renderModelBrightness(IBakedModel model, IBlockState state, float brightness, boolean glDefaults) {
        GlStateManager.rotate(90.0F, 0.0F, 1.0F, 0.0F);
        int i = blockColors.colorMultiplier(state, null, null, 0);
        float r = (i >> 16 & 255) / 255.0F, g = (i >> 8 & 255) / 255.0F, b = (i & 255) / 255.0F;
        if (!glDefaults) GlStateManager.color(brightness, brightness, brightness, 1.0F);
        renderModelBrightnessColor(state, model, brightness, r, g, b);
    }

    private void renderModelBrightnessColorQuads(float brightness, float red, float green, float blue, List<BakedQuad> listQuads) {
        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        for (BakedQuad quad : listQuads) {
            buffer.begin(7, DefaultVertexFormats.ITEM);
            if (quad.getFormat().equals(DefaultVertexFormats.ITEM)) buffer.addVertexData(quad.getVertexData());
            else quad.pipe(new VertexBufferConsumer(buffer));
            if (quad.hasTintIndex()) buffer.putColorRGB_F4(red * brightness, green * brightness, blue * brightness);
            else buffer.putColorRGB_F4(brightness, brightness, brightness);
            var d = (quad.getFace() == null ? EnumFacing.UP : quad.getFace()).getDirectionVec();
            buffer.putNormal(d.getX(), d.getY(), d.getZ());
            tessellator.draw();
        }
    }
}
