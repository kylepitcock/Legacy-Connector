package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import legacyconnector.v1_12_2.org.lwjgl.util.vector.Vector3f;

/** 1.12.2 model element: a box from/to (0..16) with its faces, rotation and shading. */
public class BlockPart {
    public final Vector3f positionFrom;
    public final Vector3f positionTo;
    public final Map<EnumFacing, BlockPartFace> mapFaces;
    public final BlockPartRotation partRotation;
    public final boolean shade;

    public BlockPart(Vector3f positionFromIn, Vector3f positionToIn, Map<EnumFacing, BlockPartFace> mapFacesIn, BlockPartRotation partRotationIn, boolean shadeIn) {
        this.positionFrom = positionFromIn;
        this.positionTo = positionToIn;
        this.mapFaces = mapFacesIn;
        this.partRotation = partRotationIn;
        this.shade = shadeIn;
        setDefaultUvs();
    }

    private void setDefaultUvs() {
        for (Map.Entry<EnumFacing, BlockPartFace> e : mapFaces.entrySet()) e.getValue().blockFaceUV.setUvs(getFaceUvs(e.getKey()));
    }

    private float[] getFaceUvs(EnumFacing facing) {
        return switch (facing) {
            case DOWN -> new float[] {positionFrom.x, 16.0F - positionTo.z, positionTo.x, 16.0F - positionFrom.z};
            case UP -> new float[] {positionFrom.x, positionFrom.z, positionTo.x, positionTo.z};
            case NORTH -> new float[] {16.0F - positionTo.x, 16.0F - positionTo.y, 16.0F - positionFrom.x, 16.0F - positionFrom.y};
            case SOUTH -> new float[] {positionFrom.x, 16.0F - positionTo.y, positionTo.x, 16.0F - positionFrom.y};
            case WEST -> new float[] {positionFrom.z, 16.0F - positionTo.y, positionTo.z, 16.0F - positionFrom.y};
            case EAST -> new float[] {16.0F - positionTo.z, 16.0F - positionTo.y, 16.0F - positionFrom.z, 16.0F - positionFrom.y};
        };
    }
}
