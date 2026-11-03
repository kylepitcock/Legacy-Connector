package org.kylepitcock.legacyconnector.core;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeManager;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.OnDatapackSyncEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A recipe the client cannot decode (an ingredient or result that is an empty stack, e.g. from a legacy item that did
 * not translate) makes the whole recipe sync fail and disconnects every player. Such recipes are dropped before
 * recipes are sent, with a warning naming them.
 */
public final class RecipeSyncGuard {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Recipes");

    private RecipeSyncGuard() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener((OnDatapackSyncEvent e) -> clean(e.getPlayerList().getServer().getRecipeManager(),
                e.getPlayerList().getServer().registryAccess()));
    }

    private static void clean(RecipeManager manager, net.minecraft.core.RegistryAccess access) {
        List<RecipeHolder<?>> keep = new ArrayList<>();
        List<String> dropped = new ArrayList<>();
        for (RecipeHolder<?> holder : manager.getRecipes()) {
            if (sendable(holder, access)) keep.add(holder);
            else dropped.add(holder.id().toString());
        }
        if (dropped.isEmpty()) return;
        manager.replaceRecipes(keep);
        LOG.warn("Dropped {} recipe(s) that cannot be encoded (empty item stacks; clients cannot receive them): {}", dropped.size(), dropped);
    }

    /** A recipe is sendable when it encodes with its own serializer: the true test, and it keeps recipes of other mods
     * whose results are not item stacks (CustomMachinery machine recipes have an empty result item). */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private static boolean sendable(RecipeHolder<?> holder, net.minecraft.core.RegistryAccess access) {
        try {
            var recipe = holder.value();
            var buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), access);
            try {
                ((net.minecraft.network.codec.StreamCodec) recipe.getSerializer().streamCodec()).encode(buf, recipe);
            } finally {
                buf.release();
            }
            return true;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
