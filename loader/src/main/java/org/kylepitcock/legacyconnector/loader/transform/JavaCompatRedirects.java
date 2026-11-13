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
        virtualToStatic("java/lang/Class", "getFields", "()[Ljava/lang/reflect/Field;", COMPAT + "ReflectionCompat");
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
        // Network calls without timeouts stall the loading thread as long as a dead host takes to give up (NetCompat).
        virtualToStatic("java/net/URL", "openConnection", "()Ljava/net/URLConnection;", COMPAT + "NetCompat");
        virtualToStatic("java/net/URL", "openStream", "()Ljava/io/InputStream;", COMPAT + "NetCompat");
        // Java 20+: Thread.stop/suspend/resume throw UnsupportedOperationException.
        virtualToStatic("java/lang/Thread", "stop", "()V", COMPAT + "JdkCompat");
        // Java 9+: Arrays.asList(array).toArray() is an Object[] (it was the array's own type on Java 8).
        staticToStatic("java/util/Arrays", "asList", "([Ljava/lang/Object;)Ljava/util/List;", COMPAT + "JdkCompat", "asList");
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
        // log4j's resource loader found a mod's files through the shared 1.7.10 class loader (CoFH copies its world generation
        // files with it); today the context loader does not see the mod jars.
        for (String owner : new String[] {"org/apache/logging/log4j/core/helpers/Loader", "org/apache/logging/log4j/core/util/Loader"}) {
            staticToStatic(owner, "getResource", "(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/net/URL;", COMPAT + "Log4jCompat", "getResource");
        }
        staticToStatic("javax/swing/JOptionPane", "showOptionDialog",
                "(Ljava/awt/Component;Ljava/lang/Object;Ljava/lang/String;IILjavax/swing/Icon;[Ljava/lang/Object;Ljava/lang/Object;)I",
                COMPAT + "DialogCompat", "showOptionDialog");
        // A mod giving up must not end the game: System.exit fails that mod instead.
        staticToStatic("java/lang/System", "exit", "(I)V", COMPAT + "JdkCompat", "exit");
        // Tamper checks (Forestry compares its jar's signature and halts the JVM) would end the game on a translated, unsigned jar.
        virtualToStatic("java/lang/Runtime", "halt", "(I)V", COMPAT + "JdkCompat");
        virtualToStatic("java/lang/Runtime", "exit", "(I)V", COMPAT + "JdkCompat");
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
        // Guava 26 turned the CharMatcher constants (WHITESPACE, DIGIT, ...) into factory methods (see CHAR_MATCHER_CONSTANTS).
        // authlib 6 has no com.mojang.util.UUIDTypeAdapter: its string conversions live in LibraryCompat.
        staticToStatic("com/mojang/util/UUIDTypeAdapter", "fromString", "(Ljava/lang/String;)Ljava/util/UUID;", COMPAT + "LibraryCompat", "uuidFromString");
        staticToStatic("com/mojang/util/UUIDTypeAdapter", "fromUUID", "(Ljava/util/UUID;)Ljava/lang/String;", COMPAT + "LibraryCompat", "uuidToString");
        // Guava 26 removed CheckedFuture: the result becomes a ListenableFuture (its supertype, which is how callers use it).
        INVOKES.put("com/google/common/util/concurrent/Futures.immediateCheckedFuture(Ljava/lang/Object;)Lcom/google/common/util/concurrent/CheckedFuture;",
                new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "GuavaCompat", "immediateCheckedFuture",
                        "(Ljava/lang/Object;)Lcom/google/common/util/concurrent/ListenableFuture;", false));
        // Guava 33 dropped Files.fileTreeTraverser (and TreeTraverser, which the shim carries per version): the same traversal from GuavaCompat.
        for (String[] v : new String[][] {{"v1_12_2", "1122"}, {"v1_7_10", "1710"}}) {
            INVOKES.put("com/google/common/io/Files.fileTreeTraverser()Llegacyconnector/" + v[0] + "/com/google/common/collect/TreeTraverser;",
                    new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "GuavaCompat", "fileTreeTraverser" + v[1],
                            "()Llegacyconnector/" + v[0] + "/com/google/common/collect/TreeTraverser;", false));
        }
        // Guava 26: Futures.transform needs an executor; sameThreadExecutor became newDirectExecutorService.
        staticToStatic("com/google/common/util/concurrent/Futures", "transform",
                "(Lcom/google/common/util/concurrent/ListenableFuture;Lcom/google/common/base/Function;)Lcom/google/common/util/concurrent/ListenableFuture;", COMPAT + "GuavaCompat", "transform");
        staticToStatic("com/google/common/util/concurrent/Futures", "transform",
                "(Lcom/google/common/util/concurrent/ListenableFuture;Lcom/google/common/util/concurrent/AsyncFunction;)Lcom/google/common/util/concurrent/ListenableFuture;", COMPAT + "GuavaCompat", "transformAsync");
        staticToStatic("com/google/common/util/concurrent/MoreExecutors", "sameThreadExecutor",
                "()Lcom/google/common/util/concurrent/ListeningExecutorService;", "com/google/common/util/concurrent/MoreExecutors", "newDirectExecutorService");
        // Guava 26: Futures.addCallback needs an executor.
        staticToStatic("com/google/common/util/concurrent/Futures", "addCallback",
                "(Lcom/google/common/util/concurrent/ListenableFuture;Lcom/google/common/util/concurrent/FutureCallback;)V", COMPAT + "GuavaCompat", "addCallback");
        // fastutil 8: no boxed get(Integer) on int-keyed maps.
        virtualToStatic("it/unimi/dsi/fastutil/ints/Int2ObjectOpenHashMap", "get", "(Ljava/lang/Integer;)Ljava/lang/Object;", COMPAT + "LibraryCompat");
        virtualToStatic("it/unimi/dsi/fastutil/bytes/ByteArrayList", "rem", "(Ljava/lang/Object;)Z", COMPAT + "LibraryCompat");
        // authlib 6: getTextures(profile) returns a record; the old (profile, requireSecure) form returned a map by type.
        virtualToStatic("com/mojang/authlib/minecraft/MinecraftSessionService", "getTextures",
                "(Lcom/mojang/authlib/GameProfile;Z)Ljava/util/Map;", COMPAT + "LibraryCompat");
        // jopt-simple 5: acceptsAll takes a List.
        virtualToStatic("joptsimple/OptionParser", "acceptsAll", "(Ljava/util/Collection;Ljava/lang/String;)Ljoptsimple/OptionSpecBuilder;", COMPAT + "LibraryCompat");
        // jopt-simple 5: internal.Strings.join takes an Iterable (4: a List).
        staticToStatic("joptsimple/internal/Strings", "join", "(Ljava/util/List;Ljava/lang/String;)Ljava/lang/String;", COMPAT + "LibraryCompat", "joinStrings");
        // Netty 4.1: ChannelPromiseAggregator.add(ChannelPromise...) is inherited from PromiseAggregator.add(Promise...) and returns that type.
        virtualToStatic("io/netty/channel/ChannelPromiseAggregator", "add", "([Lio/netty/channel/ChannelPromise;)Lio/netty/channel/ChannelPromiseAggregator;", COMPAT + "LibraryCompat");
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
        staticToStatic(gl20, "glGetActiveUniform", "(III)Ljava/lang/String;", gl, "glGetActiveUniform");
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
        // LWJGL 2 reads of shader state and the float-buffer attribute pointer (virtual shaders: see GlCompat); glClearBuffer is the real glClearBufferfv
        staticToStatic(gl20, "glGetActiveUniformSize", "(II)I", gl, "glGetActiveUniformSize");
        staticToStatic(gl20, "glGetActiveUniformType", "(II)I", gl, "glGetActiveUniformType");
        staticToStatic(gl20, "glGetUniform", "(IILjava/nio/FloatBuffer;)V", gl, "glGetUniformf");
        staticToStatic(gl20, "glGetUniform", "(IILjava/nio/IntBuffer;)V", gl, "glGetUniformi");
        staticToStatic(gl20, "glGetVertexAttrib", "(IILjava/nio/FloatBuffer;)V", gl, "glGetVertexAttribf");
        staticToStatic(gl20, "glGetVertexAttrib", "(IILjava/nio/IntBuffer;)V", gl, "glGetVertexAttribi");
        staticToStatic(gl20, "glVertexAttribPointer", "(IIZILjava/nio/FloatBuffer;)V", gl, "glVertexAttribPointerBuffer");
        INVOKES.put("org/lwjgl/opengl/GL30.glClearBuffer(IILjava/nio/FloatBuffer;)V",
                new MethodInsnNode(Opcodes.INVOKESTATIC, "org/lwjgl/opengl/GL30", "glClearBufferfv", "(IILjava/nio/FloatBuffer;)V", false));
        // OpenAL: LWJGL 2's typed-buffer overloads got explicit suffixes in LWJGL 3 (alListener(int, FloatBuffer) -> alListenerfv)
        String al = "org/lwjgl/openal/AL10";
        staticToStatic(al, "alGetListener", "(ILjava/nio/FloatBuffer;)V", al, "alGetListenerfv");
        staticToStatic(al, "alListener", "(ILjava/nio/FloatBuffer;)V", al, "alListenerfv");
        staticToStatic(al, "alSource", "(IILjava/nio/FloatBuffer;)V", al, "alSourcefv");
        INVOKES.put(al + ".alIsExtensionPresent(Ljava/lang/String;)Z",
                new MethodInsnNode(Opcodes.INVOKESTATIC, al, "alIsExtensionPresent", "(Ljava/lang/CharSequence;)Z", false));
        // javac widens a byte or short argument to int; Kotlin and some bytecode generators name the narrow overload, which does not exist
        INVOKES.put("java/lang/StringBuilder.append(B)Ljava/lang/StringBuilder;",
                new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false));
        INVOKES.put("java/lang/StringBuilder.append(S)Ljava/lang/StringBuilder;",
                new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(I)Ljava/lang/StringBuilder;", false));
        // fastutil 8: ObjectCollection.rem(Object) is gone (remove(Object) has the same meaning on an object list)
        rename("it/unimi/dsi/fastutil/objects/ObjectArrayList", "rem", "(Ljava/lang/Object;)Z", "remove");
        // authlib 6: joinServer takes the profile id, hasJoinedServer the profile name (both threw the same AuthenticationException)
        virtualToStatic("com/mojang/authlib/minecraft/MinecraftSessionService", "joinServer",
                "(Lcom/mojang/authlib/GameProfile;Ljava/lang/String;Ljava/lang/String;)V", COMPAT + "LibraryCompat");
        INVOKES.put("com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService.joinServer(Lcom/mojang/authlib/GameProfile;Ljava/lang/String;Ljava/lang/String;)V",
                new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "LibraryCompat", "joinServer",
                        "(Lcom/mojang/authlib/minecraft/MinecraftSessionService;Lcom/mojang/authlib/GameProfile;Ljava/lang/String;Ljava/lang/String;)V", false));
        INVOKES.put("com/mojang/authlib/yggdrasil/YggdrasilMinecraftSessionService.hasJoinedServer(Lcom/mojang/authlib/GameProfile;Ljava/lang/String;Ljava/net/InetAddress;)Lcom/mojang/authlib/GameProfile;",
                new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "LibraryCompat", "hasJoinedServer",
                        "(Lcom/mojang/authlib/minecraft/MinecraftSessionService;Lcom/mojang/authlib/GameProfile;Ljava/lang/String;Ljava/net/InetAddress;)Lcom/mojang/authlib/GameProfile;", false));
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
        // authlib 6 dropped Agent (the "minecraft" agent argument), the (Proxy, clientToken) service constructor and the old
        // lookup callback's onProfileLookupFailed(GameProfile, Exception); iChunUtil's profile lookups (Morph, ...) use all three.
        INVOKES.put("com/mojang/authlib/GameProfileRepository.findProfilesByNames([Ljava/lang/String;Lcom/mojang/authlib/Agent;Lcom/mojang/authlib/ProfileLookupCallback;)V",
                new MethodInsnNode(Opcodes.INVOKESTATIC, COMPAT + "LibraryCompat", "findProfilesByNames",
                        "(Lcom/mojang/authlib/GameProfileRepository;[Ljava/lang/String;Ljava/lang/Object;Lcom/mojang/authlib/ProfileLookupCallback;)V", false));
        virtualToStatic("com/mojang/authlib/minecraft/MinecraftSessionService", "fillProfileProperties",
                "(Lcom/mojang/authlib/GameProfile;Z)Lcom/mojang/authlib/GameProfile;", COMPAT + "LibraryCompat");
    }

    private JavaCompatRedirects() {}

    /** Guava 17's {@code CharMatcher} constants and the factory methods that replaced them. */
    private static final Map<String, String> CHAR_MATCHER_CONSTANTS = Map.ofEntries(
            Map.entry("WHITESPACE", "whitespace"), Map.entry("BREAKING_WHITESPACE", "breakingWhitespace"), Map.entry("ASCII", "ascii"),
            Map.entry("DIGIT", "digit"), Map.entry("JAVA_DIGIT", "javaDigit"), Map.entry("JAVA_LETTER", "javaLetter"),
            Map.entry("JAVA_LETTER_OR_DIGIT", "javaLetterOrDigit"), Map.entry("JAVA_UPPER_CASE", "javaUpperCase"),
            Map.entry("JAVA_LOWER_CASE", "javaLowerCase"), Map.entry("JAVA_ISO_CONTROL", "javaIsoControl"), Map.entry("INVISIBLE", "invisible"),
            Map.entry("SINGLE_WIDTH", "singleWidth"), Map.entry("ANY", "any"), Map.entry("NONE", "none"));

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
                if (insn instanceof MethodInsnNode call && typedToArray(method, call)) {
                    count++;
                    continue;
                }
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
                            && call.owner.equals("com/mojang/authlib/yggdrasil/YggdrasilAuthenticationService")
                            && call.desc.equals("(Ljava/net/Proxy;Ljava/lang/String;)V")) {
                        // new YggdrasilAuthenticationService(proxy, clientToken) -> new YggdrasilAuthenticationService(proxy)
                        AbstractInsnNode replaced = rewriteConstructor(insns, call, "yggdrasilService", COMPAT + "LibraryCompat");
                        if (replaced != null) {
                            insn = replaced;
                            count++;
                        }
                    } else if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>")
                            && call.owner.equals("org/objectweb/asm/commons/MethodRemapper") && !node.name.equals(call.owner)
                            && (call.desc.equals("(ILjava/lang/String;Lorg/objectweb/asm/MethodVisitor;Lorg/objectweb/asm/commons/Remapper;)V")
                            || call.desc.equals("(IILjava/lang/String;Lorg/objectweb/asm/MethodVisitor;Lorg/objectweb/asm/commons/Remapper;)V"))) {
                        // ASM 5's RemappingMethodAdapter(access, desc, mv, remapper) became MethodRemapper(mv, remapper)
                        AbstractInsnNode replaced = rewriteConstructor(insns, call, "methodRemapper", COMPAT + "LibraryCompat");
                        if (replaced != null) {
                            insn = replaced;
                            count++;
                        }
                    } else if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>")
                            && CONSTRUCTOR_FACTORIES.containsKey(call.owner) && !node.name.equals(call.owner)) {
                        // new Stopwatch(...) -> Stopwatch.createUnstarted(...), new AttributeKey(n) -> AttributeKey.valueOf(n)
                        AbstractInsnNode replaced = rewriteConstructor(insns, call, CONSTRUCTOR_FACTORIES.get(call.owner));
                        if (replaced != null) {
                            insn = replaced;
                            count++;
                        }
                    }
                } else if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                        && field.owner.equals("com/mojang/authlib/Agent")) {
                    // authlib 6 has no Agent; the value is only passed to the (redirected) lookup calls, which ignore it.
                    InsnNode replacement = new InsnNode(Opcodes.ACONST_NULL);
                    insns.set(field, replacement);
                    insn = replacement;
                    count++;
                } else if (insn instanceof org.objectweb.asm.tree.FieldInsnNode field && field.getOpcode() == Opcodes.GETSTATIC
                        && field.owner.equals("com/google/common/base/CharMatcher") && CHAR_MATCHER_CONSTANTS.containsKey(field.name)) {
                    // CharMatcher.WHITESPACE -> CharMatcher.whitespace()
                    MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC, field.owner, CHAR_MATCHER_CONSTANTS.get(field.name),
                            "()Lcom/google/common/base/CharMatcher;", false);
                    insns.set(field, replacement);
                    insn = replacement;
                    count++;
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
        count += addFilterLifeCycle(node);
        return count;
    }

    /**
     * Up to Java 8, {@code Arrays.asList(...).toArray()} returned the backing array with its own type, and code cast it straight to
     * that type ({@code (Integer[]) list.toArray()}: RedPower's power search). Since Java 9 it is always an {@code Object[]} and the cast
     * fails. A {@code java.util} collection's {@code toArray()} followed by a cast to a typed array becomes {@code toArray(new T[0])},
     * which gives that type for every collection. The added array argument needs one more operand stack slot (the classes are written
     * without recomputing maxs: a getter {@code return (T[]) list.toArray();} with max stack 1 failed verification, #244).
     */
    private static boolean typedToArray(MethodNode method, MethodInsnNode call) {
        InsnList insns = method.instructions;
        if (!call.name.equals("toArray") || !call.desc.equals("()[Ljava/lang/Object;") || !call.owner.startsWith("java/util/")) return false;
        AbstractInsnNode next = call.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        if (!(next instanceof org.objectweb.asm.tree.TypeInsnNode cast) || cast.getOpcode() != Opcodes.CHECKCAST) return false;
        if (!cast.desc.startsWith("[") || cast.desc.equals("[Ljava/lang/Object;") || cast.desc.length() < 3) return false;
        String element = cast.desc.substring(1);
        if (element.startsWith("L")) element = element.substring(1, element.length() - 1);
        else if (!element.startsWith("[")) return false; // primitive arrays are not what toArray returns
        InsnList fix = new InsnList();
        fix.add(new InsnNode(Opcodes.ICONST_0));
        fix.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.ANEWARRAY, element));
        insns.insertBefore(call, fix);
        call.desc = "([Ljava/lang/Object;)[Ljava/lang/Object;";
        method.maxStack += 1;
        return true;
    }

    private static final String LOG4J_FILTER = "org/apache/logging/log4j/core/Filter";

    /**
     * log4j 2.0-beta9's {@code Filter} had no life cycle; later versions extend {@code LifeCycle}, and log4j calls {@code stop()} on every
     * filter when the game shuts down (DragonAPI's ItemBlockMismatchFilter: an AbstractMethodError in the shutdown hook, printed by every
     * server stop). A class that implements Filter itself gets the five methods, as an always-started filter.
     */
    private static int addFilterLifeCycle(ClassNode node) {
        if ((node.access & Opcodes.ACC_INTERFACE) != 0 || node.interfaces == null || !node.interfaces.contains(LOG4J_FILTER)) return 0;
        for (MethodNode m : node.methods) if (m.name.equals("stop") && m.desc.equals("()V")) return 0;
        for (String[] def : new String[][] {{"initialize", "()V"}, {"start", "()V"}, {"stop", "()V"}}) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, def[0], def[1], null, null);
            m.instructions.add(new InsnNode(Opcodes.RETURN));
            m.maxStack = 0;
            m.maxLocals = 1;
            node.methods.add(m);
        }
        for (Object[] def : new Object[][] {{"isStarted", Opcodes.ICONST_1}, {"isStopped", Opcodes.ICONST_0}}) {
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, (String) def[0], "()Z", null, null);
            m.instructions.add(new InsnNode((Integer) def[1]));
            m.instructions.add(new InsnNode(Opcodes.IRETURN));
            m.maxStack = 1;
            m.maxLocals = 1;
            node.methods.add(m);
        }
        MethodNode state = new MethodNode(Opcodes.ACC_PUBLIC, "getState", "()Lorg/apache/logging/log4j/core/LifeCycle$State;", null, null);
        state.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(Opcodes.GETSTATIC, "org/apache/logging/log4j/core/LifeCycle$State", "STARTED",
                "Lorg/apache/logging/log4j/core/LifeCycle$State;"));
        state.instructions.add(new InsnNode(Opcodes.ARETURN));
        state.maxStack = 1;
        state.maxLocals = 1;
        node.methods.add(state);
        return 6;
    }

    /**
     * Stack map frames inside the arguments of a {@code new T(...)} (a conditional argument) hold the uninitialized object as a verification
     * type naming the NEW instruction; once the NEW is replaced by a factory call that entry is gone (the JVM rejects the class with "bad offset
     * for Uninitialized"), so the entries are taken out of the frames. Both slots (NEW and DUP) go.
     */
    private static void dropUninitializedFrames(InsnList insns, TypeInsnNode newInsn) {
        java.util.Set<Object> labels = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (AbstractInsnNode p = newInsn.getPrevious(); p != null && p.getOpcode() == -1; p = p.getPrevious()) {
            if (p instanceof org.objectweb.asm.tree.LabelNode l) labels.add(l);
        }
        if (labels.isEmpty()) return;
        for (AbstractInsnNode n = insns.getFirst(); n != null; n = n.getNext()) {
            if (!(n instanceof org.objectweb.asm.tree.FrameNode f)) continue;
            if (f.stack != null) f.stack.removeIf(labels::contains);
            if (f.local != null && f.type == Opcodes.F_FULL) f.local.removeIf(labels::contains);
            if (f.type == Opcodes.F_SAME1 && f.stack != null && f.stack.isEmpty()) f.type = Opcodes.F_SAME;
        }
    }

    /**
     * Rewrites {@code NEW T; DUP; <args>; INVOKESPECIAL T.<init>(args)V} into {@code <args>; INVOKESTATIC T.factory(args)T}
     * when NEW/DUP directly precede the argument code, which is what javac emits.
     */
    private static AbstractInsnNode rewriteConstructor(InsnList insns, MethodInsnNode init, String factory) {
        return rewriteConstructor(insns, init, factory, init.owner);
    }

    /** As above, with the factory method on another class (its static descriptor is the constructor's, returning the constructed type). */
    private static AbstractInsnNode rewriteConstructor(InsnList insns, MethodInsnNode init, String factory, String factoryOwner) {
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
        dropUninitializedFrames(insns, newInsn);
        insns.remove(newInsn);
        insns.remove(dup);
        String desc = init.desc.substring(0, init.desc.length() - 1) + "L" + init.owner + ";";
        MethodInsnNode replacement = new MethodInsnNode(Opcodes.INVOKESTATIC, factoryOwner, factory, desc, false);
        insns.set(init, replacement);
        return replacement;
    }
}
