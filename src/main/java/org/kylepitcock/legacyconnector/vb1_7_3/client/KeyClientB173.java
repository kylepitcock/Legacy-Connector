package org.kylepitcock.legacyconnector.vb1_7_3.client;

import java.util.*;
import com.mojang.blaze3d.platform.InputConstants;
import legacyconnector.vb1_7_3.net.minecraft.client.option.KeyBinding;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import org.kylepitcock.legacyconnector.vb1_7_3.*;
import org.kylepitcock.legacyconnector.core.network.LegacyPayload;

/** Native controls registration and physical input reporting; original callbacks execute on server actors. */
public final class KeyClientB173 {
    private static final Map<KeyBinding,KeyMapping> MAPPINGS=new IdentityHashMap<>();
    private static final Map<String,Boolean> SENT=new HashMap<>();
    private static final Set<String> NAMES=new HashSet<>();
    private static Object connection;
    private static volatile Object guiSnapshot;
    private static volatile boolean menuSnapshot;
    private static boolean installed,collected;
    private static int ticks;
    private KeyClientB173(){}
    public static void register(IEventBus modBus){
        if(installed)return;installed=true;
        KeyBindingsB173.installClient(registration->{if(collected)Minecraft.getInstance().execute(()->late(registration));});
        GuiQueriesB173.installClient(()->Minecraft.getInstance().isSameThread()?currentGui():guiSnapshot);
        legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.TitleScreen.mcc$nativeActivity(
                ()->Minecraft.getInstance().isSameThread()?menuActive(Minecraft.getInstance().screen):menuSnapshot);
        modBus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent event)->{
            for(var registration:KeyBindingsB173.registered())event.register(mapping(registration));
            collected=true;
        });
        var bus=net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
        bus.addListener(net.neoforged.bus.api.EventPriority.LOWEST,
                (net.neoforged.neoforge.client.event.ScreenEvent.Opening event)->menuSnapshot=menuActive(event.getNewScreen()));
        bus.addListener((net.neoforged.neoforge.client.event.InputEvent.Key event)->refreshPressed());
        bus.addListener((net.neoforged.neoforge.client.event.InputEvent.MouseButton.Post event)->refreshPressed());
        bus.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post event)->tick());
    }
    private static KeyMapping mapping(KeyBindingsB173.Registration registration){
        var key=registration.key();var existing=MAPPINGS.get(key);if(existing!=null)return existing;
        String stem=key.translationKey+" ("+registration.title()+")",name=stem;
        for(int suffix=2;!NAMES.add(name);suffix++)name=stem+" "+suffix;
        int code=KeyBinding.mcc$get$code(key);
        var mapping=new KeyMapping(name,code<0&&code>=-100?InputConstants.Type.MOUSE:InputConstants.Type.KEYSYM,
                code<0&&code>=-100?code+100:KeysB173.glfw(code),registration.title());
        MAPPINGS.put(key,mapping);
        key.mcc$bind(()->legacyCode(mapping),key::mcc$clientPressed,value->Minecraft.getInstance().execute(()->{
            mapping.setKey(value<0&&value>=-100?InputConstants.Type.MOUSE.getOrCreate(value+100):InputConstants.Type.KEYSYM.getOrCreate(KeysB173.glfw(value)));
            KeyMapping.resetMapping();
        }));
        return mapping;
    }
    private static int legacyCode(KeyMapping mapping){var key=mapping.getKey();return key.getType()==InputConstants.Type.MOUSE?key.getValue()-100:KeysB173.lwjgl(key.getValue());}
    private static boolean down(KeyMapping mapping){
        var minecraft=Minecraft.getInstance();var key=mapping.getKey();
        if(!minecraft.isWindowActive()||key.getValue()==InputConstants.UNKNOWN.getValue())return false;
        long window=minecraft.getWindow().getWindow();
        return key.getType()==InputConstants.Type.MOUSE
                ?org.lwjgl.glfw.GLFW.glfwGetMouseButton(window,key.getValue())==org.lwjgl.glfw.GLFW.GLFW_PRESS
                :InputConstants.isKeyDown(window,key.getValue());
    }
    private static void late(KeyBindingsB173.Registration registration){
        var minecraft=Minecraft.getInstance();var mapping=mapping(registration);
        if(minecraft.options==null)return;
        if(Arrays.stream(minecraft.options.keyMappings).noneMatch(value->value==mapping)){
            var keys=Arrays.copyOf(minecraft.options.keyMappings,minecraft.options.keyMappings.length+1);
            keys[keys.length-1]=mapping;minecraft.options.keyMappings=keys;KeyMapping.resetMapping();
        }
    }
    private static void refreshPressed(){for(var entry:MAPPINGS.entrySet())KeyBinding.mcc$set$pressed(entry.getKey(),down(entry.getValue()));}
    private static void tick(){
        var minecraft=Minecraft.getInstance();var current=minecraft.getConnection();
        guiSnapshot=currentGui();menuSnapshot=menuActive(minecraft.screen);
        if(current!=connection){connection=current;SENT.clear();ticks=0;}
        refreshPressed();
        if(current==null||minecraft.player==null||minecraft.level==null){SENT.clear();return;}
        boolean heartbeat=++ticks%10==0,screen=minecraft.screen!=null;
        for(var registration:KeyBindingsB173.registered()){
            var mapping=MAPPINGS.get(registration.key());if(mapping==null)continue;
            boolean pressed=down(mapping);Boolean old=SENT.put(registration.id(),pressed);
            if(old!=null&&old==pressed&&!heartbeat)continue;
            var server=minecraft.getSingleplayerServer();
            if(server!=null){var uuid=minecraft.player.getUUID();server.execute(()->{var actor=server.getPlayerList().getPlayer(uuid);if(actor!=null)KeyBindingsB173.input(actor,registration.id(),pressed,screen);});}
            else if(current.hasChannel(LegacyPayload.TYPE))net.neoforged.neoforge.network.PacketDistributor.sendToServer(KeyBindingsB173.packet(registration.id(),pressed,screen));
        }
    }
    private static boolean menuActive(net.minecraft.client.gui.screens.Screen screen){
        return legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.TitleScreen.mcc$menuActive(
                Minecraft.getInstance().level!=null,screen!=null,screen instanceof net.minecraft.client.gui.screens.TitleScreen);
    }
    private static Object currentGui(){
        var minecraft=Minecraft.getInstance();var screen=minecraft.screen;
        if(screen instanceof net.minecraft.client.gui.screens.TitleScreen)
            return new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.TitleScreen();
        if(screen instanceof LegacyB173PlainScreen plain)return plain.legacy();
        if(screen instanceof LegacyB173Screen handled&&handled.getMenu().gui()!=null)return handled.getMenu().gui();
        var original=OriginalInventoryScreenB173.original(screen);if(original!=null)return original;
        if(screen instanceof net.minecraft.client.gui.screens.inventory.InventoryScreen)
            return new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.InventoryScreen(null);
        if(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)
            return new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.ingame.HandledScreen(null){@Override protected void drawBackground(float partial){}};
        return screen==null?null:new legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen();
    }
}
