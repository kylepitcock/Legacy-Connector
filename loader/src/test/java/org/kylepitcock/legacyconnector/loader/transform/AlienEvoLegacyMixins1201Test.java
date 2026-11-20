package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Verifies the three previously dropped AlienEvo mixins now target real 1.21.1 members. */
class AlienEvoLegacyMixins1201Test implements Opcodes {
    private static final String CI = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String RENDER_TYPE = "net/minecraft/client/renderer/RenderType";
    private static final String BUFFER_SOURCE = "net/minecraft/client/renderer/MultiBufferSource$BufferSource";
    private static final String COMPAT_BUFFER_BUILDER = "org/kylepitcock/legacyconnector/v1_20_1/migrate/CompatBufferBuilder";
    private static final String BYTE_BUFFER_BUILDER = "com/mojang/blaze3d/vertex/ByteBufferBuilder";
    private static final String GUI_GRAPHICS = "net/minecraft/client/gui/GuiGraphics";
    private static final String CAMERA = "net/minecraft/client/Camera";

    private static ClassNode node(String name) {
        ClassNode node = new ClassNode();
        node.version = V17;
        node.access = ACC_PUBLIC | ACC_ABSTRACT;
        node.name = name;
        node.superName = "java/lang/Object";
        return node;
    }

    private static MethodNode method(ClassNode node, int access, String name, String desc) {
        MethodNode method = new MethodNode(access, name, desc, null, null);
        node.methods.add(method);
        return method;
    }

    private static AnnotationNode annotation(String descriptor) {
        return new AnnotationNode(descriptor);
    }

    private static void verify(ClassNode node) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0);
    }

    @Test
    void tintedGlintRegistersOnTheCurrentRenderBuffersBufferSource() {
        ClassNode node = node(AlienEvoLegacyMixins1201.RENDER_BUFFERS);
        FieldNode shadow = new FieldNode(ACC_PRIVATE, "fixedBuffers", "Ljava/util/SortedMap;", null,
                "Ljava/util/SortedMap<L" + RENDER_TYPE + ";L" + COMPAT_BUFFER_BUILDER + ";>;");
        shadow.visibleAnnotations = new ArrayList<>(List.of(
                annotation("Lorg/spongepowered/asm/mixin/Shadow;"),
                annotation("Lorg/spongepowered/asm/mixin/Final;")));
        node.fields.add(shadow);

        MethodNode handler = method(node, ACC_PRIVATE, "alienevo$registerTintedGlint", "(" + CI + ")V");
        handler.instructions.add(new VarInsnNode(ALOAD, 0));
        handler.instructions.add(new FieldInsnNode(GETFIELD, node.name, "fixedBuffers", "Ljava/util/SortedMap;"));
        handler.instructions.add(new FieldInsnNode(GETSTATIC, "net/habb/alienevo/client/renderer/AlienEvoRenderTypes",
                "TINTED_GLINT", "L" + RENDER_TYPE + ";"));
        handler.instructions.add(new TypeInsnNode(NEW, COMPAT_BUFFER_BUILDER));
        handler.instructions.add(new InsnNode(DUP));
        handler.instructions.add(new FieldInsnNode(GETSTATIC, "net/habb/alienevo/client/renderer/AlienEvoRenderTypes",
                "TINTED_GLINT", "L" + RENDER_TYPE + ";"));
        handler.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, RENDER_TYPE, "bufferSize", "()I", false));
        handler.instructions.add(new MethodInsnNode(INVOKESPECIAL, COMPAT_BUFFER_BUILDER,
                "<init>", "(I)V", false));
        handler.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/SortedMap", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true));
        handler.instructions.add(new InsnNode(POP));
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoLegacyMixins1201.apply(node));
        assertEquals("bufferSource", shadow.name);
        assertEquals("L" + BUFFER_SOURCE + ";", shadow.desc);
        assertNotNull(shadow.visibleAnnotations.stream()
                .filter(a -> a.desc.equals("Lorg/spongepowered/asm/mixin/Shadow;")).findFirst().orElse(null));
        assertNotNull(McMembers1201.get("net/minecraft/client/renderer/RenderBuffers"));
        assertTrue(McMembers1201.get("net/minecraft/client/renderer/RenderBuffers").fields()
                .contains("bufferSource:L" + BUFFER_SOURCE + ";"));
        assertNotNull(findField(handler, BUFFER_SOURCE, "fixedBuffers", "Ljava/util/SequencedMap;"));
        assertNotNull(findType(handler, NEW, BYTE_BUFFER_BUILDER));
        assertNotNull(findCall(handler, INVOKESPECIAL, BYTE_BUFFER_BUILDER, "<init>", "(I)V"));
        assertNotNull(findCall(handler, INVOKEINTERFACE, "java/util/Map", "put",
                "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
        assertFalse(hasField(handler, node.name, "fixedBuffers"));
        assertNull(Mixin1201.missingShadow(node,
                List.of("net/minecraft/client/renderer/RenderBuffers"), new Mc1201Hierarchy()),
                "the replacement @Shadow field must exist on 1.21.1 RenderBuffers");
        verify(node);
    }

    @Test
    void tintedGlintUsesTheCurrentItemGlintShader() {
        ClassNode node = node("net/habb/alienevo/client/renderer/AlienEvoRenderTypes");
        MethodNode clinit = method(node, ACC_STATIC, "<clinit>", "()V");
        String owner = "net/minecraft/client/renderer/RenderStateShard";
        String desc = "Lnet/minecraft/client/renderer/RenderStateShard$ShaderStateShard;";
        clinit.instructions.add(new FieldInsnNode(GETSTATIC, owner, "RENDERTYPE_GLINT_DIRECT_SHADER", desc));
        clinit.instructions.add(new InsnNode(POP));
        clinit.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoLegacyMixins1201.apply(node));
        assertNotNull(findField(clinit, owner, "RENDERTYPE_GLINT_SHADER", desc));
        assertFalse(hasField(clinit, owner, "RENDERTYPE_GLINT_DIRECT_SHADER"));
        assertEquals(0, AlienEvoLegacyMixins1201.apply(node), "the shader rewrite must be idempotent");
        verify(node);
    }

    @Test
    void rifleHeatMeterUsesGuiGraphicsDimensionsAndCurrentCrosshairSignature() {
        ClassNode node = node(AlienEvoLegacyMixins1201.RIFLE_CROSSHAIR);
        node.fields.add(new FieldNode(ACC_PRIVATE, "screenWidth", "I", null, null));
        node.fields.add(new FieldNode(ACC_PRIVATE, "screenHeight", "I", null, null));

        MethodNode handler = method(node, ACC_PRIVATE, "alienevo$drawHeatMeter",
                "(L" + GUI_GRAPHICS + ";" + CI + ")V");
        handler.instructions.add(new MethodInsnNode(INVOKESTATIC, "net/habb/alienevo/client/hud/RifleHeatOverlay",
                "shouldRender", "()Z", false));
        LabelNode done = new LabelNode();
        handler.instructions.add(new JumpInsnNode(IFEQ, done));
        handler.instructions.add(new VarInsnNode(ALOAD, 1));
        handler.instructions.add(new VarInsnNode(ALOAD, 0));
        handler.instructions.add(new FieldInsnNode(GETFIELD, node.name, "screenWidth", "I"));
        handler.instructions.add(new VarInsnNode(ALOAD, 0));
        handler.instructions.add(new FieldInsnNode(GETFIELD, node.name, "screenHeight", "I"));
        handler.instructions.add(new MethodInsnNode(INVOKESTATIC, "net/habb/alienevo/client/hud/RifleHeatOverlay",
                "render", "(L" + GUI_GRAPHICS + ";II)V", false));
        handler.instructions.add(done);
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoLegacyMixins1201.apply(node));
        assertEquals("(L" + GUI_GRAPHICS + ";Lnet/minecraft/client/DeltaTracker;" + CI + ")V", handler.desc);
        assertTrue(node.fields.stream().noneMatch(f -> f.name.equals("screenWidth") || f.name.equals("screenHeight")));
        MethodInsnNode width = findCall(handler, INVOKEVIRTUAL, GUI_GRAPHICS, "guiWidth", "()I");
        MethodInsnNode height = findCall(handler, INVOKEVIRTUAL, GUI_GRAPHICS, "guiHeight", "()I");
        assertNotNull(width);
        assertNotNull(height);
        assertEquals(ALOAD, ((VarInsnNode) previousReal(width)).getOpcode());
        assertEquals(1, ((VarInsnNode) previousReal(width)).var,
                "guiWidth must receive the GuiGraphics handler argument, not the Gui mixin target");
        assertEquals(ALOAD, ((VarInsnNode) previousReal(height)).getOpcode());
        assertEquals(1, ((VarInsnNode) previousReal(height)).var,
                "guiHeight must receive the GuiGraphics handler argument, not the Gui mixin target");
        assertFalse(hasField(handler, node.name, "screenWidth"));
        assertFalse(hasField(handler, node.name, "screenHeight"));
        assertNull(Mixin1201.missingShadow(node,
                List.of("net/minecraft/client/gui/Gui"), new Mc1201Hierarchy()),
                "the heat meter should no longer shadow removed Gui dimension fields");
        verify(node);
    }

    @Test
    void whirlpoolCameraUsesTheFloatMoveMethod() {
        ClassNode node = node(AlienEvoLegacyMixins1201.WHIRLPOOL_CAMERA);
        MethodNode shadow = method(node, ACC_PROTECTED | ACC_ABSTRACT, "m_90568_", "(DDD)V");
        shadow.visibleAnnotations = new ArrayList<>(List.of(annotation("Lorg/spongepowered/asm/mixin/Shadow;")));

        MethodNode handler = method(node, ACC_PRIVATE, "alienevo$whirlpoolZoom",
                "(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/world/entity/Entity;ZZF" + CI + ")V");
        handler.instructions.add(new VarInsnNode(ILOAD, 3));
        LabelNode done = new LabelNode();
        handler.instructions.add(new JumpInsnNode(IFNE, done));
        handler.instructions.add(new VarInsnNode(ALOAD, 2));
        handler.instructions.add(new TypeInsnNode(INSTANCEOF, "net/minecraft/world/entity/player/Player"));
        handler.instructions.add(new JumpInsnNode(IFEQ, done));
        handler.instructions.add(new VarInsnNode(ALOAD, 2));
        handler.instructions.add(new TypeInsnNode(CHECKCAST, "net/minecraft/world/entity/player/Player"));
        handler.instructions.add(new VarInsnNode(ASTORE, 7));
        handler.instructions.add(new VarInsnNode(ALOAD, 7));
        handler.instructions.add(new FieldInsnNode(GETSTATIC, "net/habb/alienevo/ability/EvoAbilities",
                "RIPJAWS_WHIRLPOOL", "Lnet/threetag/palladiumcore/registry/RegistrySupplier;"));
        handler.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "net/threetag/palladiumcore/registry/RegistrySupplier",
                "get", "()Ljava/lang/Object;", false));
        handler.instructions.add(new TypeInsnNode(CHECKCAST, "net/threetag/palladium/power/ability/Ability"));
        handler.instructions.add(new MethodInsnNode(INVOKESTATIC, "net/threetag/palladium/power/ability/AbilityUtil",
                "getEnabledInstances", "(Lnet/minecraft/world/entity/LivingEntity;Lnet/threetag/palladium/power/ability/Ability;)Ljava/util/Collection;",
                false));
        handler.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "java/util/Collection", "isEmpty", "()Z", true));
        handler.instructions.add(new JumpInsnNode(IFNE, done));
        handler.instructions.add(new VarInsnNode(ALOAD, 0));
        handler.instructions.add(new LdcInsnNode(-6.0D));
        handler.instructions.add(new InsnNode(DCONST_0));
        handler.instructions.add(new InsnNode(DCONST_0));
        handler.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, node.name, "m_90568_", "(DDD)V", false));
        handler.instructions.add(done);
        handler.instructions.add(new InsnNode(RETURN));

        assertEquals(1, AlienEvoLegacyMixins1201.apply(node));
        assertEquals("move", shadow.name);
        assertEquals("(FFF)V", shadow.desc);
        assertNotNull(McMembers1201.get(CAMERA));
        assertTrue(McMembers1201.get(CAMERA).methods().contains("move(FFF)V"));
        assertNotNull(findCall(handler, INVOKEVIRTUAL, node.name, "move", "(FFF)V"));
        assertTrue(handler.instructions.toArray().length > 0);
        int floatZeroes = 0;
        for (var instruction : handler.instructions.toArray()) {
            assertFalse(instruction.getOpcode() == DCONST_0, "all zero offsets must be float constants");
            if (instruction.getOpcode() == FCONST_0) floatZeroes++;
            if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof Double) {
                throw new AssertionError("the old double zoom constant must be narrowed");
            }
        }
        assertEquals(2, floatZeroes);
        assertNull(Mixin1201.missingShadow(node,
                List.of("net/minecraft/client/Camera"), new Mc1201Hierarchy()),
                "the replacement @Shadow move(float,float,float) must exist on 1.21.1 Camera");
        verify(node);
    }

    private static MethodInsnNode findCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.getOpcode() == opcode
                    && call.owner.equals(owner) && call.name.equals(name) && call.desc.equals(desc)) return call;
        }
        return null;
    }

    private static FieldInsnNode findField(MethodNode method, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(owner)
                    && field.name.equals(name) && field.desc.equals(desc)) return field;
        }
        return null;
    }

    private static TypeInsnNode findType(MethodNode method, int opcode, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof TypeInsnNode type && type.getOpcode() == opcode && type.desc.equals(desc)) return type;
        }
        return null;
    }

    private static AbstractInsnNode previousReal(AbstractInsnNode instruction) {
        AbstractInsnNode previous = instruction.getPrevious();
        while (previous != null && previous.getOpcode() < 0) {
            previous = previous.getPrevious();
        }
        return previous;
    }

    private static boolean hasField(MethodNode method, String owner, String name) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field && field.owner.equals(owner) && field.name.equals(name)) return true;
        }
        return false;
    }
}
