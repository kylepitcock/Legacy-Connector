package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins;

/**
 * GTNH Mixins' {@code IEarlyMixinLoader} (a coremod plugin that names mixin configs). The library itself is an engine mod the layer never loads;
 * its API is kept here so the mods that implement it link and load. The layer does not apply 1.7.10 mixins, so nothing calls these.
 */
public interface IEarlyMixinLoader {
    String getMixinConfig();

    java.util.List<String> getMixins(java.util.Set<String> loadedCoreMods);
}
