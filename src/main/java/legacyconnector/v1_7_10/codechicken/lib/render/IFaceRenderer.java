package legacyconnector.v1_7_10.codechicken.lib.render;

/** Something that draws one face of a block. */
public interface IFaceRenderer {
    void renderFace(Vertex5[] face, int side);
}
