package legacyconnector.v1_12_2.net.minecraft.client.renderer;

import legacyconnector.v1_12_2.net.minecraft.client.renderer.vertex.VertexBuffer;

/** 1.12.2 uploader into a {@link VertexBuffer} (chunk and sky buffers): the finished vertices become the buffer's data. */
public class VertexBufferUploader extends WorldVertexBufferUploader {
    private VertexBuffer vertexBuffer;

    @Override
    public void draw(BufferBuilder buffer) {
        buffer.reset();
        if (vertexBuffer != null) vertexBuffer.bufferData(buffer.getByteBuffer());
    }

    public void setVertexBuffer(VertexBuffer vertexBufferIn) {
        vertexBuffer = vertexBufferIn;
    }
}
