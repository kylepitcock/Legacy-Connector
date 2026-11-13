package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.kylepitcock.legacyconnector.vb1_7_3.NativeStackItemB173;

/** The texture of the modern item an icon cell of {@link NativeStackItemB173} stands for: what an original renderer draws for a foreign item. */
public final class NativeIconsClientB173 {
    private static final Map<Integer, ResourceLocation> CACHE = new ConcurrentHashMap<>();
    private NativeIconsClientB173() {}

    /** The texture file of the item in the cell, or null when the cell holds none. */
    public static ResourceLocation sprite(int slot) {
        int registryId = NativeStackItemB173.itemOfSlot(slot);
        if (registryId < 0) return null;
        return CACHE.computeIfAbsent(registryId, id -> {
            var item = BuiltInRegistries.ITEM.byId(id);
            var model = Minecraft.getInstance().getItemRenderer().getModel(new ItemStack(item), null, null, 0);
            ResourceLocation name = model.getParticleIcon().contents().name();
            return name.withPath("textures/" + name.getPath() + ".png");
        });
    }
}
