package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: the ingredient list's search box. Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IIngredientFilter {
    void setFilterText(String filterText);
    String getFilterText();
}
