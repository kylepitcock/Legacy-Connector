package legacyconnector.v1_12_2.org.lwjgl.util;

import java.io.Serializable;

/** LWJGL 2's integer rectangle (java.awt.Rectangle's semantics). */
public final class Rectangle implements ReadableRectangle, Serializable {
    private int x, y, width, height;

    public Rectangle() {
    }

    public Rectangle(int x, int y, int w, int h) {
        setBounds(x, y, w, h);
    }

    public Rectangle(ReadableRectangle r) {
        setBounds(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int w, int h) {
        width = w;
        height = h;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        width = w;
        height = h;
    }

    public void setBounds(ReadableRectangle r) {
        setBounds(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getWidth() {
        return width;
    }

    @Override
    public int getHeight() {
        return height;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int w) {
        width = w;
    }

    public void setHeight(int h) {
        height = h;
    }

    public void translate(int dx, int dy) {
        x += dx;
        y += dy;
    }

    public boolean contains(int px, int py) {
        int w = width, h = height;
        if ((w | h) < 0) return false;
        if (px < x || py < y) return false;
        w += x;
        h += y;
        return (w < x || w > px) && (h < y || h > py);
    }

    public boolean contains(ReadableRectangle r) {
        return contains(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public boolean contains(int X, int Y, int W, int H) {
        int w = width, h = height;
        if ((w | h | W | H) < 0) return false;
        if (X < x || Y < y) return false;
        w += x;
        W += X;
        if (W <= X) {
            if (w >= x || W > w) return false;
        } else if (w >= x && W > w) {
            return false;
        }
        h += y;
        H += Y;
        if (H <= Y) {
            return h < y && H <= h;
        }
        return h < y || H <= h;
    }

    public boolean intersects(ReadableRectangle r) {
        int tw = width, th = height, rw = r.getWidth(), rh = r.getHeight();
        if (rw <= 0 || rh <= 0 || tw <= 0 || th <= 0) return false;
        int tx = x, ty = y, rx = r.getX(), ry = r.getY();
        rw += rx;
        rh += ry;
        tw += tx;
        th += ty;
        return (rw < rx || rw > tx) && (rh < ry || rh > ty) && (tw < tx || tw > rx) && (th < ty || th > ry);
    }

    public Rectangle intersection(ReadableRectangle r, Rectangle dest) {
        int x1 = Math.max(x, r.getX()), y1 = Math.max(y, r.getY());
        int x2 = Math.min(x + width, r.getX() + r.getWidth()), y2 = Math.min(y + height, r.getY() + r.getHeight());
        if (dest == null) dest = new Rectangle();
        dest.setBounds(x1, y1, x2 - x1, y2 - y1);
        return dest;
    }

    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Rectangle r && r.x == x && r.y == y && r.width == width && r.height == height;
    }

    @Override
    public int hashCode() {
        return ((x * 31 + y) * 31 + width) * 31 + height;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
