package legacyconnector.v1_6_4.forward;

import legacyconnector.v1_6_4.net.minecraft.util.Ids;
import legacyconnector.v1_7_10.net.minecraft.block.Block;
import legacyconnector.v1_7_10.net.minecraft.entity.Entity;
import legacyconnector.v1_7_10.net.minecraft.entity.EntityLivingBase;
import legacyconnector.v1_7_10.net.minecraft.init.Blocks;
import legacyconnector.v1_7_10.net.minecraft.item.Item;
import legacyconnector.v1_7_10.net.minecraft.item.ItemStack;
import legacyconnector.v1_7_10.net.minecraft.util.AxisAlignedBB;
import legacyconnector.v1_7_10.net.minecraft.world.World;

/**
 * More call adapters for 1.4.7/1.5.2 members that have no 1.7.10 counterpart of the same shape (the {@code R ... static} rules of
 * {@code forward/1.5.2/member-manual.tsv}): numeric ids, old argument lists, methods 1.7.10 folded into others. Real behaviour, no placeholders.
 */
public final class Forward152Calls {
    private Forward152Calls() {}

    /** {@code stack.getDamageVsEntity(target)}: the attack damage of the stack's item. */
    public static int damageVsEntity(ItemStack stack, Entity target) {
        return stack == null ? 1 : itemDamageVsEntity(stack.getItem(), target);
    }

    /** {@code sword/axe/pickaxe.getDamageVsEntity(target)}: the item's base attack damage (an int in 1.5.2, a float in 1.7.10). */
    public static int itemDamageVsEntity(Item item, Entity target) {
        if (item instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemSword sword) return (int) sword.field_150934_a;
        if (item instanceof legacyconnector.v1_7_10.net.minecraft.item.ItemTool tool) return (int) tool.damageVsEntity;
        return 1;
    }

    /** {@code FurnaceRecipes.getExperience(itemId)}: the experience of smelting that item. */
    public static float smeltingExperience(legacyconnector.v1_7_10.net.minecraft.item.crafting.FurnaceRecipes recipes, int itemId) {
        Item item = Forward152.item(itemId);
        return item == null ? 0.0F : recipes.getSmeltingExperience(new ItemStack(item, 1, 0));
    }

    /** {@code world.getBrightness(x, y, z, minLight)}: the light curve value (0..1) of the block light, at least {@code minLight}. */
    public static float lightBrightness(World world, int x, int y, int z, int minLight) {
        int level = Math.max(world.getBlockLightValue(x, y, z), minLight);
        float f = 1.0F - Math.max(0, Math.min(15, level)) / 15.0F;
        return (1.0F - f) / (f * 3.0F + 1.0F);
    }

    /** {@code villagers.registerVillagerType(id, texturePath)}: the old name of {@code registerVillagerSkin}, the skin a path string. */
    public static void registerVillagerType(legacyconnector.v1_7_10.cpw.mods.fml.common.registry.VillagerRegistry registry, int id, String texture) {
        registry.registerVillagerSkin(id, org.kylepitcock.legacyconnector.v1_6_4.bridge.Textures152.location(texture));
    }

    /** {@code fx.setParticleIcon(renderEngine, icon)}: the particle sheet is the texture manager's; only the icon matters. */
    public static void setParticleIcon(legacyconnector.v1_7_10.net.minecraft.client.particle.EntityFX fx,
            legacyconnector.v1_7_10.net.minecraft.client.renderer.texture.TextureManager engine, legacyconnector.v1_7_10.net.minecraft.util.IIcon icon) {
        fx.setParticleIcon(icon);
    }

    /** {@code entity.playStepSound(x, y, z, blockId)}. */
    public static void playStepSound(Entity entity, int x, int y, int z, int blockId) {
        Block block = Ids.block(blockId);
        if (block != null) entity.mcc$playStepSound(x, y, z, block);
    }

    /** {@code entity.dropItemWithOffset(itemId, count, offset)}. */
    public static legacyconnector.v1_7_10.net.minecraft.entity.item.EntityItem dropItemWithOffset(Entity entity, int itemId, int count, float offset) {
        Item item = Forward152.item(itemId);
        return item == null ? null : entity.dropItemWithOffset(item, count, offset);
    }

    /** {@code block.idPicked(world, x, y, z)}: the item id middle-click picks for that block. */
    public static int idPicked(Block block, World world, int x, int y, int z) {
        Item item = block.getItem(world, x, y, z);
        return item == null ? 0 : Ids.itemId(item);
    }

    /** {@code BlockChest.isOcelotBlockingChest(world, x, y, z)}: a sitting ocelot on top keeps the chest from opening. */
    public static boolean isOcelotBlockingChest(legacyconnector.v1_7_10.net.minecraft.block.BlockChest chest, World world, int x, int y, int z) {
        AxisAlignedBB box = AxisAlignedBB.getBoundingBox(x, y + 1, z, x + 1, y + 2, z + 1);
        for (Object o : world.getEntitiesWithinAABB(legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot.class, box)) {
            if (o instanceof legacyconnector.v1_7_10.net.minecraft.entity.passive.EntityOcelot ocelot && ocelot.isSitting()) return true;
        }
        return false;
    }

    /** {@code BlockFlowerPot.getMetaForPlant(stack)}: which plant model (1..12) the pot shows for that stack, 0 when it takes none. */
    public static int getMetaForPlant(legacyconnector.v1_7_10.net.minecraft.block.BlockFlowerPot pot, ItemStack stack) {
        if (stack == null) return 0;
        Block block = Block.getBlockFromItem(stack.getItem());
        int damage = stack.getItemDamage();
        if (block == Blocks.red_flower) return damage == 0 ? 1 : 0;
        if (block == Blocks.yellow_flower) return 2;
        if (block == Blocks.cactus) return 9;
        if (block == Blocks.brown_mushroom) return 8;
        if (block == Blocks.red_mushroom) return 7;
        if (block == Blocks.deadbush) return 10;
        if (block == Blocks.sapling) return damage >= 0 && damage <= 3 ? 3 + damage : 0;
        if (block == Blocks.tallgrass) return damage == 2 ? 11 : 0;
        return 0;
    }

    /** {@code BlockFlower.checkFlowerChange(world, x, y, z)}: a plant that can no longer stand drops itself and goes away. */
    public static void checkFlowerChange(Block block, World world, int x, int y, int z) {
        if (!block.canBlockStay(world, x, y, z)) {
            block.dropBlockAsItem(world, x, y, z, world.getBlockMetadata(x, y, z), 0);
            world.setBlockToAir(x, y, z);
        }
    }

    /** {@code stack.onBlockDestroyed(world, blockId, x, y, z, player)}. */
    public static void onBlockDestroyed(ItemStack stack, World world, int blockId, int x, int y, int z,
            legacyconnector.v1_7_10.net.minecraft.entity.player.EntityPlayer player) {
        Block block = Ids.block(blockId);
        if (block != null) stack.onBlockDestroyed(world, block, x, y, z, player);
    }

    /** {@code Minecraft.getOs()} (static). */
    public static legacyconnector.v1_7_10.net.minecraft.util.Util.EnumOS getOs() {
        return legacyconnector.v1_7_10.net.minecraft.util.Util.getOSType();
    }

    /** {@code sender.translateString(key, args)}: the game's language formats the key. */
    public static String translateString(legacyconnector.v1_7_10.net.minecraft.command.ICommandSender sender, String key, Object[] args) {
        return legacyconnector.v1_7_10.net.minecraft.util.StatCollector.translateToLocalFormatted(key, args);
    }

    /** {@code StringTranslate.translateNamedKey(key)}: the {@code key.name} entry (what item and block names live under). */
    public static String translateNamedKey(legacyconnector.v1_7_10.net.minecraft.util.StringTranslate translate, String key) {
        return translate.translateKey(key + ".name");
    }

    /** {@code renderer.addBlockDestroyEffects(x, y, z, blockId, meta)}. */
    public static void addBlockDestroyEffects(legacyconnector.v1_7_10.net.minecraft.client.particle.EffectRenderer renderer, int x, int y, int z, int blockId,
            int meta) {
        Block block = Ids.block(blockId);
        if (block != null) renderer.addBlockDestroyEffects(x, y, z, block, meta);
    }

    /** {@code skull.setSkullType(type, ownerName)}: the type and, for a player head, the owner whose skin it shows. */
    public static void setSkullType(legacyconnector.v1_7_10.net.minecraft.tileentity.TileEntitySkull skull, int type, String owner) {
        skull.func_152107_a(type);
        if (owner != null && !owner.isEmpty()) skull.func_152106_a(new com.mojang.authlib.GameProfile(null, owner));
    }

    /** {@code entity.getSpeedModifier()}: the movement speed multiplier, 1.0 for a creature without a speed effect (1.7.10 keeps it in the attribute). */
    public static float getSpeedModifier(EntityLivingBase entity) {
        return 1.0F;
    }

    /** {@code selector.selectedBlockId = id}: the block of a randomized fill, by id. */
    public static void setSelectedBlockId(legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureComponent.BlockSelector selector, int id) {
        selector.mcc$setSelectedBlock(Ids.block(id));
    }

    /** {@code selector.getSelectedBlockId()}. */
    public static int getSelectedBlockId(legacyconnector.v1_7_10.net.minecraft.world.gen.structure.StructureComponent.BlockSelector selector) {
        Block block = selector.func_151561_a();
        return block == null ? 0 : Ids.blockId(block);
    }
}
