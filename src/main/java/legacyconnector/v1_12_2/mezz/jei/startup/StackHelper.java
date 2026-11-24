package legacyconnector.v1_12_2.mezz.jei.startup;

/** JEI 4 (Minecraft 1.12.2) internals some plugins reach into; here a small working copy. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
/** The stack helper plugins cast the helpers' one to; the bridge hands out a subclass. */
public abstract class StackHelper implements legacyconnector.v1_12_2.mezz.jei.api.recipe.IStackHelper {
}
