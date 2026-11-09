package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.item.*;
import legacyconnector.vb1_7_3.net.minecraft.entity.player.PlayerEntity;
import org.junit.jupiter.api.*;

class FoodUseB173Test {
    private int[] mark;
    private Item beforeFood,beforeRemainder;
    @BeforeAll static void bootstrap()throws Exception{
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    @BeforeEach void before(){mark=ContentB173.mark();beforeFood=Item.ITEMS[31910];beforeRemainder=Item.ITEMS[31911];Item.ITEMS[31910]=null;Item.ITEMS[31911]=null;}
    @AfterEach void after(){ContentB173.release(mark);Item.ITEMS[31910]=beforeFood;Item.ITEMS[31911]=beforeRemainder;}
    private static PlayerEntity actor(){var player=new PlayerEntity(null){};player.health=2;return player;}
    private static class PlainItem extends Item{PlainItem(){super(31911-256);}}
    @Test void nativeFoodPropertiesUseOriginalNutritionSaturationAndThirtyTwoTicks(){
        for(int points:new int[]{-1,0,3,4,8,20,50,Integer.MAX_VALUE}){
            Item.ITEMS[31910]=null;
            var properties=FoodUseB173.properties(new FoodItem(31910-256,points,false));int nutrition=Math.max(0,Math.min(20,points));
            assertEquals(nutrition,properties.nutrition());assertEquals(nutrition*1.2F,properties.saturation(),0.00001F);
            assertEquals(32,properties.eatDurationTicks());assertFalse(properties.canAlwaysEat());assertTrue(properties.usingConvertsTo().isEmpty());
        }
        var data=new net.minecraft.world.food.FoodData();data.setFoodLevel(4);data.setSaturation(0);
        Item.ITEMS[31910]=null;
        data.eat(FoodUseB173.properties(new FoodItem(31910-256,8,true)));assertEquals(12,data.getFoodLevel());assertEquals(9.6F,data.getSaturationLevel(),0.00001F);
    }
    @Test void originalCallbackRunsOnceConsumesOnceAndKeepsCustomRemainderInHungerMode(){
        int[] calls={0};var remainder=new PlainItem();var player=actor();
        var food=new FoodItem(31910-256,8,true){
            @Override public ItemStack use(ItemStack stack,legacyconnector.vb1_7_3.net.minecraft.world.World world,PlayerEntity actor){
                calls[0]++;super.use(stack,world,actor);return new ItemStack(remainder,1,7);
            }
        };
        var held=new ItemStack(food);var result=FoodUseB173.invoke(food,held,null,player,true);
        assertEquals(1,calls[0]);assertEquals(0,held.count);assertEquals(remainder.id,result.itemId);assertEquals(1,result.count);assertEquals(7,result.getDamage());assertEquals(2,player.health);
        assertFalse(FoodUseB173.suppressesHealing(player,1));player.heal(3);assertEquals(5,player.health);
    }
    @Test void originalBetaModeStillHealsInstantlyAndRetainsStackIdentity(){
        var player=actor();var food=new FoodItem(31910-256,4,false);var held=new ItemStack(food,2);
        assertSame(held,FoodUseB173.invoke(food,held,null,player,false));assertEquals(1,held.count);assertEquals(6,player.health);
    }
    @Test void healingSuppressionIsActorScopedNestedAndPositiveOnly(){
        var first=actor();var second=actor();
        try(var outer=FoodUseB173.withoutHealing(first)){
            assertTrue(FoodUseB173.suppressesHealing(first,1));assertFalse(FoodUseB173.suppressesHealing(second,1));assertFalse(FoodUseB173.suppressesHealing(first,-1));
            first.heal(3);second.heal(3);assertEquals(2,first.health);assertEquals(5,second.health);
            try(var inner=FoodUseB173.withoutHealing(second)){assertTrue(FoodUseB173.suppressesHealing(first,1));assertTrue(FoodUseB173.suppressesHealing(second,1));}
            assertTrue(FoodUseB173.suppressesHealing(first,1));assertFalse(FoodUseB173.suppressesHealing(second,1));
        }
        assertFalse(FoodUseB173.suppressesHealing(first,1));first.heal(1);assertEquals(3,first.health);
    }
    @Test void plainMedicineAndOtherActorHealingRemainLiveAndFailuresReleaseScope(){
        var player=actor();var other=actor();
        var medicine=new PlainItem(){@Override public ItemStack use(ItemStack stack,legacyconnector.vb1_7_3.net.minecraft.world.World world,PlayerEntity actor){actor.heal(3);return stack;}};
        FoodUseB173.invoke(medicine,new ItemStack(medicine),null,player,true);assertEquals(5,player.health);
        var food=new FoodItem(31910-256,3,false){@Override public ItemStack use(ItemStack stack,legacyconnector.vb1_7_3.net.minecraft.world.World world,PlayerEntity actor){other.heal(2);actor.heal(2);throw new IllegalStateException("fixture");}};
        assertThrows(IllegalStateException.class,()->FoodUseB173.invoke(food,new ItemStack(food),null,player,true));
        assertEquals(4,other.health);assertEquals(5,player.health);assertFalse(FoodUseB173.suppressesHealing(player,1));
    }
}
