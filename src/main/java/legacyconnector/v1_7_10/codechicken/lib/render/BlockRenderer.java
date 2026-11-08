package legacyconnector.v1_7_10.codechicken.lib.render;

import legacyconnector.v1_7_10.codechicken.lib.lighting.LC;
import legacyconnector.v1_7_10.codechicken.lib.vec.Cuboid6;

/** CodeChickenLib's block box renderer: faces and cuboids drawn through the render state. */
public class BlockRenderer {
    /** A single face of a block: four vertices and their light coordinates. */
    public static class BlockFace extends CCModel {
        public LC[] lightCoords = new LC[] {new LC(), new LC(), new LC(), new LC()};
        public boolean lcComputed;
        public int side;

        public BlockFace() {
            super(7);
            verts = new Vertex5[] {new Vertex5(), new Vertex5(), new Vertex5(), new Vertex5()};
        }

        /** Sets the face to the given side of the cuboid. */
        public BlockFace loadCuboidFace(Cuboid6 c, int side) {
            this.side = side;
            CCModel.face(verts, 0, c, side);
            lcComputed = false;
            return this;
        }

        public BlockFace computeLightCoords() {
            if (!lcComputed) {
                for (int i = 0; i < 4; i++) lightCoords[i].compute(verts[i].vec, side);
                lcComputed = true;
            }
            return this;
        }
    }

    /** Draws the sides of the cuboid whose bit is set in the mask (bit s = side s) into the started Tessellator. */
    public static void renderCuboid(Cuboid6 bounds, int sideMask) {
        CCModel.generateBlockModel(bounds, ~sideMask & 63).render();
    }

    /** Draws a unit block's sides. */
    public static void renderFullBlock(int sideMask) {
        renderCuboid(Cuboid6.full, sideMask);
    }
}
