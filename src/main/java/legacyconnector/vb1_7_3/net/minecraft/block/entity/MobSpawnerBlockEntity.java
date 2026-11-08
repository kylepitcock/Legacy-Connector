package legacyconnector.vb1_7_3.net.minecraft.block.entity;

/** Native vanilla spawner view. Setting a legacy entity name changes its native spawn data. */
public class MobSpawnerBlockEntity extends BlockEntity {
    private String spawnedEntityId="Pig";
    private boolean changed;
    public MobSpawnerBlockEntity(){}
    public void setSpawnedEntityId(String id){spawnedEntityId=java.util.Objects.requireNonNull(id);changed=true;mcc$apply();}
    public void mcc$apply(){
        if(!changed||!(mcc$host instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)||spawner.getLevel()==null)return;
        net.minecraft.world.entity.EntityType<?> type=null;
        for(var info:org.kylepitcock.legacyconnector.vb1_7_3.AnimalsB173.all())if(info.registration().name().equals(spawnedEntityId))type=info.type();
        for(var info:org.kylepitcock.legacyconnector.vb1_7_3.MobsB173.all())if(info.registration().name().equals(spawnedEntityId))type=info.type();
        if(type==null){String name=spawnedEntityId.contains(":")?spawnedEntityId:"minecraft:"+spawnedEntityId.replaceAll("([a-z])([A-Z])","$1_$2").toLowerCase(java.util.Locale.ROOT);type=net.minecraft.world.entity.EntityType.byString(name).orElse(null);}
        if(type==null)throw new IllegalArgumentException("Unbridged Beta spawner entity "+spawnedEntityId);
        var level=spawner.getLevel();spawner.setEntityId(type,level.random);var pos=spawner.getBlockPos();var state=level.getBlockState(pos);level.sendBlockUpdated(pos,state,state,3);changed=false;
    }
}
