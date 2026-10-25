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

    public void draw(legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator tessellator, float scale) {
        legacyconnector.v1_7_10.net.minecraft.util.Vec3 a = vertexPositions[1].vector3D.subtract(vertexPositions[0].vector3D);
        legacyconnector.v1_7_10.net.minecraft.util.Vec3 b = vertexPositions[1].vector3D.subtract(vertexPositions[2].vector3D);
        legacyconnector.v1_7_10.net.minecraft.util.Vec3 n = b.crossProduct(a).normalize();
        float sign = invertNormal ? -1.0F : 1.0F;
        tessellator.startDrawingQuads();
        tessellator.setNormal(sign * (float) n.xCoord, sign * (float) n.yCoord, sign * (float) n.zCoord);
        for (PositionTextureVertex v : vertexPositions) {
            tessellator.addVertexWithUV(v.vector3D.xCoord * scale, v.vector3D.yCoord * scale, v.vector3D.zCoord * scale, v.texturePositionX, v.texturePositionY);
        }
        tessellator.draw();
    }

    public void flipFace() {
        PositionTextureVertex[] flipped = new PositionTextureVertex[vertexPositions.length];
        for (int i = 0; i < vertexPositions.length; i++) flipped[i] = vertexPositions[vertexPositions.length - i - 1];
        vertexPositions = flipped;
    }
}
