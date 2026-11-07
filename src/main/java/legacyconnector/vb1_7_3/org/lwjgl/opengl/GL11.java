package legacyconnector.vb1_7_3.org.lwjgl.opengl;

import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/**
 * The GL 1.1 calls b1.7.3 GUIs make: colour, texture binding and the matrix stack go to the screen being drawn ({@link GuiDrawB173}); state
 * switches (lighting, depth, blending) are left to the modern renderer. Calls not written here are synthesized and do nothing. The real GL11
 * is never called: the modern game runs a core profile in which these immediate-mode functions do not exist.
 */
public final class GL11 {
    private GL11() {}

    public static void glColor4f(float r, float g, float b, float a) {
        if(RendererContextB173.active())RendererContextB173.color(r,g,b,a);else GuiDrawB173.color(r, g, b, a);
    }

    public static void glColor3f(float r, float g, float b) {
        if(RendererContextB173.active())RendererContextB173.color(r,g,b,1);else GuiDrawB173.color(r, g, b, 1.0F);
    }

    public static void glBindTexture(int target, int texture) {
        GuiDrawB173.bind(texture);
    }

    public static void glPushMatrix() {
        if(RendererContextB173.active())RendererContextB173.push();else GuiDrawB173.push();
    }

    public static void glPopMatrix() {
        if(RendererContextB173.active())RendererContextB173.pop();else GuiDrawB173.pop();
    }

    public static void glTranslatef(float x, float y, float z) {
        if(RendererContextB173.active())RendererContextB173.translate(x,y,z);else GuiDrawB173.translate(x, y, z);
    }

    public static void glTranslated(double x, double y, double z) {
        if(RendererContextB173.active())RendererContextB173.translate(x,y,z);else GuiDrawB173.translate((float) x, (float) y, (float) z);
    }

    public static void glScalef(float x, float y, float z) {
        if(RendererContextB173.active())RendererContextB173.scale(x,y,z);else GuiDrawB173.scale(x, y, z);
    }

    public static void glScaled(double x, double y, double z) {
        if(RendererContextB173.active())RendererContextB173.scale((float)x,(float)y,(float)z);else GuiDrawB173.scale((float) x, (float) y, (float) z);
    }

    public static void glEnable(int cap) {}

    public static void glDisable(int cap) {}

    public static void glBlendFunc(int src, int dst) {}

    public static void glAlphaFunc(int func, float ref) {}

    public static void glDepthMask(boolean flag) {}

    public static void glShadeModel(int mode) {}

    public static void glNormal3f(float x, float y, float z) {}

    public static void glRotatef(float angle, float x, float y, float z) {if(RendererContextB173.active())RendererContextB173.rotate(angle,x,y,z);}

    public static void glTexParameteri(int target, int name, int value) {}

    public static int glGenTextures() {
        return 0;
    }

    public static void glDeleteTextures(int texture) {}

    public static int glGetError() {
        return 0;
    }
}
