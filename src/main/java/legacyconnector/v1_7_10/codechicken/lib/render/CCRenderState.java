package legacyconnector.v1_7_10.codechicken.lib.render;

import legacyconnector.v1_7_10.codechicken.lib.lighting.LC;
import legacyconnector.v1_7_10.codechicken.lib.vec.Vector3;
import legacyconnector.v1_7_10.net.minecraft.client.Minecraft;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.OpenGlHelper;
import legacyconnector.v1_7_10.net.minecraft.client.renderer.Tessellator;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.world.IBlockAccess;

/**
 * CodeChickenLib's render state: the vertex being drawn (position, texture coordinate, colour, brightness, normal) and the pipeline
 * of operations that fills it from a model. Vertices go to the legacy Tessellator, which the layer turns into modern geometry.
 */
public class CCRenderState {
    /** Where a model's vertices and per-vertex data come from. */
    public interface IVertexSource {
        Vertex5[] getVertices();

        <T> T getAttributes(VertexAttribute<T> attr);

        boolean hasAttribute(VertexAttribute<?> attr);

        /** Called for each vertex before the pipeline runs; sets the per-vertex data the source computes itself. */
        void prepareVertex();
    }

    /** One step of the pipeline, run for every vertex. */
    public interface IVertexOperation {
        /** Called when the pipeline is built: false drops the operation (nothing to do for this model). */
        boolean load();

        void operate();

        /** Operations run in the order of their ids; ids come from {@link CCRenderState#registerOperation()}. */
        int operationID();
    }

    /** A per-vertex data channel of a model (colours, normals, light coordinates, sides) and the operation that applies it. */
    public abstract static class VertexAttribute<T> implements IVertexOperation {
        public final int operationIndex = registerOperation();
        public boolean active;

        public abstract T newArray(int length);

        public int operationID() {
            return operationIndex;
        }
    }

    private static int operationCount;

    public static synchronized int registerOperation() {
        return operationCount++;
    }

    public static int operationCount() {
        return operationCount;
    }

    public static IVertexSource model;
    public static int firstVertexIndex;
    public static int lastVertexIndex;
    public static int vertexIndex;
    public static CCRenderPipeline pipeline = new CCRenderPipeline();

    public static Vertex5 vert = new Vertex5();
    public static boolean hasNormal;
    public static Vector3 normal = new Vector3();
    public static boolean hasColour;
    /** The vertex colour as 0xRRGGBBAA. */
    public static int colour;
    public static boolean hasBrightness;
    public static int brightness;
    public static int side;
    public static LC lc = new LC();

    /** The colour of the model before any operation changes it. */
    public static int baseColour = 0xFFFFFFFF;
    /** Alpha (0 to 255) forced onto every vertex, or -1 to keep the vertex's own. */
    public static int alphaOverride = -1;
    public static boolean useNormals;
    public static boolean computeLighting = true;
    public static boolean useColour = true;

    public static final VertexAttribute<Vector3[]> normalAttrib = new VertexAttribute<>() {
        public Vector3[] newArray(int length) {
            return new Vector3[length];
        }

        public boolean load() {
            if (!model.hasAttribute(this)) {
                active = false;
                return false;
            }
            active = true;
            hasNormal = true;
            return true;
        }

        public void operate() {
            Vector3[] n = model.getAttributes(this);
            if (n != null && n[vertexIndex] != null) normal.set(n[vertexIndex]);
        }
    };

    public static final VertexAttribute<int[]> colourAttrib = new VertexAttribute<>() {
        public int[] newArray(int length) {
            return new int[length];
        }

        public boolean load() {
            active = model.hasAttribute(this);
            if (!active) return false;
            hasColour = true;
            return true;
        }

        public void operate() {
            int[] c = model.getAttributes(this);
            if (c != null) colour = legacyconnector.v1_7_10.codechicken.lib.colour.ColourRGBA.multiply(baseColour, c[vertexIndex]);
        }
    };

    public static final VertexAttribute<LC[]> lightCoordAttrib = new VertexAttribute<>() {
        public LC[] newArray(int length) {
            return new LC[length];
        }

        public boolean load() {
            active = model.hasAttribute(this);
            return active;
        }

        public void operate() {
            LC[] l = model.getAttributes(this);
            if (l != null && l[vertexIndex] != null) lc.set(l[vertexIndex]);
        }
    };

    public static final VertexAttribute<int[]> sideAttrib = new VertexAttribute<>() {
        public int[] newArray(int length) {
            return new int[length];
        }

        public boolean load() {
            active = model.hasAttribute(this);
            return active;
        }

        public void operate() {
            int[] s = model.getAttributes(this);
            if (s != null) side = s[vertexIndex];
        }
    };

    /** Back to the defaults: no model, an empty pipeline, white, no overrides. */
    public static void reset() {
        model = null;
        pipeline.reset();
        useNormals = false;
        hasNormal = false;
        hasColour = false;
        hasBrightness = false;
        useColour = true;
        computeLighting = true;
        baseColour = 0xFFFFFFFF;
        colour = 0xFFFFFFFF;
        alphaOverride = -1;
        side = 0;
    }

    public static void setColour(int c) {
        colour = c;
        hasColour = true;
    }

    public static void setBrightness(int b) {
        brightness = b;
        hasBrightness = true;
    }

    /** The brightness the block at the position receives. */
    public static void setBrightness(IBlockAccess world, int x, int y, int z) {
        setBrightness(world.getBlock(x, y, z).getMixedBrightnessForBlock(world, x, y, z));
    }

    /** Takes the brightness from the current light map coordinates (what the last lit entity or block set). */
    public static void pullLightmap() {
        setBrightness((int) OpenGlHelper.lastBrightnessY << 16 | (int) OpenGlHelper.lastBrightnessX);
    }

    public static void setDynamic() {
        // the layer draws every vertex as it comes; there are no cached display lists to opt out of
    }

    public static void setModel(IVertexSource source) {
        setModel(source, 0, source.getVertices().length);
    }

    public static void setModel(IVertexSource source, int start, int end) {
        model = source;
        firstVertexIndex = start;
        lastVertexIndex = end;
    }

    public static void bindModel(IVertexSource source) {
        setModel(source);
    }

    public static void setPipeline(IVertexOperation... ops) {
        pipeline.reset();
        pipeline.setPipeline(ops);
    }

    public static void setPipeline(IVertexSource source, int start, int end, IVertexOperation... ops) {
        setModel(source, start, end);
        setPipeline(ops);
    }

    /** Runs the operations on every vertex of the model, drawing into the started Tessellator. */
    public static void render(IVertexOperation... ops) {
        setPipeline(ops);
        render();
    }

    public static void render() {
        runPipeline();
    }

    public static void runPipeline() {
        if (model == null) return;
        Vertex5[] verts = model.getVertices();
        pipeline.rebuild();
        for (vertexIndex = firstVertexIndex; vertexIndex < lastVertexIndex; vertexIndex++) {
            model.prepareVertex();
            vert.set(verts[vertexIndex]);
            if (!hasColour) colour = baseColour;
            pipeline.operate();
            writeVert();
        }
    }

    public static void writeVert() {
        Tessellator tess = Tessellator.mcc$get$instance();
        if (hasNormal && useNormals) tess.setNormal((float) normal.x, (float) normal.y, (float) normal.z);
        if (hasBrightness) tess.setBrightness(brightness);
        if (hasColour || alphaOverride >= 0) {
            int a = alphaOverride >= 0 ? alphaOverride : colour & 0xFF;
            tess.setColorRGBA(colour >>> 24, colour >> 16 & 0xFF, colour >> 8 & 0xFF, a);
        }
        tess.addVertexWithUV(vert.vec.x, vert.vec.y, vert.vec.z, vert.uv.u, vert.uv.v);
    }

    public static void startDrawing() {
        startDrawing(7);
    }

    public static void startDrawing(int mode) {
        Tessellator.mcc$get$instance().startDrawing(mode);
    }

    public static void draw() {
        Tessellator.mcc$get$instance().draw();
    }

    public static void changeTexture(ResourceLocation texture) {
        Minecraft.getMinecraft().renderEngine.bindTexture(texture);
    }

    public static void changeTexture(String texture) {
        changeTexture(new ResourceLocation(texture));
    }
}
