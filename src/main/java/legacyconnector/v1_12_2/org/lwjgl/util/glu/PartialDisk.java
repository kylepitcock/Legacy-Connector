package legacyconnector.v1_12_2.org.lwjgl.util.glu;

import static legacyconnector.v1_12_2.org.lwjgl.opengl.GL11.glBegin;
import static legacyconnector.v1_12_2.org.lwjgl.opengl.GL11.glEnd;
import static legacyconnector.v1_12_2.org.lwjgl.opengl.GL11.glNormal3f;
import static legacyconnector.v1_12_2.org.lwjgl.opengl.GL11.glTexCoord2f;
import static legacyconnector.v1_12_2.org.lwjgl.opengl.GL11.glVertex3f;

/** GLU partial disk (a ring sector in the z = 0 plane, angles in degrees clockwise from +y) in immediate mode, like LWJGL 2's PartialDisk. */
public class PartialDisk extends Quadric {
    private static final int GL_POINTS = 0;
    private static final int GL_LINES = 1;
    private static final int GL_LINE_STRIP = 3;
    private static final int GL_QUAD_STRIP = 8;

    public PartialDisk() {
    }

    public void draw(float innerRadius, float outerRadius, int slices, int loops, float startAngle, float sweepAngle) {
        if (slices < 2 || loops < 1 || outerRadius <= 0.0F || innerRadius < 0.0F || innerRadius > outerRadius) return;
        if (sweepAngle < -360.0F) sweepAngle = 360.0F;
        if (sweepAngle > 360.0F) sweepAngle = 360.0F;
        if (sweepAngle < 0) {
            startAngle += sweepAngle;
            sweepAngle = -sweepAngle;
        }
        boolean full = sweepAngle == 360.0F;
        int steps = full ? slices : slices + 1;
        float angleStep = (float) Math.toRadians(sweepAngle) / slices;
        float start = (float) Math.toRadians(startAngle);
        float dr = (outerRadius - innerRadius) / loops;
        boolean outside = orientation == 100020;
        if (normals != 100002) glNormal3f(0.0F, 0.0F, outside ? 1.0F : -1.0F);

        if (drawStyle == 100012) { // GLU_FILL
            for (int l = 0; l < loops; l++) {
                float r1 = innerRadius + l * dr;
                float r2 = r1 + dr;
                glBegin(GL_QUAD_STRIP);
                for (int i = 0; i <= slices; i++) {
                    float a = outside ? start + i * angleStep : start + sweepRad(sweepAngle) - i * angleStep;
                    float sa = (float) Math.sin(a);
                    float ca = (float) Math.cos(a);
                    if (textureFlag) glTexCoord2f(0.5F + sa * r2 / outerRadius * 0.5F, 0.5F + ca * r2 / outerRadius * 0.5F);
                    glVertex3f(r2 * sa, r2 * ca, 0.0F);
                    if (textureFlag) glTexCoord2f(0.5F + sa * r1 / outerRadius * 0.5F, 0.5F + ca * r1 / outerRadius * 0.5F);
                    glVertex3f(r1 * sa, r1 * ca, 0.0F);
                }
                glEnd();
            }
        } else if (drawStyle == 100010) { // GLU_POINT
            glBegin(GL_POINTS);
            for (int i = 0; i < steps; i++) {
                float sa = (float) Math.sin(start + i * angleStep);
                float ca = (float) Math.cos(start + i * angleStep);
                for (int l = 0; l <= loops; l++) {
                    float r = innerRadius + l * dr;
                    glVertex3f(r * sa, r * ca, 0.0F);
                }
            }
            glEnd();
        } else if (drawStyle == 100011) { // GLU_LINE: arcs and spokes
            for (int l = 0; l <= loops; l++) {
                float r = innerRadius + l * dr;
                glBegin(GL_LINE_STRIP);
                for (int i = 0; i <= slices; i++) {
                    float a = start + i * angleStep;
                    glVertex3f(r * (float) Math.sin(a), r * (float) Math.cos(a), 0.0F);
                }
                glEnd();
            }
            glBegin(GL_LINES);
            for (int i = 0; i < steps; i++) {
                float sa = (float) Math.sin(start + i * angleStep);
                float ca = (float) Math.cos(start + i * angleStep);
                glVertex3f(innerRadius * sa, innerRadius * ca, 0.0F);
                glVertex3f(outerRadius * sa, outerRadius * ca, 0.0F);
            }
            glEnd();
        } else { // GLU_SILHOUETTE: the two edge arcs and, for a sector, its two straight edges
            for (float r : new float[] {innerRadius, outerRadius}) {
                if (r == 0.0F) continue;
                glBegin(GL_LINE_STRIP);
                for (int i = 0; i <= slices; i++) {
                    float a = start + i * angleStep;
                    glVertex3f(r * (float) Math.sin(a), r * (float) Math.cos(a), 0.0F);
                }
                glEnd();
            }
            if (!full) {
                glBegin(GL_LINES);
                float end = start + (float) Math.toRadians(sweepAngle);
                glVertex3f(innerRadius * (float) Math.sin(start), innerRadius * (float) Math.cos(start), 0.0F);
                glVertex3f(outerRadius * (float) Math.sin(start), outerRadius * (float) Math.cos(start), 0.0F);
                glVertex3f(innerRadius * (float) Math.sin(end), innerRadius * (float) Math.cos(end), 0.0F);
                glVertex3f(outerRadius * (float) Math.sin(end), outerRadius * (float) Math.cos(end), 0.0F);
                glEnd();
            }
        }
    }

    private static float sweepRad(float degrees) {
        return (float) Math.toRadians(degrees);
    }
}
