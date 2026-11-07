package legacyconnector.v1_12_2.vazkii.patchouli.api;

import java.util.function.UnaryOperator;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.HolderLookup;
import org.kylepitcock.legacyconnector.compat.patchouli.ComponentContext1122;
import vazkii.patchouli.api.IVariable;

/**
 * A custom book component (Patchouli 1.12 API). It is also a modern component: the modern Patchouli instantiates the class named in
 * the book's json and calls the modern methods, which hand over to the 1.12 ones with a 1.12 render context.
 */
public interface ICustomComponent extends vazkii.patchouli.api.ICustomComponent {
    @Override
    void build(int componentX, int componentY, int pageNum);

    void render(IComponentRenderContext context, float pticks, int mouseX, int mouseY);

    default void onDisplayed(IComponentRenderContext context) {
    }

    default void mouseClicked(IComponentRenderContext context, int mouseX, int mouseY, int mouseButton) {
    }

    @Override
    default void render(GuiGraphics graphics, vazkii.patchouli.api.IComponentRenderContext context, float pticks, int mouseX, int mouseY) {
        render(new ComponentContext1122(graphics, context), pticks, mouseX, mouseY);
    }

    @Override
    default void onDisplayed(vazkii.patchouli.api.IComponentRenderContext context) {
        onDisplayed(new ComponentContext1122(null, context));
    }

    @Override
    default boolean mouseClicked(vazkii.patchouli.api.IComponentRenderContext context, double mouseX, double mouseY, int mouseButton) {
        mouseClicked(new ComponentContext1122(null, context), (int) mouseX, (int) mouseY, mouseButton);
        return false;
    }

    @Override
    default void onVariablesAvailable(UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
        ComponentContext1122.fillVariables(this, lookup, registries);
    }
}
