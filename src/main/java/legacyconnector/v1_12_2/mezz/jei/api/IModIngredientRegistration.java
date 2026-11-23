package legacyconnector.v1_12_2.mezz.jei.api;

/** JEI 4 (Minecraft 1.12.2) API: registration of a mod's own ingredient types (not bridged: modern JEI needs modern helpers and renderers). Legacy plugins run on the modern JEI through org.kylepitcock.legacyconnector.compat.jei.Jei1122Bridge. */
public interface IModIngredientRegistration {
    legacyconnector.v1_12_2.mezz.jei.api.IJeiHelpers getJeiHelpers();
}
