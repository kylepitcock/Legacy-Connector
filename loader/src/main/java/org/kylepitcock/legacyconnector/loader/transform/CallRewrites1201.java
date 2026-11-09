package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Signature changes between Forge 1.20.1 / Minecraft 1.20.1 and NeoForge 1.21.1 that a rename cannot express, rewritten at the call site
 * (after the names are already Mojang/NeoForge):
 * <ul>
 *   <li>constructors that became factories ({@code new ResourceLocation("a:b")} to {@code ResourceLocation.parse}): {@code NEW, DUP, args, INVOKESPECIAL <init>}
 *       becomes {@code args, INVOKESTATIC factory};</li>
 *   <li>instance calls that need other semantics ({@code IEventBus.post} returned whether the event was cancelled) to a static helper in
 *       {@link #HELPERS} taking the receiver first;</li>
 *   <li>Minecraft 1.21.1's {@code AmethystClusterBlock} dimensions are floats, so legacy integer arguments are widened at their producers;</li>
 *   <li>Minecraft 1.21.1's {@code ArmorMaterials} constants are {@code Holder<ArmorMaterial>} values, unwrapped at legacy enum-field reads;</li>
 *   <li>Palladium 4.5.x's addon-pack repository constructor is redirected through the game-root symlink validator;</li>
 *   <li>Palladium 4.5.x's synthesized mod-pack metadata declares the active 1.21.1 resource/data pack format.</li>
 * </ul>
 */
final class CallRewrites1201 {
    static final String HELPERS = "legacyconnector/v1_20_1/Compat1201";

    /** owner.&lt;init&gt;desc to {owner, factory name, factory descriptor}. */
    private static final Map<String, String[]> FACTORIES = Map.of(
            "net/minecraft/resources/ResourceLocation.<init>(Ljava/lang/String;)V",
            new String[] {"net/minecraft/resources/ResourceLocation", "parse", "(Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"},
            "net/minecraft/resources/ResourceLocation.<init>(Ljava/lang/String;Ljava/lang/String;)V",
            new String[] {"net/minecraft/resources/ResourceLocation", "fromNamespaceAndPath",
                    "(Ljava/lang/String;Ljava/lang/String;)Lnet/minecraft/resources/ResourceLocation;"});

    private static final String PALLADIUM_ADDON_PACK_MANAGER = "net/threetag/palladium/addonpack/AddonPackManager";
    private static final String PALLADIUM_MOD_RESOURCE_PACK = "net/threetag/palladium/addonpack/forge/AddonPackManagerImpl$ModResourcePack";
    private static final String PALLADIUM_PACK_METADATA_FORMAT = "{\"pack\":{\"id\": \"%s\", \"version\": \"%s\", \"description\":\"%s\"}}";
    private static final String PALLADIUM_PACK_METADATA_FORMAT_1211 = "{\"pack\":{\"id\": \"%s\", \"version\": \"%s\", \"description\":\"%s\", \"pack_format\":%d}}";
    private static final String FOLDER_REPOSITORY_SOURCE = "net/minecraft/server/packs/repository/FolderRepositorySource";
    private static final String AMETHYST_CLUSTER_BLOCK = "net/minecraft/world/level/block/AmethystClusterBlock";
    private static final String AMETHYST_CLUSTER_CONSTRUCTOR_1201 =
            "(IILnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V";
    private static final String AMETHYST_CLUSTER_CONSTRUCTOR_1211 =
            "(FFLnet/minecraft/world/level/block/state/BlockBehaviour$Properties;)V";
    private static final String PALLADIUM_CORE_ENTITY_RENDERER_REGISTRY =
            "net/threetag/palladiumcore/registry/client/forge/EntityRendererRegistryImpl";
    private static final String ENTITY_RENDERERS_ADD_LAYERS =
            "net/neoforged/neoforge/client/event/EntityRenderersEvent$AddLayers";
    private static final String ADD_LAYERS_GET_RENDERER_1201_DESC =
            "(Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/client/renderer/entity/LivingEntityRenderer;";
    private static final String ADD_LAYERS_GET_RENDERER_1211_DESC =
            "(Lnet/minecraft/world/entity/EntityType;)Lnet/minecraft/client/renderer/entity/EntityRenderer;";
    private static final String ATTRIBUTE_MODIFIER_OPERATION =
            "net/minecraft/world/entity/ai/attributes/AttributeModifier$Operation";
    private static final String OPERATION_FROM_VALUE_DESC =
            "(I)Lnet/minecraft/world/entity/ai/attributes/AttributeModifier$Operation;";
    private static final String OPERATION_BY_ID_DESC = "Ljava/util/function/IntFunction;";
    private static final String ARMOR_MATERIALS = "net/minecraft/world/item/ArmorMaterials";
    private static final String ARMOR_MATERIALS_DESC = "Lnet/minecraft/world/item/ArmorMaterials;";
    private static final String HOLDER_DESC = "Lnet/minecraft/core/Holder;";
    private static final String HOLDER = "net/minecraft/core/Holder";
    private static final String ARMOR_MATERIAL = "net/minecraft/world/item/ArmorMaterial";
    private static final java.util.Set<String> VANILLA_ARMOR_MATERIALS = java.util.Set.of(
            "LEATHER", "CHAIN", "IRON", "GOLD", "DIAMOND", "TURTLE", "NETHERITE");

    /** owner.name+desc of an instance call to {helper name, helper descriptor (receiver first)}. */
    private static final Map<String, String[]> STATICS = Map.of(
            "net/neoforged/bus/api/IEventBus.post(Lnet/neoforged/bus/api/Event;)Z",
            new String[] {"post", "(Lnet/neoforged/bus/api/IEventBus;Lnet/neoforged/bus/api/Event;)Z"});

    private CallRewrites1201() {}


    /**
     * Palladium synthesizes pack.mcmeta for mod resource packs that do not ship one. Its
     * 1.20.1 fallback omits pack_format, which Minecraft 1.21.1 requires. Keep the
     * generated pack tied to the active PackType rather than assuming every use is a
     * client resource pack.
     */
    private static int rewritePalladiumPackMetadata(ClassNode node) {
        if (!PALLADIUM_MOD_RESOURCE_PACK.equals(node.name)) return 0;

        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (!(instruction instanceof LdcInsnNode format)
                        || !PALLADIUM_PACK_METADATA_FORMAT.equals(format.cst)) {
                    continue;
                }

                AbstractInsnNode arraySize = nextRealInstruction(instruction);
                AbstractInsnNode arrayAllocation = nextRealInstruction(arraySize);
                if (arraySize == null || arraySize.getOpcode() != Opcodes.ICONST_3
                        || !(arrayAllocation instanceof TypeInsnNode arrayType)
                        || arrayAllocation.getOpcode() != Opcodes.ANEWARRAY
                        || !"java/lang/Object".equals(arrayType.desc)) {
                    throw new IllegalStateException("Palladium ModResourcePack pack.mcmeta formatter shape changed");
                }

                AbstractInsnNode lastArrayStore = null;
                MethodInsnNode stringFormat = null;
                for (AbstractInsnNode cursor = arrayAllocation.getNext(); cursor != null; cursor = cursor.getNext()) {
                    if (cursor.getOpcode() == Opcodes.AASTORE) lastArrayStore = cursor;
                    if (cursor instanceof MethodInsnNode call
                            && call.getOpcode() == Opcodes.INVOKESTATIC
                            && call.owner.equals("java/lang/String")
                            && call.name.equals("format")
                            && call.desc.equals("(Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/String;")) {
                        stringFormat = call;
                        break;
                    }
                }
                if (lastArrayStore == null || stringFormat == null) {
                    throw new IllegalStateException("Palladium ModResourcePack pack.mcmeta String.format call was not found");
                }

                method.instructions.set(arraySize, new InsnNode(Opcodes.ICONST_4));
                format.cst = PALLADIUM_PACK_METADATA_FORMAT_1211;

                InsnList packFormatArgument = new InsnList();
                packFormatArgument.add(new InsnNode(Opcodes.DUP));
                packFormatArgument.add(new InsnNode(Opcodes.ICONST_3));
                packFormatArgument.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        PALLADIUM_ADDON_PACK_MANAGER,
                        "getPackType",
                        "()Lnet/minecraft/server/packs/PackType;",
                        false));
                packFormatArgument.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        HELPERS,
                        "packFormat",
                        "(Lnet/minecraft/server/packs/PackType;)I",
                        false));
                packFormatArgument.add(new MethodInsnNode(
                        Opcodes.INVOKESTATIC,
                        "java/lang/Integer",
                        "valueOf",
                        "(I)Ljava/lang/Integer;",
                        false));
                packFormatArgument.add(new InsnNode(Opcodes.AASTORE));
                method.instructions.insert(lastArrayStore, packFormatArgument);
                return 1;
            }
        }
        return 0;
    }

    private static AbstractInsnNode nextRealInstruction(AbstractInsnNode instruction) {
        if (instruction == null) return null;
        AbstractInsnNode next = instruction.getNext();
        while (next != null && next.getOpcode() < 0) next = next.getNext();
        return next;
    }

    /**
     * PalladiumCore 1.0.1 was compiled against the 1.20.1 AddLayers#getRenderer
     * erasure, which returned LivingEntityRenderer. NeoForge 1.21.1 broadens the
     * generic method's erased return type to EntityRenderer. The caller already
     * checks the result with instanceof LivingEntityRenderer before adding a layer,
     * so changing only the call descriptor preserves that compatibility behavior.
     */
    private static int rewritePalladiumCoreRendererLookup(ClassNode node) {
        if (!PALLADIUM_CORE_ENTITY_RENDERER_REGISTRY.equals(node.name)) return 0;

        int rewritten = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null) continue;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || call.getOpcode() != Opcodes.INVOKEVIRTUAL
                        || !ENTITY_RENDERERS_ADD_LAYERS.equals(call.owner)
                        || !"getRenderer".equals(call.name)
                        || !ADD_LAYERS_GET_RENDERER_1201_DESC.equals(call.desc)) {
                    continue;
                }
                call.desc = ADD_LAYERS_GET_RENDERER_1211_DESC;
                rewritten++;
            }
        }
        return rewritten;
    }

    static int apply(ClassNode node) {
        return apply(node, java.util.Set.of());
    }

    /** Anonymous subclasses of NeoForge's now final {@code CompoundIngredient} that only open its constructor (a Forge 1.20.1 idiom). */
    static final String COMPOUND = "net/neoforged/neoforge/common/crafting/CompoundIngredient";

    /** {@code compoundSubclasses}: the mod's classes that extend {@link #COMPOUND}; {@code new Sub(list)} becomes the factory call. */
    /**
     * 1.20.1 accepted integer pixel dimensions for AmethystClusterBlock; 1.21.1
     * keeps the same height/xzOffset values as floats. At each exact legacy call,
     * temporarily store the Properties reference, widen the two now-adjacent ints,
     * and reload Properties. This changes only the call's arguments, not their
     * producers, which might also feed unrelated integer consumers.
     */
    private static int rewriteAmethystClusterConstructors(ClassNode node) {
        int rewritten = 0;
        for (MethodNode method : node.methods) {
            if (method.instructions == null || method.instructions.size() == 0) continue;

            int propertiesLocal = -1;
            for (AbstractInsnNode instruction = method.instructions.getFirst();
                    instruction != null;
                    instruction = instruction.getNext()) {
                if (!(instruction instanceof MethodInsnNode call)
                        || call.getOpcode() != Opcodes.INVOKESPECIAL
                        || !call.owner.equals(AMETHYST_CLUSTER_BLOCK)
                        || !call.name.equals("<init>")
                        || !call.desc.equals(AMETHYST_CLUSTER_CONSTRUCTOR_1201)) {
                    continue;
                }

                if (propertiesLocal < 0) {
                    propertiesLocal = method.maxLocals;
                    method.maxLocals++;
                }

                InsnList conversions = new InsnList();
                conversions.add(new VarInsnNode(Opcodes.ASTORE, propertiesLocal));
                // Convert the second integer first. SWAP brings the first integer
                // to the top without losing the already-converted float; the final
                // SWAP restores the original argument order (height, xzOffset).
                conversions.add(new InsnNode(Opcodes.I2F));
                conversions.add(new InsnNode(Opcodes.SWAP));
                conversions.add(new InsnNode(Opcodes.I2F));
                conversions.add(new InsnNode(Opcodes.SWAP));
                conversions.add(new VarInsnNode(Opcodes.ALOAD, propertiesLocal));
                method.instructions.insertBefore(call, conversions);
                call.desc = AMETHYST_CLUSTER_CONSTRUCTOR_1211;
                rewritten++;
            }
        }
        return rewritten;
    }

    static int apply(ClassNode node, java.util.Set<String> compoundSubclasses) {
        int n = rewritePalladiumPackMetadata(node)
                + rewriteAmethystClusterConstructors(node)
                + rewritePalladiumCoreRendererLookup(node);
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            Deque<TypeInsnNode> news = new ArrayDeque<>();
            AbstractInsnNode next;
            for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = next) {
                // InsnList.set/remove clear the replaced node's links: take the successor first
                next = in.getNext();
                if (in instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode indy && indy.bsmArgs != null) {
                    // ResourceLocation::new method references: the constructor handle becomes the factory handle
                    for (int i = 0; i < indy.bsmArgs.length; i++) {
                        if (indy.bsmArgs[i] instanceof org.objectweb.asm.Handle h && h.getTag() == Opcodes.H_NEWINVOKESPECIAL) {
                            String[] f = FACTORIES.get(h.getOwner() + ".<init>" + h.getDesc());
                            if (f != null) {
                                indy.bsmArgs[i] = new org.objectweb.asm.Handle(Opcodes.H_INVOKESTATIC, f[0], f[1], f[2], false);
                                n++;
                            }
                        }
                    }
                } else if (in instanceof TypeInsnNode t && t.getOpcode() == Opcodes.NEW) {
                    news.push(t);
                } else if (in instanceof FieldInsnNode field
                        && field.getOpcode() == Opcodes.GETSTATIC
                        && field.owner.equals(ARMOR_MATERIALS)
                        && VANILLA_ARMOR_MATERIALS.contains(field.name)
                        && field.desc.equals(ARMOR_MATERIALS_DESC)) {
                    // In 1.20.1 ArmorMaterials was an enum implementing ArmorMaterial.
                    // In 1.21.1 its vanilla constants are Holder<ArmorMaterial>.
                    // Unwrap the holder at the read site to preserve the old value shape.
                    field.desc = HOLDER_DESC;
                    InsnList unwrap = new InsnList();
                    unwrap.add(new MethodInsnNode(
                            Opcodes.INVOKEINTERFACE, HOLDER, "value", "()Ljava/lang/Object;", true));
                    unwrap.add(new TypeInsnNode(Opcodes.CHECKCAST, ARMOR_MATERIAL));
                    m.instructions.insert(field, unwrap);
                    n++;
                } else if (in instanceof MethodInsnNode call) {
                    if (call.getOpcode() == Opcodes.INVOKESPECIAL && call.name.equals("<init>")) {
                        TypeInsnNode made = news.isEmpty() ? null : news.peek();
                        if (made != null && made.desc.equals(call.owner)) {
                            news.pop();
                            if (compoundSubclasses.contains(call.owner) && call.desc.equals("(Ljava/util/List;)V") && made.getNext() != null && made.getNext().getOpcode() == Opcodes.DUP) {
                                m.instructions.remove(made.getNext());
                                m.instructions.remove(made);
                                m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPERS, "compoundIngredient", "(Ljava/util/List;)Lnet/minecraft/world/item/crafting/Ingredient;", false));
                                n++;
                                continue;
                            }
                            String[] f = FACTORIES.get(call.owner + ".<init>" + call.desc);
                            // Palladium's addon-pack directory is a direct child of the game
                            // directory. Its 1.20.1 constructor call is adapted using the
                            // game's standard allowed_symlinks.txt validator. Keep this
                            // specific because another mod may pass a folder with a different
                            // root relationship.
                            boolean palladiumFolderRepository = call.owner.equals(FOLDER_REPOSITORY_SOURCE)
                                    && node.name.equals(PALLADIUM_ADDON_PACK_MANAGER)
                                    && m.name.equals("<init>") && m.desc.equals("()V");
                            if (palladiumFolderRepository) {
                                f = new String[] {HELPERS, "folderRepositorySource",
                                        "(Ljava/nio/file/Path;Lnet/minecraft/server/packs/PackType;Lnet/minecraft/server/packs/repository/PackSource;)Lnet/minecraft/server/packs/repository/FolderRepositorySource;"};
                            } else if (call.owner.equals(FOLDER_REPOSITORY_SOURCE)) {
                                f = null;
                            }
                            if (f != null && made.getNext() != null && made.getNext().getOpcode() == Opcodes.DUP) {
                                m.instructions.remove(made.getNext());
                                m.instructions.remove(made);
                                m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, f[0], f[1], f[2], false));
                                n++;
                            }
                        }
                    } else if (call.getOpcode() == Opcodes.INVOKESTATIC
                            && call.owner.equals(ATTRIBUTE_MODIFIER_OPERATION)
                            && call.name.equals("fromValue")
                            && call.desc.equals(OPERATION_FROM_VALUE_DESC)) {
                        // 1.20.1 exposed Operation.fromValue(int). In 1.21.1 the
                        // equivalent ID lookup is the public IntFunction BY_ID field.
                        // Input stack: [int]. Output stack: [Operation].
                        InsnList lookup = new InsnList();
                        lookup.add(new FieldInsnNode(
                                Opcodes.GETSTATIC,
                                ATTRIBUTE_MODIFIER_OPERATION,
                                "BY_ID",
                                OPERATION_BY_ID_DESC));
                        lookup.add(new InsnNode(Opcodes.SWAP));
                        lookup.add(new MethodInsnNode(
                                Opcodes.INVOKEINTERFACE,
                                "java/util/function/IntFunction",
                                "apply",
                                "(I)Ljava/lang/Object;",
                                true));
                        lookup.add(new TypeInsnNode(Opcodes.CHECKCAST, ATTRIBUTE_MODIFIER_OPERATION));
                        m.instructions.insertBefore(call, lookup);
                        m.instructions.remove(call);
                        n++;
                    } else {
                        String[] h = STATICS.get(call.owner + "." + call.name + call.desc);
                        if (h != null && call.getOpcode() != Opcodes.INVOKESTATIC) {
                            m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, HELPERS, h[0], h[1], false));
                            n++;
                        }
                    }
                }
            }
        }
        return n;
    }
}
