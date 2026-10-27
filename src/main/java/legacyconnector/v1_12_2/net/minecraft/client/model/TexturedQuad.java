package legacyconnector.v1_12_2.net.minecraft.client.model;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** One face of a box: four vertices with their uvs (drawn through the Tessellator as in 1.12.2). */
public class TexturedQuad {
    public PositionTextureVertex[] vertexPositions;
    public int nVertices;
    private boolean invertNormal;

    public TexturedQuad(PositionTextureVertex[] vertices) {
        vertexPositions = vertices;
        nVertices = vertices.length;
    }

    public TexturedQuad(PositionTextureVertex[] vertices, int texcoordU1, int texcoordV1, int texcoordU2, int texcoordV2, float textureWidth, float textureHeight) {
        this(vertices);
        float f = 0.0F / textureWidth;
        float f1 = 0.0F / textureHeight;
        vertices[0] = vertices[0].setTexturePosition(texcoordU2 / textureWidth - f, texcoordV1 / textureHeight + f1);
        vertices[1] = vertices[1].setTexturePosition(texcoordU1 / textureWidth + f, texcoordV1 / textureHeight + f1);
        vertices[2] = vertices[2].setTexturePosition(texcoordU1 / textureWidth + f, texcoordV2 / textureHeight - f1);
        vertices[3] = vertices[3].setTexturePosition(texcoordU2 / textureWidth - f, texcoordV2 / textureHeight - f1);
    }

    public void flipFace() {
        PositionTextureVertex[] flipped = new PositionTextureVertex[vertexPositions.length];
        for (int i = 0; i < vertexPositions.length; ++i) flipped[i] = vertexPositions[vertexPositions.length - i - 1];
        vertexPositions = flipped;
    }

    public void draw(BufferBuilder renderer, float scale) {
        Vec3d a = vertexPositions[1].vector3D.subtractReverse(vertexPositions[0].vector3D);
        Vec3d b = vertexPositions[1].vector3D.subtractReverse(vertexPositions[2].vector3D);
        Vec3d n = b.crossProduct(a).normalize();
        float nx = (float) n.x, ny = (float) n.y, nz = (float) n.z;
        if (invertNormal) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
        }
        renderer.begin(7, DefaultVertexFormats.OLDMODEL_POSITION_TEX_NORMAL);
        for (int i = 0; i < 4; ++i) {
            PositionTextureVertex v = vertexPositions[i];
            renderer.pos(v.vector3D.x * scale, v.vector3D.y * scale, v.vector3D.z * scale).tex(v.texturePositionX, v.texturePositionY).normal(nx, ny, nz).endVertex();
        }
        Tessellator.getInstance().draw();
    }
}
