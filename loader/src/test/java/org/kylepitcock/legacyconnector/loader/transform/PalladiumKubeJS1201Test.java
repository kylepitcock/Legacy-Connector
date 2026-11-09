package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Regression tests for Palladium's KubeJS 6 plugin/script integration migration to KubeJS 7. */
class PalladiumKubeJS1201Test implements Opcodes {
    private static final String OLD_PLUGIN = "dev/latvian/mods/kubejs/KubeJSPlugin";
    private static final String BINDINGS_EVENT = "Ldev/latvian/mods/kubejs/script/BindingsEvent;";
    private static final String OLD_WRAPPERS =
            "(Ldev/latvian/mods/kubejs/script/ScriptType;Ldev/latvian/mods/rhino/util/wrap/TypeWrappers;)V";
    private static final String CALLBACK_INFO = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";

    private static ClassNode pluginNode() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumKubeJS1201.PLUGIN;
        node.superName = OLD_PLUGIN;

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, OLD_PLUGIN, "<init>", "()V", false));
        constructor.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        node.methods.add(constructor);

        node.methods.add(returningMethod(ACC_PUBLIC, "init", "()V"));
        node.methods.add(returningMethod(ACC_PUBLIC, "clientInit", "()V"));
        node.methods.add(returningMethod(ACC_PUBLIC, "registerBindings", "(" + BINDINGS_EVENT + ")V"));
        node.methods.add(returningMethod(ACC_PUBLIC, "registerTypeWrappers", OLD_WRAPPERS));
        return node;
    }

    private static MethodNode returningMethod(int access, String name, String descriptor) {
        MethodNode method = new MethodNode(access, name, descriptor, null, null);
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        return method;
    }

    @Test
    void pluginIsConvertedToKubeJs7InterfaceAndKeepsItsPublicHooks() {
        ClassNode node = pluginNode();

        assertEquals(4, PalladiumKubeJS1201.apply(node));

        assertEquals("java/lang/Object", node.superName);
        assertTrue(node.interfaces.contains("dev/latvian/mods/kubejs/plugin/KubeJSPlugin"));

        MethodNode constructor = find(node, "<init>", "()V");
        assertTrue(hasCall(constructor, INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
        assertFalse(hasCall(constructor, INVOKESPECIAL, OLD_PLUGIN, "<init>", "()V"));

        MethodNode bindings = find(node, "registerBindings",
                "(Ldev/latvian/mods/kubejs/script/BindingRegistry;)V");
        assertTrue(hasCall(bindings, INVOKEVIRTUAL, "dev/latvian/mods/kubejs/script/BindingRegistry",
                "type", "()Ldev/latvian/mods/kubejs/script/ScriptType;"));
        assertEquals(5, countCalls(bindings, "dev/latvian/mods/kubejs/script/BindingRegistry", "add"));

        MethodNode wrappers = find(node, "registerTypeWrappers",
                "(Ldev/latvian/mods/kubejs/script/TypeWrapperRegistry;)V");
        assertEquals(3, countDynamicCalls(wrappers));
        assertEquals(3, countCalls(wrappers, "dev/latvian/mods/kubejs/script/TypeWrapperRegistry", "register"));

        MethodNode afterInit = find(node, "afterInit", "()V");
        assertTrue(hasCall(afterInit, INVOKESTATIC, "net/neoforged/fml/loading/FMLLoader", "getDist",
                "()Lnet/neoforged/api/distmarker/Dist;"));
        assertTrue(hasCall(afterInit, INVOKEVIRTUAL, "net/neoforged/api/distmarker/Dist", "isClient", "()Z"));
        assertTrue(hasCall(afterInit, INVOKEVIRTUAL, PalladiumKubeJS1201.PLUGIN, "clientInit", "()V"));

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0, "rewritten plugin should serialize as a class file");
    }

    @Test
    void addonScriptMixinDelegatesToTheRuntimeKubeJs7AdapterAndDropsOldLambdaPipeline() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = PalladiumKubeJS1201.SCRIPT_MANAGER_MIXIN;
        node.superName = "java/lang/Object";

        MethodNode injection = returningMethod(ACC_PUBLIC, "loadFromDirectory", "(" + CALLBACK_INFO + ")V");
        node.methods.add(injection);
        node.methods.add(returningMethod(ACC_PRIVATE | ACC_STATIC, "lambda$loadFromDirectory$0",
                "(Ljava/lang/String;)Ljava/lang/Object;"));
        node.methods.add(returningMethod(ACC_PRIVATE | ACC_STATIC, "lambda$loadFromDirectory$1", "()V"));
        node.methods.add(returningMethod(ACC_PRIVATE | ACC_STATIC, "lambda$loadFromDirectory$2", "()V"));
        node.methods.add(returningMethod(ACC_PRIVATE | ACC_STATIC, "lambda$unrelated$0", "()V"));

        assertEquals(4, PalladiumKubeJS1201.apply(node));

        MethodNode rewritten = find(node, "loadFromDirectory", "(" + CALLBACK_INFO + ")V");
        assertTrue(hasCall(rewritten, INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumKubeJSCompat",
                "loadAddonScripts", "(Ljava/lang/Object;)V"));
        assertEquals(2, node.methods.size(), "only injection and unrelated method should remain");
        assertTrue(find(node, "lambda$unrelated$0", "()V") != null);

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0, "rewritten mixin should serialize as a class file");
    }

    @Test
    void obsoleteKubeJs6ScriptMetadataClassesAreRemovedButUnrelatedClassesAreKept() {
        assertTrue(PalladiumKubeJS1201.dropLegacyKubejsClass(PalladiumKubeJS1201.SCRIPT_FILE_INFO_MIXIN));
        assertTrue(PalladiumKubeJS1201.dropLegacyKubejsClass(PalladiumKubeJS1201.ADDON_SCRIPT_FILE_INFO));
        assertFalse(PalladiumKubeJS1201.dropLegacyKubejsClass(PalladiumKubeJS1201.PLUGIN));
        assertFalse(PalladiumKubeJS1201.dropLegacyKubejsClass("net/threetag/palladium/mixin/OtherMixin"));
    }

    private static MethodNode find(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) return method;
        }
        throw new AssertionError("Missing method " + name + descriptor);
    }

    private static boolean hasCall(MethodNode method, int opcode, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == opcode
                    && call.owner.equals(owner)
                    && call.name.equals(name)
                    && call.desc.equals(descriptor)) return true;
        }
        return false;
    }

    private static int countCalls(MethodNode method, String owner, String name) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof MethodInsnNode call && call.owner.equals(owner) && call.name.equals(name)) count++;
        }
        return count;
    }

    private static int countDynamicCalls(MethodNode method) {
        int count = 0;
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof InvokeDynamicInsnNode) count++;
        }
        return count;
    }
}
