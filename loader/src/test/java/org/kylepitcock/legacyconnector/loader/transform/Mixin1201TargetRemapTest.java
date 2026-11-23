package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.transform.mixin1201.TargetRemap;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * {@code mixin-targets.tsv} rows (forms of {@link TargetRemap}): parameters reordered or removed, a parameter derived from a new one by a helper, a packed
 * value split into old floats, and a return type that became void (the handler's CallbackInfoReturnable is rebuilt). Each on a synthetic mixin run through
 * the whole proof against the real 1.21.1 member index.
 */
class Mixin1201TargetRemapTest implements Opcodes {
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String CI = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String CIR = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";
    private static final String TRACKER = "Lnet/minecraft/client/DeltaTracker;";
    private static final String POSE = "Lcom/mojang/blaze3d/vertex/PoseStack;";
    private static final String VC = "Lcom/mojang/blaze3d/vertex/VertexConsumer;";
    private static final String M4 = "Lorg/joml/Matrix4f;";

    private static ClassNode mixin(String name, String target) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = "java/lang/Object";
        AnnotationNode a = new AnnotationNode("Lorg/spongepowered/asm/mixin/Mixin;");
        a.values = new ArrayList<>(List.of("value", new ArrayList<Object>(List.of(Type.getObjectType(target)))));
        n.invisibleAnnotations = new ArrayList<>(List.of(a));
        return n;
    }

    private static MethodNode handler(ClassNode n, String name, String desc, String selector) {
        MethodNode m = new MethodNode(ACC_PRIVATE, name, desc, null, null);
        int slot = 1;
        for (Type t : Type.getArgumentTypes(desc)) {
            m.instructions.add(new VarInsnNode(t.getOpcode(ILOAD), slot));
            m.instructions.add(new InsnNode(t.getSize() == 2 ? POP2 : POP));
            slot += t.getSize();
        }
        m.instructions.add(new InsnNode(RETURN));
        m.maxLocals = slot;
        m.maxStack = 2;
        AnnotationNode a = new AnnotationNode(INJECT);
        a.values = new ArrayList<>(List.of("method", new ArrayList<Object>(List.of(selector))));
        AnnotationNode at = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/At;");
        at.values = new ArrayList<>(List.of("value", "HEAD"));
        a.values.add("at");
        a.values.add(new ArrayList<Object>(List.of(at)));
        m.invisibleAnnotations = new ArrayList<>(List.of(a));
        n.methods.add(m);
        return m;
    }

    private static Mixin1201.Stats run(ClassNode n) {
        Mixin1201.Stats st = new Mixin1201.Stats();
        Mixin1201.apply(n, Map.of(), new Mc1201Hierarchy(), Mc1201Tables.get(), st);
        return st;
    }

    private static MethodNode find(ClassNode n, String name) {
        for (MethodNode m : n.methods) if (m.name.equals(name)) return m;
        return null;
    }

    private static long calls(MethodNode m, String name) {
        long c = 0;
        for (AbstractInsnNode i : m.instructions) if (i instanceof MethodInsnNode mi && mi.name.equals(name)) c++;
        return c;
    }

    @Test
    void helperDerivedParameters_gameRendererRender() {
        ClassNode n = mixin("m/RenderMixin", "net/minecraft/client/renderer/GameRenderer");
        handler(n, "onRender", "(FJZ" + CI + ")V", "render(FJZ)V");
        Mixin1201.Stats st = run(n);
        MethodNode w = find(n, "onRender$mcc1201");
        assertNotNull(w);
        assertEquals("(" + TRACKER + "Z" + CI + ")V", w.desc);
        assertEquals(1, calls(w, "partialTick"));
        assertEquals(1, calls(w, "noTime"));
        assertEquals(1, st.repair.repaired);
        assertEquals(0, st.repair.dropped);
    }

    @Test
    void packedColourSplitIntoFloats_modelPartRender() {
        ClassNode n = mixin("m/PartMixin", "net/minecraft/client/model/geom/ModelPart");
        handler(n, "onRender", "(" + POSE + VC + "IIFFFF" + CI + ")V", "render(" + POSE + VC + "IIFFFF)V");
        run(n);
        MethodNode w = find(n, "onRender$mcc1201");
        assertNotNull(w);
        assertEquals("(" + POSE + VC + "III" + CI + ")V", w.desc);
        assertEquals(1, calls(w, "colorRed"));
        assertEquals(1, calls(w, "colorGreen"));
        assertEquals(1, calls(w, "colorBlue"));
        assertEquals(1, calls(w, "colorAlpha"));
    }

    @Test
    void ambiguousAddedParameterPicksTheMappedIndex_renderClouds() {
        ClassNode n = mixin("m/CloudMixin", "net/minecraft/client/renderer/LevelRenderer");
        handler(n, "onClouds", "(" + POSE + M4 + "FDDD" + CI + ")V", "renderClouds(" + POSE + M4 + "FDDD)V");
        Mixin1201.Stats st = run(n);
        MethodNode w = find(n, "onClouds$mcc1201");
        assertNotNull(w);
        assertEquals("(" + POSE + M4 + M4 + "FDDD" + CI + ")V", w.desc);
        // old matrix = the second new matrix (slot 3: this, poseStack, frustum, projection)
        boolean readsProjection = false;
        for (AbstractInsnNode i : w.instructions) if (i instanceof VarInsnNode v && v.getOpcode() == ALOAD && v.var == 3) readsProjection = true;
        assertTrue(readsProjection);
        assertEquals(1, st.repair.repaired);
    }

    @Test
    void returnTypeBecameVoid_chatScreenHandleChatInput() {
        ClassNode n = mixin("m/ChatMixin", "net/minecraft/client/gui/screens/ChatScreen");
        MethodNode h = handler(n, "onInput", "(Ljava/lang/String;Z" + CIR + ")V", "handleChatInput(Ljava/lang/String;Z)Z");
        Mixin1201.Stats st = run(n);
        MethodNode w = find(n, "onInput$mcc1201");
        assertNotNull(w, "a wrapper taking the plain CallbackInfo");
        assertEquals("(Ljava/lang/String;Z" + CI + ")V", w.desc);
        assertEquals(1, calls(w, "returnableOf"), "the old handler gets a returnable made from the info");
        assertEquals(1, calls(w, "propagate"), "a cancel of the returnable cancels the info");
        assertEquals("(Ljava/lang/String;Z" + CIR + ")V", h.desc, "the original handler is untouched");
        assertEquals(1, st.repair.repaired);
    }

    @Test
    void handlerThatDoesNotMatchTheOldShapeIsNotWrapped() {
        ClassNode n = mixin("m/BadMixin", "net/minecraft/client/renderer/GameRenderer");
        // captures (long) where the old target has (float): not a prefix of the old signature
        handler(n, "onRender", "(J" + CI + ")V", "render(FJZ)V");
        Mixin1201.Stats st = run(n);
        assertNull(find(n, "onRender$mcc1201"));
        assertEquals(0, st.repair.repaired);
    }

    @Test
    void everyRowOfTheTableResolvesAgainstTheIndex() throws Exception {
        int rows = 0;
        try (BufferedReader r = new BufferedReader(new InputStreamReader(
                TargetRemap.class.getClassLoader().getResourceAsStream("legacyconnector/forward/1.20.1/mixin-targets.tsv"), StandardCharsets.UTF_8))) {
            for (String line; (line = r.readLine()) != null; ) {
                if (!line.startsWith("M\t")) continue;
                String[] f = line.split("\t", -1);
                TargetRemap.Entry e = TargetRemap.method(f[1], f[2], f[3]);
                assertNotNull(e, line);
                assertTrue(McMembers1201.get(f[1]).methods().contains(f[2] + e.newDesc()), "1.21.1 has " + f[1] + "." + f[2] + e.newDesc());
                assertEquals(Type.getArgumentTypes(f[3]).length, e.argMap().size(), "one source per old parameter: " + line);
                rows++;
            }
        }
        assertTrue(rows >= 15, "rows read: " + rows);
    }
}
