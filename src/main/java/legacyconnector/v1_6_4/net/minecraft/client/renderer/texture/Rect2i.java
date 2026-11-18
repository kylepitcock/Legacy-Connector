package legacyconnector.v1_6_4.net.minecraft.client.renderer.texture;

/** Minecraft 1.5.2's {@code Rect2i}: an integer rectangle of a texture sheet (the stitcher's slot of a sprite). */
public class Rect2i {
    private int rectX;
    private int rectY;
    private int rectWidth;
    private int rectHeight;

    public Rect2i() {
        this(0, 0, 0, 0);
    }

    public Rect2i(int x, int y, int width, int height) {
        this.rectX = x;
        this.rectY = y;
        this.rectWidth = width;
        this.rectHeight = height;
    }

    /** The overlap of this rectangle and another (an empty rectangle at this one's origin when they do not touch). */
    public Rect2i intersection(Rect2i other) {
        int left = Math.max(rectX, other.rectX);
        int top = Math.max(rectY, other.rectY);
        int right = Math.min(rectX + rectWidth, other.rectX + other.rectWidth);
        int bottom = Math.min(rectY + rectHeight, other.rectY + other.rectHeight);
        if (right <= left || bottom <= top) return new Rect2i(rectX, rectY, 0, 0);
        return new Rect2i(left, top, right - left, bottom - top);
    }

    public int getRectX() {
        return rectX;
    }

    public int getRectY() {
        return rectY;
    }

    public int getRectWidth() {
        return rectWidth;
    }

    public int getRectHeight() {
        return rectHeight;
    }

    public void setRect(int x, int y, int width, int height) {
        this.rectX = x;
        this.rectY = y;
        this.rectWidth = width;
        this.rectHeight = height;
    }

    @Override
    public String toString() {
        return "Rect2i{" + rectX + "," + rectY + " " + rectWidth + "x" + rectHeight + "}";
    }
}
