package org.kylepitcock.legacyconnector.loader.meta;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.kylepitcock.legacyconnector.loader.LegacyJar;
import org.kylepitcock.legacyconnector.loader.LegacyModDecl;
import org.kylepitcock.legacyconnector.loader.LegacyVersion;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.ClassNode;

class ModMetadataEntrypointIdentityTest {
    private static LegacyModDecl declaration(String id,String isolated){var d=new LegacyModDecl();d.legacyId=id;d.fmlIdOverride=isolated;d.mainClass="example/OriginalMod";return d;}
    @Test void isolatedCopiesHaveDifferentEntrypointPackagesAndMatchingFmlAnnotations(){
        var old=declaration("Example",null);var recent=declaration("example","example_l1710");
        assertEquals(old.modernId(),recent.modernId(),"registry namespaces are deliberately unchanged");
        String first=ModMetadataGenerator.entrypointName(old),second=ModMetadataGenerator.entrypointName(recent);
        assertNotEquals(first,second);assertNotEquals(first.substring(0,first.lastIndexOf('/')),second.substring(0,second.lastIndexOf('/')),"split-package resolution must not strip either generated entrypoint");
        var jar=new LegacyJar(Path.of("example.jar"));jar.version=LegacyVersion.V1_7_10;
        for(var declaration:new LegacyModDecl[]{old,recent}){var node=new ClassNode();new ClassReader(ModMetadataGenerator.entrypointClass(jar,declaration)).accept(node,0);assertEquals(ModMetadataGenerator.entrypointName(declaration),node.name);assertEquals(declaration.fmlId(),node.visibleAnnotations.getFirst().values.get(1));}
    }
    @Test void ordinaryEntrypointNameRemainsStable(){assertEquals("legacyconnector/generated/example/LegacyModEntrypoint",ModMetadataGenerator.entrypointName(declaration("Example",null)));}
}
