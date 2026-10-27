package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableMap;
import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelPart;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;

/** Forge 1.12.2: a model state from a map of part transforms (and a transform for the whole model). */
public final class SimpleModelState implements IModelState {
    private final ImmutableMap<? extends IModelPart, TRSRTransformation> map;
    private final Optional<TRSRTransformation> def;

    public SimpleModelState(ImmutableMap<? extends IModelPart, TRSRTransformation> map) {
        this(map, Optional.empty());
    }

    public SimpleModelState(ImmutableMap<? extends IModelPart, TRSRTransformation> map, Optional<TRSRTransformation> def) {
        this.map = map;
        this.def = def;
    }

    @Override
    public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
        if (!part.isPresent()) return def;
        return Optional.ofNullable(map.get(part.get()));
    }
}
