package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** The modern item for one metadata variant of a 1.12.2 item. */
public class Legacy1122Item extends net.minecraft.world.item.Item {
    private final Item legacy;
    private final int meta;

    public Legacy1122Item(Item legacy, int meta, Properties properties) {
        super(properties);
        this.legacy = legacy;
        this.meta = meta;
    }

    public Item legacy() {
        return legacy;
    }

    public int meta() {
        return meta;
    }

    @Override
    public String getDescriptionId() {
        return legacy.getUnlocalizedName() + ".name";
    }

    @Override
    public Component getName(ItemStack stack) {
        try {
            var view = new legacyconnector.v1_12_2.net.minecraft.item.ItemStack(legacy, 1, meta);
            String name = legacy.getItemStackDisplayName(view);
            if (name != null) return Component.literal(name);
        } catch (RuntimeException | LinkageError e) {
            // fall back to the translation key
        }
        return super.getName(stack);
    }

    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return cb().use ? cb().use(context) : super.useOn(context);
    }

    // ------------------------------------------------------------------ hooks the legacy item overrides (see ItemCallbacks1122)

    private ItemCallbacks1122 callbacks;

    private ItemCallbacks1122 cb() {
        var c = callbacks;
        if (c == null) callbacks = c = new ItemCallbacks1122(legacy);
        return c;
    }

    @Override
    public net.minecraft.world.InteractionResultHolder<ItemStack> use(net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.InteractionHand hand) {
        return cb().rightClick ? cb().rightClick(level, player, hand) : super.use(level, player, hand);
    }

    @Override
    public net.minecraft.world.InteractionResult onItemUseFirst(ItemStack stack, net.minecraft.world.item.context.UseOnContext context) {
        return cb().useFirst ? cb().useFirst(context) : super.onItemUseFirst(stack, context);
    }

    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (cb().update) cb().update(stack, level, entity, slot, selected);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity) {
        if (cb().useFinish) return cb().useFinish(stack, level, entity);
        ItemStack eaten = cb().foodEaten ? stack.copy() : null;
        ItemStack out = super.finishUsingItem(stack, level, entity);
        if (eaten != null) cb().foodEaten(eaten, level, entity);
        return out;
    }

    @Override
    public void releaseUsing(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity, int timeLeft) {
        if (cb().stoppedUsing) cb().stoppedUsing(stack, level, entity, timeLeft);
        else super.releaseUsing(stack, level, entity, timeLeft);
    }

    @Override
    public void onUseTick(net.minecraft.world.level.Level level, net.minecraft.world.entity.LivingEntity entity, ItemStack stack, int remaining) {
        if (cb().usingTick) cb().usingTick(stack, entity, remaining);
        else super.onUseTick(level, entity, stack, remaining);
    }

    @Override
    public int getUseDuration(ItemStack stack, net.minecraft.world.entity.LivingEntity entity) {
        int modern = super.getUseDuration(stack, entity);
        return cb().useDuration ? cb().useDuration(stack, modern) : modern;
    }

    @Override
    public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack) {
        var modern = super.getUseAnimation(stack);
        return cb().useAction ? cb().useAction(stack, modern) : modern;
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
        return cb().hit ? cb().hit(stack, target, attacker) : super.hurtEnemy(stack, target, attacker);
    }

    @Override
    public boolean mineBlock(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity entity) {
        return cb().destroyed ? cb().destroyed(stack, level, state, pos, entity) : super.mineBlock(stack, level, state, pos, entity);
    }

    @Override
    public float getDestroySpeed(ItemStack stack, net.minecraft.world.level.block.state.BlockState state) {
        float modern = super.getDestroySpeed(stack, state);
        return cb().destroySpeed ? cb().destroySpeed(stack, state, modern) : modern;
    }

    @Override
    public net.minecraft.world.InteractionResult interactLivingEntity(ItemStack stack, net.minecraft.world.entity.player.Player player,
            net.minecraft.world.entity.LivingEntity target, net.minecraft.world.InteractionHand hand) {
        return cb().interact ? cb().interact(stack, player, target, hand) : super.interactLivingEntity(stack, player, target, hand);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        boolean modern = super.isFoil(stack);
        return cb().effect ? cb().effect(stack, modern) : modern;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        Boolean legacyBar = cb().durabilityBar ? cb().durabilityBar(stack) : null;
        return legacyBar != null ? legacyBar : super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        Integer w = cb().durability ? cb().barWidth(stack) : null;
        return w != null ? Math.max(0, Math.min(13, w)) : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        Integer c = cb().durabilityColor ? cb().barColor(stack) : null;
        return c != null ? c : super.getBarColor(stack);
    }

    @Override
    public void onCraftedBy(ItemStack stack, net.minecraft.world.level.Level level, net.minecraft.world.entity.player.Player player) {
        super.onCraftedBy(stack, level, player);
        if (cb().created) cb().created(stack, level, player);
    }
}
