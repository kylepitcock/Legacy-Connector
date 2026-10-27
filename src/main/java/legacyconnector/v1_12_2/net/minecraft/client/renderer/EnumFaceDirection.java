package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;
import net.minecraft.client.renderer.FaceInfo;

/**
 * 1.12.2 cube face corner table: for each face, its four corners as indices into a {minX, minY, minZ, maxX, maxY, maxZ}-style array
 * indexed by facing ({@link Constants}). The same table as the modern {@link FaceInfo}.
 */
public enum EnumFaceDirection {
    DOWN, UP, NORTH, SOUTH, WEST, EAST;

    private final VertexInformation[] vertexInfos = new VertexInformation[4];

    EnumFaceDirection() {
        FaceInfo modern = FaceInfo.valueOf(name());
        for (int i = 0; i < 4; i++) {
            FaceInfo.VertexInfo v = modern.getVertexInfo(i);
            vertexInfos[i] = new VertexInformation(v.xFace, v.yFace, v.zFace);
        }
    }

    public static EnumFaceDirection getFacing(EnumFacing facing) {
        return values()[switch (facing) {
            case DOWN -> 0;
            case UP -> 1;
            case NORTH -> 2;
            case SOUTH -> 3;
            case WEST -> 4;
            case EAST -> 5;
        }];
    }

    public VertexInformation getVertexInformation(int index) {
        return vertexInfos[index];
    }

    public static final class Constants {
        public static final int SOUTH_INDEX = EnumFacing.SOUTH.getIndex();
        public static final int UP_INDEX = EnumFacing.UP.getIndex();
        public static final int EAST_INDEX = EnumFacing.EAST.getIndex();
        public static final int NORTH_INDEX = EnumFacing.NORTH.getIndex();
        public static final int DOWN_INDEX = EnumFacing.DOWN.getIndex();
        public static final int WEST_INDEX = EnumFacing.WEST.getIndex();
    }

    public static class VertexInformation {
        public final int xIndex;
        public final int yIndex;
        public final int zIndex;

        private VertexInformation(int xIndexIn, int yIndexIn, int zIndexIn) {
            this.xIndex = xIndexIn;
            this.yIndex = yIndexIn;
            this.zIndex = zIndexIn;
        }
    }
}
