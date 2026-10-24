package legacyconnector.v1_7_10.org.lwjgl.util.glu;

import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glBegin;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glEnd;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glNormal3f;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glTexCoord2f;
import static legacyconnector.v1_7_10.org.lwjgl.opengl.GL11.glVertex3f;

/** GLU sphere in immediate mode (the port of LWJGL 2's Sphere): triangle fans at the poles, quad strips between. */
public class Sphere extends Quadric {
    private static final int GL_LINE_LOOP = 2;
    private static final int GL_QUAD_STRIP = 8;
    private static final int GL_TRIANGLE_FAN = 6;

    public void draw(float radius, int slices, int stacks) {
        boolean normalsOut = orientation == 100020;
        float nsign = normalsOut ? 1.0F : -1.0F;
        float drho = (float) Math.PI / stacks;
        float dtheta = 2.0F * (float) Math.PI / slices;

        if (drawStyle == 100012) { // GLU_FILL
            if (!textureFlag) {
                glBegin(GL_TRIANGLE_FAN);
                glNormal3f(0.0F, 0.0F, 1.0F);
                glVertex3f(0.0F, 0.0F, nsign * radius);
                for (int j = 0; j <= slices; j++) {
                    float theta = j == slices ? 0.0F : j * dtheta;
                    float x = -(float) Math.sin(theta) * (float) Math.sin(drho);
                    float y = (float) Math.cos(theta) * (float) Math.sin(drho);
                    float z = nsign * (float) Math.cos(drho);
                    normal3f(x * nsign, y * nsign, z * nsign);
                    glVertex3f(x * radius, y * radius, z * radius);
                }
                glEnd();
            }
            float ds = 1.0F / slices;
            float dt = 1.0F / stacks;
            float t = 1.0F;
            int imin = textureFlag ? 0 : 1;
            int imax = textureFlag ? stacks : stacks - 1;
            for (int i = imin; i < imax; i++) {
                float rho = i * drho;
                glBegin(GL_QUAD_STRIP);
                float s = 0.0F;
                for (int j = 0; j <= slices; j++) {
                    float theta = j == slices ? 0.0F : j * dtheta;
                    float x = -(float) Math.sin(theta) * (float) Math.sin(rho);
                    float y = (float) Math.cos(theta) * (float) Math.sin(rho);
                    float z = nsign * (float) Math.cos(rho);
                    normal3f(x * nsign, y * nsign, z * nsign);
                    if (textureFlag) glTexCoord2f(s, t);
                    glVertex3f(x * radius, y * radius, z * radius);
                    x = -(float) Math.sin(theta) * (float) Math.sin(rho + drho);
                    y = (float) Math.cos(theta) * (float) Math.sin(rho + drho);
                    z = nsign * (float) Math.cos(rho + drho);
                    normal3f(x * nsign, y * nsign, z * nsign);
                    if (textureFlag) glTexCoord2f(s, t - dt);
                    s += ds;
                    glVertex3f(x * radius, y * radius, z * radius);
                }
                glEnd();
                t -= dt;
            }
            if (!textureFlag) {
                glBegin(GL_TRIANGLE_FAN);
                glNormal3f(0.0F, 0.0F, -1.0F);
                glVertex3f(0.0F, 0.0F, -radius * nsign);
                float rho = (float) Math.PI - drho;
                for (int j = slices; j >= 0; j--) {
                    float theta = j == slices ? 0.0F : j * dtheta;
                    float x = -(float) Math.sin(theta) * (float) Math.sin(rho);
                    float y = (float) Math.cos(theta) * (float) Math.sin(rho);
                    float z = nsign * (float) Math.cos(rho);
                    normal3f(x * nsign, y * nsign, z * nsign);
                    glVertex3f(x * radius, y * radius, z * radius);
                }
                glEnd();
            }
        } else {
            // GLU_LINE, GLU_SILHOUETTE, GLU_POINT: latitude rings
            for (int i = 1; i < stacks; i++) {
                float rho = i * drho;
                glBegin(GL_LINE_LOOP);
                for (int j = 0; j < slices; j++) {
                    float theta = j * dtheta;
                    float x = -(float) Math.sin(theta) * (float) Math.sin(rho);
                    float y = (float) Math.cos(theta) * (float) Math.sin(rho);
                    float z = nsign * (float) Math.cos(rho);
                    normal3f(x * nsign, y * nsign, z * nsign);
                    glVertex3f(x * radius, y * radius, z * radius);
                }
                glEnd();
            }
        }
    }
}
