package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Minecraft 1.20.1 -> 1.21.1 call-site and field-shape migrations. */
class CallRewrites1201Test {
    private static final String ADDON_PACK_MANAGER = "net/threetag/palladium/addonpack/AddonPackManager";
    private static final String FOLDER_REPOSITORY_SOURCE = "net/minecraft/server/packs/repository/FolderRepositorySource";
    private static final String AMETHYST_CLUSTER_BLOCK = "net/minecraft/world/level/block/AmethystClusterBlock";
    private static final String AMETHYST_CLUSTER_CONSTRUCTOR_1201 =
            "(IILnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V";
    private static final String AMETHYST_CLUSTER_CONSTRUCTOR_1211 =
            "(FFLnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V";
    private static final String INVENTORY_SCREEN = "net/minecraft/client/gui/screens/inventory/InventoryScreen";
    private static final String RENDER_ENTITY_IN_INVENTORY_1201_DESC =
            "(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;Lorg/joml/Quaternionf;"
                    + "Lnet/minecraft/world/entity/LivingEntity;)V";
    private static final String RENDER_ENTITY_IN_INVENTORY_1211_DESC =
            "(Lnet/minecraft/client/gui/GuiGraphics;FFFLorg/joml/Vector3f;Lorg/joml/Quaternionf;"
                    + "Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V";
    private static final String PALLADIUM_CORE_ENTITY_RENDERER_REGISTRY =
            "net/threetag/palladiumcore/registry/client/forge/EntityRendererRegistryImpl";
    private static final String ADD_LAYERS_EVENT =
            "net/neoforged/neoforge/client/event/EntityRenderersEvent$AddLayers";
    private static final String ADD_LAYERS_GET_RENDERER_1201_DESC =
            "(Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;";
    private static final String ADD_LAYERS_GET_RENDERER_1211_DESC =
            "(Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/client/renderer/entity/EntityRenderer;";
    private static final String FOLDER_CONSTRUCTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/repository/PackSource;)V";
    private static final String FOLDER_HELPER =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/repository/PackSource;)Lnet/minecraft/server/packs/repository/FolderRepositorySource;";
    private static final String ARMOR_MATERIALS = "net/minecraft/world/item/ArmorMaterials";
    private static final String ARMOR_MATERIALS_DESC = "Lnet/minecraft/world/item/ArmorMaterials;";
    private static final String HOLDER_DESC = "Lnet/minecraft/core/Holder;";
    private static final String HOLDER = "net/minecraft/core/Holder";
    private static final String ARMOR_MATERIAL = "net/minecraft/world/item/ArmorMaterial";
    private static final List<String> MATERIALS =
            List.of("LEATHER", "CHAIN", "IRON", "GOLD", "DIAMOND", "TURTLE", "NETHERITE");

    private static ClassNode node(String name, MethodNode method) {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V21;
        node.name = name;
        node.superName = "java/lang/Object";
        node.methods.add(method);
        return node;
    }

    @Test
    void amethystClusterIntegerDimensionsAreWidenedToFloat() {
        String properties = "net/minecraft/world/level/block/state/BlockBehaviour$Properties";
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC, "<init>", "(L" + properties + ";)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        method.instructions.add(new IntInsnNode(Opcodes.BIPUSH, 13));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        MethodInsnNode oldConstructor = new MethodInsnNode(
                Opcodes.INVOKESPECIAL, AMETHYST_CLUSTER_BLOCK, "<init>", AMETHYST_CLUSTER_CONSTRUCTOR_1201, false);
        method.instructions.add(oldConstructor);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 4;
        method.maxLocals = 2;
        ClassNode node = node("example/CrystalClusterSubclass", method);
        node.superName = AMETHYST_CLUSTER_BLOCK;

        assertEquals(1, CallRewrites1201.apply(node));

        AbstractInsnNode[] instructions = method.instructions.toArray();
        assertEquals(Opcodes.ALOAD, instructions[0].getOpcode());
        assertEquals(Opcodes.ICONST_1, instructions[1].getOpcode());
        assertEquals(Opcodes.BIPUSH, instructions[2].getOpcode());
        assertEquals(Opcodes.ALOAD, instructions[3].getOpcode());
        assertEquals(Opcodes.ASTORE, instructions[4].getOpcode());
        assertEquals(2, ((VarInsnNode) instructions[4]).var);
        assertEquals(Opcodes.I2F, instructions[5].getOpcode()); // second dimension: int -> float
        assertEquals(Opcodes.SWAP, instructions[6].getOpcode()); // expose the first int without losing the float
        assertEquals(Opcodes.I2F, instructions[7].getOpcode()); // first dimension: int -> float
        assertEquals(Opcodes.SWAP, instructions[8].getOpcode()); // restore original dimension order
        assertEquals(Opcodes.ALOAD, instructions[9].getOpcode());
        assertEquals(2, ((VarInsnNode) instructions[9]).var);
        assertTrue(instructions[10] instanceof MethodInsnNode);
        MethodInsnNode rewritten = (MethodInsnNode) instructions[10];
        assertEquals(Opcodes.INVOKESPECIAL, rewritten.getOpcode());
        assertEquals(AMETHYST_CLUSTER_BLOCK, rewritten.owner);
        assertEquals(AMETHYST_CLUSTER_CONSTRUCTOR_1211, rewritten.desc);
        // The rewrite updates the descriptor on the existing call node; it does not replace the node.
        assertTrue(method.instructions.contains(oldConstructor));
        assertEquals(Opcodes.RETURN, instructions[11].getOpcode());
        assertEquals(3, method.maxLocals);
    }

    @Test
    void inventoryEntityPreviewUsesFloatCoordinatesAndZeroTranslation() {
        String oldMethodDesc = "(Lnet/minecraft/client/gui/GuiGraphics;IIILorg/joml/Quaternionf;"
                + "Lorg/joml/Quaternionf;Lnet/minecraft/world/entity/LivingEntity;)V";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                "renderPreview", oldMethodDesc, null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 1));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 2));
        method.instructions.add(new VarInsnNode(Opcodes.ILOAD, 3));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 4));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 5));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 6));
        MethodInsnNode oldCall = new MethodInsnNode(Opcodes.INVOKESTATIC, INVENTORY_SCREEN,
                "renderEntityInInventory", RENDER_ENTITY_IN_INVENTORY_1201_DESC, false);
        method.instructions.add(oldCall);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxLocals = 7;
        method.maxStack = 7;
        ClassNode node = node("example/TailoringPreview", method);

        assertEquals(1, CallRewrites1201.apply(node));

        assertEquals(RENDER_ENTITY_IN_INVENTORY_1211_DESC, oldCall.desc);
        assertEquals(Opcodes.INVOKESTATIC, oldCall.getOpcode());
        assertEquals(INVENTORY_SCREEN, oldCall.owner);
        assertTrue(method.instructions.contains(oldCall));
        assertEquals(14, method.maxLocals);

        AbstractInsnNode[] instructions = method.instructions.toArray();
        // The seven original arguments are spilled into fresh locals.
        assertEquals(Opcodes.ASTORE, instructions[0].getOpcode());
        assertEquals(Opcodes.ASTORE, instructions[1].getOpcode());
        assertEquals(Opcodes.ASTORE, instructions[2].getOpcode());
        assertEquals(Opcodes.ISTORE, instructions[3].getOpcode());
        assertEquals(Opcodes.ISTORE, instructions[4].getOpcode());
        assertEquals(Opcodes.ISTORE, instructions[5].getOpcode());
        assertEquals(Opcodes.ASTORE, instructions[6].getOpcode());

        int[] expectedReloadOpcodes = {
                Opcodes.ALOAD, Opcodes.ILOAD, Opcodes.I2F, Opcodes.ILOAD, Opcodes.I2F,
                Opcodes.ILOAD, Opcodes.I2F, Opcodes.NEW, Opcodes.DUP, Opcodes.INVOKESPECIAL,
                Opcodes.ALOAD, Opcodes.ALOAD, Opcodes.ALOAD
        };
        int cursor = 7;
        for (int opcode : expectedReloadOpcodes) {
            assertEquals(opcode, instructions[cursor++].getOpcode());
        }
        MethodInsnNode vectorCtor = (MethodInsnNode) instructions[16];
        assertEquals("org/joml/Vector3f", vectorCtor.owner);
        assertEquals("<init>", vectorCtor.name);
        assertEquals("()V", vectorCtor.desc);
        assertEquals(oldCall, instructions[20]);
        assertEquals(Opcodes.RETURN, instructions[21].getOpcode());
        assertTrue(method.maxStack >= 10);
    }

    @Test
    void palladiumCoreAddLayersLookupUsesTargetEntityRendererReturnType() {
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "renderLayers",
                "(L" + ADD_LAYERS_EVENT + ";Lnet/minecraft/world/entity/EntityType;)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        MethodInsnNode oldLookup = new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, ADD_LAYERS_EVENT, "getRenderer",
                ADD_LAYERS_GET_RENDERER_1201_DESC, false);
        method.instructions.add(oldLookup);
        // PalladiumCore's existing guard handles non-living renderers without casting them.
        method.instructions.add(new TypeInsnNode(
                Opcodes.INSTANCEOF, "net/minecraft/client/renderer/entity/LivingEntityRenderer"));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode node = node(PALLADIUM_CORE_ENTITY_RENDERER_REGISTRY, method);

        assertEquals(1, CallRewrites1201.apply(node));

        AbstractInsnNode[] instructions = method.instructions.toArray();
        assertEquals(Opcodes.ALOAD, instructions[0].getOpcode());
        assertEquals(Opcodes.ALOAD, instructions[1].getOpcode());
        assertTrue(instructions[2] instanceof MethodInsnNode);
        MethodInsnNode rewritten = (MethodInsnNode) instructions[2];
        assertEquals(Opcodes.INVOKEVIRTUAL, rewritten.getOpcode());
        assertEquals(ADD_LAYERS_EVENT, rewritten.owner);
        assertEquals("getRenderer", rewritten.name);
        assertEquals(ADD_LAYERS_GET_RENDERER_1211_DESC, rewritten.desc);
        assertTrue(method.instructions.contains(oldLookup));
        assertEquals(Opcodes.INSTANCEOF, instructions[3].getOpcode());
        assertEquals(Opcodes.POP, instructions[4].getOpcode());
        assertEquals(Opcodes.RETURN, instructions[5].getOpcode());
    }

    @Test
    void palladiumCoreRendererLookupRewriteIsScopedToItsRegistryClass() {
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "lookup",
                "(L" + ADD_LAYERS_EVENT + ";Lnet/minecraft/world/entity/EntityType;)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        MethodInsnNode lookup = new MethodInsnNode(
                Opcodes.INVOKEVIRTUAL, ADD_LAYERS_EVENT, "getRenderer",
                ADD_LAYERS_GET_RENDERER_1201_DESC, false);
        method.instructions.add(lookup);
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode node = node("example/OtherRendererRegistry", method);

        assertEquals(0, CallRewrites1201.apply(node));
        assertEquals(ADD_LAYERS_GET_RENDERER_1201_DESC, lookup.desc);
        assertTrue(method.instructions.contains(lookup));
    }

    @Test
    void vanillaArmorMaterialConstantsUnwrapTheirTargetHolders() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "readMaterials", "()V", null, null);
        for (String material : MATERIALS) {
            method.instructions.add(new FieldInsnNode(
                    Opcodes.GETSTATIC, ARMOR_MATERIALS, material, ARMOR_MATERIALS_DESC));
            method.instructions.add(new InsnNode(Opcodes.POP));
        }
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode node = node("example/ArmorMaterialReads", method);

        assertEquals(7, CallRewrites1201.apply(node));

        AbstractInsnNode instruction = method.instructions.getFirst();
        for (String material : MATERIALS) {
            assertTrue(instruction instanceof FieldInsnNode, material);
            FieldInsnNode field = (FieldInsnNode) instruction;
            assertEquals(Opcodes.GETSTATIC, field.getOpcode());
            assertEquals(ARMOR_MATERIALS, field.owner);
            assertEquals(material, field.name);
            assertEquals(HOLDER_DESC, field.desc);

            instruction = instruction.getNext();
            assertTrue(instruction instanceof MethodInsnNode, material);
            MethodInsnNode unwrap = (MethodInsnNode) instruction;
            assertEquals(Opcodes.INVOKEINTERFACE, unwrap.getOpcode());
            assertEquals(HOLDER, unwrap.owner);
            assertEquals("value", unwrap.name);
            assertEquals("()Ljava/lang/Object;", unwrap.desc);
            assertTrue(unwrap.itf);

            instruction = instruction.getNext();
            assertTrue(instruction instanceof TypeInsnNode, material);
            TypeInsnNode cast = (TypeInsnNode) instruction;
            assertEquals(Opcodes.CHECKCAST, cast.getOpcode());
            assertEquals(ARMOR_MATERIAL, cast.desc);

            instruction = instruction.getNext();
            assertTrue(instruction instanceof InsnNode);
            assertEquals(Opcodes.POP, instruction.getOpcode());
            instruction = instruction.getNext();
        }
        assertEquals(Opcodes.RETURN, instruction.getOpcode());
        assertEquals(null, instruction.getNext());
    }

    @Test
    void attributeModifierOperationFromValueUsesTargetByIdLookup() {
        String operation = "net/minecraft/world/entity/ai/attributes/AttributeModifier$Operation";
        String fromValueDescriptor = "(I)Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;";
        MethodNode method = new MethodNode(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "readOperation", "()V", null, null);
        method.instructions.add(new InsnNode(Opcodes.ICONST_1));
        MethodInsnNode oldLookup = new MethodInsnNode(
                Opcodes.INVOKESTATIC, operation, "fromValue", fromValueDescriptor, false);
        method.instructions.add(oldLookup);
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode node = node("example/AttributeOperationRead", method);

        assertEquals(1, CallRewrites1201.apply(node));

        AbstractInsnNode[] instructions = method.instructions.toArray();
        assertEquals(Opcodes.ICONST_1, instructions[0].getOpcode());
        assertTrue(instructions[1] instanceof FieldInsnNode);
        FieldInsnNode byId = (FieldInsnNode) instructions[1];
        assertEquals(Opcodes.GETSTATIC, byId.getOpcode());
        assertEquals(operation, byId.owner);
        assertEquals("BY_ID", byId.name);
        assertEquals("Ljava/util/function/IntFunction;", byId.desc);

        assertEquals(Opcodes.SWAP, instructions[2].getOpcode());
        assertTrue(instructions[3] instanceof MethodInsnNode);
        MethodInsnNode apply = (MethodInsnNode) instructions[3];
        assertEquals(Opcodes.INVOKEINTERFACE, apply.getOpcode());
        assertEquals("java/util/function/IntFunction", apply.owner);
        assertEquals("apply", apply.name);
        assertEquals("(I)Ljava/lang/Object;", apply.desc);
        assertTrue(apply.itf);

        assertTrue(instructions[4] instanceof TypeInsnNode);
        TypeInsnNode cast = (TypeInsnNode) instructions[4];
        assertEquals(Opcodes.CHECKCAST, cast.getOpcode());
        assertEquals(operation, cast.desc);
        assertEquals(Opcodes.POP, instructions[5].getOpcode());
        assertEquals(Opcodes.RETURN, instructions[6].getOpcode());
        assertFalse(method.instructions.contains(oldLookup));
    }

    @Test
    void unrelatedArmorMaterialsFieldsRemainUntouched() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "other", "()V", null, null);
        method.instructions.add(new FieldInsnNode(
                Opcodes.GETSTATIC, ARMOR_MATERIALS, "UNKNOWN", ARMOR_MATERIALS_DESC));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode node = node("example/Other", method);

        assertEquals(0, CallRewrites1201.apply(node));
        FieldInsnNode field = (FieldInsnNode) method.instructions.getFirst();
        assertEquals("UNKNOWN", field.name);
        assertEquals(ARMOR_MATERIALS_DESC, field.desc);
    }

    @Test
    void palladiumAddonPackFolderConstructorUsesGameSymlinkValidatorHelper() {
        MethodNode method = new MethodNode(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        method.instructions.add(new TypeInsnNode(Opcodes.NEW, FOLDER_REPOSITORY_SOURCE));
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL)); // folder Path
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL)); // PackType
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL)); // PackSource
        method.instructions.add(new MethodInsnNode(
                Opcodes.INVOKESPECIAL, FOLDER_REPOSITORY_SOURCE, "<init>", FOLDER_CONSTRUCTOR, false));
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode node = node(ADDON_PACK_MANAGER, method);

        assertEquals(1, CallRewrites1201.apply(node));

        List<AbstractInsnNode> instructions = new ArrayList<>();
        for (AbstractInsnNode in = method.instructions.getFirst(); in != null; in = in.getNext()) {
            instructions.add(in);
        }
        assertFalse(instructions.stream().anyMatch(in ->
                in instanceof TypeInsnNode type && type.getOpcode() == Opcodes.NEW
                        && type.desc.equals(FOLDER_REPOSITORY_SOURCE)));
        assertFalse(instructions.stream().anyMatch(in ->
                in instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL
                        && call.owner.equals(FOLDER_REPOSITORY_SOURCE) && call.name.equals("<init>")
                        && call.desc.equals(FOLDER_CONSTRUCTOR)));

        MethodInsnNode helper = instructions.stream()
                .filter(MethodInsnNode.class::isInstance)
                .map(MethodInsnNode.class::cast)
                .filter(call -> call.name.equals("folderRepositorySource"))
                .findFirst()
                .orElseThrow();
        assertEquals(Opcodes.INVOKESTATIC, helper.getOpcode());
        assertEquals(CallRewrites1201.HELPERS, helper.owner);
        assertEquals(FOLDER_HELPER, helper.desc);
    }

    @Test
    void folderRepositoryConstructorRewriteIsScopedToPalladiumAddonPackManager() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "create", "()V", null, null);
        TypeInsnNode allocation = new TypeInsnNode(Opcodes.NEW, FOLDER_REPOSITORY_SOURCE);
        method.instructions.add(allocation);
        method.instructions.add(new InsnNode(Opcodes.DUP));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
        MethodInsnNode constructor = new MethodInsnNode(
                Opcodes.INVOKESPECIAL, FOLDER_REPOSITORY_SOURCE, "<init>", FOLDER_CONSTRUCTOR, false);
        method.instructions.add(constructor);
        method.instructions.add(new InsnNode(Opcodes.POP));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        ClassNode node = node("example/OtherPackManager", method);

        assertEquals(0, CallRewrites1201.apply(node));
        assertEquals(allocation, method.instructions.getFirst());
        assertTrue(method.instructions.contains(constructor));
    }

    @Test
    void palladiumGeneratedPackMetadataDeclaresTheActivePackFormat() {
        String palladiumResourcePack =
                "net/threetag/palladium/addonpack/forge/AddonPackManagerImpl$ModResourcePack";
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "getRootResource",
                "([Ljava/lang/String;)Lnet/minecraft/server/packs/resources/IoSupplier;", null, null);
        method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(
                "{\"pack\":{\"id\": \"%s\", \"version\": \"%s\", \"description\":\"%s\"}}"));
        method.instructions.add(new InsnNode(Opcodes.ICONST_3));
        method.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        for (int i = 0; i < 3; i++) {
            method.instructions.add(new InsnNode(Opcodes.DUP));
            method.instructions.add(new InsnNode(Opcodes.ICONST_0 + i));
            method.instructions.add(new InsnNode(Opcodes.ACONST_NULL));
            method.instructions.add(new InsnNode(Opcodes.AASTORE));
        }
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "java/lang/String", "format",
                "(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;", false));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        ClassNode node = node(palladiumResourcePack, method);

        assertEquals(1, CallRewrites1201.apply(node));

        LdcInsnNode format = null;
        InsnNode newArraySize = null;
        int arrayStores = 0;
        MethodInsnNode packType = null;
        MethodInsnNode packFormat = null;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof LdcInsnNode ldc && ldc.cst instanceof String value
                    && value.contains("\"pack_format\":%d")) format = ldc;
            if (instruction instanceof InsnNode insn && insn.getOpcode() == Opcodes.ICONST_4) newArraySize = insn;
            if (instruction.getOpcode() == Opcodes.AASTORE) arrayStores++;
            if (instruction instanceof MethodInsnNode call && call.name.equals("getPackType")) packType = call;
            if (instruction instanceof MethodInsnNode call && call.name.equals("packFormat")) packFormat = call;
        }
        assertTrue(format != null, "pack.mcmeta fallback must include pack_format");
        assertTrue(newArraySize != null, "formatter argument array must grow to four entries");
        assertEquals(4, arrayStores);
        assertTrue(packType != null);
        assertEquals("net/threetag/palladium/addonpack/AddonPackManager", packType.owner);
        assertTrue(packFormat != null);
        assertEquals(CallRewrites1201.HELPERS, packFormat.owner);
        assertEquals("(Lnet/minecraft/server/packs/PackType;)I", packFormat.desc);
    }

    @Test
    void palladiumPackMetadataRewriteIsScopedToItsModResourcePack() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "getRootResource",
                "([Ljava/lang/String;)Lnet/minecraft/server/packs/resources/IoSupplier;", null, null);
        method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode(
                "{\"pack\":{\"id\": \"%s\", \"version\": \"%s\", \"description\":\"%s\"}}"));
        method.instructions.add(new InsnNode(Opcodes.ICONST_3));
        method.instructions.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));
        method.instructions.add(new InsnNode(Opcodes.ARETURN));
        ClassNode node = node("example/ModResourcePack", method);

        assertEquals(0, CallRewrites1201.apply(node));
        LdcInsnNode format = (LdcInsnNode) method.instructions.getFirst();
        assertFalse(((String) format.cst).contains("pack_format"));
    }

}
