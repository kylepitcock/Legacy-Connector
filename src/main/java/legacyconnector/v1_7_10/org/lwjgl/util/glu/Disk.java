package legacyconnector.v1_7_10.org.lwjgl.util.glu;

import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glBegin;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glEnd;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glNormal3f;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glTexCoord2f;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glVertex3f;

/** GLU disk (or ring) in the z = 0 plane in immediate mode (the port of LWJGL 2's Disk): quad strips per loop. */
public class Disk extends Quadric {
    private static final int GL_POINTS = 0;
    private static final int GL_LINES = 1;
    private static final int GL_LINE_LOOP = 2;
    private static final int GL_QUAD_STRIP = 8;

    public Disk() {
    }

    public void draw(float innerRadius, float outerRadius, int slices, int loops) {
        if (slices < 2 || loops < 1) return;
        float da = 2.0F * (float) Math.PI / slices;
        float dr = (outerRadius - innerRadius) / loops;
        boolean outside = orientation == 100020;
        if (normals != 100002) glNormal3f(0.0F, 0.0F, outside ? 1.0F : -1.0F);

        if (drawStyle == 100012) { // GLU_FILL
            for (int l = 0; l < loops; l++) {
                float r1 = innerRadius + l * dr;
                float r2 = r1 + dr;
                glBegin(GL_QUAD_STRIP);
                for (int i = 0; i <= slices; i++) {
                    float a = i == slices ? 0.0F : (outside ? i * da : -i * da);
                    float sa = (float) Math.sin(a);
                    float ca = (float) Math.cos(a);
                    if (textureFlag) glTexCoord2f(0.5F + sa * r2 / outerRadius * 0.5F, 0.5F + ca * r2 / outerRadius * 0.5F);
                    glVertex3f(r2 * sa, r2 * ca, 0.0F);
                    if (textureFlag) glTexCoord2f(0.5F + sa * r1 / outerRadius * 0.5F, 0.5F + ca * r1 / outerRadius * 0.5F);
                    glVertex3f(r1 * sa, r1 * ca, 0.0F);
                }
                glEnd();
            }
        } else if (drawStyle == 100011) { // GLU_LINE: rings and spokes
            for (int l = 0; l <= loops; l++) {
                float r = innerRadius + l * dr;
                glBegin(GL_LINE_LOOP);
                for (int i = 0; i < slices; i++) {
                    float a = i * da;
                    glVertex3f(r * (float) Math.sin(a), r * (float) Math.cos(a), 0.0F);
                }
                glEnd();
            }
            glBegin(GL_LINES);
            for (int i = 0; i < slices; i++) {
                float sa = (float) Math.sin(i * da);
                float ca = (float) Math.cos(i * da);
                glVertex3f(innerRadius * sa, innerRadius * ca, 0.0F);
                glVertex3f(outerRadius * sa, outerRadius * ca, 0.0F);
            }
            glEnd();
        } else if (drawStyle == 100013) { // GLU_SILHOUETTE: the edge circles
            for (float r : new float[] {innerRadius, outerRadius}) {
                if (r == 0.0F) continue;
                glBegin(GL_LINE_LOOP);
                for (int i = 0; i < slices; i++) {
                    float a = i * da;
                    glVertex3f(r * (float) Math.sin(a), r * (float) Math.cos(a), 0.0F);
                }
                glEnd();
            }
        } else { // GLU_POINT
            glBegin(GL_POINTS);
            for (int i = 0; i < slices; i++) {
                float sa = (float) Math.sin(i * da);
                float ca = (float) Math.cos(i * da);
                for (int l = 0; l <= loops; l++) {
                    float r = innerRadius + l * dr;
                    glVertex3f(r * sa, r * ca, 0.0F);
                }
            }
            glEnd();
        }
    }
}
