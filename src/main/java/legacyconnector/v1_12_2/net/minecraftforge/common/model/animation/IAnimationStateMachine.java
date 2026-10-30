package legacyconnector.v1_12_2.net.minecraftforge.common.model.animation;

import legacyconnector.v1_12_2.net.minecraftforge.common.animation.Event;
import legacyconnector.v1_12_2.net.minecraftforge.common.model.IModelState;
import org.apache.commons.lang3.tuple.Pair;

/** An animated model's state machine: the model state at a time (and the events that fired), and moving to another state. */
public interface IAnimationStateMachine {
    Pair<IModelState, Iterable<Event>> apply(float time);

    void transition(String newState);

    String currentState();

    void shouldHandleSpecialEvents(boolean value);
}
