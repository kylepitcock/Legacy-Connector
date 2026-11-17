package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Locale;
import net.minecraft.resources.ResourceLocation;

/**
 * 1.20.1's {@code net.minecraft.client.resources.model.ModelResourceLocation} as translated code sees it (the loader maps the class name to this one): a
 * {@link ResourceLocation} that also carries a variant. In 1.21.1 the real class is an unrelated record ({@code id}, {@code variant}); the layer's access
 * transformer opens ResourceLocation for this subclass. Legacy code therefore keeps passing model locations wherever a ResourceLocation is expected and keeps
 * its keys in generic containers; conversion to the record happens only at the boundary with the modern game ({@link ModelLocationCompat}).
 *
 * <p>Semantics of 1.20.1: the variant is lower-cased, equality and the hash code include it (a location with a variant never equals one without), {@code
 * toString} is {@code namespace:path#variant}, the single string constructor splits at {@code '#'}.
 */
public class LegacyModelResourceLocation extends ResourceLocation {
    public static final char VARIANT_SEPARATOR = '#';

    private final String variant;

    public LegacyModelResourceLocation(String namespace, String path, String variant) {
        super(valid(namespace, path).getNamespace(), path);
        this.variant = lowercase(variant);
    }

    public LegacyModelResourceLocation(ResourceLocation location, String variant) {
        this(location.getNamespace(), location.getPath(), variant);
    }

    public LegacyModelResourceLocation(String location) {
        this(decompose(location));
    }

    private LegacyModelResourceLocation(String[] parts) {
        this(parts[0], parts[1], parts[2]);
    }

    public static LegacyModelResourceLocation vanilla(String path, String variant) {
        return new LegacyModelResourceLocation("minecraft", path, variant);
    }

    private static ResourceLocation valid(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path); // throws the ResourceLocationException 1.20.1 threw for bad characters
    }

    private static String lowercase(String variant) {
        return variant == null ? "" : variant.toLowerCase(Locale.ROOT);
    }

    private static String[] decompose(String location) {
        int hash = location.indexOf(VARIANT_SEPARATOR);
        String id = location;
        String variant = "";
        if (hash >= 0) {
            variant = location.substring(hash + 1);
            if (hash > 1) id = location.substring(0, hash);
        }
        ResourceLocation parsed = ResourceLocation.parse(id);
        return new String[] {parsed.getNamespace(), parsed.getPath(), lowercase(variant)};
    }

    public String getVariant() {
        return variant;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof LegacyModelResourceLocation m && super.equals(o) && variant.equals(m.variant);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + variant.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + VARIANT_SEPARATOR + variant;
    }
}
