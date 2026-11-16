package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

/** A call to an {@code @Optional.Method} the layer removed (its mod is absent) is an optional integration, not an unresolved reference. */
class OptionalMethodLinkTest {
    @Test
    void removedOptionalMethodIsReportedAsOptionalNotUnresolved() {
        ClassNode node = new ClassNode();
        node.version = Opcodes.V1_8;
        node.name = "mod/BaublesHelper";
        node.superName = "java/lang/Object";
        MethodNode m = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "getBaubles", "()V", null, null);
        AnnotationNode a = new AnnotationNode("Llegacyconnector/v1_7_10/cpw/mods/fml/common/Optional$Method;");
        a.visit("modid", "Baubles");
        m.visibleAnnotations = new java.util.ArrayList<>();
        m.visibleAnnotations.add(a);
        node.methods.add(m);

        LinkReport report = new LinkReport();
        Set<String> present = new HashSet<>();
        int n = OptionalStripper.apply(node, "legacyconnector/v1_7_10/", present, null, report.optionalRemoved);
        assertEquals(1, n);
        assertTrue(node.methods.isEmpty());

        report.missingMember("mod/BaublesHelper", "getBaubles()V", "mod/Caller", false);
        report.missingMember("mod/Other", "gone()V", "mod/Caller", false);
        assertEquals(1, report.errorCount(), "only the genuinely missing member counts");
        assertEquals(1, report.optionalMembers.size());
        assertTrue(report.render().contains("Optional integrations not available"));
    }

    @Test
    void membersOfAClassThatLostAnOptionalInterfaceAreOptional() {
        LinkReport report = new LinkReport();
        report.optionalRemoved.add("mod/Crop.<interface>");
        report.lostOptionalInterface = owner -> report.optionalRemoved.contains(owner + ".<interface>");
        report.missingMember("mod/Crop", "getBlock()V", "mod/Caller", false);
        assertEquals(0, report.errorCount());
        assertEquals(1, report.optionalMembers.size());
    }
}
