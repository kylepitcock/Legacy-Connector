package legacyconnector.v1_7_10.cpw.mods.fml.common.registry;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Random;
import legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityVillager;
import legacyconnector.v1_7_10.net.minecraft.util.ResourceLocation;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VillagerBridge;

/**
 * Forge's villager registry: mods register profession ids with skins and trade handlers (which become modern
 * villager professions and trades). Village creation handlers are recorded but the modern jigsaw villages do not
 * build legacy village components.
 */
public class VillagerRegistry {
    private static final VillagerRegistry INSTANCE = new VillagerRegistry();

    public interface IVillageCreationHandler {
    }

    public interface IVillageTradeHandler {
        void manipulateTradesForVillager(EntityVillager villager, MerchantRecipeList recipeList, Random random);
    }

    private VillagerRegistry() {}

    public static VillagerRegistry instance() {
        return INSTANCE;
    }

    public void registerVillagerId(int id) {
        VillagerBridge.registerId(id);
    }

    public void registerVillagerSkin(int id, ResourceLocation skin) {
        VillagerBridge.registerSkin(id, skin.mcc$modern());
    }

    public static ResourceLocation getVillagerSkin(int id, ResourceLocation defaultSkin) {
        var skin = VillagerBridge.skin(id);
        return skin == null ? defaultSkin : new ResourceLocation(skin.getNamespace(), skin.getPath());
    }

    public static Collection<Integer> getRegisteredVillagers() {
        return VillagerBridge.registeredIds();
    }

    public void registerVillageCreationHandler(IVillageCreationHandler handler) {
        VillagerBridge.registerCreationHandler(handler);
    }

    public void registerVillageTradeHandler(int villagerId, IVillageTradeHandler handler) {
        VillagerBridge.registerTradeHandler(villagerId, handler);
    }

    public static void addExtraVillageComponents(ArrayList components, Random random, int size) {
    }

    public static void manageVillagerTrades(MerchantRecipeList list, EntityVillager villager, int villagerType, Random random) {
        VillagerBridge.applyHandlers(villagerType, villager, list, random);
    }
}
