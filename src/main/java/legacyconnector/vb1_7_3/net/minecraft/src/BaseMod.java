package legacyconnector.vb1_7_3.net.minecraft.src;

/**
 * Risugami's {@code BaseMod} for Beta 1.7.3. A mod is constructed once ({@code PlatformB173}) and registers its content from its
 * constructor; {@link #ModsLoaded()} follows once every mod is constructed. The hooks that take game objects (ticks, world generation,
 * rendering, crafting callbacks) are not declared here yet: the mods' own implementations are found by name by the runtime as it gains
 * them, and their {@code super} calls reach synthesized defaults.
 */
public abstract class BaseMod {
    public BaseMod() {}

    public int AddFuel(int id) {
        return 0;
    }

    public void AddRenderer(java.util.Map<?, ?> renderers) {}

    public void ModsLoaded() {}

    public abstract String Version();

    @Override
    public String toString() {
        return getClass().getName() + " " + Version();
    }
}
