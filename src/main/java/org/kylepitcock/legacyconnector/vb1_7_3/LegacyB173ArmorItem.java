package org.kylepitcock.legacyconnector.vb1_7_3;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

/** Native armor layer and wear pipeline with the original Beta item callbacks and constructor state. */
public final class LegacyB173ArmorItem extends net.minecraft.world.item.ArmorItem {
    private final ItemCallbacksB173 callbacks;
    public LegacyB173ArmorItem(legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem legacy,Properties properties){
        super(ArmorMaterialsB173.material(legacy),ArmorMaterialsB173.type(legacy.equipmentSlot),properties);
        callbacks=new ItemCallbacksB173(legacy);
        // Beta dispensers ejected armor as items; native ArmorItem registers an auto-equip behavior.
        net.minecraft.world.level.block.DispenserBlock.registerBehavior(this,new net.minecraft.core.dispenser.DefaultDispenseItemBehavior());
    }
    @Override public net.minecraft.world.entity.EquipmentSlot getEquipmentSlot(ItemStack stack){return callbacks.getEquipmentSlot(stack);}
    @Override public net.minecraft.network.chat.Component getName(ItemStack stack){var name=callbacks.getName(stack);return name==null?super.getName(stack):name;}
    @Override public float getDestroySpeed(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return callbacks.getDestroySpeed(stack,state);}
    @Override public boolean isCorrectToolForDrops(ItemStack stack,net.minecraft.world.level.block.state.BlockState state){return callbacks.isCorrectToolForDrops(stack,state);}
    @Override public InteractionResult useOn(UseOnContext context){return callbacks.useOn(context);}
    @Override public InteractionResultHolder<ItemStack> use(Level level,Player player,InteractionHand hand){return callbacks.use(level,player,hand);}
    @Override public float getAttackDamageBonus(net.minecraft.world.entity.Entity target,float damage,net.minecraft.world.damagesource.DamageSource source){return callbacks.getAttackDamageBonus(target,damage,source);}
    @Override public InteractionResult interactLivingEntity(ItemStack stack,Player player,net.minecraft.world.entity.LivingEntity target,InteractionHand hand){return callbacks.interactLivingEntity(stack,player,target,hand);}
    @Override public boolean mineBlock(ItemStack stack,Level level,net.minecraft.world.level.block.state.BlockState state,net.minecraft.core.BlockPos pos,net.minecraft.world.entity.LivingEntity miner){return callbacks.mineBlock(stack,level,state,pos,miner);}
    @Override public boolean hurtEnemy(ItemStack stack,net.minecraft.world.entity.LivingEntity target,net.minecraft.world.entity.LivingEntity attacker){return callbacks.hurtEnemy(stack,target,attacker);}
}
