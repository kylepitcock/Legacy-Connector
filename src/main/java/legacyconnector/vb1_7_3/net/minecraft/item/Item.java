package legacyconnector.vb1_7_3.net.minecraft.item;

import java.util.Random;

import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import org.kylepitcock.legacyconnector.vb1_7_3.ContentB173;

/**
 * Beta 1.7.3 {@code Item} ({@code gm}). The constructor takes the id minus 256 and claims {@code ITEMS[256 + id]} (a clash only printed
 * "CONFLICT" in the original, and the later item won). Damage doubles as the subtype for items with subtypes. Item textures are sprite
 * indices into gui/items.png.
 */
public class Item {
    protected static Random random = new Random();
    public static Item[] ITEMS = new Item[32000];

    public int id;
    protected int maxCount = 64;
    private int maxDamage;
    protected int textureId;
    protected boolean handheld;
    protected boolean hasSubtypes;
    private Item craftingReturnItem;
    private String translationKey;

    /** The modern item this one is (vanilla) or became (mods); null until the bridge registered it. */
    public net.minecraft.world.item.Item mcc$modern;
    /** The legacy mod that constructed the item, or null for vanilla. */
    public Object mcc$owner;

    protected Item(int id) {
        this(id,false);
    }
    /** Complete vanilla API proxies do not register content or borrow the active mod owner. */
    protected Item(int id,boolean vanillaView) {
        if(vanillaView){this.id=id;return;}
        this.id = 256 + id;
        if (ITEMS[256 + id] != null) System.out.println("CONFLICT @ " + id);
        ITEMS[256 + id] = this;
        mcc$owner = ContentB173.claimItem(this);
    }

    /** For the vanilla views, which are allocated without a constructor ({@code VanillaB173}). */
    public void mcc$initVanilla(int id, int maxCount, int maxDamage, String key) {
        this.id = id;
        this.maxCount = maxCount;
        this.maxDamage = maxDamage;
        this.translationKey = key;
    }

    public Item setTextureId(int textureId) {
        this.textureId = textureId;
        return this;
    }

    public Item setMaxCount(int maxCount) {
        this.maxCount = maxCount;
        return this;
    }

    public Item setTexturePosition(int x, int y) {
        this.textureId = x + y * 16;
        return this;
    }

    public int getTextureId(int meta) {
        return textureId;
    }

    public final int getTextureId(ItemStack stack) {
        return getTextureId(stack.getDamage());
    }

    public float getMiningSpeedMultiplier(ItemStack stack, Block block) {
        return 1.0F;
    }

    public boolean useOnBlock(ItemStack stack, legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player,
            legacyconnector.vb1_7_3.net.minecraft.world.World world, int x, int y, int z, int side) {
        return false;
    }

    public ItemStack use(ItemStack stack, legacyconnector.vb1_7_3.net.minecraft.world.World world,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {
        return stack;
    }

    public void useOnEntity(ItemStack stack, legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity target) {}

    /** Forge's metadata-aware mining speed. */
    public float getStrVsBlock(ItemStack stack, Block block, int meta) {
        return getMiningSpeedMultiplier(stack, block);
    }

    public int getMaxCount() {
        return maxCount;
    }

    public int getPlacementMetadata(int meta) {
        return 0;
    }

    public boolean hasSubtypes() {
        return hasSubtypes;
    }

    protected Item setHasSubtypes(boolean subtypes) {
        this.hasSubtypes = subtypes;
        return this;
    }

    public int getMaxDamage() {
        return maxDamage;
    }

    protected Item setMaxDamage(int maxDamage) {
        this.maxDamage = maxDamage;
        return this;
    }

    public boolean isDamageable() {
        return maxDamage > 0 && !hasSubtypes;
    }

    public boolean isSuitableFor(Block block) {
        return false;
    }

    /** Beta returns total melee damage, including the player's one point of base damage. */
    public int getAttackDamage(legacyconnector.vb1_7_3.net.minecraft.entity.Entity target) { return 1; }

    public boolean postHit(ItemStack stack, legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity target,
            legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity attacker) { return false; }

    public boolean postMine(ItemStack stack, int blockId, int x, int y, int z,
            legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity miner) { return false; }

    public Item setHandheld() {
        this.handheld = true;
        return this;
    }

    public boolean isHandheld() {
        return handheld;
    }

    public boolean isHandheldRod() {
        return false;
    }

    public Item setTranslationKey(String key) {
        this.translationKey = "item." + key;
        return this;
    }

    public String getTranslationKey() {
        return translationKey;
    }

    public String getTranslationKey(ItemStack stack) {
        return translationKey;
    }

    public Item setCraftingReturnItem(Item item) {
        if (maxCount > 1) throw new IllegalArgumentException("Max stack size must be 1 for items with crafting results");
        this.craftingReturnItem = item;
        return this;
    }

    public Item getCraftingReturnItem() {
        return craftingReturnItem;
    }

    public boolean hasCraftingReturnItem() {
        return craftingReturnItem != null;
    }

    /** MCP {@code onCreated}: the item was crafted or smelted. */
    public void onCraft(ItemStack stack, legacyconnector.vb1_7_3.net.minecraft.world.World world,
            legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity player) {}

    public String getTranslatedName() {
        String key = getTranslationKey();
        String name = key == null ? null : ContentB173.translate(key + ".name");
        // a base-game view has no Beta language entry here: the native name is the one the player sees
        if ((name == null || name.equals(key + ".name")) && mcc$modern != null) return mcc$modern.getDescription().getString();
        return name == null ? "" : name;
    }

    public int getColorMultiplier(int meta) {
        return 0xFFFFFF;
    }

    public boolean isNetworkSynced() {
        return false;
    }

    @Override
    public String toString() {
        return getClass().getName() + "[" + id + (translationKey != null ? ", " + translationKey : "") + "]";
    }
}
