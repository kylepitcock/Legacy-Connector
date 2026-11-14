package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.SimpleRemapper;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Palladium 4.5.x shipped a KubeJS 6 integration. Migrate its plugin discovery,
 * builder/event registration, EventJS wrappers, and addon-script loading to
 * KubeJS 7 while keeping optional KubeJS linkage behind the runtime bridge.
 */
final class PalladiumKubeJS1201 {
    static final String PLUGIN = "net/threetag/palladium/compat/kubejs/PalladiumKubeJSPlugin";
    static final String SCRIPT_MANAGER_MIXIN = "net/threetag/palladium/mixin/ScriptManagerMixin";
    static final String SCRIPT_FILE_INFO_MIXIN = "net/threetag/palladium/mixin/ScriptFileInfoMixin";
    static final String ADDON_SCRIPT_FILE_INFO = "net/threetag/palladium/compat/kubejs/AddonPackScriptFileInfo";

    private static final String KUBEJS6_PLUGIN = "dev/latvian/mods/kubejs/KubeJSPlugin";
    private static final String KUBEJS6_EVENT_JS = "dev/latvian/mods/kubejs/event/EventJS";
    private static final String KUBEJS6_LEVEL_EVENT_JS = "dev/latvian/mods/kubejs/level/LevelEventJS";
    private static final String KUBEJS6_ENTITY_EVENT_JS = "dev/latvian/mods/kubejs/entity/EntityEventJS";
    private static final String KUBEJS7_EVENT = "dev/latvian/mods/kubejs/event/KubeEvent";
    private static final String KUBEJS7_LEVEL_EVENT = "dev/latvian/mods/kubejs/level/KubeLevelEvent";
    private static final String KUBEJS7_ENTITY_EVENT = "dev/latvian/mods/kubejs/entity/KubeEntityEvent";
    private static final String KUBEJS7_EVENT_RESULT = "dev/latvian/mods/kubejs/event/EventResult";
    private static final String KUBEJS7_EVENT_HANDLER = "dev/latvian/mods/kubejs/event/EventHandler";
    private static final String KUBEJS7_EVENT_GROUP = "dev/latvian/mods/kubejs/event/EventGroup";
    private static final String KUBEJS7_EVENT_GROUP_REGISTRY = "dev/latvian/mods/kubejs/event/EventGroupRegistry";
    private static final String KUBEJS7_BUILDER_TYPE_REGISTRY = "dev/latvian/mods/kubejs/registry/BuilderTypeRegistry";
    private static final String COMPAT_HELPER = "legacyconnector/v1_20_1/PalladiumKubeJSCompat";
    private static final String KUBEJS7_PLUGIN = "dev/latvian/mods/kubejs/plugin/KubeJSPlugin";
    private static final String BINDING_REGISTRY = "dev/latvian/mods/kubejs/script/BindingRegistry";
    private static final String TYPE_WRAPPER_REGISTRY = "dev/latvian/mods/kubejs/script/TypeWrapperRegistry";
    private static final String DIRECT_TYPE_WRAPPER_FACTORY = "dev/latvian/mods/rhino/util/wrap/DirectTypeWrapperFactory";
    private static final String SCRIPT_TYPE = "dev/latvian/mods/kubejs/script/ScriptType";
    private static final String CALLBACK_INFO = "org/spongepowered/asm/mixin/injection/callback/CallbackInfo";

    private static final Handle LAMBDA_METAFACTORY = new Handle(
            Opcodes.H_INVOKESTATIC,
            "java/lang/invoke/LambdaMetafactory",
            "metafactory",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;"
                    + "Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)"
                    + "Ljava/lang/invoke/CallSite;",
            false);

    private PalladiumKubeJS1201() {
    }

    /** Migrate Palladium's KubeJS 6 plugin, event types, and addon-script mixin. */
    private static final String ABILITY_BUILDER =
            "net/threetag/palladium/compat/kubejs/ability/AbilityBuilder";
    private static final String CONDITION_BUILDER =
            "net/threetag/palladium/compat/kubejs/condition/ConditionBuilder";
    private static final String KUBEJS6_REGISTRY_INFO =
            "Ldev/latvian/mods/kubejs/registry/RegistryInfo;";

    static int apply(ClassNode node) {
        int builderApiChanges = rewriteLegacyBuilderApi(node);
        int eventApiChanges = rewriteLegacyEventApi(node);
        if (PLUGIN.equals(node.name)) {
            return builderApiChanges + eventApiChanges + rewritePlugin(node);
        }
        if (SCRIPT_MANAGER_MIXIN.equals(node.name)) {
            return builderApiChanges + eventApiChanges + rewriteScriptManagerMixin(node);
        }
        return builderApiChanges + eventApiChanges;
    }

    /**
     * KubeJS 7 removed RegistryInfo and no longer asks builders for their registry type:
     * BuilderTypeRegistry supplies the registry key and factory instead. These two
     * obsolete overrides also leave a method descriptor that points at the removed
     * KubeJS 6 class, which can prevent Rhino from discovering the builders' remaining
     * public methods. Remove only the dead overrides; retain every script-facing method.
     */
    private static int rewriteLegacyBuilderApi(ClassNode node) {
        if (!ABILITY_BUILDER.equals(node.name) && !CONDITION_BUILDER.equals(node.name)) {
            return 0;
        }

        MethodNode legacyRegistryGetter = findMethod(
                node, "getRegistryType", "()" + KUBEJS6_REGISTRY_INFO);
        if (legacyRegistryGetter == null) {
            return 0;
        }

        node.methods.remove(legacyRegistryGetter);
        return 1;
    }

    /**
     * KubeJS 7 replaced EventJS (a base class) with KubeEvent (an interface).
     * Rewrite every Palladium event wrapper and its call sites, then pass an
     * explicit script type when posting handlers whose old group was "common".
     */
    private static int rewriteLegacyEventApi(ClassNode node) {
        if (!containsLegacyEventJs(node)) {
            return 0;
        }

        boolean wasEventSubclass = KUBEJS6_EVENT_JS.equals(node.superName);
        boolean wasLevelEventSubclass = KUBEJS6_LEVEL_EVENT_JS.equals(node.superName);
        boolean wasEntityEventSubclass = KUBEJS6_ENTITY_EVENT_JS.equals(node.superName);
        ClassNode migrated = new ClassNode();
        node.accept(new ClassRemapper(migrated, new SimpleRemapper(Map.of(
                KUBEJS6_EVENT_JS, KUBEJS7_EVENT,
                KUBEJS6_LEVEL_EVENT_JS, KUBEJS7_LEVEL_EVENT,
                KUBEJS6_ENTITY_EVENT_JS, KUBEJS7_ENTITY_EVENT))));
        copyClassNode(migrated, node);

        if (wasEventSubclass) {
            node.superName = "java/lang/Object";
            if (!node.interfaces.contains(KUBEJS7_EVENT)) {
                node.interfaces.add(KUBEJS7_EVENT);
            }
        }
        if (wasLevelEventSubclass) {
            // KubeJS 6's LevelEventJS was a class. KubeJS 7 exposes the same
            // level contract as KubeLevelEvent, an interface extending KubeEvent.
            // Palladium's wrapper already implements getLevel(), so it can
            // implement the new interface directly without losing its event API.
            node.superName = "java/lang/Object";
            if (!node.interfaces.contains(KUBEJS7_LEVEL_EVENT)) {
                node.interfaces.add(KUBEJS7_LEVEL_EVENT);
            }
        }
        if (wasEntityEventSubclass) {
            // EntityEventJS was replaced by KubeEntityEvent, which is an interface
            // extending KubeLevelEvent/KubeEvent. ProjectileTickEventJS already
            // supplies getEntity(), so only its superclass and constructor change.
            node.superName = "java/lang/Object";
            if (!node.interfaces.contains(KUBEJS7_ENTITY_EVENT)) {
                node.interfaces.add(KUBEJS7_ENTITY_EVENT);
            }
        }

        // ClassRemapper updates descriptors and owners, but it cannot infer that
        // the old EventJS/LevelEventJS/EntityEventJS classes became interfaces. Normalize
        // invocation opcodes as well as constructor calls to Object.<init>().
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                boolean kubeEventOwner = call.owner.equals(KUBEJS7_EVENT);
                boolean kubeLevelEventOwner = call.owner.equals(KUBEJS7_LEVEL_EVENT);
                boolean kubeEntityEventOwner = call.owner.equals(KUBEJS7_ENTITY_EVENT);
                if (!kubeEventOwner && !kubeLevelEventOwner && !kubeEntityEventOwner) continue;
                if (call.name.equals("<init>")
                        && (wasEventSubclass || wasLevelEventSubclass || wasEntityEventSubclass)) {
                    call.owner = "java/lang/Object";
                    call.itf = false;
                } else if (call.getOpcode() == Opcodes.INVOKEVIRTUAL) {
                    call.setOpcode(Opcodes.INVOKEINTERFACE);
                    call.itf = true;
                } else if (call.getOpcode() == Opcodes.INVOKESPECIAL) {
                    call.itf = true;
                }
            }
        }

        int eventPosts = rewriteEventHandlerPosts(node);
        return 1 + eventPosts;
    }

    private static boolean containsLegacyEventJs(ClassNode node) {
        if (isLegacyEventType(node.superName)
                || containsLegacyEventType(node.signature)) {
            return true;
        }
        for (var field : node.fields) {
            if (containsLegacyEventType(field.desc)
                    || containsLegacyEventType(field.signature)) {
                return true;
            }
        }
        for (MethodNode method : node.methods) {
            if (containsLegacyEventType(method.desc)
                    || containsLegacyEventType(method.signature)) {
                return true;
            }
            if (method.instructions == null) continue;
            for (var instruction : method.instructions) {
                if (instruction instanceof MethodInsnNode call
                        && (isLegacyEventType(call.owner) || containsLegacyEventType(call.desc))) return true;
                if (instruction instanceof org.objectweb.asm.tree.FieldInsnNode field
                        && (isLegacyEventType(field.owner) || containsLegacyEventType(field.desc))) return true;
                if (instruction instanceof org.objectweb.asm.tree.TypeInsnNode type
                        && isLegacyEventType(type.desc)) return true;
                if (instruction instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode dynamic
                        && containsLegacyEventType(dynamic.desc)) return true;
            }
        }
        return false;
    }

    private static boolean isLegacyEventType(String name) {
        return KUBEJS6_EVENT_JS.equals(name)
                || KUBEJS6_LEVEL_EVENT_JS.equals(name)
                || KUBEJS6_ENTITY_EVENT_JS.equals(name);
    }

    private static boolean containsLegacyEventType(String value) {
        return value != null && (value.contains(KUBEJS6_EVENT_JS)
                || value.contains(KUBEJS6_LEVEL_EVENT_JS)
                || value.contains(KUBEJS6_ENTITY_EVENT_JS));
    }

    private static void copyClassNode(ClassNode source, ClassNode target) {
        target.version = source.version;
        target.access = source.access;
        target.name = source.name;
        target.signature = source.signature;
        target.superName = source.superName;
        target.interfaces = source.interfaces;
        target.sourceFile = source.sourceFile;
        target.sourceDebug = source.sourceDebug;
        target.module = source.module;
        target.outerClass = source.outerClass;
        target.outerMethod = source.outerMethod;
        target.outerMethodDesc = source.outerMethodDesc;
        target.visibleAnnotations = source.visibleAnnotations;
        target.invisibleAnnotations = source.invisibleAnnotations;
        target.visibleTypeAnnotations = source.visibleTypeAnnotations;
        target.invisibleTypeAnnotations = source.invisibleTypeAnnotations;
        target.attrs = source.attrs;
        target.innerClasses = source.innerClasses;
        target.nestHostClass = source.nestHostClass;
        target.nestMembers = source.nestMembers;
        target.permittedSubclasses = source.permittedSubclasses;
        target.recordComponents = source.recordComponents;
        target.fields = source.fields;
        target.methods = source.methods;
    }

    private static int rewriteEventHandlerPosts(ClassNode node) {
        int changed = 0;
        String oldPost = "(L" + KUBEJS7_EVENT + ";)L" + KUBEJS7_EVENT_RESULT + ";";
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            FieldInsnNode currentHandler = null;
            for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
                if (instruction instanceof FieldInsnNode field
                        && field.getOpcode() == Opcodes.GETSTATIC
                        && field.desc.equals("L" + KUBEJS7_EVENT_HANDLER + ";")) {
                    currentHandler = field;
                }
                if (!(instruction instanceof MethodInsnNode call)
                        || call.getOpcode() != Opcodes.INVOKEVIRTUAL
                        || !call.owner.equals(KUBEJS7_EVENT_HANDLER)
                        || !call.name.equals("post")
                        || !call.desc.equals(oldPost)
                        || currentHandler == null) {
                    continue;
                }

                String scriptType = eventScriptType(currentHandler.name);
                method.instructions.insertBefore(currentHandler,
                        new FieldInsnNode(Opcodes.GETSTATIC, SCRIPT_TYPE, scriptType, "L" + SCRIPT_TYPE + ";"));
                call.setOpcode(Opcodes.INVOKESTATIC);
                call.owner = COMPAT_HELPER;
                call.name = "postEvent";
                call.desc = "(Ljava/lang/Object;Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;";
                call.itf = false;
                currentHandler = null;
                changed++;
            }
        }
        return changed;
    }

    private static String eventScriptType(String handlerName) {
        String name = handlerName.toUpperCase(java.util.Locale.ROOT);
        return name.contains("CLIENT")
                || name.contains("RENDER")
                || name.contains("ANIMATION")
                || name.contains("OVERLAY")
                ? "CLIENT" : "SERVER";
    }

    /**
     * Add the KubeJS 7 discovery file only when this transformed jar actually
     * contains Palladium's migrated plugin class.
     */
    static boolean addPluginDiscoveryResource(Map<String, byte[]> resources, Set<String> classEntries) {
        if (!classEntries.contains(PLUGIN + ".class")) {
            return false;
        }
        String resource = "kubejs.plugins.txt";
        String pluginName = PLUGIN.replace('/', '.');
        String existing = resources.containsKey(resource)
                ? new String(resources.get(resource), java.nio.charset.StandardCharsets.UTF_8) : "";
        for (String line : existing.split("\\R")) {
            if (line.strip().equals(pluginName)) return false;
        }
        String updated = existing;
        if (!updated.isEmpty() && !updated.endsWith("\n") && !updated.endsWith("\r")) updated += "\n";
        updated += pluginName + "\n";
        resources.put(resource, updated.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return true;
    }

    /** KubeJS 7 owns metadata on ScriptFile, so these Palladium KubeJS 6 classes are obsolete. */
    static boolean dropLegacyKubejsClass(String name) {
        return SCRIPT_FILE_INFO_MIXIN.equals(name) || ADDON_SCRIPT_FILE_INFO.equals(name);
    }

    private static int rewritePlugin(ClassNode node) {
        if (!KUBEJS6_PLUGIN.equals(node.superName)) {
            throw new IllegalStateException("Unexpected PalladiumKubeJSPlugin superclass: " + node.superName);
        }

        MethodNode constructor = findMethod(node, "<init>", "()V");
        MethodNode init = findMethod(node, "init", "()V");
        MethodNode oldBindings = findMethod(node, "registerBindings",
                "(Ldev/latvian/mods/kubejs/script/BindingsEvent;)V");
        MethodNode oldWrappers = findMethod(node, "registerTypeWrappers",
                "(L" + SCRIPT_TYPE + ";Ldev/latvian/mods/rhino/util/wrap/TypeWrappers;)V");
        if (constructor == null || init == null || oldBindings == null || oldWrappers == null
                || findMethod(node, "clientInit", "()V") == null) {
            throw new IllegalStateException("PalladiumKubeJSPlugin bytecode no longer matches the inspected KubeJS 6 shape");
        }

        boolean superCallRewritten = false;
        for (var instruction = constructor.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof MethodInsnNode call
                    && call.getOpcode() == Opcodes.INVOKESPECIAL
                    && call.owner.equals(KUBEJS6_PLUGIN)
                    && call.name.equals("<init>")
                    && call.desc.equals("()V")) {
                call.owner = "java/lang/Object";
                superCallRewritten = true;
            }
        }
        if (!superCallRewritten) {
            throw new IllegalStateException("PalladiumKubeJSPlugin constructor no longer invokes the KubeJS 6 superclass");
        }

        node.superName = "java/lang/Object";
        node.signature = null;
        if (!node.interfaces.contains(KUBEJS7_PLUGIN)) {
            node.interfaces.add(KUBEJS7_PLUGIN);
        }

        // RegistryInfo was removed in KubeJS 7. The old init() builder setup is
        // replaced by registerBuilderTypes(), while its custom event listeners
        // remain and run from init().
        node.fields.removeIf(field -> field.name.equals("ABILITY") || field.name.equals("CONDITION"));
        rewritePluginInitializer(init);
        MethodNode classInitializer = findMethod(node, "<clinit>", "()V");
        if (classInitializer != null) {
            classInitializer.instructions.clear();
            classInitializer.tryCatchBlocks.clear();
            classInitializer.localVariables = null;
            classInitializer.maxStack = 0;
            classInitializer.maxLocals = 0;
            classInitializer.instructions.add(new InsnNode(Opcodes.RETURN));
        }

        node.methods.remove(oldBindings);
        node.methods.add(createRegisterBindings());

        node.methods.remove(oldWrappers);
        node.methods.add(createRegisterTypeWrappers(node.name));
        node.methods.add(createRegisterBuilderTypes());
        node.methods.add(createRegisterEvents());

        if (findMethod(node, "afterInit", "()V") != null) {
            throw new IllegalStateException("PalladiumKubeJSPlugin unexpectedly already defines afterInit()");
        }
        node.methods.add(createAfterInit(node.name));
        return 8;
    }

    private static void rewritePluginInitializer(MethodNode init) {
        InvokeDynamicInsnNode eventConsumer = null;
        for (var instruction = init.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (instruction instanceof InvokeDynamicInsnNode dynamic
                    && dynamic.name.equals("accept")
                    && dynamic.desc.equals("()Ljava/util/function/Consumer;")) {
                eventConsumer = dynamic;
                break;
            }
        }
        if (eventConsumer == null) {
            throw new IllegalStateException("PalladiumKubeJSPlugin.init no longer contains the CustomProjectile event-listener lambda");
        }
        while (init.instructions.getFirst() != eventConsumer) {
            init.instructions.remove(init.instructions.getFirst());
        }
        init.maxStack = Math.max(init.maxStack, 2);
        init.maxLocals = Math.max(init.maxLocals, 1);
    }

    private static MethodNode createRegisterBuilderTypes() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "registerBuilderTypes",
                "(L" + KUBEJS7_BUILDER_TYPE_REGISTRY + ";)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                COMPAT_HELPER, "registerBuilderTypes", "(Ljava/lang/Object;)V", false));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 1;
        method.maxLocals = 2;
        return method;
    }

    private static MethodNode createRegisterEvents() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "registerEvents",
                "(L" + KUBEJS7_EVENT_GROUP_REGISTRY + ";)V", null, null);
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 1));
        method.instructions.add(new FieldInsnNode(Opcodes.GETSTATIC,
                "net/threetag/palladium/compat/kubejs/PalladiumJSEvents", "GROUP",
                "L" + KUBEJS7_EVENT_GROUP + ";"));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE,
                KUBEJS7_EVENT_GROUP_REGISTRY, "register", "(L" + KUBEJS7_EVENT_GROUP + ";)V", true));
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 2;
        method.maxLocals = 2;
        return method;
    }

    private static MethodNode createRegisterBindings() {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "registerBindings",
                "(L" + BINDING_REGISTRY + ";)V", null, null);
        InsnList code = method.instructions;
        LabelNode normalBinding = new LabelNode();
        LabelNode afterClientBindings = new LabelNode();

        // bindings.add("palladium", type == CLIENT ? new PalladiumBindingClient() : new PalladiumBinding())
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new LdcInsnNode("palladium"));
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BINDING_REGISTRY, "type",
                "()L" + SCRIPT_TYPE + ";", false));
        code.add(new FieldInsnNode(Opcodes.GETSTATIC, SCRIPT_TYPE, "CLIENT", "L" + SCRIPT_TYPE + ";"));
        code.add(new JumpInsnNode(Opcodes.IF_ACMPNE, normalBinding));
        code.add(new TypeInsnNode(Opcodes.NEW, "net/threetag/palladium/compat/kubejs/PalladiumBindingClient"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "net/threetag/palladium/compat/kubejs/PalladiumBindingClient", "<init>", "()V", false));
        code.add(new JumpInsnNode(Opcodes.GOTO, afterClientBindings));
        code.add(normalBinding);
        code.add(new TypeInsnNode(Opcodes.NEW, "net/threetag/palladium/compat/kubejs/PalladiumBinding"));
        code.add(new InsnNode(Opcodes.DUP));
        code.add(new MethodInsnNode(Opcodes.INVOKESPECIAL,
                "net/threetag/palladium/compat/kubejs/PalladiumBinding", "<init>", "()V", false));
        code.add(afterClientBindings);
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BINDING_REGISTRY, "add",
                "(Ljava/lang/String;Ljava/lang/Object;)V", false));

        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new LdcInsnNode("superpowerUtil"));
        code.add(new LdcInsnNode(Type.getObjectType("net/threetag/palladium/power/SuperpowerUtil")));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BINDING_REGISTRY, "add",
                "(Ljava/lang/String;Ljava/lang/Object;)V", false));

        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new LdcInsnNode("abilityUtil"));
        code.add(new LdcInsnNode(Type.getObjectType("net/threetag/palladium/power/ability/AbilityUtil")));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BINDING_REGISTRY, "add",
                "(Ljava/lang/String;Ljava/lang/Object;)V", false));

        // Client-only bindings remain client-only; KubeJS 7's BindingRegistry exposes type().
        LabelNode returnLabel = new LabelNode();
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BINDING_REGISTRY, "type",
                "()L" + SCRIPT_TYPE + ";", false));
        code.add(new FieldInsnNode(Opcodes.GETSTATIC, SCRIPT_TYPE, "CLIENT", "L" + SCRIPT_TYPE + ";"));
        code.add(new JumpInsnNode(Opcodes.IF_ACMPNE, returnLabel));

        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new LdcInsnNode("animationUtil"));
        code.add(new LdcInsnNode(Type.getObjectType("net/threetag/palladium/client/model/animation/AnimationUtil")));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BINDING_REGISTRY, "add",
                "(Ljava/lang/String;Ljava/lang/Object;)V", false));

        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new LdcInsnNode("guiUtil"));
        code.add(new LdcInsnNode(Type.getObjectType("net/threetag/palladium/compat/kubejs/GuiUtilJS")));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, BINDING_REGISTRY, "add",
                "(Ljava/lang/String;Ljava/lang/Object;)V", false));

        code.add(returnLabel);
        code.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 4;
        method.maxLocals = 2;
        return method;
    }

    private static MethodNode createRegisterTypeWrappers(String owner) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "registerTypeWrappers",
                "(L" + TYPE_WRAPPER_REGISTRY + ";)V", null, null);
        InsnList code = method.instructions;
        LabelNode returnLabel = new LabelNode();

        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TYPE_WRAPPER_REGISTRY, "scriptType",
                "()L" + SCRIPT_TYPE + ";", false));
        code.add(new FieldInsnNode(Opcodes.GETSTATIC, SCRIPT_TYPE, "CLIENT", "L" + SCRIPT_TYPE + ";"));
        code.add(new JumpInsnNode(Opcodes.IF_ACMPNE, returnLabel));

        addDirectWrapper(code, owner, "net/threetag/palladium/util/Easing",
                "lambda$registerTypeWrappers$4");
        addDirectWrapper(code, owner, "net/threetag/palladium/client/model/animation/PalladiumAnimation$PlayerModelPart",
                "lambda$registerTypeWrappers$5");
        addDirectWrapper(code, owner, "net/threetag/palladium/util/PlayerSlot",
                "lambda$registerTypeWrappers$6");

        code.add(returnLabel);
        code.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 3;
        method.maxLocals = 2;
        return method;
    }

    private static void addDirectWrapper(InsnList code, String owner, String target, String lambdaMethod) {
        String resultDescriptor = "L" + target + ";";
        code.add(new VarInsnNode(Opcodes.ALOAD, 1));
        code.add(new LdcInsnNode(Type.getObjectType(target)));
        code.add(new InvokeDynamicInsnNode(
                "wrap",
                "()L" + DIRECT_TYPE_WRAPPER_FACTORY + ";",
                LAMBDA_METAFACTORY,
                Type.getMethodType("(Ljava/lang/Object;)Ljava/lang/Object;"),
                new Handle(Opcodes.H_INVOKESTATIC, owner, lambdaMethod,
                        "(Ljava/lang/Object;)" + resultDescriptor, false),
                Type.getMethodType("(Ljava/lang/Object;)" + resultDescriptor)));
        code.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, TYPE_WRAPPER_REGISTRY, "register",
                "(Ljava/lang/Class;L" + DIRECT_TYPE_WRAPPER_FACTORY + ";)V", false));
    }

    private static MethodNode createAfterInit(String owner) {
        MethodNode method = new MethodNode(Opcodes.ACC_PUBLIC, "afterInit", "()V", null, null);
        LabelNode returnLabel = new LabelNode();
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "net/neoforged/fml/loading/FMLLoader", "getDist",
                "()Lnet/neoforged/api/distmarker/Dist;", false));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                "net/neoforged/api/distmarker/Dist", "isClient", "()Z", false));
        method.instructions.add(new JumpInsnNode(Opcodes.IFEQ, returnLabel));
        method.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        method.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, owner, "clientInit", "()V", false));
        method.instructions.add(returnLabel);
        method.instructions.add(new InsnNode(Opcodes.RETURN));
        method.maxStack = 1;
        method.maxLocals = 1;
        return method;
    }

    private static int rewriteScriptManagerMixin(ClassNode node) {
        MethodNode injection = findMethod(node, "loadFromDirectory", "(L" + CALLBACK_INFO + ";)V");
        MethodNode namespaceFactory = findMethod(node, "lambda$loadFromDirectory$0", "(Ljava/lang/String;)Lcom/mojang/datafixers/util/Pair;");
        if (injection == null || namespaceFactory == null) {
            throw new IllegalStateException("Palladium ScriptManagerMixin no longer has the inspected KubeJS 6 loadFromDirectory shape");
        }
        String[] namespaceParts = namespaceConcatParts(namespaceFactory);

        injection.instructions.clear();
        injection.tryCatchBlocks.clear();
        injection.localVariables = null;
        injection.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        injection.instructions.add(new LdcInsnNode(namespaceParts[0]));
        injection.instructions.add(new LdcInsnNode(namespaceParts[1]));
        injection.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                "legacyconnector/v1_20_1/PalladiumKubeJSCompat", "loadAddonScripts",
                "(Ljava/lang/Object;Ljava/lang/String;Ljava/lang/String;)V", false));
        injection.instructions.add(new InsnNode(Opcodes.RETURN));
        injection.maxStack = 3;
        injection.maxLocals = 1;

        // All lambda bodies belonged to the replaced KubeJS 6 script-file construction path.
        int removed = 0;
        for (Iterator<MethodNode> iterator = node.methods.iterator(); iterator.hasNext();) {
            MethodNode method = iterator.next();
            if (method.name.startsWith("lambda$loadFromDirectory$")) {
                iterator.remove();
                removed++;
            }
        }
        return 1 + removed;
    }

    /**
     * Palladium builds its addon ScriptPackInfo namespace with StringConcatFactory.
     * Read the original concat recipe before dropping that lambda so the KubeJS 7
     * adapter preserves the exact pack namespace rather than guessing a suffix.
     */
    private static String[] namespaceConcatParts(MethodNode method) {
        for (var instruction = method.instructions.getFirst(); instruction != null; instruction = instruction.getNext()) {
            if (!(instruction instanceof InvokeDynamicInsnNode dynamic)
                    || !dynamic.name.equals("makeConcatWithConstants")
                    || !dynamic.bsm.getOwner().equals("java/lang/invoke/StringConcatFactory")
                    || dynamic.bsmArgs.length == 0
                    || !(dynamic.bsmArgs[0] instanceof String recipe)) {
                continue;
            }
            int placeholder = recipe.indexOf('\u0001');
            if (placeholder < 0 || recipe.indexOf('\u0001', placeholder + 1) >= 0 || recipe.indexOf('\u0002') >= 0) {
                throw new IllegalStateException("Palladium addon script namespace concat recipe changed: " + recipe);
            }
            return new String[] {recipe.substring(0, placeholder), recipe.substring(placeholder + 1)};
        }
        throw new IllegalStateException("Palladium addon script namespace StringConcatFactory recipe was not found");
    }

    private static MethodNode findMethod(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (method.name.equals(name) && method.desc.equals(descriptor)) {
                return method;
            }
        }
        return null;
    }
}
