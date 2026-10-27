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

    net.minecraft.client.renderer.block.model.BlockElementFace mcc$modern() {
        return new net.minecraft.client.renderer.block.model.BlockElementFace(cullFace == null ? null : cullFace.mcc$modern(), tintIndex, texture,
                blockFaceUV.mcc$modern());
    }
}
