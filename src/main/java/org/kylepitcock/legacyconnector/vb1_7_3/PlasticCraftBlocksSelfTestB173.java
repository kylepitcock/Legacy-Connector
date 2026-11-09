package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** Opt-in actual-mod fixture. Production behavior is selected solely through shared Beta APIs. */
public final class PlasticCraftBlocksSelfTestB173 {
    private PlasticCraftBlocksSelfTestB173(){}
    private static void require(boolean value,String message){if(!value)throw new IllegalStateException("B1.7.3 PLASTICCRAFT BLOCK TEST: "+message);}
    private static Block block(String name){return ContentB173.blocks().stream().filter(b->b.getClass().getSimpleName().equals(name)).findFirst().orElseThrow(()->new IllegalStateException("Actual mod block missing: "+name));}
    private static BlockState state(Block block,int meta){return block.mcc$modern.defaultBlockState().setValue(LegacyB173Block.META,meta);}
    private static String blockDetails(ServerLevel level,BlockPos pos) {
        var state=level.getBlockState(pos);
        return pos+"="+state+" legacyId="+BlocksB173.legacyId(state)+" meta="+BlocksB173.legacyMeta(state);
    }
    private static void activate(Block block,ServerLevel level,BlockPos pos,net.minecraft.world.entity.player.Player player)throws ReflectiveOperationException {
        var method=LegacyB173Block.class.getDeclaredMethod("useWithoutItem",BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class,
                net.minecraft.world.entity.player.Player.class,net.minecraft.world.phys.BlockHitResult.class);
        method.setAccessible(true);method.invoke(block.mcc$modern,level.getBlockState(pos),level,pos,player,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),Direction.UP,pos,false));
    }
    public static void run(ServerLevel level){
        var trampoline=block("BlockTrampoline");var accelerator=block("BlockAccelerator");var goo=block("BlockGooPlastic");
        var plastic=block("BlockPlastic");var jello=block("BlockJello");var door=block("BlockPlasticDoor");var glow=block("BlockGlowstick");var plexi=block("BlockPlexiglass");
        var origin=level.getSharedSpawnPos().offset(48,8,0);var w=World.mcc$wrap(level);
        Map<BlockPos,BlockState> saved=new LinkedHashMap<>();
        for(int x=-1;x<=7;x++)for(int y=-1;y<=3;y++)for(int z=-1;z<=3;z++){
            var pos=origin.offset(x,y,z);saved.put(pos,level.getBlockState(pos));level.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
        }
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var hand=player.getMainHandItem().copy();float health=player.getHealth();
        var oldPos=player.position();var oldVelocity=player.getDeltaMovement();
        var actors=new java.util.ArrayList<net.minecraft.world.entity.Entity>();
        try {
            for(int x=-1;x<=7;x++)for(int z=-1;z<=3;z++)level.setBlock(origin.offset(x,-1,z),Blocks.STONE.defaultBlockState(),2);
            var cow=net.minecraft.world.entity.EntityType.COW.create(level);require(cow!=null,"native cow fixture");actors.add(cow);
            cow.setPos(origin.getX()+.5,origin.getY()+.5,origin.getZ()+.5);
            level.setBlock(origin,state(trampoline,0),3);cow.setDeltaMovement(.1,0,.1);
            trampoline.mcc$modern.stepOn(level,origin,level.getBlockState(origin),cow);
            double single=cow.getDeltaMovement().y;require(single>0,"original trampoline bounces native living entity");
            level.setBlock(origin.east(),state(trampoline,0),3);cow.setDeltaMovement(.1,0,.1);
            trampoline.mcc$modern.stepOn(level,origin,level.getBlockState(origin),cow);
            require(cow.getDeltaMovement().y>single,"original connected-trampoline count increases bounce");

            var acc=origin.offset(3,0,0);level.setBlock(acc,state(accelerator,0),3);cow.setDeltaMovement(.1,0,-.2);
            accelerator.mcc$modern.stepOn(level,acc,level.getBlockState(acc),cow);
            require(Math.abs(cow.getDeltaMovement().x-.28)<1E-8&&Math.abs(cow.getDeltaMovement().z+.56)<1E-8,"original accelerator cow multiplier");
            cow.setDeltaMovement(.31,0,-.3);accelerator.mcc$modern.stepOn(level,acc,level.getBlockState(acc),cow);
            require(Math.abs(cow.getDeltaMovement().x-.31)<1E-8&&Math.abs(cow.getDeltaMovement().z+.3)<1E-8,"accelerator preserves components at/above threshold");

            var gp=origin.offset(5,0,0);level.setBlock(gp,state(goo,0),3);
            var item=new net.minecraft.world.entity.item.ItemEntity(level,gp.getX()+.5,gp.getY()+.8,gp.getZ()+.5,new net.minecraft.world.item.ItemStack(Items.STICK));actors.add(item);item.setDeltaMovement(.5,0,-.25);
            var inside=LegacyB173Block.class.getDeclaredMethod("entityInside",BlockState.class,net.minecraft.world.level.Level.class,BlockPos.class,net.minecraft.world.entity.Entity.class);
            inside.setAccessible(true);inside.invoke(goo.mcc$modern,level.getBlockState(gp),level,gp,item);
            require(Math.abs(item.getDeltaMovement().x-.2)<1E-8&&Math.abs(item.getDeltaMovement().z+.1)<1E-8,"original goo collision slows native item by .4");
            require(Math.abs(level.getBlockState(gp).getCollisionShape(level,gp).max(Direction.Axis.Y)-.8)<1E-5,"original goo collision height");
            String beforeWater=blockDetails(level,gp)+" neighbourBefore="+blockDetails(level,gp.east());
            boolean waterPlaced=level.setBlock(gp.east(),Blocks.WATER.defaultBlockState(),3);
            require(level.getBlockState(gp).is(plastic.mcc$modern)&&Set.of(0,1,4,7,14,15).contains(level.getBlockState(gp).getValue(LegacyB173Block.META)),
                    "water neighbour executes original goo-to-colored-plastic callback; placed="+waterPlaced+" before="+beforeWater+" after="+blockDetails(level,gp)+" neighbourAfter="+blockDetails(level,gp.east())+" expectedPlasticId="+plastic.id);
            level.setBlock(gp.east(),Blocks.AIR.defaultBlockState(),3);level.setBlock(gp,state(plastic,14),3);level.setBlock(gp.east(),Blocks.LAVA.defaultBlockState(),3);
            require(level.getBlockState(gp).is(goo.mcc$modern),"lava neighbour executes original plastic melting callback; actual="+blockDetails(level,gp)+" neighbour="+blockDetails(level,gp.east())+" expectedGooId="+goo.id);
            for(int meta=0;meta<16;meta++)require(plastic.getDroppedItemMeta(meta)==meta,"plastic drops preserve color metadata");
            require(plastic.getColor(14)==-65536,"original red plastic color");

            var jp=origin.offset(0,0,2);level.setBlock(jp,state(jello,0),3);player.setHealth(1);player.setItemInHand(InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);
            for(int bite=1;bite<=6;bite++){
                activate(jello,level,jp,player);
                require(bite==6?level.getBlockState(jp).isAir():level.getBlockState(jp).getValue(LegacyB173Block.META)==bite,"original jello bite metadata "+bite);
            }
            require(player.getHealth()==19,"six original jello bites heal three each");

            var dp=origin.offset(2,0,2);
            for(int facing=0;facing<4;facing++){
                level.setBlock(dp,state(door,facing),2);level.setBlock(dp.above(),state(door,facing|8),2);
                var bounds=level.getBlockState(dp).getCollisionShape(level,dp).bounds();
                require(Math.min(bounds.getXsize(),bounds.getZsize())<.2,"door uses thin original collision bounds");
                activate(door,level,dp,player);
                require((level.getBlockState(dp).getValue(LegacyB173Block.META)&4)!=0&&(level.getBlockState(dp.above()).getValue(LegacyB173Block.META)&4)!=0,"original door opens both halves, facing "+facing);
                activate(door,level,dp.above(),player);require((level.getBlockState(dp).getValue(LegacyB173Block.META)&4)==0,"original top-half interaction closes door");
            }
            // An ordinary native power source reaches the hosted door neighbour callback.
            level.setBlock(dp.west(),Blocks.REDSTONE_BLOCK.defaultBlockState(),3);
            require((level.getBlockState(dp).getValue(LegacyB173Block.META)&4)!=0,"native redstone opens original door");
            level.setBlock(dp.west(),Blocks.AIR.defaultBlockState(),3);
            require((level.getBlockState(dp).getValue(LegacyB173Block.META)&4)==0,"native redstone removal closes original door");

            var tp=origin.offset(4,0,2);level.setBlock(tp,state(glow,0),3);
            require(level.getBlockState(tp).getValue(LegacyB173Block.META)==5,"original glowstick placement chooses floor attachment");
            require(level.getBlockState(tp).getLightEmission(level,tp)==15&&level.getBlockState(tp).getCollisionShape(level,tp).isEmpty(),"glowstick emits full light without collision");
            level.setBlock(tp.below(),Blocks.AIR.defaultBlockState(),3);require(level.getBlockState(tp).isAir(),"original glowstick removes itself when support breaks");
            require(Block.BLOCKS_LIGHT_OPACITY[plexi.id]==1,"original plexiglass opacity table");
            var pp=origin.offset(6,0,2);level.setBlock(pp,state(plexi,0),2);level.setBlock(pp.east(),state(plexi,0),2);
            require(!plexi.isSideVisible(w,pp.getX()+1,pp.getY(),pp.getZ(),5),"original plexiglass hides same-id inner face");

            cow(level,player,actors,origin);
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 PLASTICCRAFT BLOCK TEST: original trampoline, accelerator, goo collision/water, plastic melting/color, six jello bites, four door facings/two halves/redstone, glowstick placement/light/support, plexiglass and cow milking/save/loot passed");
        }catch(ReflectiveOperationException e){throw new IllegalStateException("B1.7.3 PLASTICCRAFT BLOCK TEST failed",e);}
        finally {
            for(var actor:actors)actor.discard();
            player.setItemInHand(InteractionHand.MAIN_HAND,hand);player.setHealth(health);player.setPos(oldPos);player.setDeltaMovement(oldVelocity);
            for(var entry:saved.entrySet())level.setBlock(entry.getKey(),entry.getValue(),2);
        }
    }
    private static void cow(ServerLevel level,net.minecraft.world.entity.player.Player player,java.util.List<net.minecraft.world.entity.Entity> actors,BlockPos pos)throws ReflectiveOperationException {
        var info=MobsB173.all().stream().filter(i->i.registration().type().getSimpleName().equals("EntityPlasticCow")).findFirst().orElseThrow();
        var cow=info.type().create(level);require(cow!=null,"registered original cow constructs");actors.add(cow);cow.setPos(pos.getX()+.5,pos.getY()+2,pos.getZ()+.5);
        require(net.minecraft.world.entity.SpawnPlacements.getPlacementType(info.type())==net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,"only hosted original cow placement registered");
        player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Items.BUCKET));
        require(cow.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction()&&player.getMainHandItem().is(Items.MILK_BUCKET),"original cow callback milks native bucket");
        var mod=info.registration().type().getClassLoader().loadClass("legacyconnector_b173.plasticcraft.mod_PlasticCraft");
        var empty=(legacyconnector.vb1_7_3.net.minecraft.item.Item)mod.getField("itemEPLASTICBUCKET").get(null);
        var milk=(legacyconnector.vb1_7_3.net.minecraft.item.Item)mod.getField("itemMPLASTICBUCKET").get(null);
        player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(empty.id,1,0));
        require(cow.mobInteract(player,InteractionHand.MAIN_HAND).consumesAction()&&StacksB173.toLegacy(player.getMainHandItem()).itemId==milk.id,"original cow callback milks plastic bucket");
        var data=new net.minecraft.nbt.CompoundTag();require(cow.save(data),"original cow saves under registered identity");
        var copy=net.minecraft.world.entity.EntityType.loadEntityRecursive(data,level,e->e);require(copy instanceof LegacyB173Cow&&copy.getType()==cow.getType(),"original cow restores registered host");actors.add(copy);
        var table=level.getServer().reloadableRegistries().getLootTable(cow.getLootTable());
        var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,cow)
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,cow.position())
                .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,level.damageSources().generic())
                .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
        Set<Integer> totals=new java.util.HashSet<>();
        for(long seed=1;seed<=64;seed++){
            int total=0;for(var stack:table.getRandomItems(params,seed)){if(stack.isEmpty())continue;require(stack.is(Items.LEATHER),"original cow drops only leather");total+=stack.getCount();}
            require(total>=0&&total<=2,"original cow leather range");totals.add(total);
        }
        require(totals.equals(Set.of(0,1,2)),"original cow loaded loot covers original leather range");
    }
}
