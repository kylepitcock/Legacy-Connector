package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** 1.12.2 face of a model element: the side it is culled against, tint index, texture variable and texture coordinates. */
public class BlockPartFace {
    public static final EnumFacing FACING_DEFAULT = null;
    public final EnumFacing cullFace;
    public final int tintIndex;
    public final String texture;
    public final BlockFaceUV blockFaceUV;

    public BlockPartFace(EnumFacing cullFaceIn, int tintIndexIn, String textureIn, BlockFaceUV blockFaceUVIn) {
        this.cullFace = cullFaceIn;
        this.tintIndex = tintIndexIn;
        this.texture = textureIn;
        this.blockFaceUV = blockFaceUVIn;
    }

    /** Gson adapter of a face; the texture coordinates go through the context's {@link BlockFaceUV} adapter. */
    public static class Deserializer implements com.google.gson.JsonDeserializer<BlockPartFace> {
        @Override
        public BlockPartFace deserialize(com.google.gson.JsonElement json, java.lang.reflect.Type type, com.google.gson.JsonDeserializationContext context) {
            return ModelBlock.face(json.getAsJsonObject(), context);
        }
    }

    net.minecraft.client.renderer.block.model.BlockElementFace mcc$modern() {
        return new net.minecraft.client.renderer.block.model.BlockElementFace(cullFace == null ? null : cullFace.mcc$modern(), tintIndex, texture,
                blockFaceUV.mcc$modern());
    }
}
