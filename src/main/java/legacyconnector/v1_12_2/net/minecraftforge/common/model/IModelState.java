package legacyconnector.v1_12_2.net.minecraftforge.common.model;

import java.util.Optional;

/** The transform a model is baked with, per part (empty part: the whole model). */
public interface IModelState {
    Optional<TRSRTransformation> apply(Optional<? extends IModelPart> part);
}
