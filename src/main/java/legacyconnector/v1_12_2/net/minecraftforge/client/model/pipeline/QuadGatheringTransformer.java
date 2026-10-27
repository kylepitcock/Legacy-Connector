package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;

/** Collects a whole quad (data per element and vertex) before handing it to {@link #processQuad}. */
public abstract class QuadGatheringTransformer implements IVertexConsumer {
    protected IVertexConsumer parent;
    protected VertexFormat format;
    protected int vertices = 0;
    protected byte[] dataLength = null;
    protected float[][][] quadData = null;

    public void setParent(IVertexConsumer parent) {
        this.parent = parent;
    }

    public void setVertexFormat(VertexFormat format) {
        this.format = format;
        dataLength = new byte[format.getElementCount()];
        quadData = new float[format.getElementCount()][4][4];
    }

    @Override
    public VertexFormat getVertexFormat() {
        return format;
    }

    @Override
    public void put(int element, float... data) {
        System.arraycopy(data, 0, quadData[element][vertices], 0, Math.min(data.length, 4));
        if (data.length > 0 && dataLength[element] == 0) dataLength[element] = (byte) data.length;
        if (element == getVertexFormat().getElementCount() - 1) vertices++;
        if (vertices == 4) {
            vertices = 0;
            processQuad();
        }
    }

    protected abstract void processQuad();
}
