package legacyconnector.v1_7_10.org.lwjgl.util;

import java.io.Serializable;

/** LWJGL 2's integer size ({@code org.lwjgl.util.Dimension}: a width and a height; Forge's display and texture code passes it around). */
public final class Dimension implements Serializable {
    private int width;
    private int height;

    public Dimension() {
    }

    public Dimension(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public Dimension(Dimension d) {
        this.width = d.width;
        this.height = d.height;
    }

    public void setSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public void setSize(Dimension d) {
        this.width = d.width;
        this.height = d.height;
    }

    /** Copies the size into another dimension. */
    public void getSize(Dimension dest) {
        dest.width = width;
        dest.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Dimension d && d.width == width && d.height == height;
    }

    @Override
    public int hashCode() {
        return width * 31 + height;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[width=" + width + ",height=" + height + "]";
    }
}
