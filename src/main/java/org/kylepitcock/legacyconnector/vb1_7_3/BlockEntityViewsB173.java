package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.IdentityHashMap;
import java.util.Map;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.BlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.block.entity.SignBlockEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.network.chat.Component;

/** Callback-scoped writable views of vanilla block entities. */
public final class BlockEntityViewsB173 {
    private static final ThreadLocal<Map<net.minecraft.world.level.block.entity.BlockEntity,BlockEntity>> TOUCHED=ThreadLocal.withInitial(IdentityHashMap::new);
    private BlockEntityViewsB173(){}
    public static BlockEntity wrap(net.minecraft.world.level.block.entity.BlockEntity nativeEntity){
        if(nativeEntity instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)
            return TOUCHED.get().computeIfAbsent(nativeEntity,ignored->{var view=new legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity();var pos=spawner.getBlockPos();view.x=pos.getX();view.y=pos.getY();view.z=pos.getZ();view.world=World.mcc$wrap(spawner.getLevel());view.mcc$host=spawner;return view;});
        if(!(nativeEntity instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign))return null;
        return TOUCHED.get().computeIfAbsent(nativeEntity,ignored->{var view=new SignBlockEntity();var pos=sign.getBlockPos();view.x=pos.getX();view.y=pos.getY();view.z=pos.getZ();view.world=World.mcc$wrap(sign.getLevel());view.mcc$host=sign;for(int i=0;i<4;i++)view.texts[i]=sign.getFrontText().getMessage(i,false).getString();return view;});
    }
    public static void flush(BlockEntity view){
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity spawner){spawner.mcc$apply();return;}
        if(view instanceof SignBlockEntity legacy&&view.mcc$host instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign&&sign.getLevel()!=null&&!sign.getLevel().isClientSide){
            var text=sign.getFrontText();boolean changed=false;for(int i=0;i<4;i++){String line=legacy.texts[i]==null?"":legacy.texts[i];if(!line.equals(text.getMessage(i,false).getString())){text=text.setMessage(i,Component.literal(line));changed=true;}}
            if(changed){sign.setText(text,true);sign.setChanged();var state=sign.getBlockState();sign.getLevel().sendBlockUpdated(sign.getBlockPos(),state,state,3);}
        }
    }
    public static void flush(){try{TOUCHED.get().values().forEach(BlockEntityViewsB173::flush);}finally{TOUCHED.remove();}}
}
