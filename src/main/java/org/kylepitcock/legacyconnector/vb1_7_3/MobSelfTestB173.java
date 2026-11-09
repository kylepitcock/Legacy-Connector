package org.kylepitcock.legacyconnector.vb1_7_3;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Items;

/** Actual-mod regression fixture; production hosting is selected by vanilla API ancestry, never this name. */
public final class MobSelfTestB173 {
    public static void register(IEventBus game){
        ParticleSelfTestB173.register(game);
        GuiSessionSelfTestB173.register(game);
        ModelSelfTestB173.register(game);
        RendererSelfTestB173.register(game);
        RenderEffectsSelfTestB173.register(game);
        MoCreaturesGuiSelfTestB173.register(game);
        MoCreaturesRosterSelfTestB173.register(game);
        MoCreaturesFeaturesSelfTestB173.register(game);
        MoCreaturesNaturalSpawnSelfTestB173.register(game);
        MoCreaturesKittySelfTestB173.register(game);
        MoCreaturesAquaticSelfTestB173.register(game);
        MoCreaturesAdvancedSelfTestB173.register(game);
        MoCreaturesOgreDestructionSelfTestB173.register(game);
        MoCreaturesRemainingSelfTestB173.register(game);
        MoCreaturesLifecycleExtraSelfTestB173.register(game);
        MoCreaturesPetBehaviorSelfTestB173.register(game);
        MoCreaturesFollowSelfTestB173.register(game);
        MoCreaturesRenderCoverageSelfTestB173.register(game);
        MoCreaturesWaterSpawnSelfTestB173.register(game);
        MoCreaturesVanillaSpawnSelfTestB173.register(game);
        MoCreaturesCarrySelfTestB173.register(game);
        MoCreaturesHorseSaddleSelfTestB173.register(game);
        SpawnCapsSelfTestB173.register(game);
        MoreCreepsRosterSelfTestB173.register(game);
        MoreCreepsWeaponsSelfTestB173.register(game);
        MoreCreepsTamingSelfTestB173.register(game);
        MoreCreepsGameplaySelfTestB173.register(game);
        MoreCreepsCarrierSyncSelfTestB173.register(game);
        MoreCreepsGameplayExtraSelfTestB173.register(game);
        MoreCreepsHunchbackAiSelfTestB173.register(game);
        MoreCreepsArmySelfTestB173.register(game);
        MoreCreepsRenderVariantSelfTestB173.register(game);
        MoreCreepsRenderCoverageSelfTestB173.register(game);
        MoreCreepsJailSelfTestB173.register(game);
        MoreCreepsSquidViewSelfTestB173.register(game);
        MoreCreepsNavigationSelfTestB173.register(game);
        MoreCreepsRemainingSelfTestB173.register(game);
        MoreCreepsEffectsSelfTestB173.register(game);
        MoreCreepsLifecycleExtraSelfTestB173.register(game);
        MoreCreepsGenerationEdgeSelfTestB173.register(game);
        NaturalSpawningTickSelfTestB173.register(game);
        BetaSpawnSchedulingSelfTestB173.register(game);
        BetaEcologySelfTestB173.register(game);
        AnimalLifecycleSelfTestB173.register(game);
        BoundsSelfTestB173.register(game);
        WorldGenEntitiesSelfTestB173.register(game);
        if(Boolean.getBoolean("legacyconnector.entitySelfTest"))game.addListener((ServerStartedEvent e)->run(e.getServer().overworld()));
    }
    private static void require(boolean condition,String message){if(!condition)throw new IllegalStateException("B1.7.3 MOB TEST: "+message);}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var selected=MobsB173.all().stream().filter(i->i.registration().type().getSimpleName().equals("EntityTwilightDeer")).findFirst();
        if(selected.isEmpty()){
            require(ContentB173.entities().stream().noneMatch(r->r.type().getSimpleName().equals("EntityTwilightDeer")),"registered Twilight deer was not accepted by mob host");
            return;
        }
        LegacyB173Cow host=null,copy=null;
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
        var previous=player.getMainHandItem().copy();
        try{
            var info=selected.get();var world=legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level);
            var original=(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity)info.registration().type().getConstructor(legacyconnector.vb1_7_3.net.minecraft.world.World.class).newInstance(world);
            var pos=level.getSharedSpawnPos().above(4);original.setPositionAndAngles(pos.getX()+.5,pos.getY(),pos.getZ()+.5,25,0);
            java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel=event->{if(event.getEntity() instanceof LegacyB173Cow cow&&cow.legacy()==original)event.setCanceled(true);};
            net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
            try{require(!world.spawnEntity(original)&&original.mcc$modern==null,"canceled spawn releases original binding for retry");}
            finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
            require(world.spawnEntity(original),"original World.spawnEntity succeeds");host=(LegacyB173Cow)original.mcc$modern;
            require(host.legacy()==original&&EntityViewsB173.wrap(host)==original,"original entity identity survives views");EntityViewsB173.flush();
            require(Math.abs(host.getBbWidth()-.7F)<.001&&Math.abs(host.getBbHeight()-2.3F)<.001,"original deer dimensions");
            require(host.texturePath().equals("/twilightforest/wilddeer.png"),"original constructor texture");
            require(MobsB173.textureBytes(info,host.texturePath())!=null,"original texture bytes available");
            player.setItemInHand(InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(Items.BUCKET));
            require(host.mobInteract(player,InteractionHand.MAIN_HAND)==InteractionResult.PASS&&player.getMainHandItem().is(Items.BUCKET),"original deer false interaction prevents native cow milking");
            float before=host.getHealth();require(host.hurt(level.damageSources().generic(),3),"native damage accepted");
            require(host.getHealth()==before-3&&original.health==(int)Math.ceil(host.getHealth()),"native/legacy health synchronized");
            host.invulnerableTime=0;require(original.damage(null,1)&&host.getHealth()==before-4,"original Entity.damage reaches native host without recursion");
            var tag=new net.minecraft.nbt.CompoundTag();require(host.save(tag),"registered type saves");
            copy=(LegacyB173Cow)net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,level,entity->entity);
            require(copy!=null&&copy.getType()==host.getType()&&copy.legacy().getClass()==original.getClass(),"reload original type through registry factory");
            require(copy.getHealth()==host.getHealth()&&copy.texturePath().equals(host.texturePath())&&Math.abs(copy.getBbHeight()-2.3F)<.001,"health, texture and size survive reload");
            require(copy.getNavigation()!=null,"native navigation is present");
            var table=level.getServer().reloadableRegistries().getLootTable(host.getLootTable());
            var params=new net.minecraft.world.level.storage.loot.LootParams.Builder(level)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.THIS_ENTITY,host)
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.ORIGIN,host.position())
                    .withParameter(net.minecraft.world.level.storage.loot.parameters.LootContextParams.DAMAGE_SOURCE,level.damageSources().generic())
                    .create(net.minecraft.world.level.storage.loot.parameters.LootContextParamSets.ENTITY);
            java.util.Set<Integer> amounts=new java.util.HashSet<>();
            for(long seed=1;seed<=64;seed++){
                var drops=table.getRandomItems(params,seed);int count=0;
                // Native set_count(0) may leave an empty stack in the returned list; it produces no dropped entity.
                for(var stack:drops){if(stack.isEmpty())continue;require(stack.is(Items.LEATHER),"Beta cow-family loot contains only leather, got "+stack);count+=stack.getCount();}
                require(count>=0&&count<=2,"Beta cow-family drop range is 0–2");amounts.add(count);
            }
            require(amounts.equals(java.util.Set.of(0,1,2)),"loaded native loot table produces the original drop range");
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 MOB TEST: original Twilight deer spawn cancellation/retry, identity, false interaction, native/legacy damage, dimensions, texture, save/reload and 0–2 leather loot passed (native cow AI/model fallback)");
        }catch(ReflectiveOperationException e){throw new IllegalStateException("B1.7.3 MOB TEST failed",e);}
        finally{if(host!=null)host.discard();if(copy!=null)copy.discard();player.setItemInHand(InteractionHand.MAIN_HAND,previous);}
    }
}
