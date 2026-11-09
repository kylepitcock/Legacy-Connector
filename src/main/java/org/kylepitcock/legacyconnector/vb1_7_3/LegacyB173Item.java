package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Native ordinary-item host; callbacks are shared with native armor without allocating delegate Items. */
public final class LegacyB173Item extends net.minecraft.world.item.Item {
    private final ItemCallbacksB173 callbacks;
    static final java.util.concurrent.atomic.AtomicInteger CALLBACK_FAILURES=new java.util.concurrent.atomic.AtomicInteger();
    public LegacyB173Item(legacyconnector.vb1_7_3.net.minecraft.item.Item legacy,Properties properties){super(properties);callbacks=new ItemCallbacksB173(legacy);}
    static float attackDamageBonus(legacyconnector.vb1_7_3.net.minecraft.item.Item item,legacyconnector.vb1_7_3.net.minecraft.entity.Entity target,float strength){return ItemCallbacksB173.attackDamageBonus(item,target,strength);}
    @Override public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot(ItemStack stack){return callbacks.getEquipmentSlot(stack);}
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack){var name=callbacks.getName(stack);return name==null?super.getName(stack):name;}
    @Override public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return callbacks.getDestroySpeed(stack,state);}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return callbacks.isCorrectToolForDrops(stack,state);}
    @Override public InteractionResult useOn(UseOnContext context){return callbacks.useOn(context);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){
        if(hand==InteractionHand.MAIN_HAND&&callbacks.isFood()&&BetaGameplayConfigB173.foodUsesHunger())return super.use(level,player,hand);
        return callbacks.use(level,player,hand);
    }
    @Override public net.minecraft.world.item.UseAnim getUseAnimation(ItemStack stack){return callbacks.isFood()&&!BetaGameplayConfigB173.foodUsesHunger()?net.minecraft.world.item.UseAnim.NONE:super.getUseAnimation(stack);}
    @Override public int getUseDuration(ItemStack stack,net.minecraft.world.entity.LivingEntity entity){return callbacks.isFood()&&!BetaGameplayConfigB173.foodUsesHunger()?0:super.getUseDuration(stack,entity);}
    @Override public ItemStack finishUsingItem(ItemStack stack,Level level,net.minecraft.world.entity.LivingEntity entity){
        if(callbacks.isFood()&&entity instanceof Player player)return callbacks.finishFood(stack,level,player,BetaGameplayConfigB173.foodUsesHunger());
        return super.finishUsingItem(stack,level,entity);
    }
    @Override public float getAttackDamageBonus(net.minecraft.world.entity.Entity target,float damage,net.minecraft.world.damagesource.DamageSource source){return callbacks.getAttackDamageBonus(target,damage,source);}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,net.minecraft.world.entity.LivingEntity target,InteractionHand hand){return callbacks.interactLivingEntity(stack,player,target,hand);}
    @Override public boolean mineBlock(ItemStack stack,Level level,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.LivingEntity miner){return callbacks.mineBlock(stack,level,state,pos,miner);}
    @Override public boolean hurtEnemy(ItemStack stack,net.minecraft.world.entity.LivingEntity target,net.minecraft.world.entity.LivingEntity attacker){return callbacks.hurtEnemy(stack,target,attacker);}
}
