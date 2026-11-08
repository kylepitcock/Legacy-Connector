package legacyconnector.v1_7_10.codechicken.lib.vec;

import legacyconnector.v1_7_10.org.lwjgl.opengl.GL11;

/** A rotation by an angle (radians) around an axis; also holds the helper tables for the six block sides. */
public class Rotation extends Transformation {
    public double angle;
    public Vector3 axis;

    /** The direction of each side (0 down, 1 up, 2 north, 3 south, 4 west, 5 east). */
    public static Vector3[] axes = new Vector3[] {
        new Vector3(0, -1, 0), new Vector3(0, 1, 0), new Vector3(0, 0, -1), new Vector3(0, 0, 1), new Vector3(-1, 0, 0), new Vector3(1, 0, 0)};

    /** Rotates the bottom face onto each side. */
    public static Transformation[] sideRotations = new Transformation[] {
        new RedundantTransformation(),
        new Rotation(Math.PI, new Vector3(1, 0, 0)),
        new Rotation(-Math.PI / 2, new Vector3(1, 0, 0)),
        new Rotation(Math.PI / 2, new Vector3(1, 0, 0)),
        new Rotation(Math.PI / 2, new Vector3(0, 0, 1)),
        new Rotation(-Math.PI / 2, new Vector3(0, 0, 1))};

    /** Quarter turns around the vertical axis. */
    public static Transformation[] quarterRotations = new Transformation[] {
        new RedundantTransformation(),
        new Rotation(-Math.PI / 2, new Vector3(0, 1, 0)),
        new Rotation(Math.PI, new Vector3(0, 1, 0)),
        new Rotation(Math.PI / 2, new Vector3(0, 1, 0))};

    /** The side that side s turns to after r quarter turns around the vertical axis (rows by side, columns by r); the vertical sides stay. */
    public static int[] rotSideMap = new int[] {
        0, 0, 0, 0,
        1, 1, 1, 1,
        2, 5, 3, 4,
        3, 4, 2, 5,
        4, 2, 5, 3,
        5, 3, 4, 2};

    public Rotation(double angle, Vector3 axis) {
        this.angle = angle;
        this.axis = axis;
    }

    public Rotation(double angle, double x, double y, double z) {
        this(angle, new Vector3(x, y, z));
    }

    /** The rotation that turns the bottom face (side 0) to the side s, then r quarter turns around it. */
    public static Transformation sideOrientation(int s, int r) {
        return sideRotations[s].with(quarterRotations[r & 3]);
    }

    /** The side s after r quarter turns around the vertical axis. */
    public static int rotateSide(int s, int r) {
        return rotSideMap[s * 4 + (r & 3)];
    }

    public void apply(Vector3 v) {
        v.rotate(angle, axis);
    }

    public void apply(Matrix4 mat) {
        mat.rotate(angle, axis);
    }

    public Rotation inverse() {
        return new Rotation(-angle, axis);
    }

    public boolean isRedundant() {
        return angle == 0;
    }

    public Transformation merge(Transformation next) {
        return next instanceof Rotation r && r.axis.equals(axis) ? new Rotation(angle + r.angle, axis) : null;
    }

    public void glApply() {
        GL11.glRotated(angle * 180.0 / Math.PI, axis.x, axis.y, axis.z);
    }

    public String toString() {
        return "Rotation(" + angle + ", " + axis + ")";
    }
}
