package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import java.util.Objects;
import javax.vecmath.Vector3f;
import legacyconnector.v1_12_2.net.minecraft.block.state.IBlockState;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.EntityRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.color.BlockColors;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.texture.TextureAtlasSprite;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.IBlockAccess;

/**
 * Forge 1.12.2's flat block lighter: gathers each quad, generates its normal when the format has none, then passes every vertex on with
 * the block light of the side it faces, the block's tint and the directional shading applied (see {@link BlockInfo}).
 */
public class VertexLighterFlat extends QuadGatheringTransformer {
    protected static final VertexFormatElement NORMAL_4F = new VertexFormatElement(0, VertexFormatElement.EnumType.FLOAT, VertexFormatElement.EnumUsage.NORMAL, 4);

    protected final BlockInfo blockInfo;
    private int tint = -1;
    private boolean diffuse = true;
    protected int posIndex = -1;
    protected int normalIndex = -1;
    protected int colorIndex = -1;
    protected int lightmapIndex = -1;
    protected VertexFormat baseFormat;

    public VertexLighterFlat(BlockColors colors) {
        this.blockInfo = new BlockInfo(colors);
    }

    @Override
    public void setParent(IVertexConsumer parent) {
        super.setParent(parent);
        setVertexFormat(parent.getVertexFormat());
    }

    @Override
    public void setVertexFormat(VertexFormat format) {
        if (Objects.equals(format, baseFormat)) return;
        baseFormat = format;
        super.setVertexFormat(withNormal(format));
        updateIndices();
    }

    private void updateIndices() {
        posIndex = normalIndex = colorIndex = lightmapIndex = -1;
        for (int i = 0; i < getVertexFormat().getElementCount(); i++) {
            VertexFormatElement element = getVertexFormat().getElement(i);
            switch (element.getUsage()) {
                case POSITION -> posIndex = i;
                case NORMAL -> normalIndex = i;
                case COLOR -> colorIndex = i;
                case UV -> {
                    if (element.getIndex() == 1) lightmapIndex = i;
                }
                default -> {
                }
            }
        }
        if (posIndex == -1) throw new IllegalArgumentException("vertex lighter needs format with position");
        if (lightmapIndex == -1) throw new IllegalArgumentException("vertex lighter needs format with lightmap");
        if (colorIndex == -1) throw new IllegalArgumentException("vertex lighter needs format with color");
    }

    private static VertexFormat withNormal(VertexFormat format) {
        if (format == null || format.hasNormal()) return format;
        return new VertexFormat(format).addElement(NORMAL_4F);
    }

    @Override
    protected void processQuad() {
        float[][] position = quadData[posIndex];
        float[][] normal;
        float[][] lightmap = quadData[lightmapIndex];
        float[][] color = quadData[colorIndex];
        if (normalIndex != -1 && dataLength[normalIndex] >= 3
                && (quadData[normalIndex][0][0] != -1 || quadData[normalIndex][0][1] != -1 || quadData[normalIndex][0][2] != -1)) {
            normal = quadData[normalIndex];
        } else {
            normal = new float[4][4];
            Vector3f v1 = new Vector3f(position[3][0], position[3][1], position[3][2]);
            Vector3f t = new Vector3f(position[1][0], position[1][1], position[1][2]);
            Vector3f v2 = new Vector3f(position[2][0], position[2][1], position[2][2]);
            v1.sub(t);
            t.set(position[0][0], position[0][1], position[0][2]);
            v2.sub(t);
            v1.cross(v2, v1);
            if (v1.lengthSquared() > 0.0F) v1.normalize();
            for (int v = 0; v < 4; v++) {
                normal[v][0] = v1.x;
                normal[v][1] = v1.y;
                normal[v][2] = v1.z;
                normal[v][3] = 0.0F;
            }
        }
        int multiplier = -1;
        if (tint != -1) multiplier = blockInfo.getColorMultiplier(tint);
        VertexFormat format = parent.getVertexFormat();
        int count = format.getElementCount();
        for (int v = 0; v < 4; v++) {
            position[v][0] += blockInfo.getShx();
            position[v][1] += blockInfo.getShy();
            position[v][2] += blockInfo.getShz();
            float x = position[v][0] - 0.5F + normal[v][0] * 0.5F;
            float y = position[v][1] - 0.5F + normal[v][1] * 0.5F;
            float z = position[v][2] - 0.5F + normal[v][2] * 0.5F;
            float blockLight = lightmap[v][0], skyLight = lightmap[v][1];
            updateLightmap(normal[v], lightmap[v], x, y, z);
            if (dataLength[lightmapIndex] > 1) {
                if (blockLight > lightmap[v][0]) lightmap[v][0] = blockLight;
                if (skyLight > lightmap[v][1]) lightmap[v][1] = skyLight;
            }
            updateColor(normal[v], color[v], x, y, z, tint, multiplier);
            if (diffuse) {
                float d = LightUtil.diffuseLight(normal[v][0], normal[v][1], normal[v][2]);
                for (int i = 0; i < 3; i++) color[v][i] *= d;
            }
            if (EntityRenderer.anaglyphEnable) applyAnaglyph(color[v]);
            for (int e = 0; e < count; e++) {
                VertexFormatElement element = format.getElement(e);
                switch (element.getUsage()) {
                    case POSITION -> parent.put(e, position[v]);
                    case NORMAL -> parent.put(e, normal[v]);
                    case COLOR -> parent.put(e, color[v]);
                    case UV -> parent.put(e, element.getIndex() == 1 ? lightmap[v] : quadData[e][v]);
                    default -> parent.put(e, quadData[e][v]);
                }
            }
        }
        tint = -1;
    }

    protected void applyAnaglyph(float[] color) {
        float r = color[0];
        color[0] = (r * 30 + color[1] * 59 + color[2] * 11) / 100;
        color[1] = (r * 3 + color[1] * 7) / 10;
        color[2] = (r * 3 + color[2] * 7) / 10;
    }

    protected void updateLightmap(float[] normal, float[] lightmap, float x, float y, float z) {
        final float e1 = 1.0F - 1e-2F;
        final float e2 = 0.95F;
        boolean full = blockInfo.isFullCube();
        EnumFacing side = null;
        if ((full || y < -e1) && normal[1] < -e2) side = EnumFacing.DOWN;
        else if ((full || y > e1) && normal[1] > e2) side = EnumFacing.UP;
        else if ((full || z < -e1) && normal[2] < -e2) side = EnumFacing.NORTH;
        else if ((full || z > e1) && normal[2] > e2) side = EnumFacing.SOUTH;
        else if ((full || x < -e1) && normal[0] < -e2) side = EnumFacing.WEST;
        else if ((full || x > e1) && normal[0] > e2) side = EnumFacing.EAST;
        int i = side == null ? 0 : side.ordinal() + 1;
        int brightness = blockInfo.getPackedLight()[i];
        lightmap[0] = ((float) ((brightness >> 0x04) & 0xF) * 0x20) / 0xFFFF;
        lightmap[1] = ((float) ((brightness >> 0x14) & 0xF) * 0x20) / 0xFFFF;
    }

    protected void updateColor(float[] normal, float[] color, float x, float y, float z, float tint, int multiplier) {
        if (tint != -1) {
            color[0] *= (float) (multiplier >> 0x10 & 0xFF) / 0xFF;
            color[1] *= (float) (multiplier >> 0x8 & 0xFF) / 0xFF;
            color[2] *= (float) (multiplier & 0xFF) / 0xFF;
        }
    }

    @Override
    public void setQuadTint(int tint) {
        this.tint = tint;
    }

    @Override
    public void setQuadOrientation(EnumFacing orientation) {
    }

    public void setQuadCulled() {
    }

    @Override
    public void setTexture(TextureAtlasSprite texture) {
    }

    @Override
    public void setApplyDiffuseLighting(boolean diffuse) {
        this.diffuse = diffuse;
    }

    public void setWorld(IBlockAccess world) {
        blockInfo.setWorld(world);
    }

    public void setState(IBlockState state) {
        blockInfo.setState(state);
    }

    public void setBlockPos(BlockPos blockPos) {
        blockInfo.setBlockPos(blockPos);
    }

    public void resetBlockInfo() {
        blockInfo.reset();
    }

    public void updateBlockInfo() {
        blockInfo.updateShift();
        blockInfo.updateFlatLighting();
    }
}
