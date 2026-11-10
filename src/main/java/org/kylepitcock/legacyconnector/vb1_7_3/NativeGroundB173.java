package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.IdentityHashMap;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import legacyconnector.vb1_7_3.net.minecraft.world.World;

/** Native grounding also queries supporting-block collisions, so it must not run on a generation worker. */
public final class NativeGroundB173 {
    private static final Writes<net.minecraft.world.entity.Entity> WRITES=new Writes<>();
    private static boolean installed;
    private NativeGroundB173(){}
    public static void commit(net.minecraft.world.entity.Entity entity,boolean desired){
        if(entity.level() instanceof net.minecraft.server.level.ServerLevel level){
            boolean defer=World.mcc$generationActive()&&!level.getServer().isSameThread();
            if(defer)install();
            // Capture values and native identity only. No original view or original callback crosses threads.
            WRITES.commit(entity,desired,defer,entity::onGround,value->{
                if(entity.level()==level&&!entity.isRemoved())entity.setOnGround(value);
            },level.getServer()::execute);
        }else if(entity.onGround()!=desired)entity.setOnGround(desired);
    }
    private static synchronized void install(){
        if(installed)return;installed=true;
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(
                (net.neoforged.neoforge.event.server.ServerStoppedEvent event)->WRITES.removeIf(entity->
                        entity.level() instanceof net.minecraft.server.level.ServerLevel level&&level.getServer()==event.getServer()));
    }
    /** Small value-only queue, also exercised without loading a native level or entity. */
    static final class Writes<K> {
        private static final class Pending {boolean desired;Pending(boolean desired){this.desired=desired;}}
        private final IdentityHashMap<K,Pending> pending=new IdentityHashMap<>();
        void commit(K key,boolean desired,boolean defer,BooleanSupplier current,Consumer<Boolean> apply,Consumer<Runnable> enqueue){
            if(!defer){
                synchronized(pending){pending.remove(key);}
                if(current.getAsBoolean()!=desired)apply.accept(desired);
                return;
            }
            final Pending write;
            synchronized(pending){
                var existing=pending.get(key);
                if(existing!=null){existing.desired=desired;return;}
                if(current.getAsBoolean()==desired)return;
                write=new Pending(desired);pending.put(key,write);
            }
            enqueue.accept(()->{
                boolean next;
                synchronized(pending){
                    if(pending.get(key)!=write)return;
                    pending.remove(key);next=write.desired;
                }
                if(current.getAsBoolean()!=next)apply.accept(next);
            });
        }
        void removeIf(java.util.function.Predicate<K> predicate){synchronized(pending){pending.keySet().removeIf(predicate);}}
    }
}
