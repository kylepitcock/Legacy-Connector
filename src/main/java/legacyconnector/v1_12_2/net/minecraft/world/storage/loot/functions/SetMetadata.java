package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.functions;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.LootContext;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.RandomValueRange;
import legacyconnector.v1_12_2.net.minecraft.world.storage.loot.conditions.LootCondition;

/** 1.12.2 loot function: sets the metadata (subtype) of a stack whose item has subtypes. */
public class SetMetadata extends LootFunction {
    private final RandomValueRange metaRange;

    public SetMetadata(LootCondition[] conditions, RandomValueRange metaRange) {
        super(conditions);
        this.metaRange = metaRange;
    }

    @Override
    public ItemStack apply(ItemStack stack, Random rand, LootContext context) {
        if (stack.isItemStackDamageable()) {
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/Loot").warn("Couldn't set data of loot item {}", stack);
        } else {
            stack.setItemDamage(metaRange.generateInt(rand));
        }
        return stack;
    }
}
