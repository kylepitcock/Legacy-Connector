package org.kylepitcock.legacyconnector.v1_7_10.bridge;

import com.google.common.collect.ImmutableSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;
import org.kylepitcock.legacyconnector.core.registry.LegacyRegistrar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Forge's numeric villager professions as modern ones. Ids 0-4 are the vanilla farmer, librarian, priest, smith and
 * butcher; every id a mod registers becomes a {@code legacyconnector:villager_<id>} profession with the mod's skin as its
 * clothes layer and its trade handlers' trades; some freshly spawned unemployed villagers take such a profession (the
 * modern villages have no hook for legacy village components).
 */
public final class VillagerBridge {
    private static final Logger LOG = LoggerFactory.getLogger("LegacyConnector/Villagers");
    private static final Map<Integer, VillagerProfession> CUSTOM = new ConcurrentHashMap<>();
    private static final Map<Integer, ResourceLocation> SKINS = new ConcurrentHashMap<>();
    private static final Map<Integer, List<VillagerRegistry.IVillageTradeHandler>> HANDLERS = new ConcurrentHashMap<>();
    private static final List<Object> CREATION_HANDLERS = new ArrayList<>();
    /** A share of unemployed villagers become a modded profession when they first appear. */
    private static final float ADOPTION_CHANCE = 0.3F;
    private static boolean registered;

    private VillagerBridge() {}

    public static void register() {
        if (registered) return;
        registered = true;
        NeoForge.EVENT_BUS.addListener(VillagerBridge::onTrades);
        NeoForge.EVENT_BUS.addListener(VillagerBridge::onJoin);
        GeneratedPack.addContributor(VillagerBridge::contribute);
    }

    // ------------------------------------------------------------------ registration

    public static void registerId(int id) {
        if (id >= 0 && id <= 4) return;
        CUSTOM.computeIfAbsent(id, i -> {
            ResourceLocation key = ResourceLocation.fromNamespaceAndPath("legacyconnector", "villager_" + i);
            VillagerProfession profession = new VillagerProfession("legacyconnector.villager_" + i, holder -> false, holder -> false,
                    ImmutableSet.of(), ImmutableSet.of(), null);
            try {
                return LegacyRegistrar.register(BuiltInRegistries.VILLAGER_PROFESSION, key, profession);
            } catch (RuntimeException e) {
                LOG.warn("Could not register villager profession {}: {}", i, e.toString());
                return null;
            }
        });
        register();
    }

    public static void registerSkin(int id, ResourceLocation skin) {
        SKINS.put(id, skin);
    }

    public static ResourceLocation skin(int id) {
        return SKINS.get(id);
    }

    public static Collection<Integer> registeredIds() {
        List<Integer> ids = new ArrayList<>(List.of(0, 1, 2, 3, 4));
        ids.addAll(CUSTOM.keySet());
        return ids;
    }

    public static void registerTradeHandler(int id, VillagerRegistry.IVillageTradeHandler handler) {
        HANDLERS.computeIfAbsent(id, i -> new ArrayList<>()).add(handler);
        register();
    }

    public static void registerCreationHandler(Object handler) {
        CREATION_HANDLERS.add(handler);
        LOG.debug("Village creation handler {} recorded; modern villages do not build legacy components", handler.getClass().getName());
    }

    // ------------------------------------------------------------------ profession ids

    public static VillagerProfession professionOf(int id) {
        return switch (id) {
            case 0 -> VillagerProfession.FARMER;
            case 1 -> VillagerProfession.LIBRARIAN;
            case 2 -> VillagerProfession.CLERIC;
            case 3 -> VillagerProfession.ARMORER;
            case 4 -> VillagerProfession.BUTCHER;
            default -> CUSTOM.get(id);
        };
    }

    public static int idOf(VillagerProfession profession) {
        if (profession == VillagerProfession.FARMER) return 0;
        if (profession == VillagerProfession.LIBRARIAN) return 1;
        if (profession == VillagerProfession.CLERIC) return 2;
        if (profession == VillagerProfession.ARMORER || profession == VillagerProfession.WEAPONSMITH || profession == VillagerProfession.TOOLSMITH
                || profession == VillagerProfession.MASON || profession == VillagerProfession.LEATHERWORKER) {
            return 3;
        }
        if (profession == VillagerProfession.BUTCHER || profession == VillagerProfession.FISHERMAN || profession == VillagerProfession.SHEPHERD) return 4;
        for (var e : CUSTOM.entrySet()) if (e.getValue() == profession) return e.getKey();
        return 0;
    }

    // ------------------------------------------------------------------ trades

    public static void applyHandlers(int id, EntityVillager villager, MerchantRecipeList list, Random random) {
        for (var handler : HANDLERS.getOrDefault(id, List.of())) {
            try {
                handler.manipulateTradesForVillager(villager, list, random);
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("Trade handler {} failed: {}", handler.getClass().getName(), e.toString());
            }
        }
    }

    /** A villager without a world: trade handlers only read its profession and random. */
    private static EntityVillager placeholder() {
        try {
            var f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            return (EntityVillager) ((sun.misc.Unsafe) f.get(null)).allocateInstance(EntityBridge.VillagerView.class);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }

    /** The modern offers of a villager as 1.7.10 trade recipes. */
    public static MerchantRecipeList recipesOf(Villager villager) {
        MerchantRecipeList out = new MerchantRecipeList();
        for (MerchantOffer offer : villager.getOffers()) {
            ItemStack buy = ItemStack.mcc$wrap(offer.getBaseCostA());
            ItemStack second = offer.getCostB().isEmpty() ? null : ItemStack.mcc$wrap(offer.getCostB());
            MerchantRecipe r = new MerchantRecipe(buy, second, ItemStack.mcc$wrap(offer.getResult()));
            for (int i = 0; i < offer.getUses(); i++) r.incrementToolUses();
            out.add(r);
        }
        return out;
    }

    private static void onTrades(VillagerTradesEvent event) {
        int id = idOf(event.getType());
        if (event.getType() != VillagerProfession.NONE && id == 0 && event.getType() != VillagerProfession.FARMER) return;
        List<VillagerRegistry.IVillageTradeHandler> handlers = HANDLERS.get(id);
        if (handlers == null || handlers.isEmpty()) return;
        MerchantRecipeList list = new MerchantRecipeList();
        applyHandlers(id, placeholder(), list, new Random(id * 31L + 7));
        List<VillagerTrades.ItemListing> listings = event.getTrades().computeIfAbsent(1, k -> new ArrayList<>());
        for (Object o : list) {
            MerchantRecipe recipe = (MerchantRecipe) o;
            listings.add(listing(recipe));
        }
        if (!list.isEmpty()) LOG.info("Villager profession {}: {} legacy trade(s)", id, list.size());
    }

    private static VillagerTrades.ItemListing listing(MerchantRecipe recipe) {
        return (trader, random) -> {
            var buy = ItemStack.mcc$toModern(recipe.getItemToBuy());
            var sell = ItemStack.mcc$toModern(recipe.getItemToSell());
            if (buy == null || sell == null || buy.isEmpty() || sell.isEmpty()) return null;
            Optional<ItemCost> second = Optional.empty();
            if (recipe.hasSecondItemToBuy()) {
                var s = ItemStack.mcc$toModern(recipe.getSecondItemToBuy());
                if (s != null && !s.isEmpty()) second = Optional.of(new ItemCost(s.getItem(), s.getCount()));
            }
            return new MerchantOffer(new ItemCost(buy.getItem(), buy.getCount()), second, sell, 0, Math.max(1, recipe.getMaxTradeUses()), 5, 0.05F);
        };
    }

    // ------------------------------------------------------------------ professions of new villagers

    private static void onJoin(EntityJoinLevelEvent event) {
        if (CUSTOM.isEmpty() || event.loadedFromDisk() || event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Villager villager) || villager.isBaby()) return;
        if (villager.getVillagerData().getProfession() != VillagerProfession.NONE) return;
        if (villager.getRandom().nextFloat() >= ADOPTION_CHANCE) return;
        List<VillagerProfession> choices = new ArrayList<>();
        for (var p : CUSTOM.values()) if (p != null) choices.add(p);
        if (choices.isEmpty()) return;
        var profession = choices.get(villager.getRandom().nextInt(choices.size()));
        villager.setVillagerData(villager.getVillagerData().setProfession(profession));
        // A villager without a job site and without experience loses its profession again.
        villager.setVillagerXp(1);
    }

    // ------------------------------------------------------------------ skins

    private static void contribute(PackType type, Map<String, Supplier<byte[]>> out) {
        if (type != PackType.CLIENT_RESOURCES) return;
        for (var e : SKINS.entrySet()) {
            if (!CUSTOM.containsKey(e.getKey())) continue;
            ResourceLocation from = e.getValue();
            String target = "assets/legacyconnector/textures/entity/villager/profession/villager_" + e.getKey() + ".png";
            out.put(target, () -> read(from));
        }
    }

    private static byte[] read(ResourceLocation location) {
        try {
            var found = net.minecraft.client.Minecraft.getInstance().getResourceManager().getResource(location);
            if (found.isPresent()) {
                try (var in = found.get().open()) {
                    return in.readAllBytes();
                }
            }
        } catch (Exception | LinkageError e) {
            LOG.debug("Villager skin {} not readable yet: {}", location, e.toString());
        }
        return new byte[0];
    }
}
