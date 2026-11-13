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

/** Shared original item callbacks for ordinary items and native armor hosts. No native Item is allocated here. */
final class ItemCallbacksB173 {
    static legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.ContextScope actorScope(World world,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity actor){
        return legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(world,actor);
    }
    private final legacyconnector.vb1_7_3.net.minecraft.item.Item legacy;
    private final boolean usesAir;
    private final boolean usesEntity;
    private int failures;

    ItemCallbacksB173(legacyconnector.vb1_7_3.net.minecraft.item.Item legacy) {
        this.legacy = legacy;
        try {
            usesAir = legacy.getClass().getMethod("use", legacyconnector.vb1_7_3.net.minecraft.item.ItemStack.class,
                    World.class, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class).getDeclaringClass()
                    != legacyconnector.vb1_7_3.net.minecraft.item.Item.class;
            usesEntity = legacy.getClass().getMethod("useOnEntity", legacyconnector.vb1_7_3.net.minecraft.item.ItemStack.class,
                    legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity.class).getDeclaringClass()
                    != legacyconnector.vb1_7_3.net.minecraft.item.Item.class;
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(e);
        }
    }

     public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot(ItemStack stack){
        if(legacy instanceof legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem armor)
            return new net.minecraft.world.entity.EquipmentSlot[]{net.minecraft.world.entity.EquipmentSlot.HEAD,
                    net.minecraft.world.entity.EquipmentSlot.CHEST,net.minecraft.world.entity.EquipmentSlot.LEGS,
                    net.minecraft.world.entity.EquipmentSlot.FEET}[armor.equipmentSlot];
        return null;
    }

     public net.minecraft.network.chat.Component getName(ItemStack stack){
        var original=StacksB173.toLegacy(stack);
        if(original!=null&&legacy.hasSubtypes()){
            String key=legacy.getTranslationKey(original)+".name",name=ContentB173.translate(key);
            if(!key.equals(name))return net.minecraft.network.chat.Component.literal(name);
        }
        return null;
    }

     public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){
        var original=StacksB173.toLegacy(stack);int id=BlocksB173.legacyId(state);
        var block=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS[id];
        return original==null||block==null?1F:legacy.getStrVsBlock(original,block,BlocksB173.legacyMeta(state));
    }
     public boolean isCorrectToolForDrops(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){
        var block=legacyconnector.vb1_7_3.net.minecraft.block.Block.BLOCKS[BlocksB173.legacyId(state)];
        return block!=null&&legacy.isSuitableFor(block);
    }
    public boolean canPerformAction(net.neoforged.neoforge.common.ItemAbility ability){
        if(!(legacy instanceof legacyconnector.vb1_7_3.net.minecraft.src.Tool tool))return false;
        var family=tool.toolBase;
        return family==legacyconnector.vb1_7_3.net.minecraft.src.ToolBase.Pickaxe&&ability==net.neoforged.neoforge.common.ItemAbilities.PICKAXE_DIG
                ||family==legacyconnector.vb1_7_3.net.minecraft.src.ToolBase.Shovel&&ability==net.neoforged.neoforge.common.ItemAbilities.SHOVEL_DIG
                ||family==legacyconnector.vb1_7_3.net.minecraft.src.ToolBase.Axe&&ability==net.neoforged.neoforge.common.ItemAbilities.AXE_DIG;
    }


    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        PlayerViewB173 view = (PlayerViewB173)EntityViewsB173.wrap(player);
        var held = view.inventory.main[view.inventory.selectedSlot];
        if (held == null) return InteractionResult.PASS;
        var pos = context.getClickedPos();
        World world = World.mcc$wrap(context.getLevel());
        // NeoForge snapshots the blocks an item places and runs their onPlace afterwards; Beta ran onBlockAdded inside the write, and items
        // set up the tile entity that call makes right after placing it, so the legacy callback must see the writes at once.
        var level=context.getLevel();boolean capturing=level.captureBlockSnapshots;level.captureBlockSnapshots=false;
        try (var views=EntityViewsB173.callbackScope();var minecraft=actorScope(world,view)) {
            boolean used = legacy.useOnBlock(held, view, world, pos.getX(), pos.getY(), pos.getZ(), context.getClickedFace().get3DDataValue());
            if (!context.getLevel().isClientSide) view.writeBack();
            return used ? InteractionResult.sidedSuccess(context.getLevel().isClientSide) : InteractionResult.PASS;
        } catch (RuntimeException | LinkageError e) {
            failed("useOnBlock", e);
            return InteractionResult.FAIL;
        } finally {level.captureBlockSnapshots=capturing;EntityViewsB173.flush();BlockEntityViewsB173.flush();}
    }


    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !usesAir) return InteractionResultHolder.pass(player.getItemInHand(hand));
        PlayerViewB173 view = (PlayerViewB173)EntityViewsB173.wrap(player);
        int slot = view.inventory.selectedSlot;
        var held = view.inventory.main[slot];
        if (held == null) return InteractionResultHolder.pass(player.getItemInHand(hand));
        World world = World.mcc$wrap(level);
        try (var views=EntityViewsB173.callbackScope();var minecraft=actorScope(world,view)) {
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
        LegacyB173Item.CALLBACK_FAILURES.incrementAndGet();
        if (failures++ < 3) LoggerFactory.getLogger("LegacyConnector/b1.7.3").warn(
                "b1.7.3 item {} callback {} failed", legacy, callback, error);
    }


    public float getAttackDamageBonus(net.minecraft.world.entity.Entity target, float damage,
            net.minecraft.world.damagesource.DamageSource source) {
        var attacker = source.getEntity();
        try (var views = EntityViewsB173.callbackScope()) {
            var actor = EntityViewsB173.wrap(attacker);
            var original = EntityViewsB173.wrap(target);
            try (var minecraft = legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(
                    World.mcc$wrap(target.level()), actor)) {
                float strength = attacker instanceof Player player ? player.getAttackStrengthScale(0.5F) : 1F;
                // Native Player.attack invokes this after cooldown scaling and before critical damage.
                // Add only the legacy item's contribution; retain native attributes and enchantments.
                return attackDamageBonus(legacy, original, strength);
            }
        } catch (RuntimeException | LinkageError error) {
            failed("getAttackDamage", error);
            return 0F;
        } finally { EntityViewsB173.flush(); BlockEntityViewsB173.flush(); }
    }

    static float attackDamageBonus(legacyconnector.vb1_7_3.net.minecraft.item.Item item,
            legacyconnector.vb1_7_3.net.minecraft.entity.Entity target, float strength) {
        return (item.getAttackDamage(target) - 1F) * (0.2F + strength * strength * 0.8F);
    }


    public InteractionResult interactLivingEntity(ItemStack stack, Player player, net.minecraft.world.entity.LivingEntity target,
            InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND || !usesEntity) return InteractionResult.PASS;
        if (player.level().isClientSide) return InteractionResult.SUCCESS;
        var held = StacksB173.toLegacy(stack);
        if (held == null) return InteractionResult.PASS;
        var actor = (PlayerViewB173)EntityViewsB173.wrap(player);
        try (var minecraft = legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(World.mcc$wrap(player.level()), actor);
                var views = EntityViewsB173.callbackScope()) {
            var original = (legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity)EntityViewsB173.wrap(target);
            Runnable callback = () -> {
                legacy.useOnEntity(held, original);
                // Creative interactions receive a copy; preserve the native caller's stack and components.
                stack.setCount(Math.max(0, held.count));
                if (!stack.isEmpty() && stack.isDamageableItem()) stack.setDamageValue(held.getDamage());
                actor.writeBack();
            };
            if (player instanceof net.minecraft.server.level.ServerPlayer serverPlayer) MenusB173.withActor(serverPlayer, callback);
            else callback.run();
            return InteractionResult.CONSUME;
        } catch (RuntimeException | LinkageError e) {
            failed("useOnEntity", e);
            return InteractionResult.FAIL;
        } finally { EntityViewsB173.flush(); BlockEntityViewsB173.flush(); }
    }


    public boolean mineBlock(ItemStack stack, Level level, net.minecraft.world.level.block.state.BlockState state,
            net.minecraft.core.BlockPos pos, net.minecraft.world.entity.LivingEntity miner) {
        if (level.isClientSide) return false;
        return wear(stack, miner, null, state, pos);
    }


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
