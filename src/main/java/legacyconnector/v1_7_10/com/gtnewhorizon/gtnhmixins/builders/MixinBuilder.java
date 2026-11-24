package legacyconnector.v1_7_10.com.gtnewhorizon.gtnhmixins.builders;

import java.util.function.Supplier;

/** GTNH Mixins' {@code MixinBuilder}: the mixin class names of one entry of an {@link IMixins} catalogue, with the conditions they apply under. */
public class MixinBuilder extends AbstractBuilder {
    public MixinBuilder() {}

    public MixinBuilder(String description) {}

    public MixinBuilder addCommonMixins(String... mixins) {
        addCommonClasses(mixins);
        return this;
    }

    public MixinBuilder addClientMixins(String... mixins) {
        addClientClasses(mixins);
        return this;
    }

    public MixinBuilder addServerMixins(String... mixins) {
        addServerClasses(mixins);
        return this;
    }

    public MixinBuilder addSidedMixins(IBaseTransformer.Side side, String... mixins) {
        addSidedClasses(side, mixins);
        return this;
    }

    @Override
    public MixinBuilder setApplyIf(Supplier<Boolean> applyIf) {
        super.setApplyIf(applyIf);
        return this;
    }

    @Override
    public MixinBuilder addRequiredMod(ITargetMod mod) {
        super.addRequiredMod(mod);
        return this;
    }

    @Override
    public MixinBuilder addExcludedMod(ITargetMod mod) {
        super.addExcludedMod(mod);
        return this;
    }

    public MixinBuilder setPhase(IBaseTransformer.Phase phase) {
        this.phase = phase;
        return this;
    }
}
