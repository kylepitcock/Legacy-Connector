package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual-mod roster baseline. Specialized behavior fixtures remain separate acceptance requirements. */
public final class MoreCreepsRosterSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173RosterTest");
    private static final Set<String> EXPECTED=Set.of("CREEPSEntityArmyGuy","CREEPSEntityArmyGuyArm","CREEPSEntityAtom","CREEPSEntityBabyMummy","CREEPSEntityBigBaby","CREEPSEntityBlackSoul","CREEPSEntityBlorp","CREEPSEntityBubbleScum","CREEPSEntityBum","CREEPSEntityCamel","CREEPSEntityCamelJockey","CREEPSEntityCastleCritter","CREEPSEntityCastleGuard","CREEPSEntityCastleKing","CREEPSEntityDesertLizard","CREEPSEntityDigBug","CREEPSEntityEvilChicken","CREEPSEntityEvilCreature","CREEPSEntityEvilLight","CREEPSEntityEvilPig","CREEPSEntityEvilScientist","CREEPSEntityEvilSnowman","CREEPSEntityFloob","CREEPSEntityFloobBeam","CREEPSEntityFloobShip","CREEPSEntityFloobMotherShip","CREEPSEntityG","CREEPSEntityGooGoat","CREEPSEntityGuineaPig","CREEPSEntityHippo","CREEPSEntityHorseHead","CREEPSEntityHotdog","CREEPSEntityHunchback","CREEPSEntityHunchbackSkeleton","CREEPSEntityInvisibleMan","CREEPSEntityKid","CREEPSEntityLawyerFromHell","CREEPSEntityLolliman","CREEPSEntityManDog","CREEPSEntityMummy","CREEPSEntityPreacher","CREEPSEntityPyramidGuardian","CREEPSEntityRatMan","CREEPSEntityRobotTed","CREEPSEntityRobotTodd","CREEPSEntityRocketGiraffe","CREEPSEntityRockMonster","CREEPSEntitySneakySal","CREEPSEntitySnowDevil","CREEPSEntityThief","CREEPSEntityTombstone","CREEPSEntityTrophy","CREEPSEntityZebra");
    private static final Set<String> AQUATIC=Set.of("CREEPSEntitySquimp");
    private MoreCreepsRosterSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{var level=event.getServer().overworld();MenusB173.withActor(net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level),()->run(level));}catch(RuntimeException|LinkageError error){LOG.error("B1.7.3 CREEPS ROSTER TEST failed",error);}});}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException(detail);}
    private static void finite(org.joml.Matrix4f matrix){for(float value:matrix.get(new float[16]))require(Float.isFinite(value),"nonfinite original renderer extra transform");}
    private static void fieldIfPresent(Object value,String name,Object data)throws ReflectiveOperationException {for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())try{var field=type.getDeclaredField(name);field.setAccessible(true);field.set(value,data);return;}catch(NoSuchFieldException ignored){}}
    private static void rendererExtras(LegacyB173Animal host,ContentB173.Registration registration)throws ReflectiveOperationException {
        var snapshot=AnimalRenderStateB173.capture(host.legacy());var toggles=new LinkedHashMap<legacyconnector.vb1_7_3.net.minecraft.src.SettingBoolean,HashMap<String,Boolean>>();

        var camera=new LivingEntity(host.legacy().world){};camera.setPosition(host.getX()+2,host.getY(),host.getZ());int[] counts=new int[3];
        try{
            fieldIfPresent(host.legacy(),"name","Roster "+registration.name());fieldIfPresent(host.legacy(),"displayname",true);fieldIfPresent(host.legacy(),"tamed",true);fieldIfPresent(host.legacy(),"kittystate",3);
            AnimalRenderingB173.draw(host,.5F,(draw,texture)->finite(draw.transform()),new RendererContextB173.Extras(){
                @Override public LivingEntity camera(){return camera;}
                @Override public void item(LivingEntity entity,legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,org.joml.Matrix4f transform,RendererContextB173.Style style){counts[0]++;require(stack!=null&&!StacksB173.toModern(stack).isEmpty(),"original equipped item resolves to native item");finite(transform);finite(style.textureMatrix());}
                @Override public void text(String text,float x,float y,int argb,boolean shadow,org.joml.Matrix4f transform,RendererContextB173.Style style){counts[1]++;require(text!=null&&Float.isFinite(x)&&Float.isFinite(y),"original nameplate has finite text position");finite(transform);finite(style.textureMatrix());}
                @Override public void quad(List<RendererContextB173.Vertex> vertices,org.joml.Matrix4f transform,net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){counts[2]++;require(vertices.size()==4,"original overlay emits complete quad");finite(transform);finite(style.textureMatrix());for(var vertex:vertices)require(Double.isFinite(vertex.x())&&Double.isFinite(vertex.y())&&Double.isFinite(vertex.z())&&Double.isFinite(vertex.u())&&Double.isFinite(vertex.v()),"original overlay finite positions/UVs");if(style.texture2d())require(!texture.equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("missingno")),"original overlay texture resource exists");}
            });

            LOG.info("B1.7.3 CREEPS RENDER EXTRAS TEST: {} original renderer emitted {} items, {} text runs, {} overlay quads with finite transforms/styles and resolved resources",registration.type().getSimpleName(),counts[0],counts[1],counts[2]);
        }finally{AnimalRenderStateB173.apply(host.legacy(),null,snapshot);toggles.forEach((setting,values)->{setting.values.clear();setting.values.putAll(values);});}
    }
    private static void run(net.minecraft.server.level.ServerLevel level){
        var registrations=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")&&LivingEntity.class.isAssignableFrom(r.type())).toList();
        if(registrations.isEmpty())return;
        var names=new HashSet<String>();registrations.forEach(r->names.add(r.type().getSimpleName()));require(names.equals(EXPECTED),"original MoreCreeps registration roster changed: "+names);
        var items=ContentB173.items().stream().filter(item->item.mcc$owner==registrations.getFirst().mod()).toList();require(items.size()==40,"original MoreCreeps item roster expected40 got"+items.size());
        var failures=new ArrayList<String>();int index=0;
        for(var registration:registrations){
            String stage="constructor";var cleanup=new ArrayList<net.minecraft.world.entity.Entity>();var existingEntities=new HashSet<Integer>();for(var entity:level.getAllEntities())existingEntities.add(entity.getId());
            var blocks=new LinkedHashMap<net.minecraft.core.BlockPos,net.minecraft.world.level.block.state.BlockState>();
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);var previous=player.getMainHandItem().copy();var previousPos=player.position();
            try{
                var world=World.mcc$wrap(level);var original=(LivingEntity)registration.type().getConstructor(World.class).newInstance(world);
                var position=level.getSharedSpawnPos().offset(12,24,12);index++;level.getChunk(position);
                original.setPosition(position.getX()+.5,position.getY(),position.getZ()+.5);
                if(AQUATIC.contains(registration.type().getSimpleName()))for(int x=-3;x<=3;x++)for(int y=-2;y<=3;y++)for(int z=-3;z<=3;z++){
                    var block=position.offset(x,y,z);blocks.put(block,level.getBlockState(block));level.setBlock(block,net.minecraft.world.level.block.Blocks.WATER.defaultBlockState(),3);
                }
                stage="admission/spawn";require(world.spawnEntity(original),"original World.spawnEntity rejected; animalClosure="+AnimalsB173.unsupportedBehaviour(registration.type()));
                require(original.mcc$modern instanceof LegacyB173Animal,"original roster entity uses generic living host");var host=(LegacyB173Animal)original.mcc$modern;cleanup.add(host);
                require(EntityViewsB173.wrap(host)==original,"original identity preserved");EntityViewsB173.flush();
                stage="original renderer";require(RenderersB173.supports(registration.type()),"original AddRenderer entry not admitted");var draws=new ArrayList<ModelGeometryB173.Draw>();
                AnimalRenderingB173.draw(host,.5F,(draw,texture)->{require(!texture.equals(net.minecraft.resources.ResourceLocation.withDefaultNamespace("missingno")),"original renderer missing texture");draws.add(draw);});
                require(!draws.isEmpty(),"original renderer emitted no geometry");int faces=0;
                for(var draw:draws)for(var box:draw.cuboids())for(var face:ModelGeometryB173.vertices(box)){faces++;for(var vertex:face){var transformed=draw.transform().transformPosition(new org.joml.Vector3f(vertex.x()*draw.scale(),vertex.y()*draw.scale(),vertex.z()*draw.scale()));require(Float.isFinite(transformed.x)&&Float.isFinite(transformed.y)&&Float.isFinite(transformed.z)&&Float.isFinite(vertex.u())&&Float.isFinite(vertex.v()),"nonfinite original geometry");}}
                stage="original renderer extras";rendererExtras(host,registration);
                stage="native spawn/tracking packet";var nativeInfo=AnimalsB173.all().stream().filter(i->i.registration().type()==registration.type()).findFirst().orElseThrow();var clone=new LegacyB173Animal(nativeInfo.type(),level,null);cleanup.add(clone);
                var spawn=new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(host.getId(),host.getUUID(),host.getX(),host.getY(),host.getZ(),host.getXRot(),host.getYRot(),host.getType(),0,host.getDeltaMovement(),host.getYHeadRot());
                var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
                try{net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer,spawn);clone.recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.decode(buffer));}finally{buffer.release();}
                var values=host.getEntityData().getNonDefaultValues();require(values!=null,"tracking snapshot absent");
                buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
                try{net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(host.getId(),values));clone.getEntityData().assignValues(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());clone.applyTrackedRenderData();}finally{buffer.release();}
                require(AnimalRenderStateB173.capture(clone.legacy()).equals(host.renderState()),"original scalar tracking mismatch");
                stage="registry NBT";var tag=new net.minecraft.nbt.CompoundTag();require(host.save(tag),"native host not saveable");var loaded=net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,level,e->e);require(loaded instanceof LegacyB173Animal,"registry reload lost generic host");cleanup.add(loaded);var restored=(LegacyB173Animal)loaded;
                require(restored.legacy().getClass()==registration.type()&&restored.getHealth()==host.getHealth(),"registry reload original identity/health");
                var originalNbt=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();var restoredNbt=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();original.writeNbt(originalNbt);
                var independent=(LivingEntity)registration.type().getConstructor(World.class).newInstance(world);
                // Original readers may retain randomized constructor fields (CastleGuard even PUTs BaseTexture while reading).
                // Compare equal pre-read scalar states, rather than assuming the original restores every saved field.
                AnimalRenderStateB173.apply(independent,null,AnimalRenderStateB173.capture(restored.legacy()));
                var randomField=legacyconnector.vb1_7_3.net.minecraft.entity.Entity.class.getDeclaredField("random");randomField.setAccessible(true);randomField.set(independent,new Random(173));randomField.set(restored.legacy(),new Random(173));independent.readNbt(NbtB173.fromModern(tag.getCompound("beta")));restored.readAdditionalSaveData(tag);restoredNbt=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();restored.legacy().writeNbt(restoredNbt);var expectedNbt=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();independent.writeNbt(expectedNbt);
                require(NbtB173.toModern(expectedNbt).equals(NbtB173.toModern(restoredNbt)),"native registry NBT differs from independent original read/write policy: expected="+NbtB173.toModern(expectedNbt)+" actual="+NbtB173.toModern(restoredNbt));
                stage="original native tick callbacks";for(int tick=0;tick<3;tick++)host.tick();
                require(Double.isFinite(host.getX())&&Double.isFinite(host.getY())&&Double.isFinite(host.getZ()),"nonfinite original movement");
                stage="original empty-hand interaction";player.setPos(host.getX()+1,host.getY(),host.getZ());player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,net.minecraft.world.item.ItemStack.EMPTY);host.mobInteract(player,net.minecraft.world.InteractionHand.MAIN_HAND);host.stopRiding();
                stage="original/native damage";host.invulnerableTime=0;float health=host.getHealth();host.hurt(level.damageSources().playerAttack(player),1);require(original.health==(int)Math.ceil(host.getHealth()),"health/native original divergence: original="+original.health+" native="+host.getHealth()+" before="+health);if(registration.type().getSimpleName().equals("CREEPSEntityPreacher"))require(original.health==(int)Math.ceil(health)+1,"original Preacher damage1 heals exactly1");
                LOG.info("B1.7.3 CREEPS ROSTER TEST: {} baseline passed: original constructor/spawn/identity, {} faces, native spawn+metadata packet, three ticks, empty-hand interaction, damage and registry NBT; specialized AI/gameplay not implied",registration.type().getSimpleName(),faces);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError error){String detail=registration.type().getSimpleName()+" at "+stage+": "+error;failures.add(detail);LOG.error("B1.7.3 CREEPS ROSTER TEST failed: {}",detail,error);}
            finally{var spawned=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!existingEntities.contains(entity.getId()))spawned.add(entity);for(var entity:spawned)entity.discard();for(var entity:cleanup)entity.discard();blocks.forEach((pos,state)->level.setBlock(pos,state,3));player.stopRiding();player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,previous);player.setPos(previousPos.x,previousPos.y,previousPos.z);EntityViewsB173.flush();}
        }
        require(failures.isEmpty(),"B1.7.3 CREEPS ROSTER TEST: "+failures.size()+" failed of "+registrations.size()+": "+failures);
        LOG.info("B1.7.3 CREEPS ROSTER TEST: all {} original registrations and {} items passed core baseline; specialized behavior proofs tracked separately",registrations.size(),items.size());
    }
}
