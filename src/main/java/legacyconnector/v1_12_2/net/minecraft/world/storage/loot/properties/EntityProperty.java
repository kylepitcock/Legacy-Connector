package legacyconnector.v1_12_2.net.minecraft.world.storage.loot.properties;

import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonSerializationContext;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** 1.12.2 property of an entity a loot condition ({@code entity_properties}) tests; types are registered with {@link EntityPropertyManager}. */
public interface EntityProperty {
    boolean testProperty(java.util.Random random, legacyconnector.v1_12_2.net.minecraft.entity.Entity entityIn);

    /** Reads and writes one property type from and to loot table JSON. */
    abstract class Serializer<T extends EntityProperty> {
        private final ResourceLocation name;
        private final Class<T> propertyClass;

        protected Serializer(ResourceLocation nameIn, Class<T> propertyClassIn) {
            this.name = nameIn;
            this.propertyClass = propertyClassIn;
        }

        public ResourceLocation getName() {
            return this.name;
        }

        public Class<T> getPropertyClass() {
            return this.propertyClass;
        }

        public abstract JsonElement serialize(T property, JsonSerializationContext serializationContext);

        public abstract T deserialize(JsonElement element, JsonDeserializationContext deserializationContext);
    }
}
