package org.kylepitcock.legacyconnector.vb1_7_3;

import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import org.slf4j.LoggerFactory;

/** Modern entry points for a legacy item's right clicks. Beta had only the selected main-hand slot. */
public final class LegacyB173Item extends Item {
    private final legacyconnector.vb1_7_3.net.minecraft.item.Item legacy;
    private final boolean usesAir;
    private int failures;
    static final java.util.concurrent.atomic.AtomicInteger CALLBACK_FAILURES = new java.util.concurrent.atomic.AtomicInteger();

    public LegacyB173Item(legacyconnector.vb1_7_3.net.minecraft.item.Item legacy, Properties properties) {
        super(properties);
        this.legacy = legacy;
        try {
            usesAir = legacy.getClass().getMethod("use", legacyconnector.vb1_7_3.net.minecraft.item.ItemStack.class,
                    World.class, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class).getDeclaringClass()
                    != legacyconnector.vb1_7_3.net.minecraft.item.Item.class;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        PlayerViewB173 view = (PlayerViewB173)EntityViewsB173.wrap(player);
        var held = view.inventory.main[view.inventory.selectedSlot];
        if (held == null) return InteractionResult.PASS;
        var pos = context.getClickedPos();
        try (var views=EntityViewsB173.callbackScope()) {
            World world = World.mcc$wrap(context.getLevel());
            legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$use(world);
            boolean used = legacy.useOnBlock(held, view, world, pos.getX(), pos.getY(), pos.getZ(), context.getClickedFace().get3DDataValue());
            if (!context.getLevel().isClientSide) view.writeBack();
            return used ? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : InteractionResult.PASS;
        } catch (RuntimeException | LinkageError e) {
            failed("useOnBlock", e);
            return InteractionResult.FAIL;
        } finally {EntityViewsB173.flush();BlockEntityViewsB173.flush();}
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !usesAir) return InteractionResultHolder.pass(player.getItemInHand(hand));
        PlayerViewB173 view = (PlayerViewB173)EntityViewsB173.wrap(player);
        int slot = view.inventory.selectedSlot;
        var held = view.inventory.main[slot];
        if (held == null) return InteractionResultHolder.pass(player.getItemInHand(hand));
        try (var views=EntityViewsB173.callbackScope()) {
            World world = World.mcc$wrap(level);
            legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$use(world);
            var result = legacy.use(held, world, view);
            // Returning a new stack replaces the held item; otherwise retain direct inventory edits by the mod.
            if (result != held) view.inventory.main[slot] = result;
            if (!level.isClientSide) view.writeBack();
            return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide);
        } catch (RuntimeException | LinkageError e) {
            failed("use", e);
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        } finally {EntityViewsB173.flush();BlockEntityViewsB173.flush();}
    }

    private void failed(String callback, Throwable error) {
        CALLBACK_FAILURES.incrementAndGet();
        if (failures++ < 3) LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn(
                "b1.7.3 item {} callback {} failed", legacy, callback, error);
    }

    @Override
    public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity miner) {
        if (level.isClientSide) return false;
        return wear(stack, miner, null, state, pos);
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, net.minecraft.world.entity.LivingEntity target, net.minecraft.world.entity.LivingEntity attacker) {
        if (attacker.level().isClientSide) return false;
        return wear(stack, attacker, target, null, null);
    }

    private boolean wear(ItemStack stack, net.minecraft.world.entity.LivingEntity actor, net.minecraft.world.entity.LivingEntity target,
            net.minecraft.world.level.block.state.BlockState state, net.minecraft.core.BlockPos pos) {
        var held=StacksB173.toLegacy(stack);
        if (held==null) return false;
        var view=(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity)EntityViewsB173.wrap(actor);
        try (var views=EntityViewsB173.callbackScope()) {
            legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$use(World.mcc$wrap(actor.level()));
            boolean result=target==null ? legacy.postMine(held,BlocksB173.legacyId(state),pos.getX(),pos.getY(),pos.getZ(),view)
                    : legacy.postHit(held,(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity)EntityViewsB173.wrap(target),view);
            // Keep the caller's modern stack identity and components; vanilla still holds this object after the callback.
            stack.setCount(Math.max(0,held.count));
            if (!stack.isEmpty() && stack.isDamageableItem()) stack.setDamageValue(held.getDamage());
            if (view instanceof PlayerViewB173 player) player.writeBack();
            return result;
        } catch (RuntimeException | LinkageError e) {
            failed(target==null?"postMine":"postHit",e);return false;
        } finally { EntityViewsB173.flush(); }
    }
}
