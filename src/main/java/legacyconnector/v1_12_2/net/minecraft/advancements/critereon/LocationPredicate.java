package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * A location condition: position ranges, biome, dimension ("overworld", "the_nether", "the_end") and structure ("feature": a structure
 * of that 1.12 name contains the position, checked through the game's structures).
 */
public class LocationPredicate {
    public static final LocationPredicate ANY = new LocationPredicate(MinMaxBounds.UNBOUNDED, MinMaxBounds.UNBOUNDED, MinMaxBounds.UNBOUNDED, null, null, null);
    private final MinMaxBounds x;
    private final MinMaxBounds y;
    private final MinMaxBounds z;
    private final ResourceLocation biome;
    private final String feature;
    private final String dimension;

    public LocationPredicate(MinMaxBounds x, MinMaxBounds y, MinMaxBounds z, ResourceLocation biome, String feature, String dimension) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.biome = biome;
        this.feature = feature;
        this.dimension = dimension;
    }

    public boolean test(legacyconnector.v1_12_2.net.minecraft.world.WorldServer world, double px, double py, double pz) {
        return test(world, (float) px, (float) py, (float) pz);
    }

    public boolean test(legacyconnector.v1_12_2.net.minecraft.world.WorldServer world, float px, float py, float pz) {
        if (!x.test(px) || !y.test(py) || !z.test(pz)) return false;
        var level = world == null ? null : world.mcc$level();
        if (level == null) return this == ANY;
        if (dimension != null) {
            var key = level.dimension();
            String name = key == net.minecraft.world.level.Level.OVERWORLD ? "overworld" : key == net.minecraft.world.level.Level.NETHER ? "the_nether"
                    : key == net.minecraft.world.level.Level.END ? "the_end" : key.location().getPath();
            if (!dimension.equals(name)) return false;
        }
        var pos = net.minecraft.core.BlockPos.containing(px, py, pz);
        if (biome != null) {
            var key = level.getBiome(pos).unwrapKey().orElse(null);
            if (key == null || !key.location().toString().equals(biome.toString())) return false;
        }
        if (feature != null && level instanceof net.minecraft.server.level.ServerLevel server) {
            var tag = org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.structureTag(feature);
            if (tag == null || !server.structureManager().getStructureWithPieceAt(pos, tag).isValid()) return false;
        }
        return true;
    }

    public static LocationPredicate deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return ANY;
        JsonObject o = json.getAsJsonObject();
        JsonObject position = o.has("position") ? o.getAsJsonObject("position") : new JsonObject();
        MinMaxBounds x = MinMaxBounds.deserialize(position.get("x"));
        MinMaxBounds y = MinMaxBounds.deserialize(position.get("y"));
        MinMaxBounds z = MinMaxBounds.deserialize(position.get("z"));
        String dimension = o.has("dimension") ? o.get("dimension").getAsString() : null;
        String feature = o.has("feature") ? o.get("feature").getAsString() : null;
        ResourceLocation biome = o.has("biome") ? new ResourceLocation(o.get("biome").getAsString()) : null;
        return new LocationPredicate(x, y, z, biome, feature, dimension);
    }
}
