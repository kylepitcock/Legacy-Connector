package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
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

        node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "ABILITY",
                "Ldev/latvian/mods/kubejs/registry/RegistryInfo;", null, null));
        node.fields.add(new FieldNode(ACC_PUBLIC | ACC_STATIC, "CONDITION",
                "Ldev/latvian/mods/kubejs/registry/RegistryInfo;", null, null));

        MethodNode init = new MethodNode(ACC_PUBLIC, "init", "()V", null, null);
        init.instructions.add(new InvokeDynamicInsnNode("accept", "()Ljava/util/function/Consumer;",
                new Handle(H_INVOKESTATIC, "java/lang/invoke/LambdaMetafactory", "metafactory",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                                + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                                + "Ljava/lang/invoke/CallSite;", false),
                org.objectweb.asm.Type.getMethodType("(Ljava/lang/Object;)V"),
                new Handle(H_INVOKESTATIC, node.name, "lambda$init$0", "(Ljava/lang/Object;)V", false),
                org.objectweb.asm.Type.getMethodType("(Ljava/lang/Object;)V")));
        init.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        node.methods.add(init);
        node.methods.add(returningMethod(ACC_PUBLIC, "clientInit", "()V"));
        node.methods.add(returningMethod(ACC_PUBLIC, "registerBindings", "(" + BINDINGS_EVENT + ")V"));
        node.methods.add(returningMethod(ACC_PUBLIC, "registerTypeWrappers", OLD_WRAPPERS));

        MethodNode clinit = new MethodNode(ACC_STATIC, "<clinit>", "()V", null, null);
        clinit.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        node.methods.add(clinit);
        return node;
    }

    private static ClassNode builderNode(String name) {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = name;
        node.superName = "dev/latvian/mods/kubejs/registry/BuilderBase";
        return node;
    }

    private static MethodNode returningReferenceMethod(int access, String name, String descriptor) {
        MethodNode method = new MethodNode(access, name, descriptor, null, null);
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(ACONST_NULL));
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(ARETURN));
        return method;
    }

    private static boolean hasMethod(ClassNode node, String name, String descriptor) {
        return node.methods.stream().anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor));
    }

    private static MethodNode returningMethod(int access, String name, String descriptor) {
        MethodNode method = new MethodNode(access, name, descriptor, null, null);
        method.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        return method;
    }

    @Test
    void pluginIsConvertedToKubeJs7InterfaceAndKeepsItsPublicHooks() {
        ClassNode node = pluginNode();

        assertEquals(8, PalladiumKubeJS1201.apply(node));

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

        MethodNode builderTypes = find(node, "registerBuilderTypes",
                "(Ldev/latvian/mods/kubejs/registry/BuilderTypeRegistry;)V");
        assertTrue(hasCall(builderTypes, INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumKubeJSCompat",
                "registerBuilderTypes", "(Ljava/lang/Object;)V"));

        MethodNode events = find(node, "registerEvents",
                "(Ldev/latvian/mods/kubejs/event/EventGroupRegistry;)V");
        assertTrue(hasCall(events, INVOKEINTERFACE, "dev/latvian/mods/kubejs/event/EventGroupRegistry",
                "register", "(Ldev/latvian/mods/kubejs/event/EventGroup;)V"));

        MethodNode init = find(node, "init", "()V");
        assertEquals(1, countDynamicCalls(init), "the CustomProjectile and Palladium property listeners must remain");
        assertFalse(node.fields.stream().anyMatch(field -> field.name.equals("ABILITY") || field.name.equals("CONDITION")));
        MethodNode clinit = find(node, "<clinit>", "()V");
        assertEquals(1, clinit.instructions.size(), "the old RegistryInfo static initializer must be removed");

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
    void obsoleteKubeJs6RegistryInfoOverridesAreRemovedFromAbilityAndConditionBuilders() {
        String registryInfo = "()Ldev/latvian/mods/kubejs/registry/RegistryInfo;";

        String abilityName = "net/threetag/palladium/compat/kubejs/ability/AbilityBuilder";
        String abilityType = "L" + abilityName + ";";
        ClassNode ability = builderNode(abilityName);
        ability.methods.add(returningReferenceMethod(ACC_PUBLIC, "getRegistryType", registryInfo));
        ability.methods.add(returningReferenceMethod(ACC_PUBLIC, "icon",
                "(Lnet/threetag/palladium/util/icon/IIcon;)" + abilityType));
        ability.methods.add(returningReferenceMethod(ACC_PUBLIC, "addProperty",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)" + abilityType));
        ability.methods.add(returningReferenceMethod(ACC_PUBLIC, "firstTick",
                "(L" + abilityName + "$TickFunction;)" + abilityType));
        ability.methods.add(returningReferenceMethod(ACC_PUBLIC, "documentationDescription",
                "(Ljava/lang/String;)" + abilityType));

        assertEquals(1, PalladiumKubeJS1201.apply(ability));
        assertFalse(hasMethod(ability, "getRegistryType", registryInfo));
        assertTrue(hasMethod(ability, "icon", "(Lnet/threetag/palladium/util/icon/IIcon;)" + abilityType));
        assertTrue(hasMethod(ability, "addProperty",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)" + abilityType));
        assertTrue(hasMethod(ability, "firstTick", "(L" + abilityName + "$TickFunction;)" + abilityType));
        assertTrue(hasMethod(ability, "documentationDescription", "(Ljava/lang/String;)" + abilityType));

        String conditionName = "net/threetag/palladium/compat/kubejs/condition/ConditionBuilder";
        String conditionType = "L" + conditionName + ";";
        ClassNode condition = builderNode(conditionName);
        condition.methods.add(returningReferenceMethod(ACC_PUBLIC, "getRegistryType", registryInfo));
        condition.methods.add(returningReferenceMethod(ACC_PUBLIC, "test",
                "(L" + conditionName + "$TestFunction;)" + conditionType));
        condition.methods.add(returningReferenceMethod(ACC_PUBLIC, "addProperty",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)" + conditionType));
        condition.methods.add(returningReferenceMethod(ACC_PUBLIC, "documentationDescription",
                "(Ljava/lang/String;)" + conditionType));

        assertEquals(1, PalladiumKubeJS1201.apply(condition));
        assertFalse(hasMethod(condition, "getRegistryType", registryInfo));
        assertTrue(hasMethod(condition, "test", "(L" + conditionName + "$TestFunction;)" + conditionType));
        assertTrue(hasMethod(condition, "addProperty",
                "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/Object;Ljava/lang/String;)" + conditionType));
        assertTrue(hasMethod(condition, "documentationDescription", "(Ljava/lang/String;)" + conditionType));
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
        MethodNode namespaceFactory = new MethodNode(ACC_PRIVATE | ACC_STATIC,
                "lambda$loadFromDirectory$0", "(Ljava/lang/String;)Lcom/mojang/datafixers/util/Pair;", null, null);
        namespaceFactory.instructions.add(new InvokeDynamicInsnNode(
                "makeConcatWithConstants",
                "(Ljava/lang/String;)Ljava/lang/String;",
                new Handle(H_INVOKESTATIC, "java/lang/invoke/StringConcatFactory", "makeConcatWithConstants",
                        "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                                + "Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;",
                        false),
                "\u0001_addon_scripts"));
        namespaceFactory.instructions.add(new org.objectweb.asm.tree.InsnNode(POP));
        namespaceFactory.instructions.add(new org.objectweb.asm.tree.InsnNode(ACONST_NULL));
        namespaceFactory.instructions.add(new org.objectweb.asm.tree.InsnNode(ARETURN));
        node.methods.add(namespaceFactory);
        node.methods.add(returningMethod(ACC_PRIVATE | ACC_STATIC, "lambda$loadFromDirectory$1", "()V"));
        node.methods.add(returningMethod(ACC_PRIVATE | ACC_STATIC, "lambda$loadFromDirectory$2", "()V"));
        node.methods.add(returningMethod(ACC_PRIVATE | ACC_STATIC, "lambda$unrelated$0", "()V"));

        assertEquals(4, PalladiumKubeJS1201.apply(node));

        MethodNode rewritten = find(node, "loadFromDirectory", "(" + CALLBACK_INFO + ")V");
        assertTrue(hasCall(rewritten, INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumKubeJSCompat",
                "loadAddonScripts", "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V"));
        assertTrue(hasLdc(rewritten, ""));
        assertTrue(hasLdc(rewritten, "_addon_scripts"));
        assertEquals(2, node.methods.size(), "only injection and unrelated method should remain");
        assertTrue(find(node, "lambda$unrelated$0", "()V") != null);

        ClassWriter writer = new ClassWriter(0);
        node.accept(writer);
        assertTrue(writer.toByteArray().length > 0, "rewritten mixin should serialize as a class file");
    }

    @Test
    void kubeJs6EventWrappersBecomeKubeJs7EventsAndCommonPostsSpecifyTheirScriptType() {
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = "net/threetag/palladium/compat/kubejs/TestEventJS";
        node.superName = "dev/latvian/mods/kubejs/event/EventJS";

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.superName, "<init>", "()V", false));
        constructor.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        node.methods.add(constructor);

        MethodNode cancelMethod = new MethodNode(ACC_PUBLIC, "cancelFromScript",
                "(Ldev/latvian/mods/rhino/Context;)Ljava/lang/Object;", null, null);
        cancelMethod.instructions.add(new VarInsnNode(ALOAD, 0));
        cancelMethod.instructions.add(new VarInsnNode(ALOAD, 1));
        cancelMethod.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "dev/latvian/mods/kubejs/event/EventJS", "cancel",
                "(Ldev/latvian/mods/rhino/Context;)Ljava/lang/Object;", false));
        cancelMethod.instructions.add(new org.objectweb.asm.tree.InsnNode(ARETURN));
        node.methods.add(cancelMethod);

        MethodNode post = new MethodNode(ACC_PUBLIC | ACC_STATIC, "post", "()V", null, null);
        post.instructions.add(new FieldInsnNode(GETSTATIC,
                "net/threetag/palladium/compat/kubejs/PalladiumJSEvents", "CUSTOM_PROJECTILE_TICK",
                "Ldev/latvian/mods/kubejs/event/EventHandler;"));
        post.instructions.add(new org.objectweb.asm.tree.TypeInsnNode(NEW, node.name));
        post.instructions.add(new org.objectweb.asm.tree.InsnNode(DUP));
        post.instructions.add(new MethodInsnNode(INVOKESPECIAL, node.name, "<init>", "()V", false));
        post.instructions.add(new MethodInsnNode(INVOKEVIRTUAL,
                "dev/latvian/mods/kubejs/event/EventHandler", "post",
                "(Ldev/latvian/mods/kubejs/event/EventJS;)Ldev/latvian/mods/kubejs/event/EventResult;", false));
        post.instructions.add(new org.objectweb.asm.tree.InsnNode(POP));
        post.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        node.methods.add(post);

        assertTrue(PalladiumKubeJS1201.apply(node) > 0);
        assertEquals("java/lang/Object", node.superName);
        assertTrue(node.interfaces.contains("dev/latvian/mods/kubejs/event/KubeEvent"));

        MethodNode rewrittenConstructor = find(node, "<init>", "()V");
        assertTrue(hasCall(rewrittenConstructor, INVOKESPECIAL, "java/lang/Object", "<init>", "()V"));
        MethodNode rewrittenCancelMethod = find(node, "cancelFromScript",
                "(Ldev/latvian/mods/rhino/Context;)Ljava/lang/Object;");
        assertTrue(hasCall(rewrittenCancelMethod, INVOKEINTERFACE, "dev/latvian/mods/kubejs/event/KubeEvent",
                "cancel", "(Ldev/latvian/mods/rhino/Context;)Ljava/lang/Object;"));
        MethodNode rewrittenPost = find(node, "post", "()V");
        assertTrue(hasCall(rewrittenPost, INVOKESTATIC, "legacyconnector/v1_20_1/PalladiumKubeJSCompat",
                "postEvent", "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;"));
        assertTrue(hasField(rewrittenPost, GETSTATIC, "dev/latvian/mods/kubejs/script/ScriptType",
                "SERVER", "Ldev/latvian/mods/kubejs/script/ScriptType;"));
    }

    @Test
    void pluginDiscoveryResourceIsAddedOnlyForPalladiumAndPreservesExistingEntries() {
        java.util.Map<String, byte[]> resources = new java.util.LinkedHashMap<>();
        resources.put("kubejs.plugins.txt", "example.Plugin\n".getBytes(java.nio.charset.StandardCharsets.UTF_8));
        java.util.Set<String> classEntries = java.util.Set.of(PalladiumKubeJS1201.PLUGIN + ".class");

        assertTrue(PalladiumKubeJS1201.addPluginDiscoveryResource(resources, classEntries));
        String content = new String(resources.get("kubejs.plugins.txt"), java.nio.charset.StandardCharsets.UTF_8);
        assertTrue(content.contains("example.Plugin"));
        assertTrue(content.contains("net.threetag.palladium.compat.kubejs.PalladiumKubeJSPlugin"));
        assertFalse(PalladiumKubeJS1201.addPluginDiscoveryResource(resources, classEntries));

        java.util.Map<String, byte[]> otherResources = new java.util.LinkedHashMap<>();
        assertFalse(PalladiumKubeJS1201.addPluginDiscoveryResource(otherResources, java.util.Set.of("some/Other.class")));
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

    private static boolean hasField(MethodNode method, int opcode, String owner, String name, String descriptor) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof FieldInsnNode field
                    && field.getOpcode() == opcode
                    && field.owner.equals(owner)
                    && field.name.equals(name)
                    && field.desc.equals(descriptor)) return true;
        }
        return false;
    }

    private static boolean hasLdc(MethodNode method, Object value) {
        for (AbstractInsnNode instruction : method.instructions.toArray()) {
            if (instruction instanceof org.objectweb.asm.tree.LdcInsnNode ldc && value.equals(ldc.cst)) return true;
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

    @Test
    void kubeJs6LevelEventSubclassBecomesKubeJs7LevelEventInterface() {
        String oldLevelEvent = "dev/latvian/mods/kubejs/level/LevelEventJS";
        String newLevelEvent = "dev/latvian/mods/kubejs/level/KubeLevelEvent";
        ClassNode node = new ClassNode();
        node.version = V21;
        node.access = ACC_PUBLIC;
        node.name = "net/threetag/palladium/compat/kubejs/RegisterPalladiumPropertyEventJS";
        node.superName = oldLevelEvent;

        MethodNode constructor = new MethodNode(ACC_PUBLIC, "<init>", "()V", null, null);
        constructor.instructions.add(new VarInsnNode(ALOAD, 0));
        constructor.instructions.add(new MethodInsnNode(INVOKESPECIAL, oldLevelEvent, "<init>", "()V", false));
        constructor.instructions.add(new org.objectweb.asm.tree.InsnNode(RETURN));
        node.methods.add(constructor);

        MethodNode getLevel = new MethodNode(ACC_PUBLIC, "getLevel",
                "()Lnet/minecraft/world/level/Level;", null, null);
        getLevel.instructions.add(new org.objectweb.asm.tree.InsnNode(ACONST_NULL));
        getLevel.instructions.add(new org.objectweb.asm.tree.InsnNode(ARETURN));
        node.methods.add(getLevel);

        assertEquals(1, PalladiumKubeJS1201.apply(node));
        assertEquals("java/lang/Object", node.superName);
        assertTrue(node.interfaces.contains(newLevelEvent),
                "the migrated wrapper must implement KubeJS 7's level-event contract");
        assertTrue(hasCall(find(node, "<init>", "()V"), INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V"));
        assertFalse(hasCall(find(node, "<init>", "()V"), INVOKESPECIAL,
                oldLevelEvent, "<init>", "()V"));
        assertTrue(hasMethod(node, "getLevel", "()Lnet/minecraft/world/level/Level;"));
    }

}
