package org.kylepitcock.legacyconnector.compat.patchouli;

import java.util.Map;

/**
 * Converts the book files of one legacy Minecraft version for the modern Patchouli. A new legacy version adds one implementation and registers it
 * in {@link #BY_VERSION}; the loader bridge (LibraryBridges) and {@code bridges.patchouli.enabled} are version independent.
 *
 * <p>Version notes: 1.12.2 keeps books in {@code assets/<mod>/patchouli_books} ({@link PatchouliBooks}). 1.14 to 1.20 keep book.json and contents
 * in {@code data/<mod>/patchouli_books}; those layers are in-place remaps onto NeoForge 1.21.1, so {@code vazkii.patchouli.api} calls resolve against
 * the real modern API and only the books need work (item {@code mod:item{nbt}} stacks become {@code mod:item[components]}, and contents move to the
 * resource pack because 1.20+ requires {@code use_resource_pack}). None of those layers exists yet, so no converter is registered for them.</p>
 */
public interface BookConverter {
    /** The legacy version this converts ("1.12.2"). */
    String legacyVersion();

    /** Whether the version keeps book contents in {@code assets/} (1.12.2) rather than {@code data/}. */
    boolean booksInAssets();

    /** Converts one json file of a book ({@code relativeToBook} is the path below the book folder). */
    byte[] convert(String relativeToBook, byte[] data, String namespace, PatchouliBooks.Resolver resolver);

    /** The converters by legacy version. */
    Map<String, BookConverter> BY_VERSION = Map.of("1.12.2", new BookConverter() {
        @Override
        public String legacyVersion() {
            return "1.12.2";
        }

        @Override
        public boolean booksInAssets() {
            return true;
        }

        @Override
        public byte[] convert(String relativeToBook, byte[] data, String namespace, PatchouliBooks.Resolver resolver) {
            return PatchouliBooks.convert(relativeToBook, data, namespace, resolver);
        }
    });
}
