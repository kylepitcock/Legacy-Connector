package legacyconnector.vb1_7_3.net.minecraft.client.util;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import legacyconnector.vb1_7_3.org.lwjgl.BufferUtils;
import legacyconnector.vb1_7_3.org.lwjgl.opengl.GL11;

/**
 * Beta 1.7.3 {@code GLAllocation}: display list and texture name allocation and native-order buffers. Display lists are the layer's
 * captured geometry (mods that compile their own model parts into lists, Airships), so ids come from the same allocator as {@code glGenLists}.
 */
public class GlAllocationUtils {
    public static List<Integer> DISPLAY_LISTS = new ArrayList<>();
    public static List<Integer> TEXTURE_NAMES = new ArrayList<>();

    public static synchronized int generateDisplayLists(int count) {
        int first = GL11.glGenLists(count);
        DISPLAY_LISTS.add(first);
        DISPLAY_LISTS.add(count);
        return first;
    }

    public static synchronized void generateTextureNames(IntBuffer names) {
        for (int i = names.position(); i < names.limit(); i++) {
            int name = GL11.glGenTextures();
            names.put(i, name);
            TEXTURE_NAMES.add(name);
        }
    }

    public static synchronized void deleteDisplayLists(int list) {
        int at = DISPLAY_LISTS.indexOf(list);
        if (at >= 0 && at + 1 < DISPLAY_LISTS.size() && at % 2 == 0) {
            GL11.glDeleteLists(list, DISPLAY_LISTS.get(at + 1));
            DISPLAY_LISTS.remove(at + 1);
            DISPLAY_LISTS.remove(at);
        }
    }

    public static synchronized void clear() {
        for (int i = 0; i + 1 < DISPLAY_LISTS.size(); i += 2) GL11.glDeleteLists(DISPLAY_LISTS.get(i), DISPLAY_LISTS.get(i + 1));
        DISPLAY_LISTS.clear();
        TEXTURE_NAMES.clear();
    }

    public static ByteBuffer allocateByteBuffer(int size) { return BufferUtils.createByteBuffer(size); }

    public static IntBuffer allocateIntBuffer(int size) { return BufferUtils.createIntBuffer(size); }

    public static FloatBuffer allocateFloatBuffer(int size) { return BufferUtils.createFloatBuffer(size); }
}
