package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipeList;
import net.minecraft.world.entity.LivingEntity;
import org.kylepitcock.legacyconnector.v1_7_10.bridge.VillagerBridge;

/** A modern villager as the 1.7.10 class: numeric professions (vanilla 0-4, mods' registered ids) and its trades. */
public class EntityVillager extends EntityAgeable implements legacyconnector.v1_7_10.net.minecraft.entity.IMerchant {
    /** The trade chance curve vanilla uses when it builds trade offers (mods reflect it: FiskHeroes). */
    public static float adjustProbability(float probability) {
        float f = probability + 0.3F;
        return f > 0.9F ? 0.9F - (f - 0.9F) : f;
    }

    /** Item -> {@code Tuple(min, max)} count of the item a villager buys for one emerald. */
    public static final java.util.Map villagersSellingList = new java.util.HashMap();
    /** Item -> {@code Tuple(min, max)} emerald price (negative: items per emerald) of an item a villager sells. */
    public static final java.util.Map blacksmithSellingList = new java.util.HashMap();

    /** Adds, with {@code chance}, a trade where the villager buys {@code item} (count from {@link #villagersSellingList}) for an emerald. */
    public static void func_146091_a(MerchantRecipeList list, legacyconnector.v1_7_10.net.minecraft.item.Item item, java.util.Random random,
            float chance) {
        if (random.nextFloat() < chance) {
            list.add(new legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe(
                    new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, mcc$roll(villagersSellingList, item, random), 0),
                    legacyconnector.v1_7_10.net.minecraft.init.Items.emerald));
        }
    }

    /** Adds, with {@code chance}, a trade where the villager sells {@code item} for emeralds (price from {@link #blacksmithSellingList}). */
    public static void func_146089_b(MerchantRecipeList list, legacyconnector.v1_7_10.net.minecraft.item.Item item, java.util.Random random,
            float chance) {
        if (random.nextFloat() < chance) {
            int price = mcc$roll(blacksmithSellingList, item, random);
            var emerald = legacyconnector.v1_7_10.net.minecraft.init.Items.emerald;
            var buy = new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(emerald, price < 0 ? 1 : price, 0);
            var sell = new legacyconnector.v1_7_10.net.minecraft.item.ItemStack(item, price < 0 ? -price : 1, 0);
            list.add(new legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe(buy, sell));
        }
    }

    /** A count in the item's {@code Tuple(min, max)} range of {@code table} (1 if the item has none), as vanilla rolls it. */
    private static int mcc$roll(java.util.Map table, Object item, java.util.Random random) {
        if (!(table.get(item) instanceof legacyconnector.v1_7_10.net.minecraft.util.Tuple t)
                || !(t.getFirst() instanceof Integer min) || !(t.getSecond() instanceof Integer max)) {
            return 1;
        }
        return min >= max ? min : min + random.nextInt(max - min);
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

    public void setCustomer(EntityPlayer player) {
        var v = villager();
        if (v != null) v.setTradingPlayer(player == null ? null : player.mcc$player());
    }

    /** Client: the offers the server sent (a vanilla villager receives them itself). */
    public void setRecipes(MerchantRecipeList recipes) {
    }

    /** A trade was made: counts the use of the matching offer (the modern villager rewards it as usual). */
    public void useRecipe(legacyconnector.v1_7_10.net.minecraft.village.MerchantRecipe recipe) {
        var v = villager();
        if (v == null || recipe == null) return;
        recipe.incrementToolUses();
        var sell = legacyconnector.v1_7_10.net.minecraft.item.ItemStack.mcc$toModern(recipe.getItemToSell());
        for (var offer : v.getOffers()) {
            if (sell != null && net.minecraft.world.item.ItemStack.isSameItemSameComponents(offer.getResult(), sell)) {
                v.notifyTrade(offer);
                return;
            }
        }
    }

    public void verifySellingItem(legacyconnector.v1_7_10.net.minecraft.item.ItemStack stack) {
        var v = villager();
        if (v != null) v.notifyTradeUpdated(stack == null ? net.minecraft.world.item.ItemStack.EMPTY : stack.mcc$modern());
    }

    /** A child of the same (vanilla) kind. */
    @Override
    public EntityVillager createChild(EntityAgeable mate) {
        EntityVillager child = new EntityVillager(mcc$world());
        child.onSpawnWithEgg(null);
        return child;
    }

    public boolean isMating;
    public boolean isLookingForHome;

    public boolean isMating() {
        return isMating;
    }

    public void setMating(boolean mating) {
        this.isMating = mating;
    }

    /** The villager goes looking for a village house (the modern villager finds its own home). */
    public void setLookingForHome() {
        this.isLookingForHome = true;
    }
}
