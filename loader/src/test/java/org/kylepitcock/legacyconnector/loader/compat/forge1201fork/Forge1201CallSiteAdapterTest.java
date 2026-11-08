package org.kylepitcock.legacyconnector.loader.compat.forge1201fork;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InnerClassNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

final class Forge1201CallSiteAdapterTest {
    private static final String EVENT = "net/minecraftforge/event/entity/EntityEvent$Size";
    private static final String DIMENSIONS = "net/minecraft/world/entity/EntityDimensions";
    private static final String DIMENSIONS_DESC = "L" + DIMENSIONS + ";";

    @TempDir
    Path tempDir;

    @Test
    void loadsProfileAndAppliesExactFieldMethodAndCheckcastRules() throws Exception {
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "synthetic-descriptor-profile",
                  "rules": [{
                    "className": "sample/AdapterFixture",
                    "methodName": "adapt",
                    "methodDescriptor": "()V",
                    "operations": [
                      {
                        "type": "field-descriptor",
                        "owner": "sample/Attributes",
                        "name": "DAMAGE",
                        "descriptor": "Lsample/Attribute;",
                        "replacementDescriptor": "Lsample/Holder;",
                        "opcode": "GETSTATIC",
                        "expectedMatches": 1
                      },
                      {
                        "type": "method-descriptor",
                        "owner": "sample/Event",
                        "name": "has",
                        "descriptor": "(Lsample/Attribute;)Z",
                        "replacementDescriptor": "(Lsample/Holder;)Z",
                        "opcode": "INVOKEVIRTUAL",
                        "expectedMatches": 1
                      },
                      {
                        "type": "checkcast-type",
                        "sourceType": "java/lang/String",
                        "replacementType": "sample/SkinModel",
                        "expectedMatches": 1
                      }
                    ]
                  }]
                }
                """);

        Forge1201CallSiteAdapter.Profile profile = Forge1201CallSiteAdapter.load(profileFile);
        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(profile);
        ClassNode node = new ClassNode();
        node.name = "sample/AdapterFixture";
        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "adapt", "()V", null, null);
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "sample/Attributes", "DAMAGE", "Lsample/Attribute;"));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "sample/Event", "has", "(Lsample/Attribute;)Z", false));
        method.instructions.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/String"));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);

        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals("synthetic-descriptor-profile", stats.profileId());
        assertEquals(1, stats.rulesApplied());
        assertEquals(3, stats.operationsApplied());
        assertEquals("Lsample/Holder;", ((FieldInsnNode) method.instructions.getFirst()).desc);
        assertEquals("(Lsample/Holder;)Z",
                ((MethodInsnNode) method.instructions.get(1)).desc);
        assertEquals("sample/SkinModel", ((TypeInsnNode) method.instructions.get(2)).desc);
    }

    @Test
    void rewritesInjectedConstructorDescriptorAndExplicitFrameLocals() throws Exception {
        String className = "sample/BootstrapMod";
        String forgeBus = "net/minecraftforge/eventbus/api/IEventBus";
        String container = "net/neoforged/fml/ModContainer";
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "synthetic-bootstrap-frame-profile",
                  "rules": [{
                    "className": "sample/BootstrapMod",
                    "methodName": "<init>",
                    "methodDescriptor": "()V",
                    "operations": [
                      {
                        "type": "method-declaration-descriptor",
                        "descriptor": "()V",
                        "replacementDescriptor": "(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;)V",
                        "expectedMatches": 1
                      },
                      {
                        "type": "frame-local-type",
                        "localIndex": 2,
                        "sourceType": "net/minecraftforge/eventbus/api/IEventBus",
                        "replacementType": "net/neoforged/fml/ModContainer",
                        "expectedMatches": 1
                      },
                      {
                        "type": "local-variable",
                        "localIndex": 2,
                        "name": "forgeBus",
                        "descriptor": "Lnet/minecraftforge/eventbus/api/IEventBus;",
                        "replacementName": "container",
                        "replacementDescriptor": "Lnet/neoforged/fml/ModContainer;",
                        "expectedMatches": 1
                      }
                    ]
                  }]
                }
                """);

        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = className;
        node.superName = "java/lang/Object";

        MethodNode constructor = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        LabelNode localStart = new LabelNode();
        LabelNode branchTarget = new LabelNode();
        LabelNode localEnd = new LabelNode();
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        constructor.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));
        constructor.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        constructor.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));
        constructor.instructions.add(localStart);
        constructor.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        constructor.instructions.add(new JumpInsnNode(Opcodes.IFNULL, branchTarget));
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.instructions.add(branchTarget);
        constructor.instructions.add(new FrameNode(
                Opcodes.F_FULL, 3, new Object[]{className, forgeBus, forgeBus}, 0, null));
        constructor.instructions.add(localEnd);
        constructor.instructions.add(new InsnNode(Opcodes.RETURN));
        constructor.localVariables = new ArrayList<>();
        constructor.localVariables.add(new LocalVariableNode(
                "forgeBus", "L" + forgeBus + ";", null, localStart, localEnd, 2));
        constructor.maxStack = 1;
        constructor.maxLocals = 3;
        node.methods.add(constructor);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(profileFile));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals(1, stats.rulesApplied());
        assertEquals(3, stats.operationsApplied());
        assertEquals(
                "(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/fml/ModContainer;)V",
                constructor.desc);
        assertEquals(container, findFrame(constructor).local.get(2));
        assertEquals("container", constructor.localVariables.get(0).name);
        assertEquals("L" + container + ";", constructor.localVariables.get(0).desc);

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        ClassNode roundTripped = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(roundTripped, 0);
        MethodNode writtenConstructor = roundTripped.methods.stream()
                .filter(method -> method.name.equals("<init>"))
                .findFirst()
                .orElseThrow();
        assertEquals(constructor.desc, writtenConstructor.desc);
        assertEquals(container, findFrame(writtenConstructor).local.get(2));
        assertEquals("L" + container + ";", writtenConstructor.localVariables.get(0).desc);
    }

    @Test
    void replacesLazyDistExecutorCallsWithOneSyntheticPhysicalSideHelper() throws Exception {
        String className = "sample/ClientBootstrap";
        String dist = "net/minecraftforge/api/distmarker/Dist";
        String owner = "net/minecraftforge/fml/DistExecutor";
        String descriptor = "(L" + dist + ";Ljava/util/function/Supplier;)V";
        String helperName = "$legacyconnector$runWhenOn";
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "synthetic-lazy-dist-profile",
                  "rules": [{
                    "className": "sample/ClientBootstrap",
                    "methodName": "register",
                    "methodDescriptor": "(Lnet/minecraftforge/api/distmarker/Dist;Ljava/util/function/Supplier;)V",
                    "operations": [{
                      "type": "lazy-dist-gate",
                      "owner": "net/minecraftforge/fml/DistExecutor",
                      "name": "unsafeRunWhenOn",
                      "descriptor": "(Lnet/minecraftforge/api/distmarker/Dist;Ljava/util/function/Supplier;)V",
                      "replacementName": "$legacyconnector$runWhenOn",
                      "expectedMatches": 2
                    }]
                  }]
                }
                """);

        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = className;
        node.superName = "java/lang/Object";
        MethodNode register = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "register", descriptor, null, null);
        for (int i = 0; i < 2; i++) {
            register.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            register.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
            register.instructions.add(new MethodInsnNode(
                    Opcodes.INVOKESTATIC, owner, "unsafeRunWhenOn", descriptor, false));
        }
        register.instructions.add(new InsnNode(Opcodes.RETURN));
        register.maxStack = 2;
        register.maxLocals = 2;
        node.methods.add(register);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(profileFile));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals(1, stats.rulesApplied());
        assertEquals(1, stats.operationsApplied());
        assertEquals(2, countCalls(register, className, helperName, descriptor));
        List<MethodNode> helpers = node.methods.stream()
                .filter(method -> method.name.equals(helperName) && method.desc.equals(descriptor))
                .toList();
        assertEquals(1, helpers.size());
        MethodNode helper = helpers.get(0);
        assertTrue((helper.access & Opcodes.ACC_PRIVATE) != 0);
        assertTrue((helper.access & Opcodes.ACC_STATIC) != 0);
        assertTrue((helper.access & Opcodes.ACC_SYNTHETIC) != 0);
        assertTrue(containsField(
                helper,
                "net/neoforged/fml/loading/FMLEnvironment",
                "dist",
                "Lnet/neoforged/api/distmarker/Dist;"));
        assertTrue(containsCall(helper, "java/util/function/Supplier", "get"));
        assertTrue(containsCall(helper, "java/lang/Runnable", "run"));
        assertTrue(containsFrame(helper, Opcodes.F_SAME));

        int physicalSideBranchIndex = -1;
        int supplierGetIndex = -1;
        int helperInstructionIndex = 0;
        for (AbstractInsnNode instruction : helper.instructions) {
            if (instruction instanceof JumpInsnNode jump && jump.getOpcode() == Opcodes.IF_ACMPNE) {
                physicalSideBranchIndex = helperInstructionIndex;
            }
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals("java/util/function/Supplier")
                    && call.name.equals("get")) {
                supplierGetIndex = helperInstructionIndex;
            }
            helperInstructionIndex++;
        }
        assertTrue(physicalSideBranchIndex >= 0, "Expected a physical-side mismatch branch");
        assertTrue(supplierGetIndex > physicalSideBranchIndex,
                "The supplier must remain unevaluated until after the physical-side check");
        assertTrue(helper.maxStack >= 2);

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        ClassNode roundTripped = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(roundTripped, 0);
        MethodNode writtenHelper = roundTripped.methods.stream()
                .filter(method -> method.name.equals(helperName))
                .findFirst()
                .orElseThrow();
        assertTrue(containsFrame(writtenHelper, Opcodes.F_SAME));
    }

    @Test
    void failsClosedWhenRequestedFrameLocalDoesNotMatch() throws Exception {
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "stale-frame-profile",
                  "rules": [{
                    "className": "sample/FrameFixture",
                    "methodName": "run",
                    "methodDescriptor": "()V",
                    "operations": [{
                      "type": "frame-local-type",
                      "localIndex": 1,
                      "sourceType": "sample/Expected",
                      "replacementType": "sample/Replacement",
                      "expectedMatches": 1
                    }]
                  }]
                }
                """);
        ClassNode node = new ClassNode();
        node.name = "sample/FrameFixture";
        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "run", "()V", null, null);
        method.instructions.add(new FrameNode(
                Opcodes.F_FULL, 2, new Object[]{"sample/FrameFixture", "sample/Actual"}, 0, null));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        node.methods.add(method);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(profileFile));
        assertThrows(IOException.class, () -> session.apply(node));
    }

    private static FrameNode findFrame(MethodNode method) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FrameNode frame) return frame;
        }
        throw new AssertionError("Expected a stack-map frame");
    }

    @Test
    void rewritesEntityDimensionsEyeHeightWithoutDroppingScaleOrClamp() throws Exception {
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "synthetic-size-profile",
                  "rules": [{
                    "className": "sample/SizeAdapter",
                    "methodName": "apply",
                    "methodDescriptor": "(Lnet/minecraftforge/event/entity/EntityEvent$Size;)V",
                    "operations": [
                      {
                        "type": "instruction-sequence",
                        "match": [
                          { "kind": "var", "opcode": "ALOAD", "var": 1 },
                          {
                            "kind": "field",
                            "opcode": "GETFIELD",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "width",
                            "descriptor": "F"
                          }
                        ],
                        "replaceWith": [
                          { "kind": "var", "opcode": "ALOAD", "var": 1 },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "width",
                            "descriptor": "()F"
                          }
                        ],
                        "expectedMatches": 1
                      },
                      {
                        "type": "instruction-sequence",
                        "match": [
                          { "kind": "var", "opcode": "ALOAD", "var": 0 },
                          { "kind": "var", "opcode": "ALOAD", "var": 2 },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraftforge/event/entity/EntityEvent$Size",
                            "name": "setNewSize",
                            "descriptor": "(Lnet/minecraft/world/entity/EntityDimensions;)V"
                          }
                        ],
                        "replaceWith": [
                          { "kind": "var", "opcode": "ALOAD", "var": 0 },
                          { "kind": "var", "opcode": "ALOAD", "var": 2 },
                          { "kind": "var", "opcode": "ALOAD", "var": 0 },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraftforge/event/entity/EntityEvent$Size",
                            "name": "getNewSize",
                            "descriptor": "()Lnet/minecraft/world/entity/EntityDimensions;"
                          },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "eyeHeight",
                            "descriptor": "()F"
                          },
                          { "kind": "ldc", "valueType": "float", "value": 1.35 },
                          { "kind": "insn", "opcode": "FMUL" },
                          { "kind": "ldc", "valueType": "float", "value": 0.05 },
                          { "kind": "var", "opcode": "ALOAD", "var": 2 },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "height",
                            "descriptor": "()F"
                          },
                          { "kind": "ldc", "valueType": "float", "value": 0.05 },
                          { "kind": "insn", "opcode": "FSUB" },
                          {
                            "kind": "method",
                            "opcode": "INVOKESTATIC",
                            "owner": "java/lang/Math",
                            "name": "min",
                            "descriptor": "(FF)F"
                          },
                          {
                            "kind": "method",
                            "opcode": "INVOKESTATIC",
                            "owner": "java/lang/Math",
                            "name": "max",
                            "descriptor": "(FF)F"
                          },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "withEyeHeight",
                            "descriptor": "(F)Lnet/minecraft/world/entity/EntityDimensions;"
                          },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraftforge/event/entity/EntityEvent$Size",
                            "name": "setNewSize",
                            "descriptor": "(Lnet/minecraft/world/entity/EntityDimensions;)V"
                          }
                        ],
                        "expectedMatches": 1,
                        "minimumMaxStack": 6
                      },
                      {
                        "type": "instruction-sequence",
                        "match": [
                          { "kind": "var", "opcode": "ALOAD", "var": 0 },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraftforge/event/entity/EntityEvent$Size",
                            "name": "getNewEyeHeight",
                            "descriptor": "()F"
                          }
                        ],
                        "replaceWith": [
                          { "kind": "insn", "opcode": "FCONST_0" }
                        ],
                        "expectedMatches": 1
                      },
                      {
                        "type": "instruction-sequence",
                        "match": [
                          { "kind": "var", "opcode": "ALOAD", "var": 0 },
                          { "kind": "ldc", "valueType": "float", "value": 0.05 },
                          { "kind": "var", "opcode": "ALOAD", "var": 2 },
                          {
                            "kind": "field",
                            "opcode": "GETFIELD",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "height",
                            "descriptor": "F"
                          },
                          { "kind": "ldc", "valueType": "float", "value": 0.05 },
                          { "kind": "insn", "opcode": "FSUB" },
                          { "kind": "var", "opcode": "FLOAD", "var": 3 },
                          {
                            "kind": "method",
                            "opcode": "INVOKESTATIC",
                            "owner": "java/lang/Math",
                            "name": "min",
                            "descriptor": "(FF)F"
                          },
                          {
                            "kind": "method",
                            "opcode": "INVOKESTATIC",
                            "owner": "java/lang/Math",
                            "name": "max",
                            "descriptor": "(FF)F"
                          },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraftforge/event/entity/EntityEvent$Size",
                            "name": "setNewEyeHeight",
                            "descriptor": "(F)V"
                          }
                        ],
                        "replaceWith": [],
                        "expectedMatches": 1
                      },
                      {
                        "type": "instruction-sequence",
                        "match": [
                          { "kind": "var", "opcode": "ALOAD", "var": 1 },
                          {
                            "kind": "field",
                            "opcode": "GETFIELD",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "height",
                            "descriptor": "F"
                          }
                        ],
                        "replaceWith": [
                          { "kind": "var", "opcode": "ALOAD", "var": 1 },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/EntityDimensions",
                            "name": "height",
                            "descriptor": "()F"
                          }
                        ],
                        "expectedMatches": 1
                      }
                    ]
                  }]
                }
                """);

        ClassNode node = new ClassNode();
        node.name = "sample/SizeAdapter";
        MethodNode method = createSizeMethod();
        node.methods.add(method);

        Forge1201CallSiteAdapter.Session session =
                new Forge1201CallSiteAdapter.Session(Forge1201CallSiteAdapter.load(profileFile));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals(1, stats.rulesApplied());
        assertEquals(5, stats.operationsApplied());
        assertEquals(2, countCalls(method, EVENT, "getNewSize", "()" + DIMENSIONS_DESC));
        assertEquals(1, countCalls(method, DIMENSIONS, "eyeHeight", "()F"));
        assertEquals(1, countCalls(method, DIMENSIONS, "withEyeHeight", "(F)" + DIMENSIONS_DESC));
        assertEquals(1, countCalls(method, EVENT, "setNewSize", "(" + DIMENSIONS_DESC + ")V"));
        assertEquals(1, countCalls(method, DIMENSIONS, "width", "()F"));
        assertEquals(2, countCalls(method, DIMENSIONS, "height", "()F"));
        assertEquals(0, countCalls(method, EVENT, "getNewEyeHeight", "()F"));
        assertEquals(0, countCalls(method, EVENT, "setNewEyeHeight", "(F)V"));
        assertEquals(0, countVars(method, Opcodes.FLOAD, 3));
        assertEquals(1, countVars(method, Opcodes.FSTORE, 3));
        assertEquals(4, method.maxLocals);
        assertTrue(method.maxStack >= 6);

        List<String> calls = new ArrayList<>();
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call) calls.add(call.owner + "#" + call.name);
        }
        int withEyeHeightIndex = calls.indexOf(DIMENSIONS + "#withEyeHeight");
        int setNewSizeIndex = calls.lastIndexOf(EVENT + "#setNewSize");
        assertTrue(withEyeHeightIndex >= 0);
        assertTrue(setNewSizeIndex > withEyeHeightIndex);
    }

    @Test
    void failsClosedWhenARequiredInstructionCountDoesNotMatch() throws Exception {
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "stale-profile",
                  "rules": [{
                    "className": "sample/AdapterFixture",
                    "methodName": "adapt",
                    "methodDescriptor": "()V",
                    "operations": [{
                      "type": "method-descriptor",
                      "owner": "sample/Event",
                      "name": "has",
                      "descriptor": "(Lsample/Attribute;)Z",
                      "replacementDescriptor": "(Lsample/Holder;)Z",
                      "opcode": "INVOKEVIRTUAL",
                      "expectedMatches": 2
                    }]
                  }]
                }
                """);

        ClassNode node = new ClassNode();
        node.name = "sample/AdapterFixture";
        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "adapt", "()V", null, null);
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "sample/Event", "has", "(Lsample/Attribute;)Z", false));
        node.methods.add(method);

        Forge1201CallSiteAdapter.Session session =
                new Forge1201CallSiteAdapter.Session(Forge1201CallSiteAdapter.load(profileFile));
        assertThrows(IOException.class, () -> session.apply(node));
    }

    @Test
    void failsImmediatelyWhenARequiredMethodWasNotFound() throws Exception {
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "missing-method-profile",
                  "rules": [{
                    "className": "sample/AdapterFixture",
                    "methodName": "required",
                    "methodDescriptor": "()V",
                    "operations": [{
                      "type": "checkcast-type",
                      "sourceType": "java/lang/String",
                      "replacementType": "sample/Model",
                      "expectedMatches": 1
                    }]
                  }]
                }
                """);

        Forge1201CallSiteAdapter.Session session =
                new Forge1201CallSiteAdapter.Session(Forge1201CallSiteAdapter.load(profileFile));
        ClassNode node = new ClassNode();
        node.name = "sample/AdapterFixture";
        node.methods.add(new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "different", "()V", null, null));
        assertThrows(IOException.class, () -> session.apply(node));
    }

    @Test
    void appliesProfileBeforePackageRelocationInsideJarTransform() throws Exception {
        Path input = tempDir.resolve("metadata-translated.jar");
        Path output = tempDir.resolve("profile-transformed.jar");
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "jar-integration-profile",
                  "rules": [{
                    "className": "sample/AdapterFixture",
                    "methodName": "adapt",
                    "methodDescriptor": "()V",
                    "operations": [
                      {
                        "type": "checkcast-type",
                        "sourceType": "java/lang/String",
                        "replacementType": "net/minecraft/client/resources/PlayerSkin$Model",
                        "expectedMatches": 1
                      },
                      {
                        "type": "instruction-sequence",
                        "match": [{
                          "kind": "method",
                          "opcode": "INVOKEVIRTUAL",
                          "owner": "net/minecraftforge/client/event/EntityRenderersEvent$AddLayers",
                          "name": "getSkin",
                          "descriptor": "(Ljava/lang/String;)Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;"
                        }],
                        "replaceWith": [
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraftforge/client/event/EntityRenderersEvent$AddLayers",
                            "name": "getSkin",
                            "descriptor": "(Lnet/minecraft/client/resources/PlayerSkin$Model;)Lnet/minecraft/client/renderer/entity/EntityRenderer;"
                          },
                          {
                            "kind": "type",
                            "opcode": "CHECKCAST",
                            "typeName": "net/minecraft/client/renderer/entity/LivingEntityRenderer"
                          }
                        ],
                        "expectedMatches": 1
                      }
                    ]
                  }]
                }
                """);
        createMetadataTranslatedJar(input, createRendererFixtureClass());

        Forge1201BytecodeTransformer.Result result =
                Forge1201BytecodeTransformer.transform(input, output, profileFile);

        assertEquals("jar-integration-profile", result.adapterProfileId());
        assertEquals(1, result.adapterRulesApplied());
        assertEquals(2, result.adapterOperationsApplied());
        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(output.toFile())) {
            ClassNode transformed = new ClassNode();
            new org.objectweb.asm.ClassReader(zip.getInputStream(
                    zip.getEntry("sample/AdapterFixture.class")).readAllBytes()).accept(transformed, 0);
            MethodNode adapt = transformed.methods.stream()
                    .filter(method -> method.name.equals("adapt")).findFirst().orElseThrow();

            boolean modelCast = false;
            boolean livingRendererCast = false;
            MethodInsnNode lookup = null;
            for (AbstractInsnNode instruction : adapt.instructions) {
                if (instruction instanceof TypeInsnNode typeInsn
                        && typeInsn.getOpcode() == Opcodes.CHECKCAST) {
                    modelCast |= typeInsn.desc.equals("net/minecraft/client/resources/PlayerSkin$Model");
                    livingRendererCast |= typeInsn.desc.equals(
                            "net/minecraft/client/renderer/entity/LivingEntityRenderer");
                }
                if (instruction instanceof MethodInsnNode call) lookup = call;
            }
            assertTrue(modelCast);
            assertTrue(livingRendererCast);
            assertTrue(lookup != null);
            assertEquals("net/neoforged/neoforge/client/event/EntityRenderersEvent$AddLayers", lookup.owner);
            assertEquals("(Lnet/minecraft/client/resources/PlayerSkin$Model;)Lnet/minecraft/client/renderer/entity/EntityRenderer;",
                    lookup.desc);
        }
    }

    @Test
    void staleProfileDoesNotFinalizeAnOutputJar() throws Exception {
        Path input = tempDir.resolve("stale-input.jar");
        Path output = tempDir.resolve("stale-output.jar");
        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "stale-jar-profile",
                  "rules": [{
                    "className": "sample/MissingClass",
                    "methodName": "adapt",
                    "methodDescriptor": "()V",
                    "operations": [{
                      "type": "checkcast-type",
                      "sourceType": "java/lang/String",
                      "replacementType": "sample/Model",
                      "expectedMatches": 1
                    }]
                  }]
                }
                """);
        createMetadataTranslatedJar(input, createRendererFixtureClass());

        assertThrows(IOException.class, () -> Forge1201BytecodeTransformer.transform(input, output, profileFile));
        assertFalse(Files.exists(output));
    }

    private static byte[] createRendererFixtureClass() {
        org.objectweb.asm.ClassWriter writer =
                new org.objectweb.asm.ClassWriter(org.objectweb.asm.ClassWriter.COMPUTE_MAXS);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC, "sample/AdapterFixture", null, "java/lang/Object", null);
        var method = writer.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "adapt", "()V", null, null);
        method.visitCode();
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitTypeInsn(Opcodes.CHECKCAST,
                "net/minecraftforge/client/event/EntityRenderersEvent$AddLayers");
        method.visitInsn(Opcodes.ACONST_NULL);
        method.visitTypeInsn(Opcodes.CHECKCAST, "java/lang/String");
        method.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                "net/minecraftforge/client/event/EntityRenderersEvent$AddLayers",
                "getSkin",
                "(Ljava/lang/String;)Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;",
                false);
        method.visitInsn(Opcodes.POP);
        method.visitInsn(Opcodes.RETURN);
        method.visitMaxs(0, 0);
        method.visitEnd();
        writer.visitEnd();
        return writer.toByteArray();
    }

    private static void createMetadataTranslatedJar(Path jar, byte[] classBytes) throws IOException {
        try (java.util.zip.ZipOutputStream out =
                     new java.util.zip.ZipOutputStream(Files.newOutputStream(jar))) {
            out.putNextEntry(new java.util.zip.ZipEntry("META-INF/neoforge.mods.toml"));
            out.write("modLoader=\"javafml\"\n".getBytes(StandardCharsets.UTF_8));
            out.closeEntry();
            out.putNextEntry(new java.util.zip.ZipEntry("sample/AdapterFixture.class"));
            out.write(classBytes);
            out.closeEntry();
        }
    }

    @Test
    void rejectsUnsupportedProfileVersion() throws Exception {
        Path profileFile = writeProfile("""
                {"schemaVersion": 99, "id": "future", "rules": []}
                """);
        assertThrows(IOException.class, () -> Forge1201CallSiteAdapter.load(profileFile));
    }

    @Test
    void rewritesResourceLocationConstructorToThe1Point21Factory() throws Exception {
        String className = "com/scout/symbiote/registry/ModSounds";
        String resourceLocation = "net/minecraft/resources/ResourceLocation";
        String resourceLocationDescriptor = "L" + resourceLocation + ";";
        String soundEvent = "net/minecraft/sounds/SoundEvent";
        String methodDescriptor = "(Ljava/lang/String;)L" + soundEvent + ";";

        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "synthetic-resource-location-factory",
                  "rules": [{
                    "className": "com/scout/symbiote/registry/ModSounds",
                    "methodName": "lambda$reg$0",
                    "methodDescriptor": "(Ljava/lang/String;)Lnet/minecraft/sounds/SoundEvent;",
                    "operations": [{
                      "type": "instruction-sequence",
                      "match": [
                        { "kind": "type", "opcode": "NEW",
                          "typeName": "net/minecraft/resources/ResourceLocation" },
                        { "kind": "insn", "opcode": "DUP" },
                        { "kind": "ldc", "valueType": "string", "value": "symbiote" },
                        { "kind": "var", "opcode": "ALOAD", "var": 0 },
                        { "kind": "method", "opcode": "INVOKESPECIAL",
                          "owner": "net/minecraft/resources/ResourceLocation",
                          "name": "<init>",
                          "descriptor": "(Ljava/lang/String;Ljava/lang/String;)V" }
                      ],
                      "replaceWith": [
                        { "kind": "ldc", "valueType": "string", "value": "symbiote" },
                        { "kind": "var", "opcode": "ALOAD", "var": 0 },
                        { "kind": "method", "opcode": "INVOKESTATIC",
                          "owner": "net/minecraft/resources/ResourceLocation",
                          "name": "fromNamespaceAndPath",
                          "descriptor": "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;" }
                      ],
                      "expectedMatches": 1
                    }]
                  }]
                }
                """);

        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = className;
        node.superName = "java/lang/Object";

        MethodNode lambda = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "lambda$reg$0", methodDescriptor, null, null);
        lambda.instructions.add(new TypeInsnNode(Opcodes.NEW, resourceLocation));
        lambda.instructions.add(new InsnNode(Opcodes.DUP));
        lambda.instructions.add(new LdcInsnNode("symbiote"));
        lambda.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        lambda.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, resourceLocation, "<init>",
                "(Ljava/lang/String;Ljava/lang/String;)V", false));
        lambda.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC, soundEvent, "createVariableRangeEvent",
                "(" + resourceLocationDescriptor + ")L" + soundEvent + ";", false));
        lambda.instructions.add(new InsnNode(Opcodes.ARETURN));
        lambda.maxStack = 4;
        lambda.maxLocals = 1;
        node.methods.add(lambda);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(profileFile));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals(1, stats.rulesApplied());
        assertEquals(1, stats.operationsApplied());
        assertFalse(containsCall(lambda, resourceLocation, "<init>",
                "(Ljava/lang/String;Ljava/lang/String;)V"));
        assertTrue(containsCall(lambda, resourceLocation, "fromNamespaceAndPath",
                "(Ljava/lang/String;Ljava/lang/String;)" + resourceLocationDescriptor));

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        ClassNode roundTripped = new ClassNode();
        new ClassReader(writer.toByteArray()).accept(roundTripped, 0);
        MethodNode writtenLambda = roundTripped.methods.stream()
                .filter(method -> method.name.equals("lambda$reg$0"))
                .findFirst()
                .orElseThrow();
        assertTrue(containsCall(writtenLambda, resourceLocation, "fromNamespaceAndPath",
                "(Ljava/lang/String;Ljava/lang/String;)" + resourceLocationDescriptor));
    }

    @Test
    void rewritesAquaticEntityTagCheckAndKeepsTheConditionalBranchTarget() throws Exception {
        String mob = "net/minecraft/world/entity/Mob";
        String mobType = "net/minecraft/world/entity/MobType";
        String waterAnimal = "net/minecraft/world/entity/animal/WaterAnimal";
        String entity = "net/minecraft/world/entity/Entity";
        String entityType = "net/minecraft/world/entity/EntityType";
        String entityTypeTags = "net/minecraft/tags/EntityTypeTags";
        String tagKey = "net/minecraft/tags/TagKey";

        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "wild-host-aquatic-entity-tag",
                  "rules": [{
                    "className": "sample/AquaticClassificationFixture",
                    "methodName": "isAquatic",
                    "methodDescriptor": "(Lnet/minecraft/world/entity/Mob;)Z",
                    "operations": [
                      {
                        "type": "instruction-sequence",
                        "match": [
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/Mob",
                            "name": "m_6336_",
                            "descriptor": "()Lnet/minecraft/world/entity/MobType;"
                          },
                          {
                            "kind": "field",
                            "opcode": "GETSTATIC",
                            "owner": "net/minecraft/world/entity/MobType",
                            "name": "f_21644_",
                            "descriptor": "Lnet/minecraft/world/entity/MobType;"
                          }
                        ],
                        "replaceWith": [
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/Entity",
                            "name": "getType",
                            "descriptor": "()Lnet/minecraft/world/entity/EntityType;"
                          },
                          {
                            "kind": "field",
                            "opcode": "GETSTATIC",
                            "owner": "net/minecraft/tags/EntityTypeTags",
                            "name": "AQUATIC",
                            "descriptor": "Lnet/minecraft/tags/TagKey;"
                          },
                          {
                            "kind": "method",
                            "opcode": "INVOKEVIRTUAL",
                            "owner": "net/minecraft/world/entity/EntityType",
                            "name": "is",
                            "descriptor": "(Lnet/minecraft/tags/TagKey;)Z"
                          }
                        ],
                        "expectedMatches": 1
                      },
                      {
                        "type": "conditional-branch-opcode",
                        "opcode": "IF_ACMPNE",
                        "replacementOpcode": "IFEQ",
                        "expectedMatches": 1
                      }
                    ]
                  }]
                }
                """);

        ClassNode node = new ClassNode();
        node.name = "sample/AquaticClassificationFixture";
        MethodNode method = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "isAquatic", "(L" + mob + ";)Z", null, null);
        LabelNode aquatic = new LabelNode();
        LabelNode notAquatic = new LabelNode();
        LabelNode end = new LabelNode();
        JumpInsnNode waterAnimalShortcut = new JumpInsnNode(Opcodes.IFNE, aquatic);
        JumpInsnNode mobTypeComparison = new JumpInsnNode(Opcodes.IF_ACMPNE, notAquatic);

        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new TypeInsnNode(Opcodes.INSTANCEOF, waterAnimal));
        method.instructions.add(waterAnimalShortcut);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        LabelNode sourceLine = new LabelNode();
        method.instructions.add(sourceLine);
        method.instructions.add(new LineNumberNode(105, sourceLine));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, mob, "m_6336_", "()L" + mobType + ";", false));
        method.instructions.add(new FieldInsnNode(
                Opcodes.GETSTATIC, mobType, "f_21644_", "L" + mobType + ";"));
        method.instructions.add(mobTypeComparison);
        method.instructions.add(aquatic);
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new JumpInsnNode(Opcodes.GOTO, end));
        method.instructions.add(notAquatic);
        method.instructions.add(new InsnNode(Opcodes.ICONST_0));
        method.instructions.add(end);
        method.instructions.add(new InsnNode(Opcodes.IRETURN));
        method.maxStack = 2;
        method.maxLocals = 1;
        node.methods.add(method);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(profileFile));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals(1, stats.rulesApplied());
        assertEquals(2, stats.operationsApplied());
        assertFalse(containsCall(method, mob, "m_6336_", "()L" + mobType + ";"));
        assertFalse(containsField(method, mobType, "f_21644_", "L" + mobType + ";"));
        assertTrue(containsCall(method, entity, "getType", "()L" + entityType + ";"));
        assertTrue(containsField(method, entityTypeTags, "AQUATIC", "L" + tagKey + ";"));
        assertTrue(containsCall(method, entityType, "is", "(L" + tagKey + ";)Z"));
        assertEquals(Opcodes.IFNE, waterAnimalShortcut.getOpcode());
        assertSame(aquatic, waterAnimalShortcut.label);

        JumpInsnNode rewrittenComparison = null;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof JumpInsnNode jump && jump.label == notAquatic) {
                rewrittenComparison = jump;
                break;
            }
        }
        assertTrue(rewrittenComparison != null, "The original false-path branch must remain present");
        assertEquals(Opcodes.IFEQ, rewrittenComparison.getOpcode());
        assertSame(notAquatic, rewrittenComparison.label);
        assertEquals(2, method.maxStack);
    }


    @Test
    void rewritesStringConcatFactoryBootstrapStringArgumentExactly() throws Exception {
        String className = "sample/DormantSampleRenderer";
        String resourceLocation = "net/minecraft/resources/ResourceLocation";
        String oldConstructor = "(Ljava/lang/String;Ljava/lang/String;)V";
        String newFactory = "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;";
        String bootstrapDescriptor = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                + "Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)"
                + "Ljava/lang/invoke/CallSite;";
        String concatConstant = "block/dormant_sample_" + (char) 1;
        String escapedConcatConstant = "block/dormant_sample_" + (char) 92 + "u0001";

        String profile = """
                {
                  "schemaVersion": 1,
                  "id": "synthetic-string-concat-bootstrap-profile",
                  "rules": [{
                    "className": "sample/DormantSampleRenderer",
                    "methodName": "makeLocation",
                    "methodDescriptor": "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;",
                    "operations": [{
                      "type": "instruction-sequence",
                      "expectedMatches": 1,
                      "minimumMaxStack": 2,
                      "match": [
                        {"kind":"type","opcode":"NEW","typeName":"net/minecraft/resources/ResourceLocation"},
                        {"kind":"insn","opcode":"DUP"},
                        {"kind":"ldc","valueType":"string","value":"symbiote"},
                        {"kind":"var","opcode":"ALOAD","var":0},
                        {
                          "kind":"invokedynamic",
                          "name":"makeConcatWithConstants",
                          "descriptor":"(Ljava/lang/String;)Ljava/lang/String;",
                          "bootstrap":{
                            "tag":"H_INVOKESTATIC",
                            "owner":"java/lang/invoke/StringConcatFactory",
                            "name":"makeConcatWithConstants",
                            "descriptor":"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
                          },
                          "bootstrapArguments":[
                            {"kind":"string","value":"CONCAT_CONSTANT"}
                          ]
                        },
                        {
                          "kind":"method",
                          "opcode":"INVOKESPECIAL",
                          "owner":"net/minecraft/resources/ResourceLocation",
                          "name":"<init>",
                          "descriptor":"(Ljava/lang/String;Ljava/lang/String;)V"
                        }
                      ],
                      "replaceWith": [
                        {"kind":"ldc","valueType":"string","value":"symbiote"},
                        {"kind":"var","opcode":"ALOAD","var":0},
                        {
                          "kind":"invokedynamic",
                          "name":"makeConcatWithConstants",
                          "descriptor":"(Ljava/lang/String;)Ljava/lang/String;",
                          "bootstrap":{
                            "tag":"H_INVOKESTATIC",
                            "owner":"java/lang/invoke/StringConcatFactory",
                            "name":"makeConcatWithConstants",
                            "descriptor":"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;"
                          },
                          "bootstrapArguments":[
                            {"kind":"string","value":"CONCAT_CONSTANT"}
                          ]
                        },
                        {
                          "kind":"method",
                          "opcode":"INVOKESTATIC",
                          "owner":"net/minecraft/resources/ResourceLocation",
                          "name":"m_339182_",
                          "descriptor":"(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"
                        }
                      ]
                    }]
                  }]
                }
                """.replace("CONCAT_CONSTANT", escapedConcatConstant);

        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = className;
        node.superName = "java/lang/Object";

        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "makeLocation", "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;", null, null);
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, resourceLocation));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new LdcInsnNode("symbiote"));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InvokeDynamicInsnNode(
                "makeConcatWithConstants",
                "(Ljava/lang/String;)Ljava/lang/String;",
                new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/StringConcatFactory",
                        "makeConcatWithConstants", bootstrapDescriptor, false),
                concatConstant));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                resourceLocation, "<init>", oldConstructor, false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        method.maxStack = 3;
        method.maxLocals = 1;
        node.methods.add(method);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(writeProfile(profile)));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals("synthetic-string-concat-bootstrap-profile", stats.profileId());
        assertEquals(1, stats.rulesApplied());
        assertEquals(1, stats.operationsApplied());
        assertEquals(Opcodes.LDC, method.instructions.getFirst().getOpcode());
        assertEquals("symbiote", ((LdcInsnNode) method.instructions.getFirst()).cst);
        assertEquals(Opcodes.ALOAD, method.instructions.getFirst().getNext().getOpcode());
        InvokeDynamicInsnNode rewritten = (InvokeDynamicInsnNode) method.instructions.getFirst().getNext().getNext();
        assertEquals("makeConcatWithConstants", rewritten.name);
        assertEquals("(Ljava/lang/String;)Ljava/lang/String;", rewritten.desc);
        assertEquals(1, rewritten.bsmArgs.length);
        assertEquals(concatConstant, rewritten.bsmArgs[0]);
        MethodInsnNode factoryCall = (MethodInsnNode) rewritten.getNext();
        assertEquals(Opcodes.INVOKESTATIC, factoryCall.getOpcode());
        assertEquals(resourceLocation, factoryCall.owner);
        assertEquals("m_339182_", factoryCall.name);
        assertEquals(newFactory, factoryCall.desc);
    }

    @Test
    void rewritesLambdaMetafactoryBootstrapAndClassLiteralExactly() throws Exception {
        String className = "sample/ClientModEvents";
        String oldFactory = "sample/OldFactory";
        String newFactory = "sample/ConfigFactory";
        String oldScreenType = "sample/Minecraft";
        String containerType = "sample/Container";
        String screenType = "sample/Screen";
        String screenDescriptor = "(L" + screenType + ";)L" + screenType + ";";
        String oldImplementationDescriptor = "(L" + oldScreenType + ";L" + screenType + ";)L"
                + screenType + ";";
        String newImplementationDescriptor = "(L" + containerType + ";L" + screenType + ";)L"
                + screenType + ";";
        String bootstrapDescriptor = "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;"
                + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;"
                + "Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                + "Ljava/lang/invoke/CallSite;";
        String oldSamDescriptor = "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
        String newCallSiteDescriptor = "()L" + newFactory + ";";

        String profile = """
                {
                  "schemaVersion": 1,
                  "id": "synthetic-lambda-metafactory-profile",
                  "rules": [
                    {
                      "className": "sample/ClientModEvents",
                      "methodName": "makeFactory",
                      "methodDescriptor": "()Lsample/OldFactory;",
                      "operations": [
                        {
                          "type": "instruction-sequence",
                          "expectedMatches": 1,
                          "match": [
                            {"kind":"type","opcode":"NEW","typeName":"sample/OldFactory"},
                            {"kind":"insn","opcode":"DUP"},
                            {
                              "kind":"invokedynamic",
                              "name":"apply",
                              "descriptor":"()Ljava/util/function/BiFunction;",
                              "bootstrap":{
                                "tag":"H_INVOKESTATIC",
                                "owner":"java/lang/invoke/LambdaMetafactory",
                                "name":"metafactory",
                                "descriptor":"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
                              },
                              "bootstrapArguments":[
                                {"kind":"method-type","descriptor":"(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"},
                                {"kind":"handle","tag":"H_INVOKESTATIC","owner":"sample/ClientModEvents","name":"screenFactory","descriptor":"(Lsample/Minecraft;Lsample/Screen;)Lsample/Screen;"},
                                {"kind":"method-type","descriptor":"(Lsample/Minecraft;Lsample/Screen;)Lsample/Screen;"}
                              ]
                            },
                            {
                              "kind":"method",
                              "opcode":"INVOKESPECIAL",
                              "owner":"sample/OldFactory",
                              "name":"<init>",
                              "descriptor":"(Ljava/util/function/BiFunction;)V"
                            }
                          ],
                          "replaceWith": [
                            {
                              "kind":"invokedynamic",
                              "name":"createScreen",
                              "descriptor":"()Lsample/ConfigFactory;",
                              "bootstrap":{
                                "tag":"H_INVOKESTATIC",
                                "owner":"java/lang/invoke/LambdaMetafactory",
                                "name":"metafactory",
                                "descriptor":"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;"
                              },
                              "bootstrapArguments":[
                                {"kind":"method-type","descriptor":"(Lsample/Container;Lsample/Screen;)Lsample/Screen;"},
                                {"kind":"handle","tag":"H_INVOKESTATIC","owner":"sample/ClientModEvents","name":"screenFactory","descriptor":"(Lsample/Container;Lsample/Screen;)Lsample/Screen;"},
                                {"kind":"method-type","descriptor":"(Lsample/Container;Lsample/Screen;)Lsample/Screen;"}
                              ]
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "className": "sample/ClientModEvents",
                      "methodName": "screenFactory",
                      "methodDescriptor": "(Lsample/Minecraft;Lsample/Screen;)Lsample/Screen;",
                      "operations": [
                        {
                          "type":"method-declaration-descriptor",
                          "descriptor":"(Lsample/Minecraft;Lsample/Screen;)Lsample/Screen;",
                          "replacementDescriptor":"(Lsample/Container;Lsample/Screen;)Lsample/Screen;",
                          "expectedMatches":1
                        }
                      ]
                    },
                    {
                      "className": "sample/ClientModEvents",
                      "methodName": "register",
                      "methodDescriptor": "()V",
                      "operations": [
                        {
                          "type":"instruction-sequence",
                          "expectedMatches":1,
                          "match":[{"kind":"class-literal","typeName":"sample/OldFactory"}],
                          "replaceWith":[{"kind":"class-literal","typeName":"sample/ConfigFactory"}]
                        }
                      ]
                    }
                  ]
                }
                """;

        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = className;
        node.superName = "java/lang/Object";

        MethodNode factory = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "makeFactory", "()L" + oldFactory + ";", null, null);
        factory.instructions.add(new TypeInsnNode(Opcodes.NEW, oldFactory));
        factory.instructions.add(new InsnNode(Opcodes.DUP));
        factory.instructions.add(new InvokeDynamicInsnNode(
                "apply",
                "()Ljava/util/function/BiFunction;",
                new Handle(Opcodes.H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory",
                        "metafactory", bootstrapDescriptor, false),
                Type.getMethodType(oldSamDescriptor),
                new Handle(Opcodes.H_INVOKESTATIC, className, "screenFactory",
                        oldImplementationDescriptor, false),
                Type.getMethodType(oldImplementationDescriptor)));
        factory.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, oldFactory, "<init>",
                "(Ljava/util/function/BiFunction;)V", false));
        factory.instructions.add(new InsnNode(Opcodes.ARETURN));
        factory.maxStack = 3;
        node.methods.add(factory);

        MethodNode screenFactory = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "screenFactory", oldImplementationDescriptor, null, null);
        screenFactory.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        screenFactory.instructions.add(new InsnNode(Opcodes.ARETURN));
        screenFactory.maxStack = 1;
        screenFactory.maxLocals = 2;
        node.methods.add(screenFactory);

        MethodNode register = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "register", "()V", null, null);
        register.instructions.add(new LdcInsnNode(Type.getObjectType(oldFactory)));
        register.instructions.add(new InsnNode(Opcodes.POP));
        register.instructions.add(new InsnNode(Opcodes.RETURN));
        register.maxStack = 1;
        node.methods.add(register);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(writeProfile(profile)));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals("synthetic-lambda-metafactory-profile", stats.profileId());
        assertEquals(3, stats.rulesApplied());
        assertEquals(3, stats.operationsApplied());
        assertEquals(newImplementationDescriptor, screenFactory.desc);

        InvokeDynamicInsnNode rewritten = (InvokeDynamicInsnNode) factory.instructions.getFirst();
        assertEquals("createScreen", rewritten.name);
        assertEquals(newCallSiteDescriptor, rewritten.desc);
        assertEquals(3, rewritten.bsmArgs.length);
        assertEquals(Type.getMethodType(newImplementationDescriptor), rewritten.bsmArgs[0]);
        Handle implementation = (Handle) rewritten.bsmArgs[1];
        assertEquals(className, implementation.getOwner());
        assertEquals("screenFactory", implementation.getName());
        assertEquals(newImplementationDescriptor, implementation.getDesc());
        assertEquals(Type.getMethodType(newImplementationDescriptor), rewritten.bsmArgs[2]);
        assertEquals("sample/ConfigFactory",
                ((Type) ((LdcInsnNode) register.instructions.getFirst()).cst).getInternalName());
    }

    @Test
    void adaptsClientTickPostAndClassSubscriberAnnotationExactly() throws Exception {
        String className = "sample/SculkGlowClient";
        String oldEvent = "net/minecraftforge/event/TickEvent$ClientTickEvent";
        String oldPhase = "net/minecraftforge/event/TickEvent$Phase";
        String newEvent = "net/neoforged/neoforge/client/event/ClientTickEvent$Post";
        String oldAnnotation = "Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;";
        String newAnnotation = "Lnet/neoforged/fml/common/EventBusSubscriber;";
        String oldEventDescriptor = "L" + oldEvent + ";";
        String newEventDescriptor = "L" + newEvent + ";";
        String oldMethodDescriptor = "(" + oldEventDescriptor + ")V";
        String newMethodDescriptor = "(" + newEventDescriptor + ")V";
        String phaseDescriptor = "L" + oldPhase + ";";

        String profile = """
                {
                  "schemaVersion": 1,
                  "id": "synthetic-sculk-glow-clienttick-post-profile",
                  "rules": [{
                    "className": "sample/SculkGlowClient",
                    "methodName": "onClientTick",
                    "methodDescriptor": "(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V",
                    "operations": [
                      {
                        "type": "method-declaration-descriptor",
                        "descriptor": "(Lnet/minecraftforge/event/TickEvent$ClientTickEvent;)V",
                        "replacementDescriptor": "(Lnet/neoforged/neoforge/client/event/ClientTickEvent$Post;)V",
                        "expectedMatches": 1
                      },
                      {
                        "type": "local-variable",
                        "localIndex": 0,
                        "name": "event",
                        "descriptor": "Lnet/minecraftforge/event/TickEvent$ClientTickEvent;",
                        "replacementDescriptor": "Lnet/neoforged/neoforge/client/event/ClientTickEvent$Post;",
                        "replacementName": "event",
                        "expectedMatches": 1
                      },
                      {
                        "type": "instruction-sequence",
                        "expectedMatches": 1,
                        "match": [
                          {"kind":"var","opcode":"ALOAD","var":0},
                          {
                            "kind":"field",
                            "opcode":"GETFIELD",
                            "owner":"net/minecraftforge/event/TickEvent$ClientTickEvent",
                            "name":"phase",
                            "descriptor":"Lnet/minecraftforge/event/TickEvent$Phase;"
                          },
                          {
                            "kind":"field",
                            "opcode":"GETSTATIC",
                            "owner":"net/minecraftforge/event/TickEvent$Phase",
                            "name":"END",
                            "descriptor":"Lnet/minecraftforge/event/TickEvent$Phase;"
                          }
                        ],
                        "replaceWith": [
                          {"kind":"insn","opcode":"ACONST_NULL"},
                          {"kind":"insn","opcode":"ACONST_NULL"}
                        ]
                      },
                      {
                        "type": "class-annotation-descriptor",
                        "descriptor": "Lnet/minecraftforge/fml/common/Mod$EventBusSubscriber;",
                        "replacementDescriptor": "Lnet/neoforged/fml/common/EventBusSubscriber;",
                        "expectedMatches": 1
                      },
                      {
                        "type": "class-inner-class-remove",
                        "innerClassName": "net/minecraftforge/fml/common/Mod$EventBusSubscriber",
                        "outerClassName": "net/minecraftforge/fml/common/Mod",
                        "innerName": "EventBusSubscriber",
                        "expectedMatches": 1
                      }
                    ]
                  }]
                }
                """;

        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = className;
        node.superName = "java/lang/Object";
        node.innerClasses = new ArrayList<>(List.of(new InnerClassNode(
                "net/minecraftforge/fml/common/Mod$EventBusSubscriber",
                "net/minecraftforge/fml/common/Mod",
                "EventBusSubscriber",
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC)));

        AnnotationNode subscriber = new AnnotationNode(oldAnnotation);
        subscriber.values = new ArrayList<>();
        subscriber.values.add("modid");
        subscriber.values.add("symbiote");
        subscriber.values.add("value");
        List<String[]> distValues = new ArrayList<>();
        distValues.add(new String[] {"Lnet/minecraftforge/api/distmarker/Dist;", "CLIENT"});
        subscriber.values.add(distValues);
        node.visibleAnnotations = new ArrayList<>(List.of(subscriber));

        MethodNode method = new MethodNode(
                Opcodes.ASM9,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "onClientTick",
                oldMethodDescriptor,
                null,
                null);
        LabelNode start = new LabelNode();
        LabelNode skip = new LabelNode();
        LabelNode continuation = new LabelNode();
        LabelNode end = new LabelNode();
        method.instructions.add(start);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new FieldInsnNode(
                Opcodes.GETFIELD, oldEvent, "phase", phaseDescriptor));
        method.instructions.add(new FieldInsnNode(
                Opcodes.GETSTATIC, oldPhase, "END", phaseDescriptor));
        method.instructions.add(new JumpInsnNode(Opcodes.IF_ACMPNE, skip));
        method.instructions.add(new FieldInsnNode(
                Opcodes.GETSTATIC, className, "LIT", "Ljava/util/Map;"));
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEINTERFACE, "java/util/Map", "isEmpty", "()Z", true));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, continuation));
        method.instructions.add(skip);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(continuation);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.instructions.add(end);
        method.localVariables = new ArrayList<>(List.of(
                new LocalVariableNode("event", oldEventDescriptor, null, start, end, 0)));
        method.maxStack = 2;
        method.maxLocals = 1;
        node.methods.add(method);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(writeProfile(profile)));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals("synthetic-sculk-glow-clienttick-post-profile", stats.profileId());
        assertEquals(1, stats.rulesApplied());
        assertEquals(5, stats.operationsApplied());
        assertEquals(newAnnotation, subscriber.desc);
        assertTrue(node.innerClasses.isEmpty());
        assertEquals(newMethodDescriptor, method.desc);
        assertEquals(newEventDescriptor, method.localVariables.get(0).desc);

        AbstractInsnNode first = start.getNext();
        assertEquals(Opcodes.ACONST_NULL, first.getOpcode());
        AbstractInsnNode second = first.getNext();
        assertEquals(Opcodes.ACONST_NULL, second.getOpcode());
        AbstractInsnNode branch = second.getNext();
        assertTrue(branch instanceof JumpInsnNode);
        assertEquals(Opcodes.IF_ACMPNE, branch.getOpcode());
        assertSame(skip, ((JumpInsnNode) branch).label);

        int stalePhaseReferences = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && ((field.owner.equals(oldEvent) && field.name.equals("phase"))
                    || (field.owner.equals(oldPhase) && field.name.equals("END")))) {
                stalePhaseReferences++;
            }
        }
        assertEquals(0, stalePhaseReferences);
    }

    @Test
    void rewritesSymbioteArmSlotItemStackCallsToRegistryAwareCodec() throws Exception {
        String packet = "com/scout/symbiote/network/ClientboundSymbioteSyncPacket";
        String friendlyBuffer = "net/minecraft/network/FriendlyByteBuf";
        String itemStack = "net/minecraft/world/item/ItemStack";
        String codec = "org/kylepitcock/legacyconnector/v1_20_1/bridge/ItemStackCodec1201";
        String legacyWrite = "(L" + itemStack + ";)L" + friendlyBuffer + ";";
        String codecWrite = "(L" + friendlyBuffer + ";L" + itemStack + ";)L" + friendlyBuffer + ";";
        String legacyRead = "()L" + itemStack + ";";
        String codecRead = "(L" + friendlyBuffer + ";)L" + itemStack + ";";

        Path profileFile = writeProfile("""
                {
                  "schemaVersion": 1,
                  "id": "symbiote-itemstack-registry-codec",
                  "rules": [
                    {
                      "className": "com/scout/symbiote/network/ClientboundSymbioteSyncPacket",
                      "methodName": "encode",
                      "methodDescriptor": "(Lcom/scout/symbiote/network/ClientboundSymbioteSyncPacket;Lnet/minecraft/network/FriendlyByteBuf;)V",
                      "operations": [{
                        "type": "instruction-sequence",
                        "match": [{
                          "kind": "method",
                          "opcode": "INVOKEVIRTUAL",
                          "owner": "net/minecraft/network/FriendlyByteBuf",
                          "name": "m_130055_",
                          "descriptor": "(Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/FriendlyByteBuf;"
                        }],
                        "replaceWith": [{
                          "kind": "method",
                          "opcode": "INVOKESTATIC",
                          "owner": "org/kylepitcock/legacyconnector/v1_20_1/bridge/ItemStackCodec1201",
                          "name": "writeOptional",
                          "descriptor": "(Lnet/minecraft/network/FriendlyByteBuf;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/network/FriendlyByteBuf;"
                        }],
                        "expectedMatches": 1
                      }]
                    },
                    {
                      "className": "com/scout/symbiote/network/ClientboundSymbioteSyncPacket",
                      "methodName": "decode",
                      "methodDescriptor": "(Lnet/minecraft/network/FriendlyByteBuf;)Lcom/scout/symbiote/network/ClientboundSymbioteSyncPacket;",
                      "operations": [{
                        "type": "instruction-sequence",
                        "match": [{
                          "kind": "method",
                          "opcode": "INVOKEVIRTUAL",
                          "owner": "net/minecraft/network/FriendlyByteBuf",
                          "name": "m_130267_",
                          "descriptor": "()Lnet/minecraft/world/item/ItemStack;"
                        }],
                        "replaceWith": [{
                          "kind": "method",
                          "opcode": "INVOKESTATIC",
                          "owner": "org/kylepitcock/legacyconnector/v1_20_1/bridge/ItemStackCodec1201",
                          "name": "readOptional",
                          "descriptor": "(Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/world/item/ItemStack;"
                        }],
                        "expectedMatches": 1
                      }]
                    }
                  ]
                }
                """);

        ClassNode node = new ClassNode();
        node.version = Opcodes.V17;
        node.access = Opcodes.ACC_PUBLIC;
        node.name = packet;
        node.superName = "java/lang/Object";

        MethodNode encode = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "encode",
                "(Lcom/scout/symbiote/network/ClientboundSymbioteSyncPacket;Lnet/minecraft/network/FriendlyByteBuf;)V",
                null, null);
        encode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        encode.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        encode.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, friendlyBuffer, "m_130055_", legacyWrite, false));
        encode.instructions.add(new InsnNode(Opcodes.POP));
        encode.instructions.add(new InsnNode(Opcodes.RETURN));
        encode.maxStack = 2;
        encode.maxLocals = 2;
        node.methods.add(encode);

        MethodNode decode = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "decode",
                "(Lnet/minecraft/network/FriendlyByteBuf;)Lcom/scout/symbiote/network/ClientboundSymbioteSyncPacket;",
                null, null);
        decode.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        decode.instructions.add(new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, friendlyBuffer, "m_130267_", legacyRead, false));
        decode.instructions.add(new InsnNode(Opcodes.POP));
        decode.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        decode.instructions.add(new InsnNode(Opcodes.ARETURN));
        decode.maxStack = 1;
        decode.maxLocals = 1;
        node.methods.add(decode);

        Forge1201CallSiteAdapter.Session session = new Forge1201CallSiteAdapter.Session(
                Forge1201CallSiteAdapter.load(profileFile));
        session.apply(node);
        Forge1201CallSiteAdapter.Stats stats = session.finish();

        assertEquals("symbiote-itemstack-registry-codec", stats.profileId());
        assertEquals(2, stats.rulesApplied());
        assertEquals(2, stats.operationsApplied());
        assertEquals(0, countCalls(encode, friendlyBuffer, "m_130055_", legacyWrite));
        assertEquals(1, countCalls(encode, codec, "writeOptional", codecWrite));
        assertEquals(0, countCalls(decode, friendlyBuffer, "m_130267_", legacyRead));
        assertEquals(1, countCalls(decode, codec, "readOptional", codecRead));

        MethodInsnNode write = (MethodInsnNode) java.util.stream.StreamSupport
                .stream(java.util.Spliterators.spliteratorUnknownSize(
                        encode.instructions.iterator(), 0), false)
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(codec) && call.name.equals("writeOptional"))
                .findFirst()
                .orElseThrow();
        assertEquals(Opcodes.INVOKESTATIC, write.getOpcode());

        MethodInsnNode read = (MethodInsnNode) java.util.stream.StreamSupport
                .stream(java.util.Spliterators.spliteratorUnknownSize(
                        decode.instructions.iterator(), 0), false)
                .filter(instruction -> instruction instanceof MethodInsnNode call
                        && call.owner.equals(codec) && call.name.equals("readOptional"))
                .findFirst()
                .orElseThrow();
        assertEquals(Opcodes.INVOKESTATIC, read.getOpcode());
    }

    private Path writeProfile(String json) throws IOException {
        Path file = tempDir.resolve("adapter-profile.json");
        Files.writeString(file, json, StandardCharsets.UTF_8);
        return file;
    }


    private static boolean containsCall(MethodNode method, String owner, String name) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)) return true;
        }
        return false;
    }

    private static boolean containsCall(
            MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(descriptor)) return true;
        }
        return false;
    }

    private static boolean containsField(
            MethodNode method, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == Opcodes.GETSTATIC
                    && field.owner.equals(owner)
                    && field.name.equals(name)
                    && field.desc.equals(descriptor)) return true;
        }
        return false;
    }

    private static boolean containsFrame(MethodNode method, int type) {
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof FrameNode frame && frame.type == type) return true;
        }
        return false;
    }

    private static MethodNode createSizeMethod() {
        MethodNode method = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "apply", "(L" + EVENT + ";)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, EVENT, "getNewSize",
                "()" + DIMENSIONS_DESC, false));
        method.instructions.add(new VarInsnNode(Opcodes.ASTORE, 1));

        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, DIMENSIONS, "width", "F"));
        method.instructions.add(new LdcInsnNode(1.35F));
        method.instructions.add(new InsnNode(Opcodes.FMUL));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, DIMENSIONS, "height", "F"));
        method.instructions.add(new LdcInsnNode(1.35F));
        method.instructions.add(new InsnNode(Opcodes.FMUL));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, DIMENSIONS, "scalable",
                "(FF)" + DIMENSIONS_DESC, false));
        method.instructions.add(new VarInsnNode(Opcodes.ASTORE, 2));

        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, EVENT, "setNewSize",
                "(" + DIMENSIONS_DESC + ")V", false));

        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, EVENT, "getNewEyeHeight", "()F", false));
        method.instructions.add(new LdcInsnNode(1.35F));
        method.instructions.add(new InsnNode(Opcodes.FMUL));
        method.instructions.add(new VarInsnNode(Opcodes.FSTORE, 3));

        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new LdcInsnNode(0.05F));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 2));
        method.instructions.add(new FieldInsnNode(Opcodes.GETFIELD, DIMENSIONS, "height", "F"));
        method.instructions.add(new LdcInsnNode(0.05F));
        method.instructions.add(new InsnNode(Opcodes.FSUB));
        method.instructions.add(new VarInsnNode(Opcodes.FLOAD, 3));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "min", "(FF)F", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/Math", "max", "(FF)F", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, EVENT, "setNewEyeHeight",
                "(F)V", false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxLocals = 4;
        method.maxStack = 4;
        return method;
    }

    private static int countCalls(MethodNode method, String owner, String name, String descriptor) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner)
                    && call.name.equals(name) && call.desc.equals(descriptor)) count++;
        }
        return count;
    }

    private static int countVars(MethodNode method, int opcode, int slot) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions) {
            if (instruction instanceof VarInsnNode variable && variable.getOpcode() == opcode
                    && variable.var == slot) count++;
        }
        return count;
    }
}
