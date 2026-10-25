package org.kylepitcock.legacyconnector.v1_7_10.adapter;

import java.util.List;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.item.BlockItem;

/**
 * Modern block item for one metadata variant of a 1.7.10 {@code ItemBlock}. Placement runs the legacy ItemBlock
 * code (which calls {@code onBlockPlaced}/{@code onBlockPlacedBy} and decrements the stack itself) instead of the
 * modern {@link BlockItem#place} logic.
 */
public class LegacyBlockItemAdapter extends BlockItem implements LegacyItemHolder {
    private final Item legacy;
    private final int meta;
    private final boolean customName;

    public LegacyBlockItemAdapter(LegacyBlockAdapter block, Item legacy, int meta, Properties properties) {
        super(block, properties);
        this.legacy = legacy;
        this.meta = meta;
        this.customName = LegacyBlockAdapter.overrides(legacy, "getItemStackDisplayName", legacyconnector.v1_7_10.net.minecraft.item.ItemStack.class);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        return ItemCallbacks.useOn(legacy, context, false);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return ItemCallbacks.use(legacy, level, player, hand);
    }

    @Override
    public InteractionResult onItemUseFirst(ItemStack stack, UseOnContext context) {
        return ItemCallbacks.useOn(legacy, context, true);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return ItemCallbacks.finishUsing(legacy, stack, level, entity);
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return ItemCallbacks.useDuration(legacy, stack);
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return ItemCallbacks.useAnimation(legacy, stack);
    }

    @Override
    public void onUseTick(Level level, LivingEntity entity, ItemStack stack, int remainingUseDuration) {
        ItemCallbacks.useTick(legacy, stack, level, entity, remainingUseDuration);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        ItemCallbacks.releaseUsing(legacy, stack, level, entity, timeLeft);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        ItemCallbacks.inventoryTick(legacy, stack, level, entity, slot, selected);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        return ItemCallbacks.hurtEnemy(legacy, stack, target, attacker);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, BlockState state, BlockPos pos, LivingEntity miner) {
        return ItemCallbacks.mineBlock(legacy, stack, level, state, pos, miner);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        return ItemCallbacks.interactLiving(legacy, stack, player, target);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        ItemCallbacks.tooltip(legacy, stack, tooltip, flag);
    }

    @Override
    public Component getName(ItemStack stack) {
        return ItemCallbacks.name(legacy, stack, customName);
    }

    @Override
    public String getDescriptionId() {
        return legacy.getUnlocalizedName() + ".name";
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        var view = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$wrap(stack);
        return view != null && legacy.hasEffect(view);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        return ItemCallbacks.destroySpeed(legacy, stack, state);
    }

    @Override
    public boolean isCorrectToolForDrops(ItemStack stack, BlockState state) {
        return ItemCallbacks.correctTool(legacy, stack, state) || super.isCorrectToolForDrops(stack, state);
    }

    @Override
    public ItemStack getCraftingRemainingItem(ItemStack stack) {
        return ItemCallbacks.remainder(legacy, stack);
    }

    @Override
    public boolean hasCraftingRemainingItem(ItemStack stack) {
        return ItemCallbacks.hasRemainder(legacy, stack);
    }

    @Override
    public int getBurnTime(ItemStack stack, RecipeType<?> recipeType) {
        int t = ItemCallbacks.burnTime(stack);
        return t > 0 ? t : super.getBurnTime(stack, recipeType);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        return ItemCallbacks.entityItemUpdate(legacy, entity);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        return ItemCallbacks.leftClickEntity(legacy, stack, player, target);
    }

    @Override
    public Object legacyItem() {
        return legacy;
    }

    @Override
    public int legacyMeta() {
        return meta;
    }
}
