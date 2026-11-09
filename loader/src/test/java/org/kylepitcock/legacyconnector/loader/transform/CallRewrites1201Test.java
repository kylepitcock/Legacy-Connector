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

/** Regression tests for Minecraft 1.20.1 -> 1.21.1 call-site and field-shape migrations. */
class CallRewrites1201Test {
    private static final String ADDON_PACK_MANAGER = "net/threetag/palladium/addonpack/AddonPackManager";
    private static final String FOLDER_REPOSITORY_SOURCE = "net/minecraft/server/packs/repository/FolderRepositorySource";
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
