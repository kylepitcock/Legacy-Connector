package org.kylepitcock.legacyconnector.compat.b173;

import org.kylepitcock.legacyconnector.vb1_7_3.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;

/** Durability callbacks from the supplied, translated mod classes. */
public final class DurabilitySelfTest {
    public static void run(ServerLevel level, ServerPlayer player, BlockPos pos) {
        int passed=0;
        var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
        try {
            for (var item:ContentB173.items()) {
                if (!(item.mcc$modern instanceof LegacyB173Item)) continue;
                String name=item.getClass().getSimpleName();
                if (!java.util.Set.of("ItemCustomPickaxe","ItemCustomAxe","ItemCustomSpade","ItemCustomSword",
                        "ItemZanitePickaxe","ItemSwordZanite").contains(name)) continue;
                boolean sword=name.contains("Sword");int mining=sword?2:1, hitting=sword?1:2;
                player.getInventory().clearContent();player.getInventory().selected=0;
                var stack=StacksB173.toModern(item.id,1,0);
                player.setItemInHand(InteractionHand.MAIN_HAND,stack);
                require(stack.isDamageableItem() && stack.getMaxDamage()==item.getMaxDamage(),name+" maximum durability");
                stack.getItem().mineBlock(stack,level,Blocks.STONE.defaultBlockState(),pos,player);
                require(stack.getDamageValue()==mining,name+" mining wear");
                boolean hit=stack.getItem().hurtEnemy(stack,target,player);
                require(stack.getDamageValue()==mining+hitting,name+" attack wear: expected "+(mining+hitting)+", got "+stack.getDamageValue()+", count "+stack.getCount()+", max "+item.getMaxDamage()+", host "+stack.getItem().getClass()+", result "+hit);
                require(player.getMainHandItem()==stack,name+" held stack identity");
                var restored=net.minecraft.world.item.ItemStack.parseOptional(level.registryAccess(),
                        (net.minecraft.nbt.CompoundTag)stack.save(level.registryAccess()));
                require(restored.getDamageValue()==mining+hitting,name+" saved wear");
                require(StacksB173.toModern(StacksB173.toLegacy(restored)).getDamageValue()==mining+hitting,name+" bridge roundtrip");
                player.getAbilities().instabuild=true;
                stack.getItem().hurtEnemy(stack,target,player);
                require(stack.getDamageValue()==mining+hitting,name+" creative wear");
                player.getAbilities().instabuild=false;
                stack.setDamageValue(stack.getMaxDamage()-mining);
                stack.getItem().mineBlock(stack,level,Blocks.STONE.defaultBlockState(),pos,player);
                require(stack.getCount()==1 && stack.getDamageValue()==stack.getMaxDamage(),name+" Beta last use");
                stack.getItem().mineBlock(stack,level,Blocks.STONE.defaultBlockState(),pos,player);
                require(stack.isEmpty(),name+" breakage");
                passed++;
            }
            for(var item:ContentB173.items()) if(item.getClass().getSimpleName().equals("ItemSkyrootBucket")) {
                var stack=new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(item.id,1,8);
                stack.damage(1,PlayerViewB173.of(player));
                require(stack.getDamage()==8 && stack.count==1,"bucket metadata is not durability");
            }
            for(var item:ContentB173.items()) if(item.getClass().getSimpleName().equals("ItemFandn")) {
                BlockPos at=pos.offset(700,-20,0);
                var original=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
                for(int x=-1;x<=1;x++)for(int z=-1;z<=1;z++)for(int y=0;y<=1;y++) {
                    var p=at.offset(x,y,z);original.put(p,level.getBlockState(p));
                    level.setBlock(p,(y==0?Blocks.STONE:Blocks.AIR).defaultBlockState(),2);
                }
                try {
                    player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(item.id,1,0));
                    var context=new net.minecraft.world.item.context.UseOnContext(player,InteractionHand.MAIN_HAND,
                            new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(at),net.minecraft.core.Direction.UP,at,false));
                    player.getMainHandItem().getItem().useOn(context);
                    require(player.getMainHandItem().getDamageValue()==1,"firestarter right-click wear");
                    require(level.getBlockState(at.above()).is(Blocks.FIRE),"firestarter places fire");
                    player.getMainHandItem().setDamageValue(item.getMaxDamage());
                    player.getMainHandItem().getItem().useOn(context);
                    require(player.getMainHandItem().isEmpty(),"firestarter right-click breakage");
                } finally { original.forEach((p,state)->level.setBlock(p,state,2)); }
            }
        } finally { player.getAbilities().instabuild=false;player.getInventory().clearContent();target.discard(); }
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info(
                "B1.7.3 ACTUAL DURABILITY TEST: {} tools/swords passed mining, combat, save, creative and breakage checks",passed);
    }
    private static void require(boolean ok,String message) { if(!ok)throw new IllegalStateException(message); }
}
