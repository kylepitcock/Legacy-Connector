package legacyconnector.v1_7_10.net.minecraft.entity.passive;

import legacyconnector.v1_7_10.net.minecraft.entity.EntityAgeable;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.init.Items;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.world.World;
import net.minecraft.world.entity.animal.horse.AbstractChestedHorse;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.animal.horse.Horse;

/**
 * 1.7.10 horse: status flags in watcher slot 16 (bit 2 tame, bit 4 saddled, bit 8 chested), type in slot 19 (0 horse, 1 donkey,
 * 2 mule, 3 zombie, 4 skeleton), variant in slot 20 (colour | markings << 8), owner in 21, armor in 22 (0 none, 1 iron, 2 gold,
 * 3 diamond). A view of a modern horse, donkey, mule, llama or undead horse answers from the modern animal.
 */
public class EntityHorse extends EntityAnimal {
    private static final String[] horseTextures = {"textures/entity/horse/horse_white.png", "textures/entity/horse/horse_creamy.png",
            "textures/entity/horse/horse_chestnut.png", "textures/entity/horse/horse_brown.png", "textures/entity/horse/horse_black.png",
            "textures/entity/horse/horse_gray.png", "textures/entity/horse/horse_darkbrown.png"};
    private static final String[] horseTextureAbbrev = {"hwh", "hcr", "hch", "hbr", "hbl", "hgr", "hdb"};
    private static final String[] horseMarkingTextures = {null, "textures/entity/horse/horse_markings_white.png",
            "textures/entity/horse/horse_markings_whitefield.png", "textures/entity/horse/horse_markings_whitedots.png",
            "textures/entity/horse/horse_markings_blackdots.png"};
    private static final String[] horseMarkingTexturesAbbrev = {"", "wo_", "wmo", "wdt", "bdt"};
    private static final String[] horseArmorTextures = {null, "textures/entity/horse/armor/horse_armor_iron.png",
            "textures/entity/horse/armor/horse_armor_gold.png", "textures/entity/horse/armor/horse_armor_diamond.png"};
    private static final String[] horseArmorTexturesAbbrev = {"", "meo", "goo", "dio"};

    private final String[] field_110280_bW = new String[3];
    private String field_110286_bQ;

    /** View of a modern vanilla mob (mods test and cast vanilla mobs against this class). */
    public EntityHorse(net.minecraft.world.entity.LivingEntity modern) {
        super(modern);
    }

    public EntityHorse(World world) {
        super(world);
        setSize(1.4F, 1.6F);
    }

    /** The modern horse this is a view of (null for a mod's horse, which keeps the legacy watcher). */
    private AbstractHorse mcc$horse() {
        return mcc$entity instanceof AbstractHorse h && !mcc$hosted() ? h : null;
    }

    @Override
    protected void entityInit() {
        super.entityInit();
        dataWatcher.addObject(16, 0);
        dataWatcher.addObject(19, (byte) 0);
        dataWatcher.addObject(20, 0);
        dataWatcher.addObject(21, "");
        dataWatcher.addObject(22, 0);
    }

    @Override
    public EntityAgeable createChild(EntityAgeable mate) {
        return new EntityHorse(mcc$world());
    }

    private boolean getHorseWatchableObjectIntAsBoolean(int bit) {
        return (dataWatcher.getWatchableObjectInt(16) & bit) != 0;
    }

    private void setHorseWatchableBoolean(int bit, boolean value) {
        int flags = dataWatcher.getWatchableObjectInt(16);
        dataWatcher.updateObject(16, value ? flags | bit : flags & ~bit);
    }

    public boolean isTame() {
        var h = mcc$horse();
        return h != null ? h.isTamed() : getHorseWatchableObjectIntAsBoolean(2);
    }

    public void setHorseTamed(boolean tamed) {
        var h = mcc$horse();
        if (h != null) h.setTamed(tamed);
        else setHorseWatchableBoolean(2, tamed);
    }

    public boolean isHorseSaddled() {
        var h = mcc$horse();
        return h != null ? h.isSaddled() : getHorseWatchableObjectIntAsBoolean(4);
    }

    /** 0 horse, 1 donkey (and llama), 2 mule, 3 zombie horse, 4 skeleton horse. */
    public int getHorseType() {
        var h = mcc$horse();
        if (h != null) {
            if (h instanceof net.minecraft.world.entity.animal.horse.Mule) return 2;
            if (h instanceof AbstractChestedHorse) return 1;
            if (h instanceof net.minecraft.world.entity.animal.horse.ZombieHorse) return 3;
            if (h instanceof net.minecraft.world.entity.animal.horse.SkeletonHorse) return 4;
            return 0;
        }
        return dataWatcher.getWatchableObjectByte(19);
    }

    /** Changes a mod horse's type (a modern animal's kind is its class and stays). */
    public void setHorseType(int type) {
        if (mcc$horse() != null) return;
        dataWatcher.updateObject(19, (byte) type);
        resetTexturePrefix();
    }

    /** Coat colour (0-6) | markings (0-4) << 8. */
    public int getHorseVariant() {
        if (mcc$horse() instanceof Horse h) return h.getVariant().getId() | h.getMarkings().getId() << 8;
        if (mcc$horse() != null) return 0;
        return dataWatcher.getWatchableObjectInt(20);
    }

    public void setHorseVariant(int variant) {
        if (mcc$horse() instanceof Horse h) {
            org.kylepitcock.legacyconnector.v1_7_10.entity.ModernAccess.setHorseTypeVariant(h, variant);
        } else if (mcc$horse() == null) {
            dataWatcher.updateObject(20, Integer.valueOf(variant));
        }
        resetTexturePrefix();
    }

    /** The owner's UUID as a string ("" when untamed). */
    public String func_152119_ch() {
        var h = mcc$horse();
        if (h != null) return h.getOwnerUUID() == null ? "" : h.getOwnerUUID().toString();
        return dataWatcher.getWatchableObjectString(21);
    }

    public void func_152120_b(String owner) {
        var h = mcc$horse();
        if (h != null) {
            try {
                h.setOwnerUUID(owner == null || owner.isEmpty() ? null : java.util.UUID.fromString(owner));
            } catch (IllegalArgumentException e) {
                // not a UUID (an old name): leave the owner
            }
            return;
        }
        dataWatcher.updateObject(21, owner == null ? "" : owner);
    }

    public boolean isChested() {
        var h = mcc$horse();
        if (h != null) return h instanceof AbstractChestedHorse c && c.hasChest();
        return getHorseWatchableObjectIntAsBoolean(8);
    }

    public void setChested(boolean chested) {
        var h = mcc$horse();
        if (h != null) {
            if (h instanceof AbstractChestedHorse c) c.setChest(chested);
            return;
        }
        setHorseWatchableBoolean(8, chested);
    }

    /** Only donkeys and mules carry chests. */
    public boolean canCarryChest() {
        int type = getHorseType();
        return type == 1 || type == 2;
    }

    /** Only horses wear armor. */
    public boolean canWearArmor() {
        return getHorseType() == 0;
    }

    /** The armor worn: 0 none, 1 iron, 2 gold, 3 diamond. */
    public int func_110241_cb() {
        var h = mcc$horse();
        if (h != null) {
            var armor = h.getBodyArmorItem().getItem();
            if (armor == net.minecraft.world.item.Items.IRON_HORSE_ARMOR) return 1;
            if (armor == net.minecraft.world.item.Items.GOLDEN_HORSE_ARMOR) return 2;
            if (armor == net.minecraft.world.item.Items.DIAMOND_HORSE_ARMOR) return 3;
            return 0;
        }
        return dataWatcher.getWatchableObjectInt(22);
    }

    /** Whether the item is horse armor. */
    public static boolean func_146085_a(Item item) {
        return item == Items.iron_horse_armor || item == Items.golden_horse_armor || item == Items.diamond_horse_armor;
    }

    /** Drops what the chest holds, then the chest itself. */
    public void dropChestItems() {
        var h = mcc$horse();
        if (h != null) {
            if (!mcc$world().isRemote && h instanceof AbstractChestedHorse c && c.hasChest()) {
                var inv = h.getInventory();
                // slot 0 is the saddle; the rest is the chest
                for (int i = 1; i < inv.getContainerSize(); i++) {
                    var stack = inv.removeItemNoUpdate(i);
                    if (!stack.isEmpty()) h.spawnAtLocation(stack);
                }
            }
        }
        dropChests();
    }

    /** Drops the chest (its contents stay) and takes it off. */
    public void dropChests() {
        if (!mcc$world().isRemote && isChested()) {
            entityDropItem(new ItemStack(Blocks.chest, 1), 0.0F);
            setChested(false);
        }
    }

    // ------------------------------------------------------------------ client textures (JourneyMap draws horse icons from them)

    private void resetTexturePrefix() {
        field_110286_bQ = null;
    }

    private void setHorseTexturePaths() {
        String prefix = "horse/";
        field_110280_bW[0] = null;
        field_110280_bW[1] = null;
        field_110280_bW[2] = null;
        int type = getHorseType();
        int variant = getHorseVariant();
        if (type == 0) {
            int colour = Math.min(variant & 255, horseTextures.length - 1);
            int markings = Math.min((variant & 65280) >> 8, horseMarkingTextures.length - 1);
            field_110280_bW[0] = horseTextures[colour];
            prefix += horseTextureAbbrev[colour];
            field_110280_bW[1] = horseMarkingTextures[markings];
            prefix += horseMarkingTexturesAbbrev[markings];
        } else {
            field_110280_bW[0] = "";
            prefix += "_" + type + "_";
        }
        int armor = Math.min(func_110241_cb(), horseArmorTextures.length - 1);
        field_110280_bW[2] = horseArmorTextures[armor];
        prefix += horseArmorTexturesAbbrev[armor];
        field_110286_bQ = prefix;
    }

    /** A key naming the combination of coat, markings and armor (the renderer caches layered textures by it). */
    public String getHorseTexture() {
        setHorseTexturePaths();
        return field_110286_bQ;
    }

    /** The coat, markings and armor texture paths (null where there is none). */
    public String[] getVariantTexturePaths() {
        setHorseTexturePaths();
        return field_110280_bW;
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }
}
