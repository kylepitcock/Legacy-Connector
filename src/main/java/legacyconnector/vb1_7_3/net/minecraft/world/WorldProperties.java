package legacyconnector.vb1_7_3.net.minecraft.world;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.storage.ServerLevelData;

/**
 * Beta's {@code WorldInfo}: spawn point, seed, clock and weather of the owning world, read from and written to the modern level data
 * (the same data the native commands, sleeping and weather cycle use). A world without server data (a client view, a detached
 * world) keeps the values in the fields below, so reads after writes stay consistent.
 */
public final class WorldProperties {
    private final World world;
    private int spawnX, spawnY = 64, spawnZ, version = 19132, dimensionId;
    private String name = "";
    private boolean raining, thundering;
    private int rainTime, thunderTime;
    private long time;
    private legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound playerNbt;

    public WorldProperties(World world){this.world=java.util.Objects.requireNonNull(world);}

    private ServerLevelData data(){
        return world.mcc$level() instanceof ServerLevel level&&level.getLevelData() instanceof ServerLevelData data?data:null;
    }

    public long getTime(){long live=world.getTime();return world.mcc$level()==null&&live==0?time:live;}
    public void setTime(long time){
        var data=data();
        if(data==null){this.time=time;return;}
        data.setGameTime(time);data.setDayTime(time);
    }

    public long getSeed(){return world.getSeed();}

    public int getSpawnX(){return world.mcc$level() instanceof ServerLevel level?level.getSharedSpawnPos().getX():spawnX;}
    public int getSpawnY(){return world.mcc$level() instanceof ServerLevel level?level.getSharedSpawnPos().getY():spawnY;}
    public int getSpawnZ(){return world.mcc$level() instanceof ServerLevel level?level.getSharedSpawnPos().getZ():spawnZ;}
    public void setSpawnX(int x){setSpawn(x,getSpawnY(),getSpawnZ());}
    public void setSpawnY(int y){setSpawn(getSpawnX(),y,getSpawnZ());}
    public void setSpawnZ(int z){setSpawn(getSpawnX(),getSpawnY(),z);}
    public void setSpawn(int x,int y,int z){
        spawnX=x;spawnY=y;spawnZ=z;
        if(world.mcc$level() instanceof ServerLevel level)level.setDefaultSpawnPos(new BlockPos(x,y,z),0F);
    }

    public String getName(){var data=data();return data!=null?data.getLevelName():name;}
    public void setName(String name){this.name=name==null?"":name;}

    public int getVersion(){return version;}
    public void setVersion(int version){this.version=version;}
    public int getDimensionId(){return dimensionId;}

    /** Beta's {@code WorldInfo.getRaining()/getThundering()}: the live weather of the owning world. */
    public boolean getRaining(){return world.mcc$level()==null?raining:world.isRaining();}
    public void setRaining(boolean raining){
        this.raining=raining;
        var data=data();
        if(data!=null){data.setRaining(raining);data.setClearWeatherTime(0);if(raining&&data.getRainTime()<=0)data.setRainTime(world.mcc$level().random.nextInt(12000)+12000);}
    }
    public boolean getThundering(){return world.mcc$level()==null?thundering:world.isThundering();}
    public void setThundering(boolean thundering){
        this.thundering=thundering;
        var data=data();
        if(data!=null){data.setThundering(thundering);data.setClearWeatherTime(0);if(thundering&&data.getThunderTime()<=0)data.setThunderTime(world.mcc$level().random.nextInt(12000)+3600);}
    }
    public int getRainTime(){var data=data();return data!=null?data.getRainTime():rainTime;}
    public void setRainTime(int ticks){rainTime=ticks;var data=data();if(data!=null)data.setRainTime(ticks);}
    public int getThunderTime(){var data=data();return data!=null?data.getThunderTime():thunderTime;}
    public void setThunderTime(int ticks){thunderTime=ticks;var data=data();if(data!=null)data.setThunderTime(ticks);}

    public legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound getPlayerNbt(){return playerNbt;}
    public void setPlayerNbt(legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound nbt){playerNbt=nbt;}
}
