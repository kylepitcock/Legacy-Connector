package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins;

/** GTNH Mixins' {@code ILateMixinLoader}: the same for mixins that target mod classes (see {@link IEarlyMixinLoader}). */
public interface ILateMixinLoader {
    String getMixinConfig();

    java.util.List<String> getMixins(java.util.Set<String> loadedMods);
}
