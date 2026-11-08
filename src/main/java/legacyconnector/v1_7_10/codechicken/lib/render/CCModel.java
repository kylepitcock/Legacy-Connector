package legacyconnector.v1_7_10.codechicken.lib.render;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.codechicken.lib.lighting.LC;
import legacyconnector.v1_7_10.codechicken.lib.lighting.LightModel;
import legacyconnector.v1_7_10.codechicken.lib.render.uv.UV;
import legacyconnector.v1_7_10.codechicken.lib.render.uv.UVTransformation;
import legacyconnector.v1_7_10.codechicken.lib.vec.Cuboid6;
import legacyconnector.v1_7_10.codechicken.lib.vec.Transformation;
import legacyconnector.v1_7_10.codechicken.lib.vec.Vector3;

/** A model: vertices drawn as quads (vertexMode 7) or triangles (4), with optional normals, light coordinates and colours. */
public class CCModel implements CCRenderState.IVertexSource {
    public int vertexMode;
    public int vp;
    public Vertex5[] verts;
    private Vector3[] normals;
    private int[] colours;
    private LC[] lightCoords;
    private int[] sides;

    public CCModel(int vertexMode) {
        this.vertexMode = vertexMode;
        this.vp = vertexMode == 7 ? 4 : 3;
        this.verts = new Vertex5[0];
    }

    /** A model of {@code numVerts} empty vertices. */
    public static CCModel newModel(int vertexMode, int numVerts) {
        CCModel m = new CCModel(vertexMode);
        m.verts = new Vertex5[numVerts];
        for (int i = 0; i < numVerts; i++) m.verts[i] = new Vertex5();
        return m;
    }

    public static CCModel quadModel(int numVerts) {
        return newModel(7, numVerts);
    }

    public static CCModel triModel(int numVerts) {
        return newModel(4, numVerts);
    }

    // ---- the vertex source

    public Vertex5[] getVertices() {
        return verts;
    }

    @SuppressWarnings("unchecked")
    public <T> T getAttributes(CCRenderState.VertexAttribute<T> attr) {
        if (attr == CCRenderState.normalAttrib) return (T) normals;
        if (attr == CCRenderState.colourAttrib) return (T) colours;
        if (attr == CCRenderState.lightCoordAttrib) return (T) lightCoords;
        if (attr == CCRenderState.sideAttrib) return (T) sides;
        return null;
    }

    public boolean hasAttribute(CCRenderState.VertexAttribute<?> attr) {
        return getAttributes(attr) != null;
    }

    public void prepareVertex() {
    }

    public Vector3[] normals() {
        if (normals == null) normals = new Vector3[verts.length];
        return normals;
    }

    public CCModel setVertex(int i, Vector3 vec, UV uv, Vector3 normal) {
        verts[i] = new Vertex5(vec.copy(), uv.copy());
        if (normal != null) normals()[i] = normal.copy();
        return this;
    }

    public CCModel setVertex(int i, double x, double y, double z, double u, double v) {
        verts[i] = new Vertex5(x, y, z, u, v);
        return this;
    }

    // ---- copies and transformations

    public CCModel copy() {
        CCModel m = new CCModel(vertexMode);
        m.verts = new Vertex5[verts.length];
        for (int i = 0; i < verts.length; i++) m.verts[i] = verts[i].copy();
        if (normals != null) {
            m.normals = new Vector3[normals.length];
            for (int i = 0; i < normals.length; i++) m.normals[i] = normals[i] == null ? null : normals[i].copy();
        }
        if (colours != null) m.colours = colours.clone();
        if (sides != null) m.sides = sides.clone();
        if (lightCoords != null) {
            m.lightCoords = new LC[lightCoords.length];
            for (int i = 0; i < lightCoords.length; i++) m.lightCoords[i] = lightCoords[i] == null ? null : lightCoords[i].copy();
        }
        return m;
    }

    /** The model seen from behind: the order of each primitive's vertices reversed and the normals flipped. */
    public CCModel backfacedCopy() {
        CCModel m = copy();
        for (int p = 0; p + vp <= verts.length; p += vp) {
            for (int j = 0; j < vp; j++) {
                m.verts[p + j] = verts[p + vp - 1 - j].copy();
                if (normals != null) m.normals()[p + j] = normals[p + vp - 1 - j] == null ? null : normals[p + vp - 1 - j].copy().negate();
            }
        }
        return m;
    }

    public CCModel apply(Transformation t) {
        for (Vertex5 v : verts) v.vec.apply(t);
        if (normals != null) for (Vector3 n : normals) if (n != null) t.applyN(n);
        return this;
    }

    public CCModel apply(UVTransformation t) {
        for (Vertex5 v : verts) v.uv.apply(t);
        return this;
    }

    public Cuboid6 bounds() {
        Cuboid6 c = new Cuboid6(new Vector3(Double.MAX_VALUE, Double.MAX_VALUE, Double.MAX_VALUE), new Vector3(-Double.MAX_VALUE, -Double.MAX_VALUE, -Double.MAX_VALUE));
        for (Vertex5 v : verts) c.enclose(v.vec);
        return c;
    }

    // ---- lighting

    /** Face normals for all the vertices. */
    public CCModel computeNormals() {
        return computeNormals(0, verts.length);
    }

    public CCModel computeNormals(int start, int length) {
        Vector3[] n = normals();
        for (int p = start; p + vp <= start + length; p += vp) {
            Vector3 normal = verts[p + 1].vec.copy().sub(verts[p].vec).crossProduct(verts[p + 2].vec.copy().sub(verts[p + 1].vec)).normalize();
            for (int j = 0; j < vp; j++) n[p + j] = normal.copy();
        }
        return this;
    }

    /** Per-vertex colours from a light model applied to the normals (computes the normals when missing). */
    public CCModel computeLighting(LightModel light) {
        if (normals == null) computeNormals();
        colours = new int[verts.length];
        for (int i = 0; i < verts.length; i++) colours[i] = light.apply(0xFFFFFFFF, normals[i]);
        return this;
    }

    /** Light coordinates for the vertices, taking each primitive's side from its normal. */
    public CCModel computeLightCoords() {
        if (normals == null) computeNormals();
        lightCoords = new LC[verts.length];
        sides = new int[verts.length];
        for (int i = 0; i < verts.length; i++) {
            int side = sideOf(normals[i]);
            sides[i] = side;
            lightCoords[i] = new LC().compute(verts[i].vec, side);
        }
        return this;
    }

    private static int sideOf(Vector3 n) {
        double ax = Math.abs(n.x);
        double ay = Math.abs(n.y);
        double az = Math.abs(n.z);
        if (ay >= ax && ay >= az) return n.y < 0 ? 0 : 1;
        if (az >= ax) return n.z < 0 ? 2 : 3;
        return n.x < 0 ? 4 : 5;
    }

    // ---- rendering

    /** Draws the whole model through the pipeline of operations into the started Tessellator. */
    public void render(CCRenderState.IVertexOperation... ops) {
        render(0, verts.length, ops);
    }

    public void render(int start, int end, CCRenderState.IVertexOperation... ops) {
        CCRenderState.setModel(this, start, end);
        CCRenderState.setPipeline(ops);
        CCRenderState.render();
    }

    // ---- blocks

    /**
     * A box model of the cuboid starting at vertex {@code i} (the model has {@code i} empty vertices before it): six quads in side order
     * (down, up, north, south, west, east) with block-style texture coordinates and the side as texture index.
     */
    public CCModel generateBlock(int i, Cuboid6 bounds) {
        return generateBlock(i, bounds, 0);
    }

    public CCModel generateBlock(int i, double x1, double y1, double z1, double x2, double y2, double z2) {
        return generateBlock(i, new Cuboid6(x1, y1, z1, x2, y2, z2), 0);
    }

    public CCModel generateBlock(int i, double x1, double y1, double z1, double x2, double y2, double z2, int mask) {
        return generateBlock(i, new Cuboid6(x1, y1, z1, x2, y2, z2), mask);
    }

    /** As above, leaving out the sides whose bit is set in the mask (the model must have room for the vertices of the others). */
    public CCModel generateBlock(int i, Cuboid6 bounds, int mask) {
        int at = i;
        for (int s = 0; s < 6; s++) {
            if ((mask & 1 << s) != 0) continue;
            face(verts, at, bounds, s);
            at += 4;
        }
        return this;
    }

    /** A new quad model holding the cuboid (the faces whose bit is set in the mask are left out). */
    public static CCModel generateBlockModel(Cuboid6 bounds, int mask) {
        int faces = 0;
        for (int s = 0; s < 6; s++) if ((mask & 1 << s) == 0) faces++;
        return quadModel(faces * 4).generateBlock(0, bounds, mask);
    }

    /** The four vertices of a cuboid's face, counterclockwise seen from outside, written at offset. */
    public static void face(Vertex5[] out, int offset, Cuboid6 c, int side) {
        double x1 = c.min.x, y1 = c.min.y, z1 = c.min.z, x2 = c.max.x, y2 = c.max.y, z2 = c.max.z;
        double[][] p;
        switch (side) {
            case 0 -> p = new double[][] {{x1, y1, z2}, {x1, y1, z1}, {x2, y1, z1}, {x2, y1, z2}};
            case 1 -> p = new double[][] {{x2, y2, z2}, {x2, y2, z1}, {x1, y2, z1}, {x1, y2, z2}};
            case 2 -> p = new double[][] {{x1, y2, z1}, {x2, y2, z1}, {x2, y1, z1}, {x1, y1, z1}};
            case 3 -> p = new double[][] {{x1, y2, z2}, {x1, y1, z2}, {x2, y1, z2}, {x2, y2, z2}};
            case 4 -> p = new double[][] {{x1, y2, z2}, {x1, y2, z1}, {x1, y1, z1}, {x1, y1, z2}};
            default -> p = new double[][] {{x2, y1, z2}, {x2, y1, z1}, {x2, y2, z1}, {x2, y2, z2}};
        }
        for (int j = 0; j < 4; j++) {
            double x = p[j][0], y = p[j][1], z = p[j][2];
            double u;
            double v;
            switch (side) {
                case 0, 1 -> { u = x; v = z; }
                case 2, 3 -> { u = x; v = 1 - y; }
                default -> { u = z; v = 1 - y; }
            }
            out[offset + j] = new Vertex5(x, y, z, u, v, side);
        }
    }

    // ---- OBJ files

    /** Reads the groups of a Wavefront OBJ file as models (quads or triangles per vertexMode), transformed by the given transformation. */
    public static Map<String, CCModel> parseObjModels(InputStream input, int vertexMode, Transformation coordSystem) throws IOException {
        List<Vector3> positions = new ArrayList<>();
        List<double[]> uvs = new ArrayList<>();
        List<Vector3> norms = new ArrayList<>();
        Map<String, List<int[]>> groups = new HashMap<>();
        java.util.LinkedHashMap<String, List<int[]>> ordered = new java.util.LinkedHashMap<>();
        String group = "default";
        try (BufferedReader r = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = r.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) continue;
                String[] t = line.split("\\s+");
                switch (t[0]) {
                    case "v" -> {
                        Vector3 v = new Vector3(Double.parseDouble(t[1]), Double.parseDouble(t[2]), Double.parseDouble(t[3]));
                        if (coordSystem != null) v.apply(coordSystem);
                        positions.add(v);
                    }
                    case "vt" -> uvs.add(new double[] {Double.parseDouble(t[1]), 1 - Double.parseDouble(t[2])});
                    case "vn" -> {
                        Vector3 n = new Vector3(Double.parseDouble(t[1]), Double.parseDouble(t[2]), Double.parseDouble(t[3]));
                        if (coordSystem != null) coordSystem.applyN(n);
                        norms.add(n);
                    }
                    case "g", "o" -> group = t.length > 1 ? t[1] : "default";
                    case "f" -> {
                        List<int[]> faces = ordered.computeIfAbsent(group, g -> new ArrayList<>());
                        int[][] corners = new int[t.length - 1][3];
                        for (int i = 1; i < t.length; i++) {
                            String[] idx = t[i].split("/", -1);
                            corners[i - 1][0] = Integer.parseInt(idx[0]) - 1;
                            corners[i - 1][1] = idx.length > 1 && !idx[1].isEmpty() ? Integer.parseInt(idx[1]) - 1 : -1;
                            corners[i - 1][2] = idx.length > 2 && !idx[2].isEmpty() ? Integer.parseInt(idx[2]) - 1 : -1;
                        }
                        int per = vertexMode == 7 ? 4 : 3;
                        if (corners.length == per) {
                            for (int[] c : corners) faces.add(c);
                        } else if (per == 3) {
                            for (int i = 1; i + 1 < corners.length; i++) {
                                faces.add(corners[0]);
                                faces.add(corners[i]);
                                faces.add(corners[i + 1]);
                            }
                        } else if (corners.length == 3) {
                            faces.add(corners[0]);
                            faces.add(corners[1]);
                            faces.add(corners[2]);
                            faces.add(corners[2]);
                        }
                    }
                    default -> {
                    }
                }
            }
        }
        Map<String, CCModel> models = new java.util.LinkedHashMap<>();
        for (Map.Entry<String, List<int[]>> e : ordered.entrySet()) {
            List<int[]> list = e.getValue();
            CCModel m = newModel(vertexMode, list.size());
            boolean hasNormals = false;
            for (int i = 0; i < list.size(); i++) {
                int[] c = list.get(i);
                double[] uv = c[1] >= 0 ? uvs.get(c[1]) : new double[] {0, 0};
                m.verts[i] = new Vertex5(positions.get(c[0]).copy(), new UV(uv[0], uv[1]));
                if (c[2] >= 0) {
                    m.normals()[i] = norms.get(c[2]).copy();
                    hasNormals = true;
                }
            }
            if (!hasNormals) m.normals = null;
            models.put(e.getKey(), m);
        }
        groups.clear();
        return models;
    }
}
