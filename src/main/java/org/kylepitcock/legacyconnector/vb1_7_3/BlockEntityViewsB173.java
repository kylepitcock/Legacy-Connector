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
    private record StackState(Object identity,int id,int count,int damage){}
    private static final Map<BlockEntity,StackState[]> CHEST_BASELINES=java.util.Collections.synchronizedMap(new java.util.WeakHashMap<>());
    private static StackState state(legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack){return stack==null?new StackState(null,0,0,0):new StackState(stack,stack.itemId,stack.count,stack.getDamage());}
    private static void remember(legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity chest){var states=new StackState[27];for(int i=0;i<states.length;i++)states[i]=state(chest.getStack(i));CHEST_BASELINES.put(chest,states);}
    private BlockEntityViewsB173(){}
    public static BlockEntity wrap(net.minecraft.world.level.block.entity.BlockEntity nativeEntity){
        if(nativeEntity instanceof net.minecraft.world.level.block.entity.ChestBlockEntity chest){
            var previous=TOUCHED.get().get(nativeEntity);if(previous!=null)return previous;
            var view=new legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity();attachChest(view,chest);
            for(int i=0;i<chest.getContainerSize();i++)view.inventory[i]=StacksB173.toLegacy(chest.getItem(i));remember(view);return view;
        }
        if(nativeEntity instanceof net.minecraft.world.level.block.entity.SpawnerBlockEntity spawner)
            return TOUCHED.get().computeIfAbsent(nativeEntity,ignored->{var view=new legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity();var pos=spawner.getBlockPos();view.x=pos.getX();view.y=pos.getY();view.z=pos.getZ();view.world=World.mcc$wrap(spawner.getLevel());view.mcc$host=spawner;return view;});
        if(!(nativeEntity instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign))return null;
        return TOUCHED.get().computeIfAbsent(nativeEntity,ignored->{var view=new SignBlockEntity();var pos=sign.getBlockPos();view.x=pos.getX();view.y=pos.getY();view.z=pos.getZ();view.world=World.mcc$wrap(sign.getLevel());view.mcc$host=sign;for(int i=0;i<4;i++)view.texts[i]=sign.getFrontText().getMessage(i,false).getString();return view;});
    }
    public static void flush(BlockEntity view){
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity chest&&view.mcc$host instanceof net.minecraft.world.level.block.entity.ChestBlockEntity nativeChest){
            var before=CHEST_BASELINES.get(chest);boolean changed=false;
            var nextItems=new net.minecraft.world.item.ItemStack[nativeChest.getContainerSize()];
            for(int i=0;i<nativeChest.getContainerSize();i++){
                nextItems[i]=nativeChest.getItem(i);
                var now=state(chest.getStack(i));if(before!=null&&now.equals(before[i]))continue;
                if(before!=null&&now.identity()!=null&&now.identity()==before[i].identity()&&now.id()==before[i].id()&&now.damage()==before[i].damage()&&nextItems[i].getItem()==StacksB173.modernItem(now.id(),now.damage()))nextItems[i]=nextItems[i].copyWithCount(now.count());
                else nextItems[i]=StacksB173.toModern(chest.getStack(i));
                changed=true;
            }
            if(changed){
                if(WorldGenWritesB173.generation()){
                    var level=nativeChest.getLevel()!=null?nativeChest.getLevel():view.world==null?null:view.world.mcc$level();
                    if(level!=null){var lookup=level.registryAccess();var nbt=nativeChest.saveWithFullMetadata(lookup);var items=new net.minecraft.nbt.ListTag();
                        for(int i=0;i<nextItems.length;i++)if(!nextItems[i].isEmpty()){var item=(net.minecraft.nbt.CompoundTag)nextItems[i].save(lookup);item.putByte("Slot",(byte)i);items.add(item);}nbt.put("Items",items);nativeChest.loadWithComponents(nbt,lookup);
                    }
                }else {for(int i=0;i<nextItems.length;i++)if(nextItems[i]!=nativeChest.getItem(i))nativeChest.setItem(i,nextItems[i]);nativeChest.setChanged();}
            }remember(chest);return;
        }
        if(view instanceof legacyconnector.vb1_7_3.net.minecraft.block.entity.MobSpawnerBlockEntity spawner){spawner.mcc$apply();return;}
        if(view instanceof SignBlockEntity legacy&&view.mcc$host instanceof net.minecraft.world.level.block.entity.SignBlockEntity sign&&sign.getLevel()!=null&&!sign.getLevel().isClientSide){
            var text=sign.getFrontText();boolean changed=false;for(int i=0;i<4;i++){String line=legacy.texts[i]==null?"":legacy.texts[i];if(!line.equals(text.getMessage(i,false).getString())){text=text.setMessage(i,Component.literal(line));changed=true;}}
            if(changed){sign.setText(text,true);sign.setChanged();var state=sign.getBlockState();sign.getLevel().sendBlockUpdated(sign.getBlockPos(),state,state,3);}
        }
    }
    /** setBlockTileEntity may install a newly constructed chest before filling its inventory. */
    public static void attachChest(legacyconnector.vb1_7_3.net.minecraft.block.entity.ChestBlockEntity view,net.minecraft.world.level.block.entity.ChestBlockEntity chest){
        CHEST_BASELINES.remove(view);var pos=chest.getBlockPos();view.x=pos.getX();view.y=pos.getY();view.z=pos.getZ();if(chest.getLevel()!=null)view.world=World.mcc$wrap(chest.getLevel());view.mcc$host=chest;TOUCHED.get().put(chest,view);
    }
    public static void flush(){try{TOUCHED.get().values().forEach(BlockEntityViewsB173::flush);}finally{TOUCHED.remove();}}
}
