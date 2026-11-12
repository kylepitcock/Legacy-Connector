package org.kylepitcock.legacyconnector.loader.resource;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StructureNbtConverter1201Test {
    @Test
    void mapsOnlyForgeAttributeNamesInsideStructureEntityAttributes() throws Exception {
        byte[] original = gzip(fixture());
        StructureNbtConverter1201.Result converted = StructureNbtConverter1201.convert(original);

        assertTrue(converted.changed());
        assertFalse(converted.malformed());
        String nbt = new String(inflate(converted.data()), StandardCharsets.ISO_8859_1);
        assertEquals(1, occurrences(nbt, "forge:entity_gravity"), nbt);
        // Three nested metadata values and one unrelated-list value must remain untouched.
        assertEquals(4, occurrences(nbt, "forge:step_height_addition"), nbt);
        assertEquals(1, occurrences(nbt, "minecraft:generic.gravity"), nbt);
        assertEquals(1, occurrences(nbt, "minecraft:generic.step_height"), nbt);
        assertEquals(1, occurrences(nbt, "generic.movement_speed"), nbt);

        StructureNbtConverter1201.Result secondPass = StructureNbtConverter1201.convert(converted.data());
        assertFalse(secondPass.changed());
        assertArrayEquals(converted.data(), secondPass.data());
    }

    @Test
    void dataConverterMapsTheStructurePathAndCountsOneMigrationPerFile() throws Exception {
        DataConverter1201.Stats stats = new DataConverter1201.Stats();
        List<DataConverter1201.Out> out = DataConverter1201.convert(
                "data/alienevo/structures/probe.nbt", gzip(fixture()), stats);

        assertEquals(1, out.size());
        assertEquals("data/alienevo/structure/probe.nbt", out.get(0).path());
        StructureNbtConverter1201.Result secondPass = StructureNbtConverter1201.convert(out.get(0).data());
        assertFalse(secondPass.changed());
        assertArrayEquals(out.get(0).data(), secondPass.data());
        assertEquals(1, stats.counts.get("structure_nbt_attribute_ids"));
        assertEquals(1, stats.counts.get("plural_dir"));
    }

    @Test
    void malformedStructureNbtIsPreservedAndReported() {
        byte[] malformed = {(byte) 0x1f, (byte) 0x8b, 0, 1, 2, 3};
        DataConverter1201.Stats stats = new DataConverter1201.Stats();

        List<DataConverter1201.Out> out = DataConverter1201.convert(
                "data/alienevo/structures/broken.nbt", malformed, stats);

        assertArrayEquals(malformed, out.get(0).data());
        assertEquals("data/alienevo/structure/broken.nbt", out.get(0).path());
        assertEquals(1, stats.counts.get("structure_nbt_unparsable"));
    }

    @Test
    void nonStructureNbtIsNotParsedOrRewritten() {
        byte[] malformed = {(byte) 0x1f, (byte) 0x8b, 0, 1, 2, 3};
        DataConverter1201.Stats stats = new DataConverter1201.Stats();

        List<DataConverter1201.Out> out = DataConverter1201.convert("data/alienevo/loot_tables/probe.nbt", malformed, stats);

        assertArrayEquals(malformed, out.get(0).data());
        assertEquals("data/alienevo/loot_table/probe.nbt", out.get(0).path());
        assertFalse(stats.counts.containsKey("structure_nbt_unparsable"));
        assertFalse(stats.counts.containsKey("structure_nbt_attribute_ids"));
    }

    private static byte[] fixture() throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bytes)) {
            out.writeByte(10); // root compound
            out.writeUTF("");
            out.writeByte(9); // Attributes: list of compounds
            out.writeUTF("Attributes");
            out.writeByte(10);
            out.writeInt(3);
            writeAttribute(out, "forge:entity_gravity");
            writeAttribute(out, "forge:step_height_addition");
            writeAttribute(out, "generic.movement_speed");
            out.writeByte(9); // similarly shaped but unrelated list
            out.writeUTF("OtherNames");
            out.writeByte(10);
            out.writeInt(1);
            writeAttribute(out, "forge:step_height_addition");
            out.writeByte(8); // same id in an unrelated string
            out.writeUTF("OtherValue");
            out.writeUTF("forge:entity_gravity");
            out.writeByte(0); // root end
        }
        return bytes.toByteArray();
    }

    private static void writeAttribute(DataOutputStream out, String id) throws Exception {
        out.writeByte(8);
        out.writeUTF("Name");
        out.writeUTF(id);
        out.writeByte(6);
        out.writeUTF("Base");
        out.writeDouble(0.0);
        out.writeByte(10); // nested Name is not the attribute identifier
        out.writeUTF("Metadata");
        out.writeByte(8);
        out.writeUTF("NestedName");
        out.writeUTF("forge:step_height_addition");
        out.writeByte(0); // metadata compound end
        out.writeByte(0); // attribute compound end
    }

    private static byte[] gzip(byte[] input) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(input);
        }
        return bytes.toByteArray();
    }

    private static byte[] inflate(byte[] input) throws Exception {
        try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(input))) {
            return in.readAllBytes();
        }
    }

    private static int occurrences(String value, String needle) {
        int found = 0;
        for (int offset = 0; (offset = value.indexOf(needle, offset)) >= 0; offset += needle.length()) found++;
        return found;
    }
}
