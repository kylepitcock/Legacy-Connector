package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.Arrays;
import java.util.BitSet;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;

/**
 * 1.12.2 BufferBuilder: vertices written into a native-order byte buffer in the layout of their {@link VertexFormat}, exactly as
 * vanilla stored them, so code that reads the buffer back (sorting, vertex states, baked quad data, VBO uploads) sees the legacy
 * bytes. Drawing decodes the buffer into the render context ({@link WorldVertexBufferUploader}).
 */
public class BufferBuilder {
    private static final boolean LITTLE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;

    private ByteBuffer byteBuffer;
    private IntBuffer rawIntBuffer;
    private ShortBuffer rawShortBuffer;
    private FloatBuffer rawFloatBuffer;
    private int vertexCount;
    private VertexFormatElement vertexFormatElement;
    private int vertexFormatIndex;
    private boolean noColor;
    private int drawMode;
    public double xOffset;
    public double yOffset;
    public double zOffset;
    private VertexFormat vertexFormat;
    public boolean isDrawing;

    public BufferBuilder(int bufferSizeIn) {
        byteBuffer = GLAllocation.createDirectByteBuffer(bufferSizeIn * 4);
        rawIntBuffer = byteBuffer.asIntBuffer();
        rawShortBuffer = byteBuffer.asShortBuffer();
        rawFloatBuffer = byteBuffer.asFloatBuffer();
    }

    private static int roundUp(int value, int interval) {
        if (interval == 0) return 0;
        if (value == 0) return interval;
        if (value < 0) interval *= -1;
        int i = value % interval;
        return i == 0 ? value : value + interval - i;
    }

    private void growBuffer(int increaseAmount) {
        int used = vertexFormat == null ? rawIntBuffer.position() * 4 : vertexCount * vertexFormat.getSize();
        if (roundUp(increaseAmount, 4) / 4 > rawIntBuffer.remaining() || used + increaseAmount > byteBuffer.capacity()) {
            int i = byteBuffer.capacity();
            int j = i + roundUp(increaseAmount, 2097152);
            int k = rawIntBuffer.position();
            ByteBuffer grown = GLAllocation.createDirectByteBuffer(j);
            byteBuffer.position(0);
            byteBuffer.limit(byteBuffer.capacity());
            grown.put(byteBuffer);
            grown.rewind();
            byteBuffer = grown;
            rawFloatBuffer = byteBuffer.asFloatBuffer();
            rawIntBuffer = byteBuffer.asIntBuffer();
            rawIntBuffer.position(k);
            rawShortBuffer = byteBuffer.asShortBuffer();
            rawShortBuffer.position(k << 1);
        }
    }

    /** Sorts the quads back to front as seen from the camera (translucent geometry). */
    public void sortVertexData(float cameraX, float cameraY, float cameraZ) {
        int quads = vertexCount / 4;
        float[] dist = new float[quads];
        for (int j = 0; j < quads; ++j) {
            dist[j] = getDistanceSq(rawFloatBuffer, (float) (cameraX + xOffset), (float) (cameraY + yOffset), (float) (cameraZ + zOffset),
                    vertexFormat.getIntegerSize(), j * vertexFormat.getSize());
        }
        Integer[] order = new Integer[quads];
        for (int k = 0; k < order.length; ++k) order[k] = k;
        Arrays.sort(order, (a, b) -> Float.compare(dist[b], dist[a]));
        BitSet done = new BitSet();
        int l = vertexFormat.getSize();
        int[] tmp = new int[l];
        for (int i1 = 0; (i1 = done.nextClearBit(i1)) < order.length; ++i1) {
            int j1 = order[i1];
            if (j1 != i1) {
                rawIntBuffer.limit(j1 * l + l);
                rawIntBuffer.position(j1 * l);
                rawIntBuffer.get(tmp);
                int k1 = j1;
                for (int l1 = order[j1]; k1 != i1; l1 = order[l1]) {
                    rawIntBuffer.limit(l1 * l + l);
                    rawIntBuffer.position(l1 * l);
                    IntBuffer slice = rawIntBuffer.slice();
                    rawIntBuffer.limit(k1 * l + l);
                    rawIntBuffer.position(k1 * l);
                    rawIntBuffer.put(slice);
                    done.set(k1);
                    k1 = l1;
                }
                rawIntBuffer.limit(i1 * l + l);
                rawIntBuffer.position(i1 * l);
                rawIntBuffer.put(tmp);
            }
            done.set(i1);
        }
        rawIntBuffer.limit(rawIntBuffer.capacity());
        rawIntBuffer.position(getBufferSize());
    }

    public State getVertexState() {
        rawIntBuffer.rewind();
        int i = getBufferSize();
        rawIntBuffer.limit(i);
        int[] data = new int[i];
        rawIntBuffer.get(data);
        rawIntBuffer.limit(rawIntBuffer.capacity());
        rawIntBuffer.position(i);
        return new State(data, new VertexFormat(vertexFormat));
    }

    private int getBufferSize() {
        return vertexCount * vertexFormat.getIntegerSize();
    }

    private static float getDistanceSq(FloatBuffer buf, float x, float y, float z, int integerSize, int offset) {
        float x0 = buf.get(offset), y0 = buf.get(offset + 1), z0 = buf.get(offset + 2);
        float x1 = buf.get(offset + integerSize), y1 = buf.get(offset + integerSize + 1), z1 = buf.get(offset + integerSize + 2);
        float x2 = buf.get(offset + integerSize * 2), y2 = buf.get(offset + integerSize * 2 + 1), z2 = buf.get(offset + integerSize * 2 + 2);
        float x3 = buf.get(offset + integerSize * 3), y3 = buf.get(offset + integerSize * 3 + 1), z3 = buf.get(offset + integerSize * 3 + 2);
        float dx = (x0 + x1 + x2 + x3) * 0.25F - x;
        float dy = (y0 + y1 + y2 + y3) * 0.25F - y;
        float dz = (z0 + z1 + z2 + z3) * 0.25F - z;
        return dx * dx + dy * dy + dz * dz;
    }

    public void setVertexState(State state) {
        rawIntBuffer.clear();
        vertexFormat = new VertexFormat(state.getVertexFormat());
        growBuffer(state.getRawBuffer().length * 4);
        rawIntBuffer.put(state.getRawBuffer());
        vertexCount = state.getVertexCount();
    }

    public void reset() {
        vertexCount = 0;
        vertexFormatElement = null;
        vertexFormatIndex = 0;
    }

    public void begin(int glMode, VertexFormat format) {
        if (isDrawing) throw new IllegalStateException("Already building!");
        isDrawing = true;
        reset();
        drawMode = glMode;
        vertexFormat = format;
        vertexFormatElement = format.getElement(vertexFormatIndex);
        noColor = false;
        byteBuffer.limit(byteBuffer.capacity());
    }

    /** Leaves a draw a failed legacy renderer began and never finished, so the next renderer does not hit "Already building!". */
    public void mcc$abort() {
        isDrawing = false;
        reset();
    }

    private int elementOffset() {
        return vertexCount * vertexFormat.getSize() + vertexFormat.getOffset(vertexFormatIndex);
    }

    public BufferBuilder tex(double u, double v) {
        int i = elementOffset();
        switch (vertexFormatElement.getType()) {
            case FLOAT -> {
                byteBuffer.putFloat(i, (float) u);
                byteBuffer.putFloat(i + 4, (float) v);
            }
            case UINT, INT -> {
                byteBuffer.putInt(i, (int) u);
                byteBuffer.putInt(i + 4, (int) v);
            }
            case USHORT, SHORT -> {
                byteBuffer.putShort(i, (short) (int) v);
                byteBuffer.putShort(i + 2, (short) (int) u);
            }
            case UBYTE, BYTE -> {
                byteBuffer.put(i, (byte) (int) v);
                byteBuffer.put(i + 1, (byte) (int) u);
            }
        }
        nextVertexFormatIndex();
        return this;
    }

    /** Light as (sky, block); stored block first, the lightmap texture's u. */
    public BufferBuilder lightmap(int skyLight, int blockLight) {
        int i = elementOffset();
        switch (vertexFormatElement.getType()) {
            case FLOAT -> {
                byteBuffer.putFloat(i, (float) skyLight);
                byteBuffer.putFloat(i + 4, (float) blockLight);
            }
            case UINT, INT -> {
                byteBuffer.putInt(i, skyLight);
                byteBuffer.putInt(i + 4, blockLight);
            }
            case USHORT, SHORT -> {
                byteBuffer.putShort(i, (short) blockLight);
                byteBuffer.putShort(i + 2, (short) skyLight);
            }
            case UBYTE, BYTE -> {
                byteBuffer.put(i, (byte) blockLight);
                byteBuffer.put(i + 1, (byte) skyLight);
            }
        }
        nextVertexFormatIndex();
        return this;
    }

    public void putBrightness4(int vertex0, int vertex1, int vertex2, int vertex3) {
        int i = (vertexCount - 4) * vertexFormat.getIntegerSize() + vertexFormat.getUvOffsetById(1) / 4;
        int j = vertexFormat.getSize() >> 2;
        rawIntBuffer.put(i, vertex0);
        rawIntBuffer.put(i + j, vertex1);
        rawIntBuffer.put(i + j * 2, vertex2);
        rawIntBuffer.put(i + j * 3, vertex3);
    }

    public void putPosition(double x, double y, double z) {
        int i = vertexFormat.getIntegerSize();
        int j = (vertexCount - 4) * i;
        for (int k = 0; k < 4; ++k) {
            int l = j + k * i;
            rawIntBuffer.put(l, Float.floatToRawIntBits((float) (x + xOffset) + Float.intBitsToFloat(rawIntBuffer.get(l))));
            rawIntBuffer.put(l + 1, Float.floatToRawIntBits((float) (y + yOffset) + Float.intBitsToFloat(rawIntBuffer.get(l + 1))));
            rawIntBuffer.put(l + 2, Float.floatToRawIntBits((float) (z + zOffset) + Float.intBitsToFloat(rawIntBuffer.get(l + 2))));
        }
    }

    public int getColorIndex(int vertexIndex) {
        return ((vertexCount - vertexIndex) * vertexFormat.getSize() + vertexFormat.getColorOffset()) / 4;
    }

    public void putColorMultiplier(float red, float green, float blue, int vertexIndex) {
        int i = getColorIndex(vertexIndex);
        int j = -1;
        if (!noColor) {
            j = rawIntBuffer.get(i);
            if (LITTLE) {
                int r = (int) ((j & 255) * red);
                int g = (int) ((j >> 8 & 255) * green);
                int b = (int) ((j >> 16 & 255) * blue);
                j = j & 0xFF000000 | b << 16 | g << 8 | r;
            } else {
                int r = (int) ((j >> 24 & 255) * red);
                int g = (int) ((j >> 16 & 255) * green);
                int b = (int) ((j >> 8 & 255) * blue);
                j = j & 255 | r << 24 | g << 16 | b << 8;
            }
        }
        rawIntBuffer.put(i, j);
    }

    private void putColor(int argb, int vertexIndex) {
        int i = getColorIndex(vertexIndex);
        putColorRGBA(i, argb >> 16 & 255, argb >> 8 & 255, argb & 255, argb >> 24 & 255);
    }

    public void putColorRGB_F(float red, float green, float blue, int vertexIndex) {
        int i = getColorIndex(vertexIndex);
        putColorRGBA(i, clamp((int) (red * 255.0F)), clamp((int) (green * 255.0F)), clamp((int) (blue * 255.0F)), 255);
    }

    private static int clamp(int v) {
        return v < 0 ? 0 : Math.min(v, 255);
    }

    public void putColorRGBA(int index, int red, int green, int blue, int alpha) {
        if (LITTLE) rawIntBuffer.put(index, alpha << 24 | blue << 16 | green << 8 | red);
        else rawIntBuffer.put(index, red << 24 | green << 16 | blue << 8 | alpha);
    }

    public void noColor() {
        noColor = true;
    }

    public boolean isColorDisabled() {
        return noColor;
    }

    public BufferBuilder color(float red, float green, float blue, float alpha) {
        return color((int) (red * 255.0F), (int) (green * 255.0F), (int) (blue * 255.0F), (int) (alpha * 255.0F));
    }

    public BufferBuilder color(int red, int green, int blue, int alpha) {
        if (noColor) return this;
        int i = elementOffset();
        switch (vertexFormatElement.getType()) {
            case FLOAT -> {
                byteBuffer.putFloat(i, red / 255.0F);
                byteBuffer.putFloat(i + 4, green / 255.0F);
                byteBuffer.putFloat(i + 8, blue / 255.0F);
                byteBuffer.putFloat(i + 12, alpha / 255.0F);
            }
            case UINT, INT -> {
                byteBuffer.putFloat(i, (float) red);
                byteBuffer.putFloat(i + 4, (float) green);
                byteBuffer.putFloat(i + 8, (float) blue);
                byteBuffer.putFloat(i + 12, (float) alpha);
            }
            case USHORT, SHORT -> {
                byteBuffer.putShort(i, (short) red);
                byteBuffer.putShort(i + 2, (short) green);
                byteBuffer.putShort(i + 4, (short) blue);
                byteBuffer.putShort(i + 6, (short) alpha);
            }
            case UBYTE, BYTE -> {
                if (LITTLE) {
                    byteBuffer.put(i, (byte) red);
                    byteBuffer.put(i + 1, (byte) green);
                    byteBuffer.put(i + 2, (byte) blue);
                    byteBuffer.put(i + 3, (byte) alpha);
                } else {
                    byteBuffer.put(i, (byte) alpha);
                    byteBuffer.put(i + 1, (byte) blue);
                    byteBuffer.put(i + 2, (byte) green);
                    byteBuffer.put(i + 3, (byte) red);
                }
            }
        }
        nextVertexFormatIndex();
        return this;
    }

    public void addVertexData(int[] vertexData) {
        growBuffer(vertexData.length * 4 + vertexFormat.getSize());
        rawIntBuffer.position(getBufferSize());
        rawIntBuffer.put(vertexData);
        vertexCount += vertexData.length / vertexFormat.getIntegerSize();
    }

    public void endVertex() {
        ++vertexCount;
        growBuffer(vertexFormat.getSize());
    }

    public BufferBuilder pos(double x, double y, double z) {
        int i = elementOffset();
        switch (vertexFormatElement.getType()) {
            case FLOAT -> {
                byteBuffer.putFloat(i, (float) (x + xOffset));
                byteBuffer.putFloat(i + 4, (float) (y + yOffset));
                byteBuffer.putFloat(i + 8, (float) (z + zOffset));
            }
            case UINT, INT -> {
                byteBuffer.putInt(i, Float.floatToRawIntBits((float) (x + xOffset)));
                byteBuffer.putInt(i + 4, Float.floatToRawIntBits((float) (y + yOffset)));
                byteBuffer.putInt(i + 8, Float.floatToRawIntBits((float) (z + zOffset)));
            }
            case USHORT, SHORT -> {
                byteBuffer.putShort(i, (short) (int) (x + xOffset));
                byteBuffer.putShort(i + 2, (short) (int) (y + yOffset));
                byteBuffer.putShort(i + 4, (short) (int) (z + zOffset));
            }
            case UBYTE, BYTE -> {
                byteBuffer.put(i, (byte) (int) (x + xOffset));
                byteBuffer.put(i + 1, (byte) (int) (y + yOffset));
                byteBuffer.put(i + 2, (byte) (int) (z + zOffset));
            }
        }
        nextVertexFormatIndex();
        return this;
    }

    public void putNormal(float x, float y, float z) {
        int i = (byte) (int) (x * 127.0F) & 255;
        int j = (byte) (int) (y * 127.0F) & 255;
        int k = (byte) (int) (z * 127.0F) & 255;
        int l = i | j << 8 | k << 16;
        int stride = vertexFormat.getSize() >> 2;
        int at = (vertexCount - 4) * stride + vertexFormat.getNormalOffset() / 4;
        rawIntBuffer.put(at, l);
        rawIntBuffer.put(at + stride, l);
        rawIntBuffer.put(at + stride * 2, l);
        rawIntBuffer.put(at + stride * 3, l);
    }

    public void nextVertexFormatIndex() {
        ++vertexFormatIndex;
        vertexFormatIndex %= vertexFormat.getElementCount();
        vertexFormatElement = vertexFormat.getElement(vertexFormatIndex);
        if (vertexFormatElement.getUsage() == VertexFormatElement.EnumUsage.PADDING) nextVertexFormatIndex();
    }

    public BufferBuilder normal(float x, float y, float z) {
        int i = elementOffset();
        switch (vertexFormatElement.getType()) {
            case FLOAT -> {
                byteBuffer.putFloat(i, x);
                byteBuffer.putFloat(i + 4, y);
                byteBuffer.putFloat(i + 8, z);
            }
            case UINT, INT -> {
                byteBuffer.putInt(i, (int) x);
                byteBuffer.putInt(i + 4, (int) y);
                byteBuffer.putInt(i + 8, (int) z);
            }
            case USHORT, SHORT -> {
                byteBuffer.putShort(i, (short) ((int) (x * 32767.0F) & 65535));
                byteBuffer.putShort(i + 2, (short) ((int) (y * 32767.0F) & 65535));
                byteBuffer.putShort(i + 4, (short) ((int) (z * 32767.0F) & 65535));
            }
            case UBYTE, BYTE -> {
                byteBuffer.put(i, (byte) ((int) (x * 127.0F) & 255));
                byteBuffer.put(i + 1, (byte) ((int) (y * 127.0F) & 255));
                byteBuffer.put(i + 2, (byte) ((int) (z * 127.0F) & 255));
            }
        }
        nextVertexFormatIndex();
        return this;
    }

    public void setTranslation(double x, double y, double z) {
        xOffset = x;
        yOffset = y;
        zOffset = z;
    }

    public void finishDrawing() {
        if (!isDrawing) throw new IllegalStateException("Not building!");
        isDrawing = false;
        byteBuffer.position(0);
        byteBuffer.limit(getBufferSize() * 4);
    }

    public ByteBuffer getByteBuffer() {
        return byteBuffer;
    }

    public VertexFormat getVertexFormat() {
        return vertexFormat;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public int getDrawMode() {
        return drawMode;
    }

    public void putColor4(int argb) {
        for (int i = 0; i < 4; ++i) putColor(argb, i + 1);
    }

    public void putColorRGB_F4(float red, float green, float blue) {
        for (int i = 0; i < 4; ++i) putColorRGB_F(red, green, blue, i + 1);
    }

    /** Forge: appends raw vertex bytes in the current format. */
    public void putBulkData(ByteBuffer buffer) {
        growBuffer(buffer.limit() + vertexFormat.getSize());
        byteBuffer.position(vertexCount * vertexFormat.getSize());
        byteBuffer.put(buffer);
        vertexCount += buffer.limit() / vertexFormat.getSize();
        byteBuffer.position(0);
    }

    public class State {
        private final int[] stateRawBuffer;
        private final VertexFormat stateVertexFormat;

        public State(int[] buffer, VertexFormat format) {
            stateRawBuffer = buffer;
            stateVertexFormat = format;
        }

        public int[] getRawBuffer() {
            return stateRawBuffer;
        }

        public int getVertexCount() {
            return stateRawBuffer.length / stateVertexFormat.getIntegerSize();
        }

        public VertexFormat getVertexFormat() {
            return stateVertexFormat;
        }
    }
}
