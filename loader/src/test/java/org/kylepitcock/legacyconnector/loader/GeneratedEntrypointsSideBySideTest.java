package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.loader.meta.ModMetadataGenerator;

class GeneratedEntrypointsSideBySideTest {
    @TempDir Path temp;
    @Test void splitPackageResolutionKeepsBothActualGeneratedEntrypointsInTheirOwnerJars()throws Exception{
        var paths=new ArrayList<Path>();var names=new ArrayList<String>();
        for(int i=0;i<2;i++){
            var decl=new LegacyModDecl();decl.legacyId="example";decl.mainClass="example/OriginalMod";if(i==1)decl.fmlIdOverride="example_l1710";
            var jar=new LegacyJar(temp.resolve("mod"+i+".jar"));jar.version=i==0?LegacyVersion.V1_12_2:LegacyVersion.V1_7_10;
            String entry=ModMetadataGenerator.entrypointName(decl)+".class";names.add(entry);paths.add(jar.path);
            try(var out=new ZipOutputStream(Files.newOutputStream(jar.path))){out.putNextEntry(new ZipEntry(entry));out.write(ModMetadataGenerator.entrypointClass(jar,decl));out.closeEntry();}
        }
        var outcome=SplitPackageResolver.resolve(paths,temp.resolve("cache"));
        for(int i=0;i<2;i++)try(var jar=new ZipFile(outcome.jars().get(i).toFile())){assertNotNull(jar.getEntry(names.get(i)),"each FML mod must retain its own discoverable @Mod constructor after package resolution");}
    }
}
