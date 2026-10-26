package legacyconnector.v1_7_10.net.minecraft.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * 1.7.10's per-world village registry. Modern villages are points of interest: each meeting point (a bell) is a village centre and
 * the homes (beds) around it are its doors. Villages are found on demand on the server thread and kept per meeting point, so
 * reputations and aggressors last while the world runs; elsewhere (clients, generation threads) no village is found.
 */
public class VillageCollection extends legacyconnector.v1_7_10.net.minecraft.world.WorldSavedData {
    /** How far around a meeting point its homes count as the village's doors (1.7.10 villages reached 32 blocks). */
    private static final int MCC_REACH = 32;

    private World worldObj;
    private final List<Village> villageList = new ArrayList<>();
    private final Map<Long, Village> mcc$byMeetingPoint = new HashMap<>();
    private int tickCounter;

    public VillageCollection(String name) {
        super(name);
    }

    public VillageCollection(World world) {
        super("villages");
        this.worldObj = world;
        markDirty();
    }

    public VillageCollection() {
        this("villages");
    }

    public void func_82566_a(World world) {
        this.worldObj = world;
        for (Village v : villageList) v.func_82691_a(world);
    }

    public List<Village> getVillageList() {
        mcc$refresh(null, 0);
        return villageList;
    }

    /** The nearest village whose centre is within its radius plus {@code radius} of the point, or null. */
    public Village findNearestVillage(int x, int y, int z, int radius) {
        mcc$refresh(new net.minecraft.core.BlockPos(x, y, z), radius + MCC_REACH * 2);
        Village best = null;
        float bestDistance = Float.MAX_VALUE;
        for (Village v : villageList) {
            float d = v.getCenter().getDistanceSquared(x, y, z);
            if (d < bestDistance) {
                float reach = radius + v.getVillageRadius();
                if (d <= reach * reach) {
                    best = v;
                    bestDistance = d;
                }
            }
        }
        return best;
    }

    /** Builds (or keeps) a village for every modern meeting point near the position (all loaded ones when {@code near} is null). */
    private void mcc$refresh(net.minecraft.core.BlockPos near, int range) {
        if (worldObj == null || !(worldObj.mcc$liveLevel() instanceof net.minecraft.server.level.ServerLevel level) || !level.getServer().isSameThread()) return;
        try {
            var poi = level.getPoiManager();
            var occupancy = net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY;
            java.util.stream.Stream<net.minecraft.world.entity.ai.village.poi.PoiRecord> meetings;
            if (near != null) {
                meetings = poi.getInRange(t -> t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING), near, Math.min(range, 128), occupancy);
            } else {
                List<net.minecraft.world.entity.ai.village.poi.PoiRecord> all = new ArrayList<>();
                for (var player : level.players()) {
                    poi.getInRange(t -> t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING), player.blockPosition(), 128, occupancy).forEach(all::add);
                }
                meetings = all.stream();
            }
            meetings.forEach(record -> {
                var at = record.getPos();
                if (mcc$byMeetingPoint.containsKey(at.asLong())) return;
                Village village = new Village(worldObj);
                int time = (int) worldObj.getTotalWorldTime();
                poi.getInRange(t -> t.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME), at, MCC_REACH, occupancy)
                        .forEach(home -> village.addVillageDoorInfo(new VillageDoorInfo(home.getPos().getX(), home.getPos().getY(), home.getPos().getZ(), 0, 0, time)));
                if (village.isAnnihilated()) village.addVillageDoorInfo(new VillageDoorInfo(at.getX(), at.getY(), at.getZ(), 0, 0, time));
                mcc$byMeetingPoint.put(at.asLong(), village);
                villageList.add(village);
            });
        } catch (RuntimeException e) {
            // the point-of-interest store is not available here: the villages already known stay
        }
    }

    /** Villager positions came from 1.7.10's village search; the modern villages need none. */
    public void addVillagerPosition(int x, int y, int z) {
    }

    /** Advances the villages' clocks and forgets aggressors that left. */
    public void tick() {
        tickCounter++;
        for (Village v : villageList) v.tick(tickCounter);
    }

    @Override
    public void readFromNBT(NBTTagCompound tag) {
        tickCounter = tag.getInteger("Tick");
        var list = tag.getTagList("Villages", 10);
        for (int i = 0; i < list.tagCount(); i++) {
            Village v = new Village();
            v.readVillageDataFromNBT(list.getCompoundTagAt(i));
            villageList.add(v);
        }
    }

    @Override
    public void writeToNBT(NBTTagCompound tag) {
        tag.setInteger("Tick", tickCounter);
        var list = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
        for (Village v : villageList) {
            NBTTagCompound t = new NBTTagCompound();
            v.writeVillageDataToNBT(t);
            list.appendTag(t);
        }
        tag.setTag("Villages", list);
    }
}
