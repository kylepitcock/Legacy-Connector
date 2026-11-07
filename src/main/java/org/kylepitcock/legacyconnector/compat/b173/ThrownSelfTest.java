package org.kylepitcock.legacyconnector.compat.b173;

import org.kylepitcock.legacyconnector.vb1_7_3.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Actual mod use callbacks, projectile flight, renderer data and impacts. */
public final class ThrownSelfTest {
    public static void run(ServerLevel level,ServerPlayer player,BlockPos base) {
        int passed=0;
        // Reuse the already-visible entity section exercised by the bucket/bow test.
        BlockPos pos=base.offset(512,-20,0);
        AABB area=new AABB(pos).inflate(32);
        for(var item:ContentB173.items()) {
            String name=item.getClass().getSimpleName();
            if(!java.util.Set.of("ItemNotchHammer","ItemSlimeEggs").contains(name))continue;
            boolean hammer=name.equals("ItemNotchHammer");
            var target=net.minecraft.world.entity.EntityType.ZOMBIE.create(level);
            target.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+4.5);target.setNoAi(true);level.addFreshEntity(target);
            try {
                equip(player,pos,item.id);
                player.getMainHandItem().getItem().use(level,player,InteractionHand.MAIN_HAND);
                require(hammer?player.getMainHandItem().getDamageValue()==1:player.getMainHandItem().getCount()==1,name+" use cost before spawn query");
                var hosts=level.getEntitiesOfClass(LegacyB173Arrow.class,area);
                require(hosts.size()==1,name+" spawns exactly one projectile: found "+hosts.size()+", item host "+item.mcc$modern.getClass());
                var host=hosts.getFirst();
                require(host.isThrown(),name+" uses thrown renderer type");
                require(host.getOwner()==player,name+" shooter retained");
                require(hammer?player.getMainHandItem().getDamageValue()==1:player.getMainHandItem().getCount()==1,name+" use cost");
                if(hammer) {
                    require(!host.texture().equals(ProjectileTexturesB173.FALLBACK),"wave PNG selected");
                    var assets=new java.util.HashMap<String,java.util.function.Supplier<byte[]>>();ProjectileTexturesB173.contribute(assets);
                    var bytes=assets.get("assets/"+host.texture().getNamespace()+"/"+host.texture().getPath());
                    try(var stream=host.legacy.getClass().getModule().getResourceAsStream("aether/other/NotchWave.png")) {
                        require(stream!=null && bytes!=null && java.util.Arrays.equals(bytes.get(),stream.readAllBytes()),"original wave PNG packaged");
                    }
                } else require(!host.sprite().isEmpty() && host.sprite().getItem()==item.mcc$modern,"egg uses its registered item texture");
                var tag=new net.minecraft.nbt.CompoundTag();host.save(tag);
                var copy=(LegacyB173Arrow)net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,level,e->e);
                require(copy!=null && copy.legacy!=null && copy.legacy.getClass()==host.legacy.getClass(),name+" class restored");
                require(copy.isThrown() && copy.texture().equals(host.texture()) && net.minecraft.world.item.ItemStack.matches(copy.sprite(),host.sprite()),name+" renderer restored");
                copy.discard();
                var start=host.position();float health=target.getHealth();
                for(int tick=0;tick<20 && !host.isRemoved() && target.getHealth()==health;tick++)host.tick();
                require(host.position().distanceTo(start)>0.1,name+" flight");
                require(target.getHealth()<health,name+" impact damages mob");
                if(!hammer)require(target.getRemainingFireTicks()>0,"egg impact ignites mob");
            } catch(Exception e) { throw new IllegalStateException(name+" thrown projectile test",e); }
            finally { target.discard();for(var host:level.getEntitiesOfClass(LegacyB173Arrow.class,area))host.discard(); }
            var original=new java.util.LinkedHashMap<BlockPos,net.minecraft.world.level.block.state.BlockState>();
            try {
                for(int x=-2;x<=2;x++)for(int y=-1;y<=3;y++) {
                    var at=pos.offset(x,y,4);original.put(at,level.getBlockState(at));level.setBlock(at,Blocks.STONE.defaultBlockState(),2);
                }
                equip(player,pos,item.id);player.getMainHandItem().getItem().use(level,player,InteractionHand.MAIN_HAND);
                var hosts=level.getEntitiesOfClass(LegacyB173Arrow.class,area);require(hosts.size()==1,name+" wall shot spawned");
                var host=hosts.getFirst();
                for(int tick=0;tick<20 && !host.isRemoved();tick++)host.tick();
                require(host.isRemoved(),name+" wall impact removes projectile");
            } finally {
                for(var host:level.getEntitiesOfClass(LegacyB173Arrow.class,area))host.discard();
                original.forEach((at,state)->level.setBlock(at,state,2));player.getInventory().clearContent();
            }
            passed++;
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 ACTUAL THROWN TEST: {} spawn, renderer, save/reload, flight, mob and wall impacts passed",name);
        }
        org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 ACTUAL THROWN TEST: {} items passed",passed);
    }
    private static void equip(ServerPlayer player,BlockPos pos,int id) {
        player.getInventory().clearContent();player.getInventory().selected=0;
        player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(id,Math.min(2,legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS[id].getMaxCount()),0));
        player.setPos(pos.getX()+0.5,pos.getY(),pos.getZ()+0.5);player.setYRot(0);player.setXRot(0);
    }
    private static void require(boolean ok,String message) { if(!ok)throw new IllegalStateException(message); }
}
