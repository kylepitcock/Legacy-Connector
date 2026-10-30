package legacyconnector.v1_12_2.net.minecraftforge.common.animation;

/** Forge 1.12.2: receives the animation events that fired since the last frame. */
public interface IEventHandler<T> {
    void handleEvents(T instance, float time, Iterable<Event> pastEvents);
}
