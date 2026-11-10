package org.kylepitcock.legacyconnector.vb1_7_3;

import static org.junit.jupiter.api.Assertions.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class VanillaItemCoverageB173Test {
    @BeforeAll static void bootstrap() throws Exception {
        var dist=net.neoforged.fml.loading.FMLLoader.class.getDeclaredField("dist");dist.setAccessible(true);
        dist.set(null,net.neoforged.api.distmarker.Dist.CLIENT);
        net.neoforged.fml.loading.LoadingModList.of(List.of(),List.of(),List.of(),List.of(),Map.of());
        net.minecraft.SharedConstants.tryDetectVersion();net.minecraft.server.Bootstrap.bootStrap();VanillaB173.init();
    }
    private record Original(String field,int id){}
    private static List<Original> originalItems() throws Exception {
        var items=new ArrayList<Original>();
        try(var stream=VanillaB173.class.getResourceAsStream("/legacyconnector/b1.7.3/vanilla-ids.tsv")){
            assertNotNull(stream);
            try(var reader=new BufferedReader(new InputStreamReader(stream,StandardCharsets.UTF_8))){
                for(String line;(line=reader.readLine())!=null;){
                    String[] fields=line.split("\t");
                    if(fields[0].equals("item"))items.add(new Original(fields[1],Integer.parseInt(fields[2])));
                }
            }
        }return items;
    }
    private static void mapping(int id,int meta,String expected){
        var key=ResourceLocation.withDefaultNamespace(expected);
        assertTrue(BuiltInRegistries.ITEM.containsKey(key),"Native registry item "+key);
        var item=VanillaB173.modernItem(id,meta);
        assertNotNull(item,"Missing Beta item "+id+":"+meta);assertNotSame(Items.AIR,item);
        assertEquals(key,BuiltInRegistries.ITEM.getKey(item),"Wrong Beta item "+id+":"+meta);
        var original=new legacyconnector.vb1_7_3.net.minecraft.item.ItemStack(id,1,meta);
        var modern=StacksB173.toModern(original);assertFalse(modern.isEmpty());assertSame(item,modern.getItem());
        var reversed=StacksB173.toLegacy(new net.minecraft.world.item.ItemStack(item));
        assertNotNull(reversed);assertEquals(id,reversed.itemId,"Canonical reverse ID for "+key);
        assertEquals(meta,reversed.getDamage(),"Canonical reverse metadata for "+key);
        assertSame(item,StacksB173.toModern(reversed).getItem());
    }
    @Test void all106OriginalItemRowsHaveRealNativeItemsAndCanonicalReverseIds() throws Exception {
        var renamed=Map.ofEntries(
                Map.entry("SEEDS","wheat_seeds"),Map.entry("CHAIN_HELMET","chainmail_helmet"),Map.entry("CHAIN_CHESTPLATE","chainmail_chestplate"),
                Map.entry("CHAIN_LEGGINGS","chainmail_leggings"),Map.entry("CHAIN_BOOTS","chainmail_boots"),Map.entry("RAW_PORKCHOP","porkchop"),
                Map.entry("SIGN","oak_sign"),Map.entry("WOODEN_DOOR","oak_door"),Map.entry("BOAT","oak_boat"),Map.entry("CLAY","clay_ball"),
                Map.entry("RAW_FISH","cod"),Map.entry("COOKED_FISH","cooked_cod"),Map.entry("DYE","ink_sac"),Map.entry("BED","red_bed"),
                Map.entry("MAP","filled_map"),Map.entry("SLIMEBALL","slime_ball"),Map.entry("RECORD_THIRTEEN","music_disc_13"),Map.entry("RECORD_CAT","music_disc_cat"));
        var items=originalItems();assertEquals(106,items.size());
        assertTrue(items.stream().anyMatch(item->item.id()==2256));assertTrue(items.stream().anyMatch(item->item.id()==2257));
        assertAll(items.stream().<org.junit.jupiter.api.function.Executable>map(item->()->mapping(item.id(),0,renamed.getOrDefault(item.field(),item.field().toLowerCase(Locale.ROOT)))));
    }
    @Test void everyCoalDyeAndFlattenedBlockItemSubtypeRetainsIdentityAndCanonicalMetadata(){
        mapping(263,1,"charcoal");
        String[] dyes={"ink_sac","red_dye","green_dye","cocoa_beans","lapis_lazuli","purple_dye","cyan_dye","light_gray_dye","gray_dye","pink_dye","lime_dye","yellow_dye","light_blue_dye","magenta_dye","orange_dye","bone_meal"};
        String[] colors={"white","orange","magenta","light_blue","yellow","lime","pink","gray","light_gray","cyan","purple","blue","brown","green","red","black"};
        for(int meta=0;meta<16;meta++){mapping(351,meta,dyes[meta]);mapping(35,meta,colors[meta]+"_wool");}
        String[] woods={"oak","spruce","birch"};
        for(int meta=0;meta<3;meta++){mapping(6,meta,woods[meta]+"_sapling");mapping(17,meta,woods[meta]+"_log");mapping(18,meta,woods[meta]+"_leaves");}
        String[] slabs={"smooth_stone_slab","sandstone_slab","oak_slab","cobblestone_slab"};
        for(int meta=0;meta<4;meta++)mapping(44,meta,slabs[meta]);
        mapping(31,0,"dead_bush");mapping(31,1,"short_grass");mapping(31,2,"fern");
    }
    @Test void blockStateOnlyAliasesAreExcludedFromTheTrueItemAudit(){
        for(int id:new int[]{8,9,10,11,51,90})assertNull(VanillaB173.modernItem(id,0),"Block-only ID "+id);
    }
}
