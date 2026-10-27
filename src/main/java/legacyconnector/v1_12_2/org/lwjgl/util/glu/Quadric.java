package legacyconnector.v1_12_2.org.lwjgl.util.glu;

/** GLU quadric state shared by {@link Sphere}; the constants are GLU's. */
public class Quadric {
    protected int drawStyle = 100012; // GLU_FILL
    protected int orientation = 100020; // GLU_OUTSIDE
    protected boolean textureFlag;
    protected int normals = 100000; // GLU_SMOOTH

    public void setDrawStyle(int style) {
        drawStyle = style;
    }

    public int getDrawStyle() {
        return drawStyle;
    }

    public void setNormals(int mode) {
        normals = mode;
    }

    public int getNormals() {
        return normals;
    }

    public void setOrientation(int orientation) {
        this.orientation = orientation;
    }

    public int getOrientation() {
        return orientation;
    }

    public void setTextureFlag(boolean flag) {
        textureFlag = flag;
    }

    public boolean getTextureFlag() {
        return textureFlag;
    }

    protected void normal3f(float x, float y, float z) {
        if (normals == 100002) return; // GLU_NONE
        float len = (float) Math.sqrt(x * x + y * y + z * z);
        if (len > 0) {
            x /= len;
            y /= len;
            z /= len;
        }
        legacyconnector.v1_12_2.org.lwjgl.opengl.GL11.glNormal3f(x, y, z);
    }
}
