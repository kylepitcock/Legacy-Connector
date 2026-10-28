package legacyconnector.v1_12_2.net.minecraft.tileentity;

import java.lang.reflect.Field;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.net.minecraft.util.WeightedSpawnerEntity;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.world.level.BaseSpawner;

/**
 * 1.12.2's spawner logic. The spawning itself is the modern game's {@link BaseSpawner} (same rules, same saved keys): mods' subclasses
 * (spawner tile entities, spawner minecarts) get one of their own, a view of a game spawner uses the spawner's. The 1.12 fields hold the
 * settings; they are written to the modern spawner before it runs and read back after.
 */
public abstract class MobSpawnerBaseLogic {
    private int spawnDelay = 20;
    private WeightedSpawnerEntity spawnData = new WeightedSpawnerEntity();
    private double mobRotation;
    private double prevMobRotation;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    private Entity cachedEntity;
    private int maxNearbyEntities = 6;
    private int activatingRangeFromPlayer = 16;
    private int spawnRange = 4;
    private final BaseSpawner mcc$spawner;
    private final boolean mcc$view;

    public MobSpawnerBaseLogic() {
        this.mcc$spawner = new BaseSpawner() {
            @Override
            public void broadcastEvent(net.minecraft.world.level.Level level, net.minecraft.core.BlockPos pos, int eventId) {
                MobSpawnerBaseLogic.this.broadcastEvent(eventId);
            }
        };
        this.mcc$view = false;
    }

    /** The logic of a game spawner (its settings are the modern spawner's). */
    protected MobSpawnerBaseLogic(BaseSpawner modern) {
        this.mcc$spawner = modern;
        this.mcc$view = true;
        mcc$pull();
    }

    public BaseSpawner mcc$modern() {
        return mcc$spawner;
    }

    private static final String[] FIELDS = {"spawnDelay", "minSpawnDelay", "maxSpawnDelay", "spawnCount", "maxNearbyEntities", "requiredPlayerRange", "spawnRange"};
    private static Field[] fields;

    private static synchronized Field[] fields() {
        if (fields == null) {
            Field[] out = new Field[FIELDS.length];
            for (int i = 0; i < FIELDS.length; i++) {
                try {
                    out[i] = BaseSpawner.class.getDeclaredField(FIELDS[i]);
                    out[i].setAccessible(true);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    out[i] = null;
                }
            }
            fields = out;
        }
        return fields;
    }

    private int[] mcc$values() {
        return new int[] {spawnDelay, minSpawnDelay, maxSpawnDelay, spawnCount, maxNearbyEntities, activatingRangeFromPlayer, spawnRange};
    }

    /** Writes the 1.12 settings into the modern spawner. */
    public void mcc$push() {
        Field[] f = fields();
        int[] v = mcc$values();
        for (int i = 0; i < f.length; i++) {
            try {
                if (f[i] != null) f[i].setInt(mcc$spawner, v[i]);
            } catch (IllegalAccessException e) {
                // left as the modern spawner has it
            }
        }
    }

    /** Reads the settings and the rotation of the mob in the cage from the modern spawner. */
    public void mcc$pull() {
        Field[] f = fields();
        int[] v = new int[f.length];
        for (int i = 0; i < f.length; i++) {
            try {
                v[i] = f[i] == null ? mcc$values()[i] : f[i].getInt(mcc$spawner);
            } catch (IllegalAccessException e) {
                v[i] = mcc$values()[i];
            }
        }
        spawnDelay = v[0];
        minSpawnDelay = v[1];
        maxSpawnDelay = v[2];
        spawnCount = v[3];
        maxNearbyEntities = v[4];
        activatingRangeFromPlayer = v[5];
        spawnRange = v[6];
        mobRotation = mcc$spawner.getSpin();
        prevMobRotation = mcc$spawner.getoSpin();
    }

    private net.minecraft.world.level.Level mcc$level() {
        World w = getSpawnerWorld();
        return w == null ? null : w.mcc$level();
    }

    private net.minecraft.core.BlockPos mcc$pos() {
        BlockPos p = getSpawnerPosition();
        return p == null ? net.minecraft.core.BlockPos.ZERO : new net.minecraft.core.BlockPos(p.getX(), p.getY(), p.getZ());
    }

    /** The entity this spawner makes (its id). */
    public ResourceLocation getEntityId() {
        String id = spawnData.getNbt().getString("id");
        return id == null || id.isEmpty() ? null : new ResourceLocation(id);
    }

    public void setEntityId(ResourceLocation id) {
        if (id == null) return;
        spawnData.getNbt().setString("id", id.toString());
        var type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(id.mcc$modern());
        if (type.isPresent()) {
            var level = mcc$level();
            mcc$spawner.setEntityId(type.get(), level, level == null ? net.minecraft.util.RandomSource.create() : level.getRandom(), mcc$pos());
        }
        cachedEntity = null;
    }

    /** True when a player is close enough for the spawner to work. */
    public boolean isActivated() {
        var level = mcc$level();
        BlockPos p = getSpawnerPosition();
        return level != null && p != null && level.hasNearbyAlivePlayer(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, activatingRangeFromPlayer);
    }

    /** One tick of the spawner: the modern spawner's client spin or server spawning. */
    public void updateSpawner() {
        var level = mcc$level();
        if (level == null) return;
        mcc$push();
        if (level instanceof net.minecraft.server.level.ServerLevel server) mcc$spawner.serverTick(server, mcc$pos());
        else mcc$spawner.clientTick(level, mcc$pos());
        mcc$pull();
    }

    /** Waits a new random delay before the next spawn. */
    public void resetTimer() {
        if (maxSpawnDelay <= minSpawnDelay) {
            spawnDelay = minSpawnDelay;
        } else {
            var level = mcc$level();
            java.util.Random r = new java.util.Random();
            spawnDelay = minSpawnDelay + (level == null ? r.nextInt(maxSpawnDelay - minSpawnDelay) : level.getRandom().nextInt(maxSpawnDelay - minSpawnDelay));
        }
        mcc$push();
        broadcastEvent(1);
    }

    public void readFromNBT(NBTTagCompound nbt) {
        mcc$spawner.load(mcc$level(), mcc$pos(), nbt.mcc$modern());
        if (nbt.hasKey("SpawnData", 10)) spawnData = new WeightedSpawnerEntity(1, nbt.getCompoundTag("SpawnData"));
        mcc$pull();
        cachedEntity = null;
    }

    public NBTTagCompound writeToNBT(NBTTagCompound compound) {
        if (getEntityId() == null) return compound;
        mcc$push();
        var tag = mcc$spawner.save(new net.minecraft.nbt.CompoundTag());
        for (String key : tag.getAllKeys()) compound.mcc$modern().put(key, tag.get(key).copy());
        return compound;
    }

    /** The entity drawn spinning inside the cage. */
    public Entity getCachedEntity() {
        var level = mcc$level();
        if (cachedEntity == null && level != null) {
            var modern = mcc$spawner.getOrCreateDisplayEntity(level, mcc$pos());
            cachedEntity = modern == null ? null : org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(modern);
        }
        return cachedEntity;
    }

    /** A block event of the spawner: 1 resets the delay to the minimum on the client. */
    public boolean setDelayToMin(int delay) {
        var level = mcc$level();
        if (delay == 1 && level != null && level.isClientSide) {
            spawnDelay = minSpawnDelay;
            mcc$push();
            return true;
        }
        return false;
    }

    public void setNextSpawnData(WeightedSpawnerEntity p_184993_1_) {
        if (p_184993_1_ == null) return;
        spawnData = p_184993_1_;
        String id = p_184993_1_.getNbt().getString("id");
        if (id != null && !id.isEmpty()) setEntityId(new ResourceLocation(id));
    }

    public abstract void broadcastEvent(int id);

    public abstract World getSpawnerWorld();

    public abstract BlockPos getSpawnerPosition();

    public double getMobRotation() {
        return mobRotation;
    }

    public double getPrevMobRotation() {
        return prevMobRotation;
    }

    /** Forge: the entity a spawner minecart spawns from (null for block spawners). */
    public Entity getSpawnerEntity() {
        return null;
    }
}
