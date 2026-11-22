package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;
import org.junit.jupiter.api.Test;

class PalladiumRendererRegistryDebugCompatTest {
    @Test
    void resolvesEntityTypeRegistrySupplierToItsResourceKey() {
        Supplier<EntityType<?>> supplier = () -> EntityType.PIG;

        assertEquals("minecraft:pig", PalladiumRendererRegistryDebugCompat.resolveSupplierType(supplier));
    }

    @Test
    void reportsNonEntitySupplierValuesWithoutFailing() {
        Supplier<String> supplier = () -> "test:renderer";
        assertEquals("test:renderer", PalladiumRendererRegistryDebugCompat.resolveSupplierType(supplier));
    }

    @Test
    void reportsSupplierFailuresWithoutPropagatingThem() {
        Supplier<Object> supplier = () -> {
            throw new IllegalStateException("not registered yet");
        };

        assertEquals("<supplier-error:IllegalStateException>",
                PalladiumRendererRegistryDebugCompat.resolveSupplierType(supplier));
    }

    @Test
    void reportsNonSupplierKeysWithoutCasting() {
        assertEquals("<not-supplier:java.lang.String>",
                PalladiumRendererRegistryDebugCompat.resolveSupplierType("example"));
    }
}
