package org.kylepitcock.legacyconnector.loader.transform;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Hierarchy.Origin;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Tables.Bridge;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Tables.Call;
import org.kylepitcock.legacyconnector.loader.transform.Mc1201Tables.Field;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * Minecraft 1.20.1 to 1.21.1 for one class of a mod (names are already Mojang's): the part of the migration that renaming cannot express.
 * <ol>
 *   <li><b>Calls</b> whose signature, owner semantics or argument types changed go to a static helper ({@code mc-calls.tsv}); constructors that
 *       became factories or take other arguments ({@code new ResourceLocation(String)}, {@code new MobEffectInstance(MobEffect, ...)}) are rewritten
 *       from the {@code NEW, DUP, args, INVOKESPECIAL} pattern to {@code args, INVOKESTATIC};</li>
 *   <li><b>Static fields</b> that changed type or name ({@code MobEffects.X} is a {@code Holder} now) are read in the new form and converted back;</li>
 *   <li><b>Bridge methods</b> for overridden methods whose signature changed ({@code BlockEntity.load(CompoundTag)} to
 *       {@code loadAdditional(CompoundTag, HolderLookup.Provider)}): the class gets the 1.21.1-shaped method, which calls the mod's method, like the
 *       bridge methods javac emits for covariant overrides ({@code mc-bridges.tsv});</li>
 *   <li>Calls of a bridged method that resolve to Minecraft's own implementation ({@code super.saveAdditional(tag)}, {@code blockEntity.load(tag)})
 *       get the extra {@code HolderLookup.Provider} argument from the context the helpers keep.</li>
 * </ol>
 * A call is only touched when no class of the mod declares the method on the way up from its receiver type: the mod's own methods keep their signatures.
 */
final class Mc1201Migration {
    private static final String CONTEXT = Mc1201Tables.HELPER_PACKAGE + "MigrateContext";
    private static final String PROVIDER = "net/minecraft/core/HolderLookup$Provider";
    private static final String HOLDER = "net/minecraft/core/Holder";
    private static final String ABSTRACT_WIDGET = "net/minecraft/client/gui/components/AbstractWidget";
    private static final String LEGACY_WIDGET_NARRATION = "updateNarration";
    private static final String MODERN_WIDGET_NARRATION = "updateWidgetNarration";
    private static final String WIDGET_NARRATION_DESC =
            "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";

    /** What {@link #apply} did, per kind (for the report line). */
    static final class Counts {
        int calls;
        int constructors;
        int fields;
        int bridges;
        int providerCalls;
        int selectionListPositionCalls;
        int widgetNarrationOverrides;

        int total() {
            return calls + constructors + fields + bridges + providerCalls + selectionListPositionCalls
                    + widgetNarrationOverrides;
        }

        void add(Counts o) {
            calls += o.calls;
            constructors += o.constructors;
            fields += o.fields;
            bridges += o.bridges;
            providerCalls += o.providerCalls;
            selectionListPositionCalls += o.selectionListPositionCalls;
            widgetNarrationOverrides += o.widgetNarrationOverrides;
        }

        @Override
        public String toString() {
            return calls + " call(s) redirected to helpers, "
                    + selectionListPositionCalls + " selection-list position call(s) renamed to setX, "
                    + widgetNarrationOverrides + " AbstractWidget narration override(s) renamed to updateWidgetNarration, "
                    + constructors + " constructor(s) made factory calls, "
                    + fields + " static field read(s) converted, "
                    + bridges + " bridge method(s) for changed overrides, "
                    + providerCalls + " call(s) given the registry provider";
        }
    }

    private Mc1201Migration() {}

    static Counts apply(ClassNode node, Mc1201Hierarchy h) {
        return apply(node, h, Mc1201Tables.get());
    }

    static Counts apply(ClassNode node, Mc1201Hierarchy h, Mc1201Tables t) {
        Counts c = new Counts();
        renameWidgetNarrationOverride(node, h, c);
        renameOverrides(node, h, t, c);
        swapMobBucketSuper(node);
        renameBusConstant(node);
        dropEmptySubscriber(node);
        for (MethodNode m : node.methods) {
            if (m.instructions == null || m.instructions.size() == 0) continue;
            rewriteBody(m, h, t, c);
        }
        if ((node.access & Opcodes.ACC_INTERFACE) == 0) {
            addBridges(node, h, t, c);
            addRecipeSerializerCodecs(node, c);
            addTriggerCodec(node, c);
            addConditionCodec(node, c);
        }
        return c;
    }

    /**
     * 1.20.1 widgets override NarratableEntry.updateNarration(output). In 1.21.1
     * AbstractWidget makes updateNarration final and delegates to the new
     * updateWidgetNarration hook. Preserve the mod's implementation under the
     * new hook name instead of removing it or leaving an abstract-method gap.
     */
    private static void renameWidgetNarrationOverride(ClassNode node, Mc1201Hierarchy hierarchy, Counts counts) {
        if (!hierarchy.isSubclassOf(node.name, ABSTRACT_WIDGET)) return;
        MethodNode legacy = declared(node, LEGACY_WIDGET_NARRATION, WIDGET_NARRATION_DESC);
        if (legacy == null) return;
        if ((legacy.access & (Opcodes.ACC_STATIC | Opcodes.ACC_PRIVATE)) != 0) {
            throw new IllegalStateException(
                    "Minecraft widget narration override has unexpected access flags in " + node.name);
        }
        if (declared(node, MODERN_WIDGET_NARRATION, WIDGET_NARRATION_DESC) != null) {
            throw new IllegalStateException(
                    "Both legacy and modern widget narration hooks are declared in " + node.name);
        }
        legacy.name = MODERN_WIDGET_NARRATION;
        counts.widgetNarrationOverrides++;
    }

    /** A recipe condition written for 1.20.1 ({@code getID()} and {@code test}) lacks the {@code codec()} of 1.21.1: the codec of the serializer registered under its id. */
    private static void addConditionCodec(ClassNode node, Counts c) {
        if (node.interfaces == null || !node.interfaces.contains("net/neoforged/neoforge/common/conditions/ICondition")) return;
        String desc = "()Lcom/mojang/serialization/MapCodec;";
        if (declared(node, "codec", desc) != null) return;
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "codec", desc, null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, node.name, "getID", "()Lnet/minecraft/resources/ResourceLocation;", false));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "legacyconnector/v1_20_1/net/minecraftforge/common/crafting/CraftingHelper", "conditionCodecOf",
                "(Lnet/minecraft/resources/ResourceLocation;)Lcom/mojang/serialization/MapCodec;", false));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        m.maxStack = 1;
        m.maxLocals = 1;
        node.methods.add(m);
        c.bridges++;
    }

    /** A class that implements {@code CriterionTrigger} itself (no {@code SimpleCriterionTrigger} base) lacks the {@code codec()} of 1.21.1: it reads the legacy instance. */
    private static void addTriggerCodec(ClassNode node, Counts c) {
        if (node.interfaces == null || !node.interfaces.contains("net/minecraft/advancements/CriterionTrigger")) return;
        String desc = "()Lcom/mojang/serialization/Codec;";
        if (declared(node, "codec", desc) != null) return;
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, "codec", desc, null, null);
        m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
        m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, Mc1201Tables.HELPER_PACKAGE + "CritCompat", "directCodec", "(Ljava/lang/Object;)Lcom/mojang/serialization/Codec;", false));
        m.instructions.add(new InsnNode(Opcodes.ARETURN));
        m.maxStack = 1;
        m.maxLocals = 1;
        node.methods.add(m);
        c.bridges++;
    }

    /**
     * A recipe serializer written for 1.20.1 ({@code fromJson}/{@code fromNetwork}/{@code toNetwork}) lacks the {@code codec()} and {@code streamCodec()} of 1.21.1:
     * the class gets both, answered by {@code RecipeCompat} from the legacy methods.
     */
    private static void addRecipeSerializerCodecs(ClassNode node, Counts c) {
        if (declared(node, "fromNetwork", "(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)Lnet/minecraft/world/item/crafting/Recipe;") == null
                && node.methods.stream().noneMatch(m -> m.name.equals("fromNetwork") && m.desc.startsWith("(Lnet/minecraft/resources/ResourceLocation;Lnet/minecraft/network/FriendlyByteBuf;)"))) return;
        String helper = Mc1201Tables.HELPER_PACKAGE + "RecipeCompat";
        String[][] adds = {{"codec", "()Lcom/mojang/serialization/MapCodec;", "(Ljava/lang/Object;)Lcom/mojang/serialization/MapCodec;"},
                {"streamCodec", "()Lnet/minecraft/network/codec/StreamCodec;", "(Ljava/lang/Object;)Lnet/minecraft/network/codec/StreamCodec;"}};
        for (String[] a : adds) {
            if (declared(node, a[0], a[1]) != null) continue;
            MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC, a[0], a[1], null, null);
            m.instructions.add(new VarInsnNode(Opcodes.ALOAD, 0));
            m.instructions.add(new MethodInsnNode(Opcodes.INVOKESTATIC, helper, a[0], a[2], false));
            m.instructions.add(new InsnNode(Opcodes.ARETURN));
            m.maxStack = 1;
            m.maxLocals = 1;
            node.methods.add(m);
            c.bridges++;
        }
    }

    /** The 1.21.1 type interfaces whose single {@code codec()} returns a {@code MapCodec} (it was a {@code Codec} in 1.20.1: a lambda {@code () -> CODEC} implements them). */
    private static final java.util.Set<String> MAP_CODEC_TYPES = java.util.Set.of(
            "net/minecraft/world/level/levelgen/structure/StructureType",
            "net/minecraft/world/level/levelgen/structure/placement/StructurePlacementType",
            "net/minecraft/world/level/levelgen/structure/templatesystem/StructureProcessorType",
            "net/minecraft/world/level/levelgen/structure/templatesystem/RuleTestType",
            "net/minecraft/world/level/levelgen/structure/templatesystem/PosRuleTestType",
            "net/minecraft/world/level/levelgen/placement/PlacementModifierType",
            "net/minecraft/world/level/levelgen/feature/featuresize/FeatureSizeType",
            "net/minecraft/world/level/levelgen/feature/foliageplacers/FoliagePlacerType",
            "net/minecraft/world/level/levelgen/feature/stateproviders/BlockStateProviderType",
            "net/minecraft/world/level/levelgen/feature/trunkplacers/TrunkPlacerType");

    /**
     * {@code StructureType<S> type = () -> CODEC}: the lambda is built as a {@code Supplier} of the old codec instead, and a helper makes the interface
     * (via a proxy) whose {@code codec()} converts it to the {@code MapCodec} 1.21.1 asks for.
     */
    private static boolean codecLambda(MethodNode m, org.objectweb.asm.tree.InvokeDynamicInsnNode d) {
        if (!d.name.equals("codec") || !d.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory") || d.bsmArgs.length < 3
                || !(d.bsmArgs[0] instanceof Type sam) || !sam.getDescriptor().equals("()Lcom/mojang/serialization/Codec;")) return false;
        Type made = Type.getReturnType(d.desc);
        if (made.getSort() != Type.OBJECT || !MAP_CODEC_TYPES.contains(made.getInternalName())) return false;
        d.name = "get";
        d.desc = d.desc.substring(0, d.desc.indexOf(')') + 1) + "Ljava/util/function/Supplier;";   // captured arguments (a lambda over a codec parameter) stay
        d.bsmArgs[0] = Type.getMethodType("()Ljava/lang/Object;");
        d.bsmArgs[2] = Type.getMethodType("()Lcom/mojang/serialization/Codec;");
        InsnList after = new InsnList();
        after.add(new org.objectweb.asm.tree.LdcInsnNode(made));
        after.add(new InsnNode(Opcodes.SWAP));
        after.add(new MethodInsnNode(Opcodes.INVOKESTATIC, "org/kylepitcock/legacyconnector/v1_20_1/migrate/EntityCompat", "codecTypeLambda",
                "(Ljava/lang/Class;Ljava/util/function/Supplier;)Ljava/lang/Object;", false));
        after.add(new org.objectweb.asm.tree.TypeInsnNode(Opcodes.CHECKCAST, made.getInternalName()));
        m.instructions.insert(d, after);
        return true;
    }

    /**
     * A bucket class extending {@code MobBucketItem} and calling the 1.20.1 supplier constructor (Aquaculture's fish bucket): it extends the stand-in
     * {@code LegacyMobBucketItem}, which has that constructor, instead.
     */
    private static void swapMobBucketSuper(ClassNode node) {
        final String mob = "net/minecraft/world/item/MobBucketItem";
        final String standIn = "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyMobBucketItem";
        final String desc = "(Ljava/util/function/Supplier;Ljava/util/function/Supplier;Ljava/util/function/Supplier;Lnet/minecraft/world/item/Item$Properties;)V";
        if (!mob.equals(node.superName)) return;
        boolean swapped = false;
        for (MethodNode m : node.methods) {
            for (AbstractInsnNode in : m.instructions.toArray()) {
                if (in instanceof MethodInsnNode call && call.getOpcode() == Opcodes.INVOKESPECIAL && call.owner.equals(mob) && call.name.equals("<init>") && call.desc.equals(desc)) {
                    call.owner = standIn;
                    swapped = true;
                }
            }
        }
        if (swapped) node.superName = standIn;
    }

    /**
     * {@code @Mod.EventBusSubscriber(bus = Bus.FORGE)}: the enum constant is {@code GAME} in NeoForge 21.1 (FML would fail with "No enum constant FORGE" while
     * scanning); the default (no value) needs nothing.
     */
    private static void renameBusConstant(ClassNode node) {
        if (node.visibleAnnotations == null) return;
        for (var a : node.visibleAnnotations) {
            if (!a.desc.equals("Lnet/neoforged/fml/common/EventBusSubscriber;") || a.values == null) continue;
            for (int i = 0; i + 1 < a.values.size(); i += 2) {
                if (a.values.get(i + 1) instanceof String[] e && e.length == 2 && e[0].equals("Lnet/neoforged/fml/common/EventBusSubscriber$Bus;") && e[1].equals("FORGE")) {
                    a.values.set(i + 1, new String[] {e[0], "GAME"});
                }
            }
        }
    }

    /** Forge ignored {@code @Mod.EventBusSubscriber} on a class without {@code @SubscribeEvent} methods; NeoForge's automatic subscriber throws, so the annotation goes. */
    private static void dropEmptySubscriber(ClassNode node) {
        if (node.visibleAnnotations == null) return;
        org.objectweb.asm.tree.AnnotationNode sub = null;
        for (var a : node.visibleAnnotations) if (a.desc.equals("Lnet/neoforged/fml/common/EventBusSubscriber;")) sub = a;
        if (sub == null) return;
        int subscribers = 0;
        int clientOnly = 0;
        for (MethodNode m : node.methods) {
            if (m.visibleAnnotations == null || m.visibleAnnotations.stream().noneMatch(a -> a.desc.equals("Lnet/neoforged/bus/api/SubscribeEvent;"))) continue;
            subscribers++;
            // FML removes @OnlyIn(CLIENT) methods on a dedicated server: a class with only those has no subscribers there
            if (m.visibleAnnotations.stream().anyMatch(a -> a.desc.equals("Lnet/neoforged/api/distmarker/OnlyIn;") && a.values != null && a.values.size() >= 2
                    && a.values.get(1) instanceof String[] e && e[1].equals("CLIENT"))
                    // a handler of a client event (RenderLevelStageEvent...) or one that mentions a client class: reading the methods of the class on a server loads it
                    || m.desc.contains("Lnet/minecraft/client/") || m.desc.contains("Lnet/neoforged/neoforge/client/") || m.desc.contains("FMLClientSetupEvent")) clientOnly++;
        }
        if (subscribers == 0) {
            // kept: Subscribers1201 replaces it by a load of the class (Forge loaded such a class for its static initializer: Aquaculture's Hooks registers its hook items there)
            return;
        } else if (subscribers == clientOnly && (sub.values == null || !sub.values.contains("value"))) {
            if (sub.values == null) sub.values = new java.util.ArrayList<>();
            sub.values.add("value");
            sub.values.add(new java.util.ArrayList<Object>(java.util.Collections.singletonList((Object) new String[] {"Lnet/neoforged/api/distmarker/Dist;", "CLIENT"})));
        }
    }

    /**
     * A mod method that overrides a method which is final in 1.21.1 ({@code LivingEntity.getDimensions(Pose)}, the point to override moved to
     * {@code getDefaultDimensions}) is renamed to the new override point, with its {@code super} and own-class calls; with no replacement it is disabled.
     */
    private static void renameOverrides(ClassNode node, Mc1201Hierarchy h, Mc1201Tables t, Counts c) {
        for (String[] row : t.overrides) {
            MethodNode decl = declared(node, row[1], row[2]);
            if (decl == null || (decl.access & Opcodes.ACC_STATIC) != 0 || !h.isSubclassOf(node.name, row[0])) continue;
            boolean disable = row[3].equals("-");
            String newName = disable ? "mcc$disabled$" + row[1] : row[3];
            if (declared(node, newName, row[2]) != null) continue;
            decl.name = newName;
            c.bridges++;
            if (disable) continue;
            for (MethodNode m : node.methods) {
                if (m.instructions == null) continue;
                for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = in.getNext()) {
                    if (in instanceof MethodInsnNode call && call.name.equals(row[1]) && call.desc.equals(row[2])
                            && (call.getOpcode() == Opcodes.INVOKESPECIAL ? h.isSubclassOf(call.owner, row[0]) && !call.owner.equals(node.name)
                                    : call.getOpcode() == Opcodes.INVOKEVIRTUAL && call.owner.equals(node.name))) {
                        call.name = newName;
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------------ calls and fields

    private static final String ABSTRACT_SELECTION_LIST =
            "net/minecraft/client/gui/components/AbstractSelectionList";
    private static final String OBJECT_SELECTION_LIST =
            "net/minecraft/client/gui/components/ObjectSelectionList";

    /**
     * Minecraft 1.20.1's AbstractSelectionList.setLeftPos(int) only moved the
     * widget's left edge. In 1.21.1 the list inherits AbstractWidget.setX(int),
     * with width and row bounds derived from the widget geometry. Redirect
     * calls on any known selection-list subclass to that equivalent setter.
     */
    private static boolean rewriteSelectionListPosition(MethodInsnNode call, Mc1201Hierarchy hierarchy) {
        if ((call.getOpcode() != Opcodes.INVOKEVIRTUAL && call.getOpcode() != Opcodes.INVOKEINTERFACE)
                || !"setLeftPos".equals(call.name)
                || !"(I)V".equals(call.desc)) {
            return false;
        }
        if (!hierarchy.isSubclassOf(call.owner, ABSTRACT_SELECTION_LIST)
                && !hierarchy.isSubclassOf(call.owner, OBJECT_SELECTION_LIST)) {
            return false;
        }
        call.name = "setX";
        return true;
    }

    private static void rewriteBody(MethodNode m, Mc1201Hierarchy h, Mc1201Tables t, Counts c) {
        Deque<TypeInsnNode> news = new ArrayDeque<>();
        AbstractInsnNode next;
        for (AbstractInsnNode in = m.instructions.getFirst(); in != null; in = next) {
            next = in.getNext(); // InsnList.set/remove clear the replaced node's links
            if (in instanceof TypeInsnNode ti && ti.getOpcode() == Opcodes.NEW) {
                news.push(ti);
            } else if (in instanceof MethodInsnNode call) {
                if (rewriteSelectionListPosition(call, h)) {
                    c.selectionListPositionCalls++;
                }
                if (!call.name.equals("<init>")
                        && (h.isMinecraft(call.owner)
                                || (h.isMod(call.owner) && h.declaredIn(call.owner, call.name, call.desc) != Origin.MOD))) {
                    // Renamed Minecraft members can be called directly on Minecraft classes or inherited through a mod-owned subclass.
                    // A method declared by the mod itself must keep its own name.
                    for (String ancestor : h.minecraftAncestors(call.owner)) {
                        String renamed = t.renamedMember(ancestor, call.name, call.desc);
                        if (!renamed.equals(call.name)) {
                            call.name = renamed;
                            c.calls++;
                            break;
                        }
                    }
                }
                if (CLASSES_THAT_BECAME_INTERFACES.contains(call.owner) && !call.itf) {
                    call.itf = true;
                    if (call.getOpcode() == Opcodes.INVOKEVIRTUAL) call.setOpcode(Opcodes.INVOKEINTERFACE);
                    c.calls++;
                }
                if (call.getOpcode() == Opcodes.INVOKEINTERFACE && CLASSES_THAT_WERE_INTERFACES.contains(call.owner)) {
                    call.setOpcode(Opcodes.INVOKEVIRTUAL);
                    call.itf = false;
                }
                if (call.name.equals("<init>")) {
                    TypeInsnNode made = call.getOpcode() == Opcodes.INVOKESPECIAL && !news.isEmpty() && news.peek().desc.equals(call.owner) ? news.pop() : null;
                    Call rule = t.calls.get(call.owner + ".<init>" + call.desc);
                    if (rule != null && rule.helper().startsWith("=")) {
                        inPlace(m, call, rule);
                        c.constructors++;
                    } else if (made != null && rule != null && made.getNext() != null && made.getNext().getOpcode() == Opcodes.DUP) {
                        m.instructions.remove(made.getNext());
                        m.instructions.remove(made);
                        String hd = rule.helperDesc() != null ? rule.helperDesc() : call.desc.substring(0, call.desc.length() - 1) + "L" + call.owner + ";";
                        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, rule.helper(), rule.helperName(), hd, false));
                        c.constructors++;
                    }
                } else if (call.owner.startsWith("[")) {
                    // array clone and friends
                } else if (redirect(m, call, h, t)) {
                    c.calls++;
                } else if (addProvider(m, call, h, t)) {
                    c.providerCalls++;
                }
            } else if (in instanceof org.objectweb.asm.tree.InvokeDynamicInsnNode d) {
                if (codecLambda(m, d)) c.calls++;   // the stack grew: counted so the class gets its frames and limits computed again
                registryBufferLambda(d);
                redirectHandles(d, h, t, c);
            } else if (in instanceof FieldInsnNode f && (f.getOpcode() == Opcodes.GETFIELD || f.getOpcode() == Opcodes.PUTFIELD)) {
                // an instance field Minecraft dropped: mc-calls.tsv rows named $get$field / $set$field (descriptors "()T" / "(T)V") with a helper taking the receiver
                boolean get = f.getOpcode() == Opcodes.GETFIELD;
                String cd = get ? "()" + f.desc : "(" + f.desc + ")V";
                Call rule = null;
                for (String owner : h.minecraftAncestors(f.owner)) {
                    rule = t.calls.get(owner + (get ? ".$get$" : ".$set$") + f.name + cd);
                    if (rule != null) break;
                }
                if (rule != null) {
                    String hd = rule.helperDesc() != null ? rule.helperDesc() : "(L" + rule.owner() + ";" + cd.substring(1);
                    m.instructions.set(f, new MethodInsnNode(Opcodes.INVOKESTATIC, rule.helper(), rule.helperName(), hd, false));
                    c.fields++;
                }
            } else if (in instanceof FieldInsnNode f && f.getOpcode() == Opcodes.GETSTATIC) {
                // a class of the jar itself keeps its own field (Farmer's Delight translated as a mod reads its ModBiomeFeatures.WILD_CROP RegistryObject;
                // the rows exist for other mods that use it as a library)
                Field rule = h.isMod(f.owner) ? null : t.fields.get(f.owner + "." + f.name + ":" + f.desc);
                if (rule == null) {
                    // a static field inherited from a Minecraft class (a mod item reading Item.BASE_ATTACK_DAMAGE_UUID through its own class)
                    for (String owner : h.minecraftAncestors(f.owner)) {
                        rule = t.fields.get(owner + "." + f.name + ":" + f.desc);
                        if (rule != null) break;
                    }
                }
                if (rule != null) {
                    rewriteField(m, f, rule);
                    c.fields++;
                }
            }
        }
    }

    /** Library interfaces of 1.20.1 that are classes now under the renamed name (GeckoLib's {@code CoreGeoBone} became {@code GeoBone}): their calls are virtual. */
    /** Classes of 1.20.1 that are interfaces in 1.21.1 (DataResult: Datafixer Upper 6 to 8): static calls need an interface method reference. */
    private static final java.util.Set<String> CLASSES_THAT_BECAME_INTERFACES = java.util.Set.of("com/mojang/serialization/DataResult");

    private static final java.util.Set<String> CLASSES_THAT_WERE_INTERFACES = java.util.Set.of("software/bernie/geckolib/cache/object/GeoBone");

    /** Functional interfaces whose buffer parameter became a {@code RegistryFriendlyByteBuf}: a lambda written for the 1.20.1 {@code FriendlyByteBuf} would not link. */
    private static final java.util.Set<String> REGISTRY_BUFFER_INTERFACES = java.util.Set.of("net/neoforged/neoforge/network/IContainerFactory");

    /**
     * {@code (id, inv, buf) -> ...} for {@code IContainerFactory}: the lambda method keeps taking the {@code FriendlyByteBuf} (a supertype), but the
     * metafactory's own method types must name the {@code RegistryFriendlyByteBuf} of the interface.
     */
    private static void registryBufferLambda(org.objectweb.asm.tree.InvokeDynamicInsnNode d) {
        if (!d.bsm.getOwner().equals("java/lang/invoke/LambdaMetafactory") || d.bsmArgs.length < 3) return;
        if (!REGISTRY_BUFFER_INTERFACES.contains(Type.getReturnType(d.desc).getInternalName())) return;
        for (int i : new int[] {0, 2}) {
            if (!(d.bsmArgs[i] instanceof Type t) || t.getSort() != Type.METHOD) continue;
            d.bsmArgs[i] = Type.getMethodType(t.getDescriptor().replace("Lnet/minecraft/network/FriendlyByteBuf;", "Lnet/minecraft/network/RegistryFriendlyByteBuf;"));
        }
    }

    /**
     * Method references ({@code ItemStack::of}, {@code Foo::new}) are handles, not calls: a call rule that redirects to a static helper
     * (not the in-place {@code =} rules) applies to the handle as well; the helper takes the receiver first, as a static handle does for an
     * unbound instance reference. A bound receiver ({@code obj::method}) is passed as the first captured argument, which is the same shape.
     */
    private static void redirectHandles(org.objectweb.asm.tree.InvokeDynamicInsnNode d, Mc1201Hierarchy h, Mc1201Tables t, Counts c) {
        if (d.bsmArgs == null) return;
        for (int i = 0; i < d.bsmArgs.length; i++) {
            if (!(d.bsmArgs[i] instanceof org.objectweb.asm.Handle hd)) continue;
            int tag = hd.getTag();
            // H_INVOKEINTERFACE (9) counts too: Recipe::getId, ItemStack-like interface references are handles of an interface method
            if (tag < Opcodes.H_INVOKEVIRTUAL || tag > Opcodes.H_INVOKEINTERFACE || hd.getOwner().startsWith("[")) continue;
            boolean ctor = tag == Opcodes.H_NEWINVOKESPECIAL;
            Call rule = null;
            for (String owner : h.minecraftAncestors(hd.getOwner())) {
                rule = t.calls.get(owner + "." + hd.getName() + hd.getDesc());
                if (rule != null) break;
            }
            if (rule == null || rule.helper().startsWith("=")) continue;
            if (!ctor && tag != Opcodes.H_INVOKESTATIC && h.declaredIn(hd.getOwner(), hd.getName(), hd.getDesc()) == Origin.MOD) continue;
            String desc = rule.helperDesc();
            if (desc == null) {
                if (ctor) desc = hd.getDesc().substring(0, hd.getDesc().length() - 1) + "L" + hd.getOwner() + ";";
                else desc = tag == Opcodes.H_INVOKESTATIC ? hd.getDesc() : "(L" + rule.owner() + ";" + hd.getDesc().substring(1);
            }
            // A method handle has no following POP instruction to discard a changed return value. Keep it legacy-shaped;
            // direct call sites can be adapted safely when the old return value is immediately discarded.
            if (Type.getReturnType(hd.getDesc()).getSort() != Type.VOID && Type.getReturnType(desc).getSort() == Type.VOID) continue;
            d.bsmArgs[i] = new org.objectweb.asm.Handle(Opcodes.H_INVOKESTATIC, rule.helper(), rule.helperName(), desc, false);
            c.calls++;
        }
    }

    /** The first rule found at the receiver type or one of its Minecraft ancestors, if no mod class declares the method. */
    private static boolean redirect(MethodNode m, MethodInsnNode call, Mc1201Hierarchy h, Mc1201Tables t) {
        Call rule = null;
        boolean special = call.getOpcode() == Opcodes.INVOKESPECIAL;
        for (String owner : h.minecraftAncestors(call.owner)) {
            if (special) rule = t.calls.get(owner + "." + call.name + call.desc + "@special");
            else rule = t.calls.get(owner + "." + call.name + call.desc + "@virtual");   // rows that must not touch super calls
            if (rule == null) rule = t.calls.get(owner + "." + call.name + call.desc);
            if (rule != null) break;
        }
        if (rule == null && special && h.reachesUnknown(call.owner)) {
            // super.appendHoverText(...) through a class of a library that is not in the set (its native 1.21.1 build replaces the 1.20.1 copy): the class is not
            // known, and the old signature does not exist in it either way, so the rule of the Minecraft method with that name and descriptor applies
            rule = t.specialRuleFor(call.name, call.desc);
        }
        if (rule == null) return false;
        if (call.getOpcode() != Opcodes.INVOKESTATIC && !special && h.declaredIn(call.owner, call.name, call.desc) == Origin.MOD) return false;
        if (special && h.declaredIn(call.owner, call.name, call.desc) == Origin.MOD) return false;
        String hd = rule.helperDesc();
        if (rule.helper().startsWith("=")) {
            inPlace(m, call, rule);
            return true;
        }
        if (hd == null) hd = call.getOpcode() == Opcodes.INVOKESTATIC ? call.desc : "(L" + rule.owner() + ";" + call.desc.substring(1);
        Type oldReturn = Type.getReturnType(call.desc);
        Type helperReturn = Type.getReturnType(hd);
        if (oldReturn.getSort() != Type.VOID && helperReturn.getSort() == Type.VOID) {
            // A return-type change from value to void is safe only when the old value is immediately discarded.
            // If the caller observes it, leave the call untouched so SoftLink1201 reports the unsupported behavior
            // instead of fabricating a result with different semantics.
            AbstractInsnNode discarded = call.getNext();
            while (discarded != null && discarded.getOpcode() < 0) discarded = discarded.getNext();
            int popOpcode = oldReturn.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP;
            if (discarded == null || discarded.getOpcode() != popOpcode) return false;
            m.instructions.remove(discarded);
        }
        m.instructions.set(call, new MethodInsnNode(Opcodes.INVOKESTATIC, rule.helper(), rule.helperName(), hd, false));
        return true;
    }

    /**
     * A rule that keeps the call but changes it: {@code =} renames and/or re-describes it (opcode and receiver stay), {@code =swap} also swaps the
     * last two arguments (both one slot wide: constructors that exchanged two parameters), {@code =pop} drops the last argument (one slot), {@code =ret} discards
     * the result of a method that used to return void.
     */
    private static void inPlace(MethodNode m, MethodInsnNode call, Call rule) {
        if (rule.helper().equals("=args")) {
            rewriteArgs(m, call, rule);
            return;
        }
        if (rule.helper().equals("=swap")) m.instructions.insertBefore(call, new InsnNode(Opcodes.SWAP));
        else if (rule.helper().equals("=pop")) m.instructions.insertBefore(call, new InsnNode(Opcodes.POP));
        else if (rule.helper().equals("=swappop")) {
            // drops the second to last argument (a constructor that lost its id parameter: CustomRecipe)
            m.instructions.insertBefore(call, new InsnNode(Opcodes.SWAP));
            m.instructions.insertBefore(call, new InsnNode(Opcodes.POP));
        }
        // four colour floats on the stack become one packed ARGB int (GeckoLib 4.9; lib-calls.tsv)
        else if (rule.helper().equals("=colour")) m.instructions.insertBefore(call, new MethodInsnNode(Opcodes.INVOKESTATIC, Mc1201Tables.HELPER_PACKAGE + "GeckoCompat", "pack", "(FFFF)I", false));
        else if (rule.helper().equals("=ret")) m.instructions.insert(call, new InsnNode(Type.getReturnType(rule.helperDesc()).getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        if (!rule.helperName().equals("-")) call.name = rule.helperName();
        if (rule.helperDesc() != null) call.desc = rule.helperDesc();
    }

    /**
     * {@code =args}: the call keeps its name and receiver, its arguments are produced again from the old ones. The helper-name column lists one token per
     * new parameter: {@code N} (old argument N unchanged), {@code Class.method@a+b} (a static helper taking old arguments a and b, returning the new parameter),
     * and the helper-descriptor column is the new descriptor. The old arguments are spilled to fresh locals first, so any argument can be dropped, reordered or
     * converted (super constructors that lost an id, took a {@code Holder}...).
     */
    private static void rewriteArgs(MethodNode m, MethodInsnNode call, Call rule) {
        Type[] old = Type.getArgumentTypes(call.desc);
        Type[] now = Type.getArgumentTypes(rule.helperDesc());
        String[] tokens = rule.helperName().split(",");
        if (tokens.length != now.length) throw new IllegalStateException("=args row for " + call.owner + "." + call.name + call.desc + ": " + tokens.length + " tokens for " + now.length + " parameters");
        int[] slot = new int[old.length];
        int next = m.maxLocals;
        for (int i = 0; i < old.length; i++) {
            slot[i] = next;
            next += old[i].getSize();
        }
        InsnList il = new InsnList();
        for (int i = old.length - 1; i >= 0; i--) il.add(new VarInsnNode(old[i].getOpcode(Opcodes.ISTORE), slot[i]));
        for (int j = 0; j < now.length; j++) {
            String tok = tokens[j].strip();
            int at = tok.indexOf('@');
            if (at < 0) {
                int i = Integer.parseInt(tok);
                il.add(new VarInsnNode(old[i].getOpcode(Opcodes.ILOAD), slot[i]));
                continue;
            }
            String helper = tok.substring(0, at);
            StringBuilder desc = new StringBuilder("(");
            for (String a : tok.substring(at + 1).split("\\+")) {
                int i = Integer.parseInt(a);
                il.add(new VarInsnNode(old[i].getOpcode(Opcodes.ILOAD), slot[i]));
                desc.append(old[i].getDescriptor());
            }
            desc.append(')').append(now[j].getDescriptor());
            int dot = helper.lastIndexOf('.');
            String cls = helper.substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, helper.substring(dot + 1), desc.toString(), false));
        }
        m.instructions.insertBefore(call, il);
        call.desc = rule.helperDesc();
        m.maxLocals = next;
        m.maxStack += 4;
    }

    private static void rewriteField(MethodNode m, FieldInsnNode f, Field rule) {
        String newName = rule.newName().equals("-") ? f.name : rule.newName();
        String newDesc = rule.newDesc().equals("-") ? f.desc : rule.newDesc();
        InsnList after = new InsnList();
        String post = rule.post();
        if (rule.newDesc().equals("none")) {
            // the field is gone: a helper without arguments produces the value
            int dot = post.lastIndexOf('.');
            String cls = post.substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            m.instructions.set(f, new MethodInsnNode(Opcodes.INVOKESTATIC, cls, post.substring(dot + 1), "()" + f.desc, false));
            return;
        }
        if (post.equals("value")) {
            after.add(new MethodInsnNode(Opcodes.INVOKEINTERFACE, HOLDER, "value", "()Ljava/lang/Object;", true));
            after.add(new TypeInsnNode(Opcodes.CHECKCAST, Type.getType(f.desc).getInternalName()));
        } else if (post.equals("location")) {
            after.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL, "net/minecraft/resources/ResourceKey", "location", "()Lnet/minecraft/resources/ResourceLocation;", false));
        } else if (!post.equals("-")) {
            int dot = post.lastIndexOf('.');
            String cls = post.substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            after.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, post.substring(dot + 1), "(" + newDesc + ")" + f.desc, false));
        }
        f.name = newName;
        f.desc = newDesc;
        if (after.size() > 0) m.instructions.insert(f, after);
        m.maxStack = Math.max(m.maxStack, 2);
    }

    /** Context carried by an extra trailing parameter of the 1.21.1 method: the type to the {@link MigrateContext} getter that returns it. */
    private static final java.util.Map<String, String[]> EXTRAS = java.util.Map.of(
            PROVIDER, new String[] {"provider", "()L" + PROVIDER + ";"},
            "net/minecraft/network/syncher/SynchedEntityData$Builder", new String[] {"builder", "()Lnet/minecraft/network/syncher/SynchedEntityData$Builder;"});

    /** A call of a bridged method that Minecraft implements: appends the registry provider (or entity data builder) the 1.21.1 method needs. */
    private static boolean addProvider(MethodNode m, MethodInsnNode call, Mc1201Hierarchy h, Mc1201Tables t) {
        for (Bridge b : t.bridges) {
            if (!b.oldName().equals(call.name) || !b.oldDesc().equals(call.desc)) continue;
            if (call.getOpcode() == Opcodes.INVOKESTATIC) continue;
            Type[] oldArgs = Type.getArgumentTypes(b.oldDesc());
            Type[] newArgs = Type.getArgumentTypes(b.newDesc());
            if (newArgs.length != oldArgs.length + 1 || !b.args().isEmpty() || b.ret() != null) continue;
            String[] getter = EXTRAS.get(newArgs[newArgs.length - 1].getInternalName());
            if (getter == null) continue;
            if (!b.newDesc().equals(b.oldDesc().substring(0, b.oldDesc().indexOf(')')) + newArgs[newArgs.length - 1].getDescriptor() + b.oldDesc().substring(b.oldDesc().indexOf(')')))) continue;
            if (!h.isSubclassOf(call.owner, b.owner())) continue;
            if (h.declaredIn(call.owner, call.name, call.desc) == Origin.MOD) continue;
            m.instructions.insertBefore(call, new MethodInsnNode(Opcodes.INVOKESTATIC, CONTEXT, getter[0], getter[1], false));
            call.name = b.newName();
            call.desc = b.newDesc();
            m.maxStack += 1;
            return true;
        }
        return false;
    }

    // ------------------------------------------------------------------ bridge methods

    private static void addBridges(ClassNode node, Mc1201Hierarchy h, Mc1201Tables t, Counts c) {
        for (Bridge b : t.bridges) {
            MethodNode old = declared(node, b.oldName(), b.oldDesc());
            if (old == null || (old.access & (Opcodes.ACC_STATIC | Opcodes.ACC_ABSTRACT)) != 0) continue;
            if (declared(node, b.newName(), b.newDesc()) != null) continue;
            if (!h.isSubclassOf(node.name, b.owner()) && !h.reachesUnknown(node.name)) continue;
            MethodNode bridge = bridge(node, old, b);
            if (bridge == null) continue;
            node.methods.add(bridge);
            c.bridges++;
        }
    }

    private static MethodNode declared(ClassNode node, String name, String desc) {
        for (MethodNode m : node.methods) if (m.name.equals(name) && m.desc.equals(desc)) return m;
        return null;
    }

    private static MethodNode bridge(ClassNode node, MethodNode old, Bridge b) {
        Type[] newArgs = Type.getArgumentTypes(b.newDesc());
        int[] slots = new int[newArgs.length];
        int slot = 1;
        for (int i = 0; i < newArgs.length; i++) {
            slots[i] = slot;
            slot += newArgs[i].getSize();
        }
        Type[] oldArgs = Type.getArgumentTypes(b.oldDesc());
        List<String> tokens = b.args().isEmpty() ? identity(oldArgs.length) : b.args();
        if (tokens.size() != oldArgs.length) return null;
        InsnList il = new InsnList();
        if (b.remember() >= 0) {
            il.add(new VarInsnNode(Opcodes.ALOAD, slots[b.remember()]));
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, CONTEXT, "remember", "(Ljava/lang/Object;)V", false));
        }
        il.add(new VarInsnNode(Opcodes.ALOAD, 0));
        int stack = 1;
        for (int p = 0; p < oldArgs.length; p++) {
            String tok = tokens.get(p);
            if (tok.equals("this")) {
                il.add(new VarInsnNode(Opcodes.ALOAD, 0));
                stack += 1;
                continue;
            }
            if (tok.equals("null")) {
                il.add(new InsnNode(Opcodes.ACONST_NULL));
                stack += 1;
                continue;
            }
            String helper = null;
            int at = tok.indexOf('@');
            if (at > 0) {
                helper = tok.substring(0, at);
                tok = tok.substring(at + 1);
            }
            int i = Integer.parseInt(tok);
            il.add(new VarInsnNode(newArgs[i].getOpcode(Opcodes.ILOAD), slots[i]));
            if (helper != null) {
                int dot = helper.lastIndexOf('.');
                String cls = helper.substring(0, dot);
                if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
                il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, helper.substring(dot + 1), "(" + newArgs[i].getDescriptor() + ")" + oldArgs[p].getDescriptor(), false));
            } else if (!newArgs[i].equals(oldArgs[p]) && newArgs[i].getSort() >= Type.ARRAY && oldArgs[p].getSort() >= Type.ARRAY) {
                il.add(new TypeInsnNode(Opcodes.CHECKCAST, oldArgs[p].getInternalName()));
            } else if (!newArgs[i].equals(oldArgs[p])) {
                return null;
            }
            stack += Math.max(newArgs[i].getSize(), oldArgs[p].getSize());
        }
        boolean priv = (old.access & Opcodes.ACC_PRIVATE) != 0;
        il.add(new MethodInsnNode(priv ? Opcodes.INVOKESPECIAL : Opcodes.INVOKEVIRTUAL, node.name, b.oldName(), b.oldDesc(), (node.access & Opcodes.ACC_INTERFACE) != 0));
        Type oldRet = Type.getReturnType(b.oldDesc());
        Type newRet = Type.getReturnType(b.newDesc());
        if (b.ret() != null && b.ret().startsWith("arg:")) {
            // the 1.21.1 method returns one of its parameters that the old method had no result for (playerWillDestroy returns the state)
            if (oldRet.getSort() != Type.VOID) il.add(new InsnNode(oldRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            int i = Integer.parseInt(b.ret().substring(4));
            il.add(new VarInsnNode(newArgs[i].getOpcode(Opcodes.ILOAD), slots[i]));
        } else if (b.ret() != null && (b.ret().equals("const:true") || b.ret().equals("const:false"))) {
            // the 1.21.1 method returns a boolean the old (void or other) method had no answer for: the constant, after the old result is dropped
            if (oldRet.getSort() != Type.VOID) il.add(new InsnNode(oldRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
            il.add(new InsnNode(b.ret().equals("const:true") ? Opcodes.ICONST_1 : Opcodes.ICONST_0));
        } else if (b.ret() != null) {
            int dot = b.ret().lastIndexOf('.');
            String cls = b.ret().substring(0, dot);
            if (!cls.contains("/")) cls = Mc1201Tables.HELPER_PACKAGE + cls;
            il.add(new MethodInsnNode(Opcodes.INVOKESTATIC, cls, b.ret().substring(dot + 1), "(" + oldRet.getDescriptor() + ")" + newRet.getDescriptor(), false));
        } else if (newRet.getSort() == Type.VOID && oldRet.getSort() != Type.VOID) {
            il.add(new InsnNode(oldRet.getSize() == 2 ? Opcodes.POP2 : Opcodes.POP));
        } else if (!oldRet.equals(newRet)) {
            if (oldRet.getSort() >= Type.ARRAY && newRet.getSort() >= Type.ARRAY) il.add(new TypeInsnNode(Opcodes.CHECKCAST, newRet.getInternalName()));
            else return null;
        }
        il.add(new InsnNode(newRet.getOpcode(Opcodes.IRETURN)));
        MethodNode out = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_SYNTHETIC, b.newName(), b.newDesc(), null, null);
        out.instructions = il;
        out.maxLocals = slot;
        out.maxStack = Math.max(stack + 1, 3) + Math.max(newRet.getSize(), oldRet.getSize());
        return out;
    }

    private static List<String> identity(int n) {
        String[] t = new String[n];
        for (int i = 0; i < n; i++) t[i] = Integer.toString(i);
        return List.of(t);
    }
}
