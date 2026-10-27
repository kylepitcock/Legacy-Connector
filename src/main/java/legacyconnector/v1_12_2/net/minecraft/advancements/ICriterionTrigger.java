package legacyconnector.v1_12_2.net.minecraft.advancements;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * A kind of advancement criterion. Mods' triggers keep the listeners of each player's advancements and grant the criterion
 * through {@link Listener#grantCriterion} when their condition is met.
 */
public interface ICriterionTrigger<T extends ICriterionInstance> {
    ResourceLocation getId();

    void addListener(PlayerAdvancements playerAdvancementsIn, Listener<T> listener);

    void removeListener(PlayerAdvancements playerAdvancementsIn, Listener<T> listener);

    void removeAllListeners(PlayerAdvancements playerAdvancementsIn);

    T deserializeInstance(com.google.gson.JsonObject json, com.google.gson.JsonDeserializationContext context);

    /** One criterion of one advancement listening for this trigger. */
    class Listener<T extends ICriterionInstance> {
        private final T criterionInstance;
        private final Advancement advancement;
        private final String criterionName;
        /** What grants the criterion in the modern game, when the modern advancement system made this listener. */
        public Runnable mcc$grant;

        public Listener(T criterionInstanceIn, Advancement advancementIn, String criterionNameIn) {
            this.criterionInstance = criterionInstanceIn;
            this.advancement = advancementIn;
            this.criterionName = criterionNameIn;
        }

        public T getCriterionInstance() {
            return criterionInstance;
        }

        public void grantCriterion(PlayerAdvancements playerAdvancementsIn) {
            if (mcc$grant != null) mcc$grant.run();
            else if (playerAdvancementsIn != null) playerAdvancementsIn.grantCriterion(advancement, criterionName);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Listener<?> l)) return false;
            return java.util.Objects.equals(criterionInstance, l.criterionInstance) && java.util.Objects.equals(advancement, l.advancement)
                    && java.util.Objects.equals(criterionName, l.criterionName);
        }

        @Override
        public int hashCode() {
            return java.util.Objects.hash(criterionInstance, advancement, criterionName);
        }
    }
}
