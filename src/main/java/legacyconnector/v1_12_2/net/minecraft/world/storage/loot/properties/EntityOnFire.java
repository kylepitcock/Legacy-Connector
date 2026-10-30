package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.properties;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializationContext;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 entity property "on_fire": whether the entity is burning. */
public class EntityOnFire implements EntityProperty {
    private final boolean onFire;

    public EntityOnFire(boolean onFireIn) {
        this.onFire = onFireIn;
    }

    @Override
    public boolean testProperty(java.util.Random random, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn) {
        return entityIn.isBurning() == this.onFire;
    }

    public static class Serializer extends EntityProperty.Serializer<EntityOnFire> {
        protected Serializer() {
            super(new ResourceLocation("on_fire"), EntityOnFire.class);
        }

        @Override
        public JsonElement serialize(EntityOnFire property, JsonSerializationContext serializationContext) {
            return new JsonPrimitive(property.onFire);
        }

        @Override
        public EntityOnFire deserialize(JsonElement element, JsonDeserializationContext deserializationContext) {
            return new EntityOnFire(net.minecraft.util.GsonHelper.convertToBoolean(element, "on_fire"));
        }
    }
}
