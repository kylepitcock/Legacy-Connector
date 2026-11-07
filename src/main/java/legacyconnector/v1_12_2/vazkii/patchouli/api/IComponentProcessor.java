package legacyconnector.v1_12_2.vazkii.patchouli.api;

import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import net.minecraft.world.level.Level;
import org.kylepitcock.legacyconnector.compat.patchouli.ComponentContext1122;
import vazkii.patchouli.api.IVariable;

/**
 * Computes template variables for a page (Patchouli 1.12 API). It is also a modern processor: the modern Patchouli instantiates the
 * class named in the entry's json and calls the modern methods, which hand over to the 1.12 ones.
 */
public interface IComponentProcessor extends vazkii.patchouli.api.IComponentProcessor {
    void setup(IVariableProvider<String> variables);

    String process(String key);

    default void refresh(GuiScreen parent, int left, int top) {
    }

    @Override
    default boolean allowRender(String group) {
        return true;
    }

    @Override
    default void setup(Level level, vazkii.patchouli.api.IVariableProvider variables) {
        setup(new ComponentContext1122.Variables(variables, level == null ? null : level.registryAccess()));
    }

    @Override
    default IVariable process(Level level, String key) {
        String value = org.kylepitcock.legacyconnector.compat.patchouli.Ids1122.processed(process(key));
        return value == null ? null : IVariable.wrap(value, level == null ? null : level.registryAccess());
    }
}
