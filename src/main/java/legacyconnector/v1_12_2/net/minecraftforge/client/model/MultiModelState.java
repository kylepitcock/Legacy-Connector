package legacyconnector.v1_12_2.net.minecraftforge.client.model;

import com.google.common.collect.ImmutableList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelPart;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.TRSRTransformation;
import org.apache.commons.lang3.tuple.Pair;

/** Forge: the states of the sub-models of a combined model, one per (model, index) part. */
public final class MultiModelState implements IModelState {
    private final Map<MultiModelPart, IModelState> states = new HashMap<>();

    public <M extends IModel, S extends IModelState> MultiModelState(ImmutableList<Pair<M, S>> statesIn) {
        for (int i = 0; i < statesIn.size(); i++) {
            Pair<M, S> pair = statesIn.get(i);
            states.put(new MultiModelPart(pair.getLeft(), i), pair.getRight());
        }
    }

    /** The state of one sub-model: the part's state from the combined state when it has one, else the model's default. */
    public static IModelState getPartState(IModelState state, IModel model, int index) {
        if (state.apply(Optional.of(new MultiModelPart(model, index))).isPresent()) return new PartState(state, model, index);
        return model.getDefaultState();
    }

    @Override
    public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
        if (part.isPresent() && part.get() instanceof MultiModelPart key && states.containsKey(key)) {
            return Optional.of(states.get(key).apply(Optional.empty()).orElse(TRSRTransformation.identity()));
        }
        return Optional.empty();
    }

    private static final class PartState implements IModelState {
        private final IModelState state;
        private final IModel model;
        private final int index;

        PartState(IModelState state, IModel model, int index) {
            this.state = state;
            this.model = model;
            this.index = index;
        }

        @Override
        public Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part) {
            if (!part.isPresent()) return state.apply(Optional.of(new MultiModelPart(model, index)));
            return Optional.empty();
        }
    }

    private static final class MultiModelPart implements IModelPart {
        private final IModel model;
        private final int index;

        MultiModelPart(IModel model, int index) {
            this.model = model;
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            return this == o || o instanceof MultiModelPart other && index == other.index && Objects.equals(model, other.model);
        }

        @Override
        public int hashCode() {
            return Objects.hash(model, index);
        }
    }
}
