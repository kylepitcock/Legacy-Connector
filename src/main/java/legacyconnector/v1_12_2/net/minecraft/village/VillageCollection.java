package legacyconnector.v1_12_2.net.minecraft.village;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.world.World;

/**
 * 1.12.2 village list of a world: the modern villages, found through their meeting points (bells) in the point-of-interest data. Villages
 * are recreated on each query, so a village's reputation lasts as long as a mod keeps the object.
 */
public class VillageCollection {
    private World world;

    public VillageCollection(String name) {
    }

    public VillageCollection(World world) {
        this.world = world;
    }

    public void setWorldsForAll(World world) {
        this.world = world;
    }

    private net.minecraft.server.level.ServerLevel mcc$level() {
        return world != null && world.mcc$level() instanceof net.minecraft.server.level.ServerLevel s ? s : null;
    }

    /** 1.12 collected villager positions to find doors; the modern game finds its own homes. */
    public void addToVillagerPositionList(BlockPos pos) {
    }

    public void tick() {
    }

    public List<Village> getVillageList() {
        List<Village> out = new ArrayList<>();
        var level = mcc$level();
        if (level == null) return out;
        for (var player : level.players()) {
            level.getPoiManager().findAll(h -> h.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING), p -> true, player.blockPosition(), 128,
                    net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY).forEach(p -> {
                        BlockPos center = BlockPos.mcc$of(p);
                        for (Village v : out) if (v.getCenter().equals(center)) return;
                        out.add(Village.mcc$at(world, center));
                    });
        }
        return out;
    }

    /** The village whose center is nearest {@code pos} within {@code radius} of its edge, or null. */
    public Village getNearestVillage(BlockPos pos, int radius) {
        var level = mcc$level();
        if (level == null || pos == null) return null;
        var found = level.getPoiManager().findClosest(h -> h.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING), pos.mcc$modern(), radius + 32,
                net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY);
        return found.map(p -> Village.mcc$at(world, BlockPos.mcc$of(p))).orElse(null);
    }

    public void readFromNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
    }

    public legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound writeToNBT(legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound tag) {
        return tag;
    }

    public static String fileNameForProvider(legacyconnector.v1_12_2.net.minecraft.world.WorldProvider provider) {
        return "villages" + provider.getDimensionType().getSuffix();
    }
}
