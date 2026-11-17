package org.kylepitcock.legacyconnector.loader.synth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.kylepitcock.legacyconnector.loader.shim.ClassInfo;
import org.kylepitcock.legacyconnector.loader.shim.ShimIndex;
import org.kylepitcock.legacyconnector.loader.transform.Hierarchy;
import org.kylepitcock.legacyconnector.loader.transform.LinkReport;
import org.kylepitcock.legacyconnector.loader.transform.ShimLinker;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/**
 * Whether a missing legacy type is synthesized as an interface or as a class is decided from how the whole mod set uses it: implemented by a
 * class, called through {@code invokeinterface}, produced by a lambda or method reference, all mean interface; only extending (or plain
 * use) means a class. A class that implements a type the placeholder made a class fails at load (IncompatibleClassChangeError).
 */
class SynthInterfaceDecisionTest implements Opcodes {
    private static final LegacyVersion V = LegacyVersion.V1_12_2;
    private static final String P = V.synthPrefix();
    private static final String IMPLEMENTED = P + "org/lwjgl/util/glu/Callback";
    private static final String CALLED = P + "net/legacy/ICalled";
    private static final String LAMBDA = P + "net/legacy/IFunctional";
    private static final String EXTENDED = P + "net/legacy/Base";

    private static ClassNode node(String name, String sup) {
        ClassNode n = new ClassNode();
        n.version = V1_8;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = sup;
        return n;
    }

    @Test
    void usageAcrossTheModSetDecidesInterfaceOrClass() {
        Map<String, ClassInfo> shim = new HashMap<>();
        shim.put("java/lang/Object", new ClassInfo("java/lang/Object", null, List.of(), ACC_PUBLIC));
        Hierarchy h = new Hierarchy(new ShimIndex(shim), null, Map.of(), V);
        SynthDemand demand = new SynthDemand();

        ClassNode implementer = node("mod/CallbackImpl", "java/lang/Object");
        implementer.interfaces.add(IMPLEMENTED);

        ClassNode caller = node("mod/Caller", "java/lang/Object");
        MethodNode m = new MethodNode(ACC_PUBLIC, "run", "(L" + CALLED + ";)V", null, null);
        caller.methods.add(m);
        m.visitVarInsn(ALOAD, 1);
        m.visitMethodInsn(INVOKEINTERFACE, CALLED, "go", "()V", true);
        Handle metafactory = new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;"
                        + "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;", false);
        m.visitInvokeDynamicInsn("apply", "()L" + LAMBDA + ";", metafactory, Type.getMethodType("()V"),
                new Handle(H_INVOKESTATIC, "mod/Caller", "lambda$run$0", "()V", false), Type.getMethodType("()V"));
        m.visitInsn(POP);
        m.visitInsn(RETURN);

        ClassNode extender = node("mod/Sub", EXTENDED);

        for (ClassNode n : List.of(implementer, caller, extender)) {
            h.addLocal(ClassInfo.of(n));
            ShimLinker.apply(n, h, new LinkReport(), demand);
        }
        assertTrue(demand.interfaces.contains(IMPLEMENTED), "implemented");
        assertTrue(demand.interfaces.contains(CALLED), "invokeinterface");
        assertTrue(demand.interfaces.contains(LAMBDA), "lambda target");
        assertTrue(!demand.interfaces.contains(EXTENDED), "extended is a class");

        demand.classes.addAll(List.of(IMPLEMENTED, CALLED, LAMBDA, EXTENDED));
        Map<String, byte[]> out = new SynthJarBuilder(V, new ShimIndex(shim), null, demand).build();
        for (String itf : List.of(IMPLEMENTED, CALLED, LAMBDA)) {
            assertTrue((access(out.get(itf)) & ACC_INTERFACE) != 0, itf + " is an interface");
        }
        assertEquals(0, access(out.get(EXTENDED)) & ACC_INTERFACE, "extended placeholder is a class");
        assertEquals(0, access(out.get(EXTENDED)) & ACC_FINAL, "and extendable");
    }

    private static int access(byte[] b) {
        return new ClassReader(b).getAccess();
    }
}
