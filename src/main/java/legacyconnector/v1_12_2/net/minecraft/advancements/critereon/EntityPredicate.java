package legacyconnector.v1_12_2.net.minecraft.advancements.critereon;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/**
 * An entity condition: its type, distance from the player (x/y/z, horizontal or absolute), location, potion effects (amplifier and
 * duration ranges) and NBT.
 */
public class EntityPredicate {
    public static final EntityPredicate ANY = new EntityPredicate(null, null, LocationPredicate.ANY, null, NBTPredicate.ANY);
    private final ResourceLocation type;
    private final Map<String, MinMaxBounds> distance;
    private final LocationPredicate location;
    private final Map<ResourceLocation, MinMaxBounds[]> effects;
    private final NBTPredicate nbt;

    public EntityPredicate(ResourceLocation type, Map<String, MinMaxBounds> distance, LocationPredicate location, Map<ResourceLocation, MinMaxBounds[]> effects,
            NBTPredicate nbt) {
        this.type = type;
        this.distance = distance;
        this.location = location;
        this.effects = effects;
        this.nbt = nbt;
    }

    public boolean test(legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayerMP player, legacyconnector.v1_12_2.net.minecraft.entity.Entity entity) {
        if (this == ANY) return true;
        if (entity == null) return false;
        var modern = entity.mcc$modern();
        if (modern == null) return false;
        if (type != null) {
            var key = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(modern.getType());
            if (!key.toString().equals(type.toString())) return false;
        }
        if (distance != null && player != null && player.mcc$modern() != null) {
            var p = player.mcc$modern();
            double dx = p.getX() - modern.getX(), dy = p.getY() - modern.getY(), dz = p.getZ() - modern.getZ();
            if (!check("x", (float) Math.abs(dx)) || !check("y", (float) Math.abs(dy)) || !check("z", (float) Math.abs(dz))) return false;
            MinMaxBounds h = distance.get("horizontal");
            if (h != null && !h.testSquare(dx * dx + dz * dz)) return false;
            MinMaxBounds a = distance.get("absolute");
            if (a != null && !a.testSquare(dx * dx + dy * dy + dz * dz)) return false;
        }
        if (location != LocationPredicate.ANY && !(org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122.wrap(modern.level()) instanceof legacyconnector.v1_12_2.net.minecraft.world.WorldServer ws
                && location.test(ws, modern.getX(), modern.getY(), modern.getZ()))) return false;
        if (effects != null && !effects.isEmpty()) {
            if (!(modern instanceof net.minecraft.world.entity.LivingEntity living)) return false;
            for (var e : effects.entrySet()) {
                var effect = net.minecraft.core.registries.BuiltInRegistries.MOB_EFFECT.getHolder(e.getKey().mcc$modern()).orElse(null);
                var instance = effect == null ? null : living.getEffect(effect);
                if (instance == null || !e.getValue()[0].test(instance.getAmplifier()) || !e.getValue()[1].test(instance.getDuration())) return false;
            }
        }
        return nbt.test(entity);
    }

    private boolean check(String axis, float value) {
        MinMaxBounds b = distance.get(axis);
        return b == null || b.test(value);
    }

    public static EntityPredicate deserialize(JsonElement json) {
        if (json == null || json.isJsonNull()) return ANY;
        JsonObject o = json.getAsJsonObject();
        ResourceLocation type = o.has("type") ? new ResourceLocation(o.get("type").getAsString()) : null;
        Map<String, MinMaxBounds> distance = null;
        if (o.has("distance")) {
            distance = new LinkedHashMap<>();
            JsonObject d = o.getAsJsonObject("distance");
            for (String k : new String[] {"x", "y", "z", "horizontal", "absolute"}) if (d.has(k)) distance.put(k, MinMaxBounds.deserialize(d.get(k)));
        }
        Map<ResourceLocation, MinMaxBounds[]> effects = null;
        if (o.has("effects")) {
            effects = new LinkedHashMap<>();
            for (var e : o.getAsJsonObject("effects").entrySet()) {
                JsonObject v = e.getValue().getAsJsonObject();
                effects.put(new ResourceLocation(e.getKey()), new MinMaxBounds[] {MinMaxBounds.deserialize(v.get("amplifier")), MinMaxBounds.deserialize(v.get("duration"))});
            }
        }
        return new EntityPredicate(type, distance, LocationPredicate.deserialize(o.get("location")), effects, NBTPredicate.deserialize(o.get("nbt")));
    }
}
