package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.transform.mixin1201.ParamAlignment;
import org.kylepitcock.legacyconnector.loader.transform.mixin1201.Refmaps;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/**
 * The mixin target proof of the 1.20.1 translation against the real 1.21.1 member index: each class of case (exact, signature changed and repaired, gone,
 * unrepairable) on small synthetic mixins, including the shape of issue #250 (a leading parameter added to {@code ItemStack.getTooltipLines}).
 */
class Mixin1201RepairTest implements Opcodes {
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String CIR = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfoReturnable;";
    private static final String CI = "Lorg/spongepowered/asm/mixin/injection/callback/CallbackInfo;";
    private static final String PLAYER = "Lnet/minecraft/world/entity/player/Player;";
    private static final String FLAG = "Lnet/minecraft/world/item/TooltipFlag;";
    private static final String CONTEXT = "Lnet/minecraft/world/item/Item$TooltipContext;";
    private static final String ITEM_STACK = "net/minecraft/world/item/ItemStack";
    private static final String ENTITY = "net/minecraft/world/entity/Entity";

    private static ClassNode mixin(String name, String target) {
        ClassNode n = new ClassNode();
        n.version = V17;
        n.access = ACC_PUBLIC;
        n.name = name;
        n.superName = "java/lang/Object";
        AnnotationNode a = new AnnotationNode("Lorg/spongepowered/asm/mixin/Mixin;");
        a.values = new ArrayList<>(List.of("value", new ArrayList<Object>(List.of(Type.getObjectType(target)))));
        n.invisibleAnnotations = new ArrayList<>(List.of(a));
        return n;
    }

    /** A handler that reads each of its parameters (so slots matter) and returns. */
    private static MethodNode handler(ClassNode n, String name, String desc) {
        MethodNode m = new MethodNode(ACC_PRIVATE, name, desc, null, null);
        int slot = 1;
        for (Type t : Type.getArgumentTypes(desc)) {
            m.instructions.add(new VarInsnNode(t.getOpcode(ILOAD), slot));
            m.instructions.add(new InsnNode(t.getSize() == 2 ? POP2 : POP));
            slot += t.getSize();
        }
        m.instructions.add(new InsnNode(RETURN));
        m.maxLocals = slot;
        m.maxStack = 2;
        n.methods.add(m);
        return m;
    }

    private static AnnotationNode injector(MethodNode m, String annotation, String selector, String atValue, String atTarget) {
        AnnotationNode a = new AnnotationNode(annotation);
        a.values = new ArrayList<>(List.of("method", new ArrayList<Object>(List.of(selector))));
        AnnotationNode at = new AnnotationNode("Lorg/spongepowered/asm/mixin/injection/At;");
        at.values = new ArrayList<>(atTarget == null ? List.of("value", atValue) : List.of("value", atValue, "target", atTarget));
        a.values.add("at");
        a.values.add(new ArrayList<Object>(List.of(at)));
        if (m.invisibleAnnotations == null) m.invisibleAnnotations = new ArrayList<>();
        m.invisibleAnnotations.add(a);
        return a;
    }

    private static Mixin1201.Stats run(ClassNode n) {
        Mixin1201.Stats st = new Mixin1201.Stats();
        Mixin1201.apply(n, Map.of(), new Mc1201Hierarchy(), Mc1201Tables.get(), st);
        return st;
    }

    private static List<?> methods(AnnotationNode a) {
        return (List<?>) a.values.get(1);
    }

    private static MethodNode find(ClassNode n, String name) {
        for (MethodNode m : n.methods) if (m.name.equals(name)) return m;
        return null;
    }

    @Test
    void leadingParameterAddedToTheTarget_issue250() {
        ClassNode n = mixin("m/TooltipMixin", ITEM_STACK);
        // 1.20.1: getTooltipLines(Player, TooltipFlag)
        MethodNode h = handler(n, "onTooltip", "(" + PLAYER + FLAG + CIR + ")V");
        AnnotationNode a = injector(h, INJECT, "getTooltipLines", "RETURN", null);
        Mixin1201.Stats st = run(n);
        assertEquals("(" + CONTEXT + PLAYER + FLAG + CIR + ")V", h.desc, "the handler receives the added TooltipContext (ignored) first");
        assertEquals(1, st.repair.repaired);
        assertEquals(1, st.repair.handlersChanged);
        assertEquals("getTooltipLines", methods(a).get(0), "a single overload: the selector may stay a bare name");
        // the old parameters now sit one slot higher: player 2, flag 3, callback info 4
        List<Integer> loads = new ArrayList<>();
        for (var in = h.instructions.getFirst(); in != null; in = in.getNext()) if (in instanceof VarInsnNode v) loads.add(v.var);
        assertEquals(List.of(2, 3, 4), loads);
        assertTrue(h.maxLocals >= 5);
        assertTrue(st.repair.notes.stream().anyMatch(s -> s.contains("onTooltip") && s.contains("re-signed")));
    }

    @Test
    void descriptorSelectorFollowsTheAddedParameter() {
        ClassNode n = mixin("m/TooltipMixin2", ITEM_STACK);
        MethodNode h = handler(n, "onTooltip", "(" + PLAYER + FLAG + CIR + ")V");
        AnnotationNode a = injector(h, INJECT, "getTooltipLines(" + PLAYER + FLAG + ")Ljava/util/List;", "HEAD", null);
        run(n);
        assertEquals("getTooltipLines(" + CONTEXT + PLAYER + FLAG + ")Ljava/util/List;", methods(a).get(0));
        assertEquals("(" + CONTEXT + PLAYER + FLAG + CIR + ")V", h.desc);
    }

    @Test
    void exactNameIsLeftAlone() {
        ClassNode n = mixin("m/TickMixin", ENTITY);
        MethodNode h = handler(n, "onTick", "(" + CI + ")V");
        AnnotationNode a = injector(h, INJECT, "tick", "HEAD", null);
        Mixin1201.Stats st = run(n);
        assertEquals("(" + CI + ")V", h.desc);
        assertEquals("tick", methods(a).get(0));
        assertEquals(1, st.repair.exact);
        assertEquals(0, st.repair.dropped);
        assertTrue(n.methods.contains(h));
    }

    @Test
    void injectionIntoAMethodThatIsGoneIsDroppedAlone() {
        ClassNode n = mixin("m/GoneMixin", ENTITY);
        MethodNode gone = handler(n, "onGone", "(" + CI + ")V");
        injector(gone, INJECT, "noSuchMethodInAnyMinecraft", "HEAD", null);
        MethodNode ok = handler(n, "onTick", "(" + CI + ")V");
        injector(ok, INJECT, "tick", "HEAD", null);
        Mixin1201.Stats st = run(n);
        assertEquals(1, st.repair.dropped);
        assertFalse(n.methods.contains(gone), "the handler of the dead injection is removed");
        assertTrue(n.methods.contains(ok), "the other injection stays");
        assertTrue(st.droppedClasses.isEmpty(), "the mixin class and the mod stay");
        assertTrue(st.repair.notes.stream().anyMatch(s -> s.contains("GoneMixin.onGone") && s.contains("dropped") && s.contains("noSuchMethodInAnyMinecraft")), st.repair.notes.toString());
    }

    @Test
    void oneDeadSelectorOfSeveralIsRemovedFromItsAnnotation() {
        ClassNode n = mixin("m/TwoMixin", ENTITY);
        MethodNode h = handler(n, "onEither", "(" + CI + ")V");
        AnnotationNode a = injector(h, INJECT, "tick", "HEAD", null);
        ((List<Object>) a.values.get(1)).add("alsoNotThere");
        run(n);
        assertEquals(List.of("tick"), methods(a));
        assertTrue(n.methods.contains(h));
    }

    @Test
    void removedParameterIsNotRepairable() {
        ClassNode n = mixin("m/RemovedMixin", ITEM_STACK);
        // a 1.20.1 target with an extra parameter the 1.21.1 method no longer has: not an addition
        MethodNode h = handler(n, "onTooltip", "(" + PLAYER + FLAG + "I" + CIR + ")V");
        injector(h, INJECT, "getTooltipLines", "RETURN", null);
        Mixin1201.Stats st = run(n);
        assertFalse(n.methods.contains(h));
        assertEquals(1, st.repair.dropped);
        assertEquals(0, st.repair.repaired);
    }

    @Test
    void unknownOwnersAreNotJudged() {
        ClassNode n = mixin("m/OtherMixin", "com/example/SomeLibraryClass");
        MethodNode h = handler(n, "onX", "(" + CI + ")V");
        AnnotationNode a = injector(h, INJECT, "whatever", "HEAD", null);
        Mixin1201.Stats st = run(n);
        assertTrue(n.methods.contains(h));
        assertEquals("whatever", methods(a).get(0));
        assertEquals(0, st.repair.dropped + st.repair.repaired);
    }

    @Test
    void injectionPointOfAGoneCallIsDropped() {
        ClassNode n = mixin("m/AtMixin", ENTITY);
        MethodNode h = handler(n, "onCall", "(" + CI + ")V");
        injector(h, INJECT, "tick", "INVOKE", "L" + ENTITY + ";noSuchCallAnywhere()V");
        Mixin1201.Stats st = run(n);
        assertEquals(1, st.repair.atDropped);
        assertFalse(n.methods.contains(h), "an injector whose only injection point is gone cannot apply");
    }

    @Test
    void callTargetThatGainedAParameterIsRetargetedForInject() {
        ClassNode n = mixin("m/AtMixin2", ENTITY);
        MethodNode h = handler(n, "onCall", "(" + CI + ")V");
        AnnotationNode a = injector(h, INJECT, "tick", "INVOKE", "L" + ITEM_STACK + ";getTooltipLines(" + PLAYER + FLAG + ")Ljava/util/List;");
        Mixin1201.Stats st = run(n);
        AnnotationNode at = (AnnotationNode) ((List<?>) a.values.get(3)).get(0);
        assertEquals("L" + ITEM_STACK + ";getTooltipLines(" + CONTEXT + PLAYER + FLAG + ")Ljava/util/List;", at.values.get(3));
        assertEquals(1, st.repair.atRepaired);
        assertEquals("(" + CI + ")V", h.desc, "an @Inject handler does not mirror the call it hooks");
    }

    @Test
    void redirectSelectorRepairedWithoutTouchingTheHandler() {
        ClassNode n = mixin("m/RedirectMixin", ITEM_STACK);
        MethodNode h = handler(n, "redir", "(" + PLAYER + ")V");
        AnnotationNode a = injector(h, "Lorg/spongepowered/asm/mixin/injection/Redirect;", "getTooltipLines(" + PLAYER + FLAG + ")Ljava/util/List;", "INVOKE", "Lfoo;bar()V");
        run(n);
        assertEquals("getTooltipLines(" + CONTEXT + PLAYER + FLAG + ")Ljava/util/List;", methods(a).get(0));
        assertEquals("(" + PLAYER + ")V", h.desc);
    }

    @Test
    void ambiguousAlignmentIsNotClaimed() {
        // (int) to (int, int): the old int could be either one
        assertNull(ParamAlignment.pureInsertions("(I)V", "(II)V"));
        // (Player, Flag) to (Context, Player, Flag): one way
        List<ParamAlignment.Insert> ins = ParamAlignment.pureInsertions("(" + PLAYER + FLAG + ")V", "(" + CONTEXT + PLAYER + FLAG + ")V");
        assertNotNull(ins);
        assertEquals(List.of(new ParamAlignment.Insert(0, CONTEXT)), ins);
        // a changed return type is not an addition
        assertNull(ParamAlignment.pureInsertions("(" + PLAYER + ")V", "(" + CONTEXT + PLAYER + ")I"));
        // a removed parameter is not an addition
        assertNull(ParamAlignment.pureInsertions("(" + PLAYER + FLAG + ")V", "(" + FLAG + ")V"));
    }

    @Test
    void wideParameterShiftsLaterSlots() {
        ClassNode n = mixin("m/WideMixin", ITEM_STACK);
        // a handler body reading a long parameter and a following one: slots must move by the width of the inserted parameter
        MethodNode h = handler(n, "h", "(" + PLAYER + "JI" + CIR + ")V");
        h.visibleParameterAnnotations = null;
        assertTrue(org.kylepitcock.legacyconnector.loader.transform.mixin1201.HandlerResigner.resign(h, List.of(new ParamAlignment.Insert(1, "D"))));
        assertEquals("(" + PLAYER + "DJI" + CIR + ")V", h.desc);
        List<Integer> loads = new ArrayList<>();
        for (var in = h.instructions.getFirst(); in != null; in = in.getNext()) if (in instanceof VarInsnNode v) loads.add(v.var);
        assertEquals(List.of(1, 4, 6, 7), loads, "player stays, the long moves behind the double (2 slots), the int and callback follow");
    }

    @Test
    void localCaptureOfTheInsertedTypeIsRefused() {
        ClassNode n = mixin("m/LocalMixin", ITEM_STACK);
        MethodNode h = handler(n, "h", "(" + PLAYER + FLAG + CIR + PLAYER + ")V");
        // a @Local Player after the callback: the inserted parameter is a Player too, so the capture would become ambiguous
        AnnotationNode local = new AnnotationNode("Lcom/llamalad7/mixinextras/sugar/Local;");
        @SuppressWarnings("unchecked") List<AnnotationNode>[] pa = (List<AnnotationNode>[]) new List<?>[4];
        pa[3] = new ArrayList<>(List.of(local));
        h.invisibleParameterAnnotations = pa;
        assertFalse(org.kylepitcock.legacyconnector.loader.transform.mixin1201.HandlerResigner.canResign(h, List.of(new ParamAlignment.Insert(0, PLAYER))));
        assertTrue(org.kylepitcock.legacyconnector.loader.transform.mixin1201.HandlerResigner.canResign(h, List.of(new ParamAlignment.Insert(0, CONTEXT))));
    }

    @Test
    void instanceHandlerCannotTakeAStaticTargetThatReplacedAnInstanceMethod() {
        // Enchantment.getFullname(int) became the static getFullname(Holder, int): an instance handler cannot be re-signed onto it
        ClassNode n = mixin("m/FullnameMixin", "net/minecraft/world/item/enchantment/Enchantment");
        MethodNode h = handler(n, "onName", "(I" + CIR + ")V");
        injector(h, INJECT, "getFullname", "RETURN", null);
        Mixin1201.Stats st = run(n);
        assertEquals(1, st.repair.dropped);
        assertEquals(0, st.repair.repaired);
        assertFalse(n.methods.contains(h));
    }

    @Test
    void srgAndIntermediaryNamesThatNoTableMovedAreNotJudged() {
        ClassNode n = mixin("m/SrgMixin", ENTITY);
        MethodNode h = handler(n, "onX", "(" + CI + ")V");
        injector(h, INJECT, "m_9999999_", "HEAD", null);
        MethodNode h2 = handler(n, "onY", "(" + CI + ")V");
        injector(h2, INJECT, "method_99999", "HEAD", null);
        Mixin1201.Stats st = run(n);
        assertTrue(n.methods.contains(h) && n.methods.contains(h2));
        assertEquals(0, st.repair.dropped);
        assertEquals(2, st.repair.unproven);
    }

    @Test
    void zeroCaptureHandlerDoesNotFollowAChangedReturnType() {
        // FrogAi.getTemptations() returned an Ingredient in 1.20.1 and returns a Predicate in 1.21.1: the handler would set a value of the wrong type
        ClassNode n = mixin("m/FrogMixin", "net/minecraft/world/entity/animal/frog/FrogAi");
        MethodNode h = handler(n, "onTempt", "(" + CIR + ")V");
        injector(h, INJECT, "getTemptations()Lnet/minecraft/world/item/crafting/Ingredient;", "RETURN", null);
        Mixin1201.Stats st = run(n);
        assertEquals(1, st.repair.dropped);
        assertFalse(n.methods.contains(h));
    }

    @Test
    void ownerQualifiedDefineSynchedDataGetsTheBuilderHandler() {
        ClassNode n = mixin("m/DataMixin", ENTITY);
        MethodNode h = handler(n, "onData", "(" + CI + ")V");
        AnnotationNode a = injector(h, INJECT, "L" + ENTITY + ";defineSynchedData()V", "TAIL", null);
        Mixin1201.Stats st = run(n);
        assertEquals("L" + ENTITY + ";defineSynchedData(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;)V", methods(a).get(0));
        assertEquals("(Lnet/minecraft/network/syncher/SynchedEntityData$Builder;" + CI + ")V", h.desc);
        assertEquals(0, st.repair.dropped);
    }

    @Test
    void refmapDescriptorLetsAPartialHandlerFindItsOverload() {
        // the handler captured only the Player of the 1.20.1 (Player, TooltipFlag) method; the refmap knows the full old descriptor
        ClassNode n = mixin("m/RefmapMixin", ITEM_STACK);
        MethodNode h = handler(n, "onTooltip", "(" + PLAYER + CIR + ")V");
        injector(h, INJECT, "getTooltipLines", "RETURN", null);
        Map<String, byte[]> files = Map.of("m.refmap.json", ("{\"mappings\":{\"m/RefmapMixin\":{\"getTooltipLines\":\"m_41651_(" + PLAYER + FLAG + ")Ljava/util/List;\"}}}").getBytes());
        Mixin1201.Stats st = new Mixin1201.Stats();
        st.refmaps = Refmaps.parse(files);
        assertEquals("(" + PLAYER + FLAG + ")Ljava/util/List;", st.refmaps.get("m/RefmapMixin").get("getTooltipLines"));
        Mixin1201.apply(n, Map.of(), new Mc1201Hierarchy(), Mc1201Tables.get(), st);
        assertEquals("(" + CONTEXT + PLAYER + CIR + ")V", h.desc);
        assertEquals(1, st.repair.repaired);
    }
}
