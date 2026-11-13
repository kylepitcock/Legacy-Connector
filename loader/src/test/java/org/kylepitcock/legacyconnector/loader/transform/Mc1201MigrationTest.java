package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

/** The Minecraft 1.20.1 to 1.21.1 migration on synthetic classes, with the real tables and the real 1.21.1 class tree. */
class Mc1201MigrationTest implements Opcodes {
    private static final String MIGRATE = "org/kylepitcock/legacyconnector/v1_20_1/migrate/";
    private static final String MEI = "net/minecraft/world/effect/MobEffectInstance";
    private static final String ME = "net/minecraft/world/effect/MobEffect";
    private static final String STACK = "net/minecraft/world/item/ItemStack";
    private static final String CT = "net/minecraft/nbt/CompoundTag";
    private static final String BE = "net/minecraft/world/level/block/entity/BlockEntity";
    private static final String PROVIDER = "net/minecraft/core/HolderLookup$Provider";

    private static ClassNode cls(String name, String sup) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = sup;
        return n;
    }

    private static MethodNode method(ClassNode n, int access, String name, String desc) {
        MethodNode m = new MethodNode(access, name, desc, null, null);
        n.methods.add(m);
        return m;
    }

    private static MethodInsnNode find(MethodNode m, String owner, String name) {
        for (AbstractInsnNode in : m.instructions) {
            if (in instanceof MethodInsnNode c && c.owner.equals(owner) && c.name.equals(name)) return c;
        }
        return null;
    }

    private static MethodNode findDeclared(ClassNode node, String name, String descriptor) {
        for (MethodNode method : node.methods) {
            if (name.equals(method.name) && descriptor.equals(method.desc)) return method;
        }
        return null;
    }

    private static Mc1201Hierarchy hierarchy(ClassNode... nodes) {
        Mc1201Hierarchy h = new Mc1201Hierarchy();
        for (ClassNode n : nodes) h.add(n);
        return h;
    }

    private static Mc1201Migration.Counts apply(ClassNode... nodes) {
        Mc1201Hierarchy h = new Mc1201Hierarchy();
        for (ClassNode n : nodes) h.add(n);
        Mc1201Migration.Counts total = new Mc1201Migration.Counts();
        for (ClassNode n : nodes) total.add(Mc1201Migration.apply(n, h));
        return total;
    }

    @Test
    void constructorBecomesFactoryCall() {
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "make", "(L" + ME + ";)L" + MEI + ";");
        m.instructions.add(new TypeInsnNode(NEW, MEI));
        m.instructions.add(new InsnNode(DUP));
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new org.objectweb.asm.tree.IntInsnNode(BIPUSH, 20));
        m.instructions.add(new InsnNode(ICONST_1));
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL, MEI, "<init>", "(L" + ME + ";II)V", false));
        m.instructions.add(new InsnNode(ARETURN));
        Mc1201Migration.Counts counts = apply(c);
        assertEquals(1, counts.constructors);
        MethodInsnNode call = find(m, MIGRATE + "HolderCompat", "newEffect");
        assertNotNull(call);
        assertEquals(INVOKESTATIC, call.getOpcode());
        assertEquals("(L" + ME + ";II)L" + MEI + ";", call.desc);
        for (AbstractInsnNode in : m.instructions) assertFalse(in instanceof TypeInsnNode, "NEW and DUP are gone");
    }

    @Test
    void mobBucketConstructorBecomesLegacyFactoryCall() {
        String mobBucket = "net/minecraft/world/item/MobBucketItem";
        String supplier = "java/util/function/Supplier";
        String properties = "net/minecraft/world/item/Item$Properties";
        String oldDesc = "(L" + supplier + ";L" + supplier + ";L" + supplier + ";L" + properties + ";)V";
        String factoryDesc = "(L" + supplier + ";L" + supplier + ";L" + supplier + ";L" + properties + ";)L" + mobBucket + ";";

        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "make", factoryDesc);
        m.instructions.add(new TypeInsnNode(NEW, mobBucket));
        m.instructions.add(new InsnNode(DUP));
        for (int i = 0; i < 4; i++) m.instructions.add(new VarInsnNode(ALOAD, i));
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL, mobBucket, "<init>", oldDesc, false));
        m.instructions.add(new InsnNode(ARETURN));
        m.maxLocals = 4;
        m.maxStack = 6;

        assertEquals(1, apply(c).constructors);
        MethodInsnNode factory = find(m, MIGRATE + "LegacyMobBucketItem", "create");
        assertNotNull(factory);
        assertEquals(INVOKESTATIC, factory.getOpcode());
        assertEquals(factoryDesc, factory.desc);
        for (AbstractInsnNode in : m.instructions) {
            assertFalse(in instanceof TypeInsnNode, "the original NEW must be replaced by the factory");
            assertFalse(in.getOpcode() == DUP, "the constructor's DUP must be removed with NEW");
        }
    }

    @Test
    void mobBucketSubclassUsesLegacyStandInForItsSupplierSuperConstructor() {
        String mobBucket = "net/minecraft/world/item/MobBucketItem";
        String standIn = MIGRATE + "LegacyMobBucketItem";
        String supplier = "java/util/function/Supplier";
        String properties = "net/minecraft/world/item/Item$Properties";
        String desc = "(L" + supplier + ";L" + supplier + ";L" + supplier + ";L" + properties + ";)V";

        ClassNode c = cls("test/FishBucket", mobBucket);
        MethodNode init = method(c, ACC_PUBLIC, "<init>", desc);
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        for (int i = 1; i <= 4; i++) init.instructions.add(new VarInsnNode(ALOAD, i));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, mobBucket, "<init>", desc, false));
        init.instructions.add(new InsnNode(RETURN));
        init.maxLocals = 5;
        init.maxStack = 5;

        assertEquals(0, apply(c).constructors, "the supplier super call stays a constructor call on the stand-in");
        assertEquals(standIn, c.superName);
        MethodInsnNode superCall = find(init, standIn, "<init>");
        assertNotNull(superCall);
        assertEquals(INVOKESPECIAL, superCall.getOpcode());
        assertEquals(desc, superCall.desc);
    }

    @Test
    void holderFieldIsUnwrapped() {
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "poison", "()L" + ME + ";");
        m.instructions.add(new FieldInsnNode(GETSTATIC, "net/minecraft/world/effect/MobEffects", "POISON", "L" + ME + ";"));
        m.instructions.add(new InsnNode(ARETURN));
        assertEquals(1, apply(c).fields);
        FieldInsnNode f = (FieldInsnNode) m.instructions.getFirst();
        assertEquals("Lnet/minecraft/core/Holder;", f.desc);
        assertEquals("value", ((MethodInsnNode) f.getNext()).name);
        assertEquals(CHECKCAST, f.getNext().getNext().getOpcode());
    }

    @Test
    void droppedInstanceFieldBecomesHelperCalls() {
        String opts = "net/minecraft/client/Options";
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "toggle", "(L" + opts + ";)V");
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new FieldInsnNode(GETFIELD, opts, "renderDebug", "Z"));
        m.instructions.add(new InsnNode(ICONST_1));
        m.instructions.add(new InsnNode(IXOR));
        m.instructions.add(new FieldInsnNode(PUTFIELD, opts, "renderDebug", "Z"));
        m.instructions.add(new InsnNode(RETURN));
        assertEquals(2, apply(c).fields);
        MethodInsnNode get = find(m, MIGRATE + "ClientCompat", "getRenderDebug");
        MethodInsnNode set = find(m, MIGRATE + "ClientCompat", "setRenderDebug");
        assertNotNull(get);
        assertNotNull(set);
        assertEquals("(L" + opts + ";)Z", get.desc);
        assertEquals("(L" + opts + ";Z)V", set.desc);
    }

    @Test
    void cauldronMapFieldIsUnwrappedAndRecipeRemainderIsConverted() {
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "water", "()Ljava/util/Map;");
        m.instructions.add(new FieldInsnNode(GETSTATIC, "net/minecraft/core/cauldron/CauldronInteraction", "WATER", "Ljava/util/Map;"));
        m.instructions.add(new InsnNode(ARETURN));
        assertEquals(1, apply(c).fields);
        FieldInsnNode f = (FieldInsnNode) m.instructions.getFirst();
        assertEquals("Lnet/minecraft/core/cauldron/CauldronInteraction$InteractionMap;", f.desc);
        assertNotNull(find(m, MIGRATE + "MiscCompat", "cauldronMap"));

        ClassNode r = cls("test/Rec", "java/lang/Object");
        MethodNode n = method(r, ACC_PUBLIC | ACC_STATIC, "rest", "(Lnet/minecraft/world/item/crafting/CraftingRecipe;Lnet/minecraft/world/Container;)Lnet/minecraft/core/NonNullList;");
        n.instructions.add(new VarInsnNode(ALOAD, 0));
        n.instructions.add(new VarInsnNode(ALOAD, 1));
        n.instructions.add(new MethodInsnNode(INVOKEINTERFACE, "net/minecraft/world/item/crafting/CraftingRecipe", "getRemainingItems",
                "(Lnet/minecraft/world/Container;)Lnet/minecraft/core/NonNullList;", true));
        n.instructions.add(new InsnNode(ARETURN));
        assertEquals(1, apply(r).calls);
        assertNotNull(find(n, MIGRATE + "RecipeCompat", "getRemainingItems"));
    }

    @Test
    void itemStackNbtCallsGoToTheHelper() {
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "tag", "(L" + STACK + ";)L" + CT + ";");
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, STACK, "getOrCreateTag", "()L" + CT + ";", false));
        m.instructions.add(new InsnNode(ARETURN));
        assertEquals(1, apply(c).calls);
        MethodInsnNode call = find(m, MIGRATE + "ItemStackNbt", "getOrCreateTag");
        assertNotNull(call);
        assertEquals("(L" + STACK + ";)L" + CT + ";", call.desc);
    }

    @Test
    void callOnAModSubclassOfMinecraftIsFoundThroughTheHierarchy() {
        // a call through a subclass of the declaring class (Player.hasEffect is LivingEntity's)
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "has", "(Lnet/minecraft/world/entity/player/Player;L" + ME + ";)Z");
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new VarInsnNode(ALOAD, 1));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/world/entity/player/Player", "hasEffect", "(L" + ME + ";)Z", false));
        m.instructions.add(new InsnNode(IRETURN));
        assertEquals(1, apply(c).calls);
        MethodInsnNode call = find(m, MIGRATE + "HolderCompat", "hasEffect");
        assertNotNull(call);
        assertEquals("(Lnet/minecraft/world/entity/LivingEntity;L" + ME + ";)Z", call.desc);
    }

    @Test
    void legacyAbstractWidgetNarrationOverridesUseTheNewWidgetHook() {
        String selectionList = "net/minecraft/client/gui/components/AbstractSelectionList";
        ClassNode list = cls("test/LegacyList", selectionList);
        String descriptor = "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";
        MethodNode legacy = method(list, ACC_PUBLIC, "updateNarration", descriptor);
        legacy.instructions.add(new InsnNode(RETURN));
        legacy.maxStack = 0;
        legacy.maxLocals = 2;

        Mc1201Migration.Counts counts = apply(list);
        assertEquals(1, counts.widgetNarrationOverrides);
        assertNull(findDeclared(list, "updateNarration", descriptor));
        MethodNode modern = findDeclared(list, "updateWidgetNarration", descriptor);
        assertNotNull(modern);
        assertEquals(ACC_PUBLIC, modern.access & (ACC_PUBLIC | ACC_PROTECTED | ACC_PRIVATE));
        assertEquals(0, Mc1201Migration.apply(list, hierarchy(list)).widgetNarrationOverrides,
                "the widget narration migration must be idempotent");
    }

    @Test
    void unrelatedNonWidgetNarrationMethodsAreNotRenamed() {
        ClassNode other = cls("test/OtherNarratable", "java/lang/Object");
        String descriptor = "(Lnet/minecraft/client/gui/narration/NarrationElementOutput;)V";
        MethodNode legacy = method(other, ACC_PUBLIC, "updateNarration", descriptor);
        legacy.instructions.add(new InsnNode(RETURN));

        Mc1201Migration.Counts counts = apply(other);
        assertEquals(0, counts.widgetNarrationOverrides);
        assertNotNull(findDeclared(other, "updateNarration", descriptor));
        assertNull(findDeclared(other, "updateWidgetNarration", descriptor));
    }

    @Test
    void legacySelectionListSetLeftPosCallsUseTheWidgetXSetter() {
        String selectionList = "net/minecraft/client/gui/components/AbstractSelectionList";
        ClassNode list = cls("test/MySelectionList", selectionList);
        ClassNode caller = cls("test/Caller", "java/lang/Object");
        MethodNode move = method(caller, ACC_PUBLIC | ACC_STATIC, "move", "(Ltest/MySelectionList;I)V");
        move.instructions.add(new VarInsnNode(ALOAD, 0));
        move.instructions.add(new VarInsnNode(ILOAD, 1));
        move.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "test/MySelectionList",
                "setLeftPos", "(I)V", false));
        move.instructions.add(new InsnNode(RETURN));

        Mc1201Migration.Counts counts = apply(list, caller);
        assertEquals(1, counts.selectionListPositionCalls);
        assertNotNull(find(move, "test/MySelectionList", "setX"));
        assertNull(find(move, "test/MySelectionList", "setLeftPos"));
    }

    @Test
    void legacySelectionListPositionMigrationAlsoRecognizesObjectSelectionListSubclasses() {
        String selectionList = "net/minecraft/client/gui/components/AbstractSelectionList";
        String objectSelectionList = "net/minecraft/client/gui/components/ObjectSelectionList";
        ClassNode list = cls("test/NeoForgeList", objectSelectionList);
        ClassNode objectListBase = cls(objectSelectionList, selectionList);
        ClassNode caller = cls("test/Caller", "java/lang/Object");
        MethodNode move = method(caller, ACC_PUBLIC | ACC_STATIC, "move", "(Ltest/NeoForgeList;I)V");
        move.instructions.add(new VarInsnNode(ALOAD, 0));
        move.instructions.add(new VarInsnNode(ILOAD, 1));
        move.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "test/NeoForgeList",
                "setLeftPos", "(I)V", false));
        move.instructions.add(new InsnNode(RETURN));

        Mc1201Migration.Counts counts = apply(objectListBase, list, caller);
        assertEquals(1, counts.selectionListPositionCalls);
        assertNotNull(find(move, "test/NeoForgeList", "setX"));
        assertNull(find(move, "test/NeoForgeList", "setLeftPos"));
    }

    @Test
    void unrelatedSetLeftPosMethodIsNotRenamed() {
        ClassNode other = cls("test/Other", "java/lang/Object");
        ClassNode caller = cls("test/Caller", "java/lang/Object");
        MethodNode move = method(caller, ACC_PUBLIC | ACC_STATIC, "move", "(Ltest/Other;I)V");
        move.instructions.add(new VarInsnNode(ALOAD, 0));
        move.instructions.add(new VarInsnNode(ILOAD, 1));
        move.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "test/Other",
                "setLeftPos", "(I)V", false));
        move.instructions.add(new InsnNode(RETURN));

        Mc1201Migration.Counts counts = apply(other, caller);
        assertEquals(0, counts.selectionListPositionCalls);
        assertNotNull(find(move, "test/Other", "setLeftPos"));
        assertNull(find(move, "test/Other", "setX"));
    }

    @Test
    void directMinecraftCallsUseRenamedMemberTable() {
        String gameRenderer = "net/minecraft/client/renderer/GameRenderer";
        String shaderInstance = "net/minecraft/client/renderer/ShaderInstance";
        String desc = "()L" + shaderInstance + ";";
        ClassNode c = cls("test/PalladiumIcon", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "shader", desc);
        m.instructions.add(new MethodInsnNode(INVOKESTATIC, gameRenderer, "getPositionColorTexShader", desc, false));
        m.instructions.add(new InsnNode(ARETURN));

        assertEquals(1, apply(c).calls);
        assertNotNull(find(m, gameRenderer, "getPositionTexColorShader"),
                "the 1.20.1 shader getter must be renamed to its 1.21.1 equivalent");
        verify(c);
    }

    @Test
    void legacyScoreboardQueriesUseThe1_21_1ScoreHolderApi() {
        String scoreboard = "net/minecraft/world/scores/Scoreboard";
        String objective = "net/minecraft/world/scores/Objective";
        ClassNode c = cls("test/ScoreboardCalls", "java/lang/Object");

        MethodNode has = method(c, ACC_PUBLIC | ACC_STATIC, "hasScore",
                "(L" + scoreboard + ";Ljava/lang/String;L" + objective + ";)Z");
        has.instructions.add(new VarInsnNode(ALOAD, 0));
        has.instructions.add(new VarInsnNode(ALOAD, 1));
        has.instructions.add(new VarInsnNode(ALOAD, 2));
        has.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, scoreboard, "hasPlayerScore",
                "(Ljava/lang/String;L" + objective + ";)Z", false));
        has.instructions.add(new InsnNode(IRETURN));

        MethodNode scores = method(c, ACC_PUBLIC | ACC_STATIC, "scores",
                "(L" + scoreboard + ";Ljava/lang/String;)Ljava/util/Map;");
        scores.instructions.add(new VarInsnNode(ALOAD, 0));
        scores.instructions.add(new VarInsnNode(ALOAD, 1));
        scores.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, scoreboard, "getPlayerScores",
                "(Ljava/lang/String;)Ljava/util/Map;", false));
        scores.instructions.add(new InsnNode(ARETURN));

        assertEquals(2, apply(c).calls);

        MethodInsnNode hasHelper = find(has, MIGRATE + "MiddleCompat", "hasPlayerScore");
        assertNotNull(hasHelper);
        assertEquals(INVOKESTATIC, hasHelper.getOpcode());
        assertEquals("(L" + scoreboard + ";Ljava/lang/String;L" + objective + ";)Z", hasHelper.desc);

        MethodInsnNode scoresHelper = find(scores, MIGRATE + "MiddleCompat", "playerScores");
        assertNotNull(scoresHelper);
        assertEquals(INVOKESTATIC, scoresHelper.getOpcode());
        assertEquals("(L" + scoreboard + ";Ljava/lang/String;)Ljava/util/Map;", scoresHelper.desc);
        verify(c);
    }

    @Test
    void aMethodOfTheModKeepsItsSignature() {
        // the mod's own getTag() on its own class must not be sent to the ItemStack helper
        ClassNode holder = cls("test/MyStack", "java/lang/Object");
        method(holder, ACC_PUBLIC, "getTag", "()L" + CT + ";").instructions.add(new InsnNode(ACONST_NULL));
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "tag", "(Ltest/MyStack;)L" + CT + ";");
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "test/MyStack", "getTag", "()L" + CT + ";", false));
        m.instructions.add(new InsnNode(ARETURN));
        assertEquals(0, apply(holder, c).calls);
        assertNull(find(m, MIGRATE + "ItemStackNbt", "getTag"));
    }

    @Test
    void modMethodShadowingAnItemStackMethodOnASubclassIsKept() {
        // a mod subclass of Item that declares getMaxDamage() keeps it; Item's own is redirected
        ClassNode item = cls("test/MyItem", "net/minecraft/world/item/Item");
        MethodNode own = method(item, ACC_PUBLIC, "getMaxDamage", "()I");
        own.instructions.add(new InsnNode(ICONST_1));
        own.instructions.add(new InsnNode(IRETURN));
        ClassNode c = cls("test/Mod", "java/lang/Object");
        MethodNode m = method(c, ACC_PUBLIC | ACC_STATIC, "max", "(Ltest/MyItem;Lnet/minecraft/world/item/Item;)I");
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "test/MyItem", "getMaxDamage", "()I", false));
        m.instructions.add(new VarInsnNode(ALOAD, 1));
        m.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, "net/minecraft/world/item/Item", "getMaxDamage", "()I", false));
        m.instructions.add(new InsnNode(IADD));
        m.instructions.add(new InsnNode(IRETURN));
        assertEquals(1, apply(item, c).calls);
        assertNotNull(find(m, "test/MyItem", "getMaxDamage"));
        assertNotNull(find(m, MIGRATE + "MiscCompat", "getMaxDamage"));
    }

    @Test
    void blockEntityOverrideGetsABridgeAndSuperCallsGetTheProvider() {
        ClassNode c = cls("test/MyBE", BE);
        MethodNode load = method(c, ACC_PUBLIC, "load", "(L" + CT + ";)V");
        load.instructions.add(new VarInsnNode(ALOAD, 0));
        load.instructions.add(new VarInsnNode(ALOAD, 1));
        load.instructions.add(new MethodInsnNode(INVOKESPECIAL, BE, "load", "(L" + CT + ";)V", false));
        load.instructions.add(new InsnNode(RETURN));
        MethodNode save = method(c, ACC_PROTECTED, "saveAdditional", "(L" + CT + ";)V");
        save.instructions.add(new InsnNode(RETURN));

        Mc1201Migration.Counts counts = apply(c);
        assertEquals(2, counts.bridges);
        assertEquals(1, counts.providerCalls);

        // the super call now passes the provider and calls the 1.21.1 method
        MethodInsnNode sup = find(load, BE, "loadAdditional");
        assertNotNull(sup);
        assertEquals("(L" + CT + ";L" + PROVIDER + ";)V", sup.desc);
        assertNotNull(find(load, MIGRATE + "MigrateContext", "provider"));

        // the bridge: remembers the provider, calls the mod's load
        MethodNode bridge = null;
        for (MethodNode m : c.methods) if (m.name.equals("loadAdditional")) bridge = m;
        assertNotNull(bridge);
        assertEquals("(L" + CT + ";L" + PROVIDER + ";)V", bridge.desc);
        assertNotNull(find(bridge, MIGRATE + "MigrateContext", "remember"));
        MethodInsnNode back = find(bridge, "test/MyBE", "load");
        assertNotNull(back);
        assertEquals(INVOKEVIRTUAL, back.getOpcode());
        assertEquals("(L" + CT + ";)V", back.desc);
        verify(c);
    }

    @Test
    void bridgeIsNotAddedWhenTheModAlreadyUsesTheNewSignature() {
        ClassNode c = cls("test/NewBE", BE);
        method(c, ACC_PUBLIC, "load", "(L" + CT + ";)V").instructions.add(new InsnNode(RETURN));
        method(c, ACC_PUBLIC, "loadAdditional", "(L" + CT + ";L" + PROVIDER + ";)V").instructions.add(new InsnNode(RETURN));
        assertEquals(0, apply(c).bridges);
    }

    @Test
    void bridgeNeedsABlockEntityBelow() {
        ClassNode c = cls("test/Plain", "java/lang/Object");
        method(c, ACC_PUBLIC, "load", "(L" + CT + ";)V").instructions.add(new InsnNode(RETURN));
        assertEquals(0, apply(c).bridges);
    }

    @Test
    void blockUseBridgesToUseItemOnWithAResultAdapter() {
        String result = "net/minecraft/world/InteractionResult";
        String desc = "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;"
                + "Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/InteractionHand;Lnet/minecraft/world/phys/BlockHitResult;)L" + result + ";";
        ClassNode c = cls("test/MyBlock", "net/minecraft/world/level/block/Block");
        MethodNode use = method(c, ACC_PUBLIC, "use", desc);
        use.instructions.add(new InsnNode(ACONST_NULL));
        use.instructions.add(new InsnNode(ARETURN));
        assertEquals(1, apply(c).bridges);
        MethodNode bridge = null;
        for (MethodNode m : c.methods) if (m.name.equals("useItemOn")) bridge = m;
        assertNotNull(bridge);
        assertEquals("(Lnet/minecraft/world/item/ItemStack;" + desc.substring(1, desc.indexOf(')')) + ")Lnet/minecraft/world/ItemInteractionResult;", bridge.desc);
        assertNotNull(find(bridge, MIGRATE + "BridgeHelpers", "toItemResult"));
        verify(c);
    }

    @Test
    void doorConstructorArgumentsAreSwapped() {
        String props = "net/minecraft/world/level/block/state/BlockBehaviour$Properties";
        String type = "net/minecraft/world/level/block/state/properties/BlockSetType";
        String door = "net/minecraft/world/level/block/DoorBlock";
        ClassNode c = cls("test/MyDoor", door);
        MethodNode init = method(c, ACC_PUBLIC, "<init>", "(L" + props + ";L" + type + ";)V");
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new VarInsnNode(ALOAD, 1));
        init.instructions.add(new VarInsnNode(ALOAD, 2));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, door, "<init>", "(L" + props + ";L" + type + ";)V", false));
        init.instructions.add(new InsnNode(RETURN));
        assertEquals(1, apply(c).constructors);
        MethodInsnNode sup = find(init, door, "<init>");
        assertEquals("(L" + type + ";L" + props + ";)V", sup.desc);
        assertEquals(SWAP, sup.getPrevious().getOpcode());
    }

    @Test
    void armorItemConstructorGetsAHolderAndDurability() {
        String mat = "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyArmorMaterial";
        String type = "net/minecraft/world/item/ArmorItem$Type";
        String props = "net/minecraft/world/item/Item$Properties";
        String armor = "net/minecraft/world/item/ArmorItem";
        ClassNode c = cls("test/MyArmor", armor);
        MethodNode init = method(c, ACC_PUBLIC, "<init>", "(L" + mat + ";L" + type + ";L" + props + ";)V");
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new VarInsnNode(ALOAD, 1));
        init.instructions.add(new VarInsnNode(ALOAD, 2));
        init.instructions.add(new VarInsnNode(ALOAD, 3));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, armor, "<init>", "(L" + mat + ";L" + type + ";L" + props + ";)V", false));
        init.instructions.add(new InsnNode(RETURN));
        init.maxLocals = 4;
        init.maxStack = 4;
        assertEquals(1, apply(c).constructors);
        MethodInsnNode sup = find(init, armor, "<init>");
        assertEquals("(Lnet/minecraft/core/Holder;L" + type + ";L" + props + ";)V", sup.desc);
        MethodInsnNode holder = find(init, MIGRATE + "ArmorCompat", "holder");
        assertNotNull(holder);
        assertEquals("(L" + mat + ";)Lnet/minecraft/core/Holder;", holder.desc);
        MethodInsnNode properties = find(init, MIGRATE + "ArmorCompat", "properties");
        assertNotNull(properties);
        assertEquals("(L" + mat + ";L" + type + ";L" + props + ";)L" + props + ";", properties.desc);
        assertTrue(init.maxLocals >= 7, "three argument spill slots");
    }

    @Test
    void customRecipeDropsItsId() {
        String custom = "net/minecraft/world/item/crafting/CustomRecipe";
        String rl = "net/minecraft/resources/ResourceLocation";
        String cat = "net/minecraft/world/item/crafting/CraftingBookCategory";
        ClassNode c = cls("test/MyRecipe", custom);
        MethodNode init = method(c, ACC_PUBLIC, "<init>", "(L" + rl + ";L" + cat + ";)V");
        init.instructions.add(new VarInsnNode(ALOAD, 0));
        init.instructions.add(new VarInsnNode(ALOAD, 1));
        init.instructions.add(new VarInsnNode(ALOAD, 2));
        init.instructions.add(new MethodInsnNode(INVOKESPECIAL, custom, "<init>", "(L" + rl + ";L" + cat + ";)V", false));
        init.instructions.add(new InsnNode(RETURN));
        apply(c);
        MethodInsnNode sup = find(init, custom, "<init>");
        assertEquals("(L" + cat + ";)V", sup.desc);
        assertEquals(POP, sup.getPrevious().getOpcode());
        assertEquals(SWAP, sup.getPrevious().getPrevious().getOpcode());
    }

    @Test
    void tierImplementationGetsTheBlockTagMethod() {
        ClassNode c = cls("test/MyTier", "java/lang/Object");
        c.interfaces.add("net/minecraft/world/item/Tier");
        MethodNode level = method(c, ACC_PUBLIC, "getLevel", "()I");
        level.instructions.add(new InsnNode(ICONST_2));
        level.instructions.add(new InsnNode(IRETURN));
        assertEquals(1, apply(c).bridges);
        boolean found = false;
        for (var m : c.methods) found |= m.name.equals("getIncorrectBlocksForDrops") && m.desc.equals("()Lnet/minecraft/tags/TagKey;");
        assertTrue(found);
    }

    @Test
    void voidSuperCallThatNowReturnsAValueIsPopped() {
        String block = "net/minecraft/world/level/block/Block";
        String level = "net/minecraft/world/level/Level";
        String pos = "net/minecraft/core/BlockPos";
        String state = "net/minecraft/world/level/block/state/BlockState";
        String player = "net/minecraft/world/entity/player/Player";
        String oldDesc = "(L" + level + ";L" + pos + ";L" + state + ";L" + player + ";)V";
        ClassNode c = cls("test/MyBlock", block);
        MethodNode m = method(c, ACC_PUBLIC, "x", oldDesc);
        m.instructions.add(new VarInsnNode(ALOAD, 0));
        for (int i = 1; i <= 4; i++) m.instructions.add(new VarInsnNode(ALOAD, i));
        m.instructions.add(new MethodInsnNode(INVOKESPECIAL, block, "playerWillDestroy", oldDesc, false));
        m.instructions.add(new InsnNode(RETURN));
        apply(c);
        MethodInsnNode call = find(m, block, "playerWillDestroy");
        assertEquals("(L" + level + ";L" + pos + ";L" + state + ";L" + player + ";)L" + state + ";", call.desc);
        assertEquals(POP, call.getNext().getOpcode());
    }

    @Test
    void overrideOfFinalAbstractWidgetRenderMovesToRenderWidget() {
        String widget = "net/minecraft/client/gui/components/AbstractWidget";
        String graphics = "net/minecraft/client/gui/GuiGraphics";
        String desc = "(L" + graphics + ";IIF)V";
        ClassNode c = cls("test/RotatingIconButton", widget);

        MethodNode render = method(c, ACC_PUBLIC, "render", desc);
        render.instructions.add(new InsnNode(RETURN));
        render.maxStack = 0;
        render.maxLocals = 5;

        // A same-class virtual call must follow the renamed override too.
        MethodNode invoke = method(c, ACC_PUBLIC, "renderFromCallback", desc);
        invoke.instructions.add(new VarInsnNode(ALOAD, 0));
        invoke.instructions.add(new VarInsnNode(ALOAD, 1));
        invoke.instructions.add(new VarInsnNode(ILOAD, 2));
        invoke.instructions.add(new VarInsnNode(ILOAD, 3));
        invoke.instructions.add(new VarInsnNode(FLOAD, 4));
        invoke.instructions.add(new MethodInsnNode(INVOKEVIRTUAL, c.name, "render", desc, false));
        invoke.instructions.add(new InsnNode(RETURN));
        invoke.maxStack = 5;
        invoke.maxLocals = 5;

        assertEquals(1, apply(c).bridges);

        MethodNode renamed = null;
        for (MethodNode m : c.methods) {
            if (m.name.equals("renderWidget") && m.desc.equals(desc)) renamed = m;
        }
        assertNotNull(renamed, "the old override must use AbstractWidget's 1.21.1 renderWidget hook");
        assertNotNull(find(invoke, c.name, "renderWidget"), "calls to the renamed override must follow it");
        assertNull(find(invoke, c.name, "render"), "the obsolete final render method must not be called");
        verify(c);
    }

    @Test
    void migratedClassesStillVerify() {
        ClassNode c = cls("test/MyBE", BE);
        MethodNode load = method(c, ACC_PUBLIC, "load", "(L" + CT + ";)V");
        load.instructions.add(new InsnNode(RETURN));
        load.maxStack = 2;
        load.maxLocals = 2;
        apply(c);
        verify(c);
    }

    private static void verify(ClassNode c) {
        // structural check only (frames are not required for these straight-line methods at class version 17 when no branch exists)
        ClassWriter w = new ClassWriter(0);
        c.accept(w);
        assertTrue(w.toByteArray().length > 0);
        new ClassReader(w.toByteArray()).accept(new ClassNode(), 0);
    }
}
