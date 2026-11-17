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
public class EntityHorse extends EntityAnimal implements legacyconnector.v1_7_10.net.minecraft.inventory.IInvBasic {
    /** 1.7.10 {@code horseChest}: slot 0 saddle, slot 1 armor, slots 2-16 the chest of a donkey or mule (a mod's horse only). */
    public legacyconnector.v1_7_10.net.minecraft.inventory.AnimalChest horseChest;

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
        func_110226_cD();
    }

    // ------------------------------------------------------------------ the horse chest (1.7.10 semantics)

    /** The size of the horse chest: 17 for a chested donkey or mule, 2 (saddle, armor) otherwise. */
    protected int func_110225_cC() {
        int type = getHorseType();
        return isChested() && (type == 1 || type == 2) ? 17 : 2;
    }

    /** (Re)creates the horse chest at the current size, keeping what fits, and listens to it. */
    protected void func_110226_cD() {
        if (mcc$horse() != null) return; // a view of a modern horse keeps its own inventory
        var old = horseChest;
        horseChest = new legacyconnector.v1_7_10.net.minecraft.inventory.AnimalChest("HorseChest", func_110225_cC());
        horseChest.func_110133_a(getCommandSenderName());
        if (old != null) {
            old.func_110132_b(this);
            int n = Math.min(old.getSizeInventory(), horseChest.getSizeInventory());
            for (int i = 0; i < n; i++) {
                ItemStack stack = old.getStackInSlot(i);
                if (stack != null) horseChest.setInventorySlotContents(i, stack.copy());
            }
        }
        horseChest.func_110134_a(this);
        func_110232_cE();
    }

    /** Applies the saddle and armor slots of the chest to the horse's flags. */
    protected void func_110232_cE() {
        if (horseChest == null || mcc$horse() != null || mcc$world().isRemote) return;
        setHorseSaddled(horseChest.getStackInSlot(0) != null);
        if (canWearArmor()) setHorseArmorStack(horseChest.getStackInSlot(1));
    }

    @Override
    public void onInventoryChanged(legacyconnector.v1_7_10.net.minecraft.inventory.InventoryBasic inventory) {
        boolean saddled = isHorseSaddled();
        func_110232_cE();
        if (mcc$get$ticksExisted(this) > 20 && !saddled && isHorseSaddled()) playSound("mob.horse.leather", 0.5F, 1.0F);
    }

    /** Opens the horse inventory for the player (a tame horse without another rider). */
    public void openGUI(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        if (horseChest == null || mcc$world().isRemote || !isTame()) return;
        var rider = mcc$get$riddenByEntity(this);
        if (rider != null && rider != player) return;
        horseChest.func_110133_a(getCommandSenderName());
        player.displayGUIHorse(this, horseChest);
    }

    /** Saves like 1.7.10: {@code ChestedHorse}, {@code Items} (slots 2+), {@code ArmorItem}, {@code SaddleItem}, type, variant, tame, owner. */
    @Override
    public void writeEntityToNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.writeEntityToNBT(tag);
        if (mcc$horse() != null) return;
        tag.setBoolean("ChestedHorse", isChested());
        tag.setInteger("Type", getHorseType());
        tag.setInteger("Variant", getHorseVariant());
        tag.setBoolean("Tame", isTame());
        tag.setString("OwnerUUID", func_152119_ch());
        if (horseChest == null) return;
        if (isChested()) {
            var items = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagList();
            for (int i = 2; i < horseChest.getSizeInventory(); i++) {
                ItemStack stack = horseChest.getStackInSlot(i);
                if (stack == null) continue;
                var entry = new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound();
                entry.setByte("Slot", (byte) i);
                stack.writeToNBT(entry);
                items.appendTag(entry);
            }
            tag.setTag("Items", items);
        }
        if (horseChest.getStackInSlot(1) != null) tag.setTag("ArmorItem", horseChest.getStackInSlot(1).writeToNBT(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound()));
        if (horseChest.getStackInSlot(0) != null) tag.setTag("SaddleItem", horseChest.getStackInSlot(0).writeToNBT(new legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound()));
    }

    @Override
    public void readEntityFromNBT(legacyconnector.v1_7_10.net.minecraft.nbt.NBTTagCompound tag) {
        super.readEntityFromNBT(tag);
        if (mcc$horse() != null) return;
        if (tag.hasKey("Type")) setHorseType(tag.getInteger("Type"));
        if (tag.hasKey("Variant")) setHorseVariant(tag.getInteger("Variant"));
        if (tag.hasKey("ChestedHorse")) setChested(tag.getBoolean("ChestedHorse"));
        if (tag.hasKey("Tame")) setHorseTamed(tag.getBoolean("Tame"));
        if (tag.hasKey("OwnerUUID")) func_152120_b(tag.getString("OwnerUUID"));
        func_110226_cD(); // the size follows the chested flag and type just read
        if (horseChest == null) return;
        if (isChested()) {
            var items = tag.getTagList("Items", 10);
            for (int i = 0; i < items.tagCount(); i++) {
                var entry = items.getCompoundTagAt(i);
                int slot = entry.getByte("Slot") & 255;
                if (slot >= 2 && slot < horseChest.getSizeInventory()) horseChest.setInventorySlotContents(slot, ItemStack.loadItemStackFromNBT(entry));
            }
        }
        if (tag.hasKey("ArmorItem", 10)) {
            ItemStack armor = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("ArmorItem"));
            if (armor != null && func_146085_a(armor.getItem())) horseChest.setInventorySlotContents(1, armor);
        }
        if (tag.hasKey("SaddleItem", 10)) {
            ItemStack saddle = ItemStack.loadItemStackFromNBT(tag.getCompoundTag("SaddleItem"));
            if (saddle != null && saddle.getItem() == Items.saddle) horseChest.setInventorySlotContents(0, saddle);
        } else if (tag.getBoolean("Saddle")) {
            horseChest.setInventorySlotContents(0, new ItemStack(Items.saddle));
        }
        func_110232_cE();
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

    /** Tames the horse for the player (a rider that tamed it); false when it is already tame. */
    public boolean setTamedBy(legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        if (isTame()) return false;
        setHorseTamed(true);
        if (player != null) func_152120_b(player.getUniqueID().toString());
        return true;
    }

    /** The most temper a horse can have before it is tamed (100). */
    public int getMaxTemper() {
        var h = mcc$horse();
        return h != null ? h.getMaxTemper() : 100;
    }

    /** Raises the temper (not above the maximum) and returns the new value (1.6.4 name). */
    public int increaseTemper(int amount) {
        var h = mcc$horse();
        if (h == null) return 0;
        int value = Math.max(0, Math.min(h.getMaxTemper(), h.getTemper() + amount));
        h.setTemper(value);
        return value;
    }

    /** The owner's player name ("" when untamed or offline; 1.6.4 stored the name, 1.7.10 the UUID). */
    public String getOwnerName() {
        var h = mcc$horse();
        if (h == null || h.getOwnerUUID() == null) return "";
        var owner = h.level().getPlayerByUUID(h.getOwnerUUID());
        return owner != null ? owner.getGameProfile().getName() : "";
    }

    private int mcc$temper;

    /** The horse's temper (how far it is from being tamed by riding; a mod's own horse keeps what its code set). */
    public int getTemper() {
        var h = mcc$horse();
        return h != null ? h.getTemper() : mcc$temper;
    }

    /** Sets the temper (0 to the maximum). */
    public void setTemper(int temper) {
        var h = mcc$horse();
        if (h != null) h.setTemper(Math.max(0, Math.min(h.getMaxTemper(), temper)));
        else mcc$temper = temper;
    }

    /** Donkeys and mules cannot breed. */
    public boolean isSterile() {
        int type = getHorseType();
        return type == 1 || type == 2;
    }

    /** {@code func_110201_q}: how far the mouth is open (0-1), as {@link #getMouthOpennessAngle}. */
    public float func_110201_q(float partialTicks) {
        return getMouthOpennessAngle(partialTicks);
    }

    /** Whether a rider is making the horse jump (the charge bar); a modern horse keeps it as its own flag. */
    public void setHorseJumping(boolean jumping) {
        mcc$jumping = jumping;
        var h = mcc$horse();
        if (h != null) h.setIsJumping(jumping);
    }

    private boolean mcc$jumping;

    /** Whether the horse is in the middle of a charged jump. */
    public boolean isHorseJumping() {
        return mcc$jumping;
    }

    /** The charge of the rider's jump (0 to 100): a modern horse jumps with it. */
    public void setJumpPower(int power) {
        var h = mcc$horse();
        if (h != null) h.onPlayerJump(power);
        else mcc$jumpPower = power;
    }

    private int mcc$jumpPower;

    /** The type and variant a spawned horse is created with (the group the natural spawn carries). */
    public static class GroupData implements legacyconnector.v1_7_10.net.minecraft.entity.IEntityLivingData {
        public int field_111107_a;
        public int field_111106_b;

        public GroupData(int type, int variant) {
            this.field_111107_a = type;
            this.field_111106_b = variant;
        }
    }

    /** Whether the horse has bred already: modern horses do not track it. */
    public boolean getHasReproduced() {
        return false;
    }

    /** Vanilla's "is breeding" flag (legacy data flag 16): in love, as the modern animal tracks it. */
    public boolean func_110205_ce() {
        return isInLove();
    }

    /** Whether the horse is eating (lowering its head to the grass). */
    public boolean isEatingHaystack() {
        var h = mcc$horse();
        return h != null && h.isEating();
    }

    /** Starts or stops the eating animation (head down to the grass). */
    public void setEatingHaystack(boolean eating) {
        var h = mcc$horse();
        if (h != null) h.setEating(eating);
    }

    /** Rears the horse up on its hind legs or puts it down. */
    public void setRearing(boolean rearing) {
        var h = mcc$horse();
        if (h != null) h.setStanding(rearing);
        else setHorseWatchableBoolean(64, rearing);
    }

    /** Whether the horse is rearing (up on its hind legs). */
    public boolean isRearing() {
        var h = mcc$horse();
        return h != null ? h.isStanding() : getHorseWatchableObjectIntAsBoolean(64);
    }

    /** Rears up with the angry whinny. */
    public void makeHorseRearWithSound() {
        var h = mcc$horse();
        if (h != null) h.playSound(net.minecraft.sounds.SoundEvents.HORSE_ANGRY, 1.0F, 1.0F);
        else setHorseWatchableBoolean(64, true);
    }

    /** The horse's jump strength (0.4 - 1.0 in 1.7.10; the modern jump strength attribute has the same scale). */
    public double getHorseJumpStrength() {
        var h = mcc$horse();
        return h != null ? h.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.JUMP_STRENGTH) : 0.7D;
    }

    /** Saddles the horse (or takes the saddle off). */
    public void setHorseSaddled(boolean saddled) {
        var h = mcc$horse();
        if (h != null) {
            if (saddled) h.equipSaddle(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.SADDLE), net.minecraft.sounds.SoundSource.NEUTRAL);
            else h.getSlot(400).set(net.minecraft.world.item.ItemStack.EMPTY); // slot 400: the saddle
        } else {
            setHorseWatchableBoolean(4, saddled);
        }
    }

    /** Puts horse armor on (an empty stack takes it off). */
    public void setHorseArmorStack(ItemStack armor) {
        var h = mcc$horse();
        if (h != null) {
            h.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY, armor == null ? net.minecraft.world.item.ItemStack.EMPTY : ItemStack.mcc$toModern(armor));
        } else {
            int type = armor == null || armor.getItem() == null ? 0 : armor.getItem() == Items.iron_horse_armor ? 1 : armor.getItem() == Items.golden_horse_armor ? 2
                    : armor.getItem() == Items.diamond_horse_armor ? 3 : 0;
            dataWatcher.updateObject(22, type);
        }
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
        } else if (horseChest != null && !mcc$world().isRemote && isChested()) {
            // a mod's horse: the chest slots (2 and up) fall to the ground, the saddle and armor stay with the horse
            for (int i = 2; i < horseChest.getSizeInventory(); i++) {
                ItemStack stack = horseChest.getStackInSlot(i);
                if (stack != null) {
                    entityDropItem(stack, 0.0F);
                    horseChest.setInventorySlotContents(i, null);
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

    // ------------------------------------------------------------------ animation state read by the renderer and model

    /** Whether the horse is grown up. */
    public boolean isAdultHorse() {
        return !isChild();
    }

    /** Render scale: 1 for adults, 0.5 growing to 1 over a foal's 24000 ticks (1.7.10's formula). */
    public float getHorseSize() {
        int age = getGrowingAge();
        return age >= 0 ? 1.0F : 0.5F + (float) (-24000 - age) / -24000.0F * 0.5F;
    }

    /** How far the head is lowered to the grass (0-1). */
    public float getGrassEatingAmount(float partialTicks) {
        var h = mcc$horse();
        return h != null ? h.getEatAnim(partialTicks) : isEatingHaystack() ? 1.0F : 0.0F;
    }

    /** How far the horse is up on its hind legs (0-1). */
    public float getRearingAmount(float partialTicks) {
        var h = mcc$horse();
        return h != null ? h.getStandAnim(partialTicks) : isRearing() ? 1.0F : 0.0F;
    }

    /** How far the mouth is open (0-1). */
    public float getMouthOpennessAngle(float partialTicks) {
        var h = mcc$horse();
        return h != null ? h.getMouthAnim(partialTicks) : 0.0F;
    }

    /** Whether the skin is composed of layers (coat, markings, armor) rather than one fixed texture. */
    public boolean func_110239_cn() {
        return getHorseType() == 0 || func_110241_cb() > 0;
    }

    /** 1.7.10: the task AI (mods' subclasses inherit it: their walking speed follows the move helper). */
    @Override
    public boolean isAIEnabled() {
        return true;
    }
}
