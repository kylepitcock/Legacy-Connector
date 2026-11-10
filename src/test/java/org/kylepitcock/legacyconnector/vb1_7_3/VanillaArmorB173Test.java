package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.class_277$1;
import legacyconnector.vb1_7_3.net.minecraft.inventory.SimpleInventory;
import legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaArmorB173Test {
    @BeforeAll static void bootstrap() throws Exception {NativeStackComponentsB173Test.bootstrap();}
    @Test void allTwentyArmorViewsHaveCanonicalConstructorStateAndPassOnlyTheirOriginalSlot(){
        int[] types={0,1,2,3,1},protection={3,8,6,3},durability={11,16,15,13};
        var inventory=new SimpleInventory("Armor fixture",4);var slots=new class_277$1[4];
        for(int slot=0;slot<4;slot++)slots[slot]=new class_277$1(null,inventory,slot,0,0,slot);
        for(int material=0;material<5;material++)for(int slot=0;slot<4;slot++){
            int id=298+material*4+slot;var nativeItem=VanillaB173.modernItem(id,0);
            var stack=StacksB173.toLegacy(new net.minecraft.world.item.ItemStack(nativeItem));assertNotNull(stack);assertEquals(id,stack.itemId);
            var armor=assertInstanceOf(ArmorItem.class,stack.getItem());assertSame(Item.ITEMS[id],armor);
            assertEquals(types[material],armor.type);assertEquals(material,armor.textureIndex);assertEquals(slot,armor.equipmentSlot);
            assertEquals(protection[slot],armor.maxProtection);assertEquals((durability[slot]*3)<<types[material],armor.getMaxDamage());
            assertEquals(1,armor.getMaxCount());assertNull(armor.mcc$owner);assertSame(nativeItem,armor.mcc$modern);
            for(int target=0;target<4;target++)assertEquals(target==slot,slots[target].canInsert(stack),"Original armor predicate for vanilla ID "+id+" at slot "+target);
        }
        var stick=StacksB173.toLegacy(new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.STICK));
        for(var slot:slots)assertFalse(slot.canInsert(stick));
        var pumpkin=new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(86,1,0);
        assertTrue(slots[0].canInsert(pumpkin));assertFalse(slots[1].canInsert(pumpkin));
    }
    @Test void allTwentyNativeArmorRoundtripsKeepDamageComponentsAndNativeAttributes(){
        for(int id=298;id<=317;id++){
            var item=VanillaB173.modernItem(id,0);var nativeStack=new net.minecraft.world.item.ItemStack(item);nativeStack.setDamageValue(7);
            nativeStack.set(DataComponents.CUSTOM_NAME,Component.literal("Native armor Ω "+id));
            var tag=new net.minecraft.nbt.CompoundTag();tag.putInt("nativeArmorFixture",id);
            nativeStack.set(DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));
            var attributes=nativeStack.get(DataComponents.ATTRIBUTE_MODIFIERS);int nativeDurability=nativeStack.getMaxDamage();
            var legacy=StacksB173.toLegacy(nativeStack);assertInstanceOf(ArmorItem.class,legacy.getItem());assertEquals(7,legacy.getDamage());
            var restored=StacksB173.toModern(legacy);assertSame(item,restored.getItem());assertEquals(nativeDurability,restored.getMaxDamage());
            assertEquals(attributes,restored.get(DataComponents.ATTRIBUTE_MODIFIERS));
            assertTrue(net.minecraft.world.item.ItemStack.isSameItemSameComponents(nativeStack,restored),"Complete native component bag for armor "+id);
        }
    }
    @Test void vanillaArmorFactoryNeverClaimsActiveOwnerOrChangesCanonicalItemTable(){
        var metadata=new com.google.gson.JsonObject();metadata.addProperty("modernId","fixture_armor_owner");
        var owner=new org.kylepitcock.legacyconnector.core.LegacyModRecord("b1.7.3","fixture_armor_owner","Fixture",null,null,metadata);
        var previous=ContentB173.current();int count=ContentB173.items().size();var canonical=Item.ITEMS[317];
        ContentB173.enter(owner);
        try{
            var armor=ArmorItem.mcc$vanilla(317,1,4,3);
            assertEquals(317,armor.id);assertEquals(1,armor.type);assertEquals(4,armor.textureIndex);assertEquals(3,armor.equipmentSlot);
            assertEquals(3,armor.maxProtection);assertEquals(78,armor.getMaxDamage());assertNull(armor.mcc$owner);
            assertSame(canonical,Item.ITEMS[317]);assertEquals(count,ContentB173.items().size());
        }finally{if(previous==null)ContentB173.exit();else ContentB173.enter(previous);}
    }
}
