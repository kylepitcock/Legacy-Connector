package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.*;
import com.google.gson.JsonParser;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.zip.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.loader.meta.ModMetadataGenerator;
import org.objectweb.asm.*;

class BetaNamespaceTest {
    @TempDir Path root;
    @Test void actualBetaInspectionPrefixesIdentityButPreservesOriginalClassAndNames()throws Exception{
        Path folder=Files.createDirectory(root.resolve("b1.7.3")),path=folder.resolve("example.zip");var writer=new ClassWriter(0);writer.visit(Opcodes.V1_6,Opcodes.ACC_PUBLIC,"mod_Example",null,"BaseMod",null);writer.visitEnd();
        try(var out=new ZipOutputStream(Files.newOutputStream(path))){out.putNextEntry(new ZipEntry("mod_Example.class"));out.write(writer.toByteArray());out.closeEntry();out.putNextEntry(new ZipEntry("mob/original.png"));out.write(new byte[]{1,2,3});out.closeEntry();}
        LegacyJar jar;try(var zip=new ZipFile(path.toFile())){jar=BetaJars.inspect(zip,path,new LegacyJar(path));}
        assertNotNull(jar);var declaration=jar.mods.getFirst();assertEquals("Example",declaration.legacyId);assertEquals("b173_example",declaration.modernId());assertEquals("b173_example",declaration.fmlId());assertTrue(declaration.mainClass.startsWith("legacyconnector_b173/example/"),"Java packages retain their original stable lane");
        var metadata=JsonParser.parseString(new String(ModMetadataGenerator.legacyJson(jar),StandardCharsets.UTF_8)).getAsJsonObject().getAsJsonArray("mods").get(0).getAsJsonObject();assertEquals("b173_example",metadata.get("modernId").getAsString());assertEquals("Example",metadata.get("legacyId").getAsString());
        try(var zip=new ZipFile(path.toFile())){assertNotNull(zip.getEntry("mob/original.png"),"original resource paths remain untouched");}
        var forge=new LegacyModDecl();forge.legacyId="Example";assertEquals("example",forge.modernId());assertNotEquals(forge.fmlId(),declaration.fmlId());
    }
    @Test void everyBetaIdGetsOneStableValidPrefix(){
        assertEquals("b173_example",LegacyModDecl.betaModernId("Example"));assertEquals("b173_buildcraft_core",LegacyModDecl.betaModernId("BuildCraft|Core"));String longId=LegacyModDecl.betaModernId("x".repeat(100));assertEquals(64,longId.length());assertTrue(longId.matches("[a-z][a-z0-9_]{1,63}"));assertEquals(longId,LegacyModDecl.betaModernId("X".repeat(100)));assertNotEquals(LegacyModDecl.betaModernId("x".repeat(100)+"a"),LegacyModDecl.betaModernId("x".repeat(100)+"b"));
    }
}
