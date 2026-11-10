package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtUtils;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.DeferredGenWrites;

/** Durable ordered Beta writes; data storage is opened only during native level loading. */
public final class BetaDeferredGenData extends SavedData {
    private static final String NAME="legacyconnector_beta_pending_generation";
    private static final Map<ServerLevel,BetaDeferredGenData> LIVE=new ConcurrentHashMap<>();
    private static boolean installed;
    public record Entry(long sequence,BlockPos position,BlockState state,boolean attachment){}
    private final Map<Long,Entry> pending=new LinkedHashMap<>();
    private final Map<BlockPos,Integer> references=new LinkedHashMap<>();
    private final Map<BlockPos,java.util.LinkedHashSet<Long>> positionOrder=new LinkedHashMap<>();
    private final java.util.Set<Long> unready=new java.util.HashSet<>();
    private static final ThreadLocal<Map<BetaDeferredGenData,java.util.ArrayList<Long>>> BUILDING=ThreadLocal.withInitial(java.util.HashMap::new);
    private final Map<BlockPos,CompoundTag> tiles=new LinkedHashMap<>();
    public static final int MAX_ENTITIES=4096;
    static final long MAX_ENTITY_BYTES=16L*1024*1024,MAX_ENTITY_SIZE=256L*1024;
    public record EntityEntry(long sequence,BlockPos position,java.util.UUID uuid,String owner,CompoundTag nbt){
        public EntityEntry{position=position.immutable();nbt=nbt.copy();}
        @Override public CompoundTag nbt(){return nbt.copy();}
    }
    private final java.util.NavigableMap<Long,EntityEntry> entities=new java.util.TreeMap<>();
    private final java.util.Set<java.util.UUID> entityIds=new java.util.HashSet<>();
    private final Map<Long,Integer> pendingChunks=new java.util.HashMap<>();
    private long entityBytes;
    private int entityReservations;
    private long sequence;
    public synchronized boolean reserveEntity(){if(entities.size()+entityReservations>=MAX_ENTITIES)return false;entityReservations++;return true;}
    public synchronized void releaseEntity(){if(entityReservations<=0)throw new IllegalStateException("Unbalanced Beta generation entity reservation");entityReservations--;}
    public synchronized long recordEntity(CompoundTag tag,String owner,boolean building){
        var ids=BetaDeferredGenEntities.identities(tag);long bytes=tag.sizeInBytes();
        if(ids==null||entities.size()>=MAX_ENTITIES||bytes>MAX_ENTITY_SIZE||bytes>MAX_ENTITY_BYTES-entityBytes||ids.stream().anyMatch(entityIds::contains))return -1;
        var position=BetaDeferredGenEntities.position(tag);if(position==null)return -1;
        long id=++sequence;entities.put(id,new EntityEntry(id,position,ids.getFirst(),owner,tag));entityIds.addAll(ids);entityBytes+=bytes;
        if(building){unready.add(id);BUILDING.get().computeIfAbsent(this,data->new java.util.ArrayList<>()).add(id);}
        setDirty();return id;
    }
    public record EntitySnapshot(CompoundTag nbt,String owner){}
    public synchronized boolean recordEntities(List<EntitySnapshot> snapshots){
        if(snapshots.size()>MAX_ENTITIES-entities.size())return false;
        long bytes=entityBytes;var ids=new java.util.HashSet<>(entityIds);
        for(var snapshot:snapshots){var found=BetaDeferredGenEntities.identities(snapshot.nbt());long size=snapshot.nbt().sizeInBytes();
            if(found==null||BetaDeferredGenEntities.position(snapshot.nbt())==null||size>MAX_ENTITY_SIZE||size>MAX_ENTITY_BYTES-bytes)return false;
            for(var uuid:found)if(!ids.add(uuid))return false;bytes+=size;
        }
        for(var snapshot:snapshots)if(recordEntity(snapshot.nbt(),snapshot.owner(),true)<0)throw new IllegalStateException("Beta entity batch admission changed while locked");
        return true;
    }
    public synchronized void abortPopulationEntities(){
        var ids=BUILDING.get().get(this);if(ids==null)return;
        var removed=new java.util.HashSet<Long>();for(long id:ids)if(entities.containsKey(id)){acceptedEntity(id);removed.add(id);}
        ids.removeIf(removed::contains);
    }
    public synchronized List<EntityEntry> entityEntries(){return List.copyOf(entities.values());}
    public synchronized List<EntityEntry> entityEntriesAfter(long after,int limit){return entities.tailMap(after,false).values().stream().limit(limit).toList();}
    public synchronized boolean entityReady(long id){var entry=entities.get(id);return entry!=null&&!unready.contains(id)&&entityChunksReady(entry.nbt);}
    private boolean entityChunksReady(CompoundTag tag){
        if(pendingChunks.containsKey(net.minecraft.world.level.ChunkPos.asLong(BetaDeferredGenEntities.position(tag))))return false;
        var passengers=tag.getList("Passengers",10);for(int i=0;i<passengers.size();i++)if(!entityChunksReady(passengers.getCompound(i)))return false;
        return true;
    }
    public synchronized void acceptedEntity(long id){var entry=entities.remove(id);if(entry==null)return;entityBytes-=entry.nbt.sizeInBytes();entityIds.removeAll(BetaDeferredGenEntities.identities(entry.nbt));unready.remove(id);setDirty();}

    public static synchronized void install(){
        if(installed)return;installed=true;
        BetaDeferredGenEntities.install();
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Load event)->{
            if(event.getLevel() instanceof ServerLevel level){
                if(!level.getServer().isSameThread())throw new IllegalStateException("Beta pending generation must load on the native server thread");
                var data=level.getDataStorage().computeIfAbsent(new SavedData.Factory<>(BetaDeferredGenData::new,BetaDeferredGenData::load,null),NAME);
                LIVE.put(level,data);
                for(var entry:data.entries())DeferredGenWrites.restoreIntercepted(level,entry.sequence(),entry.position(),entry.state(),entry.attachment(),data.tile(entry.position()));
            }
        });
        bus.addListener((net.neoforged.neoforge.event.level.LevelEvent.Unload event)->{
            if(event.getLevel() instanceof ServerLevel level)LIVE.remove(level);
        });
    }
    public static BetaDeferredGenData of(ServerLevel level){
        var data=LIVE.get(level);
        if(data==null)throw new IllegalStateException("Beta pending generation storage was not initialized during level load");
        return data;
    }
    public static BetaDeferredGenData cached(ServerLevel level){return LIVE.get(level);}
    public synchronized long record(BlockPos position,BlockState state,boolean attachment){
        return record(position,state,attachment,legacyconnector.vb1_7_3.net.minecraft.world.World.mcc$generationActive());
    }
    synchronized long record(BlockPos position,BlockState state,boolean attachment,boolean building){
        long id=++sequence;var key=position.immutable();pending.put(id,new Entry(id,key,state,attachment));references.merge(key,1,Integer::sum);pendingChunks.merge(net.minecraft.world.level.ChunkPos.asLong(key),1,Integer::sum);
        positionOrder.computeIfAbsent(key,p->new java.util.LinkedHashSet<>()).add(id);
        if(building){
            unready.add(id);BUILDING.get().computeIfAbsent(this,data->new java.util.ArrayList<>()).add(id);
        }
        setDirty();return id;
    }
    public synchronized boolean contains(BlockPos position){return references.containsKey(position);}
    public synchronized void tile(BlockPos position,CompoundTag tag){
        if(contains(position)){tiles.put(position.immutable(),tag.copy());setDirty();}
    }
    public synchronized CompoundTag tile(BlockPos position){var tag=tiles.get(position);return tag==null?null:tag.copy();}
    public synchronized List<Entry> entries(){return List.copyOf(pending.values());}
    public synchronized boolean canApply(long id){
        var entry=pending.get(id);if(entry==null||unready.contains(id))return false;
        var order=positionOrder.get(entry.position());return order!=null&&order.iterator().next()==id;
    }
    public synchronized List<BlockPos> populationPositions(){
        var ids=BUILDING.get().get(this);if(ids==null)return List.of();
        var positions=new java.util.LinkedHashSet<BlockPos>();for(long id:ids){var entry=pending.get(id);if(entry!=null)positions.add(entry.position());}
        return List.copyOf(positions);
    }
    public synchronized List<BlockPos> completedPopulation(Map<BlockPos,CompoundTag> snapshots){
        var groups=BUILDING.get();var ids=groups.remove(this);if(groups.isEmpty())BUILDING.remove();
        if(ids==null)return List.of();var positions=new java.util.LinkedHashSet<BlockPos>();
        for(long id:ids){unready.remove(id);var entry=pending.get(id);if(entry!=null)positions.add(entry.position());}
        snapshots.forEach((position,tag)->{if(positions.contains(position))tiles.put(position.immutable(),tag.copy());});
        if(!ids.isEmpty())setDirty();
        return List.copyOf(positions);
    }
    public synchronized void accepted(long id){
        var entry=pending.remove(id);if(entry==null)return;
        references.computeIfPresent(entry.position(),(key,count)->count<=1?null:count-1);
        pendingChunks.computeIfPresent(net.minecraft.world.level.ChunkPos.asLong(entry.position()),(key,count)->count<=1?null:count-1);
        var order=positionOrder.get(entry.position());if(order!=null){order.remove(id);if(order.isEmpty())positionOrder.remove(entry.position());}unready.remove(id);
        if(!contains(entry.position()))tiles.remove(entry.position());setDirty();
    }
    public static BetaDeferredGenData load(CompoundTag tag,HolderLookup.Provider registries){
        var data=new BetaDeferredGenData();data.sequence=tag.getLong("sequence");
        var entries=tag.getList("pending",10);
        for(int i=0;i<entries.size();i++){
            var value=entries.getCompound(i);long id=value.getLong("sequence");
            var position=BlockPos.of(value.getLong("position"));
            var state=NbtUtils.readBlockState(BuiltInRegistries.BLOCK.asLookup(),value.getCompound("state"));
            data.pending.put(id,new Entry(id,position,state,value.getBoolean("attachment")));data.sequence=Math.max(data.sequence,id);
            data.references.merge(position,1,Integer::sum);data.pendingChunks.merge(net.minecraft.world.level.ChunkPos.asLong(position),1,Integer::sum);
            data.positionOrder.computeIfAbsent(position,key->new java.util.LinkedHashSet<>()).add(id);
        }
        var tiles=tag.getList("tiles",10);for(int i=0;i<tiles.size();i++){var value=tiles.getCompound(i);data.tiles.put(BlockPos.of(value.getLong("position")),value.getCompound("tile").copy());}
        var entities=tag.getList("entities",10);
        for(int i=0;i<entities.size();i++){
            var value=entities.getCompound(i);var nbt=value.getCompound("entity");var ids=BetaDeferredGenEntities.identities(nbt);var position=BetaDeferredGenEntities.position(nbt);long bytes=nbt.sizeInBytes();
            if(ids==null||position==null||data.entities.size()>=MAX_ENTITIES||bytes>MAX_ENTITY_SIZE||bytes>MAX_ENTITY_BYTES-data.entityBytes||ids.stream().anyMatch(data.entityIds::contains))throw new IllegalStateException("Invalid or oversized saved Beta generation entity queue; records were not discarded");
            long id=value.getLong("sequence");if(id<=0||data.entities.containsKey(id)||data.pending.containsKey(id))throw new IllegalStateException("Duplicate or invalid saved Beta entity sequence");data.entities.put(id,new EntityEntry(id,position,ids.getFirst(),value.getString("owner"),nbt));data.entityIds.addAll(ids);data.entityBytes+=bytes;data.sequence=Math.max(data.sequence,id);
        }
        return data;
    }
    @Override public synchronized CompoundTag save(CompoundTag tag,HolderLookup.Provider registries){
        tag.putLong("sequence",sequence);var savedEntities=new ListTag();
        for(var entity:entities.values())if(!unready.contains(entity.sequence())){var value=new CompoundTag();value.putLong("sequence",entity.sequence());value.putString("owner",entity.owner());value.put("entity",entity.nbt());savedEntities.add(value);}
        tag.put("entities",savedEntities);var entries=new ListTag();
        var committedPositions=new java.util.HashSet<BlockPos>();
        for(var entry:pending.values()){
            if(unready.contains(entry.sequence()))continue;
            committedPositions.add(entry.position());
            var value=new CompoundTag();value.putLong("sequence",entry.sequence());value.putLong("position",entry.position().asLong());
            value.put("state",NbtUtils.writeBlockState(entry.state()));value.putBoolean("attachment",entry.attachment());entries.add(value);
        }tag.put("pending",entries);
        var savedTiles=new ListTag();tiles.forEach((position,tile)->{if(committedPositions.contains(position)){var value=new CompoundTag();value.putLong("position",position.asLong());value.put("tile",tile.copy());savedTiles.add(value);}});tag.put("tiles",savedTiles);return tag;
    }
    @Override public synchronized boolean isDirty(){return super.isDirty();}
    @Override public synchronized void setDirty(boolean dirty){super.setDirty(dirty);}
    /** Keep the native snapshot-and-clear transaction atomic with worker-side additions. */
    @Override public synchronized void save(java.io.File file,HolderLookup.Provider registries){super.save(file,registries);}
}
