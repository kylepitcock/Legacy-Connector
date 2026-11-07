package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.net.minecraft.stats.Achievement;
import legacyconnector.v1_7_10.net.minecraft.stats.AchievementList;
import legacyconnector.v1_7_10.net.minecraft.util.ChatComponentTranslation;
import legacyconnector.v1_7_10.net.minecraftforge.common.AchievementPage;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.Ids;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The achievements mods add (1.7.10 has no advancements), as generated data pack advancements: the modern advancements screen shows them. Each
 * achievement page is a tab (a generated root advancement named after the page); achievements of no page, and those hung off vanilla
 * achievements, go to a tab of their own. Parent chains are kept; the criterion is {@code impossible} and awarded from
 * {@code EntityPlayer.addStat} when the legacy code earns the achievement ({@link #award}), so the advancement and the legacy record agree. The
 * legacy toast stays (the advancements neither toast nor announce).
 */
public final class Achievements1710 {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Achievements1710");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String CRITERION = "unlocked";
    private static final ResourceLocation OTHER_ROOT = ResourceLocation.fromNamespaceAndPath("legacyconnector", "achievements/legacy");

    private Achievements1710() {}

    /** The advancement of a mod achievement. */
    public static ResourceLocation idOf(Achievement a) {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "achievements/" + Ids.normalizePath(a.statId));
    }

    private static ResourceLocation pageRoot(AchievementPage page) {
        return ResourceLocation.fromNamespaceAndPath("legacyconnector", "achievements/page_" + Ids.normalizePath(page.getName()).replace('/', '_'));
    }

    private static Set<Achievement> vanilla() {
        Set<Achievement> out = new HashSet<>();
        for (var f : AchievementList.class.getFields()) {
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != Achievement.class) continue;
            try {
                out.add((Achievement) f.get(null));
            } catch (IllegalAccessException e) {
                // not readable: treated as a mod achievement
            }
        }
        return out;
    }

    public static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.SERVER_DATA) return;
        Set<Achievement> vanilla = vanilla();
        List<Achievement> mods = AchievementList.achievementList.stream().filter(a -> !vanilla.contains(a)).toList();
        if (mods.isEmpty()) return;
        Map<Achievement, AchievementPage> pageOf = new HashMap<>();
        for (AchievementPage page : AchievementPage.getAchievementPages()) {
            for (Achievement a : page.getAchievements()) pageOf.putIfAbsent(a, page);
        }
        Set<ResourceLocation> roots = new HashSet<>();
        Map<ResourceLocation, AchievementPage> rootPages = new HashMap<>();
        int count = 0;
        for (Achievement a : mods) {
            AchievementPage page = pageOf.get(a);
            ResourceLocation root = page == null ? OTHER_ROOT : pageRoot(page);
            ResourceLocation parent = a.parentAchievement == null || vanilla.contains(a.parentAchievement) ? root : idOf(a.parentAchievement);
            if (parent.equals(root)) {
                roots.add(root);
                if (page != null) rootPages.put(root, page);
            }
            JsonObject json = advancement(a, parent);
            byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
            ResourceLocation id = idOf(a);
            out.put("data/" + id.getNamespace() + "/advancement/" + id.getPath() + ".json", () -> bytes);
            count++;
        }
        for (ResourceLocation root : roots) {
            AchievementPage page = rootPages.get(root);
            Achievement first = page == null || page.getAchievements().isEmpty() ? mods.get(0) : page.getAchievements().get(0);
            JsonObject json = new JsonObject();
            JsonObject display = display(first);
            display.addProperty("title", page == null ? "Legacy achievements" : page.getName());
            display.addProperty("description", page == null ? "Achievements of legacy mods" : "Achievements of " + page.getName());
            display.addProperty("background", "minecraft:textures/gui/advancements/backgrounds/stone.png");
            json.add("display", display);
            JsonObject criteria = new JsonObject();
            JsonObject auto = new JsonObject();
            auto.addProperty("trigger", "minecraft:tick");
            criteria.add("auto", auto);
            json.add("criteria", criteria);
            byte[] bytes = GSON.toJson(json).getBytes(StandardCharsets.UTF_8);
            out.put("data/" + root.getNamespace() + "/advancement/" + root.getPath() + ".json", () -> bytes);
        }
        LOG.info("1.7.10 achievements: {} advancement(s) in {} tab(s) generated", count, roots.size());
    }

    private static JsonObject display(Achievement a) {
        var legacy = a.theItemStack;
        var stack = legacy == null || legacy.getItem() == null ? net.minecraft.world.item.ItemStack.EMPTY
                : ItemBridge.toModern(legacy.getItem(), 1, legacy.getItemDamage());
        String icon = stack.isEmpty() ? "minecraft:barrier" : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        JsonObject iconJson = new JsonObject();
        iconJson.addProperty("id", icon);
        JsonObject d = new JsonObject();
        d.add("icon", iconJson);
        d.addProperty("frame", a.getSpecial() ? "goal" : "task");
        d.addProperty("show_toast", false);
        d.addProperty("announce_to_chat", false);
        return d;
    }

    private static JsonObject advancement(Achievement a, ResourceLocation parent) {
        JsonObject display = display(a);
        String key = a.getStatName() instanceof ChatComponentTranslation t ? t.getKey() : null;
        JsonObject title = new JsonObject();
        if (key != null) title.addProperty("translate", key);
        else title.addProperty("text", a.statId);
        JsonObject desc = new JsonObject();
        desc.addProperty("translate", a.mcc$descriptionKey());
        display.add("title", title);
        display.add("description", desc);
        JsonObject json = new JsonObject();
        json.addProperty("parent", parent.toString());
        json.add("display", display);
        JsonObject criteria = new JsonObject();
        JsonObject c = new JsonObject();
        c.addProperty("trigger", "minecraft:impossible");
        criteria.add(CRITERION, c);
        json.add("criteria", criteria);
        JsonArray req = new JsonArray();
        JsonArray group = new JsonArray();
        group.add(CRITERION);
        req.add(group);
        json.add("requirements", req);
        return json;
    }

    /** Achievements recorded before the advancements existed (or earned with the advancement removed) are awarded when the player joins. */
    public static void register() {
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent e) -> {
            if (!(e.getEntity() instanceof ServerPlayer sp)) return;
            var tag = sp.getPersistentData();
            if (!tag.contains("legacyconnector_achievements", 9)) return;
            var earned = new HashSet<String>();
            for (var t : tag.getList("legacyconnector_achievements", 8)) earned.add(t.getAsString());
            for (Achievement a : AchievementList.achievementList) if (earned.contains(a.statId)) award(sp, a);
        });
    }

    /** The player earned the achievement in legacy code: the advancement of it is awarded too. */
    public static void award(ServerPlayer sp, Achievement a) {
        try {
            var server = sp.getServer();
            if (server == null) return;
            var holder = server.getAdvancements().get(idOf(a));
            if (holder != null) sp.getAdvancements().award(holder, CRITERION);
        } catch (RuntimeException e) {
            LOG.debug("Cannot award the advancement of {}: {}", a.statId, e.toString());
        }
    }
}
