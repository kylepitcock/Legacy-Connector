package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.ChunkPos;
import org.kylepitcock.legacyconnector.core.LegacyModLocks;

/** Explicit population spawns become immutable native NBT on their owning worker, then replay on the native level thread. */
public final class BetaDeferredGenEntities {
    private static final org.slf4j.Logger LOG=org.slf4j.LoggerFactory.getLogger("LegacyConnector/BetaGenerationEntities");
    private static final ThreadLocal<Map<ServerLevel,List<Entity>>> STAGED=ThreadLocal.withInitial(HashMap::new);
    private static final Map<ServerLevel,Long> CURSORS=new java.util.concurrent.ConcurrentHashMap<>();
    private static final Map<ServerLevel,Long> FAILURES=new java.util.concurrent.ConcurrentHashMap<>();
    private static boolean installed;
    private BetaDeferredGenEntities(){}
    public static synchronized void install(){
        if(installed)return;installed=true;var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.LevelTickEvent.Post event)->{if(event.getLevel() instanceof ServerLevel level)replay(level);});
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload event)->{if(event.getLevel() instanceof ServerLevel level){CURSORS.remove(level);FAILURES.remove(level);}});
    }
    public static boolean stage(ServerLevel level,Entity entity){
        var data=BetaDeferredGenData.of(level);if(!data.reserveEntity()){warn(level,"generation entity admission limit",null);return false;}
        try{STAGED.get().computeIfAbsent(level,key->new ArrayList<>()).add(entity);return true;}
        catch(RuntimeException|Error error){data.releaseEntity();throw error;}
    }
    public static void abort(ServerLevel level){
        var groups=STAGED.get();var entities=groups.remove(level);if(groups.isEmpty())STAGED.remove();
        if(entities!=null)for(var entity:entities)BetaDeferredGenData.of(level).releaseEntity();
    }
    public static void snapshot(ServerLevel level){
        try{EntityViewsB173.flush();}catch(RuntimeException|Error error){abort(level);throw error;}
        var groups=STAGED.get();var entities=groups.remove(level);if(groups.isEmpty())STAGED.remove();if(entities==null)return;
        var data=BetaDeferredGenData.of(level);
        try{
            var snapshots=new ArrayList<BetaDeferredGenData.EntitySnapshot>();var children=new HashSet<UUID>();
            for(var entity:entities)entity.getPassengersAndSelf().forEach(node->{if(node!=entity)children.add(node.getUUID());});
            for(var entity:entities){
                if(entity.isRemoved()||children.contains(entity.getUUID()))continue;
                if(entity instanceof B173EntityHost host)host.flushView();
                if(entity.isRemoved())continue;
                var tag=new CompoundTag();if(!entity.save(tag))throw new IllegalStateException("Original generation entity could not serialize: "+entity.getType());
                captureInitialState(entity,tag);
                String owner=entity.getSelfAndPassengers().filter(node->node instanceof B173EntityHost host&&host.legacy()!=null)
                    .map(node->((B173EntityHost)node).legacy().getClass().getName()).distinct().sorted().collect(java.util.stream.Collectors.joining("\n"));
                snapshots.add(new BetaDeferredGenData.EntitySnapshot(tag,owner));
            }
            if(!data.recordEntities(snapshots))throw new IllegalStateException("Original generation entity NBT rejected by bounded transactional admission");
        }finally{for(var entity:entities)data.releaseEntity();}
    }
    private static final String INITIAL_STATE="legacyconnector:beta_initial_scalars";
    private static void captureInitialState(Entity root,CompoundTag tag){
        var nodes=new HashMap<UUID,Entity>();root.getSelfAndPassengers().forEach(node->nodes.put(node.getUUID(),node));
        captureInitialState(tag,nodes);
    }
    private static void captureInitialState(CompoundTag tag,Map<UUID,Entity> nodes){
        var node=nodes.get(tag.getUUID("UUID"));
        if(node instanceof B173EntityHost host&&host.legacy()!=null){
            var initial=new CompoundTag();initial.putString("class",host.legacy().getClass().getName());
            initial.put("fields",AnimalRenderStateB173.captureGenerationScalars(host.legacy()));tag.put(INITIAL_STATE,initial);
        }
        var passengers=tag.getList("Passengers",10);for(int i=0;i<passengers.size();i++)captureInitialState(passengers.getCompound(i),nodes);
    }
    static Runnable prepareInitialState(Object original,CompoundTag initial){
        if(!initial.getAllKeys().equals(Set.of("class","fields"))||!initial.contains("class",net.minecraft.nbt.Tag.TAG_STRING)
            ||!initial.getString("class").equals(original.getClass().getName())||!initial.contains("fields",net.minecraft.nbt.Tag.TAG_COMPOUND))
            throw new IllegalArgumentException("Deferred original scalar state does not match the restored class");
        return AnimalRenderStateB173.prepareGenerationScalars(original,initial.getCompound("fields"));
    }
    private static void prepareInitialTree(CompoundTag tag,Map<UUID,Entity> nodes,List<Runnable> changes){
        var node=nodes.get(tag.getUUID("UUID"));
        if(tag.contains(INITIAL_STATE)){
            if(!tag.contains(INITIAL_STATE,net.minecraft.nbt.Tag.TAG_COMPOUND)||!(node instanceof B173EntityHost host)||host.legacy()==null)
                throw new IllegalArgumentException("Deferred scalar state requires its restored original host");
            changes.add(prepareInitialState(host.legacy(),tag.getCompound(INITIAL_STATE)));
        }
        var passengers=tag.getList("Passengers",10);for(int i=0;i<passengers.size();i++)prepareInitialTree(passengers.getCompound(i),nodes,changes);
    }
    /** Bounded tree validation prevents malformed UUIDs, duplicate passengers and oversized nesting from entering storage. */
    static List<UUID> identities(CompoundTag tag){
        var result=new ArrayList<UUID>();var seen=new HashSet<UUID>();
        return identities(tag,result,seen,0)?List.copyOf(result):null;
    }
    private static boolean identities(CompoundTag tag,List<UUID> result,Set<UUID> seen,int depth){
        if(depth>16||result.size()>=64||!tag.hasUUID("UUID")||tag.getString("id").isEmpty()||position(tag)==null)return false;
        UUID id=tag.getUUID("UUID");if(!seen.add(id))return false;result.add(id);
        var passengers=tag.getList("Passengers",10);for(int i=0;i<passengers.size();i++)if(!identities(passengers.getCompound(i),result,seen,depth+1))return false;
        return true;
    }
    static BlockPos position(CompoundTag tag){
        var position=tag.getList("Pos",6);if(position.size()!=3)return null;
        double x=position.getDouble(0),y=position.getDouble(1),z=position.getDouble(2);
        if(!Double.isFinite(x)||!Double.isFinite(y)||!Double.isFinite(z)||Math.abs(x)>30000000||Math.abs(z)>30000000)return null;
        return BlockPos.containing(x,y,z);
    }
    private static List<java.util.concurrent.locks.ReentrantLock> ownerLocks(String names){
        var result=new ArrayList<java.util.concurrent.locks.ReentrantLock>();
        for(String name:names.split("\n")){
            if(name.isEmpty())continue;java.util.concurrent.locks.ReentrantLock lock=null;
            for(var mod:ContentB173.loadedMods())try{lock=LegacyModLocks.of(Class.forName(name,false,mod.getClass().getClassLoader()));break;}catch(ClassNotFoundException ignored){}
            if(lock==null)throw new IllegalStateException("Original generation entity owner unavailable: "+name);
            if(!result.contains(lock))result.add(lock);
        }return result;
    }
    /** Successful UUIDs are never re-added when a later passenger was rejected; hidden native UUIDs stay pending. */
    static boolean publishIdentities(List<UUID> ids,java.util.function.Predicate<UUID> visible,java.util.function.Predicate<UUID> admit){
        for(var id:ids)if(!visible.test(id)&&!admit.test(id))return false;
        return ids.stream().allMatch(visible);
    }
    private static boolean ready(ServerLevel level,CompoundTag tag){
        BlockPos pos=position(tag);if(pos==null)return false;long chunk=ChunkPos.asLong(pos);
        if(level.getChunkSource().getChunkNow(pos.getX()>>4,pos.getZ()>>4)==null||!level.areEntitiesLoaded(chunk))return false;
        var passengers=tag.getList("Passengers",10);for(int i=0;i<passengers.size();i++)if(!ready(level,passengers.getCompound(i)))return false;
        return true;
    }
    private static void validateVisibleTypes(ServerLevel level,CompoundTag tag){
        var entity=level.getEntity(tag.getUUID("UUID"));
        if(entity!=null){
            String type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
            if(!type.equals(tag.getString("id")))throw new IllegalStateException("Generation entity UUID belongs to a different native type");
            if(entity instanceof B173EntityHost host&&host.legacy()==null)throw new IllegalStateException("Loaded original generation identity is not restored");
            if(tag.contains("betaClass")&&entity instanceof B173EntityHost host&&!host.legacy().getClass().getName().equals(tag.getString("betaClass")))throw new IllegalStateException("Generation entity UUID belongs to a different original class");
        }
        var passengers=tag.getList("Passengers",10);for(int i=0;i<passengers.size();i++)validateVisibleTypes(level,passengers.getCompound(i));
    }
    private static boolean existingTree(ServerLevel level,CompoundTag tag,Entity parent){
        var entity=level.getEntity(tag.getUUID("UUID"));if(entity==null)return false;
        String type=net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
        if(!type.equals(tag.getString("id")))throw new IllegalStateException("Generation entity UUID belongs to a different native type");
        if(tag.contains("betaClass")&&entity instanceof B173EntityHost host&&(host.legacy()==null||!host.legacy().getClass().getName().equals(tag.getString("betaClass"))))throw new IllegalStateException("Generation entity UUID belongs to a different original class");
        if(parent!=null&&entity.getVehicle()!=parent)return false;
        var passengers=tag.getList("Passengers",10);for(int i=0;i<passengers.size();i++)if(!existingTree(level,passengers.getCompound(i),entity))return false;
        return true;
    }
    private static void replay(ServerLevel level){
        if(!level.getServer().isSameThread())throw new IllegalStateException("Beta generation entity replay must run on the native server thread");
        var data=BetaDeferredGenData.cached(level);if(data==null)return;
        var entries=data.entityEntriesAfter(CURSORS.getOrDefault(level,0L),32);if(entries.isEmpty()){CURSORS.put(level,0L);return;}
        for(var entry:entries){CURSORS.put(level,entry.sequence());
            if(!data.entityReady(entry.sequence()))continue;
            CompoundTag tag=entry.nbt();if(!ready(level,tag))continue;
            try{
                var ids=identities(tag);validateVisibleTypes(level,tag);if(existingTree(level,tag,null)){data.acceptedEntity(entry.sequence());continue;}
                var locks=ownerLocks(entry.owner());int acquired=0;
                for(var lock:locks){if(!lock.tryLock())break;acquired++;}
                if(acquired!=locks.size()){for(int i=acquired-1;i>=0;i--)locks.get(i).unlock();continue;}
                var minecraft=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$instance();var previousWorld=minecraft.world;
                try(var context=legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.mcc$context(legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$wrap(level),null)){
                    Entity entity=EntityType.loadEntityRecursive(tag,level,java.util.function.Function.identity());
                    if(entity==null||entity.getSelfAndPassengers().anyMatch(node->node.isRemoved()||(node instanceof B173EntityHost host&&host.legacy()==null))){warn(level,"original entity restore/admission unavailable",null);continue;}
                    var nodes=entity.getSelfAndPassengers().toList();var candidates=new LinkedHashMap<UUID,Entity>();var parents=new LinkedHashMap<UUID,UUID>();
                    for(var node:nodes){candidates.put(node.getUUID(),node);if(node.getVehicle()!=null)parents.put(node.getUUID(),node.getVehicle().getUUID());}
                    if(candidates.size()!=ids.size()||!candidates.keySet().containsAll(ids)){warn(level,"passenger restore/admission unavailable",null);continue;}
                    var initialChanges=new ArrayList<Runnable>();prepareInitialTree(tag,candidates,initialChanges);
                    initialChanges.forEach(Runnable::run);
                    for(var node:nodes)if(node instanceof B173EntityHost host){host.flushView();host.publishRenderState();}
                    for(var node:nodes)node.stopRiding();
                    // addFreshEntity checks native known UUIDs even when an entity is not visible yet; false retains the durable record.
                    boolean accepted=publishIdentities(ids,id->level.getEntity(id)!=null,id->{var node=candidates.get(id);return node!=null&&level.addFreshEntity(node)&&level.getEntity(id)!=null;});
                    for(var relationship:parents.entrySet()){
                        var child=level.getEntity(relationship.getKey());var parent=level.getEntity(relationship.getValue());
                        if(child!=null&&parent!=null&&child.getVehicle()!=parent&&!child.startRiding(parent,true))accepted=false;
                    }
                    for(var id:ids)if(level.getEntity(id) instanceof B173EntityHost host)host.syncView();
                    if(accepted&&existingTree(level,tag,null))data.acceptedEntity(entry.sequence());else warn(level,"native entity admission retained for retry",null);
                }finally{minecraft.world=previousWorld;for(int i=locks.size()-1;i>=0;i--)locks.get(i).unlock();}
            }catch(RuntimeException|LinkageError error){warn(level,"generation entity replay failed; durable NBT retained",error);}
        }
    }
    private static void warn(ServerLevel level,String message,Throwable error){long count=FAILURES.merge(level,1L,Long::sum);if(count<=4||(count&(count-1))==0)LOG.warn("{} in {} ({} retained/rejected attempts)",message,level.dimension().location(),count,error);}
}
