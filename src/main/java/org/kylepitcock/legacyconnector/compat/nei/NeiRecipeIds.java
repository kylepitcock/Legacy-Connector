package org.kylepitcock.legacyconnector.compat.nei;

import com.google.common.hash.Hashing;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.core.data.CompatComponents;

/** Synthetic IDs for NEI recipes, which do not have registry names of their own. */
final class NeiRecipeIds {
    private final String handlerName;
    private final Map<String, Integer> occurrences = new HashMap<>();

    NeiRecipeIds(Class<?> handlerClass) {
        handlerName = handlerClass.getName();
    }

    ResourceLocation next(List<NeiBridge.Slot> inputs, List<NeiBridge.Slot> outputs, List<NeiBridge.Slot> others) {
        var hash = Hashing.sha256().newHasher();
        hash.putInt(handlerName.length()).putUnencodedChars(handlerName);
        for (var slots : List.of(inputs, outputs, others)) {
            // Slot coordinates and roles matter; the order of slots and alternatives does not.
            var keys = slots.stream().map(NeiRecipeIds::slotKey).sorted().toList();
            hash.putInt(keys.size());
            for (String key : keys) hash.putInt(key.length()).putUnencodedChars(key);
        }
        String fingerprint = hash.hash().toString();
        // A handler can emit multiple rows with identical exposed slots, or no item slots at all.
        int occurrence = occurrences.merge(fingerprint, 1, Integer::sum);
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "nei/" + fingerprint + "/" + occurrence);
    }

    private static String slotKey(NeiBridge.Slot slot) {
        StringBuilder key = new StringBuilder().append(slot.x()).append(',').append(slot.y()).append(';');
        for (String stack : slot.stacks().stream().map(NeiRecipeIds::stackKey).distinct().sorted().toList()) {
            key.append(stack.length()).append(':').append(stack);
        }
        return key.toString();
    }

    private static String stackKey(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        // CompoundTag's SNBT representation sorts keys, including in nested compounds.
        return BuiltInRegistries.ITEM.getKey(stack.getItem()) + "|" + stack.getCount() + "|"
                + stack.getOrDefault(CompatComponents.legacyMeta(), 0) + "|" + stack.getDamageValue() + "|"
                + (data == null || data.isEmpty() ? "" : data.copyTag());
    }
}
