package legacyconnector.v1_7_10.codechicken.lib.vec;

/** CodeChickenLib's integer rectangle (x, y, w, h), used by NEI-style GUI code for hit tests. */
public class Rectangle4i {
    public int x;
    public int y;
    public int w;
    public int h;

    public Rectangle4i() {
    }

    public Rectangle4i(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }

    public boolean contains(int px, int py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    public boolean intersects(Rectangle4i r) {
        return r.x < x + w && r.x + r.w > x && r.y < y + h && r.y + r.h > y;
    }

    public Rectangle4i offset(int dx, int dy) {
        return new Rectangle4i(x + dx, y + dy, w, h);
    }

    public Rectangle4i copy() {
        return new Rectangle4i(x, y, w, h);
    }

    public int x1() {
        return x;
    }

    public int y1() {
        return y;
    }

    public int x2() {
        return x + w;
    }

    public int y2() {
        return y + h;
    }

    public boolean equals(Object o) {
        return o instanceof Rectangle4i r && r.x == x && r.y == y && r.w == w && r.h == h;
    }

    public int hashCode() {
        return ((x * 31 + y) * 31 + w) * 31 + h;
    }

    public String toString() {
        return "Rectangle4i(" + x + ", " + y + ", " + w + ", " + h + ")";
    }
}
