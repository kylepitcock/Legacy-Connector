package legacyconnector.v1_12_2.mezz.jei.api.gui;

/** JEI 4 (Minecraft 1.12.2) API: adds lines to a slot's tooltip. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
@FunctionalInterface
public interface ITooltipCallback<T> {
    void onTooltip(int slotIndex, boolean input, T ingredient, java.util.List<String> tooltip);
}
