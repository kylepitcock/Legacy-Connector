package org.kylepitcock.legacyconnector.loader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.loader.mapping.ForwardRenames;
import org.kylepitcock.legacyconnector.loader.mapping.NotchNames;
import org.kylepitcock.legacyconnector.loader.synth.LegacyHierarchyData;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

class Forward147Test {
    @Test
    void versionIsAForwardVersionChainedOver152And164() {
        assertSame(LegacyVersion.V1_4_7, LegacyVersion.fromMinecraftVersion("1.4.7"));
        assertSame(LegacyVersion.V1_4_7, LegacyVersion.fromMinecraftVersion("[1.4.6]"));
        assertSame(LegacyVersion.V1_5_2, LegacyVersion.fromMinecraftVersion("1.5.2"));
        assertSame(LegacyVersion.V1_7_10, LegacyVersion.V1_4_7.forward());
        assertSame(LegacyVersion.V1_7_10, LegacyVersion.V1_4_7.family());
        assertTrue(LegacyVersion.V1_4_7.numericIds());
        assertTrue(LegacyVersion.V1_4_7.flatNames());
        assertFalse(LegacyVersion.V1_5_2.flatNames());
        assertEquals(List.of("1.6.4", "1.5.2", "1.4.7"), LegacyVersion.V1_4_7.forwardTableIds());
        assertEquals("legacyconnector/v1_6_4/", LegacyVersion.V1_4_7.shimPrefix());
        assertEquals("legacyconnector_synth/v1_4_7/", LegacyVersion.V1_4_7.synthPrefix());
    }

    @Test
    void flatNamesMoveIntoThePackagesOf152AndThenFollowTheForwardRenames() {
        assertEquals("net/minecraft/block/Block", ForwardRenames.flat(LegacyVersion.V1_4_7, "net/minecraft/src/Block"));
        assertEquals("net/minecraft/entity/player/EntityPlayer$EnumStatus", ForwardRenames.flat(LegacyVersion.V1_4_7, "net/minecraft/src/EntityPlayer$EnumStatus"));
        // the ModLoader emulation stays flat; other versions are untouched
        assertEquals("net/minecraft/src/BaseMod", ForwardRenames.flat(LegacyVersion.V1_4_7, "net/minecraft/src/BaseMod"));
        assertEquals("net/minecraft/src/Block", ForwardRenames.flat(LegacyVersion.V1_5_2, "net/minecraft/src/Block"));
        assertEquals("net/minecraft/block/Block", ForwardRenames.map(LegacyVersion.V1_4_7, "net/minecraft/src/Block"));
        // flat name -> 1.5.2 package -> 1.6.4 forward rename (Icon style renames of the 1.7.10 shim)
        assertEquals("net/minecraft/block/Block$SoundType", ForwardRenames.map(LegacyVersion.V1_4_7, "net/minecraft/src/StepSound"));
        assertEquals("net/minecraft/item/Item$ToolMaterial", ForwardRenames.map(LegacyVersion.V1_4_7, "net/minecraft/src/EnumToolMaterial"));
        assertEquals("net/minecraft/src/Block", ForwardRenames.map(LegacyVersion.V1_7_10, "net/minecraft/src/Block"));
    }

    @Test
    void flatTableTargetsExistInThe152Hierarchy() {
        var h147 = LegacyHierarchyData.forVersion(LegacyVersion.V1_4_7);
        var h152 = LegacyHierarchyData.forVersion(LegacyVersion.V1_5_2);
        int checked = 0, absent = 0;
        for (String flat : List.of("Block", "Item", "ItemStack", "World", "Entity", "EntityPlayer", "TileEntity", "NBTTagCompound", "RenderEngine", "TextureFX",
                "StepSound", "EnumToolMaterial", "BiomeGenBase", "Material", "Tessellator", "GuiScreen", "Container", "Packet250CustomPayload")) {
            String flatName = "net/minecraft/src/" + flat;
            assertNotNull(h147.get(flatName), "1.4.7 has no " + flatName);
            String packaged = ForwardRenames.flat(LegacyVersion.V1_4_7, flatName);
            assertTrue(packaged.startsWith("net/minecraft/") && !packaged.startsWith("net/minecraft/src/"), flatName + " is not packaged: " + packaged);
            checked++;
            if (h152.get(packaged) == null) absent++;
        }
        // classes 1.5 dropped or moved again (TextureFX: the texture system was rewritten) may be absent, but not the core ones
        assertTrue(checked - absent >= checked - 3, "too many flat classes without a 1.5.2 counterpart: " + absent);
    }

    @Test
    void notchNamesOf147ResolveToFlatClasses() {
        NotchNames n = NotchNames.get("1.4.7");
        assertFalse(n.isEmpty());
        // the table is written from the 1.4.7 client jar and the MCP 6.x joined.srg: notch name -> flat MCP name
        assertEquals("net/minecraft/src/ThreadDownloadResources", n.mcpClass("arn"));
        assertNull(n.mcpClass("net"));
    }

    @Test
    void modLoaderJarIsRecognisedAs147(@TempDir Path dir) throws IOException {
        ClassWriter cw = new ClassWriter(0);
        cw.visit(Opcodes.V1_6, Opcodes.ACC_PUBLIC, "mod_Example", null, "BaseMod", null);
        var init = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "()V", null, null);
        init.visitCode();
        init.visitVarInsn(Opcodes.ALOAD, 0);
        init.visitMethodInsn(Opcodes.INVOKESPECIAL, "BaseMod", "<init>", "()V", false);
        init.visitInsn(Opcodes.RETURN);
        init.visitMaxs(1, 1);
        init.visitEnd();
        cw.visitEnd();
        Path jar = dir.resolve("mod_Example-1.4.7.zip");
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("mod_Example.class"));
            zip.write(cw.toByteArray());
            zip.closeEntry();
        }
        Files.write(jar, bytes.toByteArray());
        LegacyJar inspected = LegacyJarInspector.inspect(jar);
        assertNotNull(inspected);
        assertSame(LegacyVersion.V1_4_7, inspected.version);
        assertEquals(1, inspected.mods.size());
        assertEquals("mod_Example", inspected.mods.get(0).legacyId);
        assertTrue(inspected.mods.get(0).modLoader);
    }
}
