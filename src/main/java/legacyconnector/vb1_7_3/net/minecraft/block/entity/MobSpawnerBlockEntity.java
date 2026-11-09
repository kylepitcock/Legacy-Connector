package legacyconnector.vb1_7_3.net.minecraft.block.entity;

/** Native vanilla spawner view. Setting a legacy entity name changes its native spawn data. */
public class MobSpawnerBlockEntity extends BlockEntity {
    private String spawnedEntityId="Pig";
    private boolean changed;
    public MobSpawnerBlockEntity(){}
    public void setSpawnedEntityId(String id){spawnedEntityId=java.util.Objects.requireNonNull(id);changed=true;mcc$apply();}
    public void mcc$apply(){
        if(!changed||!(mcc$host instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner))return;
        var level=spawner.getLevel()!=null?spawner.getLevel():world==null?null:world.mcc$level();if(level==null)return;
        net.minecraft.world.entity.EntityType<?> type=null;
        for(var info:org.kylepitcock.legacyconnector.vb1_7_3.AnimalsB173.all())if(info.registration().name().equals(spawnedEntityId))type=info.type();
        for(var info:org.kylepitcock.legacyconnector.vb1_7_3.MobsB173.all())if(info.registration().name().equals(spawnedEntityId))type=info.type();
        if(type==null){String name=spawnedEntityId.contains(":")?spawnedEntityId:"minecraft:"+spawnedEntityId.replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(java.util.Locale.ROOT);type=net.minecraft.world.entity.EntityType.byString(name).orElse(null);}
        if(type==null)throw new IllegalArgumentException("Unbridged Beta spawner entity "+spawnedEntityId);
        if(org.kylepitcock.legacyconnector.vb1_7_3.WorldGenWritesB173.generation()){
            var lookup=level.registryAccess();var nbt=spawner.saveWithFullMetadata(lookup);var data=nbt.getCompound("SpawnData");var entity=data.getCompound("entity");entity.putString("id",net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());data.put("entity",entity);nbt.put("SpawnData",data);
            // Native SpawnerBlockEntity.loadAdditional passes its full Level into an updating callback.
            // BaseSpawner accepts null explicitly, preserving spawn parameters without requesting a generating chunk.
            spawner.getSpawner().load(null,spawner.getBlockPos(),nbt);
        }else {spawner.setEntityId(type,level.random);var pos=spawner.getBlockPos();var state=spawner.getBlockState();level.sendBlockUpdated(pos,state,state,3);}changed=false;
    }
}
