package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.lang.reflect.InvocationTargetException;
import legacyconnector.vb1_7_3.net.minecraft.src.DimensionBase;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.Dimension;
import legacyconnector.vb1_7_3.net.minecraft.world.dimension.PortalForcer;
import org.junit.jupiter.api.Test;

class DimensionFactoryB173Test {
    static class PackageProvider extends Dimension { PackageProvider(){} }
    static class PackageTeleporter extends PortalForcer { PackageTeleporter(){} }
    static class BrokenProvider extends Dimension {
        BrokenProvider(){throw new IllegalArgumentException("original constructor failure");}
    }
    private static Object create(Class<?> type) throws Exception {
        var factory=DimensionBase.class.getDeclaredMethod("instantiate",Class.class);factory.setAccessible(true);
        return factory.invoke(null,type);
    }
    @Test void relocatedPackagePrivateProviderAndTeleporterConstructorsRemainAccessible() throws Exception {
        assertInstanceOf(PackageProvider.class,create(PackageProvider.class));
        assertInstanceOf(PackageTeleporter.class,create(PackageTeleporter.class));
    }
    @Test void originalConstructorFailureStillPropagates() {
        var error=assertThrows(InvocationTargetException.class,()->create(BrokenProvider.class));
        assertInstanceOf(IllegalArgumentException.class,error.getCause());
        assertEquals("original constructor failure",error.getCause().getMessage());
    }
}
