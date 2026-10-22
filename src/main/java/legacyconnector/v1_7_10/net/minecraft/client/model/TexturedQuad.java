package legacyconnector.v1_7_10.net.minecraft.client.model;

/** One face of a box. Legacy code reads and adjusts the vertices; drawing goes through the modern cube. */
public class TexturedQuad {
    public PositionTextureVertex[] vertexPositions;
    public int nVertices;
    private boolean invertNormal;

    public TexturedQuad(PositionTextureVertex[] vertices) {
        this.vertexPositions = vertices;
        this.nVertices = vertices.length;
    }

    public TexturedQuad(PositionTextureVertex[] vertices, int u1, int v1, int u2, int v2, float texWidth, float texHeight) {
        this(vertices);
        float f = 0.0F / texWidth;
        float f1 = 0.0F / texHeight;
        vertices[0] = vertices[0].setTexturePosition(u2 / texWidth - f, v1 / texHeight + f1);
        vertices[1] = vertices[1].setTexturePosition(u1 / texWidth + f, v1 / texHeight + f1);
        vertices[2] = vertices[2].setTexturePosition(u1 / texWidth + f, v2 / texHeight - f1);
        vertices[3] = vertices[3].setTexturePosition(u2 / texWidth - f, v2 / texHeight - f1);
    }

    public void flipFace() {
        PositionTextureVertex[] flipped = new PositionTextureVertex[vertexPositions.length];
        for (int i = 0; i < vertexPositions.length; i++) flipped[i] = vertexPositions[vertexPositions.length - i - 1];
        vertexPositions = flipped;
    }
}
