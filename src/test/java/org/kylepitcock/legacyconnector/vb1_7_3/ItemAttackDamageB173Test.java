package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import legacyconnector.vb1_7_3.net.minecraft.item.Item;
import legacyconnector.vb1_7_3.net.minecraft.src.ItemCustomMaterialTool;
import legacyconnector.vb1_7_3.net.minecraft.block.material.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ItemAttackDamageB173Test {
    private Item previousItem;
    private int[] contentMark;
    @BeforeEach void before() { previousItem=Item.ITEMS[31000];contentMark=ContentB173.mark(); }
    @AfterEach void after() { ContentB173.release(contentMark);Item.ITEMS[31000]=previousItem; }

    private static class PlainItem extends Item { PlainItem(){super(31000-256);} }

    @Test void baseItemKeepsNativeBaseDamage() {
        var item=new PlainItem();
        assertEquals(1,item.getAttackDamage(null));
        assertEquals(0F,LegacyB173Item.attackDamageBonus(item,null,1F));
        assertEquals(0F,LegacyB173Item.attackDamageBonus(item,null,0F));
    }

    @Test void actualTargetAndVirtualOverrideDetermineDamage() {
        var target=new Entity(null){};
        var other=new Entity(null){};
        var item=new PlainItem(){@Override public int getAttackDamage(Entity entity){return entity==target?9:3;}};
        assertEquals(8F,LegacyB173Item.attackDamageBonus(item,target,1F));
        assertEquals(2F,LegacyB173Item.attackDamageBonus(item,other,1F));
    }

    @Test void sharedMaterialApiSuppliesTotalDamageWithoutAddingBaseTwice() {
        // Actual API constructor: attack plus material damage. Native player already contributes one.
        for(int materialDamage:new int[]{0,1,2,3,5}) {
            var item=new ItemCustomMaterialTool(31000-256,4,0,32,4F,materialDamage,false,new Material[0]);
            assertEquals(4+materialDamage,item.getAttackDamage(null));
            assertEquals(4+materialDamage,1F+LegacyB173Item.attackDamageBonus(item,null,1F));
        }
    }

    @Test void nativeCooldownAndOtherDamageContributionsRemainIndependent() {
        var item=new PlainItem(){@Override public int getAttackDamage(Entity target){return 9;}};
        for(float strength:new float[]{0F,0.5F,1F}) {
            float scale=0.2F+strength*strength*0.8F;
            float nativeAttributes=4F*scale; // one base point plus three points from other native attributes
            float enchantment=2F*strength;
            float combined=nativeAttributes+LegacyB173Item.attackDamageBonus(item,null,strength)+enchantment;
            assertEquals(12F*scale+enchantment,combined,0.00001F);
        }
    }
}
