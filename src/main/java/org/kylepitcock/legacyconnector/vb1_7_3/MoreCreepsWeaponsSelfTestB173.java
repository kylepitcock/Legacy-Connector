package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import java.lang.reflect.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.world.InteractionHand;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual original weapon, projectile and custom-item callbacks exercised through shared native hosts. */
public final class MoreCreepsWeaponsSelfTestB173 {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173WeaponsTest");
    private record Weapon(String item,String projectile,int durability,int consumption,String shooter){}
    private static final List<Weapon> WEAPONS=List.of(
        new Weapon("CREEPSItemGun","CREEPSEntityBullet",2,0,"field_22384_f"),
        new Weapon("CREEPSItemRayGun","CREEPSEntityRay",2,0,"field_22384_f"),
        new Weapon("CREEPSItemShrinkRay","CREEPSEntityShrink",1,0,"field_22384_f"),
        new Weapon("CREEPSItemFrisbee","CREEPSEntityFrisbee",0,1,null),
        new Weapon("CREEPSItemEvilEgg","CREEPSEntityEvilEgg",0,1,"owner"));
    private static void put(Object object,String name,Object value)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);f.set(object,value);return;}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException(detail);}
    private static Object get(Object object,String name)throws ReflectiveOperationException {for(Class<?> type=object.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(object);}catch(NoSuchFieldException ignored){}throw new NoSuchFieldException(name);}
    private static Set<Integer> ids(ServerLevel level){var result=new HashSet<Integer>();for(var entity:level.getAllEntities())result.add(entity.getId());return result;}
    private static void cleanup(ServerLevel level,Set<Integer> before){var added=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getId()))added.add(entity);added.forEach(net.minecraft.world.entity.Entity::discard);}
    private static void canceledSpawn(ServerLevel level,Entity example)throws ReflectiveOperationException {
        var original=(Entity)example.getClass().getConstructor(World.class).newInstance(World.mcc$wrap(level));original.setPosition(example.x+3,example.y,example.z);
        java.util.function.Consumer<net.neoforged.neoforge.event.entity.EntityJoinLevelEvent> cancel=event->{if(event.getEntity() instanceof B173EntityHost host&&host.legacy()==original)event.setCanceled(true);};
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(cancel);
        try{require(!World.mcc$wrap(level).spawnEntity(original)&&original.mcc$modern==null,"canceled actual neutral spawn rolls back original binding");}finally{net.neoforged.neoforge.common.NeoForge.EVENT_BUS.unregister(cancel);}
        require(World.mcc$wrap(level).spawnEntity(original)&&original.mcc$modern instanceof B173EntityHost,"same original neutral entity can retry canceled spawn");original.mcc$modern.discard();
    }
    private static void wireAndSave(ServerLevel level,net.minecraft.world.entity.Entity source,B173EntityHost host)throws ReflectiveOperationException {
        var copy=source.getType().create(level);require(copy instanceof B173EntityHost,"neutral native type factory creates shared host");
        try{
            var spawn=new net.minecraft.network.protocol.game.ClientboundAddEntityPacket(source.getId(),source.getUUID(),source.getX(),source.getY(),source.getZ(),source.getXRot(),source.getYRot(),source.getType(),0,source.getDeltaMovement(),0);
            var buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try{net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.encode(buffer,spawn);copy.recreateFromPacket(net.minecraft.network.protocol.game.ClientboundAddEntityPacket.STREAM_CODEC.decode(buffer));}finally{buffer.release();}
            var values=source.getEntityData().getNonDefaultValues();require(values!=null,"neutral tracked class/state snapshot exists");buffer=new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(),level.registryAccess());
            try{net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.encode(buffer,new net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket(source.getId(),values));copy.getEntityData().assignValues(net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket.STREAM_CODEC.decode(buffer).packedItems());((B173EntityHost)copy).prepareRenderView(.5F);}finally{buffer.release();}
            var clone=(B173EntityHost)copy;require(clone.legacy().getClass()==host.legacy().getClass(),"neutral tracking reconstructs exact original class");require(AnimalRenderStateB173.capture(clone.legacy()).equals(AnimalRenderStateB173.capture(host.legacy())),"neutral metadata wire preserves original scalar fields");
            var received=clone.legacy();var sent=host.legacy();require(received.width==sent.width&&received.height==sent.height&&received.standingEyeHeight==sent.standingEyeHeight&&received.age==sent.age&&received.renderDistanceMultiplier==sent.renderDistanceMultiplier&&received.noClip==sent.noClip,"neutral codec preserves common original dimensions, eye, age, distance and clip state");
            require(Math.abs(copy.getBoundingBox().getXsize()-sent.width)<.00001&&Math.abs(copy.getBoundingBox().getYsize()-sent.height)<.00001&&Math.abs(copy.getBbWidth()-sent.width)<.00001&&Math.abs(copy.getBbHeight()-sent.height)<.00001,"neutral codec common dimensions update native bounds");
            if(sent instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity item){var receivedItem=(legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity)received;require(receivedItem.itemAge==item.itemAge&&receivedItem.pickupDelay==item.pickupDelay,"neutral item codec preserves original age and pickup delay");}
            var tag=new net.minecraft.nbt.CompoundTag();require(source.save(tag),"neutral original is saveable");var loaded=net.minecraft.world.entity.EntityType.loadEntityRecursive(tag,level,e->e);require(loaded instanceof B173EntityHost,"neutral native registry NBT restores host");
            try{var restored=(B173EntityHost)loaded;require(restored.legacy().getClass()==host.legacy().getClass(),"neutral registry restores exact original class");var independent=(Entity)host.legacy().getClass().getConstructor(World.class).newInstance(World.mcc$wrap(level));AnimalRenderStateB173.apply(independent,null,AnimalRenderStateB173.capture(restored.legacy()));var written=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();host.legacy().writeNbt(written);independent.readNbt(NbtB173.fromModern(NbtB173.toModern(written)));var expected=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();independent.writeNbt(expected);var actual=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();restored.legacy().writeNbt(actual);require(NbtB173.toModern(expected).equals(NbtB173.toModern(actual)),"neutral NBT follows actual original read/write policy");}finally{if(loaded!=null)loaded.discard();}
            int[] quads={0};EntityRenderingB173.draw(copy,clone,.5F,(draw,texture)->{},new RendererContextB173.Extras(){@Override public void quad(List<RendererContextB173.Vertex> vertices,org.joml.Matrix4f transform,net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){quads[0]++;require(vertices.size()==4&&!texture.getPath().equals("missingno")&&!texture.getPath().contains("sheets/"),"actual neutral renderer resolves sprite texture");for(var vertex:vertices)require(Double.isFinite(vertex.x()+vertex.y()+vertex.z()+vertex.u()+vertex.v())&&vertex.u()>=0&&vertex.u()<=1&&vertex.v()>=0&&vertex.v()<=1,"actual sprite quad finite position and normalized UV");}});if(RenderersB173.supports(host.legacy().getClass()))require(quads[0]>0,"registered actual neutral renderer emits sprite geometry");else require(quads[0]==0,"original rendererless neutral class stays invisible");
        }finally{if(copy!=null)copy.discard();}
    }
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError e){LOG.error("B1.7.3 CREEPS WEAPONS TEST failed",e);}});}
    private static void run(ServerLevel level){
        var items=ContentB173.items().stream().filter(i->i.getClass().getPackageName().equals("legacyconnector_b173.morecreepsandweirdos")).toList();if(items.isEmpty())return;
        var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);var previous=player.getMainHandItem().copy();var inventoryBefore=player.getInventory().save(new net.minecraft.nbt.ListTag());var previousPos=player.position();var failures=new ArrayList<String>();
        try{var pos=level.getSharedSpawnPos().offset(12,30,12);level.getChunk(pos);player.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);player.setYRot(0);player.setXRot(0);
            for(var weapon:WEAPONS){var before=ids(level);try{
                var item=items.stream().filter(i->i.getClass().getSimpleName().equals(weapon.item)).findFirst().orElseThrow();int count=weapon.consumption>0?2:1;
                player.setItemInHand(InteractionHand.MAIN_HAND,StacksB173.toModern(item.id,count,0));
                MenusB173.withActor(player,()->player.getMainHandItem().getItem().use(level,player,InteractionHand.MAIN_HAND));
                require(player.getMainHandItem().getCount()==count-weapon.consumption,"original stack consumption "+weapon.item);
                require(player.getMainHandItem().getDamageValue()==weapon.durability,"original durability cost "+weapon.item);
                var spawned=new ArrayList<net.minecraft.world.entity.Entity>();for(var entity:level.getAllEntities())if(!before.contains(entity.getId()))spawned.add(entity);
                require(spawned.size()==1,"original weapon spawns exactly one hosted projectile: "+weapon.item+" got="+spawned.size());
                var nativeEntity=spawned.getFirst();require(nativeEntity instanceof B173EntityHost,"original weapon uses neutral host");var host=(B173EntityHost)nativeEntity;var original=host.legacy();
                canceledSpawn(level,original);
                require(original.getClass().getSimpleName().equals(weapon.projectile)&&original.mcc$modern==nativeEntity&&EntityViewsB173.wrap(nativeEntity)==original,"actual projectile class and stable original identity");EntityViewsB173.flush();
                if(weapon.shooter!=null)require(((Entity)get(original,weapon.shooter)).mcc$modern==player,"original projectile preserves exact shooter identity");
                if(weapon.projectile.equals("CREEPSEntityFrisbee")){
                    // Untouched original (World,Entity) constructor uses the thrower's pose but never assigns this field.
                    var raw=original.getClass().getConstructor(World.class,Entity.class).newInstance(World.mcc$wrap(level),PlayerViewB173.of(player));
                    require(get(raw,"field_20051_g")==null&&get(original,"field_20051_g")==null,"actual Frisbee preserves original constructor's unassigned owner policy");
                }
                if(original instanceof legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity itemView){require(nativeEntity instanceof net.minecraft.world.entity.item.ItemEntity,"original ItemEntity uses native item ancestry");var nativeItem=(net.minecraft.world.entity.item.ItemEntity)nativeEntity;require(net.minecraft.world.item.ItemStack.matches(nativeItem.getItem(),StacksB173.toModern(itemView.stack))&&EntityFieldsB173.pickupDelay(nativeItem)==itemView.pickupDelay,"actual original item stack and pickup delay reach native host");}
                wireAndSave(level,nativeEntity,host);
                require(Double.isFinite(original.velocityX)&&Double.isFinite(original.velocityY)&&Double.isFinite(original.velocityZ)&&original.velocityX*original.velocityX+original.velocityY*original.velocityY+original.velocityZ*original.velocityZ>0,"original constructor launch velocity reaches host");
                long ticks=host.originalTickCount();int baseTicks=host.baseTickCount();var start=nativeEntity.position();MenusB173.withActor(player,nativeEntity::tick);
                require(host.originalTickCount()==ticks+1,"native tick invokes exactly one actual projectile tick");
                int expectedBase=Set.of("CREEPSEntityBullet","CREEPSEntityRay","CREEPSEntityShrink").contains(weapon.projectile)?1:0;require(host.baseTickCount()==baseTicks+expectedBase,"original explicit Entity/Item superclass tick runs exactly once, with no appended item phase: "+weapon.projectile);
                require(nativeEntity.isRemoved()||nativeEntity.position().distanceTo(start)>.001,"actual projectile tick moves or resolves collision");
                if(weapon.projectile.equals("CREEPSEntityBullet")){
                    var victim=new net.minecraft.world.entity.npc.Villager(net.minecraft.world.entity.EntityType.VILLAGER,level);victim.setNoAi(true);victim.setPos(player.getX()+5,player.getY(),player.getZ()+1);require(level.addFreshEntity(victim),"native bullet collision victim enters query sections");nativeEntity.setPos(victim.getX(),victim.getY()+.5,victim.getZ()-1);nativeEntity.setDeltaMovement(0,0,1.5);float health=victim.getHealth();int damage=((Number)get(original,"damage")).intValue();MenusB173.withActor(player,nativeEntity::tick);require(victim.getHealth()==health-damage&&nativeEntity.isRemoved(),"actual Bullet ray collision applies original damage and removes projectile");
                }
                if(weapon.projectile.equals("CREEPSEntityFrisbee")){
                    require(!nativeEntity.isRemoved(),"original Frisbee survives flight before owner pickup");put(original,"field_20052_f",true);put(original,"field_20057_a",0);put(original,"field_20051_g",PlayerViewB173.of(player));host.flushView();int inventory=player.getInventory().countItem(StacksB173.toModern(item.id,1,0).getItem());
                    try(var scope=EntityViewsB173.callbackScope()){var actor=EntityViewsB173.wrap(player);require(get(original,"field_20051_g")==actor,"controlled Frisbee callback receives its exact assigned owner view");original.getClass().getMethod("onCollideWithPlayer2",legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity.class).invoke(original,actor);host.flushView();}
                    require(nativeEntity.isRemoved()&&player.getInventory().countItem(StacksB173.toModern(item.id,1,0).getItem())==inventory+1,"controlled original embedded Frisbee pickup branch returns one item to exact assigned owner: removed="+nativeEntity.isRemoved()+" count="+player.getInventory().countItem(StacksB173.toModern(item.id,1,0).getItem())+" before="+inventory);
                }
                LOG.info("B1.7.3 CREEPS WEAPONS TEST: {} actual item use, original class, cost, launch/shooter and native tick passed",weapon.item);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError e){failures.add(weapon.item+": "+e);LOG.error("B1.7.3 CREEPS WEAPONS TEST failed: {}",weapon.item,e);}finally{cleanup(level,before);EntityViewsB173.flush();}}
            for(String name:List.of("CREEPSEntityRocket","CREEPSEntityDesertLizardFireball")){var before=ids(level);try{
                var originalClass=Class.forName(items.getFirst().getClass().getPackageName()+"."+name,true,items.getFirst().getClass().getClassLoader());var shooter=PlayerViewB173.of(player);Entity original=name.endsWith("Rocket")?(Entity)originalClass.getConstructor(World.class,LivingEntity.class,float.class).newInstance(World.mcc$wrap(level),shooter,0F):(Entity)originalClass.getConstructor(World.class,LivingEntity.class,double.class,double.class,double.class).newInstance(World.mcc$wrap(level),shooter,0D,0D,1D);
                require(World.mcc$wrap(level).spawnEntity(original)&&original.mcc$modern instanceof B173EntityHost,"actual mob projectile constructor is hosted: "+name);var entity=original.mcc$modern;var host=(B173EntityHost)entity;String owner=name.endsWith("Rocket")?"field_22384_f":"field_9397_j";require(((Entity)get(original,owner)).mcc$modern==player,"actual mob projectile retains shooter reference: "+name);wireAndSave(level,entity,host);int ticks=host.originalTickCount();MenusB173.withActor(player,entity::tick);require(host.originalTickCount()==ticks+1,"actual mob projectile ticks exactly once: "+name);                require(!entity.isRemoved(),"actual mob projectile survives controlled initial flight: "+name);var wall=net.minecraft.core.BlockPos.containing(player.getX()+5,player.getY()+.5,player.getZ()+1);var previousBlock=level.getBlockState(wall);
                try{level.setBlock(wall,net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);entity.setPos(wall.getX()+.5,wall.getY()+.5,wall.getZ()-.75);entity.setDeltaMovement(0,0,1.5);MenusB173.withActor(player,entity::tick);if(name.endsWith("Rocket"))require(entity.isRemoved()||(Boolean)get(original,"field_22378_l"),"actual Rocket collision explodes or records original embedded-block state");else require(entity.isRemoved()&&original.dead,"actual Fireball block collision runs original explosion/removal callback");}finally{level.setBlock(wall,previousBlock,3);}LOG.info("B1.7.3 CREEPS WEAPONS TEST: {} original constructor, shooter, tick, block collision, packet, NBT and renderer policy passed",name);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError e){failures.add(name+": "+e);LOG.error("B1.7.3 CREEPS WEAPONS TEST failed: {}",name,e);}finally{cleanup(level,before);EntityViewsB173.flush();}}
            for(String name:List.of("CREEPSEntityMoney","CREEPSEntityGooDonut")){var before=ids(level);try{
                var type=Class.forName(items.getFirst().getClass().getPackageName()+"."+name,true,items.getFirst().getClass().getClassLoader());var original=(legacyconnector.vb1_7_3.net.minecraft.entity.ItemEntity)type.getConstructor(World.class).newInstance(World.mcc$wrap(level));original.setPosition(player.getX()+1,player.getY(),player.getZ());require(World.mcc$wrap(level).spawnEntity(original)&&original.mcc$modern instanceof B173EntityHost,"actual custom dropped item hosted: "+name);var entity=original.mcc$modern;var host=(B173EntityHost)entity;wireAndSave(level,entity,host);put(original,"pickupDelay",0);host.flushView();var nativeItem=StacksB173.toModern(original.stack).getItem();int count=player.getInventory().countItem(nativeItem);entity.playerTouch(player);
                if(name.endsWith("Money"))require(entity.isRemoved()&&player.getInventory().countItem(nativeItem)==count+1,"actual original Money pickup grants one item and removes original");else require(!entity.isRemoved()&&player.getInventory().countItem(nativeItem)==count,"actual GooDonut empty pickup override preserves projectile and inventory");LOG.info("B1.7.3 CREEPS WEAPONS TEST: {} original custom-item pickup, packet, NBT and renderer policy passed",name);
            }catch(ReflectiveOperationException|RuntimeException|LinkageError e){failures.add(name+": "+e);LOG.error("B1.7.3 CREEPS WEAPONS TEST failed: {}",name,e);}finally{cleanup(level,before);EntityViewsB173.flush();}}
        }finally{player.getInventory().clearContent();player.getInventory().load(inventoryBefore);player.setItemInHand(InteractionHand.MAIN_HAND,previous);player.setPos(previousPos.x,previousPos.y,previousPos.z);}
        require(failures.isEmpty(),"B1.7.3 CREEPS WEAPONS TEST: "+failures);LOG.info("B1.7.3 CREEPS WEAPONS TEST: all 9 actual item/projectile cases passed; client ticking and GPU drawing not implied");
    }
}
