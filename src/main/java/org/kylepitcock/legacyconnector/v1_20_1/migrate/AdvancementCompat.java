package org.kylepitcock.legacyconnector.v1_20_1.migrate;

import java.util.Map;
import javax.annotation.Nullable;
import net.minecraft.advancements.Advancement;
import net.minecraft.advancements.AdvancementHolder;
import net.minecraft.advancements.AdvancementNode;
import net.minecraft.advancements.Criterion;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.client.multiplayer.ClientAdvancements;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

/**
 * 1.20.1's {@code Advancement} became the data record {@code Advancement} plus the {@code AdvancementHolder} that pairs it with its id; the class rename maps
 * the old class to the holder, and what the old class answered itself is read from the record here.
 */
public final class AdvancementCompat {
    private AdvancementCompat() {}

    public static LegacyAdvancementList clientList(ClientAdvancements advancements) {
        return new LegacyAdvancementList(advancements.getTree());
    }

    @Nullable
    public static DisplayInfo getDisplay(AdvancementHolder holder) {
        return holder.value().display().orElse(null);
    }

    public static Map<String, Criterion<?>> getCriteria(AdvancementHolder holder) {
        return holder.value().criteria();
    }

    public static Component getChatComponent(AdvancementHolder holder) {
        return Advancement.name(holder);
    }

    /** The parent advancement, looked up where the holder lives: the connected client's tree, else the server's advancement manager. */
    @Nullable
    public static AdvancementHolder getParent(AdvancementHolder holder) {
        ResourceLocation parent = holder.value().parent().orElse(null);
        if (parent == null) return null;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server != null) return server.getAdvancements().get(parent);
        return Client.get(parent);
    }

    /** The root of the tree this advancement is in (the node's root), or itself. */
    public static AdvancementHolder getRoot(AdvancementHolder holder) {
        AdvancementHolder at = holder;
        for (int guard = 0; guard < 64; guard++) {
            AdvancementHolder parent = getParent(at);
            if (parent == null) return at;
            at = parent;
        }
        return at;
    }

    /** Kept apart so the dedicated server never loads the client classes. */
    private static final class Client {
        @Nullable
        static AdvancementHolder get(ResourceLocation id) {
            var connection = net.minecraft.client.Minecraft.getInstance().getConnection();
            if (connection == null) return null;
            AdvancementNode node = connection.getAdvancements().getTree().get(id);
            return node == null ? null : node.holder();
        }
    }
}
