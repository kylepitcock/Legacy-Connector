package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium's 1.20.1-to-1.21.1 addon-pack folder source constructor migration. */
class PalladiumAddonPackManager1201Test implements Opcodes {
    private static final String FOLDER_SOURCE =
            "net/minecraft/server/packs/repository/FolderRepositorySource";
    private static final String LEGACY_DESCRIPTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;)V";
    private static final String TARGET_DESCRIPTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;"
                    + "Lnet/minecraft/world/level/validation/DirectoryValidator;)V";
    private static final String HELPER_DESCRIPTOR =
            "()Lnet/minecraft/world/level/validation/DirectoryValidator;";
    private static final String COMPAT_HELPERS = "legacyconnector/v1_20_1/Compat1201";
    private static final String COMPAT_FACTORY_DESCRIPTOR =
            "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;"
                    + "Lnet/minecraft/server/packs/repository/PackSource;)"
                    + "Lnet/minecraft/server/packs/repository/FolderRepositorySource;";
    private static final String PACK = "net/minecraft/server/packs/repository/Pack";
    private static final String PACK_HELPER = "legacyconnector/v1_20_1/PalladiumAddonPackCompat";
    private static final String PACK_HELPER_DESC = "(L" + PACK + ";)L" + PACK + ";";
    private static final String PACK_CALLBACK_DESC =
            "(Ljava/util/function/Consumer;L" + PACK + ";)V";

    @Test
    void addonPackManagerUsesMinecraftSymlinkValidatorForFolderRepositorySource() {
        ClassNode node = addonPackManagerNode(LEGACY_DESCRIPTOR);

        assertEquals(2, PalladiumAddonPackManager1201.apply(node));

        MethodNode constructor = findConstructor(node);
        MethodNode packCallback = find(node, "lambda$getWrappedPackFinder$0", PACK_CALLBACK_DESC);
        assertNotNull(packCallback);
        assertTrue(hasCall(packCallback, INVOKESTATIC, PACK_HELPER,
                "prefixAndRequireAddonPack", PACK_HELPER_DESC));
        assertTrue(hasCall(packCallback, INVOKEINTERFACE, "java/util/function/Consumer",
                "accept", "(Ljava/lang/Object;)V"));
        assertFalse(hasPackFieldWrite(packCallback, "f_10401_", "Ljava/lang/String;"));
        assertFalse(hasPackFieldWrite(packCallback, "f_10407_", "Z"));
        MethodInsnNode rewritten = findFolderSourceConstructor(constructor);
        assertEquals(TARGET_DESCRIPTOR, rewritten.desc);
        assertTrue(hasCallBefore(constructor, rewritten, INVOKESTATIC,
                "legacyconnector/v1_20_1/PalladiumAddonPackCompat",
                "addonPackDirectoryValidator", HELPER_DESCRIPTOR));
        assertEquals(6, constructor.maxStack, "the validator return value adds one stack slot");
        assertFalse(hasLegacyFolderSourceConstructor(constructor));
    }

    @Test
    void recognizesFactoryCallInsertedByGeneralCallRewriter() {
        ClassNode node = addonPackManagerNodeWithCompatFactory();

        assertEquals(0, PalladiumAddonPackManager1201.apply(node));

        MethodNode packCallback = find(node, "lambda$getWrappedPackFinder$0", PACK_CALLBACK_DESC);
        assertNotNull(packCallback);
        assertTrue(hasCall(packCallback, INVOKESTATIC, PACK_HELPER,
                "prefixAndRequireAddonPack", PACK_HELPER_DESC));

        boolean foundFactory = false;
        for (var instruction : node.methods.get(0).instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == INVOKESTATIC
                    && COMPAT_HELPERS.equals(call.owner)
                    && "folderRepositorySource".equals(call.name)
                    && COMPAT_FACTORY_DESCRIPTOR.equals(call.desc)) {
                foundFactory = true;
            }
        }
        assertTrue(foundFactory, "the already-inserted shared compatibility factory remains intact");
    }

    @Test
    void unrelatedClassesAreNotRewritten() {
        ClassNode node = addonPackManagerNode(LEGACY_DESCRIPTOR);
        node.name = "example/UnrelatedManager";

        assertEquals(0, PalladiumAddonPackManager1201.apply(node));
        assertEquals(LEGACY_DESCRIPTOR, findFolderSourceConstructor(findConstructor(node)).desc);
        assertTrue(hasPackFieldWrite(find(node, "lambda$getWrappedPackFinder$0", PACK_CALLBACK_DESC),
                "f_10401_", "Ljava/lang/String;"));
    }

    @Test
    void alreadyModernFolderRepositorySourceCallIsLeftUnchanged() {
        ClassNode node = addonPackManagerNode(TARGET_DESCRIPTOR);

        assertEquals(1, PalladiumAddonPackManager1201.apply(node));
        assertEquals(TARGET_DESCRIPTOR, findFolderSourceConstructor(findConstructor(node)).desc);
        assertFalse(hasCallBefore(findConstructor(node), findFolderSourceConstructor(findConstructor(node)),
                INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumAddonPackCompat",
                "addonPackDirectoryValidator", HELPER_DESCRIPTOR));
        assertTrue(hasCall(find(node, "lambda$getWrappedPackFinder$0", PACK_CALLBACK_DESC),
                INVOKESTATIC, PACK_HELPER, "prefixAndRequireAddonPack", PACK_HELPER_DESC));
    }

    private static ClassNode addonPackManagerNodeWithCompatFactory() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumAddonPackManager1201.ADDON_PACK_MANAGER;
        node.superName = "java/lang/Object";

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        constructor.instructions.add(new MethodInsnNode(
                INVOKESTATIC, COMPAT_HELPERS, "folderRepositorySource", COMPAT_FACTORY_DESCRIPTOR, false));
        constructor.instructions.add(new InsnNode(POP));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxStack = 3;
        constructor.maxLocals = 1;
        node.methods.add(constructor);
        node.methods.add(packMetadataLambda(true));
        return node;
    }

    private static ClassNode addonPackManagerNode(String constructorDescriptor) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumAddonPackManager1201.ADDON_PACK_MANAGER;
        node.superName = "java/lang/Object";

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false));
        constructor.instructions.add(new TypeInsnNode(NEW, FOLDER_SOURCE));
        constructor.instructions.add(new InsnNode(DUP));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        constructor.instructions.add(new InsnNode(ACONST_NULL));
        if (TARGET_DESCRIPTOR.equals(constructorDescriptor)) {
            constructor.instructions.add(new InsnNode(ACONST_NULL));
        }
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, FOLDER_SOURCE, "<init>", constructorDescriptor, false));
        constructor.instructions.add(new InsnNode(POP));
        constructor.instructions.add(new InsnNode(RETURN));
        constructor.maxStack = TARGET_DESCRIPTOR.equals(constructorDescriptor) ? 6 : 5;
        constructor.maxLocals = 1;
        node.methods.add(constructor);
        node.methods.add(packMetadataLambda(false));
        return node;
    }

    private static MethodNode packMetadataLambda(boolean alreadyAdapted) {
        MethodNode method = new MethodNode(ACC_PRIVATE | ACC_STATIC | ACC_SYNTHETIC,
                "lambda$getWrappedPackFinder$0", PACK_CALLBACK_DESC, null, null);
        if (alreadyAdapted) {
            method.instructions.add(new VarInsnNode(ALOAD, 1));
            method.instructions.add(new MethodInsnNode(INVOKESTATIC, PACK_HELPER,
                    "prefixAndRequireAddonPack", PACK_HELPER_DESC, false));
            method.instructions.add(new VarInsnNode(ASTORE, 1));
        } else {
            method.instructions.add(new VarInsnNode(ALOAD, 1));
            method.instructions.add(new org.objectweb.asm.tree.LdcInsnNode("addonpack:legacy-id"));
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
                    PUTFIELD, PACK, "f_10401_", "Ljava/lang/String;"));
            method.instructions.add(new VarInsnNode(ALOAD, 1));
            method.instructions.add(new InsnNode(ICONST_1));
            method.instructions.add(new org.objectweb.asm.tree.FieldInsnNode(
                    PUTFIELD, PACK, "f_10407_", "Z"));
        }
        method.instructions.add(new VarInsnNode(ALOAD, 0));
        method.instructions.add(new VarInsnNode(ALOAD, 1));
        method.instructions.add(new MethodInsnNode(INVOKEINTERFACE,
                "java/util/function/Consumer", "accept", "(Ljava/lang/Object;)V", true));
        method.instructions.add(new InsnNode(RETURN));
        method.maxStack = 2;
        method.maxLocals = 2;
        return method;
    }

    private static MethodNode findConstructor(ClassNode node) {
        for (MethodNode method : node.methods) if ("<init>".equals(method.name)) return method;
        throw new AssertionError("AddonPackManager constructor missing");
    }

    private static MethodNode find(ClassNode node, String name, String desc) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && desc.equals(method.desc)) return method;
        }
        throw new AssertionError("Missing method " + name + desc);
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && owner.equals(call.owner)
                    && name.equals(call.name)
                    && desc.equals(call.desc)) return true;
        }
        return false;
    }

    private static boolean hasPackFieldWrite(MethodNode method, String name, String desc) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field
                    && field.getOpcode() == PUTFIELD
                    && PACK.equals(field.owner)
                    && name.equals(field.name)
                    && desc.equals(field.desc)) return true;
        }
        return false;
    }

    private static MethodInsnNode findFolderSourceConstructor(MethodNode method) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(FOLDER_SOURCE)
                    && call.name.equals("<init>")) return call;
        }
        throw new AssertionError("FolderRepositorySource constructor call missing");
    }

    private static boolean hasLegacyFolderSourceConstructor(MethodNode method) {
        for (var instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.owner.equals(FOLDER_SOURCE)
                    && call.name.equals("<init>")
                    && call.desc.equals(LEGACY_DESCRIPTOR)) return true;
        }
        return false;
    }

    private static boolean hasCallBefore(
            MethodNode method, MethodInsnNode target, int opcode, String owner, String name, String descriptor) {
        for (var instruction = method.instructions.getFirst(); instruction != null && instruction != target; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(descriptor)) return true;
        }
        return false;
    }
}
