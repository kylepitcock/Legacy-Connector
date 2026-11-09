package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;

/** ModLoader armor texture prefixes become native armor-material layers using the original PNG bytes. */
public final class ArmorMaterialsB173 {
    private static boolean installed;
    private ArmorMaterialsB173(){}
    static synchronized void install(){
        if(installed)return;installed=true;
        org.kylepitcock.legacyconnector.core.pack.GeneratedPack.addContributor((pack,out)->{
            if(pack!=net.minecraft.server.packs.PackType.CLIENT_RESOURCES)return;
            for(var item:ContentB173.items())if(item instanceof legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem armor
                    &&armor.mcc$modern instanceof LegacyB173ArmorItem){
                String prefix=ContentB173.armorTexture(armor.textureIndex);
                var owner=armor.mcc$owner instanceof LegacyModRecord mod?mod:null;
                var asset=asset(owner==null?"legacyconnector":owner.modernId,prefix);
                for(int layer=1;layer<=2;layer++){
                    String original="armor/"+prefix+"_"+layer+".png";
                    java.nio.file.Path source=null;
                    if(owner!=null){var path=owner.container.getModInfo().getOwningFile().getFile().findResource(original.split("/"));if(Files.isRegularFile(path))source=path;}
                    if(source==null)for(var mod:org.kylepitcock.legacyconnector.core.LegacyMods.all())if(PlatformB173.VERSION.equals(mod.version)){
                        var path=mod.container.getModInfo().getOwningFile().getFile().findResource(original.split("/"));if(Files.isRegularFile(path)){source=path;break;}
                    }
                    if(source!=null){var file=source;var texture=texture(asset,layer);out.put("assets/"+texture.getNamespace()+"/"+texture.getPath(),()->{
                        try{return Files.readAllBytes(file);}catch(java.io.IOException error){throw new java.io.UncheckedIOException(error);}
                    });}
                }
            }
        });
    }
    static ArmorItem.Type type(int slot){return switch(slot){
        case 0->ArmorItem.Type.HELMET;case 1->ArmorItem.Type.CHESTPLATE;case 2->ArmorItem.Type.LEGGINGS;case 3->ArmorItem.Type.BOOTS;
        default->throw new IllegalArgumentException("Beta armor slot "+slot);
    };}
    static ResourceLocation asset(String namespace,String prefix){
        return ResourceLocation.fromNamespaceAndPath(namespace,"beta/"+HexFormat.of().formatHex(prefix.getBytes(StandardCharsets.UTF_8)));
    }
    static ResourceLocation texture(ResourceLocation asset,int layer){
        return asset.withPath(path->"textures/models/armor/"+path+"_layer_"+layer+".png");
    }
    static Holder<ArmorMaterial> material(legacyconnector.vb1_7_3.net.minecraft.item.ArmorItem armor){
        String prefix=ContentB173.armorTexture(armor.textureIndex);
        String namespace=armor.mcc$owner instanceof LegacyModRecord mod?mod.modernId:"legacyconnector";
        ResourceLocation asset=asset(namespace,prefix);
        // Built-in Beta prefixes use the native equivalents if no mod supplied a replacement PNG.
        String vanilla=switch(prefix){case "cloth"->"leather";case "chain"->"chainmail";case "iron","diamond"->prefix;case "gold"->"gold";default->null;};
        if(vanilla!=null){
            boolean replacement=false;
            for(var mod:org.kylepitcock.legacyconnector.core.LegacyMods.all())if(PlatformB173.VERSION.equals(mod.version)
                    &&Files.isRegularFile(mod.container.getModInfo().getOwningFile().getFile().findResource("armor",prefix+"_1.png"))){replacement=true;break;}
            if(!replacement)asset=ResourceLocation.withDefaultNamespace(vanilla);
        }
        return Holder.direct(new ArmorMaterial(Map.of(type(armor.equipmentSlot),armor.maxProtection),0,
                net.minecraft.sounds.SoundEvents.ARMOR_EQUIP_GENERIC,()->net.minecraft.world.item.crafting.Ingredient.EMPTY,
                List.of(new ArmorMaterial.Layer(asset)),0,0));
    }
}
