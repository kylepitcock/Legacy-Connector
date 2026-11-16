package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.advancements.CriterionTriggerInstance;
import net.minecraft.advancements.critereon.CriterionValidator;

/**
 * What a mod class that implements {@code CriterionTriggerInstance} directly (1.20.1: an interface of {@code getCriterion} and {@code serializeToJson}) is
 * made to implement instead: the 1.21.1 interface has an abstract {@code validate(CriterionValidator)} that such a class never had. The loader swaps the
 * interface in the class's interface list only, so descriptors still name the real type.
 */
public interface LegacyCriterionTriggerInstance extends CriterionTriggerInstance {
    @Override
    default void validate(CriterionValidator validator) {
    }
}
