package legacyconnector.v1_7_10.org.lwjgl.util.glu.tessellation;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_7_10.org.lwjgl.util.glu.GLU;
import legacyconnector.v1_7_10.org.lwjgl.util.glu.GLUtessellator;
import legacyconnector.v1_7_10.org.lwjgl.util.glu.GLUtessellatorCallback;
import legacyconnector.v1_7_10.org.lwjgl.util.glu.GLUtessellatorCallbackAdapter;

/**
 * Triangulates polygons for the GLU tessellation API. Each contour is cut into triangles by ear clipping and reported as one
 * GL_TRIANGLES run through the begin, vertex and end callbacks. The winding rule and the boundary-only property are stored but not
 * applied: contours are filled on their own (holes and overlaps are not resolved, so {@code combine} is never needed).
 */
public class GLUtessellatorImpl implements GLUtessellator {
    private static final GLUtessellatorCallback NONE = new GLUtessellatorCallbackAdapter();
    private static final int GL_TRIANGLES = 4;

    private GLUtessellatorCallback callback = NONE;
    private double windingRule = GLU.GLU_TESS_WINDING_ODD;
    private double boundaryOnly;
    private double tolerance;
    private double nx;
    private double ny;
    private double nz;
    private Object polygonData;
    private boolean inPolygon;
    private boolean inContour;
    private List<double[]> coords = new ArrayList<>();
    private List<Object> datas = new ArrayList<>();
    private final List<List<double[]>> contourCoords = new ArrayList<>();
    private final List<List<Object>> contourDatas = new ArrayList<>();

    public static GLUtessellator gluNewTess() {
        return new GLUtessellatorImpl();
    }

    @Override
    public void gluDeleteTess() {
        callback = NONE;
        contourCoords.clear();
        contourDatas.clear();
    }

    @Override
    public void gluTessProperty(int which, double value) {
        switch (which) {
            case GLU.GLU_TESS_WINDING_RULE -> windingRule = value;
            case GLU.GLU_TESS_BOUNDARY_ONLY -> boundaryOnly = value;
            case GLU.GLU_TESS_TOLERANCE -> tolerance = value;
            default -> callback.error(GLU.GLU_INVALID_ENUM);
        }
    }

    @Override
    public void gluGetTessProperty(int which, double[] value, int offset) {
        switch (which) {
            case GLU.GLU_TESS_WINDING_RULE -> value[offset] = windingRule;
            case GLU.GLU_TESS_BOUNDARY_ONLY -> value[offset] = boundaryOnly;
            case GLU.GLU_TESS_TOLERANCE -> value[offset] = tolerance;
            default -> callback.error(GLU.GLU_INVALID_ENUM);
        }
    }

    @Override
    public void gluTessNormal(double x, double y, double z) {
        nx = x;
        ny = y;
        nz = z;
    }

    /** LWJGL 2 takes one receiver object for every callback kind; a null one resets it to doing nothing. */
    @Override
    public void gluTessCallback(int which, GLUtessellatorCallback cb) {
        callback = cb == null ? NONE : cb;
    }

    @Override
    public void gluTessBeginPolygon(Object data) {
        inPolygon = true;
        polygonData = data;
        contourCoords.clear();
        contourDatas.clear();
    }

    @Override
    public void gluTessBeginContour() {
        if (!inPolygon) gluTessBeginPolygon(null);
        if (inContour) gluTessEndContour();
        inContour = true;
        coords = new ArrayList<>();
        datas = new ArrayList<>();
    }

    @Override
    public void gluTessVertex(double[] c, int offset, Object data) {
        if (!inContour) gluTessBeginContour();
        coords.add(new double[] {c[offset], c[offset + 1], c[offset + 2]});
        datas.add(data);
    }

    @Override
    public void gluTessEndContour() {
        if (!inContour) return;
        inContour = false;
        if (coords.size() >= 3) {
            contourCoords.add(coords);
            contourDatas.add(datas);
        }
    }

    @Override
    public void gluTessEndPolygon() {
        if (inContour) gluTessEndContour();
        inPolygon = false;
        if (!contourCoords.isEmpty()) {
            callback.begin(GL_TRIANGLES);
            callback.beginData(GL_TRIANGLES, polygonData);
            for (int c = 0; c < contourCoords.size(); c++) triangulate(contourCoords.get(c), contourDatas.get(c));
            callback.end();
            callback.endData(polygonData);
        }
        contourCoords.clear();
        contourDatas.clear();
    }

    @Override
    public void gluBeginPolygon() {
        gluTessBeginPolygon(null);
        gluTessBeginContour();
    }

    @Override
    public void gluNextContour(int type) {
        gluTessEndContour();
        gluTessBeginContour();
    }

    @Override
    public void gluEndPolygon() {
        gluTessEndContour();
        gluTessEndPolygon();
    }

    /** Ear clipping in the plane dropped along the dominant axis of the contour normal. */
    private void triangulate(List<double[]> pts, List<Object> data) {
        int n = pts.size();
        double ax = nx;
        double ay = ny;
        double az = nz;
        if (ax == 0.0D && ay == 0.0D && az == 0.0D) {
            // Newell's method for the normal of the contour
            for (int i = 0; i < n; i++) {
                double[] p = pts.get(i);
                double[] q = pts.get((i + 1) % n);
                ax += (p[1] - q[1]) * (p[2] + q[2]);
                ay += (p[2] - q[2]) * (p[0] + q[0]);
                az += (p[0] - q[0]) * (p[1] + q[1]);
            }
        }
        double abx = Math.abs(ax);
        double aby = Math.abs(ay);
        double abz = Math.abs(az);
        int drop = abx >= aby && abx >= abz ? 0 : aby >= abz ? 1 : 2;
        // (u, v, dropped axis) is right-handed, so counter-clockwise in (u, v) faces along +axis
        int u = (drop + 1) % 3;
        int v = (drop + 2) % 3;
        boolean facesNegative = (drop == 0 ? ax : drop == 1 ? ay : az) < 0.0D;
        double area = 0.0D;
        for (int i = 0; i < n; i++) {
            double[] p = pts.get(i);
            double[] q = pts.get((i + 1) % n);
            area += p[u] * q[v] - q[u] * p[v];
        }
        List<Integer> idx = new ArrayList<>();
        for (int i = 0; i < n; i++) idx.add(area < 0.0D ? n - 1 - i : i);
        int guard = 0;
        while (idx.size() > 3 && guard++ < n * n) {
            boolean clipped = false;
            for (int i = 0; i < idx.size(); i++) {
                int a = idx.get((i + idx.size() - 1) % idx.size());
                int b = idx.get(i);
                int c = idx.get((i + 1) % idx.size());
                if (!isEar(pts, idx, a, b, c, u, v)) continue;
                emitTriangle(data, a, b, c, facesNegative);
                idx.remove(i);
                clipped = true;
                break;
            }
            if (!clipped) break; // degenerate contour: fan out what is left
        }
        for (int i = 1; i + 1 < idx.size(); i++) emitTriangle(data, idx.get(0), idx.get(i), idx.get(i + 1), facesNegative);
    }

    private void emitTriangle(List<Object> data, int a, int b, int c, boolean reversed) {
        emit(data.get(a));
        emit(data.get(reversed ? c : b));
        emit(data.get(reversed ? b : c));
    }

    private void emit(Object vertexData) {
        callback.vertex(vertexData);
        callback.vertexData(vertexData, polygonData);
    }

    private static double cross(double[] a, double[] b, double[] c, int u, int v) {
        return (b[u] - a[u]) * (c[v] - a[v]) - (b[v] - a[v]) * (c[u] - a[u]);
    }

    private static boolean isEar(List<double[]> pts, List<Integer> idx, int a, int b, int c, int u, int v) {
        double[] pa = pts.get(a);
        double[] pb = pts.get(b);
        double[] pc = pts.get(c);
        double orient = cross(pa, pb, pc, u, v);
        if (orient < 0.0D) return false; // reflex corner
        if (orient == 0.0D) return true; // collinear: drop the vertex
        for (int k : idx) {
            if (k == a || k == b || k == c) continue;
            double[] p = pts.get(k);
            if (cross(pa, pb, p, u, v) >= 0.0D && cross(pb, pc, p, u, v) >= 0.0D && cross(pc, pa, p, u, v) >= 0.0D) return false;
        }
        return true;
    }
}
