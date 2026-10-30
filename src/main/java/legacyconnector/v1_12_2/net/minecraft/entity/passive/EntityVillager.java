package legacyconnector.v1_12_2.net.minecraft.entity.passive;

import static org.kylepitcock.legacyconnector.v1_12_2.entity.Sounds1122.legacy;

import java.util.Random;
import legacyconnector.v1_12_2.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_12_2.net.minecraft.entity.IMerchant;
import legacyconnector.v1_12_2.net.minecraft.entity.INpc;
import legacyconnector.v1_12_2.net.minecraft.entity.SharedMonsterAttributes;
import legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer;
import legacyconnector.v1_12_2.net.minecraft.inventory.InventoryBasic;
import legacyconnector.v1_12_2.net.minecraft.item.Item;
import legacyconnector.v1_12_2.net.minecraft.item.ItemStack;
import legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagCompound;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataParameter;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.DataSerializers;
import legacyconnector.v1_12_2.net.minecraft.network.datasync.EntityDataManager;
import legacyconnector.v1_12_2.net.minecraft.util.DamageSource;
import legacyconnector.v1_12_2.net.minecraft.util.EnumHand;
import legacyconnector.v1_12_2.net.minecraft.util.SoundEvent;
import legacyconnector.v1_12_2.net.minecraft.util.math.BlockPos;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipe;
import legacyconnector.v1_12_2.net.minecraft.village.MerchantRecipeList;
import legacyconnector.v1_12_2.net.minecraft.world.World;
import net.minecraft.sounds.SoundEvents;

/**
 * 1.12.2 villager and its trade building blocks ({@link PriceInfo}, {@link ITradeList} and the vanilla trade kinds). A view of a modern
 * villager reads its profession and offers and trades through the modern trading screen; a mod's subclass keeps its own profession
 * and trade list and opens the trading screen on itself.
 */
public class EntityVillager extends EntityAgeable implements INpc, IMerchant {
    private static final DataParameter<Integer> PROFESSION = EntityDataManager.createKey(EntityVillager.class, DataSerializers.VARINT);
    private boolean isMating;
    private boolean isPlaying;
    private EntityPlayer buyingPlayer;
    protected MerchantRecipeList buyingList;
    private final InventoryBasic villagerInventory = new InventoryBasic("Items", false, 8);
    private int careerId;
    private int careerLevel;

    public EntityVillager(World world) {
        this(world, 0);
    }

    public EntityVillager(World world, int professionId) {
        super(world);
        // a view of a villager already in the world keeps its profession
        if (mcc$villager() == null || !mcc$entity.isAddedToLevel()) setProfession(professionId);
        setSize(0.6F, 1.95F);
    }

    protected EntityVillager(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    protected net.minecraft.world.entity.npc.Villager mcc$villager() {
        return mcc$entity instanceof net.minecraft.world.entity.npc.Villager v ? v : null;
    }

    // ------------------------------------------------------------------ professions (1.12 ids: farmer, librarian, priest, smith, butcher, nitwit)

    public static net.minecraft.world.entity.npc.VillagerProfession mcc$modernProfession(int id) {
        return switch (id) {
            case 0 -> net.minecraft.world.entity.npc.VillagerProfession.FARMER;
            case 1 -> net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN;
            case 2 -> net.minecraft.world.entity.npc.VillagerProfession.CLERIC;
            case 3 -> net.minecraft.world.entity.npc.VillagerProfession.ARMORER;
            case 4 -> net.minecraft.world.entity.npc.VillagerProfession.BUTCHER;
            case 5 -> net.minecraft.world.entity.npc.VillagerProfession.NITWIT;
            default -> net.minecraft.world.entity.npc.VillagerProfession.NONE;
        };
    }

    public static int mcc$legacyProfession(net.minecraft.world.entity.npc.VillagerProfession p) {
        if (p == net.minecraft.world.entity.npc.VillagerProfession.FARMER || p == net.minecraft.world.entity.npc.VillagerProfession.FISHERMAN
                || p == net.minecraft.world.entity.npc.VillagerProfession.SHEPHERD || p == net.minecraft.world.entity.npc.VillagerProfession.FLETCHER) return 0;
        if (p == net.minecraft.world.entity.npc.VillagerProfession.LIBRARIAN || p == net.minecraft.world.entity.npc.VillagerProfession.CARTOGRAPHER) return 1;
        if (p == net.minecraft.world.entity.npc.VillagerProfession.CLERIC) return 2;
        if (p == net.minecraft.world.entity.npc.VillagerProfession.ARMORER || p == net.minecraft.world.entity.npc.VillagerProfession.WEAPONSMITH
                || p == net.minecraft.world.entity.npc.VillagerProfession.TOOLSMITH) return 3;
        if (p == net.minecraft.world.entity.npc.VillagerProfession.BUTCHER || p == net.minecraft.world.entity.npc.VillagerProfession.LEATHERWORKER) return 4;
        if (p == net.minecraft.world.entity.npc.VillagerProfession.NITWIT) return 5;
        return 0;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataManager.register(PROFESSION, 0);
    }

    public void setProfession(int professionId) {
        var v = mcc$villager();
        if (v != null) {
            v.setVillagerData(v.getVillagerData().setProfession(mcc$modernProfession(professionId)));
            return;
        }
        dataManager.set(PROFESSION, professionId);
    }

    public int getProfession() {
        var v = mcc$villager();
        if (v != null) return mcc$legacyProfession(v.getVillagerData().getProfession());
        Integer p = dataManager.get(PROFESSION);
        return p == null ? 0 : Math.max(p % 6, 0);
    }

    private legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession mcc$forgeProfession;

    /** Forge: the profession as a registry entry (a vanilla one for vanilla ids). */
    public legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession getProfessionForge() {
        if (mcc$forgeProfession != null && mcc$villager() == null) return mcc$forgeProfession;
        var p = legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry.getById(getProfession());
        return p != null ? p : mcc$forgeProfession;
    }

    public void setProfession(legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry.VillagerProfession profession) {
        if (profession == null) return;
        mcc$forgeProfession = profession;
        int id = legacyconnector.v1_12_2.net.minecraftforge.fml.common.registry.VillagerRegistry.getId(profession);
        if (id >= 0) setProfession(id);
    }

    /** Adds the trades of the next career level (1.12's populateBuyingList; a modern villager levels up by itself). */
    protected void populateBuyingList() {
        if (mcc$villager() != null) return;
        var profession = getProfessionForge();
        if (profession == null) return;
        if (careerId == 0) careerId = profession.getRandomCareer(rand) + 1;
        var career = profession.getCareer(careerId - 1);
        if (career == null) return;
        if (buyingList == null) buyingList = new MerchantRecipeList();
        var trades = career.getTrades(careerLevel);
        ++careerLevel;
        if (trades != null) for (ITradeList t : trades) t.addMerchantRecipe(this, buyingList, rand);
    }

    // ------------------------------------------------------------------ AI, attributes, state

    @Override
    protected void initEntityAI() {
        tasks.addTask(0, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAISwimming(this));
        tasks.addTask(1, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIAvoidEntity(this,
                legacyconnector.v1_12_2.net.minecraft.entity.monster.EntityZombie.class, 8.0F, 0.6D, 0.6D));
        tasks.addTask(2, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMoveIndoors(this));
        tasks.addTask(3, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIRestrictOpenDoor(this));
        tasks.addTask(4, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIOpenDoor(this, true));
        tasks.addTask(5, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIMoveTowardsRestriction(this, 0.6D));
        tasks.addTask(9, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest2(this, EntityPlayer.class, 3.0F, 1.0F));
        tasks.addTask(9, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWanderAvoidWater(this, 0.6D));
        tasks.addTask(10, new legacyconnector.v1_12_2.net.minecraft.entity.ai.EntityAIWatchClosest(this, legacyconnector.v1_12_2.net.minecraft.entity.EntityLiving.class, 8.0F));
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getEntityAttribute(SharedMonsterAttributes.MOVEMENT_SPEED).setBaseValue(0.5D);
    }

    public boolean isMating() {
        return isMating;
    }

    public void setMating(boolean mating) {
        isMating = mating;
    }

    public boolean isPlaying() {
        return isPlaying;
    }

    public void setPlaying(boolean playing) {
        isPlaying = playing;
    }

    /**
     * Forge: the villager's spawn set-up. With {@code forgeCheck} a new villager takes a random profession first; then the living
     * set-up and its trades.
     */
    public legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData finalizeMobSpawn(legacyconnector.v1_12_2.net.minecraft.world.DifficultyInstance difficulty,
            legacyconnector.v1_12_2.net.minecraft.entity.IEntityLivingData data, boolean forgeCheck) {
        data = super.onInitialSpawn(difficulty, data);
        if (forgeCheck && mcc$gameVillager() == null) setProfession(getRNG().nextInt(6));
        if (mcc$gameVillager() == null) {
            try {
                populateBuyingList();
            } catch (RuntimeException | LinkageError e) {
                // trades are made again when a player opens them
            }
        }
        return data;
    }

    public InventoryBasic getVillagerInventory() {
        return villagerInventory;
    }

    // ------------------------------------------------------------------ food, farming and breeding (1.12 rules; a game villager answers with its own)

    private boolean mcc$willingToMate;
    /** Phantom field: {@link #mcc$get$isWillingToMate}. */
    @Deprecated
    private boolean isWillingToMate;
    /** Ticks until a trade that sold out is restocked (with {@link #needsInitilization}, a new trade tier is added then). */
    private int timeUntilReset;
    private boolean needsInitilization;
    /** Emeralds the villager earned from trades. */
    private int wealth;

    /** isWillingToMate: the breeding readiness (the modern villager's for vanilla villagers). */
    public static boolean mcc$get$isWillingToMate(EntityVillager v) {
        return v.getIsWillingToMate(false);
    }

    public static void mcc$set$isWillingToMate(EntityVillager v, boolean value) {
        v.setIsWillingToMate(value);
    }

    private net.minecraft.world.entity.npc.Villager mcc$gameVillager() {
        return mcc$hosted() ? null : mcc$villager();
    }

    private static Object mcc$modernItem(ItemStack stack) {
        return stack == null || stack.isEmpty() ? null : stack.getItem().mcc$modern;
    }

    /** Whether the inventory holds bread or crops worth {@code multiplier} meals (farmers also count wheat). */
    private boolean mcc$hasEnoughItems(int multiplier) {
        boolean farmer = getProfession() == 0;
        for (int i = 0; i < villagerInventory.getSizeInventory(); ++i) {
            ItemStack stack = villagerInventory.getStackInSlot(i);
            Object item = mcc$modernItem(stack);
            if (item == null) continue;
            int n = stack.getCount();
            if (item == net.minecraft.world.item.Items.BREAD && n >= 3 * multiplier) return true;
            if ((item == net.minecraft.world.item.Items.POTATO || item == net.minecraft.world.item.Items.CARROT
                    || item == net.minecraft.world.item.Items.BEETROOT) && n >= 12 * multiplier) {
                return true;
            }
            if (farmer && item == net.minecraft.world.item.Items.WHEAT && n >= 9 * multiplier) return true;
        }
        return false;
    }

    /** Wants more food: less than five meals for a farmer, less than one for the others. */
    public boolean wantsMoreFood() {
        var v = mcc$gameVillager();
        if (v != null) return v.wantsMoreFood();
        return !mcc$hasEnoughItems(getProfession() == 0 ? 5 : 1);
    }

    /** Has food to share (two meals or more). */
    public boolean canAbondonItems() {
        var v = mcc$gameVillager();
        if (v != null) return v.hasExcessFood();
        return mcc$hasEnoughItems(2);
    }

    /** Holds seeds or crops it can plant. */
    public boolean isFarmItemInInventory() {
        var v = mcc$gameVillager();
        if (v != null) return v.hasFarmSeeds();
        for (int i = 0; i < villagerInventory.getSizeInventory(); ++i) {
            Object item = mcc$modernItem(villagerInventory.getStackInSlot(i));
            if (item == net.minecraft.world.item.Items.WHEAT_SEEDS || item == net.minecraft.world.item.Items.POTATO
                    || item == net.minecraft.world.item.Items.CARROT || item == net.minecraft.world.item.Items.BEETROOT_SEEDS) {
                return true;
            }
        }
        return false;
    }

    /** Willing to breed; with {@code updateFirst} it eats a meal to become willing when it has one. */
    public boolean getIsWillingToMate(boolean updateFirst) {
        var v = mcc$gameVillager();
        if (v != null) return v.canBreed();
        if (!mcc$willingToMate && updateFirst && mcc$hasEnoughItems(1)) {
            for (int i = 0; i < villagerInventory.getSizeInventory(); ++i) {
                ItemStack stack = villagerInventory.getStackInSlot(i);
                Object item = mcc$modernItem(stack);
                if (item == null) continue;
                int cost = item == net.minecraft.world.item.Items.BREAD ? 3 : item == net.minecraft.world.item.Items.POTATO
                        || item == net.minecraft.world.item.Items.CARROT || item == net.minecraft.world.item.Items.BEETROOT ? 12 : 0;
                if (cost > 0 && stack.getCount() >= cost) {
                    stack.shrink(cost);
                    if (stack.isEmpty()) villagerInventory.setInventorySlotContents(i, ItemStack.EMPTY);
                    break;
                }
            }
            mcc$willingToMate = true;
        }
        return mcc$willingToMate;
    }

    public void setIsWillingToMate(boolean willing) {
        mcc$willingToMate = willing;
    }

    @Override
    protected boolean canDespawn() {
        return false;
    }

    @Override
    public EntityVillager createChild(EntityAgeable ageable) {
        EntityVillager child = new EntityVillager(mcc$get$world(this));
        return child;
    }

    @Override
    public boolean canBeLeashedTo(EntityPlayer player) {
        return false;
    }

    // ------------------------------------------------------------------ trading (IMerchant)

    @Override
    public void setCustomer(EntityPlayer player) {
        var v = mcc$villager();
        if (v != null) v.setTradingPlayer(player == null ? null : player.mcc$player());
        buyingPlayer = player;
    }

    @Override
    public EntityPlayer getCustomer() {
        var v = mcc$villager();
        if (v != null) {
            var p = v.getTradingPlayer();
            return p == null ? null : (EntityPlayer) org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122.wrap(p);
        }
        return buyingPlayer;
    }

    public boolean isTrading() {
        return getCustomer() != null;
    }

    /** The trades: a modern villager's current offers (a copy), or this villager's own list. */
    @Override
    public MerchantRecipeList getRecipes(EntityPlayer player) {
        var v = mcc$villager();
        if (v != null) {
            MerchantRecipeList list = new MerchantRecipeList();
            for (var offer : v.getOffers()) list.add(MerchantRecipe.mcc$of(offer));
            return list;
        }
        if (buyingList == null) populateBuyingList();
        if (buyingList == null) buyingList = new MerchantRecipeList();
        return buyingList;
    }

    @Override
    public void setRecipes(MerchantRecipeList recipes) {
        var v = mcc$villager();
        if (v != null) {
            if (recipes != null) v.setOffers(recipes.mcc$toModern());
            return;
        }
        buyingList = recipes;
    }

    @Override
    public void useRecipe(MerchantRecipe recipe) {
        if (recipe == null) return;
        recipe.incrementToolUses();
        livingSoundTime = -getTalkInterval();
        if (recipe.getToolUses() == 1 || rand.nextInt(5) == 0) {
            timeUntilReset = 40;
            needsInitilization = true;
        }
        if (recipe.getItemToBuy().getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.EMERALD) wealth += recipe.getItemToBuy().getCount();
        playSound(legacy(SoundEvents.VILLAGER_YES), getSoundVolume(), getSoundPitch());
        var m = mcc$entity;
        if (recipe.getRewardsExp() && m != null && !m.level().isClientSide()) {
            int xp = 3 + rand.nextInt(4);
            m.level().addFreshEntity(new net.minecraft.world.entity.ExperienceOrb(m.level(), m.getX(), m.getY() + 0.5D, m.getZ(), xp));
        }
    }

    @Override
    public void verifySellingItem(ItemStack stack) {
        if (mcc$entity != null && !mcc$entity.level().isClientSide() && livingSoundTime > -getTalkInterval() + 20) {
            livingSoundTime = -getTalkInterval();
            playSound(legacy(stack == null || stack.isEmpty() ? SoundEvents.VILLAGER_NO : SoundEvents.VILLAGER_YES), getSoundVolume(), getSoundPitch());
        }
    }

    @Override
    public World getWorld() {
        return mcc$get$world(this);
    }

    @Override
    public BlockPos getPos() {
        return getPosition();
    }

    /** Right-click: trading with a villager that has trades (a view opens the modern villager's own screen). */
    @Override
    protected boolean processInteract(EntityPlayer player, EnumHand hand) {
        if (mcc$villager() != null) return super.processInteract(player, hand);
        ItemStack held = player.getHeldItem(hand);
        if (held.getItem() == legacyconnector.v1_12_2.net.minecraft.init.Items.NAME_TAG) return super.processInteract(player, hand);
        if (isEntityAlive() && !isTrading() && !isChild() && !player.isSneaking()) {
            if (mcc$entity != null && !mcc$entity.level().isClientSide() && !getRecipes(player).isEmpty()) {
                setCustomer(player);
                player.displayVillagerTradeGui(this);
            }
            return true;
        }
        return super.processInteract(player, hand);
    }

    @Override
    protected void writeEntityToNBT(NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$villager() != null) return;
        tag.setInteger("Profession", getProfession());
        tag.setInteger("Career", careerId);
        tag.setInteger("CareerLevel", careerLevel);
        if (buyingList != null) tag.setTag("Offers", buyingList.getRecipiesAsTags());
        var items = new legacyconnector.v1_12_2.net.minecraft.nbt.NBTTagList();
        for (int i = 0; i < villagerInventory.getSizeInventory(); ++i) {
            ItemStack s = villagerInventory.getStackInSlot(i);
            if (!s.isEmpty()) items.appendTag(s.writeToNBT(new NBTTagCompound()));
        }
        tag.setTag("Inventory", items);
    }

    @Override
    protected void readEntityFromNBT(NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$villager() != null) return;
        setProfession(tag.getInteger("Profession"));
        careerId = tag.getInteger("Career");
        careerLevel = tag.getInteger("CareerLevel");
        if (tag.hasKey("Offers", 10)) buyingList = new MerchantRecipeList(tag.getCompoundTag("Offers"));
        var items = tag.getTagList("Inventory", 10);
        for (int i = 0; i < items.tagCount(); ++i) {
            ItemStack s = new ItemStack(items.getCompoundTagAt(i));
            if (!s.isEmpty()) villagerInventory.addItem(s);
        }
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return legacy(isTrading() ? SoundEvents.VILLAGER_TRADE : SoundEvents.VILLAGER_AMBIENT);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return legacy(SoundEvents.VILLAGER_HURT);
    }

    @Override
    protected SoundEvent getDeathSound() {
        return legacy(SoundEvents.VILLAGER_DEATH);
    }

    @Override
    protected legacyconnector.v1_12_2.net.minecraft.util.ResourceLocation getLootTable() {
        return null;
    }

    @Override
    public float getEyeHeight() {
        return isChild() ? 0.81F : 1.62F;
    }

    // ------------------------------------------------------------------ trade building blocks

    /** A price range: {@link #getPrice} picks one in [first, second] (negative ranges mean "this many items for one emerald"). */
    public static class PriceInfo extends legacyconnector.v1_12_2.net.minecraft.util.Tuple<Integer, Integer> {
        public PriceInfo(int min, int max) {
            super(min, max);
            if (max < min) org.slf4j.LoggerFactory.getLogger("LegacyConnector/Villagers").warn("PriceRange({}, {}) invalid, {} smaller than {}", min, max, max, min);
        }

        public int getPrice(Random rand) {
            return getFirst() >= getSecond() ? getFirst() : getFirst() + rand.nextInt(getSecond() - getFirst() + 1);
        }
    }

    /** Adds trades to a merchant's list (a career's trades per level). */
    public interface ITradeList {
        void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipeList, Random random);
    }

    /** Buys items for an emerald. */
    public static class EmeraldForItems implements ITradeList {
        public Item buyingItem;
        public PriceInfo price;

        public EmeraldForItems(Item item, PriceInfo price) {
            this.buyingItem = item;
            this.price = price;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipeList, Random random) {
            int i = price == null ? 1 : price.getPrice(random);
            recipeList.add(new MerchantRecipe(new ItemStack(buyingItem, i, 0), legacyconnector.v1_12_2.net.minecraft.init.Items.EMERALD));
        }
    }

    /** Sells an item for emeralds (a negative price: several items for one emerald). */
    public static class ListItemForEmeralds implements ITradeList {
        public ItemStack itemToBuy;
        public PriceInfo priceInfo;

        public ListItemForEmeralds(Item item, PriceInfo price) {
            this(new ItemStack(item), price);
        }

        public ListItemForEmeralds(ItemStack stack, PriceInfo price) {
            this.itemToBuy = stack;
            this.priceInfo = price;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipeList, Random random) {
            int i = priceInfo == null ? 1 : priceInfo.getPrice(random);
            ItemStack emeralds;
            ItemStack sold;
            if (i < 0) {
                emeralds = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.EMERALD);
                sold = new ItemStack(itemToBuy.getItem(), -i, itemToBuy.getMetadata());
            } else {
                emeralds = new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.EMERALD, i, 0);
                sold = new ItemStack(itemToBuy.getItem(), 1, itemToBuy.getMetadata());
            }
            recipeList.add(new MerchantRecipe(emeralds, sold));
        }
    }

    /** Turns an item plus emeralds into another item. */
    public static class ItemAndEmeraldToItem implements ITradeList {
        public ItemStack buyingItemStack;
        public PriceInfo buyingPriceInfo;
        public ItemStack sellingItemstack;
        public PriceInfo sellingPriceInfo;

        public ItemAndEmeraldToItem(Item buying, PriceInfo buyingPrice, Item selling, PriceInfo sellingPrice) {
            this.buyingItemStack = new ItemStack(buying);
            this.buyingPriceInfo = buyingPrice;
            this.sellingItemstack = new ItemStack(selling);
            this.sellingPriceInfo = sellingPrice;
        }

        @Override
        public void addMerchantRecipe(IMerchant merchant, MerchantRecipeList recipeList, Random random) {
            int i = buyingPriceInfo.getPrice(random);
            int j = sellingPriceInfo.getPrice(random);
            recipeList.add(new MerchantRecipe(new ItemStack(buyingItemStack.getItem(), i, buyingItemStack.getMetadata()),
                    new ItemStack(legacyconnector.v1_12_2.net.minecraft.init.Items.EMERALD), new ItemStack(sellingItemstack.getItem(), j, sellingItemstack.getMetadata())));
        }
    }
}
