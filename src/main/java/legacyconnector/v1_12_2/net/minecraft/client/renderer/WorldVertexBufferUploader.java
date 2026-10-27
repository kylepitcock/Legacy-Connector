package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormat;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexFormatElement;
import org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyVertices;

/**
 * 1.12.2's uploader of a finished {@link BufferBuilder}. Instead of binding client arrays, the vertices are decoded from the legacy
 * byte layout (position, texture uv, lightmap, colour, normal) and drawn through the render context that also runs the emulated GL11,
 * so legacy geometry lands in the modern frame with the current matrix, texture and blend state.
 */
public class WorldVertexBufferUploader {
    private static final boolean LITTLE = ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN;
    private final LegacyVertices vertices = new LegacyVertices();

    public void draw(BufferBuilder buffer) {
        if (buffer.getVertexCount() > 0) {
            try {
                LegacyVertices.Snapshot draw = decode(vertices, buffer);
                if (!DisplayLists.record(() -> LegacyVertices.emit(draw))) LegacyVertices.emit(draw);
            } catch (RuntimeException e) {
                org.kylepitcock.legacyconnector.v1_12_2.client.RenderErrors1122.report("drawing a legacy vertex buffer", e);
            }
        }
        buffer.reset();
    }

    /** Decodes the buffer's vertices into a draw for the render context. */
    public static LegacyVertices.Snapshot decode(LegacyVertices out, BufferBuilder buffer) {
        return decode(out, buffer.getByteBuffer(), buffer.getVertexFormat(), buffer.getVertexCount(), buffer.getDrawMode(), buffer.isColorDisabled());
    }

    /** Decodes {@code n} vertices of {@code format} from raw legacy bytes (a buffer builder's, or a vertex buffer's copy). */
    public static LegacyVertices.Snapshot decode(LegacyVertices out, ByteBuffer bytes, VertexFormat format, int n, int mode, boolean noColor) {
        int stride = format.getSize();
        int pos = format.mcc$offsetOf(VertexFormatElement.EnumUsage.POSITION, 0);
        VertexFormatElement posE = format.mcc$elementOf(VertexFormatElement.EnumUsage.POSITION, 0);
        int uv = format.mcc$offsetOf(VertexFormatElement.EnumUsage.UV, 0);
        VertexFormatElement uvE = format.mcc$elementOf(VertexFormatElement.EnumUsage.UV, 0);
        int lm = format.mcc$offsetOf(VertexFormatElement.EnumUsage.UV, 1);
        VertexFormatElement lmE = format.mcc$elementOf(VertexFormatElement.EnumUsage.UV, 1);
        int color = format.mcc$offsetOf(VertexFormatElement.EnumUsage.COLOR, 0);
        VertexFormatElement colorE = format.mcc$elementOf(VertexFormatElement.EnumUsage.COLOR, 0);
        int normal = format.mcc$offsetOf(VertexFormatElement.EnumUsage.NORMAL, 0);
        VertexFormatElement normalE = format.mcc$elementOf(VertexFormatElement.EnumUsage.NORMAL, 0);
        out.begin(mode);
        for (int i = 0; i < n; i++) {
            int base = i * stride;
            if (base + stride > bytes.limit()) break;
            if (uv >= 0) out.uv(read(bytes, base + uv, uvE, 0), read(bytes, base + uv, uvE, 1));
            if (lm >= 0) {
                int block, sky;
                if (lmE.getType() == VertexFormatElement.EnumType.SHORT || lmE.getType() == VertexFormatElement.EnumType.USHORT) {
                    block = bytes.getShort(base + lm) & 0xFFFF;
                    sky = bytes.getShort(base + lm + 2) & 0xFFFF;
                } else {
                    sky = (int) read(bytes, base + lm, lmE, 0);
                    block = (int) read(bytes, base + lm, lmE, 1);
                }
                out.light(block & 0xFFFF | (sky & 0xFFFF) << 16);
            }
            if (color >= 0 && !noColor) {
                if (colorE.getType() == VertexFormatElement.EnumType.UBYTE || colorE.getType() == VertexFormatElement.EnumType.BYTE) {
                    int b0 = bytes.get(base + color) & 255, b1 = bytes.get(base + color + 1) & 255;
                    int b2 = bytes.get(base + color + 2) & 255, b3 = bytes.get(base + color + 3) & 255;
                    if (LITTLE) out.color(b0, b1, b2, b3);
                    else out.color(b3, b2, b1, b0);
                } else if (colorE.getType() == VertexFormatElement.EnumType.FLOAT) {
                    out.color((float) read(bytes, base + color, colorE, 0), (float) read(bytes, base + color, colorE, 1),
                            (float) read(bytes, base + color, colorE, 2), colorE.getElementCount() > 3 ? (float) read(bytes, base + color, colorE, 3) : 1.0F);
                } else {
                    out.color((int) read(bytes, base + color, colorE, 0), (int) read(bytes, base + color, colorE, 1),
                            (int) read(bytes, base + color, colorE, 2), colorE.getElementCount() > 3 ? (int) read(bytes, base + color, colorE, 3) : 255);
                }
            }
            if (normal >= 0) {
                float scale = switch (normalE.getType()) {
                    case BYTE, UBYTE -> 1.0F / 127.0F;
                    case SHORT, USHORT -> 1.0F / 32767.0F;
                    default -> 1.0F;
                };
                out.normal((float) read(bytes, base + normal, normalE, 0) * scale, (float) read(bytes, base + normal, normalE, 1) * scale,
                        (float) read(bytes, base + normal, normalE, 2) * scale);
            }
            if (pos >= 0) out.vertex(read(bytes, base + pos, posE, 0), read(bytes, base + pos, posE, 1), read(bytes, base + pos, posE, 2));
        }
        return out.take();
    }

    private static double read(ByteBuffer b, int at, VertexFormatElement e, int component) {
        if (component >= e.getElementCount()) return 0;
        int size = e.getType().getSize();
        int i = at + component * size;
        return switch (e.getType()) {
            case FLOAT -> b.getFloat(i);
            case UBYTE -> b.get(i) & 255;
            case BYTE -> b.get(i);
            case USHORT -> b.getShort(i) & 0xFFFF;
            case SHORT -> b.getShort(i);
            case UINT -> b.getInt(i) & 0xFFFFFFFFL;
            case INT -> b.getInt(i);
        };
    }
}
