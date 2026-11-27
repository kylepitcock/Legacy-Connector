package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

class PalladiumCoreLightningBoltMixin1201Test implements Opcodes {
    private static final String INJECT = "Lorg/spongepowered/asm/mixin/injection/Inject;";
    private static final String AT = "Lorg/spongepowered/asm/mixin/injection/At;";
    private static final String TARGET =
            "Lnet/minecraft/world/level/Level;getEntities(Lnet/minecraft/world/entity/Entity;"
                    + "Lnet/minecraft/world/phys/AABB;Ljava/util/function/Predicate;)Ljava/util/List;";

    private static ClassNode fixture(String name) {
        ClassNode node = new ClassNode();
        node.name = name;
        MethodNode method = new MethodNode(ACC_PRIVATE, "tick", "()V", null, null);
        AnnotationNode at = new AnnotationNode(AT);
        at.values = new ArrayList<>(List.of(
                "value", "INVOKE_ASSIGN",
                "target", TARGET,
                "ordinal", 1,
                "shift", new String[] {"Lorg/spongepowered/asm/mixin/injection/At$Shift;", "BY"},
                "by", 1));
        AnnotationNode inject = new AnnotationNode(INJECT);
        inject.values = new ArrayList<>(List.of("method", List.of("tick"), "at", List.of(at)));
        method.visibleAnnotations = new ArrayList<>(List.of(inject));
        node.methods.add(method);
        return node;
    }

    private static AnnotationNode targetInject(ClassNode node) {
        return node.methods.get(0).visibleAnnotations.get(0);
    }

    private static AnnotationNode targetAt(ClassNode node) {
        AnnotationNode inject = targetInject(node);
        return assertInstanceOf(AnnotationNode.class, ((List<?>) inject.values.get(3)).get(0));
    }

    private static Object value(AnnotationNode annotation, String name) {
        if (annotation.values == null) return null;
        for (int i = 0; i + 1 < annotation.values.size(); i += 2) {
            if (name.equals(annotation.values.get(i))) return annotation.values.get(i + 1);
        }
        return null;
    }

    @Test
    void declaresMaxShiftByOnInjectAndIsIdempotent() {
        ClassNode node = fixture(PalladiumCoreLightningBoltMixin1201.MIXIN);

        assertEquals(1, PalladiumCoreLightningBoltMixin1201.apply(node));
        assertEquals(1, value(targetInject(node), "maxShiftBy"));
        assertEquals(null, value(targetAt(node), "maxShiftBy"));
        assertEquals(0, PalladiumCoreLightningBoltMixin1201.apply(node));
    }

    @Test
    void migratesThePreviouslyMisplacedNestedMaxShiftByValue() {
        ClassNode node = fixture(PalladiumCoreLightningBoltMixin1201.MIXIN);
        targetAt(node).values.add("maxShiftBy");
        targetAt(node).values.add(1);

        assertEquals(2, PalladiumCoreLightningBoltMixin1201.apply(node));
        assertEquals(1, value(targetInject(node), "maxShiftBy"));
        assertEquals(null, value(targetAt(node), "maxShiftBy"));
        assertEquals(0, PalladiumCoreLightningBoltMixin1201.apply(node));
    }

    @Test
    void adaptsTheSameMixinWhenItsPackageWasRelocated() {
        ClassNode node = fixture("legacyconnector_relocated/sbs_1_20_1/" + PalladiumCoreLightningBoltMixin1201.MIXIN);

        assertEquals(1, PalladiumCoreLightningBoltMixin1201.apply(node));
        assertEquals(1, value(targetInject(node), "maxShiftBy"));
        assertEquals(null, value(targetAt(node), "maxShiftBy"));
        assertEquals(0, PalladiumCoreLightningBoltMixin1201.apply(node));
    }

    @Test
    void leavesOtherClassesUntouched() {
        ClassNode node = fixture("example/UnrelatedMixin");

        assertEquals(0, PalladiumCoreLightningBoltMixin1201.apply(node));
        assertEquals(null, value(targetInject(node), "maxShiftBy"));
        assertEquals(null, value(targetAt(node), "maxShiftBy"));
    }
}
