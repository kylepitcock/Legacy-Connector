package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;

/** 1.12.2 loot condition: passes with a fixed chance. */
public class RandomChance implements LootCondition {
    private final float chance;

    public RandomChance(float chance) {
        this.chance = chance;
    }

    @Override
    public boolean testCondition(Random rand, LootContext context) {
        return rand.nextFloat() < chance;
    }
}
