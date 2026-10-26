package legacyconnector.v1_7_10.net.minecraft.village;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.util.ChunkCoordinates;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * A 1.7.10 village: its doors, centre and radius, the players' reputation, the mobs that attacked it and its mating season.
 * {@link VillageCollection} builds them from the modern villages (meeting points with their homes as doors); the time
 * stamps run on the world's clock.
 */
public class Village {
    private World worldObj;
    private final List<VillageDoorInfo> villageDoorInfoList = new ArrayList<>();
    private final ChunkCoordinates centerHelper = new ChunkCoordinates(0, 0, 0);
    private final ChunkCoordinates center = new ChunkCoordinates(0, 0, 0);
    private int villageRadius;
    private int lastAddDoorTimestamp;
    private int tickCounter;
    private int numVillagers;
    private int noBreedTicks;
    private final TreeMap<String, Integer> playerReputation = new TreeMap<>();
    private final List<VillageAgressor> villageAgressors = new ArrayList<>();
    private int numIronGolems;

    public Village() {
    }

    public Village(World world) {
        this.worldObj = world;
    }

    /** A village around a fixed centre (the modern meeting point), before any door is known. */
    public Village(int x, int y, int z, int radius) {
        center.set(x, y, z);
        this.villageRadius = radius;
    }

    public void func_82691_a(World world) {
        this.worldObj = world;
    }

    /** The village clock: the world's time when the village has a world (the modern level ticks it), else its own counter. */
    private int now() {
        if (worldObj != null) tickCounter = (int) worldObj.getTotalWorldTime();
        return tickCounter;
    }

    /** One village tick: forgets aggressors after 300 ticks and doors whose block is gone. */
    public void tick(int tick) {
        this.tickCounter = tick;
        removeDeadAndOldAgressors();
    }

    private void removeDeadAndOldAgressors() {
        int time = now();
        villageAgressors.removeIf(a -> a.agressor == null || !a.agressor.isEntityAlive() || Math.abs(time - a.agressionTime) > 300);
    }

    public ChunkCoordinates getCenter() {
        return center;
    }

    public int getVillageRadius() {
        return villageRadius;
    }

    public int getNumVillageDoors() {
        return villageDoorInfoList.size();
    }

    public int getTicksSinceLastDoorAdding() {
        return now() - lastAddDoorTimestamp;
    }

    /** The villagers within the village (counted from the modern level when it has one). */
    public int getNumVillagers() {
        if (worldObj != null && worldObj.mcc$fullLevel() instanceof net.minecraft.server.level.ServerLevel level && level.getServer().isSameThread()) {
            var box = new net.minecraft.world.phys.AABB(center.posX - villageRadius, center.posY - 4, center.posZ - villageRadius,
                    center.posX + villageRadius, center.posY + 4, center.posZ + villageRadius);
            numVillagers = level.getEntitiesOfClass(net.minecraft.world.entity.npc.AbstractVillager.class, box).size();
        }
        return numVillagers;
    }

    public boolean isInRange(int x, int y, int z) {
        return center.getDistanceSquared(x, y, z) < (float) (villageRadius * villageRadius);
    }

    public List<VillageDoorInfo> getVillageDoorInfoList() {
        return villageDoorInfoList;
    }

    public VillageDoorInfo findNearestDoor(int x, int y, int z) {
        VillageDoorInfo best = null;
        int bestDistance = Integer.MAX_VALUE;
        for (VillageDoorInfo door : villageDoorInfoList) {
            int d = door.getDistanceSquared(x, y, z);
            if (d < bestDistance) {
                best = door;
                bestDistance = d;
            }
        }
        return best;
    }

    /** The door to go to: the least crowded one within 16 blocks, else the nearest. */
    public VillageDoorInfo findNearestDoorUnrestricted(int x, int y, int z) {
        VillageDoorInfo best = null;
        int bestScore = Integer.MAX_VALUE;
        for (VillageDoorInfo door : villageDoorInfoList) {
            int d = door.getDistanceSquared(x, y, z);
            int score = d > 256 ? d * 1000 : door.getDoorOpeningRestrictionCounter();
            if (score < bestScore) {
                best = door;
                bestScore = score;
            }
        }
        return best;
    }

    public VillageDoorInfo getVillageDoorAt(int x, int y, int z) {
        if (center.getDistanceSquared(x, y, z) > (float) (villageRadius * villageRadius)) return null;
        for (VillageDoorInfo door : villageDoorInfoList) {
            if (door.posX == x && door.posZ == z && Math.abs(door.posY - y) <= 1) return door;
        }
        return null;
    }

    public void addVillageDoorInfo(VillageDoorInfo door) {
        villageDoorInfoList.add(door);
        centerHelper.posX += door.posX;
        centerHelper.posY += door.posY;
        centerHelper.posZ += door.posZ;
        updateVillageRadiusAndCenter();
        lastAddDoorTimestamp = door.lastActivityTimestamp;
    }

    /** The centre is the average of the doors, the radius reaches the farthest door (at least 32). */
    private void updateVillageRadiusAndCenter() {
        int n = villageDoorInfoList.size();
        if (n == 0) {
            villageRadius = 0;
            return;
        }
        center.set(centerHelper.posX / n, centerHelper.posY / n, centerHelper.posZ / n);
        int farthest = 0;
        for (VillageDoorInfo door : villageDoorInfoList) farthest = Math.max(door.getDistanceSquared(center.posX, center.posY, center.posZ), farthest);
        villageRadius = Math.max(32, (int) Math.sqrt(farthest) + 1);
    }

    public boolean isAnnihilated() {
        return villageDoorInfoList.isEmpty();
    }

    /** Remembers (or refreshes) a mob that attacked the village; golems and guards go after it. */
    public void addOrRenewAgressor(EntityLivingBase entity) {
        if (entity == null) return;
        int time = now();
        for (VillageAgressor a : villageAgressors) {
            if (a.agressor == entity) {
                a.agressionTime = time;
                return;
            }
        }
        villageAgressors.add(new VillageAgressor(entity, time));
    }

    /** The aggressor nearest to the entity, or null. */
    public EntityLivingBase findNearestVillageAggressor(EntityLivingBase entity) {
        removeDeadAndOldAgressors();
        double best = Double.MAX_VALUE;
        VillageAgressor nearest = null;
        for (VillageAgressor a : villageAgressors) {
            double d = a.agressor.getDistanceSqToEntity(entity);
            if (d <= best) {
                nearest = a;
                best = d;
            }
        }
        return nearest == null ? null : nearest.agressor;
    }

    /** The nearest online player whose reputation here is too low (an enemy of the village), or null. */
    public EntityPlayer func_82685_c(EntityLivingBase entity) {
        if (worldObj == null) return null;
        double best = Double.MAX_VALUE;
        EntityPlayer nearest = null;
        for (String name : playerReputation.keySet()) {
            if (!isPlayerReputationTooLow(name)) continue;
            EntityPlayer player = worldObj.getPlayerEntityByName(name);
            if (player == null) continue;
            double d = player.getDistanceSqToEntity(entity);
            if (d <= best) {
                nearest = player;
                best = d;
            }
        }
        return nearest;
    }

    public int getReputationForPlayer(String name) {
        Integer rep = playerReputation.get(name);
        return rep == null ? 0 : rep;
    }

    /** Changes a player's reputation by {@code delta} (kept within -30..10) and returns the new value. */
    public int setReputationForPlayer(String name, int delta) {
        int rep = Math.max(-30, Math.min(10, getReputationForPlayer(name) + delta));
        playerReputation.put(name, rep);
        return rep;
    }

    public boolean isPlayerReputationTooLow(String name) {
        return getReputationForPlayer(name) <= -15;
    }

    /** Villagers stop breeding for a while (3600 ticks). */
    public void endMatingSeason() {
        noBreedTicks = now();
    }

    public boolean isMatingSeason() {
        return noBreedTicks == 0 || now() - noBreedTicks >= 3600;
    }

    /** Changes every known player's reputation by the amount (a golem was killed, the village was helped). */
    public void setDefaultPlayerReputation(int delta) {
        for (String name : new ArrayList<>(playerReputation.keySet())) setReputationForPlayer(name, delta);
    }

    public void readVillageDataFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        numVillagers = tag.getInteger("PopSize");
        villageRadius = tag.getInteger("Radius");
        numIronGolems = tag.getInteger("Golems");
        lastAddDoorTimestamp = tag.getInteger("Stable");
        tickCounter = tag.getInteger("Tick");
        noBreedTicks = tag.getInteger("MTick");
        center.set(tag.getInteger("CX"), tag.getInteger("CY"), tag.getInteger("CZ"));
        centerHelper.set(tag.getInteger("ACX"), tag.getInteger("ACY"), tag.getInteger("ACZ"));
        var doors = tag.getTagList("Doors", 10);
        for (int i = 0; i < doors.tagCount(); i++) {
            var d = doors.getCompoundTagAt(i);
            villageDoorInfoList.add(new VillageDoorInfo(d.getInteger("X"), d.getInteger("Y"), d.getInteger("Z"), d.getInteger("IDX"), d.getInteger("IDZ"),
                    d.getInteger("TS")));
        }
        var players = tag.getTagList("Players", 10);
        for (int i = 0; i < players.tagCount(); i++) {
            var p = players.getCompoundTagAt(i);
            playerReputation.put(p.getString("Name"), p.getInteger("S"));
        }
    }

    public void writeVillageDataToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        tag.setInteger("PopSize", numVillagers);
        tag.setInteger("Radius", villageRadius);
        tag.setInteger("Golems", numIronGolems);
        tag.setInteger("Stable", lastAddDoorTimestamp);
        tag.setInteger("Tick", tickCounter);
        tag.setInteger("MTick", noBreedTicks);
        tag.setInteger("CX", center.posX);
        tag.setInteger("CY", center.posY);
        tag.setInteger("CZ", center.posZ);
        tag.setInteger("ACX", centerHelper.posX);
        tag.setInteger("ACY", centerHelper.posY);
        tag.setInteger("ACZ", centerHelper.posZ);
        var doors = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (VillageDoorInfo door : villageDoorInfoList) {
            var d = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
            d.setInteger("X", door.posX);
            d.setInteger("Y", door.posY);
            d.setInteger("Z", door.posZ);
            d.setInteger("IDX", door.insideDirectionX);
            d.setInteger("IDZ", door.insideDirectionZ);
            d.setInteger("TS", door.lastActivityTimestamp);
            doors.appendTag(d);
        }
        tag.setTag("Doors", doors);
        var players = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (var e : playerReputation.entrySet()) {
            var p = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
            p.setString("Name", e.getKey());
            p.setInteger("S", e.getValue());
            players.appendTag(p);
        }
        tag.setTag("Players", players);
    }

    /** A mob that attacked the village and when. */
    class VillageAgressor {
        public EntityLivingBase agressor;
        public int agressionTime;

        VillageAgressor(EntityLivingBase agressor, int time) {
            this.agressor = agressor;
            this.agressionTime = time;
        }
    }
}
