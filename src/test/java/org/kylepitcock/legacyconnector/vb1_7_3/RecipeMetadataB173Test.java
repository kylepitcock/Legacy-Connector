package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.util.*;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.*;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.BlockItemStateProperties;
import net.minecraft.world.item.crafting.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RecipeMetadataB173Test {
    @BeforeAll static void bootstrap()throws Exception{
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();
    }
    private static ItemStack subtype(int metadata){
        var stack=new ItemStack(Items.PAPER);var tag=new CompoundTag();tag.putInt("b173_meta",metadata);
        tag.putString("unrelated","preserved");stack.set(DataComponents.CUSTOM_DATA,CustomData.of(tag));return stack;
    }
    @Test void implicitZeroAndEverySubtypeCompareOnlyTheLegacyInteger(){
        for(int expected=0;expected<=5;expected++){
            var ingredient=new BetaMetadataIngredient(Items.PAPER,expected);
            assertEquals(expected==0,ingredient.test(new ItemStack(Items.PAPER)));
            for(int actual=0;actual<=5;actual++){
                var stack=subtype(actual);stack.set(DataComponents.CUSTOM_NAME,net.minecraft.network.chat.Component.literal("Native name"));
                assertEquals(expected==actual,ingredient.test(stack));
            }
            assertFalse(ingredient.test(new ItemStack(Items.STICK)));
        }
    }
    @Test void subtypeNativeRecipeOutputsAndAssemblyKeepAllMetadataVariants(){
        for(int meta=1;meta<=5;meta++){
            var output=subtype(meta);output.setCount(4);
            JsonObject definition=new JsonObject();definition.addProperty("category","misc");
            var pattern=new com.google.gson.JsonArray();pattern.add("X");definition.add("pattern",pattern);
            var key=new JsonObject();var input=new JsonObject();input.addProperty("item","minecraft:stone");key.add("X",input);definition.add("key",key);
            definition.add("result",RecipesB173.result(output));
            var recipe=RecipeSerializer.SHAPED_RECIPE.codec().codec().parse(JsonOps.INSTANCE,definition).getOrThrow();
            var assembled=recipe.assemble(CraftingInput.of(1,1,List.of(new ItemStack(Items.STONE))),null);
            assertEquals(4,assembled.getCount());assertTrue(new BetaMetadataIngredient(Items.PAPER,meta).test(assembled));
            assertEquals("preserved",assembled.get(DataComponents.CUSTOM_DATA).copyTag().getString("unrelated"));
            assertFalse(new BetaMetadataIngredient(Items.PAPER,0).test(assembled));
            var decoded=ItemStack.CODEC.parse(JsonOps.INSTANCE,RecipesB173.result(assembled)).getOrThrow();
            assertTrue(ItemStack.isSameItemSameComponents(assembled,decoded));
        }
    }
    @Test void recipeOutputCodecAlsoRetainsBlockStateAndActualToolDamage(){
        var block=new ItemStack(Items.STONE,3);block.set(DataComponents.BLOCK_STATE,new BlockItemStateProperties(Map.of("meta","7")));
        var decoded=ItemStack.CODEC.parse(JsonOps.INSTANCE,RecipesB173.result(block)).getOrThrow();
        assertEquals("7",decoded.get(DataComponents.BLOCK_STATE).properties().get("meta"));assertEquals(3,decoded.getCount());
        var shovel=new ItemStack(Items.WOODEN_SHOVEL);shovel.setDamageValue(17);
        var repaired=ItemStack.CODEC.parse(JsonOps.INSTANCE,RecipesB173.result(shovel)).getOrThrow();
        assertEquals(17,repaired.getDamageValue());assertTrue(new BetaMetadataIngredient(Items.WOODEN_SHOVEL,17).test(repaired));
        assertFalse(new BetaMetadataIngredient(Items.WOODEN_SHOVEL,0).test(repaired));
    }
    @Test void ingredientCodecAndDisplayStackKeepTheSameMetadataPredicate(){
        var ingredient=new BetaMetadataIngredient(Items.PAPER,3);
        var encoded=BetaMetadataIngredient.CODEC.codec().encodeStart(JsonOps.INSTANCE,ingredient).getOrThrow();
        var decoded=BetaMetadataIngredient.CODEC.codec().parse(JsonOps.INSTANCE,encoded).getOrThrow();
        assertEquals(ingredient,decoded);assertFalse(decoded.isSimple());
        assertTrue(decoded.test(decoded.getItems().findFirst().orElseThrow()));assertTrue(decoded.test(subtype(3)));assertFalse(decoded.test(subtype(4)));
    }
    private static final class SubtypeItem extends legacyconnector.vb1_7_3.net.minecraft.item.Item{
        SubtypeItem(){super(31900-256);setHasSubtypes(true);mcc$owner=new Object();mcc$modern=Items.PAPER;}
    }
    @Test void registeredSubtypeInputsRequireExactMetadataButWildcardRepairRemainsBroad(){
        var previous=legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS[31900];int[] mark=ContentB173.mark();
        try{
            var item=new SubtypeItem();
            for(int meta=0;meta<=5;meta++){
                var ingredient=RecipesB173.stackIngredient(item.id,meta).getAsJsonObject();
                assertEquals("legacyconnector:beta_metadata",ingredient.get("type").getAsString());assertEquals(meta,ingredient.get("metadata").getAsInt());
            }
            var wildcard=RecipesB173.stackIngredient(item.id,-1).getAsJsonObject();assertFalse(wildcard.has("type"));assertEquals("minecraft:paper",wildcard.get("item").getAsString());
            var exactTool=RecipesB173.stackIngredient(269,0).getAsJsonObject();assertEquals("legacyconnector:beta_metadata",exactTool.get("type").getAsString());assertEquals(0,exactTool.get("metadata").getAsInt());
            var anyTool=RecipesB173.stackIngredient(269,-1).getAsJsonObject();assertFalse(anyTool.has("type"));
            assertEquals("minecraft:red_wool",RecipesB173.stackIngredient(35,14).getAsJsonObject().get("item").getAsString());
        }finally{ContentB173.release(mark);legacyconnector.vb1_7_3.net.minecraft.item.Item.ITEMS[31900]=previous;}
    }
}
