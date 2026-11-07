package org.kylepitcock.legacyconnector.compat.patchouli;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.function.UnaryOperator;
import legacyconnector.v1_12_2.net.minecraft.client.gui.FontRenderer;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiButton;
import legacyconnector.v1_12_2.net.minecraft.client.gui.GuiScreen;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.item.crafting.Ingredient;
import legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_12_2.vazkii.patchouli.api.IComponentRenderContext;
import legacyconnector.v1_12_2.vazkii.patchouli.api.IVariableProvider;
import legacyconnector.v1_12_2.vazkii.patchouli.api.VariableHolder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.core.HolderLookup;
import net.minecraft.network.chat.Component;
import org.kylepitcock.legacyconnector.v1_12_2.bridge.Stacks1122;
import vazkii.patchouli.api.IVariable;

/** The 1.12 component render context over the modern one, plus the variable plumbing 1.12 custom components and processors use. */
public final class ComponentContext1122 implements IComponentRenderContext {
    private final GuiGraphics graphics;
    private final vazkii.patchouli.api.IComponentRenderContext modern;

    public ComponentContext1122(GuiGraphics graphics, vazkii.patchouli.api.IComponentRenderContext modern) {
        this.graphics = graphics;
        this.modern = modern;
    }

    @Override
    public GuiScreen getGui() {
        return null;
    }

    @Override
    public FontRenderer getFont() {
        return null;
    }

    @Override
    public void renderItemStack(int x, int y, int mouseX, int mouseY, ItemStack stack) {
        if (graphics != null) modern.renderItemStack(graphics, x, y, mouseX, mouseY, Stacks1122.toModernLive(stack));
    }

    @Override
    public void renderIngredient(int x, int y, int mouseX, int mouseY, Ingredient ingredient) {
        if (graphics == null || ingredient == null) return;
        var stacks = new java.util.ArrayList<net.minecraft.world.item.ItemStack>();
        for (ItemStack s : ingredient.getMatchingStacks()) {
            var m = Stacks1122.toModern(s);
            if (!m.isEmpty()) stacks.add(m);
        }
        if (stacks.isEmpty()) return;
        modern.renderIngredient(graphics, x, y, mouseX, mouseY,
                net.minecraft.world.item.crafting.Ingredient.of(stacks.stream().map(net.minecraft.world.item.ItemStack::getItem).toArray(net.minecraft.world.level.ItemLike[]::new)));
    }

    @Override
    public boolean isAreaHovered(int mouseX, int mouseY, int x, int y, int w, int h) {
        return modern.isAreaHovered(mouseX, mouseY, x, y, w, h);
    }

    @Override
    public void setHoverTooltip(List<String> tooltip) {
        modern.setHoverTooltip(tooltip);
    }

    @Override
    public void registerButton(GuiButton button, int pageNum, Runnable onClick) {
        Button b = Button.builder(Component.literal(button.displayString == null ? "" : button.displayString), x -> onClick.run())
                .bounds(button.x, button.y, button.width, button.height).build();
        modern.registerButton(b, pageNum, onClick);
    }

    @Override
    public ResourceLocation getBookTexture() {
        return new ResourceLocation(modern.getBookTexture().toString());
    }

    @Override
    public ResourceLocation getCraftingTexture() {
        return new ResourceLocation(modern.getCraftingTexture().toString());
    }

    @Override
    public int getTextColor() {
        return modern.getTextColor();
    }

    @Override
    public int getHeaderColor() {
        return modern.getHeaderColor();
    }

    /** The variables of a page as 1.12 strings. */
    public static final class Variables implements IVariableProvider<String> {
        private final vazkii.patchouli.api.IVariableProvider modern;
        private final HolderLookup.Provider registries;

        public Variables(vazkii.patchouli.api.IVariableProvider modern, HolderLookup.Provider registries) {
            this.modern = modern;
            this.registries = registries;
        }

        @Override
        public String get(String key) {
            IVariable v = modern.get(key, registries);
            return v == null ? null : v.asString("");
        }

        @Override
        public boolean has(String key) {
            return modern.has(key);
        }
    }

    /**
     * Fills the fields of a 1.12 custom component marked {@code @VariableHolder} from the page's variables: the modern Patchouli reads the
     * json value into the field first (a literal or a "#name" reference), then asks for the variables to be resolved.
     */
    public static void fillVariables(Object component, UnaryOperator<IVariable> lookup, HolderLookup.Provider registries) {
        for (Class<?> c = component.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (!f.isAnnotationPresent(VariableHolder.class) || Modifier.isStatic(f.getModifiers())) continue;
                try {
                    f.setAccessible(true);
                    Object current = f.get(component);
                    if (!(current instanceof String raw)) continue;
                    String resolved = lookup.apply(IVariable.wrap(raw, registries)).asString(raw);
                    f.set(component, resolved);
                } catch (ReflectiveOperationException | RuntimeException e) {
                    // a field the variable cannot be assigned to keeps the json value
                }
            }
        }
    }
}
