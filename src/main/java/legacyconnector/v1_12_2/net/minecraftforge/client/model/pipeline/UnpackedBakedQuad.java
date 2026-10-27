package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** A quad kept as unpacked floats per vertex and element (packed into vertex data on first use), built element by element. */
public class UnpackedBakedQuad extends BakedQuad {
    protected final float[][][] unpackedData;
    protected final VertexFormat format;
    protected boolean packed = false;

    public UnpackedBakedQuad(float[][][] unpackedData, int tint, EnumFacing orientation, TextureAtlasSprite texture, boolean applyDiffuseLighting, VertexFormat format) {
        super(new int[format.getSize()], tint, orientation, texture, applyDiffuseLighting, format);
        this.unpackedData = unpackedData;
        this.format = format;
    }

    @Override
    public int[] getVertexData() {
        if (!packed) {
            packed = true;
            for (int v = 0; v < 4; v++) {
                for (int e = 0; e < format.getElementCount(); e++) LightUtil.pack(unpackedData[v][e], vertexData, format, v, e);
            }
        }
        return vertexData;
    }

    @Override
    public void pipe(IVertexConsumer consumer) {
        int[] eMap = LightUtil.mapFormats(consumer.getVertexFormat(), format);
        if (hasTintIndex()) consumer.setQuadTint(getTintIndex());
        consumer.setTexture(sprite);
        consumer.setApplyDiffuseLighting(applyDiffuseLighting);
        consumer.setQuadOrientation(getFace());
        for (int v = 0; v < 4; v++) {
            for (int e = 0; e < consumer.getVertexFormat().getElementCount(); e++) {
                if (eMap[e] != format.getElementCount()) consumer.put(e, unpackedData[v][eMap[e]]);
                else consumer.put(e);
            }
        }
    }

    public static class Builder implements IVertexConsumer {
        private final VertexFormat format;
        private final float[][][] unpackedData;
        private int tint = -1;
        private EnumFacing orientation;
        private TextureAtlasSprite texture;
        private boolean applyDiffuseLighting = true;
        private int vertices = 0;
        private int elements = 0;
        private boolean full = false;
        private boolean contractUVs = false;

        public Builder(VertexFormat format) {
            this.format = format;
            unpackedData = new float[4][format.getElementCount()][4];
        }

        @Override
        public VertexFormat getVertexFormat() {
            return format;
        }

        public void setContractUVs(boolean value) {
            contractUVs = value;
        }

        @Override
        public void setQuadTint(int tint) {
            this.tint = tint;
        }

        @Override
        public void setQuadOrientation(EnumFacing orientation) {
            this.orientation = orientation;
        }

        @Deprecated
        public void setQuadColored() {
        }

        @Override
        public void setTexture(TextureAtlasSprite texture) {
            this.texture = texture;
        }

        @Override
        public void setApplyDiffuseLighting(boolean diffuse) {
            applyDiffuseLighting = diffuse;
        }

        @Override
        public void put(int element, float... data) {
            for (int i = 0; i < 4; i++) {
                if (i < data.length) unpackedData[vertices][element][i] = data[i];
                else unpackedData[vertices][element][i] = 0;
            }
            elements++;
            if (elements == format.getElementCount()) {
                vertices++;
                elements = 0;
            }
            if (vertices == 4) full = true;
        }

        public UnpackedBakedQuad build() {
            if (!full) throw new IllegalStateException("not enough data");
            if (texture == null) throw new IllegalStateException("texture not set");
            if (contractUVs) {
                float tX = texture.getIconWidth() / (texture.getMaxU() - texture.getMinU());
                float tY = texture.getIconHeight() / (texture.getMaxV() - texture.getMinV());
                float tS = Math.max(tX, tY);
                float ep = 1.0F / (tS * 0x100);
                int uve = 0;
                while (uve < format.getElementCount()) {
                    var e = format.getElement(uve);
                    if (e.getUsage() == legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement.EnumUsage.UV && e.getIndex() == 0) break;
                    uve++;
                }
                if (uve == format.getElementCount()) throw new IllegalStateException("Can't contract UVs: format doesn't contain UVs");
                float[] uvc = new float[4];
                for (int v = 0; v < 4; v++) for (int i = 0; i < 4; i++) uvc[i] += unpackedData[v][uve][i] / 4;
                for (int v = 0; v < 4; v++) {
                    for (int i = 0; i < 4; i++) {
                        float uo = unpackedData[v][uve][i];
                        float eps = 1.0F / 0x100;
                        float un = uo * (1 - eps) + uvc[i] * eps;
                        float ud = uo - un;
                        float aud = ud;
                        if (aud < 0) aud = -aud;
                        if (aud < ep) {
                            float udc = un - uvc[i];
                            if (udc < 0) udc = -udc;
                            if (udc < 2 * ep) un = (uo + uvc[i]) / 2;
                            else un = uo + (ud < 0 ? ep : -ep);
                        }
                        unpackedData[v][uve][i] = un;
                    }
                }
            }
            return new UnpackedBakedQuad(unpackedData, tint, orientation, texture, applyDiffuseLighting, format);
        }
    }
}
