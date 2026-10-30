package org.kylepitcock.legacyconnector.v1_12_2.adapter;

import legacyconnector.v1_12_2.net.minecraft.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/** The modern item of a 1.12.2 ItemBlock: places its modern block. */
public class Legacy1122BlockItem extends net.minecraft.world.item.BlockItem {
    private final Item legacy;

    public Legacy1122BlockItem(Item legacy, Block block, Properties properties) {
        super(block, properties);
        this.legacy = legacy;
    }

    public Item legacy() {
        return legacy;
    }

    /** A mod ItemBlock with its own placement code places through it; otherwise the modern placement with the legacy placement state. */
    @Override
    public net.minecraft.world.InteractionResult useOn(net.minecraft.world.item.context.UseOnContext context) {
        return cb().use ? cb().use(context) : super.useOn(context);
    }

    private Boolean placementState;

    /** The block's legacy getStateForPlacement (facing, half, ...) from the clicked side, hit point, stack metadata and placer. */
    @Override
    protected net.minecraft.world.level.block.state.BlockState getPlacementState(net.minecraft.world.item.context.BlockPlaceContext context) {
        var modern = super.getPlacementState(context);
        if (modern == null || !(legacy instanceof legacyconnector.v1_12_2.net.minecraft.item.ItemBlock ib) || ib.getBlock() == null) return modern;
        var block = ib.getBlock();
        if (placementState == null) {
            Class<?> W = legacyconnector.v1_12_2.net.minecraft.world.World.class, P = legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.class,
                    F = legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.class, L = legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase.class,
                    f = float.class;
            placementState = Legacy1122Block.overrides(block, "getStateForPlacement", W, P, F, f, f, f, int.class, L)
                    || Legacy1122Block.overrides(block, "getStateForPlacement", W, P, F, f, f, f, int.class, L, legacyconnector.v1_12_2.net.minecraft.util.EnumHand.class)
                    || legacy.getHasSubtypes();
        }
        if (!placementState) return modern;
        try {
            var pos = context.getClickedPos();
            var hit = context.getClickLocation();
            var placer = context.getPlayer() == null ? null : ItemCallbacks1122.living(context.getPlayer());
            int meta = legacy.getMetadata(org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122.fromModern(context.getItemInHand()).getMetadata());
            var state = block.getStateForPlacement(ItemCallbacks1122.world(context.getLevel()), legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos.mcc$of(pos),
                    legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.mcc$of(context.getClickedFace()), (float) (hit.x - pos.getX()), (float) (hit.y - pos.getY()),
                    (float) (hit.z - pos.getZ()), meta, placer, ItemCallbacks1122.hand(context.getHand()));
            var out = org.kylepitcock.legacyconnector.v1_12_2.bridge.StatesBridge1122.toModern(state);
            return out.getBlock() == modern.getBlock() && canPlace(context, out) ? out : modern;
        } catch (RuntimeException | LinkageError e) {
            return modern;
        }
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

    @Override
    public void appendHoverText(net.minecraft.world.item.ItemStack stack, TooltipContext context, java.util.List<net.minecraft.network.chat.Component> lines,
            net.minecraft.world.item.TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        if (cb().tooltip) cb().appendHoverText(stack, context, lines, flag);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, net.minecraft.world.entity.item.ItemEntity entity) {
        return cb().entityItemUpdate ? cb().entityItemUpdate(entity) : super.onEntityItemUpdate(stack, entity);
    }

    @Override
    public boolean doesSneakBypassUse(ItemStack stack, net.minecraft.world.level.LevelReader level, net.minecraft.core.BlockPos pos,
            net.minecraft.world.entity.player.Player player) {
        return cb().sneakBypass ? cb().sneakBypass(stack, level, pos, player) : super.doesSneakBypassUse(stack, level, pos, player);
    }
}
