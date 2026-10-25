package legacyconnector.v1_12_2.net.minecraft.util;

import java.util.Locale;

/** 1.12.2 resource location: a lower-cased namespace and path (legacy code passes anything; the modern id is normalized). */
public class ResourceLocation implements Comparable<ResourceLocation> {
    protected final String namespace;
    protected final String path;

    public ResourceLocation(String namespace, String path) {
        this.namespace = namespace == null || namespace.isEmpty() ? "minecraft" : namespace.toLowerCase(Locale.ROOT);
        this.path = path == null ? "" : path.toLowerCase(Locale.ROOT);
    }

    /** The constructor mods subclass through: the parts are namespace and path. */
    protected ResourceLocation(int unused, String... parts) {
        this(parts[0], parts[1]);
    }

    public ResourceLocation(String location) {
        int colon = location.indexOf(':');
        if (colon >= 0) {
            this.namespace = colon == 0 ? "minecraft" : location.substring(0, colon).toLowerCase(Locale.ROOT);
            this.path = location.substring(colon + 1).toLowerCase(Locale.ROOT);
        } else {
            this.namespace = "minecraft";
            this.path = location.toLowerCase(Locale.ROOT);
        }
    }

    public String getPath() {
        return path;
    }

    public String getNamespace() {
        return namespace;
    }

    @Override
    public String toString() {
        return namespace + ":" + path;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof ResourceLocation r && r.namespace.equals(namespace) && r.path.equals(path);
    }

    @Override
    public int hashCode() {
        return 31 * namespace.hashCode() + path.hashCode();
    }

    @Override
    public int compareTo(ResourceLocation other) {
        int c = path.compareTo(other.path);
        return c != 0 ? c : namespace.compareTo(other.namespace);
    }

    public net.minecraft.resources.ResourceLocation mcc$modern() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(org.kylepitcock.legacyconnector.core.Ids.normalizePath(namespace), org.kylepitcock.legacyconnector.core.Ids.normalizePath(path));
    }
}
