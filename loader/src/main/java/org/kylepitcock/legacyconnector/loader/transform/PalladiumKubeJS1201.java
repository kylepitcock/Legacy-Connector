package org.kylepitcock.legacyconnector.loader.transform;

import java.util.Iterator;
import java.util.List;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
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
 * Palladium 4.5.x shipped a KubeJS 6 integration. Its plugin superclass,
 * binding event, wrapper hook and addon-script file model changed in KubeJS 7.
 *
 * Keep Palladium's public scripting hooks, adapt its plugin class to the KubeJS
 * 7 interface, and replace only the old addon-script injection body with a
 * runtime helper that uses KubeJS 7 ScriptFileInfo / ScriptFile.
 */
final class PalladiumKubeJS1201 {
    static final String PLUGIN = "net/threetag/palladium/compat/kubejs/PalladiumKubeJSPlugin";
    static final String SCRIPT_MANAGER_MIXIN = "net/threetag/palladium/mixin/ScriptManagerMixin";
    static final String SCRIPT_FILE_INFO_MIXIN = "net/threetag/palladium/mixin/ScriptFileInfoMixin";
    static final String ADDON_SCRIPT_FILE_INFO = "net/threetag/palladium/compat/kubejs/AddonPackScriptFileInfo";

    private static final String KUBEJS6_PLUGIN = "dev/latvian/mods/kubejs/KubeJSPlugin";
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

    /** Apply only the two Palladium classes whose KubeJS 6 implementation must change. */
    static int apply(ClassNode node) {
        if (PLUGIN.equals(node.name)) {
            return rewritePlugin(node);
        }
        if (SCRIPT_MANAGER_MIXIN.equals(node.name)) {
            return rewriteScriptManagerMixin(node);
        }
        return 0;
    }

    /** KubeJS 7 owns metadata on ScriptFile, so these Palladium KubeJS 6 classes are obsolete. */
    static boolean dropLegacyKubejsClass(String name) {
        return SCRIPT_FILE_INFO_MIXIN.equals(name) || ADDON_SCRIPT_FILE_INFO.equals(name);
    }

    /** Remove the old accessor mixin from Palladium's config after its class is dropped. */
    static void dropScriptFileInfoMixin(Mixin1201.Stats stats) {
        stats.droppedClasses.add(SCRIPT_FILE_INFO_MIXIN);
    }

    private static int rewritePlugin(ClassNode node) {
        if (!KUBEJS6_PLUGIN.equals(node.superName)) {
            throw new IllegalStateException("Unexpected PalladiumKubeJSPlugin superclass: " + node.superName);
        }

        MethodNode constructor = findMethod(node, "<init>", "()V");
        MethodNode oldBindings = findMethod(node, "registerBindings",
                "(Ldev/latvian/mods/kubejs/script/BindingsEvent;)V");
        MethodNode oldWrappers = findMethod(node, "registerTypeWrappers",
                "(L" + SCRIPT_TYPE + ";Ldev/latvian/mods/rhino/util/wrap/TypeWrappers;)V");
        if (constructor == null || oldBindings == null || oldWrappers == null
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

        node.methods.remove(oldBindings);
        node.methods.add(createRegisterBindings());

        node.methods.remove(oldWrappers);
        node.methods.add(createRegisterTypeWrappers(node.name));

        if (findMethod(node, "afterInit", "()V") != null) {
            throw new IllegalStateException("PalladiumKubeJSPlugin unexpectedly already defines afterInit()");
        }
        node.methods.add(createAfterInit(node.name));
        return 4;
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
