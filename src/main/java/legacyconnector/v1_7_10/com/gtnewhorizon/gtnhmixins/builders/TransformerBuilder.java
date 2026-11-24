package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins.builders;

import java.util.function.Supplier;

/** GTNH Mixins' {@code TransformerBuilder}: the class transformer names of one entry of an {@link ITransformers} catalogue. */
public class TransformerBuilder extends AbstractBuilder {
    public TransformerBuilder() {}

    public TransformerBuilder(String description) {}

    public TransformerBuilder addCommonTransformers(String... transformers) {
        addCommonClasses(transformers);
        return this;
    }

    public TransformerBuilder addClientTransformers(String... transformers) {
        addClientClasses(transformers);
        return this;
    }

    public TransformerBuilder addServerTransformers(String... transformers) {
        addServerClasses(transformers);
        return this;
    }

    public TransformerBuilder addSidedTransformers(IBaseTransformer.Side side, String... transformers) {
        addSidedClasses(side, transformers);
        return this;
    }

    @Override
    public TransformerBuilder setApplyIf(Supplier<Boolean> applyIf) {
        super.setApplyIf(applyIf);
        return this;
    }

    @Override
    public TransformerBuilder addRequiredMod(ITargetMod mod) {
        super.addRequiredMod(mod);
        return this;
    }

    @Override
    public TransformerBuilder addExcludedMod(ITargetMod mod) {
        super.addExcludedMod(mod);
        return this;
    }
}
