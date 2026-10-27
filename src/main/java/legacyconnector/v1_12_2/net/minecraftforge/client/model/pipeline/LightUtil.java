package legacyconnector.v1_12_2.net.minecraftforge.client.model.pipeline;

import java.util.concurrent.ConcurrentHashMap;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.BufferBuilder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model.BakedQuad;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import legacyconnector.v1_12_2.net.minecraft.util.EnumFacing;

/** Forge's quad helpers: element packing and unpacking in any vertex format, diffuse face shading, and quad output. */
public class LightUtil {
    private static final ConcurrentHashMap<Long, int[]> FORMAT_MAPS = new ConcurrentHashMap<>();

    public static float diffuseLight(float x, float y, float z) {
        return Math.min(x * x * 0.6F + y * y * ((3.0F + y) / 4.0F) + z * z * 0.8F, 1.0F);
    }

    public static float diffuseLight(EnumFacing side) {
        return switch (side) {
            case DOWN -> 0.5F;
            case UP -> 1.0F;
            case NORTH, SOUTH -> 0.8F;
            default -> 0.6F;
        };
    }

    public static EnumFacing toSide(float x, float y, float z) {
        if (Math.abs(x) > Math.abs(y)) {
            if (Math.abs(x) > Math.abs(z)) return x < 0 ? EnumFacing.WEST : EnumFacing.EAST;
            return z < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
        }
        if (Math.abs(y) > Math.abs(z)) return y < 0 ? EnumFacing.DOWN : EnumFacing.UP;
        return z < 0 ? EnumFacing.NORTH : EnumFacing.SOUTH;
    }

    public static void putBakedQuad(IVertexConsumer consumer, BakedQuad quad) {
        consumer.setTexture(quad.getSprite());
        consumer.setQuadOrientation(quad.getFace());
        if (quad.hasTintIndex()) consumer.setQuadTint(quad.getTintIndex());
        consumer.setApplyDiffuseLighting(quad.shouldApplyDiffuseLighting());
        float[] data = new float[4];
        VertexFormat formatFrom = consumer.getVertexFormat();
        VertexFormat formatTo = quad.getFormat();
        int countFrom = formatFrom.getElementCount();
        int countTo = formatTo.getElementCount();
        int[] eMap = mapFormats(formatFrom, formatTo);
        for (int v = 0; v < 4; v++) {
            for (int e = 0; e < countFrom; e++) {
                if (eMap[e] != countTo) {
                    unpack(quad.getVertexData(), data, formatTo, v, eMap[e]);
                    consumer.put(e, data);
                } else {
                    consumer.put(e);
                }
            }
        }
    }

    /** For each element of {@code from}, the index of the same element in {@code to} (or to's element count when it has none). */
    public static int[] mapFormats(VertexFormat from, VertexFormat to) {
        long key = ((long) System.identityHashCode(from) << 32) | (System.identityHashCode(to) & 0xFFFFFFFFL);
        return FORMAT_MAPS.computeIfAbsent(key, k -> {
            int fromCount = from.getElementCount();
            int toCount = to.getElementCount();
            int[] eMap = new int[fromCount];
            for (int e = 0; e < fromCount; e++) {
                VertexFormatElement expected = from.getElement(e);
                int e2;
                for (e2 = 0; e2 < toCount; e2++) {
                    VertexFormatElement current = to.getElement(e2);
                    if (expected.getUsage() == current.getUsage() && expected.getIndex() == current.getIndex()) break;
                }
                eMap[e] = e2;
            }
            return eMap;
        });
    }

    public static void unpack(int[] from, float[] to, VertexFormat formatFrom, int v, int e) {
        int length = Math.min(4, to.length);
        VertexFormatElement element = formatFrom.getElement(e);
        int vertexStart = v * formatFrom.getSize() + formatFrom.getOffset(e);
        int count = element.getElementCount();
        VertexFormatElement.EnumType type = element.getType();
        int size = type.getSize();
        int mask = (256 << (8 * (size - 1))) - 1;
        for (int i = 0; i < length; i++) {
            if (i < count) {
                int pos = vertexStart + size * i;
                int index = pos >> 2;
                int offset = pos & 3;
                if (index >= from.length) {
                    to[i] = 0;
                    continue;
                }
                int bits = from[index];
                bits = bits >>> (offset * 8);
                if ((pos + size - 1) / 4 != index && index + 1 < from.length) bits |= from[index + 1] << ((4 - offset) * 8);
                bits &= mask;
                switch (type) {
                    case FLOAT -> to[i] = Float.intBitsToFloat(bits);
                    case UBYTE, USHORT -> to[i] = (float) bits / mask;
                    case UINT -> to[i] = (float) ((double) (bits & 0xFFFFFFFFL) / 0xFFFFFFFFL);
                    case BYTE -> to[i] = ((float) (byte) bits) / (mask >> 1);
                    case SHORT -> to[i] = ((float) (short) bits) / (mask >> 1);
                    case INT -> to[i] = (float) ((double) (bits & 0xFFFFFFFFL) / (0xFFFFFFFFL >> 1));
                }
            } else {
                to[i] = i == 3 && element.getUsage() == VertexFormatElement.EnumUsage.POSITION ? 1 : 0;
            }
        }
    }

    public static void pack(float[] from, int[] to, VertexFormat formatTo, int v, int e) {
        VertexFormatElement element = formatTo.getElement(e);
        int vertexStart = v * formatTo.getSize() + formatTo.getOffset(e);
        int count = element.getElementCount();
        VertexFormatElement.EnumType type = element.getType();
        int size = type.getSize();
        int mask = (256 << (8 * (size - 1))) - 1;
        for (int i = 0; i < 4; i++) {
            if (i < count) {
                int pos = vertexStart + size * i;
                int index = pos >> 2;
                int offset = pos & 3;
                if (index >= to.length) continue;
                int bits;
                float f = i < from.length ? from[i] : 0;
                if (type == VertexFormatElement.EnumType.FLOAT) bits = Float.floatToRawIntBits(f);
                else if (type == VertexFormatElement.EnumType.UBYTE || type == VertexFormatElement.EnumType.USHORT || type == VertexFormatElement.EnumType.UINT) bits = Math.round(f * mask);
                else bits = Math.round(f * (mask >> 1));
                to[index] &= ~(mask << (offset * 8));
                to[index] |= ((bits & mask) << (offset * 8));
            }
        }
    }

    /** Draws a quad with one colour (ARGB) into a buffer of the item format, as items were drawn. */
    public static void renderQuadColor(BufferBuilder buffer, BakedQuad quad, int auxColor) {
        if (quad.getFormat().equals(buffer.getVertexFormat())) {
            buffer.addVertexData(quad.getVertexData());
            ForgeHooksClientColor.putColor4(buffer, auxColor);
        } else {
            renderQuadColorSlow(buffer, quad, auxColor);
        }
    }

    public static void renderQuadColorSlow(IVertexConsumer consumer, BakedQuad quad, int auxColor) {
        ItemConsumer cons = new ItemConsumer(consumer);
        float b = (auxColor & 0xFF) / 255F;
        float g = ((auxColor >>> 8) & 0xFF) / 255F;
        float r = ((auxColor >>> 16) & 0xFF) / 255F;
        float a = ((auxColor >>> 24) & 0xFF) / 255F;
        cons.setAuxColor(r, g, b, a);
        quad.pipe(cons);
    }

    public static void renderQuadColorSlow(BufferBuilder buffer, BakedQuad quad, int auxColor) {
        renderQuadColorSlow(new VertexBufferConsumer(buffer), quad, auxColor);
    }

    /** Multiplies each vertex colour by an extra colour (item tints) on the way to another consumer. */
    public static class ItemConsumer extends VertexTransformer {
        private int vertices = 0;
        private final float[] auxColor = {1, 1, 1, 1};
        private final float[] buf = new float[4];

        public ItemConsumer(IVertexConsumer parent) {
            super(parent);
        }

        public void setAuxColor(float... auxColor) {
            System.arraycopy(auxColor, 0, this.auxColor, 0, this.auxColor.length);
        }

        @Override
        public void put(int element, float... data) {
            if (getVertexFormat().getElement(element).getUsage() == VertexFormatElement.EnumUsage.COLOR) {
                System.arraycopy(auxColor, 0, buf, 0, buf.length);
                int n = Math.min(4, data.length);
                for (int i = 0; i < n; i++) buf[i] *= data[i];
                super.put(element, buf);
            } else {
                super.put(element, data);
            }
            if (element == getVertexFormat().getElementCount() - 1) {
                vertices++;
                if (vertices == 4) vertices = 0;
            }
        }
    }

    /** Forge's putQuadColor: the last quad's vertex colours multiplied by one colour. */
    private static final class ForgeHooksClientColor {
        static void putColor4(BufferBuilder buffer, int argb) {
            if (!buffer.getVertexFormat().hasColor()) return;
            float r = (argb >> 16 & 255) / 255.0F, g = (argb >> 8 & 255) / 255.0F, b = (argb & 255) / 255.0F;
            for (int i = 1; i <= 4; i++) buffer.putColorMultiplier(r, g, b, i);
        }
    }

    /** Forge's default baked format (vertex format of models baked without one). */
    public static final VertexFormat DEFAULT_FORMAT = DefaultVertexFormats.ITEM;
}
