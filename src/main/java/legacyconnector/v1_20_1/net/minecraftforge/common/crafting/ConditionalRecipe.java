package legacyconnector.v1_20_1.net.minecraftforge.common.crafting;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.conditions.ICondition;
import org.kylepitcock.legacyconnector.v1_20_1.migrate.LegacyFinishedRecipe;

/**
 * Forge 1.20.1's conditional recipe wrapper, only as far as a data generator builds one ({@code ConditionalRecipe.builder().addCondition(..).addRecipe(..)
 * .build(consumer, id)}). Data generation does not run in the game and 1.21.1 puts conditions on the recipe itself, so the builder only records what it is
 * given and hands the wrapped recipes to the consumer unchanged.
 */
public final class ConditionalRecipe {
    private ConditionalRecipe() {
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final List<ICondition> conditions = new ArrayList<>();
        private final List<LegacyFinishedRecipe> recipes = new ArrayList<>();

        public Builder addCondition(ICondition condition) {
            conditions.add(condition);
            return this;
        }

        public Builder addRecipe(LegacyFinishedRecipe recipe) {
            recipes.add(recipe);
            return this;
        }

        public Builder addRecipe(Consumer<Consumer<LegacyFinishedRecipe>> callable) {
            callable.accept(recipes::add);
            return this;
        }

        public Builder setAdvancement(String namespace, String path, Object advancement) {
            return this;
        }

        public Builder setAdvancement(Object advancement) {
            return this;
        }

        public void build(Consumer<LegacyFinishedRecipe> consumer, String namespace, String path) {
            build(consumer, ResourceLocation.fromNamespaceAndPath(namespace, path));
        }

        public void build(Consumer<LegacyFinishedRecipe> consumer, ResourceLocation id) {
            for (LegacyFinishedRecipe r : recipes) consumer.accept(r);
        }
    }
}
