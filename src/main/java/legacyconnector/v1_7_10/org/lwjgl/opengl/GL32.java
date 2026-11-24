package legacyconnector.v1_7_10.org.lwjgl.opengl;

/**
 * Legacy {@code org.lwjgl.opengl.GL32}. LWJGL 2 wrapped sync objects in {@link GLSync} where LWJGL 3 returns the pointer, so the class cannot extend
 * LWJGL 3's: the sync calls are written here, everything else forwards to it (generated from LWJGL 3.3.3's GL32; the LWJGL 2 names that differ
 * from LWJGL 3's follow the sync calls). Relocated for the 1.4.7 to 1.7.10 family only.
 */
public class GL32 {
    public static final int GL_CONTEXT_PROFILE_MASK = 0x9126;
    public static final int GL_CONTEXT_CORE_PROFILE_BIT = 0x1;
    public static final int GL_CONTEXT_COMPATIBILITY_PROFILE_BIT = 0x2;
    public static final int GL_MAX_VERTEX_OUTPUT_COMPONENTS = 0x9122;
    public static final int GL_MAX_GEOMETRY_INPUT_COMPONENTS = 0x9123;
    public static final int GL_MAX_GEOMETRY_OUTPUT_COMPONENTS = 0x9124;
    public static final int GL_MAX_FRAGMENT_INPUT_COMPONENTS = 0x9125;
    public static final int GL_FIRST_VERTEX_CONVENTION = 0x8E4D;
    public static final int GL_LAST_VERTEX_CONVENTION = 0x8E4E;
    public static final int GL_PROVOKING_VERTEX = 0x8E4F;
    public static final int GL_QUADS_FOLLOW_PROVOKING_VERTEX_CONVENTION = 0x8E4C;
    public static final int GL_TEXTURE_CUBE_MAP_SEAMLESS = 0x884F;
    public static final int GL_SAMPLE_POSITION = 0x8E50;
    public static final int GL_SAMPLE_MASK = 0x8E51;
    public static final int GL_SAMPLE_MASK_VALUE = 0x8E52;
    public static final int GL_TEXTURE_2D_MULTISAMPLE = 0x9100;
    public static final int GL_PROXY_TEXTURE_2D_MULTISAMPLE = 0x9101;
    public static final int GL_TEXTURE_2D_MULTISAMPLE_ARRAY = 0x9102;
    public static final int GL_PROXY_TEXTURE_2D_MULTISAMPLE_ARRAY = 0x9103;
    public static final int GL_MAX_SAMPLE_MASK_WORDS = 0x8E59;
    public static final int GL_MAX_COLOR_TEXTURE_SAMPLES = 0x910E;
    public static final int GL_MAX_DEPTH_TEXTURE_SAMPLES = 0x910F;
    public static final int GL_MAX_INTEGER_SAMPLES = 0x9110;
    public static final int GL_TEXTURE_BINDING_2D_MULTISAMPLE = 0x9104;
    public static final int GL_TEXTURE_BINDING_2D_MULTISAMPLE_ARRAY = 0x9105;
    public static final int GL_TEXTURE_SAMPLES = 0x9106;
    public static final int GL_TEXTURE_FIXED_SAMPLE_LOCATIONS = 0x9107;
    public static final int GL_SAMPLER_2D_MULTISAMPLE = 0x9108;
    public static final int GL_INT_SAMPLER_2D_MULTISAMPLE = 0x9109;
    public static final int GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE = 0x910A;
    public static final int GL_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910B;
    public static final int GL_INT_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910C;
    public static final int GL_UNSIGNED_INT_SAMPLER_2D_MULTISAMPLE_ARRAY = 0x910D;
    public static final int GL_DEPTH_CLAMP = 0x864F;
    public static final int GL_GEOMETRY_SHADER = 0x8DD9;
    public static final int GL_GEOMETRY_VERTICES_OUT = 0x8DDA;
    public static final int GL_GEOMETRY_INPUT_TYPE = 0x8DDB;
    public static final int GL_GEOMETRY_OUTPUT_TYPE = 0x8DDC;
    public static final int GL_MAX_GEOMETRY_TEXTURE_IMAGE_UNITS = 0x8C29;
    public static final int GL_MAX_GEOMETRY_UNIFORM_COMPONENTS = 0x8DDF;
    public static final int GL_MAX_GEOMETRY_OUTPUT_VERTICES = 0x8DE0;
    public static final int GL_MAX_GEOMETRY_TOTAL_OUTPUT_COMPONENTS = 0x8DE1;
    public static final int GL_LINES_ADJACENCY = 0xA;
    public static final int GL_LINE_STRIP_ADJACENCY = 0xB;
    public static final int GL_TRIANGLES_ADJACENCY = 0xC;
    public static final int GL_TRIANGLE_STRIP_ADJACENCY = 0xD;
    public static final int GL_FRAMEBUFFER_INCOMPLETE_LAYER_TARGETS = 0x8DA8;
    public static final int GL_FRAMEBUFFER_ATTACHMENT_LAYERED = 0x8DA7;
    public static final int GL_PROGRAM_POINT_SIZE = 0x8642;
    public static final int GL_MAX_SERVER_WAIT_TIMEOUT = 0x9111;
    public static final int GL_OBJECT_TYPE = 0x9112;
    public static final int GL_SYNC_CONDITION = 0x9113;
    public static final int GL_SYNC_STATUS = 0x9114;
    public static final int GL_SYNC_FLAGS = 0x9115;
    public static final int GL_SYNC_FENCE = 0x9116;
    public static final int GL_SYNC_GPU_COMMANDS_COMPLETE = 0x9117;
    public static final int GL_UNSIGNALED = 0x9118;
    public static final int GL_SIGNALED = 0x9119;
    public static final int GL_SYNC_FLUSH_COMMANDS_BIT = 0x1;
    public static final int GL_ALREADY_SIGNALED = 0x911A;
    public static final int GL_TIMEOUT_EXPIRED = 0x911B;
    public static final int GL_CONDITION_SATISFIED = 0x911C;
    public static final int GL_WAIT_FAILED = 0x911D;

    protected GL32() {
    }

    public static GLSync glFenceSync(int condition, int flags) {
        return new GLSync(org.lwjgl.opengl.GL32.glFenceSync(condition, flags));
    }

    public static boolean glIsSync(GLSync sync) {
        return sync != null && org.lwjgl.opengl.GL32.glIsSync(sync.getPointer());
    }

    public static void glDeleteSync(GLSync sync) {
        if (sync != null) org.lwjgl.opengl.GL32.glDeleteSync(sync.getPointer());
    }

    public static int glClientWaitSync(GLSync sync, int flags, long timeout) {
        return org.lwjgl.opengl.GL32.glClientWaitSync(sync.getPointer(), flags, timeout);
    }

    public static void glWaitSync(GLSync sync, int flags, long timeout) {
        org.lwjgl.opengl.GL32.glWaitSync(sync.getPointer(), flags, timeout);
    }

    // LWJGL 2 names and shapes
    public static int glGetSync(GLSync sync, int pname) {
        return org.lwjgl.opengl.GL32.glGetSynci(sync.getPointer(), pname, null);
    }

    public static void glGetSync(GLSync sync, int pname, java.nio.IntBuffer length, java.nio.IntBuffer values) {
        org.lwjgl.opengl.GL32.glGetSynciv(sync.getPointer(), pname, length, values);
    }

    public static void glGetInteger64(int pname, java.nio.LongBuffer data) {
        org.lwjgl.opengl.GL32.glGetInteger64v(pname, data);
    }

    public static long glGetInteger64(int pname, int index) {
        return org.lwjgl.opengl.GL32.glGetInteger64i(pname, index);
    }

    public static long glGetBufferParameter(int target, int pname) {
        return org.lwjgl.opengl.GL32.glGetBufferParameteri64(target, pname);
    }

    public static void glGetMultisample(int pname, int index, java.nio.FloatBuffer val) {
        org.lwjgl.opengl.GL32.glGetMultisamplefv(pname, index, val);
    }

    // generated forwards to LWJGL 3
    public static void glGetBufferParameteri64v(int p0, int p1, java.nio.LongBuffer p2) {
        org.lwjgl.opengl.GL32.glGetBufferParameteri64v(p0, p1, p2);
    }

    public static long glGetBufferParameteri64(int p0, int p1) {
        return org.lwjgl.opengl.GL32.glGetBufferParameteri64(p0, p1);
    }

    public static void glDrawElementsBaseVertex(int p0, int p1, int p2, long p3, int p4) {
        org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(p0, p1, p2, p3, p4);
    }

    public static void glDrawElementsBaseVertex(int p0, int p1, java.nio.ByteBuffer p2, int p3) {
        org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(p0, p1, p2, p3);
    }

    public static void glDrawElementsBaseVertex(int p0, java.nio.ByteBuffer p1, int p2) {
        org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(p0, p1, p2);
    }

    public static void glDrawElementsBaseVertex(int p0, java.nio.ShortBuffer p1, int p2) {
        org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(p0, p1, p2);
    }

    public static void glDrawElementsBaseVertex(int p0, java.nio.IntBuffer p1, int p2) {
        org.lwjgl.opengl.GL32.glDrawElementsBaseVertex(p0, p1, p2);
    }

    public static void glDrawRangeElementsBaseVertex(int p0, int p1, int p2, int p3, int p4, long p5, int p6) {
        org.lwjgl.opengl.GL32.glDrawRangeElementsBaseVertex(p0, p1, p2, p3, p4, p5, p6);
    }

    public static void glDrawRangeElementsBaseVertex(int p0, int p1, int p2, int p3, java.nio.ByteBuffer p4, int p5) {
        org.lwjgl.opengl.GL32.glDrawRangeElementsBaseVertex(p0, p1, p2, p3, p4, p5);
    }

    public static void glDrawRangeElementsBaseVertex(int p0, int p1, int p2, java.nio.ByteBuffer p3, int p4) {
        org.lwjgl.opengl.GL32.glDrawRangeElementsBaseVertex(p0, p1, p2, p3, p4);
    }

    public static void glDrawRangeElementsBaseVertex(int p0, int p1, int p2, java.nio.ShortBuffer p3, int p4) {
        org.lwjgl.opengl.GL32.glDrawRangeElementsBaseVertex(p0, p1, p2, p3, p4);
    }

    public static void glDrawRangeElementsBaseVertex(int p0, int p1, int p2, java.nio.IntBuffer p3, int p4) {
        org.lwjgl.opengl.GL32.glDrawRangeElementsBaseVertex(p0, p1, p2, p3, p4);
    }

    public static void glDrawElementsInstancedBaseVertex(int p0, int p1, int p2, long p3, int p4, int p5) {
        org.lwjgl.opengl.GL32.glDrawElementsInstancedBaseVertex(p0, p1, p2, p3, p4, p5);
    }

    public static void glDrawElementsInstancedBaseVertex(int p0, int p1, java.nio.ByteBuffer p2, int p3, int p4) {
        org.lwjgl.opengl.GL32.glDrawElementsInstancedBaseVertex(p0, p1, p2, p3, p4);
    }

    public static void glDrawElementsInstancedBaseVertex(int p0, java.nio.ByteBuffer p1, int p2, int p3) {
        org.lwjgl.opengl.GL32.glDrawElementsInstancedBaseVertex(p0, p1, p2, p3);
    }

    public static void glDrawElementsInstancedBaseVertex(int p0, java.nio.ShortBuffer p1, int p2, int p3) {
        org.lwjgl.opengl.GL32.glDrawElementsInstancedBaseVertex(p0, p1, p2, p3);
    }

    public static void glDrawElementsInstancedBaseVertex(int p0, java.nio.IntBuffer p1, int p2, int p3) {
        org.lwjgl.opengl.GL32.glDrawElementsInstancedBaseVertex(p0, p1, p2, p3);
    }

    public static void glMultiDrawElementsBaseVertex(int p0, java.nio.IntBuffer p1, int p2, org.lwjgl.PointerBuffer p3, java.nio.IntBuffer p4) {
        org.lwjgl.opengl.GL32.glMultiDrawElementsBaseVertex(p0, p1, p2, p3, p4);
    }

    public static void glProvokingVertex(int p0) {
        org.lwjgl.opengl.GL32.glProvokingVertex(p0);
    }

    public static void glTexImage2DMultisample(int p0, int p1, int p2, int p3, int p4, boolean p5) {
        org.lwjgl.opengl.GL32.glTexImage2DMultisample(p0, p1, p2, p3, p4, p5);
    }

    public static void glTexImage3DMultisample(int p0, int p1, int p2, int p3, int p4, int p5, boolean p6) {
        org.lwjgl.opengl.GL32.glTexImage3DMultisample(p0, p1, p2, p3, p4, p5, p6);
    }

    public static void glGetMultisamplefv(int p0, int p1, java.nio.FloatBuffer p2) {
        org.lwjgl.opengl.GL32.glGetMultisamplefv(p0, p1, p2);
    }

    public static float glGetMultisamplef(int p0, int p1) {
        return org.lwjgl.opengl.GL32.glGetMultisamplef(p0, p1);
    }

    public static void glSampleMaski(int p0, int p1) {
        org.lwjgl.opengl.GL32.glSampleMaski(p0, p1);
    }

    public static void glFramebufferTexture(int p0, int p1, int p2, int p3) {
        org.lwjgl.opengl.GL32.glFramebufferTexture(p0, p1, p2, p3);
    }

    public static boolean glIsSync(long p0) {
        return org.lwjgl.opengl.GL32.glIsSync(p0);
    }

    public static void glDeleteSync(long p0) {
        org.lwjgl.opengl.GL32.glDeleteSync(p0);
    }

    public static int glClientWaitSync(long p0, int p1, long p2) {
        return org.lwjgl.opengl.GL32.glClientWaitSync(p0, p1, p2);
    }

    public static void glWaitSync(long p0, int p1, long p2) {
        org.lwjgl.opengl.GL32.glWaitSync(p0, p1, p2);
    }

    public static void glGetInteger64v(int p0, java.nio.LongBuffer p1) {
        org.lwjgl.opengl.GL32.glGetInteger64v(p0, p1);
    }

    public static long glGetInteger64(int p0) {
        return org.lwjgl.opengl.GL32.glGetInteger64(p0);
    }

    public static void glGetInteger64i_v(int p0, int p1, java.nio.LongBuffer p2) {
        org.lwjgl.opengl.GL32.glGetInteger64i_v(p0, p1, p2);
    }

    public static long glGetInteger64i(int p0, int p1) {
        return org.lwjgl.opengl.GL32.glGetInteger64i(p0, p1);
    }

    public static void glGetSynciv(long p0, int p1, java.nio.IntBuffer p2, java.nio.IntBuffer p3) {
        org.lwjgl.opengl.GL32.glGetSynciv(p0, p1, p2, p3);
    }

    public static int glGetSynci(long p0, int p1, java.nio.IntBuffer p2) {
        return org.lwjgl.opengl.GL32.glGetSynci(p0, p1, p2);
    }

    public static void glGetBufferParameteri64v(int p0, int p1, long[] p2) {
        org.lwjgl.opengl.GL32.glGetBufferParameteri64v(p0, p1, p2);
    }

    public static void glMultiDrawElementsBaseVertex(int p0, int[] p1, int p2, org.lwjgl.PointerBuffer p3, int[] p4) {
        org.lwjgl.opengl.GL32.glMultiDrawElementsBaseVertex(p0, p1, p2, p3, p4);
    }

    public static void glGetMultisamplefv(int p0, int p1, float[] p2) {
        org.lwjgl.opengl.GL32.glGetMultisamplefv(p0, p1, p2);
    }

    public static void glGetInteger64v(int p0, long[] p1) {
        org.lwjgl.opengl.GL32.glGetInteger64v(p0, p1);
    }

    public static void glGetInteger64i_v(int p0, int p1, long[] p2) {
        org.lwjgl.opengl.GL32.glGetInteger64i_v(p0, p1, p2);
    }

    public static void glGetSynciv(long p0, int p1, int[] p2, int[] p3) {
        org.lwjgl.opengl.GL32.glGetSynciv(p0, p1, p2, p3);
    }
}
