package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.attachment.IAttachmentSerializer;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/** Native player save/load and tick ownership for registered Beta PlayerAPI extensions. */
public final class PlayerApiLifecycleB173 {
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS=DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES,"legacyconnector");
    private static final java.util.function.Supplier<AttachmentType<Data>> DATA=ATTACHMENTS.register("b173_player_api",()->AttachmentType.<Data>builder(holder->new Data((Player)holder))
        .serialize(new IAttachmentSerializer<CompoundTag,Data>(){
            @Override public Data read(IAttachmentHolder holder,CompoundTag tag,HolderLookup.Provider provider){
                Data data=new Data((Player)holder);data.pending=tag.copy();data.lastGood=tag.copy();return data;
            }
            @Override public CompoundTag write(Data data,HolderLookup.Provider provider){return save(data);}
        }).build());
    public static final class Data {
        private final Player player;
        private CompoundTag pending,lastGood;
        private boolean saving;
        private boolean releaseAfterSave;
        private boolean dead;
        private int lastTick=Integer.MIN_VALUE;
        private final Set<String> reported=new HashSet<>();
        private Data(Player player){this.player=player;}
    }
    private static Consumer<ServerPlayer> snapshotSink=player->{};
    private static java.util.function.BiConsumer<ServerPlayer,PlayerViewB173> presentationSink=(player,view)->{};
    private static boolean installed;
    private PlayerApiLifecycleB173(){}
    public static void installSnapshotSink(Consumer<ServerPlayer> sink){snapshotSink=java.util.Objects.requireNonNull(sink);}
    public static void installPresentationSink(java.util.function.BiConsumer<ServerPlayer,PlayerViewB173> sink){presentationSink=java.util.Objects.requireNonNull(sink);}
    public static synchronized void install(IEventBus modBus){
        if(installed)return;installed=true;ATTACHMENTS.register(modBus);
        PlayerPresentationNetworkB173.install();
        PlayerAbilitiesB173.install();
        PlayerHealthCapacityB173.install();
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener((net.neoforged.neoforge.event.tick.PlayerTickEvent.Post event)->{
            if(!(event.getEntity() instanceof ServerPlayer player)||!PlayerApiB173.hasRegistered())return;
            Data data=player.getData(DATA);if(data.dead||data.lastTick==player.tickCount)return;data.lastTick=player.tickCount;
            run(data,"tick",()->{PlayerViewB173 view=ready(data);try{PlayerApiB173.tick(view);}finally{view.writeBack();}PlayerPresentationNetworkB173.guard("lifecycle",()->presentationSink.accept(player,view));snapshotSink.accept(player);});
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent event)->{
            if(event.getEntity() instanceof ServerPlayer player&&PlayerApiB173.hasRegistered()){
                Data data=player.getData(DATA);run(data,"login",()->{var view=ready(data);view.writeBack();PlayerPresentationNetworkB173.guard("lifecycle",()->presentationSink.accept(player,view));snapshotSink.accept(player);});
            }
        });
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,false,(net.neoforged.neoforge.event.entity.living.LivingDeathEvent event)->{
            if(event.getEntity() instanceof ServerPlayer player&&PlayerApiB173.hasRegistered()){
                Data data=player.getData(DATA);run(data,"death",()->{PlayerViewB173 view=ready(data);try{PlayerApiB173.death(view);}finally{data.dead=true;view.writeBack();}PlayerPresentationNetworkB173.guard("lifecycle",()->presentationSink.accept(player,view));});
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.Clone event)->{
            if(!(event.getEntity() instanceof ServerPlayer player)||!PlayerApiB173.hasRegistered())return;
            Player old=event.getOriginal();Data original=old.getData(DATA);CompoundTag tag=event.isWasDeath()?null:save(original);
            Data copy=new Data(player);copy.pending=tag==null?null:tag.copy();copy.lastGood=tag==null?null:tag.copy();player.setData(DATA,copy);
            PlayerViewB173.release(old);
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent event)->{
            if(event.getEntity() instanceof ServerPlayer player&&PlayerApiB173.hasRegistered()){
                Data data=player.getData(DATA);run(data,"respawn",()->{var view=ready(data);view.writeBack();PlayerPresentationNetworkB173.guard("lifecycle",()->presentationSink.accept(player,view));snapshotSink.accept(player);});
            }
        });
        bus.addListener((net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedOutEvent event)->{
            if(event.getEntity() instanceof ServerPlayer player&&PlayerApiB173.hasRegistered())player.getData(DATA).releaseAfterSave=true;
            else PlayerViewB173.release(event.getEntity());
        });
        bus.addListener((net.neoforged.neoforge.event.server.ServerStoppedEvent event)->PlayerViewB173.clear());
    }
    public static void initializeView(PlayerViewB173 view){
        try{PlayerApiB173.ensure(view);}catch(RuntimeException|LinkageError error){/* ensure reports each failed base once. */}
    }
    private static PlayerViewB173 ready(Data data){
        PlayerViewB173 view=PlayerViewB173.of(data.player);
        PlayerApiB173.ensure(view);
        if(data.pending!=null){
            CompoundTag tag=data.pending;data.pending=null;
            PlayerApiB173.read(view,NbtB173.fromModern(tag));
            view.writeBack();
        }
        return view;
    }
    private static CompoundTag save(Data data){
        if(!(data.player instanceof ServerPlayer)||!PlayerApiB173.hasRegistered())return null;
        if(data.dead){if(data.releaseAfterSave)PlayerViewB173.release(data.player);return data.lastGood==null?null:data.lastGood.copy();}
        if(data.saving)return data.lastGood==null?null:data.lastGood.copy();
        data.saving=true;
        try{
            PlayerViewB173 view=ready(data);long before=PlayerApiB173.callbackFailures(view);
            if(PlayerApiB173.bases(view).isEmpty())return data.lastGood==null?null:data.lastGood.copy();
            var tag=new legacyconnector.vb1_7_3.net.minecraft.nbt.NbtCompound();
            try{PlayerApiB173.write(view,tag);}finally{view.writeBack();}
            if(before==PlayerApiB173.callbackFailures(view))data.lastGood=NbtB173.toModern(tag);
        }catch(RuntimeException|LinkageError error){report(data,"save",error);}
        finally{data.saving=false;if(data.releaseAfterSave)PlayerViewB173.release(data.player);}
        return data.lastGood==null?null:data.lastGood.copy();
    }
    private static void run(Data data,String hook,Runnable callback){try{callback.run();}catch(RuntimeException|LinkageError error){report(data,hook,error);}}
    private static void report(Data data,String hook,Throwable error){if(data.reported.add(hook))org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").error("PlayerAPI native {} failed for {}",hook,data.player.getUUID(),error);}
}
