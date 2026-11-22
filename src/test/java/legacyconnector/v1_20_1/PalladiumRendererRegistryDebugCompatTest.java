package legacyconnector.v1_20_1;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class PalladiumRendererRegistryDebugCompatTest {
    @Test
    void formatsResolvedEntityTypeRegistryKeyWithoutGlobalBootstrap() {
        assertEquals("minecraft:pig",
                PalladiumRendererRegistryDebugCompat.formatEntityTypeKey(ResourceLocation.parse("minecraft:pig")));
    }

    @Test
    void reportsMissingEntityTypeRegistryKey() {
        assertEquals("<unregistered-entity-type>",
                PalladiumRendererRegistryDebugCompat.formatEntityTypeKey(null));
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
    void reportsLinkageErrorsWithoutPropagatingThem() {
        Supplier<Object> supplier = () -> {
            throw new ExceptionInInitializerError("Minecraft registry is not bootstrapped in this test");
        };

        assertEquals("<supplier-error:ExceptionInInitializerError>",
                PalladiumRendererRegistryDebugCompat.resolveSupplierType(supplier));
    }

    @Test
    void reportsNonSupplierKeysWithoutCasting() {
        assertEquals("<not-supplier:java.lang.String>",
                PalladiumRendererRegistryDebugCompat.resolveSupplierType("example"));
    }
}
