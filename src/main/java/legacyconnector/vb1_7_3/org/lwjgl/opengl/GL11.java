package legacyconnector.vb1_7_3.org.lwjgl.opengl;

import org.kylepitcock.legacyconnector.vb1_7_3.GuiDrawB173;
import org.kylepitcock.legacyconnector.vb1_7_3.RendererContextB173;

/**
 * The GL 1.1 calls b1.7.3 GUIs make: colour, texture binding and the matrix stack go to the screen being drawn ({@link GuiDrawB173}); state
 * switches (lighting, depth, blending) are captured for native entity draw pipelines. The real GL11
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
        // A texture effect that binds its own sheet through GL (BuildCraft's oil) names the sheet it draws into.
        if(org.kylepitcock.legacyconnector.vb1_7_3.AnimationsB173.bindId(texture))return;
        if(RendererContextB173.active())RendererContextB173.bind(texture);else GuiDrawB173.bind(texture);
    }

    // ---------------------------------------------------------------------------------------------------- display lists
    // A list holds the geometry a mod's code drew between glNewList and glEndList (BuildCraft builds its pipe item cubes this way); a call
    // draws it again through the Tessellator, in whatever transform and texture are current.
    private static final java.util.Map<Integer, java.util.List<legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.Vertex>> LISTS
            = new java.util.concurrent.ConcurrentHashMap<>();
    private static final java.util.concurrent.atomic.AtomicInteger NEXT_LIST = new java.util.concurrent.atomic.AtomicInteger(1);
    private static final ThreadLocal<Integer> COMPILING = new ThreadLocal<>();

    public static int glGenLists(int range) { return NEXT_LIST.getAndAdd(Math.max(1, range)); }

    public static void glNewList(int list, int mode) {
        COMPILING.set(list);
        legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.beginCapture();
    }

    public static void glEndList() {
        Integer list = COMPILING.get();
        COMPILING.remove();
        var vertices = legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.endCapture();
        if (list != null) LISTS.put(list, vertices);
    }

    public static void glCallList(int list) {
        var vertices = LISTS.get(list);
        if (vertices == null) return;
        var tessellator = legacyconnector.vb1_7_3.net.minecraft.client.render.Tessellator.INSTANCE;
        for (int q = 0; q + 3 < vertices.size(); q += 4) {
            tessellator.startQuads();
            for (int i = 0; i < 4; i++) {
                var v = vertices.get(q + i);
                int c = v.color();
                tessellator.color(c & 255, (c >> 8) & 255, (c >> 16) & 255, (c >>> 24) & 255);
                tessellator.vertex(v.x(), v.y(), v.z(), v.u(), v.v());
            }
            tessellator.draw();
        }
    }

    public static void glDeleteLists(int list, int range) {
        for (int i = 0; i < range; i++) LISTS.remove(list + i);
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

    public static void glEnable(int cap) {if(RendererContextB173.active())RendererContextB173.enable(cap,true);else if(cap==3553)GuiDrawB173.texture2d(true);}

    public static void glDisable(int cap) {if(RendererContextB173.active())RendererContextB173.enable(cap,false);else if(cap==3553)GuiDrawB173.texture2d(false);}

    public static void glBlendFunc(int src, int dst) {if(RendererContextB173.active())RendererContextB173.blendFunc(src,dst);}

    public static void glAlphaFunc(int func, float ref) {}

    public static void glDepthMask(boolean flag) {if(RendererContextB173.active())RendererContextB173.depthMask(flag);}
    public static void glDepthFunc(int func) {if(RendererContextB173.active())RendererContextB173.depthFunc(func);}
    public static void glMatrixMode(int mode) {if(RendererContextB173.active())RendererContextB173.matrixMode(mode);else GuiDrawB173.matrixMode(mode);}
    public static void glLoadIdentity() {if(RendererContextB173.active())RendererContextB173.loadIdentity();else GuiDrawB173.loadIdentity();}

    /** A GUI's orthographic projection maps its coordinates onto the screen; a model renderer's is ignored. */
    public static void glOrtho(double left,double right,double bottom,double top,double near,double far) {if(!RendererContextB173.active())GuiDrawB173.ortho(left,right,bottom,top);}
    public static void glClear(int mask) {}
    public static void glViewport(int x,int y,int width,int height) {}

    public static void glShadeModel(int mode) {}

    public static void glNormal3f(float x, float y, float z) {if(RendererContextB173.active())RendererContextB173.normal(x,y,z);}
    public static void glPushAttrib(int mask) {if(RendererContextB173.active())RendererContextB173.pushCurrentAttributes(mask);}
    public static void glPopAttrib() {if(RendererContextB173.active())RendererContextB173.popCurrentAttributes();}

    public static void glRotatef(float angle, float x, float y, float z) {if(RendererContextB173.active())RendererContextB173.rotate(angle,x,y,z);else GuiDrawB173.rotate(angle,x,y,z);}

    public static void glTexParameteri(int target, int name, int value) {}

    public static int glGenTextures() {
        return 0;
    }

    public static void glDeleteTextures(int texture) {}

    public static int glGetError() {
        return 0;
    }
}
