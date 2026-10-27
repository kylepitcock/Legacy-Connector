package legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex;

import java.util.ArrayList;
import java.util.List;

/** 1.12.2 vertex format: an ordered list of elements and their byte offsets in one vertex. */
public class VertexFormat {
    private final List<VertexFormatElement> elements = new ArrayList<>();
    private final List<Integer> offsets = new ArrayList<>();
    private int vertexSize;
    private int colorElementOffset = -1;
    private final List<Integer> uvOffsetsById = new ArrayList<>();
    private int normalElementOffset = -1;
    private int hashCode;

    public VertexFormat(VertexFormat format) {
        this();
        for (int i = 0; i < format.getElementCount(); i++) addElement(format.getElement(i));
        vertexSize = format.getSize();
    }

    public VertexFormat() {
    }

    public void clear() {
        elements.clear();
        offsets.clear();
        colorElementOffset = -1;
        uvOffsetsById.clear();
        normalElementOffset = -1;
        vertexSize = 0;
        hashCode = 0;
    }

    public VertexFormat addElement(VertexFormatElement element) {
        if (element.isPositionElement() && hasPosition()) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/VertexFormat").warn("VertexFormat error: Trying to add a position VertexFormatElement when one already exists, ignoring.");
            return this;
        }
        elements.add(element);
        offsets.add(vertexSize);
        switch (element.getUsage()) {
            case NORMAL -> normalElementOffset = vertexSize;
            case COLOR -> colorElementOffset = vertexSize;
            case UV -> {
                // (vanilla threw for a lightmap UV without a texture UV before it; such custom formats exist)
                while (uvOffsetsById.size() < element.getIndex()) uvOffsetsById.add(-1);
                uvOffsetsById.add(element.getIndex(), vertexSize);
            }
            default -> {
            }
        }
        vertexSize += element.getSize();
        hashCode = 0;
        return this;
    }

    public boolean hasNormal() {
        return normalElementOffset >= 0;
    }

    public int getNormalOffset() {
        return normalElementOffset;
    }

    public boolean hasColor() {
        return colorElementOffset >= 0;
    }

    public int getColorOffset() {
        return colorElementOffset;
    }

    public boolean hasUvOffset(int id) {
        return uvOffsetsById.size() - 1 >= id;
    }

    public int getUvOffsetById(int id) {
        return uvOffsetsById.get(id);
    }

    public String toString() {
        StringBuilder s = new StringBuilder("format: " + elements.size() + " elements: ");
        for (int i = 0; i < elements.size(); i++) {
            s.append(elements.get(i));
            if (i != elements.size() - 1) s.append(" ");
        }
        return s.toString();
    }

    private boolean hasPosition() {
        for (VertexFormatElement e : elements) if (e.isPositionElement()) return true;
        return false;
    }

    public int getIntegerSize() {
        return getSize() / 4;
    }

    public int getSize() {
        return vertexSize;
    }

    public List<VertexFormatElement> getElements() {
        return elements;
    }

    public int getElementCount() {
        return elements.size();
    }

    public VertexFormatElement getElement(int index) {
        return elements.get(index);
    }

    public int getOffset(int index) {
        return offsets.get(index);
    }

    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VertexFormat other = (VertexFormat) o;
        return vertexSize == other.vertexSize && elements.equals(other.elements) && offsets.equals(other.offsets);
    }

    public int hashCode() {
        if (hashCode != 0) return hashCode;
        int i = elements.hashCode();
        i = 31 * i + offsets.hashCode();
        i = 31 * i + vertexSize;
        hashCode = i;
        return i;
    }

    /** Offset of the first element with this usage (and UV index), or -1. */
    public int mcc$offsetOf(VertexFormatElement.EnumUsage usage, int index) {
        for (int i = 0; i < elements.size(); i++) {
            VertexFormatElement e = elements.get(i);
            if (e.getUsage() == usage && e.getIndex() == index) return offsets.get(i);
        }
        return -1;
    }

    /** The element with this usage (and UV index), or null. */
    public VertexFormatElement mcc$elementOf(VertexFormatElement.EnumUsage usage, int index) {
        for (VertexFormatElement e : elements) if (e.getUsage() == usage && e.getIndex() == index) return e;
        return null;
    }
}
