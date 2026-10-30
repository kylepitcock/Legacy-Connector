package legacyconnector.v1_12_2.net.minecraftforge.fml.common.eventhandler;

/** Something run on behalf of a mod (an event listener): makes that mod the active one while it runs. */
public interface IContextSetter {
    default void setModContainer(legacyconnector.v1_12_2.net.minecraftforge.fml.common.ModContainer mod) {
        legacyconnector.v1_12_2.net.minecraftforge.fml.common.Loader.instance().setActiveModContainer(mod);
    }
}
