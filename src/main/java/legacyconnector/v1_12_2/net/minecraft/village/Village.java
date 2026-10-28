package legacyconnector.v1_12_2.net.minecraft.village;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 village, mapped onto the modern village structure of points of interest: its center is a meeting point (village bell), its
 * doors are the beds (homes) around it, its villagers and iron golems the ones within its radius. Player reputation is kept here.
 */
public class Village {
    @Deprecated
    public World world;
    private World mcc$world;
    private BlockPos center = BlockPos.ORIGIN;
    private int villageRadius = 32;
    @Deprecated
    public int numIronGolems;
    private final Map<String, Integer> playerReputation = new HashMap<>();
    private int tickCounter;

    public Village() {
    }

    public Village(World world) {
        this.mcc$world = world;
    }

    public static Village mcc$at(World world, BlockPos center) {
        Village v = new Village(world);
        v.center = center;
        return v;
    }

    public static World mcc$get$world(Village v) {
        return v.mcc$world;
    }

    public static void mcc$set$world(Village v, World w) {
        v.mcc$world = w;
    }

    public static int mcc$get$numIronGolems(Village v) {
        return v.mcc$count(net.minecraft.world.entity.animal.IronGolem.class);
    }

    public static void mcc$set$numIronGolems(Village v, int n) {
    }

    public void setWorld(World world) {
        this.mcc$world = world;
    }

    private net.minecraft.server.level.ServerLevel mcc$level() {
        return mcc$world != null && mcc$world.mcc$level() instanceof net.minecraft.server.level.ServerLevel s ? s : null;
    }

    private int mcc$count(Class<? extends net.minecraft.world.entity.Entity> type) {
        var level = mcc$level();
        if (level == null) return 0;
        var c = center.mcc$modern();
        var box = new net.minecraft.world.phys.AABB(c).inflate(villageRadius, 4.0D, villageRadius);
        return level.getEntitiesOfClass(type, box).size();
    }

    private List<net.minecraft.core.BlockPos> mcc$homes() {
        var level = mcc$level();
        if (level == null) return List.of();
        return level.getPoiManager().findAll(h -> h.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME), p -> true, center.mcc$modern(),
                villageRadius, net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).toList();
    }

    public void tick(int tickCounter) {
        this.tickCounter = tickCounter;
    }

    public BlockPos getCenter() {
        return center;
    }

    public int getVillageRadius() {
        return villageRadius;
    }

    public int getNumVillageDoors() {
        return mcc$homes().size();
    }

    public int getTicksSinceLastDoorAdding() {
        return 0;
    }

    public int getNumVillagers() {
        return mcc$count(net.minecraft.world.entity.npc.Villager.class);
    }

    public boolean isBlockPosWithinSqVillageRadius(BlockPos pos) {
        return center.distanceSq(pos) < (double) (villageRadius * villageRadius);
    }

    public List<VillageDoorInfo> getVillageDoorInfoList() {
        List<VillageDoorInfo> out = new ArrayList<>();
        for (var p : mcc$homes()) out.add(new VillageDoorInfo(BlockPos.mcc$of(p), legacyconnector.v1_12_2.net.minecraft.util.EnumFacing.NORTH, tickCounter));
        return out;
    }

    public VillageDoorInfo getNearestDoor(BlockPos pos) {
        VillageDoorInfo best = null;
        int bestDist = Integer.MAX_VALUE;
        for (VillageDoorInfo d : getVillageDoorInfoList()) {
            int dist = d.getDistanceToDoorBlockSq(pos);
            if (dist < bestDist) {
                best = d;
                bestDist = dist;
            }
        }
        return best;
    }

    public VillageDoorInfo getDoorInfo(BlockPos pos) {
        return getNearestDoor(pos);
    }

    public VillageDoorInfo getExistedDoor(BlockPos pos) {
        if (center.distanceSq(pos) > (double) (villageRadius * villageRadius)) return null;
        for (VillageDoorInfo d : getVillageDoorInfoList()) {
            if (d.getDoorBlockPos().getX() == pos.getX() && d.getDoorBlockPos().getZ() == pos.getZ() && Math.abs(d.getDoorBlockPos().getY() - pos.getY()) <= 1) return d;
        }
        return null;
    }

    /** A random spot in the village where an entity of that size fits (1.12: ten tries). */
    public BlockPos findRandomSpawnPos(BlockPos pos, int x, int y, int z) {
        var level = mcc$level();
        java.util.Random rand = new java.util.Random();
        for (int i = 0; i < 10; ++i) {
            BlockPos p = pos.add(rand.nextInt(16) - 8, rand.nextInt(6) - 3, rand.nextInt(16) - 8);
            if (isBlockPosWithinSqVillageRadius(p) && level != null && level.noCollision(new net.minecraft.world.phys.AABB(p.getX(), p.getY(), p.getZ(),
                    p.getX() + x, p.getY() + y, p.getZ() + z)) && level.getBlockState(p.mcc$modern().below()).isSolid()) {
                return p;
            }
        }
        return null;
    }

    public void addOrRenewAgressor(legacyconnector.v1_12_2.net.minecraft.entity.EntityLivingBase entity) {
        var m = entity == null ? null : entity.mcc$entity;
        var level = mcc$level();
        if (m instanceof net.minecraft.world.entity.LivingEntity l && level != null) {
            var c = center.mcc$modern();
            for (var golem : level.getEntitiesOfClass(net.minecraft.world.entity.animal.IronGolem.class, new net.minecraft.world.phys.AABB(c).inflate(villageRadius))) {
                if (golem.getTarget() == null) golem.setTarget(l);
            }
        }
    }

    public int getPlayerReputation(String name) {
        Integer r = playerReputation.get(name);
        return r == null ? 0 : r;
    }

    public int getPlayerReputation(java.util.UUID id) {
        return getPlayerReputation(id == null ? "" : id.toString());
    }

    public int modifyPlayerReputation(String name, int amount) {
        int r = net.minecraft.util.Mth.clamp(getPlayerReputation(name) + amount, -30, 10);
        playerReputation.put(name, r);
        return r;
    }

    public int modifyPlayerReputation(java.util.UUID id, int amount) {
        return modifyPlayerReputation(id == null ? "" : id.toString(), amount);
    }

    public boolean isPlayerReputationTooLow(String name) {
        return getPlayerReputation(name) <= -15;
    }

    public void setDefaultPlayerReputation(int reputation) {
        for (String name : playerReputation.keySet()) modifyPlayerReputation(name, reputation);
    }

    public boolean isMatingSeason() {
        return false;
    }

    public void endMatingSeason() {
    }

    public boolean isAnnihilated() {
        return getNumVillageDoors() == 0;
    }

    public <T> T getCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<T> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return null;
    }

    public boolean hasCapability(legacyconnector.v1_12_2.net.minecraftforge.common.capabilities.Capability<?> capability,
            legacyconnector.v1_12_2.net.minecraft.util.EnumFacing facing) {
        return false;
    }
}
