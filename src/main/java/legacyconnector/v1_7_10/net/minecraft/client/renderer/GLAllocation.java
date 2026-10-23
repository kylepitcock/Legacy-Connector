package legacyconnector.v1_7_10.net.minecraft.client.renderer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/** Direct buffers and display-list ids as legacy rendering code allocates them. */
public class GLAllocation {
    private static final AtomicInteger NEXT_LIST = new AtomicInteger(1);

    public static synchronized int generateDisplayLists(int range) {
        return org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists.generate(range);
    }

    public static synchronized void deleteDisplayLists(int list) {
        org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists.delete(list, 1);
    }

    public static synchronized void deleteDisplayLists(int list, int range) {
        org.kylepitcock.legacyconnector.v1_7_10.client.DisplayLists.delete(list, range);
    }

    public static synchronized ByteBuffer createDirectByteBuffer(int size) {
        return ByteBuffer.allocateDirect(size).order(ByteOrder.nativeOrder());
    }

    public static IntBuffer createDirectIntBuffer(int size) {
        return createDirectByteBuffer(size << 2).asIntBuffer();
    }

    public static FloatBuffer createDirectFloatBuffer(int size) {
        return createDirectByteBuffer(size << 2).asFloatBuffer();
    }
}
