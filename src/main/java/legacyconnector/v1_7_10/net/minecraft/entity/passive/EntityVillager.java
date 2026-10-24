package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VillagerBridge;

/** A modern villager as the 1.7.10 class: numeric professions (vanilla 0-4, mods' registered ids) and its trades. */
public class EntityVillager extends EntityAgeable {
    /** The trade chance curve vanilla uses when it builds trade offers (mods reflect it: FiskHeroes). */
    public static float adjustProbability(float probability) {
        float f = probability + 0.3F;
        return f > 0.9F ? 0.9F - (f - 0.9F) : f;
    }

    public EntityVillager(legacyconnector.v1_7_10.net.minecraft.world.World world) {
        super(world);
    }

    public EntityVillager(legacyconnector.v1_7_10.net.minecraft.world.World world, int profession) {
        super(world);
    }

    protected EntityVillager(LivingEntity modern) {
        super(modern);
    }

    private net.minecraft.world.entity.npc.Villager villager() {
        return mcc$entity instanceof net.minecraft.world.entity.npc.Villager v ? v : null;
    }

    public int getProfession() {
        var v = villager();
        return v == null ? 0 : VillagerBridge.idOf(v.getVillagerData().getProfession());
    }

    public void setProfession(int id) {
        var v = villager();
        if (v == null) return;
        var profession = VillagerBridge.professionOf(id);
        if (profession != null) v.setVillagerData(v.getVillagerData().setProfession(profession));
    }

    public MerchantRecipeList getRecipes(EntityPlayer player) {
        var v = villager();
        return v == null ? new MerchantRecipeList() : VillagerBridge.recipesOf(v);
    }

    public EntityPlayer getCustomer() {
        var v = villager();
        var p = v == null ? null : v.getTradingPlayer();
        return p == null ? null : (EntityPlayer) org.kylepitcock.legacyconnector.v1_7_10.bridge.EntityBridge.wrap(p);
    }

    public boolean isTrading() {
        var v = villager();
        return v != null && v.isTrading();
    }

    public boolean isPlayerStatusEnabled() {
        return true;
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return null;
    }

    public boolean isMating() {
        return false;
    }
}
