package legacyconnector.v1_7_10.org.lwjgl.util;

import java.io.Serializable;

/** LWJGL 2's integer rectangle (x, y, width, height), used for GUI hit tests. */
public final class Rectangle implements ReadableRectangle, Serializable {
    private int x;
    private int y;
    private int width;
    private int height;

    public Rectangle() {
    }

    public Rectangle(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public Rectangle(ReadableRectangle r) {
        this(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setLocation(int x, int y) {
        this.x = x;
        this.y = y;
    }

    public void setSize(int w, int h) {
        this.width = w;
        this.height = h;
    }

    public void setBounds(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.width = w;
        this.height = h;
    }

    public void setX(int x) {
        this.x = x;
    }

    public void setY(int y) {
        this.y = y;
    }

    public void setWidth(int w) {
        this.width = w;
    }

    public void setHeight(int h) {
        this.height = h;
    }

    public void getBounds(Rectangle dest) {
        dest.setBounds(x, y, width, height);
    }

    public boolean contains(int px, int py) {
        return px >= x && py >= y && px < x + width && py < y + height;
    }

    public boolean contains(Point p) {
        return contains(p.getX(), p.getY());
    }

    public boolean intersects(ReadableRectangle r) {
        return r.getX() < x + width && r.getX() + r.getWidth() > x && r.getY() < y + height && r.getY() + r.getHeight() > y;
    }

    public boolean isEmpty() {
        return width <= 0 || height <= 0;
    }

    public boolean equals(Object o) {
        return o instanceof Rectangle r && r.x == x && r.y == y && r.width == width && r.height == height;
    }

    public int hashCode() {
        return ((x * 31 + y) * 31 + width) * 31 + height;
    }

    public String toString() {
        return getClass().getName() + "[x=" + x + ",y=" + y + ",width=" + width + ",height=" + height + "]";
    }
}
