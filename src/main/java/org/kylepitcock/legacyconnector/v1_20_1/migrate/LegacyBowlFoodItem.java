package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/** Stand-in for 1.20.1's {@code BowlFoodItem} (gone in 1.21.1): a food that leaves a bowl behind. */
public class LegacyBowlFoodItem extends Item {
    public LegacyBowlFoodItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player && player.hasInfiniteMaterials()) return result;
        return result.isEmpty() ? new ItemStack(Items.BOWL) : result;
    }
}
