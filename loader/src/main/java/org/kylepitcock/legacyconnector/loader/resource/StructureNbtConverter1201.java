package org.kylepitcock.legacyconnector.loader.resource;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Narrow binary-NBT migrations for 1.20.1 structure templates. Minecraft data fixers handle vanilla ids on template load;
 * Forge-only entity attributes are not in that mapping and must be updated while the archive is translated.
 */
final class StructureNbtConverter1201 {
    private static final int TAG_END = 0;
    private static final int TAG_BYTE = 1;
    private static final int TAG_SHORT = 2;
    private static final int TAG_INT = 3;
    private static final int TAG_LONG = 4;
    private static final int TAG_FLOAT = 5;
    private static final int TAG_DOUBLE = 6;
    private static final int TAG_BYTE_ARRAY = 7;
    private static final int TAG_STRING = 8;
    private static final int TAG_LIST = 9;
    private static final int TAG_COMPOUND = 10;
    private static final int TAG_INT_ARRAY = 11;
    private static final int TAG_LONG_ARRAY = 12;
    private static final int MAX_DEPTH = 512;
    private static final int MAX_TAGS = 1_000_000;
    private static final int MAX_INFLATED_BYTES = 64 * 1024 * 1024;
    private static final byte[] GZIP_MAGIC = {(byte) 0x1f, (byte) 0x8b};

    record Result(byte[] data, boolean changed, boolean malformed) {}

    private StructureNbtConverter1201() {}

    static Result convert(byte[] original) {
        boolean compressed = isGzip(original);
        try {
            byte[] raw = compressed ? inflate(original) : original;
            if (raw.length > MAX_INFLATED_BYTES) throw new IOException("structure NBT exceeds the size limit");
            Parser parser = new Parser(raw);
            parser.parse();
            if (parser.patches.isEmpty()) return new Result(original, false, false);
            byte[] migrated = applyPatches(raw, parser.patches);
            return new Result(compressed ? gzip(migrated) : migrated, true, false);
        } catch (IOException | IllegalArgumentException e) {
            // The structure stays available to Minecraft's own loader; a malformed or unfamiliar file is never rewritten partially.
            return new Result(original, false, true);
        }
    }

    private static boolean isGzip(byte[] data) {
        return data.length >= 2 && data[0] == GZIP_MAGIC[0] && data[1] == GZIP_MAGIC[1];
    }

    private static byte[] inflate(byte[] data) throws IOException {
        try (var in = new GZIPInputStream(new ByteArrayInputStream(data));
             var out = new ByteArrayOutputStream(Math.min(data.length * 3, 1_048_576))) {
            byte[] buffer = new byte[8192];
            int total = 0;
            for (int read; (read = in.read(buffer)) != -1;) {
                total += read;
                if (total > MAX_INFLATED_BYTES) throw new IOException("structure NBT exceeds the size limit");
                out.write(buffer, 0, read);
            }
            return out.toByteArray();
        }
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream(data.length / 2);
        try (GZIPOutputStream out = new GZIPOutputStream(bytes)) {
            out.write(data);
        }
        return bytes.toByteArray();
    }

    private static byte[] applyPatches(byte[] raw, List<Patch> patches) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(raw.length);
        int offset = 0;
        for (Patch patch : patches) {
            if (patch.start < offset || patch.end < patch.start || patch.end > raw.length) throw new IOException("overlapping NBT string patches");
            out.write(raw, offset, patch.start - offset);
            out.write(patch.replacement);
            offset = patch.end;
        }
        out.write(raw, offset, raw.length - offset);
        return out.toByteArray();
    }

    private record Patch(int start, int end, byte[] replacement) {}

    private static final class Parser {
        private final byte[] raw;
        private final ByteArrayInputStream bytes;
        private final DataInputStream in;
        private final List<Patch> patches = new ArrayList<>();
        private int tags;

        private Parser(byte[] raw) {
            this.raw = raw;
            this.bytes = new ByteArrayInputStream(raw);
            this.in = new DataInputStream(bytes);
        }

        private void parse() throws IOException {
            int type = in.readUnsignedByte();
            if (type != TAG_COMPOUND) throw new IOException("NBT root is not a compound");
            in.readUTF(); // root name
            parseCompound(false, 0);
            if (bytes.available() != 0) throw new IOException("trailing bytes after NBT root");
        }

        private void parseCompound(boolean attributeEntry, int depth) throws IOException {
            checkDepth(depth);
            for (;;) {
                int type = in.readUnsignedByte();
                if (type == TAG_END) return;
                String name = in.readUTF();
                countTag();
                boolean attributeName = attributeEntry && type == TAG_STRING && "Name".equals(name);
                parsePayload(type, name, attributeName, depth + 1);
            }
        }

        private void parsePayload(int type, String name, boolean attributeEntry, int depth) throws IOException {
            checkDepth(depth);
            switch (type) {
                case TAG_BYTE -> in.readByte();
                case TAG_SHORT -> in.readShort();
                case TAG_INT -> in.readInt();
                case TAG_LONG -> in.readLong();
                // Read/write IEEE values as integers so even unusual NaN payload bits are preserved.
                case TAG_FLOAT -> in.readInt();
                case TAG_DOUBLE -> in.readLong();
                case TAG_BYTE_ARRAY -> skipArray(in.readInt(), 1);
                case TAG_STRING -> parseString(name, attributeEntry);
                case TAG_LIST -> parseList(name, depth + 1);
                // This flag is true only for a compound directly used as an Attributes-list element; nested compound children receive false.
                case TAG_COMPOUND -> parseCompound(attributeEntry, depth + 1);
                case TAG_INT_ARRAY -> skipArray(in.readInt(), Integer.BYTES);
                case TAG_LONG_ARRAY -> skipArray(in.readInt(), Long.BYTES);
                default -> throw new IOException("unknown NBT tag type " + type);
            }
        }

        private void parseString(String name, boolean attributeEntry) throws IOException {
            int start = raw.length - bytes.available();
            String value = in.readUTF();
            if (!attributeEntry || !"Name".equals(name)) return;
            // Vanilla generic.* names are handled by Minecraft's template DataFixer; only Forge's namespace is outside that mapping.
            if (!value.startsWith("forge:")) return;
            String migrated = DataConverter1201.mapAttributeId(value);
            if (migrated.equals(value)) return;
            int end = raw.length - bytes.available();
            ByteArrayOutputStream encoded = new ByteArrayOutputStream();
            try (DataOutputStream out = new DataOutputStream(encoded)) {
                out.writeUTF(migrated);
            }
            patches.add(new Patch(start, end, encoded.toByteArray()));
        }

        private void parseList(String name, int depth) throws IOException {
            int elementType = in.readUnsignedByte();
            int length = in.readInt();
            if (length < 0 || length > MAX_TAGS) throw new IOException("invalid NBT list length " + length);
            if (elementType == TAG_END && length != 0) throw new IOException("nonempty TAG_End list");
            boolean attributeList = "Attributes".equals(name) && elementType == TAG_COMPOUND;
            for (int i = 0; i < length; i++) {
                countTag();
                parsePayload(elementType, null, attributeList, depth + 1);
            }
        }

        private void skipArray(int length, int width) throws IOException {
            if (length < 0 || length > MAX_TAGS) throw new IOException("invalid NBT array length " + length);
            long byteLength = (long) length * width;
            if (byteLength > bytes.available()) throw new IOException("truncated NBT array");
            in.skipNBytes(byteLength);
        }

        private void countTag() throws IOException {
            if (++tags > MAX_TAGS) throw new IOException("structure NBT has too many tags");
        }

        private static void checkDepth(int depth) throws IOException {
            if (depth > MAX_DEPTH) throw new IOException("structure NBT nesting is too deep");
        }
    }
}
