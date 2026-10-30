package legacyconnector.v1_12_2.net.minecraftforge.client.settings;

import java.util.ArrayList;
import java.util.List;
import legacyconnector.v1_12_2.net.minecraft.client.settings.KeyBinding;

/**
 * Forge's key code to bindings index ({@code KeyBinding.HASH}). Answered live from the mods' bindings and the game's own key mappings,
 * so adding or removing entries by hand changes nothing (the game keeps its own index).
 */
public class KeyBindingMap {
    /** The binding of a key code that is active now (its conflict context and modifier hold), or null. */
    public KeyBinding lookupActive(int keyCode) {
        if (keyCode == 0) return null;
        for (KeyBinding k : KeyBinding.mcc$registered()) {
            if (k.isActiveAndMatches(keyCode)) return k;
        }
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return null;
        var options = net.minecraft.client.Minecraft.getInstance().options;
        if (options == null) return null;
        for (var mapping : options.keyMappings) {
            KeyBinding k = mcc$wrap(mapping);
            if (k != null && k.isActiveAndMatches(keyCode)) return k;
        }
        return null;
    }

    /** Every binding set to a key code, active or not. */
    public List<KeyBinding> lookupAll(int keyCode) {
        List<KeyBinding> out = new ArrayList<>();
        if (keyCode == 0) return out;
        java.util.Set<Object> seen = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (KeyBinding k : KeyBinding.mcc$registered()) {
            if (k.getKeyCode() == keyCode) out.add(k);
            seen.add(k.mcc$modern());
        }
        if (!net.neoforged.fml.loading.FMLEnvironment.dist.isClient()) return out;
        var options = net.minecraft.client.Minecraft.getInstance().options;
        if (options == null) return out;
        for (var mapping : options.keyMappings) {
            if (seen.contains(mapping)) continue;
            KeyBinding k = mcc$wrap(mapping);
            if (k != null && k.getKeyCode() == keyCode) out.add(k);
        }
        return out;
    }

    public void addKey(int keyCode, KeyBinding keyBinding) {
    }

    public void removeKey(KeyBinding keyBinding) {
    }

    public void clearMap() {
    }

    private static final java.util.Map<Object, KeyBinding> WRAPPED = new java.util.IdentityHashMap<>();

    /** One legacy view per game key mapping (mods' own bindings answer as themselves). */
    private static synchronized KeyBinding mcc$wrap(net.minecraft.client.KeyMapping mapping) {
        for (KeyBinding k : KeyBinding.mcc$registered()) if (k.mcc$modern() == mapping) return k;
        return WRAPPED.computeIfAbsent(mapping, KeyBinding::mcc$vanilla);
    }
}
