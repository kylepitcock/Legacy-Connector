package legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex;

import java.nio.ByteBuffer;
import legacyconnector.v1_12_2.net.minecraft.client.renderer.WorldVertexBufferUploader;
import org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists;
import org.kylepitcock.legacyconnector.v1_7_10.client.LegacyVertices;

/**
 * 1.12.2 vertex buffer object. The legacy bytes are kept on the heap (a GL buffer would be drawn with the fixed-function client
 * arrays the core profile lacks) and every {@link #drawArrays} decodes them into the render context, like an immediate draw.
 */
public class VertexBuffer {
    private final VertexFormat vertexFormat;
    private ByteBuffer data;
    private int count;

    public VertexBuffer(VertexFormat vertexFormatIn) {
        vertexFormat = vertexFormatIn;
    }

    public void bindBuffer() {
    }

    public void bufferData(ByteBuffer data) {
        int size = data.limit() - data.position();
        ByteBuffer copy = ByteBuffer.allocate(size).order(java.nio.ByteOrder.nativeOrder());
        copy.put(data.duplicate());
        copy.flip();
        this.data = copy;
        count = vertexFormat.getSize() == 0 ? 0 : size / vertexFormat.getSize();
    }

    public void drawArrays(int mode) {
        if (data == null || count == 0) return;
        LegacyVertices.Snapshot draw = WorldVertexBufferUploader.decode(new LegacyVertices(), data, vertexFormat, count, mode, false);
        if (!DisplayLists.record(() -> LegacyVertices.emit(draw))) LegacyVertices.emit(draw);
    }

    public void unbindBuffer() {
    }

    public void deleteGlBuffers() {
        data = null;
        count = 0;
    }
}
