package org.kylepitcock.legacyconnector.loader.transform;

import java.util.HashMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/**
 * Call-site rewrites for Java 8 -> Java 21 behaviour changes that cannot be fixed by renaming a class.
 *
 * <p>Every redirect keeps the operand stack shape identical (the replacement is a static method taking the
 * receiver as first argument), so existing stack map frames stay valid and no frame recomputation - which
 * would require loading the whole class hierarchy - is necessary.</p>
 */
public final class JavaCompatRedirects {
    private static final String COMPAT = "org/kylepitcock/legacyconnector/compat/jdk/";

    /** Key: {@code owner.name+desc} as it appears after relocation. */
    private static final Map<String, MethodInsnNode> INVOKES = new HashMap<>();

    static {
        // Java 12+: Field.modifiers is filtered from reflection, breaking the classic "remove final" hack.
        // Java 18+: static final fields can no longer be written through reflection at all.
        virtualToStatic("java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", COMPAT + "ReflectionCompat");
        // LaunchWrapper's private runTransformers, looked up on whatever loader loaded the mod (AE2's generated part classes).
        virtualToStatic("java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", COMPAT + "ReflectionCompat");
        // SRG names built at run time (production mods reflect with them) resolve to the shim's MCP names (SrgNames).
        virtualToStatic("java/lang/Class", "getField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/Class", "getMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setInt", "(Ljava/lang/Object;I)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "set", "(Ljava/lang/Object;Ljava/lang/Object;)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setBoolean", "(Ljava/lang/Object;Z)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setLong", "(Ljava/lang/Object;J)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setFloat", "(Ljava/lang/Object;F)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "setDouble", "(Ljava/lang/Object;D)V", COMPAT + "ReflectionCompat");
        // Reads of (and access to) private fields of JDK classes, which the module system refuses since Java 16.
        virtualToStatic("java/lang/reflect/Field", "setAccessible", "(Z)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/AccessibleObject", "setAccessible", "(Z)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Method", "setAccessible", "(Z)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Constructor", "setAccessible", "(Z)V", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "getInt", "(Ljava/lang/Object;)I", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "getLong", "(Ljava/lang/Object;)J", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "getBoolean", "(Ljava/lang/Object;)Z", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "getFloat", "(Ljava/lang/Object;)F", COMPAT + "ReflectionCompat");
        virtualToStatic("java/lang/reflect/Field", "getDouble", "(Ljava/lang/Object;)D", COMPAT + "ReflectionCompat");
        // Libraries that invoke handlers reflectively often log only the InvocationTargetException; note the cause.
        virtualToStatic("java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", COMPAT + "ReflectionCompat");
        // Mod jars are served from union: URLs; mods listing their own files expect jar:/file: (ResourceCompat).
        virtualToStatic("java/lang/Class", "getResource", "(Ljava/lang/String;)Ljava/net/URL;", COMPAT + "ResourceCompat");
        virtualToStatic("java/lang/ClassLoader", "getResource", "(Ljava/lang/String;)Ljava/net/URL;", COMPAT + "ResourceCompat");
        virtualToStatic("java/lang/Class", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", COMPAT + "ResourceCompat");
        virtualToStatic("java/lang/ClassLoader", "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", COMPAT + "ResourceCompat");
        virtualToStatic("java/lang/ClassLoader", "getResources", "(Ljava/lang/String;)Ljava/util/Enumeration;", COMPAT + "ResourceCompat");
        virtualToStatic("java/security/CodeSource", "getLocation", "()Ljava/net/URL;", COMPAT + "ResourceCompat");
        // Java 20+: Thread.stop/suspend/resume throw UnsupportedOperationException.
        virtualToStatic("java/lang/Thread", "stop", "()V", COMPAT + "JdkCompat");
        // Java 11: Thread.stop(Throwable) was removed.
        virtualToStatic("java/lang/Thread", "stop", "(Ljava/lang/Throwable;)V", COMPAT + "JdkCompat");
        virtualToStatic("java/lang/Thread", "suspend", "()V", COMPAT + "JdkCompat");
        virtualToStatic("java/lang/Thread", "resume", "()V", COMPAT + "JdkCompat");
        // Java 18+: installing a SecurityManager throws by default.
        // Swing dialogs (fatal-error popups, update notices) cannot show in the headless game: log them instead.
        for (String desc : new String[] {"(Ljava/awt/Component;Ljava/lang/Object;)V", "(Ljava/awt/Component;Ljava/lang/Object;Ljava/lang/String;I)V",
                "(Ljava/awt/Component;Ljava/lang/Object;Ljava/lang/String;ILjavax/swing/Icon;)V"}) {
            staticToStatic("javax/swing/JOptionPane", "showMessageDialog", desc, COMPAT + "DialogCompat", "showMessageDialog");
        }
        for (String desc : new String[] {"(Ljava/awt/Component;Ljava/lang/Object;)I", "(Ljava/awt/Component;Ljava/lang/Object;Ljava/lang/String;I)I",
                "(Ljava/awt/Component;Ljava/lang/Object;Ljava/lang/String;II)I"}) {
            staticToStatic("javax/swing/JOptionPane", "showConfirmDialog", desc, COMPAT + "DialogCompat", "showConfirmDialog");
        }
        staticToStatic("javax/swing/JOptionPane", "showOptionDialog",
                "(Ljava/awt/Component;Ljava/lang/Object;Ljava/lang/String;IILjavax/swing/Icon;[Ljava/lang/Object;Ljava/lang/Object;)I",
                COMPAT + "DialogCompat", "showOptionDialog");
        // A mod giving up must not end the game: System.exit fails that mod instead.
        staticToStatic("java/lang/System", "exit", "(I)V", COMPAT + "JdkCompat", "exit");
        // log4j 2.0-beta9 factories, replaced by builders (JourneyMap's own log file)
        staticToStatic("org/apache/logging/log4j/core/layout/PatternLayout", "createLayout",
                "(Ljava/lang/String;Lorg/apache/logging/log4j/core/config/Configuration;Lorg/apache/logging/log4j/core/pattern/RegexReplacement;"
                        + "Ljava/lang/String;Ljava/lang/String;)Lorg/apache/logging/log4j/core/layout/PatternLayout;", COMPAT + "Log4jCompat", "createLayout");
        staticToStatic("org/apache/logging/log4j/core/appender/RandomAccessFileAppender", "createAppender",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Lorg/apache/logging/log4j/core/Layout;"
                        + "Lorg/apache/logging/log4j/core/Filter;Ljava/lang/String;Ljava/lang/String;Lorg/apache/logging/log4j/core/config/Configuration;)"
                        + "Lorg/apache/logging/log4j/core/appender/RandomAccessFileAppender;", COMPAT + "Log4jCompat", "createAppender");
        staticToStatic("java/lang/System", "setSecurityManager", "(Ljava/lang/SecurityManager;)V", COMPAT + "JdkCompat", "setSecurityManager");
        // ASM 5 -> 9: descriptor parsing became strict.
        staticToStatic("org/objectweb/asm/Type", "getType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;", COMPAT + "LibraryCompat", "asmGetType");
        staticToStatic("org/objectweb/asm/Type", "getReturnType", "(Ljava/lang/String;)Lorg/objectweb/asm/Type;", COMPAT + "LibraryCompat", "asmGetReturnType");
        staticToStatic("org/objectweb/asm/Type", "getArgumentTypes", "(Ljava/lang/String;)[Lorg/objectweb/asm/Type;", COMPAT + "LibraryCompat", "asmGetArgumentTypes");
        // Guava 17 -> 32 removals.
        staticToStatic("com/google/common/base/Objects", "toStringHelper", "(Ljava/lang/Object;)Lcom/google/common/base/MoreObjects$ToStringHelper;", "com/google/common/base/MoreObjects", "toStringHelper");
        staticToStatic("com/google/common/base/Objects", "toStringHelper", "(Ljava/lang/Class;)Lcom/google/common/base/MoreObjects$ToStringHelper;", "com/google/common/base/MoreObjects", "toStringHelper");
        staticToStatic("com/google/common/base/Objects", "toStringHelper", "(Ljava/lang/String;)Lcom/google/common/base/MoreObjects$ToStringHelper;", "com/google/common/base/MoreObjects", "toStringHelper");
        staticToStatic("com/google/common/base/Objects", "firstNonNull", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", "com/google/common/base/MoreObjects", "firstNonNull");
        staticToStatic("com/google/common/collect/Iterators", "emptyIterator", "()Lcom/google/common/collect/UnmodifiableIterator;", COMPAT + "GuavaCompat", "emptyIterator");
        virtualToStatic("com/google/common/base/Stopwatch", "elapsedMillis", "()J", COMPAT + "GuavaCompat");
        virtualToStatic("com/google/common/base/Stopwatch", "elapsedTime", "(Ljava/util/concurrent/TimeUnit;)J", COMPAT + "GuavaCompat");
        // Guava scans only URLClassLoaders; mods run in module class loaders (Logistics Pipes finds its packets this way).
        staticToStatic("com/google/common/reflect/ClassPath", "from", "(Ljava/lang/ClassLoader;)Lcom/google/common/reflect/ClassPath;", COMPAT + "GuavaCompat", "classPathFrom");
        // Guava 26: Futures.addCallback needs an executor.
        staticToStatic("com/google/common/util/concurrent/Futures", "addCallback",
                "(Lcom/google/common/util/concurrent/ListenableFuture;Lcom/google/common/util/concurrent/FutureCallback;)V", COMPAT + "GuavaCompat", "addCallback");
        // fastutil 8: no boxed get(Integer) on int-keyed maps.
        virtualToStatic("it/unimi/dsi/fastutil/ints/Int2ObjectOpenHashMap", "get", "(Ljava/lang/Integer;)Ljava/lang/Object;", COMPAT + "LibraryCompat");
        // LWJGL 2 -> 3: typed-buffer overloads got explicit suffixes (glUniform1(int, FloatBuffer) -> glUniform1fv).
        String gl20 = "org/lwjgl/opengl/GL20";
        staticToStatic(gl20, "glGetShader", "(IILjava/nio/IntBuffer;)V", COMPAT + "GlCompat", "glGetShaderiv");
        staticToStatic(gl20, "glGetProgram", "(IILjava/nio/IntBuffer;)V", COMPAT + "GlCompat", "glGetProgramiv");
        staticToStatic(gl20, "glGetShaderi", "(II)I", COMPAT + "GlCompat", "glGetShaderi");
        staticToStatic(gl20, "glGetProgrami", "(II)I", COMPAT + "GlCompat", "glGetProgrami");
        staticToStatic(gl20, "glGetShaderiv", "(IILjava/nio/IntBuffer;)V", COMPAT + "GlCompat", "glGetShaderiv");
        staticToStatic(gl20, "glGetProgramiv", "(IILjava/nio/IntBuffer;)V", COMPAT + "GlCompat", "glGetProgramiv");
        // Shader set-up runs on loading threads that have no GL context (the driver call would abort the JVM).
        String gl = COMPAT + "GlCompat";
        staticToStatic(gl20, "glShaderSource", "(ILjava/nio/ByteBuffer;)V", gl, "glShaderSource");
        staticToStatic(gl20, "glShaderSource", "(ILjava/lang/CharSequence;)V", gl, "glShaderSource");
        staticToStatic(gl20, "glCreateShader", "(I)I", gl, "glCreateShader");
        staticToStatic(gl20, "glCompileShader", "(I)V", gl, "glCompileShader");
        staticToStatic(gl20, "glGetShader", "(II)I", gl, "glGetShader");
        staticToStatic(gl20, "glGetShaderInfoLog", "(II)Ljava/lang/String;", gl, "glGetShaderInfoLog");
        staticToStatic(gl20, "glCreateProgram", "()I", gl, "glCreateProgram");
        staticToStatic(gl20, "glAttachShader", "(II)V", gl, "glAttachShader");
        staticToStatic(gl20, "glLinkProgram", "(I)V", gl, "glLinkProgram");
        staticToStatic(gl20, "glValidateProgram", "(I)V", gl, "glValidateProgram");
        staticToStatic(gl20, "glGetProgram", "(II)I", gl, "glGetProgram");
        staticToStatic(gl20, "glGetProgramInfoLog", "(II)Ljava/lang/String;", gl, "glGetProgramInfoLog");
        staticToStatic(gl20, "glUseProgram", "(I)V", gl, "glUseProgram");
        staticToStatic(gl20, "glDeleteShader", "(I)V", gl, "glDeleteShader");
        staticToStatic(gl20, "glDeleteProgram", "(I)V", gl, "glDeleteProgram");
        staticToStatic(gl20, "glGetUniformLocation", "(ILjava/lang/CharSequence;)I", gl, "glGetUniformLocation");
        staticToStatic(gl20, "glBindAttribLocation", "(IILjava/lang/CharSequence;)V", gl, "glBindAttribLocation");
        String arb = "org/lwjgl/opengl/ARBShaderObjects";
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform1fARB", "(IF)V", gl, "glUniform1fARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform1f", "(IF)V", gl, "glUniform1f");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform1iARB", "(II)V", gl, "glUniform1iARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform1i", "(II)V", gl, "glUniform1i");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform1ARB", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform1ARBvf");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform1", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform1vf");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform1ARB", "(ILjava/nio/IntBuffer;)V", gl, "glUniform1ARBvi");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform1", "(ILjava/nio/IntBuffer;)V", gl, "glUniform1vi");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform2fARB", "(IFF)V", gl, "glUniform2fARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform2f", "(IFF)V", gl, "glUniform2f");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform2iARB", "(III)V", gl, "glUniform2iARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform2i", "(III)V", gl, "glUniform2i");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform2ARB", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform2ARBvf");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform2", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform2vf");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform2ARB", "(ILjava/nio/IntBuffer;)V", gl, "glUniform2ARBvi");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform2", "(ILjava/nio/IntBuffer;)V", gl, "glUniform2vi");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform3fARB", "(IFFF)V", gl, "glUniform3fARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform3f", "(IFFF)V", gl, "glUniform3f");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform3iARB", "(IIII)V", gl, "glUniform3iARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform3i", "(IIII)V", gl, "glUniform3i");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform3ARB", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform3ARBvf");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform3", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform3vf");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform3ARB", "(ILjava/nio/IntBuffer;)V", gl, "glUniform3ARBvi");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform3", "(ILjava/nio/IntBuffer;)V", gl, "glUniform3vi");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform4fARB", "(IFFFF)V", gl, "glUniform4fARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform4f", "(IFFFF)V", gl, "glUniform4f");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform4iARB", "(IIIII)V", gl, "glUniform4iARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform4i", "(IIIII)V", gl, "glUniform4i");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform4ARB", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform4ARBvf");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform4", "(ILjava/nio/FloatBuffer;)V", gl, "glUniform4vf");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniform4ARB", "(ILjava/nio/IntBuffer;)V", gl, "glUniform4ARBvi");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniform4", "(ILjava/nio/IntBuffer;)V", gl, "glUniform4vi");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniformMatrix2ARB", "(IZLjava/nio/FloatBuffer;)V", gl, "glUniformMatrix2ARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniformMatrix2", "(IZLjava/nio/FloatBuffer;)V", gl, "glUniformMatrix2");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniformMatrix3ARB", "(IZLjava/nio/FloatBuffer;)V", gl, "glUniformMatrix3ARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniformMatrix3", "(IZLjava/nio/FloatBuffer;)V", gl, "glUniformMatrix3");
        staticToStatic("org/lwjgl/opengl/ARBShaderObjects", "glUniformMatrix4ARB", "(IZLjava/nio/FloatBuffer;)V", gl, "glUniformMatrix4ARB");
        staticToStatic("org/lwjgl/opengl/GL20", "glUniformMatrix4", "(IZLjava/nio/FloatBuffer;)V", gl, "glUniformMatrix4");
        staticToStatic(arb, "glCreateShaderObjectARB", "(I)I", gl, "glCreateShaderObjectARB");
        staticToStatic(arb, "glShaderSourceARB", "(ILjava/nio/ByteBuffer;)V", gl, "glShaderSourceARB");
        staticToStatic(arb, "glShaderSourceARB", "(ILjava/lang/CharSequence;)V", gl, "glShaderSourceARB");
        staticToStatic(arb, "glCompileShaderARB", "(I)V", gl, "glCompileShaderARB");
        staticToStatic(arb, "glGetObjectParameteriARB", "(II)I", gl, "glGetObjectParameteriARB");
        staticToStatic(arb, "glGetInfoLogARB", "(II)Ljava/lang/String;", gl, "glGetInfoLogARB");
        staticToStatic(arb, "glCreateProgramObjectARB", "()I", gl, "glCreateProgramObjectARB");
        staticToStatic(arb, "glAttachObjectARB", "(II)V", gl, "glAttachObjectARB");
        staticToStatic(arb, "glLinkProgramARB", "(I)V", gl, "glLinkProgramARB");
        staticToStatic(arb, "glValidateProgramARB", "(I)V", gl, "glValidateProgramARB");
        staticToStatic(arb, "glUseProgramObjectARB", "(I)V", gl, "glUseProgramObjectARB");
        staticToStatic(arb, "glDeleteObjectARB", "(I)V", gl, "glDeleteObjectARB");
        staticToStatic(arb, "glGetUniformLocationARB", "(ILjava/lang/CharSequence;)I", gl, "glGetUniformLocationARB");
        String vsh = "org/lwjgl/opengl/ARBVertexShader";
        staticToStatic(gl20, "glDetachShader", "(II)V", gl, "glDetachShader");
        staticToStatic(arb, "glDetachObjectARB", "(II)V", gl, "glDetachObjectARB");
        staticToStatic(gl20, "glEnableVertexAttribArray", "(I)V", gl, "glEnableVertexAttribArray");
        staticToStatic(gl20, "glDisableVertexAttribArray", "(I)V", gl, "glDisableVertexAttribArray");
        staticToStatic(vsh, "glEnableVertexAttribArrayARB", "(I)V", gl, "glEnableVertexAttribArrayARB");
        staticToStatic(vsh, "glDisableVertexAttribArrayARB", "(I)V", gl, "glDisableVertexAttribArrayARB");
        staticToStatic(gl20, "glGetAttribLocation", "(ILjava/lang/CharSequence;)I", gl, "glGetAttribLocation");
        staticToStatic(vsh, "glGetAttribLocationARB", "(ILjava/lang/CharSequence;)I", gl, "glGetAttribLocationARB");
        staticToStatic(gl20, "glVertexAttribPointer", "(IIIZIJ)V", gl, "glVertexAttribPointer");
        staticToStatic(vsh, "glVertexAttribPointerARB", "(IIIZIJ)V", gl, "glVertexAttribPointerARB");
        staticToStatic(vsh, "glVertexAttrib4fARB", "(IFFFF)V", gl, "glVertexAttrib4fARB");
        staticToStatic("org/lwjgl/opengl/GL33", "glVertexAttribDivisor", "(II)V", gl, "glVertexAttribDivisor");
        staticToStatic("org/lwjgl/opengl/ARBInstancedArrays", "glVertexAttribDivisorARB", "(II)V", gl, "glVertexAttribDivisor");
        staticToStatic("org/lwjgl/opengl/GL31", "glDrawArraysInstanced", "(IIII)V", gl, "glDrawArraysInstanced");
        staticToStatic("org/lwjgl/opengl/ARBDrawInstanced", "glDrawArraysInstancedARB", "(IIII)V", gl, "glDrawArraysInstanced");
        staticToStatic("org/lwjgl/opengl/EXTFramebufferObject", "glCheckFramebufferStatusEXT", "(I)I", gl, "glCheckFramebufferStatusEXT");
        staticToStatic("org/lwjgl/opengl/ARBVertexArrayObject", "glGenVertexArrays", "()I", gl, "glGenVertexArraysARB");
        staticToStatic("org/lwjgl/opengl/ARBVertexArrayObject", "glBindVertexArray", "(I)V", gl, "glBindVertexArrayARB");
        staticToStatic("org/lwjgl/opengl/ARBOcclusionQuery", "glGenQueriesARB", "(Ljava/nio/IntBuffer;)V", gl, "glGenQueriesARB");
        staticToStatic("org/lwjgl/opengl/ARBOcclusionQuery", "glDeleteQueriesARB", "(Ljava/nio/IntBuffer;)V", gl, "glDeleteQueriesARB");
        // Guava 17 -> 32: TypeToken.isAssignableFrom was renamed isSupertypeOf.
        rename("com/google/common/reflect/TypeToken", "isAssignableFrom", "(Lcom/google/common/reflect/TypeToken;)Z", "isSupertypeOf");
        rename("com/google/common/reflect/TypeToken", "isAssignableFrom", "(Ljava/lang/reflect/Type;)Z", "isSupertypeOf");
        // authlib 1.5 -> 6: profile completeness and property filling moved.
        virtualToStatic("com/mojang/authlib/GameProfile", "isComplete", "()Z", COMPAT + "LibraryCompat");
        // authlib 6: Property is a record.
        virtualToStatic("com/mojang/authlib/properties/Property", "getName", "()Ljava/lang/String;", COMPAT + "LibraryCompat");
        virtualToStatic("com/mojang/authlib/properties/Property", "getValue", "()Ljava/lang/String;", COMPAT + "LibraryCompat");
        virtualToStatic("com/mojang/authlib/properties/Property", "getSignature", "()Ljava/lang/String;", COMPAT + "LibraryCompat");
        // text2speech: say(String) became say(String, boolean).
        virtualToStatic("com/mojang/text2speech/Narrator", "say", "(Ljava/lang/String;)V", COMPAT + "LibraryCompat");
        virtualToStatic("com/mojang/authlib/minecraft/MinecraftSessionService", "fillProfileProperties",
                "(Lcom/mojang/authlib/GameProfile;Z)Lcom/mojang/authlib/GameProfile;", COMPAT + "LibraryCompat");
    }

    private JavaCompatRedirects() {}

    private static void virtualToStatic(String owner, String name, String desc, String target) {
        String staticDesc = "(L" + owner + ";" + desc.substring(1);
        INVOKES.put(owner + "." + name + desc, new MethodInsnNode(Opcodes.INVOKESTATIC, target, name, staticDesc, false));
    }

    /** Same call shape, new name (virtual stays virtual). */
    private static void rename(String owner, String name, String desc, String newName) {
        INVOKES.put(owner + "." + name + desc, new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, newName, desc, false));
    }

    /** Library classes whose public constructor became a factory method: owner -> factory name. */
    private static final Map<String, String> CONSTRUCTOR_FACTORIES = Map.of(
            "com/google/common/base/Stopwatch", "createUnstarted",
            // Netty 4.0 -> 4.1: new AttributeKey(name) -> AttributeKey.valueOf(name)
            "io/netty/util/AttributeKey", "valueOf");

    private static void staticToStatic(String owner, String name, String desc, String target, String targetName) {
        INVOKES.put(owner + "." + name + desc, new MethodInsnNode(Opcodes.INVOKESTATIC, target, targetName, desc, false));
    }

    /** @return number of rewritten call sites */
    public static int apply(ClassNode node) {
        int count = 0;
        for (MethodNode method : node.methods) {
            InsnList insns = method.instructions;
            for (AbstractInsnNode insn = insns.getFirst(); insn != null; insn = insn.getNext()) {
                if (insn instanceof MethodInsnNode call) {
                    MethodInsnNode target = INVOKES.get(call.owner + "." + call.name + call.desc);
                    // Only virtual call sites are rewritten for virtual methods; super calls stay intact.
                    if (target != null && (call.getOpcode() != Opcodes.INVOKESPECIAL)) {
                        MethodInsnNode replacement = (MethodInsnNode) target.clone(null);
                        insns.set(call, replacement);
                        insn = replacement;
                        count++;
                    } else if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>")
                            && call.owner.equals("com/mojang/authlib/GameProfile") && call.desc.equals("(Ljava/util/UUID;Ljava/lang/String;)V")) {
                        // Old authlib allowed a null id (or name), e.g. mods' fake players "[Railcraft]"; today it throws.
                        // The arguments are fixed in place (NEW/DUP stay, so stack map frames are unaffected):
                        // [id, name] -> DUP2, fixId -> [id, name, id'] -> DUP_X2, POP, SWAP, POP -> [id', name] -> fixName.
                        InsnList fix = new InsnList();
                        fix.add(new InsnNode(Opcodes.DUP2));
                        fix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "GameProfileCompat", "id", "(Ljava/util/UUID;Ljava/lang/String;)Ljava/util/UUID;", false));
                        fix.add(new InsnNode(Opcodes.DUP_X2));
                        fix.add(new InsnNode(Opcodes.POP));
                        fix.add(new InsnNode(Opcodes.SWAP));
                        fix.add(new InsnNode(Opcodes.POP));
                        fix.add(new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "GameProfileCompat", "name", "(Ljava/lang/String;)Ljava/lang/String;", false));
                        insns.insertBefore(call, fix);
                        method.maxStack += 2;
                        count++;
                    } else if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>")
                            && CONSTRUCTOR_FACTORIES.containsKey(call.owner) && !node.name.equals(call.owner)) {
                        // new Stopwatch(...) -> Stopwatch.createUnstarted(...), new AttributeKey(n) -> AttributeKey.valueOf(n)
                        AbstractInsnNode replaced = rewriteConstructor(insns, call, CONSTRUCTOR_FACTORIES.get(call.owner));
                        if (replaced != null) {
                            insn = replaced;
                            count++;
                        }
                    }
                } else if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.CHECKCAST
                        && type.desc.endsWith("/net/minecraft/launchwrapper/LaunchClassLoader") && type.desc.startsWith("legacyconnector/")) {
                    // (LaunchClassLoader) getClass().getClassLoader(): modlauncher's loader is no LaunchClassLoader.
                    MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC, type.desc, "mcc$cast",
                            "(Ljava/lang/Object;)L" + type.desc + ";", false);
                    insns.set(type, replacement);
                    insn = replacement;
                    count++;
                } else if (insn instanceof TypeInsnNode type && type.getOpcode() == Opcodes.CHECKCAST
                        && type.desc.equals("java/net/URLClassLoader")) {
                    // Java 9+: the app/system class loader is no longer a URLClassLoader.
                    MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "JdkCompat", "asURLClassLoader",
                            "(Ljava/lang/Object;)Ljava/net/URLClassLoader;", false);
                    insns.set(type, replacement);
                    insn = replacement;
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Rewrites {@code NEW T; DUP; <args>; INVOKESPECIAL T.<init>(args)V} into {@code <args>; INVOKESTATIC T.factory(args)T}
     * when NEW/DUP directly precede the argument code, which is what javac emits.
     */
    private static AbstractInsnNode rewriteConstructor(InsnList insns, MethodInsnNode init, String factory) {
        AbstractInsnNode cursor = init.getPrevious();
        int depth = 0;
        // Walk back to the matching NEW; skip nested NEW/<init> pairs within the arguments.
        while (cursor != null) {
            if (cursor instanceof MethodInsnNode m && m.getOpcode() == Opcodes.INVOKESPECIAL && m.name.equals("<init>")) depth++;
            if (cursor instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW) {
                if (depth == 0) break;
                depth--;
            }
            cursor = cursor.getPrevious();
        }
        if (!(cursor instanceof TypeInsnNode newInsn) || !newInsn.desc.equals(init.owner)) return null;
        AbstractInsnNode dup = newInsn.getNext();
        if (dup == null || dup.getOpcode() != Opcodes.DUP) return null;
        insns.remove(newInsn);
        insns.remove(dup);
        String desc = init.desc.substring(0, init.desc.length() - 1) + "L" + init.owner + ";";
        MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC, init.owner, factory, desc, false);
        insns.set(init, replacement);
        return replacement;
    }
}
