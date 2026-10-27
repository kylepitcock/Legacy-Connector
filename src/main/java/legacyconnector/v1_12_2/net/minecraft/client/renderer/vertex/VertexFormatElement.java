package legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex;

import java.nio.ByteBuffer;

/** 1.12.2 vertex format element: one attribute (type, usage, index, count) of a legacy vertex layout. */
public class VertexFormatElement {
    private final EnumType type;
    private final EnumUsage usage;
    private final int index;
    private final int elementCount;

    public VertexFormatElement(int indexIn, EnumType typeIn, EnumUsage usageIn, int count) {
        if (!isFirstOrUV(indexIn, usageIn)) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/VertexFormat").warn("Multiple vertex elements of the same type other than UVs are not supported. Forcing type to UV.");
            usage = EnumUsage.UV;
        } else {
            usage = usageIn;
        }
        type = typeIn;
        index = indexIn;
        elementCount = count;
    }

    private boolean isFirstOrUV(int indexIn, EnumUsage usageIn) {
        return indexIn == 0 || usageIn == EnumUsage.UV;
    }

    public final EnumType getType() {
        return type;
    }

    public final EnumUsage getUsage() {
        return usage;
    }

    public final int getElementCount() {
        return elementCount;
    }

    public final int getIndex() {
        return index;
    }

    public String toString() {
        return elementCount + "," + usage.getDisplayName() + "," + type.getDisplayName();
    }

    public final int getSize() {
        return type.getSize() * elementCount;
    }

    public final boolean isPositionElement() {
        return usage == EnumUsage.POSITION;
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VertexFormatElement other = (VertexFormatElement) o;
        return elementCount == other.elementCount && index == other.index && type == other.type && usage == other.usage;
    }

    public int hashCode() {
        int i = type.hashCode();
        i = 31 * i + usage.hashCode();
        i = 31 * i + index;
        return 31 * i + elementCount;
    }

    public enum EnumType {
        FLOAT(4, "Float", 5126),
        UBYTE(1, "Unsigned Byte", 5121),
        BYTE(1, "Byte", 5120),
        USHORT(2, "Unsigned Short", 5123),
        SHORT(2, "Short", 5122),
        UINT(4, "Unsigned Int", 5125),
        INT(4, "Int", 5124);

        private final int size;
        private final String displayName;
        private final int glConstant;

        EnumType(int sizeIn, String displayNameIn, int glConstantIn) {
            size = sizeIn;
            displayName = displayNameIn;
            glConstant = glConstantIn;
        }

        public int getSize() {
            return size;
        }

        public String getDisplayName() {
            return displayName;
        }

        public int getGlConstant() {
            return glConstant;
        }
    }

    public enum EnumUsage {
        POSITION("Position"),
        NORMAL("Normal"),
        COLOR("Vertex Color"),
        UV("UV"),
        MATRIX("Bone Matrix"),
        BLEND_WEIGHT("Blend Weight"),
        PADDING("Padding"),
        GENERIC("Generic");

        private final String displayName;

        EnumUsage(String displayNameIn) {
            displayName = displayNameIn;
        }

        public String getDisplayName() {
            return displayName;
        }

        /**
         * Forge: binds the element as a fixed-function client array before a draw. Legacy geometry is decoded and drawn by the
         * render context instead (see Tessellator), so there is no client state to set up.
         */
        public void preDraw(VertexFormat format, int element, int stride, ByteBuffer buffer) {
        }

        public void postDraw(VertexFormat format, int element, int stride, ByteBuffer buffer) {
        }
    }
}
