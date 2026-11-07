package legacyconnector.v1_12_2.mcp.mobius.waila.api.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaBlockDecorator;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaConfigHandler;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaDataProvider;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaEntityProvider;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaRegistrar;
import legacyconnector.v1_12_2.mcp.mobius.waila.api.IWailaTooltipRenderer;

/**
 * Waila's registrar and option table for 1.12.2 mods. It only records what the mods register; the Jade half
 * ({@code compat.jade.LegacyConnectorJadePlugin}) runs the providers when the player looks at something.
 */
public class ModuleRegistrar implements IWailaRegistrar, IWailaConfigHandler {
    private static final ModuleRegistrar INSTANCE = new ModuleRegistrar();

    public static ModuleRegistrar instance() {
        return INSTANCE;
    }

    public record Registration<P>(P provider, Class<?> target) {}

    public final List<Registration<IWailaDataProvider>> stack = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> head = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> body = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> tail = new ArrayList<>();
    public final List<Registration<IWailaDataProvider>> nbt = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityOverride = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityHead = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityBody = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityTail = new ArrayList<>();
    public final List<Registration<IWailaEntityProvider>> entityNbt = new ArrayList<>();
    private final Map<String, Boolean> config = new HashMap<>();
    private final Map<String, HashMap<String, String>> modules = new HashMap<>();

    protected ModuleRegistrar() {}

    public boolean isEmpty() {
        return stack.isEmpty() && head.isEmpty() && body.isEmpty() && tail.isEmpty() && entityHead.isEmpty() && entityBody.isEmpty()
                && entityTail.isEmpty() && entityOverride.isEmpty();
    }

    @Override
    public void addConfig(String modName, String key, String text) {
        addConfig(modName, key, text, true);
    }

    @Override
    public void addConfig(String modName, String key, String text, boolean defaultValue) {
        config.putIfAbsent(key, defaultValue);
        modules.computeIfAbsent(modName, k -> new HashMap<>()).put(key, text);
    }

    @Override
    public void addConfigRemote(String modName, String key, String text) {
        addConfig(modName, key, text, true);
    }

    @Override
    public void addConfigRemote(String modName, String key, String text, boolean defaultValue) {
        addConfig(modName, key, text, defaultValue);
    }

    @Override
    public void addConfig(String modName, String key) {
        addConfig(modName, key, key, true);
    }

    @Override
    public void addConfig(String modName, String key, boolean defaultValue) {
        addConfig(modName, key, key, defaultValue);
    }

    @Override
    public void addConfigRemote(String modName, String key) {
        addConfig(modName, key, key, true);
    }

    @Override
    public void addConfigRemote(String modName, String key, boolean defaultValue) {
        addConfig(modName, key, key, defaultValue);
    }

    @Override
    public void registerStackProvider(IWailaDataProvider provider, Class<?> block) {
        stack.add(new Registration<>(provider, block));
    }

    @Override
    public void registerHeadProvider(IWailaDataProvider provider, Class<?> block) {
        head.add(new Registration<>(provider, block));
    }

    @Override
    public void registerBodyProvider(IWailaDataProvider provider, Class<?> block) {
        body.add(new Registration<>(provider, block));
    }

    @Override
    public void registerTailProvider(IWailaDataProvider provider, Class<?> block) {
        tail.add(new Registration<>(provider, block));
    }

    @Override
    public void registerNBTProvider(IWailaDataProvider provider, Class<?> block) {
        nbt.add(new Registration<>(provider, block));
    }

    @Override
    public void registerHeadProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityHead.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerBodyProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityBody.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerTailProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityTail.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerOverrideEntityProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityOverride.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerNBTProvider(IWailaEntityProvider provider, Class<?> entity) {
        entityNbt.add(new Registration<>(provider, entity));
    }

    @Override
    public void registerDecorator(IWailaBlockDecorator decorator, Class<?> block) {
        // Jade has no block decoration hook
    }

    @Override
    public void registerTooltipRenderer(String name, IWailaTooltipRenderer renderer) {
        // Jade draws its own tooltip
    }

    @Override
    public Set<String> getModuleNames() {
        return modules.keySet();
    }

    @Override
    public HashMap<String, String> getConfigKeys(String module) {
        return modules.getOrDefault(module, new HashMap<>());
    }

    @Override
    public boolean getConfig(String key, boolean defaultValue) {
        return config.getOrDefault(key, defaultValue);
    }

    @Override
    public boolean getConfig(String key) {
        return getConfig(key, true);
    }

    /** Waila's {@code ConfigHandler.setConfig}: switches one option. */
    public void mcc$setConfig(String key, boolean value) {
        config.put(key, value);
    }
}
