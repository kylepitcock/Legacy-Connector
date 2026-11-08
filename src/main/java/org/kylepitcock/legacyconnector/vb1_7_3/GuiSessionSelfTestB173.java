package org.kylepitcock.legacyconnector.vb1_7_3;

import io.netty.buffer.Unpooled;
import java.lang.reflect.Proxy;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.PacketFlow;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;

/** Actual original naming screen through server ownership, drawing packets and round-tripped remote input. */
public final class GuiSessionSelfTestB173 {
    private GuiSessionSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173GuiSessionTest").error("B1.7.3 GUI SESSION TEST failed",error);}});}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException("B1.7.3 GUI SESSION TEST: "+detail);}
    private static LegacyPayload wire(LegacyPayload payload){var buffer=Unpooled.buffer();try{LegacyPayload.CODEC.encode(buffer,payload);return LegacyPayload.CODEC.decode(buffer);}finally{buffer.release();}}
    private static CompoundTag tag(LegacyPayload payload){var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(wire(payload).data()));try{return buffer.readNbt();}finally{buffer.release();}}
    private static LegacyPayload input(CompoundTag tag){var buffer=new FriendlyByteBuf(Unpooled.buffer());try{buffer.writeNbt(tag);byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);return wire(new LegacyPayload("b1.7.3-gui","screen",0,bytes));}finally{buffer.release();}}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var registration=ContentB173.entities().stream().filter(r->r.type().getName().equals("legacyconnector_b173.mocreatures.EntityHorse")).findFirst();if(registration.isEmpty())return;
        try{
            var type=registration.get().type();LivingEntity entity=(LivingEntity)type.getConstructor(World.class).newInstance(World.mcc$wrap(level));
            Class<?> originalGui=Class.forName(type.getPackageName()+".MoCGUI",true,type.getClassLoader());Screen gui=(Screen)originalGui.getConstructor(LivingEntity.class,String.class).newInstance(entity,"");
            var player=net.neoforged.neoforge.common.util.FakePlayerFactory.getMinecraft(level);
            IPayloadContext context=(IPayloadContext)Proxy.newProxyInstance(GuiSessionSelfTestB173.class.getClassLoader(),new Class<?>[]{IPayloadContext.class},(proxy,method,args)->switch(method.getName()){
                case "player"->player;case "flow"->PacketFlow.SERVERBOUND;case "enqueueWork"->{((Runnable)args[0]).run();yield CompletableFuture.completedFuture(null);}default->null;
            });
            var frames=new ArrayList<LegacyPayload>();var singleton=Minecraft.mcc$instance();Screen previous=singleton.currentScreen;
            GuiSessionsB173.withSender((recipient,payload)->{require(recipient==player,"frame targets initiating player");frames.add(wire(payload));},()->{
                var previousStack=player.getMainHandItem().copy();
                try{
                    player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.item.ItemStack(net.minecraft.world.item.Items.WHEAT,2));
                    var callbackView=PlayerViewB173.of(player);
                    MenusB173.withActor(player,()->{
                        callbackView.inventory.main[callbackView.inventory.selectedSlot].count=1;
                        MenusB173.withActor(player,()->Minecraft.mcc$instance().setScreen(new Screen()));
                        Minecraft.mcc$instance().setScreen(gui);
                        require(frames.isEmpty(),"nested actor screen request waits for original callback inventory commit");
                        callbackView.writeBack();
                    });
                    require(player.getMainHandItem().getCount()==1,"opening naming screen preserves pending original item consumption");
                }finally{player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,previousStack);}
                require(!frames.isEmpty(),"original server screen emits opening frame");CompoundTag first=tag(frames.getLast());int id=first.getInt("id");
                require(id>0&&!first.getList("commands",10).isEmpty(),"opening frame carries original drawing commands");
                require(Minecraft.mcc$instance()==singleton&&singleton.currentScreen==previous,"server GUI leaves client singleton untouched");
                for(char c:"NetworkPet".toCharArray()){CompoundTag request=new CompoundTag();request.putInt("id",id);request.putString("action","key");request.putInt("width",640);request.putInt("height",360);request.putInt("character",c);request.putInt("key",0);GuiSessionsB173.receive(input(request),context);}
                CompoundTag done=new CompoundTag();done.putInt("id",id);done.putString("action","press");done.putInt("width",640);done.putInt("height",360);done.putInt("x",222);done.putInt("y",212);done.putInt("button",0);GuiSessionsB173.receive(input(done),context);
                require(tag(frames.getLast()).getBoolean("closed"),"original Done closes the server session without stale drawing frame");
                try{require(type.getField("name").get(entity).equals("NetworkPet"),"remote keys/Done execute original naming mutation on server entity");}catch(ReflectiveOperationException error){throw new IllegalStateException(error);}
                int count=frames.size();GuiSessionsB173.receive(input(done),context);require(frames.size()==count,"closed session ignores stale input");
            });
            require(!legacyconnector.vb1_7_3.org.lwjgl.input.Keyboard.areRepeatEventsEnabled(),"server close invokes original removed callback");
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173GuiSessionTest").info("B1.7.3 GUI SESSION TEST: original MoCGUI server ownership, frame/input packet round-trip, Done naming mutation, close cleanup and stale input passed");
        }catch(ReflectiveOperationException error){throw new IllegalStateException("B1.7.3 GUI SESSION TEST failed",error);}
    }
}
