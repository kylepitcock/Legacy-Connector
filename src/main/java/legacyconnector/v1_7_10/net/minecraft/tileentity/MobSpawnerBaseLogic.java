package legacyconnector.v1_7_10.net.minecraft.tileentity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityList;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLiving;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTBase;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.block.entity.SpawnerBlockEntity;
import org.kylepitcock.legacyconnector.v1_7_10.entity.LegacyHosts;

/**
 * 1.7.10's spawner logic. Mods subclass it for their own spawners ({@link #getSpawnerWorld} and the coordinates, {@link #func_98267_a}
 * to signal the client) and get 1.7.10's spawning ({@link #updateSpawner}). A view of a vanilla spawner block ({@code host}) drives
 * the modern spawner instead: the entity it spawns, its settings and its ticking are the modern block entity's.
 */
public class MobSpawnerBaseLogic {
    private static final Map<String, String> VANILLA = Map.ofEntries(
            Map.entry("Zombie", "zombie"), Map.entry("Skeleton", "skeleton"), Map.entry("Spider", "spider"), Map.entry("CaveSpider", "cave_spider"),
            Map.entry("Creeper", "creeper"), Map.entry("Blaze", "blaze"), Map.entry("Silverfish", "silverfish"), Map.entry("Enderman", "enderman"),
            Map.entry("Slime", "slime"), Map.entry("PigZombie", "zombified_piglin"), Map.entry("Ghast", "ghast"), Map.entry("LavaSlime", "magma_cube"),
            Map.entry("Witch", "witch"), Map.entry("Villager", "villager"), Map.entry("Pig", "pig"), Map.entry("Cow", "cow"),
            Map.entry("Chicken", "chicken"), Map.entry("Sheep", "sheep"), Map.entry("Wolf", "wolf"), Map.entry("Bat", "bat"),
            Map.entry("Squid", "squid"), Map.entry("Ozelot", "ocelot"), Map.entry("EntityHorse", "horse"), Map.entry("Giant", "giant"),
            Map.entry("MushroomCow", "mooshroom"), Map.entry("VillagerGolem", "iron_golem"), Map.entry("SnowMan", "snow_golem"),
            Map.entry("WitherBoss", "wither"), Map.entry("EnderDragon", "ender_dragon"));

    private final SpawnerBlockEntity host;

    public int spawnDelay = 20;
    private String entityTypeName = "Pig";
    private List potentialEntitySpawns;
    private WeightedRandomMinecart randomEntity;
    /** Rotation of the mob drawn inside the cage, this and the previous frame. */
    public double field_98287_c;
    public double field_98284_d;
    private int minSpawnDelay = 200;
    private int maxSpawnDelay = 800;
    private int spawnCount = 4;
    private Entity cachedEntity;
    private int maxNearbyEntities = 6;
    private int activatingRangeFromPlayer = 16;
    private int spawnRange = 4;

    public MobSpawnerBaseLogic() {
        this(null);
    }

    public MobSpawnerBaseLogic(SpawnerBlockEntity host) {
        this.host = host;
    }

    // ------------------------------------------------------------------ what it spawns

    public void setEntityName(String name) {
        this.entityTypeName = name == null ? "Pig" : name;
        EntityType<?> type = typeFor(entityTypeName);
        if (type != null && host != null) host.setEntityId(type, RandomSource.create());
    }

    public String getEntityNameToSpawn() {
        if (getRandomEntity() == null) {
            if ("Minecart".equals(entityTypeName)) entityTypeName = "MinecartRideable";
            return entityTypeName;
        }
        return getRandomEntity().entityTypeName;
    }

    private static EntityType<?> typeFor(String name) {
        String vanilla = VANILLA.get(name);
        if (vanilla != null) return BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.withDefaultNamespace(vanilla));
        Class<?> cls = EntityList.stringToClassMapping.get(name);
        if (cls != null) {
            var info = LegacyHosts.info(cls);
            if (info != null) return info.type();
        }
        return null;
    }

    public WeightedRandomMinecart getRandomEntity() {
        return randomEntity;
    }

    public void setRandomEntity(WeightedRandomMinecart entity) {
        this.randomEntity = entity;
    }

    // ------------------------------------------------------------------ where it is (mods' subclasses override these)

    public World getSpawnerWorld() {
        if (host == null || host.getLevel() == null) return null;
        return World.mcc$wrap(host.getLevel());
    }

    public int getSpawnerX() {
        return host == null ? 0 : host.getBlockPos().getX();
    }

    public int getSpawnerY() {
        return host == null ? 0 : host.getBlockPos().getY();
    }

    public int getSpawnerZ() {
        return host == null ? 0 : host.getBlockPos().getZ();
    }

    /** Signals an event to the spawner's clients (1: the spawn delay was reset); a vanilla spawner sends its block event. */
    public void func_98267_a(int id) {
        if (host != null && host.getLevel() != null) {
            host.getLevel().blockEvent(host.getBlockPos(), host.getBlockState().getBlock(), id, 0);
        }
    }

    // ------------------------------------------------------------------ spawning

    /** Whether a player is within the activation range (16 blocks by default). */
    public boolean isActivated() {
        if (host != null) {
            var level = host.getLevel();
            BlockPos p = host.getBlockPos();
            return level != null && level.hasNearbyAlivePlayer(p.getX() + 0.5D, p.getY() + 0.5D, p.getZ() + 0.5D, activatingRangeFromPlayer);
        }
        World world = getSpawnerWorld();
        return world != null && world.getClosestPlayer(getSpawnerX() + 0.5D, getSpawnerY() + 0.5D, getSpawnerZ() + 0.5D, activatingRangeFromPlayer) != null;
    }

    /** One spawner tick: counts down, then spawns up to {@code spawnCount} mobs around the spawner (a vanilla spawner ticks its modern logic). */
    public void updateSpawner() {
        if (host != null) {
            var level = host.getLevel();
            if (level instanceof net.minecraft.server.level.ServerLevel server) host.getSpawner().serverTick(server, host.getBlockPos());
            else if (level != null) host.getSpawner().clientTick(level, host.getBlockPos());
            return;
        }
        if (!isActivated()) return;
        World world = getSpawnerWorld();
        int x = getSpawnerX(), y = getSpawnerY(), z = getSpawnerZ();
        if (world.isRemote) {
            world.spawnParticle("smoke", x + world.rand.nextFloat(), y + world.rand.nextFloat(), z + world.rand.nextFloat(), 0.0D, 0.0D, 0.0D);
            world.spawnParticle("flame", x + world.rand.nextFloat(), y + world.rand.nextFloat(), z + world.rand.nextFloat(), 0.0D, 0.0D, 0.0D);
            if (spawnDelay > 0) --spawnDelay;
            field_98284_d = field_98287_c;
            field_98287_c = (field_98287_c + 1000.0F / (spawnDelay + 200.0F)) % 360.0D;
            return;
        }
        if (spawnDelay == -1) resetTimer();
        if (spawnDelay > 0) {
            --spawnDelay;
            return;
        }
        boolean spawned = false;
        for (int i = 0; i < spawnCount; ++i) {
            Entity entity = EntityList.createEntityByName(getEntityNameToSpawn(), world);
            if (entity == null) return;
            int nearby = world.getEntitiesWithinAABB(entity.getClass(),
                    AxisAlignedBB.getBoundingBox(x, y, z, x + 1, y + 1, z + 1).expand(spawnRange * 2, 4.0D, spawnRange * 2)).size();
            if (nearby >= maxNearbyEntities) {
                resetTimer();
                return;
            }
            double ex = x + (world.rand.nextDouble() - world.rand.nextDouble()) * spawnRange;
            double ey = y + world.rand.nextInt(3) - 1;
            double ez = z + (world.rand.nextDouble() - world.rand.nextDouble()) * spawnRange;
            EntityLiving living = entity instanceof EntityLiving l ? l : null;
            entity.setLocationAndAngles(ex, ey, ez, world.rand.nextFloat() * 360.0F, 0.0F);
            if (living == null || living.getCanSpawnHere()) {
                spawnEntity(entity);
                world.playAuxSFX(2004, x, y, z, 0);
                if (living != null) living.spawnExplosionParticle();
                spawned = true;
            }
        }
        if (spawned) resetTimer();
    }

    /** {@code func_98265_a}: spawns the entity with the spawn data's NBT (or as if from an egg) and returns it. */
    public Entity spawnEntity(Entity entity) {
        if (entity == null) return null;
        World world = Entity.mcc$get$worldObj(entity) != null ? Entity.mcc$get$worldObj(entity) : getSpawnerWorld();
        if (getRandomEntity() != null) {
            NBTTagCompound tag = new NBTTagCompound();
            entity.writeToNBTOptional(tag);
            NBTTagCompound properties = getRandomEntity().field_98222_b;
            for (String key : properties.getKeySet()) {
                NBTBase value = properties.getTag(key);
                if (value != null) tag.setTag(key, value.copy());
            }
            entity.readFromNBT(tag);
            if (world != null) world.spawnEntityInWorld(entity);
        } else if (entity instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase && world != null) {
            if (entity instanceof EntityLiving living) living.onSpawnWithEgg(null);
            world.spawnEntityInWorld(entity);
        }
        return entity;
    }

    private void resetTimer() {
        World world = getSpawnerWorld();
        java.util.Random rand = world != null ? world.rand : new java.util.Random();
        spawnDelay = maxSpawnDelay <= minSpawnDelay ? minSpawnDelay : minSpawnDelay + rand.nextInt(maxSpawnDelay - minSpawnDelay);
        if (potentialEntitySpawns != null && !potentialEntitySpawns.isEmpty()) {
            setRandomEntity((WeightedRandomMinecart) legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom.getRandomItem(rand, potentialEntitySpawns));
        }
        func_98267_a(1);
    }

    /** Client: resets the delay when the server signals a spawn (event 1). */
    public boolean setDelayToMin(int id) {
        World world = getSpawnerWorld();
        if (id == 1 && world != null && world.isRemote) {
            spawnDelay = minSpawnDelay;
            return true;
        }
        return false;
    }

    /** The mob drawn inside the cage. */
    public Entity getEntityToRender() {
        if (cachedEntity == null) {
            Entity entity = EntityList.createEntityByName(getEntityNameToSpawn(), null);
            if (entity != null) {
                spawnEntity(entity);
                cachedEntity = entity;
            }
        }
        return cachedEntity;
    }

    // ------------------------------------------------------------------ saving

    public void readFromNBT(NBTTagCompound tag) {
        entityTypeName = tag.getString("EntityId");
        spawnDelay = tag.getShort("Delay");
        if (tag.hasKey("SpawnPotentials", 9)) {
            potentialEntitySpawns = new ArrayList();
            NBTTagList list = tag.getTagList("SpawnPotentials", 10);
            for (int i = 0; i < list.tagCount(); ++i) potentialEntitySpawns.add(new WeightedRandomMinecart(list.getCompoundTagAt(i)));
        } else {
            potentialEntitySpawns = null;
        }
        if (tag.hasKey("SpawnData", 10)) setRandomEntity(new WeightedRandomMinecart(tag.getCompoundTag("SpawnData"), entityTypeName));
        else setRandomEntity(null);
        if (tag.hasKey("MinSpawnDelay", 99)) {
            minSpawnDelay = tag.getShort("MinSpawnDelay");
            maxSpawnDelay = tag.getShort("MaxSpawnDelay");
            spawnCount = tag.getShort("SpawnCount");
        }
        if (tag.hasKey("MaxNearbyEntities", 99)) {
            maxNearbyEntities = tag.getShort("MaxNearbyEntities");
            activatingRangeFromPlayer = tag.getShort("RequiredPlayerRange");
        }
        if (tag.hasKey("SpawnRange", 99)) spawnRange = tag.getShort("SpawnRange");
        cachedEntity = null;
        if (host != null) {
            // the settings have the same names in the modern format; the mob is set by name
            var modern = tag.mcc$modern().copy();
            modern.remove("EntityId");
            modern.remove("SpawnData");
            modern.remove("SpawnPotentials");
            host.getSpawner().load(host.getLevel(), host.getBlockPos(), modern);
            setEntityName(entityTypeName.isEmpty() ? "Pig" : entityTypeName);
            host.setChanged();
        }
    }

    public void writeToNBT(NBTTagCompound tag) {
        tag.setString("EntityId", getEntityNameToSpawn());
        int delay = spawnDelay;
        if (host != null) {
            var modern = host.getSpawner().save(new net.minecraft.nbt.CompoundTag());
            delay = modern.getShort("Delay");
            minSpawnDelay = modern.getShort("MinSpawnDelay");
            maxSpawnDelay = modern.getShort("MaxSpawnDelay");
            spawnCount = modern.getShort("SpawnCount");
            maxNearbyEntities = modern.getShort("MaxNearbyEntities");
            activatingRangeFromPlayer = modern.getShort("RequiredPlayerRange");
            spawnRange = modern.getShort("SpawnRange");
        }
        tag.setShort("Delay", (short) delay);
        tag.setShort("MinSpawnDelay", (short) minSpawnDelay);
        tag.setShort("MaxSpawnDelay", (short) maxSpawnDelay);
        tag.setShort("SpawnCount", (short) spawnCount);
        tag.setShort("MaxNearbyEntities", (short) maxNearbyEntities);
        tag.setShort("RequiredPlayerRange", (short) activatingRangeFromPlayer);
        tag.setShort("SpawnRange", (short) spawnRange);
        if (getRandomEntity() != null) tag.setTag("SpawnData", getRandomEntity().field_98222_b.copy());
        if (getRandomEntity() != null || potentialEntitySpawns != null && !potentialEntitySpawns.isEmpty()) {
            NBTTagList list = new NBTTagList();
            if (potentialEntitySpawns != null && !potentialEntitySpawns.isEmpty()) {
                for (Object o : potentialEntitySpawns) list.appendTag(((WeightedRandomMinecart) o).func_98220_a());
            } else {
                list.appendTag(getRandomEntity().func_98220_a());
            }
            tag.setTag("SpawnPotentials", list);
        }
    }

    /** One weighted entry of a spawner's spawn list: an entity name and the NBT ("Properties") it spawns with. */
    public class WeightedRandomMinecart extends legacyconnector.v1_7_10.net.minecraft.util.WeightedRandom.Item {
        public final NBTTagCompound field_98222_b;
        public final String entityTypeName;

        public WeightedRandomMinecart(NBTTagCompound tag) {
            super(tag.getInteger("Weight"));
            NBTTagCompound properties = tag.getCompoundTag("Properties");
            String type = tag.getString("Type");
            if ("Minecart".equals(type)) type = mcc$minecartName(properties);
            this.field_98222_b = properties;
            this.entityTypeName = type;
        }

        public WeightedRandomMinecart(NBTTagCompound properties, String type) {
            super(1);
            if ("Minecart".equals(type) && properties != null) type = mcc$minecartName(properties);
            this.field_98222_b = properties;
            this.entityTypeName = type;
        }

        private static String mcc$minecartName(NBTTagCompound properties) {
            if (properties == null) return "MinecartRideable";
            return switch (properties.getInteger("Type")) {
                case 1 -> "MinecartChest";
                case 2 -> "MinecartFurnace";
                default -> "MinecartRideable";
            };
        }

        public NBTTagCompound func_98220_a() {
            NBTTagCompound tag = new NBTTagCompound();
            tag.setTag("Properties", field_98222_b);
            tag.setString("Type", entityTypeName);
            tag.setInteger("Weight", itemWeight);
            return tag;
        }
    }
}
