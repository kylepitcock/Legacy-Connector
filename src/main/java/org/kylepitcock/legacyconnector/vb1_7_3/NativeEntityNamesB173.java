package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.entity.Entity;
import net.minecraft.core.registries.BuiltInRegistries;

/** Canonical Beta registry names for ordinary native entities represented by generic compatibility views. */
public final class NativeEntityNamesB173 {
    private static final Map<String,String> NAMES=Map.ofEntries(
            Map.entry("arrow","Arrow"),Map.entry("snowball","Snowball"),Map.entry("item","Item"),Map.entry("painting","Painting"),
            Map.entry("creeper","Creeper"),Map.entry("skeleton","Skeleton"),Map.entry("spider","Spider"),Map.entry("giant","Giant"),
            Map.entry("zombie","Zombie"),Map.entry("slime","Slime"),Map.entry("ghast","Ghast"),Map.entry("zombified_piglin","PigZombie"),
            Map.entry("pig","Pig"),Map.entry("sheep","Sheep"),Map.entry("cow","Cow"),Map.entry("chicken","Chicken"),
            Map.entry("squid","Squid"),Map.entry("wolf","Wolf"),Map.entry("tnt","PrimedTnt"),Map.entry("falling_block","FallingSand"),
            Map.entry("minecart","Minecart"),Map.entry("chest_minecart","Minecart"),Map.entry("furnace_minecart","Minecart"),Map.entry("boat","Boat"));
    private NativeEntityNamesB173(){}
    public static String ordinary(Entity view){
        // Only anonymous views created by our ordinary-native adapter need this fallback. Unknown original
        // subclasses retain exact-class registry semantics, even if somebody attaches a modern counterpart.
        if(view.getClass().getEnclosingClass()!=EntityViewsB173.class||view.mcc$modern==null)return null;
        var type=view.mcc$modern.getType();if(type==null)return null;
        var id=BuiltInRegistries.ENTITY_TYPE.getKey(type);return id==null?null:alias(id.toString());
    }
    static String alias(String nativeId){
        if(nativeId==null||!nativeId.startsWith("minecraft:"))return null;
        return NAMES.get(nativeId.substring("minecraft:".length()));
    }
}
