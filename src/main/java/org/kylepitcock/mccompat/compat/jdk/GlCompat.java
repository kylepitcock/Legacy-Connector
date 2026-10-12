package org.kylepitcock.mccompat.compat.jdk;

import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Legacy shader entry points. 1.7.10 mods write GLSL for the fixed-function pipeline (gl_Vertex, gl_ModelViewMatrix)
 * which the modern core profile cannot compile, and set shaders up on loading threads that have no OpenGL context,
 * where a driver call would abort the JVM. Shaders are therefore virtual: creation hands out ids that never reach the
 * driver and compile/link report success, so mods carry on (their effect simply is not applied).
 */
public final class GlCompat {
    private static final AtomicInteger NEXT = new AtomicInteger(0x20000000);
    private static final int COMPILE_STATUS = 35713, LINK_STATUS = 35714, VALIDATE_STATUS = 35715, OBJECT_COMPILE_STATUS_ARB = 35725;

    private GlCompat() {}

    public static int glCreateShader(int type) {
        return NEXT.getAndIncrement();
    }

    public static void glShaderSource(int shader, ByteBuffer source) {
    }

    public static void glShaderSource(int shader, CharSequence source) {
    }

    public static void glCompileShader(int shader) {
    }

    public static int glGetShader(int shader, int pname) {
        return pname == COMPILE_STATUS || pname == LINK_STATUS || pname == VALIDATE_STATUS || pname == OBJECT_COMPILE_STATUS_ARB ? 1 : 0;
    }

    public static int glGetShaderi(int shader, int pname) {
        return glGetShader(shader, pname);
    }

    public static int glGetProgrami(int program, int pname) {
        return glGetShader(program, pname);
    }

    public static void glGetShaderiv(int shader, int pname, java.nio.IntBuffer out) {
        out.put(out.position(), glGetShader(shader, pname));
    }

    public static void glGetProgramiv(int program, int pname, java.nio.IntBuffer out) {
        out.put(out.position(), glGetShader(program, pname));
    }

    public static String glGetShaderInfoLog(int shader, int maxLength) {
        return "";
    }

    public static int glCreateProgram() {
        return NEXT.getAndIncrement();
    }

    public static void glAttachShader(int program, int shader) {
    }

    public static void glLinkProgram(int program) {
    }

    public static void glValidateProgram(int program) {
    }

    public static int glGetProgram(int program, int pname) {
        return glGetShader(program, pname);
    }

    public static String glGetProgramInfoLog(int program, int maxLength) {
        return "";
    }

    public static void glUseProgram(int program) {
    }

    public static void glDeleteShader(int shader) {
    }

    public static void glDeleteProgram(int program) {
    }

    public static int glGetUniformLocation(int program, CharSequence name) {
        return -1;
    }

    public static void glBindAttribLocation(int program, int index, CharSequence name) {
    }

    // ARB_shader_objects spellings of the same calls.
    public static int glCreateShaderObjectARB(int type) {
        return glCreateShader(type);
    }

    public static void glShaderSourceARB(int shader, ByteBuffer source) {
    }

    public static void glShaderSourceARB(int shader, CharSequence source) {
    }

    public static void glCompileShaderARB(int shader) {
    }

    public static int glGetObjectParameteriARB(int object, int pname) {
        return glGetShader(object, pname);
    }

    public static String glGetInfoLogARB(int object, int maxLength) {
        return "";
    }

    public static int glCreateProgramObjectARB() {
        return glCreateProgram();
    }

    public static void glAttachObjectARB(int program, int shader) {
    }

    public static void glLinkProgramARB(int program) {
    }

    public static void glValidateProgramARB(int program) {
    }

    public static void glUseProgramObjectARB(int program) {
    }

    public static void glDeleteObjectARB(int object) {
    }

    public static int glGetUniformLocationARB(int program, CharSequence name) {
        return -1;
    }
}
