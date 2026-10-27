package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;

/** Writes piped quads into a buffer builder, element by element in the buffer's format (offset by a block position). */
public class VertexBufferConsumer implements IVertexConsumer {
    private static final float[] DUMMY_COLOR = {1, 1, 1, 1};
    private BufferBuilder renderer;
    private int[] quadData;
    private int v = 0;
    private BlockPos offset = BlockPos.ORIGIN;

    public VertexBufferConsumer() {
    }

    public VertexBufferConsumer(BufferBuilder buffer) {
        setBuffer(buffer);
    }

    @Override
    public VertexFormat getVertexFormat() {
        return renderer.getVertexFormat();
    }

    @Override
    public void put(int e, float... data) {
        VertexFormat format = getVertexFormat();
        if (renderer.isColorDisabled() && format.getElement(e).getUsage() == legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage.COLOR) {
            data = DUMMY_COLOR;
        }
        LightUtil.pack(data, quadData, format, v, e);
        if (e == format.getElementCount() - 1) {
            v++;
            if (v == 4) {
                renderer.addVertexData(quadData);
                renderer.putPosition(offset.getX(), offset.getY(), offset.getZ());
                java.util.Arrays.fill(quadData, 0);
                v = 0;
            }
        }
    }

    private void checkVertexFormat() {
        if (quadData == null || renderer.getVertexFormat().getSize() != quadData.length) {
            quadData = new int[renderer.getVertexFormat().getSize()];
        }
    }

    public void setBuffer(BufferBuilder buffer) {
        renderer = buffer;
        checkVertexFormat();
    }

    public void setOffset(BlockPos offset) {
        this.offset = new BlockPos(offset);
    }

    @Override
    public void setQuadTint(int tint) {
    }

    @Override
    public void setQuadOrientation(EnumFacing orientation) {
    }

    @Override
    public void setApplyDiffuseLighting(boolean diffuse) {
    }

    @Override
    public void setTexture(TextureAtlasSprite texture) {
    }
}
