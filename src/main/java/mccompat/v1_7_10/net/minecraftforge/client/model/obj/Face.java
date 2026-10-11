package mccompat.v1_7_10.net.minecraftforge.client.model.obj;

import mccompat.v1_7_10.net.minecraft.client.renderer.Tessellator;

public class Face {
    public Vertex[] vertices;
    public Vertex[] vertexNormals;
    public Vertex faceNormal;
    public TextureCoordinate[] textureCoordinates;

    public void addFaceForRender(Tessellator tessellator) {
        addFaceForRender(tessellator, 0.0005F);
    }

    public void addFaceForRender(Tessellator tessellator, float textureOffset) {
        if (faceNormal == null) faceNormal = calculateFaceNormal();
        tessellator.setNormal(faceNormal.x, faceNormal.y, faceNormal.z);
        float averageU = 0F, averageV = 0F;
        if (textureCoordinates != null && textureCoordinates.length > 0) {
            for (TextureCoordinate t : textureCoordinates) {
                averageU += t.u;
                averageV += t.v;
            }
            averageU /= textureCoordinates.length;
            averageV /= textureCoordinates.length;
        }
        for (int i = 0; i < vertices.length; ++i) {
            if (textureCoordinates != null && textureCoordinates.length > 0) {
                float offsetU = textureOffset, offsetV = textureOffset;
                if (textureCoordinates[i].u > averageU) offsetU = -offsetU;
                if (textureCoordinates[i].v > averageV) offsetV = -offsetV;
                if (vertexNormals != null && i < vertexNormals.length) {
                    tessellator.setNormal(vertexNormals[i].x, vertexNormals[i].y, vertexNormals[i].z);
                }
                tessellator.addVertexWithUV(vertices[i].x, vertices[i].y, vertices[i].z, textureCoordinates[i].u + offsetU,
                        textureCoordinates[i].v + offsetV);
            } else {
                tessellator.addVertex(vertices[i].x, vertices[i].y, vertices[i].z);
            }
        }
    }

    public Vertex calculateFaceNormal() {
        float ax = vertices[1].x - vertices[0].x, ay = vertices[1].y - vertices[0].y, az = vertices[1].z - vertices[0].z;
        float bx = vertices[2].x - vertices[0].x, by = vertices[2].y - vertices[0].y, bz = vertices[2].z - vertices[0].z;
        float nx = ay * bz - az * by, ny = az * bx - ax * bz, nz = ax * by - ay * bx;
        float len = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (len == 0) return new Vertex(0, 1, 0);
        return new Vertex(nx / len, ny / len, nz / len);
    }
}
