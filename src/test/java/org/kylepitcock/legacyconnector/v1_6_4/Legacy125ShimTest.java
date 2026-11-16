package org.kylepitcock.legacyconnector.v1_6_4;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import legacyconnector.v1_6_4.net.minecraft.src.MLProp;
import legacyconnector.v1_6_4.net.minecraft.src.Packet230ModLoader;
import legacyconnector.v1_6_4.net.minecraft.src.forge.Property;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.kylepitcock.legacyconnector.core.MLPropConfig;

/** The pure parts of the Minecraft 1.2.5 run-time layer: ModLoaderMP's packet wire form, ModLoader's {@code @MLProp} config file, Forge 3.x's property values. */
class Legacy125ShimTest {
    @Test
    void packet230RoundTrips() {
        Packet230ModLoader p = new Packet230ModLoader();
        p.modId = 12345;
        p.packetType = 7;
        p.dataInt = new int[] {1, -2, 300};
        p.dataFloat = new float[] {1.5F};
        p.dataDouble = new double[] {2.25, -8};
        p.dataString = new String[] {"abc", "", "zz"};
        Packet230ModLoader q = Packet230ModLoader.decode(p.encode());
        assertEquals(12345, q.modId);
        assertEquals(7, q.packetType);
        assertArrayEquals(p.dataInt, q.dataInt);
        assertArrayEquals(p.dataFloat, q.dataFloat);
        assertArrayEquals(p.dataDouble, q.dataDouble);
        assertArrayEquals(p.dataString, q.dataString);
    }

    /** A mod class as ModLoader saw it: public static fields with {@code @MLProp}. */
    public static class ModConfigFixture {
        @MLProp(info = "a block id", min = 100, max = 200)
        public static int blockId = 150;
        @MLProp
        public static boolean enabled = true;
        @MLProp(name = "otherName")
        public static String label = "x";
    }

    @Test
    void mlPropFieldsReadAndWriteTheConfigFile(@TempDir Path dir) throws Exception {
        MLPropConfig.load(ModConfigFixture.class, dir, MLProp.class);
        String written = Files.readString(dir.resolve("ModConfigFixture.cfg"));
        assertTrue(written.contains("blockId=150"), written);
        assertTrue(written.contains("otherName=x"), written);
        Files.writeString(dir.resolve("ModConfigFixture.cfg"), "blockId=180\nenabled=false\notherName=y\n");
        MLPropConfig.load(ModConfigFixture.class, dir, MLProp.class);
        assertEquals(180, ModConfigFixture.blockId);
        assertEquals(false, ModConfigFixture.enabled);
        assertEquals("y", ModConfigFixture.label);
        // a value outside min..max keeps the field as it is
        Files.writeString(dir.resolve("ModConfigFixture.cfg"), "blockId=999\n");
        MLPropConfig.load(ModConfigFixture.class, dir, MLProp.class);
        assertEquals(180, ModConfigFixture.blockId);
    }

    @Test
    void forgeProperty() {
        Property p = new Property();
        p.value = " 42 ";
        assertTrue(p.isIntValue());
        assertEquals(42, p.getInt());
        p.value = "nope";
        assertEquals(7, p.getInt(7));
        p.value = "True";
        assertTrue(p.isBooleanValue());
        assertTrue(p.getBoolean(false));
    }
}
