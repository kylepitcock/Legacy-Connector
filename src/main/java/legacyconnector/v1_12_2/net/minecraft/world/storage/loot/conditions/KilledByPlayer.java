package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;

/** 1.12.2 loot condition: the looted entity was (or, inverted, was not) killed by a player. */
public class KilledByPlayer implements LootCondition {
    private final boolean inverse;

    public KilledByPlayer(boolean inverse) {
        this.inverse = inverse;
    }

    @Override
    public boolean testCondition(Random rand, LootContext context) {
        boolean byPlayer = context.getKillerPlayer() != null;
        return byPlayer == !inverse;
    }
}
