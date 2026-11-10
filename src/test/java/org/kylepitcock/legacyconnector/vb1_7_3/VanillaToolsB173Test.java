package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.block.Block;
import legacyconnector.vb1_7_3.net.minecraft.item.*;
import legacyconnector.vb1_7_3.net.minecraft.src.Tool;
import legacyconnector.vb1_7_3.net.minecraft.src.ToolBase;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaToolsB173Test {
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();}
    @Test void allFifteenVanillaMiningToolsHaveCompleteTypedProxiesAndCanonicalMaterials() throws Exception {
        int[][] ids={{269,270,271},{273,274,275},{256,257,258},{277,278,279},{284,285,286}};
        ToolMaterial[] materials={ToolMaterial.WOOD,ToolMaterial.STONE,ToolMaterial.IRON,ToolMaterial.DIAMOND,ToolMaterial.GOLD};
        Class<?>[] types={ShovelItem.class,PickaxeItem.class,AxeItem.class};ToolBase[] families={ToolBase.Shovel,ToolBase.Pickaxe,ToolBase.Axe};
        var material=ToolItem.class.getDeclaredField("toolMaterial");material.setAccessible(true);
        for(int tier=0;tier<ids.length;tier++)for(int family=0;family<3;family++){
            int id=ids[tier][family];var modern=VanillaB173.modernItem(id,0);var stack=StacksB173.toLegacy(new net.minecraft.world.item.ItemStack(modern));
            assertNotNull(stack);assertEquals(id,stack.itemId);var view=stack.getItem();assertSame(Item.ITEMS[id],view);
            assertInstanceOf(types[family],view);var tool=assertInstanceOf(Tool.class,view);
            assertSame(materials[tier],material.get(view));assertSame(families[family],tool.toolBase);assertFalse(tool.usingSAPI);
            assertEquals(materials[tier].getDurability(),view.getMaxDamage());assertEquals(1,view.getMaxCount());assertTrue(view.isHandheld());
            assertNotNull(tool.mineBlocks);assertNotNull(tool.mineMaterials);assertFalse(tool.mineBlocks.isEmpty());assertNull(view.mcc$owner);
            int target=family==0?3:family==1?1:17;assertTrue(tool.canHarvest(Block.BLOCKS[target]));
            assertEquals(materials[tier].getMiningSpeedMultiplier(),view.getMiningSpeedMultiplier(stack,Block.BLOCKS[target]));
            assertSame(modern,StacksB173.toModern(stack).getItem());
        }
    }
    @Test void vanillaToolFamiliesRetainExactHarvestEligibilityAndTierDropPredicates(){
        var pick=assertInstanceOf(PickaxeItem.class,Item.ITEMS[278]);
        assertTrue(pick.canHarvest(Block.BLOCKS[1]),"Vanilla diamond pick qualifies through the actual shared Tool API");
        assertFalse(((Tool)Item.ITEMS[279]).canHarvest(Block.BLOCKS[1]));assertFalse(((Tool)Item.ITEMS[277]).canHarvest(Block.BLOCKS[1]));
        assertTrue(((Tool)Item.ITEMS[279]).canHarvest(Block.BLOCKS[17]));assertTrue(((Tool)Item.ITEMS[277]).canHarvest(Block.BLOCKS[3]));
        assertTrue(pick.isSuitableFor(Block.BLOCKS[49]));assertFalse(Item.ITEMS[270].isSuitableFor(Block.BLOCKS[49]));
        assertFalse(Item.ITEMS[274].isSuitableFor(Block.BLOCKS[56]));assertTrue(Item.ITEMS[257].isSuitableFor(Block.BLOCKS[56]));
        assertTrue(Item.ITEMS[277].isSuitableFor(Block.BLOCKS[78]));assertTrue(Item.ITEMS[277].isSuitableFor(Block.BLOCKS[80]));
        assertFalse(Item.ITEMS[277].isSuitableFor(Block.BLOCKS[1]));assertFalse(Item.ITEMS[279].isSuitableFor(Block.BLOCKS[1]));
        assertFalse(StacksB173.toLegacy(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK)).getItem() instanceof Tool);
    }
    @Test void toolRoundtripPreservesNativeComponentsDamageAndNativeAttributeDefinitions(){
        var modern=new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.DIAMOND_PICKAXE);modern.setDamageValue(17);
        modern.set(DataComponents.CUSTOM_NAME,Component.literal("Original native tool Ω"));
        var data=new net.minecraft.nbt.CompoundTag();data.putLong("nativeFixture",123456789L);
        modern.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
        var attributes=modern.get(DataComponents.ATTRIBUTE_MODIFIERS);var legacy=StacksB173.toLegacy(modern);
        assertInstanceOf(PickaxeItem.class,legacy.getItem());assertEquals(17,legacy.getDamage());
        var restored=StacksB173.toModern(legacy);assertTrue(net.minecraft.world.item.ItemStack.isSameItemSameComponents(modern,restored));
        assertEquals(attributes,restored.get(DataComponents.ATTRIBUTE_MODIFIERS));assertSame(net.minecraft.world.item.Items.DIAMOND_PICKAXE,restored.getItem());
    }
    @Test void completeVanillaFactoriesNeverRegisterOrBorrowTheCurrentModOwner(){
        var metadata=new com.google.gson.JsonObject();metadata.addProperty("modernId","fixture_tool_owner");
        var owner=new org.kylepitcock.legacyconnector.core.LegacyModRecord("b1.7.3","fixture_tool_owner","Fixture",null,null,metadata);
        var previous=ContentB173.current();int count=ContentB173.items().size();var canonical=Item.ITEMS[278];
        ContentB173.enter(owner);
        try{
            var proxy=PickaxeItem.mcc$vanilla(278,ToolMaterial.DIAMOND);
            assertNull(proxy.mcc$owner);assertEquals(278,proxy.id);assertSame(ToolBase.Pickaxe,proxy.toolBase);
            assertEquals(1561,proxy.getMaxDamage());assertNotNull(proxy.mineBlocks);assertTrue(proxy.canHarvest(Block.BLOCKS[1]));
            assertEquals(count,ContentB173.items().size());assertSame(canonical,Item.ITEMS[278]);
        }finally{if(previous==null)ContentB173.exit();else ContentB173.enter(previous);}
    }
}
