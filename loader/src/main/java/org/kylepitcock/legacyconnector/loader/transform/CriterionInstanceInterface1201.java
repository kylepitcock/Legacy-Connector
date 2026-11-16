package org.kylepitcock.legacyconnector.loader.transform;

import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;

/**
 * A class that implements {@code CriterionTriggerInstance} directly (AE2's advancement trigger instance) lacks 1.21.1's abstract
 * {@code validate(CriterionValidator)}: the interface in its interface list becomes {@code LegacyCriterionTriggerInstance}, which extends it and
 * has a default for that method. Descriptors keep the real type, so vanilla calls are unaffected.
 */
final class CriterionInstanceInterface1201 {
    private CriterionInstanceInterface1201() {}

    private static final String REAL = "net/minecraft/advancements/CriterionTriggerInstance";
    private static final String STAND_IN = "org/kylepitcock/legacyconnector/v1_20_1/migrate/LegacyCriterionTriggerInstance";

    /** @return the number of classes changed */
    static int apply(List<ClassNode> nodes) {
        int changed = 0;
        for (ClassNode n : nodes) {
            if ((n.access & Opcodes.ACC_INTERFACE) != 0 || n.interfaces == null) continue;
            int i = n.interfaces.indexOf(REAL);
            if (i < 0) continue;
            n.interfaces.set(i, STAND_IN);
            changed++;
        }
        return changed;
    }
}
