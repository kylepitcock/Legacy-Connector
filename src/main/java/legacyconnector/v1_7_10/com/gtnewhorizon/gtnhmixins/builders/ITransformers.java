package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins.builders;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** GTNH Mixins' enum catalogue of class transformers (see {@link IMixins}). */
public interface ITransformers extends IBaseTransformer {
    TransformerBuilder getBuilder();

    /** The transformer class names of the catalogue that apply here. */
    static <E extends Enum<E> & ITransformers> String[] getTransformers(Class<E> catalogue) {
        List<AbstractBuilder> builders = new ArrayList<>();
        for (E e : catalogue.getEnumConstants()) builders.add(e.getBuilder());
        Set<ITargetMod> loaded = AbstractBuilder.getLoadedTargetedMods(builders, null, Set.of(), Set.of());
        List<String> common = new ArrayList<>();
        List<String> side = new ArrayList<>();
        AbstractBuilder.loadClasses(builders, loaded, common, side);
        common.addAll(side);
        return common.toArray(new String[0]);
    }
}
