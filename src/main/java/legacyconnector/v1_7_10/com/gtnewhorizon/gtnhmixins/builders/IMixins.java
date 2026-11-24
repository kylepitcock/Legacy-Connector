package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** GTNH Mixins' enum catalogue of mixins: each constant describes a set of mixin classes and when it applies. */
public interface IMixins extends IBaseTransformer {
    MixinBuilder getBuilder();

    /** Every mixin class name of the catalogue, whatever its conditions. */
    static <E extends Enum<E> & IMixins> List<String> getMixins(Class<E> catalogue) {
        List<String> out = new ArrayList<>();
        for (E e : catalogue.getEnumConstants()) e.getBuilder().addAllClassesTo(out);
        return out;
    }

    /** The mixins of the early phase that apply here. */
    static <E extends Enum<E> & IMixins> List<String> getEarlyMixins(Class<E> catalogue, Set<String> loadedCoreMods) {
        return select(catalogue, Phase.EARLY, loadedCoreMods, Set.of());
    }

    /** The mixins of the late phase that apply here. */
    static <E extends Enum<E> & IMixins> List<String> getLateMixins(Class<E> catalogue, Set<String> loadedMods) {
        return select(catalogue, Phase.LATE, Set.of(), loadedMods);
    }

    private static <E extends Enum<E> & IMixins> List<String> select(Class<E> catalogue, Phase phase, Set<String> coreMods, Set<String> mods) {
        List<AbstractBuilder> builders = new ArrayList<>();
        for (E e : catalogue.getEnumConstants()) builders.add(e.getBuilder());
        Set<ITargetMod> loaded = AbstractBuilder.getLoadedTargetedMods(builders, phase, coreMods, mods);
        List<String> common = new ArrayList<>();
        List<String> side = new ArrayList<>();
        List<AbstractBuilder> ofPhase = new ArrayList<>();
        for (AbstractBuilder b : builders) if (b.phase == phase) ofPhase.add(b);
        AbstractBuilder.loadClasses(ofPhase, loaded, common, side);
        common.addAll(side);
        return common;
    }
}
