package legacyconnector.v1_12_2.vazkii.patchouli.api;

/** The variables of a template or page a component processor reads (Patchouli 1.12 API). */
public interface IVariableProvider<T> {
    T get(String key);

    boolean has(String key);
}
