package mccompat.v1_7_10.cpw.mods.fml.common.registry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import mccompat.v1_7_10.cpw.mods.fml.common.IFuelHandler;
import mccompat.v1_7_10.cpw.mods.fml.common.IWorldGenerator;
import mccompat.v1_7_10.net.minecraft.block.Block;
import mccompat.v1_7_10.net.minecraft.item.Item;
import mccompat.v1_7_10.net.minecraft.item.ItemBlock;
import mccompat.v1_7_10.net.minecraft.item.ItemStack;
import mccompat.v1_7_10.net.minecraft.item.crafting.IRecipe;
import mccompat.v1_7_10.net.minecraft.tileentity.TileEntity;
import org.kylepitcock.mccompat.v1_7_10.bridge.BlockBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.FuelBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.ItemBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.RecipeBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.TileEntityBridge;
import org.kylepitcock.mccompat.v1_7_10.bridge.WorldGenBridge;

public class GameRegistry {
    public static void registerWorldGenerator(IWorldGenerator generator, int modGenerationWeight) {
        WorldGenBridge.register(generator, modGenerationWeight);
    }

    public static void registerFuelHandler(IFuelHandler handler) {
        FuelBridge.register(handler);
    }

    public static void registerItem(Item item, String name) {
        registerItem(item, name, null);
    }

    public static Item registerItem(Item item, String name, String modId) {
        ItemBridge.register(item, name, modId);
        return item;
    }

    public static Block registerBlock(Block block, String name) {
        return registerBlock(block, ItemBlock.class, name);
    }

    public static Block registerBlock(Block block, Class<? extends ItemBlock> itemclass, String name) {
        return registerBlock(block, itemclass, name, (String) null);
    }

    public static Block registerBlock(Block block, Class<? extends ItemBlock> itemclass, String name, String modId, Object... itemCtorArgs) {
        BlockBridge.register(block, itemclass, name, modId, itemCtorArgs);
        return block;
    }

    public static Block registerBlock(Block block, Class<? extends ItemBlock> itemclass, String name, Object... itemCtorArgs) {
        return registerBlock(block, itemclass, name, null, itemCtorArgs);
    }

    public static void registerTileEntity(Class<? extends TileEntity> tileEntityClass, String id) {
        mccompat.v1_7_10.net.minecraft.tileentity.TileEntity.addMapping(tileEntityClass, id);
    }

    public static void registerTileEntityWithAlternatives(Class<? extends TileEntity> tileEntityClass, String id, String... alternatives) {
        mccompat.v1_7_10.net.minecraft.tileentity.TileEntity.addMapping(tileEntityClass, id);
        for (String alt : alternatives) TileEntityBridge.addAlias(alt, id);
    }

    public static void addRecipe(ItemStack output, Object... params) {
        addShapedRecipe(output, params);
    }

    public static IRecipe addShapedRecipe(ItemStack output, Object... params) {
        return RecipeBridge.addShaped(output, params, true);
    }

    public static void addShapelessRecipe(ItemStack output, Object... params) {
        RecipeBridge.addShapeless(output, params);
    }

    public static void addRecipe(IRecipe recipe) {
        RecipeBridge.add(recipe);
    }

    public static void addSmelting(Block input, ItemStack output, float xp) {
        RecipeBridge.addSmelting(new ItemStack(input, 1, Short.MAX_VALUE), output, xp);
    }

    public static void addSmelting(Item input, ItemStack output, float xp) {
        RecipeBridge.addSmelting(new ItemStack(input, 1, Short.MAX_VALUE), output, xp);
    }

    public static void addSmelting(ItemStack input, ItemStack output, float xp) {
        RecipeBridge.addSmelting(input, output, xp);
    }

    public static int getFuelValue(ItemStack stack) {
        return FuelBridge.burnTime(stack);
    }

    public static Block findBlock(String modId, String name) {
        return BlockBridge.byName(modId + ":" + name);
    }

    public static Item findItem(String modId, String name) {
        return ItemBridge.byName(modId + ":" + name);
    }

    public static ItemStack findItemStack(String modId, String name, int stackSize) {
        Item item = findItem(modId, name);
        return item == null ? null : new ItemStack(item, stackSize, 0);
    }

    public static void registerCustomItemStack(String name, ItemStack stack) {
        ItemBridge.registerCustomStack(name, stack);
    }

    public static UniqueIdentifier findUniqueIdentifierFor(Block block) {
        String name = BlockBridge.nameOf(block);
        return name == null ? null : new UniqueIdentifier(name);
    }

    public static UniqueIdentifier findUniqueIdentifierFor(Item item) {
        String name = ItemBridge.nameOf(item);
        return name == null ? null : new UniqueIdentifier(name);
    }

    public static final class UniqueIdentifier {
        public final String modId;
        public final String name;

        public UniqueIdentifier(String modId, String name) {
            this.modId = modId;
            this.name = name;
        }

        public UniqueIdentifier(String full) {
            int colon = full.indexOf(':');
            this.modId = colon < 0 ? "minecraft" : full.substring(0, colon);
            this.name = colon < 0 ? full : full.substring(colon + 1);
        }

        @Override
        public boolean equals(Object o) {
            return o instanceof UniqueIdentifier u && u.modId.equals(modId) && u.name.equals(name);
        }

        @Override
        public int hashCode() {
            return modId.hashCode() * 31 + name.hashCode();
        }

        @Override
        public String toString() {
            return modId + ":" + name;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE, ElementType.FIELD})
    public @interface ObjectHolder {
        String value();
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface ItemStackHolder {
        String value();

        int meta() default 0;

        String nbt() default "";
    }
}
