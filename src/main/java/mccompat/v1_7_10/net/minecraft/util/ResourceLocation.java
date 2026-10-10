package mccompat.v1_7_10.net.minecraft.util;

import org.kylepitcock.mccompat.core.Ids;

/**
 * 1.7.10 resource locations allowed upper case and most characters. Paths are normalized the same way the
 * loader normalizes asset file names, so a legacy {@code "IC2:textures/Foo.png"} still finds its (renamed) file.
 */
public class ResourceLocation {
    private final String resourceDomain;
    private final String resourcePath;

    public ResourceLocation(String domain, String path) {
        this.resourceDomain = domain;
        this.resourcePath = path;
    }

    public ResourceLocation(String location) {
        int colon = location.indexOf(':');
        if (colon >= 0) {
            this.resourceDomain = colon == 0 ? "minecraft" : location.substring(0, colon);
            this.resourcePath = location.substring(colon + 1);
        } else {
            this.resourceDomain = "minecraft";
            this.resourcePath = location;
        }
    }

    public String getResourcePath() {
        return resourcePath;
    }

    public String getResourceDomain() {
        return resourceDomain;
    }

    @Override
    public String toString() {
        return resourceDomain + ":" + resourcePath;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof ResourceLocation r && r.resourceDomain.equals(resourceDomain) && r.resourcePath.equals(resourcePath);
    }

    @Override
    public int hashCode() {
        return 31 * resourceDomain.hashCode() + resourcePath.hashCode();
    }

    public net.minecraft.resources.ResourceLocation mcc$modern() {
        return net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(Ids.normalizePath(resourceDomain), Ids.normalizePath(resourcePath));
    }
}
