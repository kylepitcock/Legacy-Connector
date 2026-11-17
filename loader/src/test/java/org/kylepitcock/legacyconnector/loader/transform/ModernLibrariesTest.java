package org.kylepitcock.legacyconnector.loader.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** A legacy reference counts as belonging to a required modern mod by package root, so a class the modern jar lacks is reported instead of silently "linked". */
class ModernLibrariesTest {
    private static ModernLibraries ccl() {
        Set<String> pkgs = Set.of("codechicken/lib", "codechicken/lib/vec", "codechicken/lib/render/buffer");
        return new ModernLibraries(Map.of("codechickenlib", new ModernLibraries.Lib("codechickenlib", Path.of("ccl.jar"), pkgs)));
    }

    @Test
    void commonRootNeedsTwoSegments() {
        assertEquals("codechicken/lib", ModernLibraries.commonRoot(Set.of("codechicken/lib", "codechicken/lib/vec")));
        assertNull(ModernLibraries.commonRoot(Set.of("a/b", "c/d")));
        assertNull(ModernLibraries.commonRoot(Set.of("a/b", "a/c")));
    }

    @Test
    void packageOfTheLegacyOnlyClassBelongsToTheLibrary() {
        ModernLibraries libs = ccl();
        assertEquals("codechickenlib", libs.ownerOfPackage("codechicken/lib/vec/Vector3").id());
        // a package the modern jar does not have, under its root: still the library's API
        assertEquals("codechickenlib", libs.ownerOfPackage("codechicken/lib/block/property/unlisted/UnlistedGenericTile").id());
        assertNull(libs.ownerOfPackage("cofh/core/Thing"));
        assertNull(libs.ownerOfPackage("codechicken/nei/Foo"));
        assertNull(libs.restrictedTo(Set.of("other")).ownerOfPackage("codechicken/lib/vec/Vector3"));
    }
}
