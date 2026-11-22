package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;

/** The Java 8 to 21 call redirects: each rewrite keeps the operand stack shape, which is what these tests pin down. */
class JavaCompatRedirectsTest implements Opcodes {
    private static final String COMPAT = "org/kylepitcock/legacyconnector/compat/jdk/";

    private static ClassNode node(String name) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = "java/lang/Object";
        return n;
    }

    private static MethodNode method(ClassNode n, String name, String desc) {
        MethodNode m = new MethodNode(ACC_PUBLIC | ACC_STATIC, name, desc, null, null);
        m.maxStack = 4;
        m.maxLocals = 4;
        n.methods.add(m);
        return m;
    }

    /** Opcode and member of every real instruction, e.g. {@code 184 a/B.c(D)}. */
    private static List<String> insns(MethodNode m) {
        List<String> out = new ArrayList<>();
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) {
            if (i.getOpcode() < 0) continue;
            String op = Integer.toString(i.getOpcode());
            if (i instanceof MethodInsnNode c) op += " " + c.owner + "." + c.name + c.desc;
            else if (i instanceof FieldInsnNode f) op += " " + f.owner + "." + f.name;
            else if (i instanceof TypeInsnNode t) op += " " + t.desc;
            out.add(op);
        }
        return out;
    }

    private static String op(int opcode, String rest) {
        return opcode + (rest.isEmpty() ? "" : " " + rest);
    }

    @Test
    void classGetDeclaredFieldBecomesStaticWithReceiverFirst() {
        ClassNode n = node("t/A");
        MethodNode m = method(n, "f", "(Ljava/lang/Class;)Ljava/lang/reflect/Field;");
        m.visitVarInsn(ALOAD, 0);
        m.visitLdcInsn("modifiers");
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertTrue(insns(m).contains(op(INVOKESTATIC, COMPAT + "ReflectionCompat.getDeclaredField(Ljava/lang/Class;Ljava/lang/String;)Ljava/lang/reflect/Field;")),
                insns(m).toString());
    }

    @Test
    void superCallsToARedirectedMethodAreLeftAlone() {
        ClassNode n = node("t/B");
        MethodNode m = method(n, "f", "(Ljava/lang/StringBuilder;)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitInsn(ICONST_1);
        m.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "append", "(B)Ljava/lang/StringBuilder;", false);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        assertEquals(0, JavaCompatRedirects.apply(n));
    }

    @Test
    void narrowAppendOverloadWidensToInt() {
        ClassNode n = node("t/C");
        MethodNode m = method(n, "f", "(Ljava/lang/StringBuilder;)V");
        m.visitVarInsn(ALOAD, 0);
        m.visitInsn(ICONST_1);
        m.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(B)Ljava/lang/StringBuilder;", false);
        m.visitInsn(POP);
        m.visitInsn(RETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertTrue(insns(m).contains(op(INVOKEVIRTUAL, "java/lang/StringBuilder.append(I)Ljava/lang/StringBuilder;")), insns(m).toString());
    }

    @Test
    void stopwatchConstructorBecomesFactoryAndDropsNewDup() {
        ClassNode n = node("t/D");
        MethodNode m = method(n, "f", "()Ljava/lang/Object;");
        m.visitTypeInsn(NEW, "com/google/common/base/Stopwatch");
        m.visitInsn(DUP);
        m.visitMethodInsn(INVOKESPECIAL, "com/google/common/base/Stopwatch", "<init>", "()V", false);
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertEquals(List.of(op(INVOKESTATIC, "com/google/common/base/Stopwatch.createUnstarted()Lcom/google/common/base/Stopwatch;"), op(ARETURN, "")),
                insns(m));
    }

    @Test
    void factoryRewriteKeepsArgumentsAndSkipsNestedConstructors() {
        ClassNode n = node("t/E");
        MethodNode m = method(n, "f", "()Ljava/lang/Object;");
        // new AttributeKey(new String("x")): the nested NEW/<init> pair belongs to the argument
        m.visitTypeInsn(NEW, "io/netty/util/AttributeKey");
        m.visitInsn(DUP);
        m.visitTypeInsn(NEW, "java/lang/String");
        m.visitInsn(DUP);
        m.visitLdcInsn("x");
        m.visitMethodInsn(INVOKESPECIAL, "java/lang/String", "<init>", "(Ljava/lang/String;)V", false);
        m.visitMethodInsn(INVOKESPECIAL, "io/netty/util/AttributeKey", "<init>", "(Ljava/lang/String;)V", false);
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        List<String> expected = List.of(op(NEW, "java/lang/String"), op(DUP, ""), op(LDC, ""),
                op(INVOKESPECIAL, "java/lang/String.<init>(Ljava/lang/String;)V"),
                op(INVOKESTATIC, "io/netty/util/AttributeKey.valueOf(Ljava/lang/String;)Lio/netty/util/AttributeKey;"), op(ARETURN, ""));
        assertEquals(expected, insns(m));
    }

    @Test
    void aClassMayConstructItsOwnFactoryTarget() {
        ClassNode n = node("com/google/common/base/Stopwatch");
        MethodNode m = method(n, "f", "()Ljava/lang/Object;");
        m.visitTypeInsn(NEW, "com/google/common/base/Stopwatch");
        m.visitInsn(DUP);
        m.visitMethodInsn(INVOKESPECIAL, "com/google/common/base/Stopwatch", "<init>", "()V", false);
        m.visitInsn(ARETURN);
        assertEquals(0, JavaCompatRedirects.apply(n));
    }

    @Test
    void charMatcherConstantsBecomeFactoryCalls() {
        ClassNode n = node("t/F");
        MethodNode m = method(n, "f", "()Ljava/lang/Object;");
        m.visitFieldInsn(GETSTATIC, "com/google/common/base/CharMatcher", "WHITESPACE", "Lcom/google/common/base/CharMatcher;");
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertEquals(List.of(op(INVOKESTATIC, "com/google/common/base/CharMatcher.whitespace()Lcom/google/common/base/CharMatcher;"), op(ARETURN, "")),
                insns(m));
    }

    @Test
    void authlibAgentBecomesNull() {
        ClassNode n = node("t/G");
        MethodNode m = method(n, "f", "()Ljava/lang/Object;");
        m.visitFieldInsn(GETSTATIC, "com/mojang/authlib/Agent", "MINECRAFT", "Lcom/mojang/authlib/Agent;");
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertEquals(List.of(op(ACONST_NULL, ""), op(ARETURN, "")), insns(m));
    }

    @Test
    void urlClassLoaderCastBecomesHelper() {
        ClassNode n = node("t/H");
        MethodNode m = method(n, "f", "(Ljava/lang/Object;)Ljava/lang/Object;");
        m.visitVarInsn(ALOAD, 0);
        m.visitTypeInsn(CHECKCAST, "java/net/URLClassLoader");
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertTrue(insns(m).contains(op(INVOKESTATIC, COMPAT + "JdkCompat.asURLClassLoader(Ljava/lang/Object;)Ljava/net/URLClassLoader;")));
        assertFalse(insns(m).stream().anyMatch(s -> s.startsWith(CHECKCAST + " ")));
    }

    @Test
    void launchClassLoaderCastOfTheShimBecomesCastHelper() {
        ClassNode n = node("t/I");
        MethodNode m = method(n, "f", "(Ljava/lang/Object;)Ljava/lang/Object;");
        String lcl = "legacyconnector/v1_7_10/net/minecraft/launchwrapper/LaunchClassLoader";
        m.visitVarInsn(ALOAD, 0);
        m.visitTypeInsn(CHECKCAST, lcl);
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertTrue(insns(m).contains(op(INVOKESTATIC, lcl + ".mcc$cast(Ljava/lang/Object;)L" + lcl + ";")), insns(m).toString());
    }

    @Test
    void toArrayThenTypedCastPassesATypedArrayAndGrowsTheStack() {
        ClassNode n = node("t/J");
        MethodNode m = method(n, "f", "(Ljava/util/List;)[Ljava/lang/Integer;");
        m.maxStack = 1;
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;", true);
        m.visitTypeInsn(CHECKCAST, "[Ljava/lang/Integer;");
        m.visitInsn(ARETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertEquals(List.of(op(ALOAD, ""), op(ICONST_0, ""), op(ANEWARRAY, "java/lang/Integer"),
                op(INVOKEINTERFACE, "java/util/List.toArray([Ljava/lang/Object;)[Ljava/lang/Object;"), op(CHECKCAST, "[Ljava/lang/Integer;"), op(ARETURN, "")),
                insns(m));
        assertEquals(2, m.maxStack);
    }

    @Test
    void toArrayCastToObjectArrayOrPrimitiveArrayIsUntouched() {
        ClassNode n = node("t/K");
        MethodNode m = method(n, "f", "(Ljava/util/List;)Ljava/lang/Object;");
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;", true);
        m.visitTypeInsn(CHECKCAST, "[Ljava/lang/Object;");
        m.visitInsn(ARETURN);
        MethodNode m2 = method(n, "g", "(Ljava/util/List;)Ljava/lang/Object;");
        m2.visitVarInsn(ALOAD, 0);
        m2.visitMethodInsn(INVOKEINTERFACE, "java/util/List", "toArray", "()[Ljava/lang/Object;", true);
        m2.visitTypeInsn(CHECKCAST, "[I");
        m2.visitInsn(ARETURN);
        assertEquals(0, JavaCompatRedirects.apply(n));
    }

    @Test
    void toArrayOfANonJavaUtilClassIsUntouched() {
        ClassNode n = node("t/L");
        MethodNode m = method(n, "f", "(Lx/Mine;)Ljava/lang/Object;");
        m.visitVarInsn(ALOAD, 0);
        m.visitMethodInsn(INVOKEVIRTUAL, "x/Mine", "toArray", "()[Ljava/lang/Object;", false);
        m.visitTypeInsn(CHECKCAST, "[Ljava/lang/String;");
        m.visitInsn(ARETURN);
        assertEquals(0, JavaCompatRedirects.apply(n));
    }

    @Test
    void gameProfileConstructorArgumentsAreFixedInPlace() {
        ClassNode n = node("t/M");
        MethodNode m = method(n, "f", "(Ljava/util/UUID;Ljava/lang/String;)Ljava/lang/Object;");
        m.visitTypeInsn(NEW, "com/mojang/authlib/GameProfile");
        m.visitInsn(DUP);
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ALOAD, 1);
        m.visitMethodInsn(INVOKESPECIAL, "com/mojang/authlib/GameProfile", "<init>", "(Ljava/util/UUID;Ljava/lang/String;)V", false);
        m.visitInsn(ARETURN);
        int before = m.maxStack;
        assertEquals(1, JavaCompatRedirects.apply(n));
        assertEquals(List.of(op(NEW, "com/mojang/authlib/GameProfile"), op(DUP, ""), op(ALOAD, ""), op(ALOAD, ""), op(DUP2, ""),
                op(INVOKESTATIC, COMPAT + "GameProfileCompat.id(Ljava/util/UUID;Ljava/lang/String;)Ljava/util/UUID;"), op(DUP_X2, ""), op(POP, ""), op(SWAP, ""),
                op(POP, ""), op(INVOKESTATIC, COMPAT + "GameProfileCompat.name(Ljava/lang/String;)Ljava/lang/String;"),
                op(INVOKESPECIAL, "com/mojang/authlib/GameProfile.<init>(Ljava/util/UUID;Ljava/lang/String;)V"), op(ARETURN, "")), insns(m));
        assertEquals(before + 2, m.maxStack);
    }

    @Test
    void asm5RemappingMethodAdapterSuperCallDropsLeadingArguments() {
        ClassNode n = node("t/N");
        n.superName = "org/objectweb/asm/commons/MethodRemapper";
        MethodNode m = new MethodNode(ACC_PUBLIC, "<init>", "(ILjava/lang/String;Lorg/objectweb/asm/MethodVisitor;Lorg/objectweb/asm/commons/Remapper;)V", null, null);
        m.maxStack = 5;
        m.maxLocals = 5;
        n.methods.add(m);
        m.visitVarInsn(ALOAD, 0);
        m.visitVarInsn(ILOAD, 1);
        m.visitVarInsn(ALOAD, 2);
        m.visitVarInsn(ALOAD, 3);
        m.visitVarInsn(ALOAD, 4);
        m.visitMethodInsn(INVOKESPECIAL, "org/objectweb/asm/commons/MethodRemapper", "<init>",
                "(ILjava/lang/String;Lorg/objectweb/asm/MethodVisitor;Lorg/objectweb/asm/commons/Remapper;)V", false);
        m.visitInsn(RETURN);
        assertEquals(1, JavaCompatRedirects.apply(n));
        MethodInsnNode call = null;
        for (AbstractInsnNode i = m.instructions.getFirst(); i != null; i = i.getNext()) if (i instanceof MethodInsnNode c) call = c;
        assertNotNull(call);
        assertEquals("(Lorg/objectweb/asm/MethodVisitor;Lorg/objectweb/asm/commons/Remapper;)V", call.desc);
        assertEquals(7, m.maxLocals, "two temporaries past the old locals");
    }

    @Test
    void filterImplementationsGetLifeCycleMethodsOnce() {
        ClassNode n = node("t/Filter");
        n.interfaces.add("org/apache/logging/log4j/core/Filter");
        assertEquals(6, JavaCompatRedirects.apply(n));
        assertTrue(n.methods.stream().anyMatch(x -> x.name.equals("getState")));
        assertTrue(n.methods.stream().anyMatch(x -> x.name.equals("isStarted")));
        int methods = n.methods.size();
        assertEquals(0, JavaCompatRedirects.apply(n), "a class that already has stop() is not extended again");
        assertEquals(methods, n.methods.size());
    }

    @Test
    void filterInterfaceItselfIsNotExtended() {
        ClassNode n = node("t/FilterIface");
        n.access |= ACC_INTERFACE | ACC_ABSTRACT;
        n.interfaces.add("org/apache/logging/log4j/core/Filter");
        assertEquals(0, JavaCompatRedirects.apply(n));
    }

    @Test
    void unrelatedClassIsLeftAlone() {
        ClassNode n = node("t/Plain");
        MethodNode m = method(n, "f", "()V");
        m.visitInsn(RETURN);
        assertEquals(0, JavaCompatRedirects.apply(n));
        assertEquals(List.of(op(RETURN, "")), insns(m));
    }
}
