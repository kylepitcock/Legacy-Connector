package legacyconnector.v1_6_4.facade.net.minecraft.entity;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem;
import legacyconnector.v1_7_10.net.minecraft.item.Item;

/** Facade of the 1.7.10 shim's {@code Entity} for 1.6.4 mods: the members that took item ids. See {@code Hierarchy.facadeOf}. Never instantiated. */
public final class Entity {
    private Entity() {}

    /** {@code dropItemWithOffset(itemId, count, yOffset)}: as {@link #dropItem}, spawned {@code yOffset} above the entity's feet. */
    public static EntityItem dropItemWithOffset(legacyconnector.v1_7_10.net.minecraft.entity.Entity self, int itemId, int count, float yOffset) {
        Item item = Ids.item(itemId);
        return item == null ? null : self.dropItemWithOffset(item, count, yOffset);
    }

    /** {@code dropItem(itemId, count)}: drops {@code count} of the item with that id at the entity; null for an unused id or air, as in 1.6.4. */
    public static EntityItem dropItem(legacyconnector.v1_7_10.net.minecraft.entity.Entity self, int itemId, int count) {
        Item item = Ids.item(itemId);
        return item == null ? null : self.dropItem(item, count);
    }

    /** 1.6.4 {@code Entity.addToPlayerScore(killed, score)}: only living entities and players keep a score (1.7.10 moved it down). */
    public static void addToPlayerScore(legacyconnector.v1_7_10.net.minecraft.entity.Entity self, legacyconnector.v1_7_10.net.minecraft.entity.Entity killed, int score) {
        if (self instanceof legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase living) living.addToPlayerScore(killed, score);
    }

    private static final java.util.Map<legacyconnector.v1_7_10.net.minecraft.entity.Entity, String[]> CLOAK_AND_SKIN = java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());

    private static String legacyUrl(legacyconnector.v1_7_10.net.minecraft.entity.Entity self, String folder) {
        if (!(self instanceof legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer)) return null;
        String name = self.getCommandSenderName();
        return name == null || name.isEmpty() ? null : "http://skins.minecraft.net/" + folder + "/" + name + ".png";
    }

    /**
     * 1.4.7 and 1.5.2 {@code cloakUrl} of a player: where the cape downloaded from ({@code skins.minecraft.net/MinecraftCloaks/<name>.png}, which the layer's
     * downloader answers with the cape of the named profile). Null for an entity that is not a player; a mod may set it to its own URL.
     */
    public static String mcc$get$cloakUrl(legacyconnector.v1_7_10.net.minecraft.entity.Entity self) {
        String[] set = CLOAK_AND_SKIN.get(self);
        return set != null && set[0] != null ? set[0] : legacyUrl(self, "MinecraftCloaks");
    }

    public static void mcc$set$cloakUrl(legacyconnector.v1_7_10.net.minecraft.entity.Entity self, String url) {
        CLOAK_AND_SKIN.computeIfAbsent(self, k -> new String[2])[0] = url;
    }

    /** 1.4.7 and 1.5.2 {@code skinUrl} of a player, like {@link #mcc$get$cloakUrl} for the skin. */
    public static String mcc$get$skinUrl(legacyconnector.v1_7_10.net.minecraft.entity.Entity self) {
        String[] set = CLOAK_AND_SKIN.get(self);
        return set != null && set[1] != null ? set[1] : legacyUrl(self, "MinecraftSkins");
    }

    public static void mcc$set$skinUrl(legacyconnector.v1_7_10.net.minecraft.entity.Entity self, String url) {
        CLOAK_AND_SKIN.computeIfAbsent(self, k -> new String[2])[1] = url;
    }

    /**
     * 1.4.7 and 1.5.2 {@code updateCloak()}: the player's cape check (it set the cloak URL and, on the client, asked the render engine for the image). A no-op
     * here: the cape is the native player's, shown by the host; {@link #mcc$get$cloakUrl} gives mods the URL they read.
     */
    public static void updateCloak(legacyconnector.v1_7_10.net.minecraft.entity.Entity self) {
    }
}
