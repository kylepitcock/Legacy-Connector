package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelPart;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;

/** Forge 1.12.2: two model states applied one after the other (the second's transform composed onto the first's). */
public class ModelStateComposition implements IModelState {
    private final IModelState first;
    private final IModelState second;

    public ModelStateComposition(IModelState first, IModelState second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
        Optional<TRSRTransformation> f = first.apply(part), s = second.apply(part);
        if (f.isPresent() && s.isPresent()) return Optional.of(f.get().compose(s.get()));
        return f.isPresent() ? f : s;
    }
}
