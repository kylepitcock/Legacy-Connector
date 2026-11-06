package legacyconnector.v1_12_2.net.minecraft.client.renderer.block.model;

import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;

/** A model name plus a variant: "domain:path#variant" ("inventory" is the item model). */
public class ModelResourceLocation extends ResourceLocation {
    private final String variant;

    protected ModelResourceLocation(String[] parsed) {
        super(parsed[0], parsed[1]);
        this.variant = parsed[2].isEmpty() ? "normal" : parsed[2].toLowerCase(java.util.Locale.ROOT);
    }

    public ModelResourceLocation(String location) {
        this(parse(location));
    }

    public ModelResourceLocation(String location, String variant) {
        this(parse(location + (variant == null || variant.isEmpty() ? "" : "#" + variant)));
    }

    public ModelResourceLocation(ResourceLocation location, String variant) {
        this(new String[] {location.getNamespace(), location.getPath(), variant == null ? "" : variant});
    }

    public ModelResourceLocation(String namespace, String path, String variant) {
        this(new String[] {namespace, path, variant == null ? "" : variant});
    }

    private static String[] parse(String location) {
        String variant = "";
        int hash = location.indexOf('#');
        if (hash >= 0) {
            variant = location.substring(hash + 1);
            location = location.substring(0, hash);
        }
        int colon = location.indexOf(':');
        String ns = colon >= 0 ? location.substring(0, colon) : "minecraft";
        String path = colon >= 0 ? location.substring(colon + 1) : location;
        return new String[] {ns, path, variant};
    }

    public String getVariant() {
        return variant;
    }

    /** Like Forge's: equal only with the same variant (mods key custom models by "block#normal" and ask for "block#base0"). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        return o instanceof ModelResourceLocation m && super.equals(o) && variant.equals(m.variant);
    }

    @Override
    public int hashCode() {
        return 31 * super.hashCode() + variant.hashCode();
    }

    @Override
    public String toString() {
        return super.toString() + "#" + variant;
    }
}
