package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HexFormat;
import java.util.Map;
import java.util.function.Supplier;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import org.kylepitcock.legacyconnector.core.LegacyModRecord;
import org.kylepitcock.legacyconnector.core.LegacyMods;
import org.kylepitcock.legacyconnector.core.pack.GeneratedPack;

/** Original entity PNGs are resources in the native pack, including conditional renderer texture choices. */
public final class MobTexturesB173 implements GeneratedPack.Contributor {
    /** Vanilla Beta's powered-creeper layer, renamed when entity textures were reorganized. */
    private static final Map<String,String> VANILLA=Map.ofEntries(Map.entry("armor/power.png","textures/entity/creeper/creeper_armor.png"),
            Map.entry("mob/saddle.png","textures/entity/pig/pig_saddle.png"),
            // base-game mob sheets a mod's subclass keeps (same UV layout as the modern models)
            Map.entry("mob/cow.png","textures/entity/cow/cow.png"),Map.entry("mob/pig.png","textures/entity/pig/pig.png"),
            Map.entry("mob/chicken.png","textures/entity/chicken.png"),Map.entry("mob/sheep.png","textures/entity/sheep/sheep.png"),
            Map.entry("mob/sheep_fur.png","textures/entity/sheep/sheep_fur.png"),
            Map.entry("mob/spider.png","textures/entity/spider/spider.png"),Map.entry("mob/spider_eyes.png","textures/entity/spider_eyes.png"),
            Map.entry("mob/creeper.png","textures/entity/creeper/creeper.png"),Map.entry("mob/wolf.png","textures/entity/wolf/wolf.png"),
            Map.entry("mob/wolf_angry.png","textures/entity/wolf/wolf_angry.png"),Map.entry("mob/wolf_tame.png","textures/entity/wolf/wolf_tame.png"));
    private static String path(String name){
        if(name==null)return null;String p=name.replace('\\','/');while(p.startsWith("/"))p=p.substring(1);
        if(p.isEmpty()||p.contains(":")||java.util.Arrays.asList(p.split("/")).contains(".."))return null;return p;
    }
    private static ResourceLocation id(LegacyModRecord mod,String path){return ResourceLocation.fromNamespaceAndPath(mod.modernId,"textures/b173/entity/"+HexFormat.of().formatHex(path.getBytes(StandardCharsets.UTF_8))+".png");}
    public static ResourceLocation resolve(LegacyModRecord mod,String name){
        String p=path(name);if(p==null)return ResourceLocation.withDefaultNamespace("missingno");
        if(mod!=null&&Files.isRegularFile(mod.container.getModInfo().getOwningFile().getFile().findResource(p.split("/"))))return id(mod,p);
        for(var other:LegacyMods.all())if(PlatformB173.VERSION.equals(other.version)&&Files.isRegularFile(other.container.getModInfo().getOwningFile().getFile().findResource(p.split("/"))))return id(other,p);
        String vanilla=VANILLA.get(p);if(vanilla!=null)return ResourceLocation.withDefaultNamespace(vanilla);
        if(GUI_SHEETS.contains(p))return ResourceLocation.fromNamespaceAndPath("legacyconnector","textures/b173/gui/"+p.substring("gui/".length()));
        return ResourceLocation.withDefaultNamespace("missingno");
    }
    /** Vanilla GUI sheets a renderer may bind by their b1.7.3 path (the HUD of a mod's frame callback); the pack carries them. */
    private static final java.util.Set<String> GUI_SHEETS=java.util.Set.of("gui/icons.png","gui/gui.png","gui/container.png","gui/crafting.png","gui/trap.png","gui/inventory.png","gui/furnace.png");
    @Override public void contribute(PackType type,Map<String,Supplier<byte[]>> out){
        if(type!=PackType.CLIENT_RESOURCES)return;
        // the vanilla sheets come from the client's resource manager; a dedicated server (the self test) must not load client classes
        if(net.neoforged.fml.loading.FMLEnvironment.dist.isClient())for(var sheet:org.kylepitcock.legacyconnector.vb1_7_3.client.ClientGuiB173.guiSheets().entrySet()){
            var bytes=new java.io.ByteArrayOutputStream();
            try{javax.imageio.ImageIO.write(sheet.getValue(),"png",bytes);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}
            byte[] data=bytes.toByteArray();out.put("assets/legacyconnector/textures/b173/gui/"+sheet.getKey(),()->data);
        }
        for(var mod:LegacyMods.all())if(PlatformB173.VERSION.equals(mod.version)){
            var root=mod.container.getModInfo().getOwningFile().getFile().getSecureJar().getRootPath();
            try(var files=Files.walk(root)){
                for(var file:files.filter(Files::isRegularFile).filter(p->p.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".png")).toList()){
                    String p=root.relativize(file).toString().replace('\\','/');var location=id(mod,p);
                    out.put("assets/"+location.getNamespace()+"/"+location.getPath(),()->{try{return Files.readAllBytes(file);}catch(java.io.IOException e){throw new java.io.UncheckedIOException(e);}});
                }
            }catch(java.io.IOException e){throw new java.io.UncheckedIOException("Cannot expose Beta mob textures of "+mod.modernId,e);}
        }
    }
}
