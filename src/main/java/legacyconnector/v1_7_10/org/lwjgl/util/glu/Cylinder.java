package legacyconnector.v1_7_10.org.lwjgl.util.glu;

import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glBegin;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glEnd;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glTexCoord2f;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glVertex3f;

/** GLU cylinder (or cone) around the z axis in immediate mode (the port of LWJGL 2's Cylinder): quad strips per stack. */
public class Cylinder extends Quadric {
    private static final int GL_POINTS = 0;
    private static final int GL_LINES = 1;
    private static final int GL_LINE_LOOP = 2;
    private static final int GL_QUAD_STRIP = 8;

    public Cylinder() {
    }

    public void draw(float baseRadius, float topRadius, float height, int slices, int stacks) {
        if (slices < 2 || stacks < 1) return;
        float da = 2.0F * (float) Math.PI / slices;
        float dr = (topRadius - baseRadius) / stacks;
        float dz = height / stacks;
        float nz = height == 0.0F ? 0.0F : (baseRadius - topRadius) / height;
        float ny = 1.0F / (float) Math.sqrt(1.0F + nz * nz);
        nz *= ny;
        boolean outside = orientation == 100020;

        if (drawStyle == 100012) { // GLU_FILL
            float ds = 1.0F / slices;
            float dt = 1.0F / stacks;
            float t = 0.0F;
            float z = 0.0F;
            float r = baseRadius;
            for (int j = 0; j < stacks; j++) {
                float s = 0.0F;
                glBegin(GL_QUAD_STRIP);
                for (int i = 0; i <= slices; i++) {
                    float a = i == slices ? 0.0F : i * da;
                    float x = (float) Math.sin(a);
                    float y = (float) Math.cos(a);
                    if (outside) normal3f(x * ny, y * ny, nz);
                    else normal3f(-x * ny, -y * ny, -nz);
                    if (textureFlag) glTexCoord2f(s, t);
                    glVertex3f(x * r, y * r, z);
                    if (textureFlag) glTexCoord2f(s, t + dt);
                    glVertex3f(x * (r + dr), y * (r + dr), z + dz);
                    s += ds;
                }
                glEnd();
                r += dr;
                t += dt;
                z += dz;
            }
        } else if (drawStyle == 100011 || drawStyle == 100013) { // GLU_LINE, GLU_SILHOUETTE: rings
            float z = 0.0F;
            float r = baseRadius;
            for (int j = 0; j <= stacks; j++) {
                if (j != 0 && j != stacks && drawStyle == 100013) {
                    z += dz;
                    r += dr;
                    continue;
                }
                glBegin(GL_LINE_LOOP);
                for (int i = 0; i < slices; i++) {
                    float x = (float) Math.cos(i * da);
                    float y = (float) Math.sin(i * da);
                    normal3f(x * ny, y * ny, nz);
                    glVertex3f(x * r, y * r, z);
                }
                glEnd();
                z += dz;
                r += dr;
            }
            if (drawStyle == 100011) { // GLU_LINE: the length-wise lines
                glBegin(GL_LINES);
                for (int i = 0; i < slices; i++) {
                    float x = (float) Math.cos(i * da);
                    float y = (float) Math.sin(i * da);
                    normal3f(x, y, 0.0F);
                    glVertex3f(x * baseRadius, y * baseRadius, 0.0F);
                    glVertex3f(x * topRadius, y * topRadius, height);
                }
                glEnd();
            }
        } else { // GLU_POINT
            glBegin(GL_POINTS);
            for (int i = 0; i < slices; i++) {
                float x = (float) Math.cos(i * da);
                float y = (float) Math.sin(i * da);
                normal3f(x * ny, y * ny, nz);
                float z = 0.0F;
                float r = baseRadius;
                for (int j = 0; j <= stacks; j++) {
                    glVertex3f(x * r, y * r, z);
                    z += dz;
                    r += dr;
                }
            }
            glEnd();
        }
    }
}
