package legacyconnector.v1_12_2.net.minecraft.client.renderer;

/** 1.12.2 Tessellator: the shared buffer builder, drawn into the active render context on {@link #draw()}. */
public class Tessellator {
    private final BufferBuilder buffer;
    private final WorldVertexBufferUploader vboUploader = new WorldVertexBufferUploader();
    private static final Tessellator INSTANCE = new Tessellator(2097152);

    public static Tessellator getInstance() {
        return INSTANCE;
    }

    public Tessellator(int bufferSize) {
        buffer = new BufferBuilder(bufferSize);
    }

    public void draw() {
        buffer.finishDrawing();
        vboUploader.draw(buffer);
    }

    public BufferBuilder getBuffer() {
        return buffer;
    }

    /** Leaves a draw a failed legacy renderer began and never ended (see the render context setup). */
    public static void mcc$reset() {
        if (INSTANCE.buffer.isDrawing) INSTANCE.buffer.mcc$abort();
    }
}
