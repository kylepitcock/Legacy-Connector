package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
import legacyconnector.vb1_7_3.net.minecraft.client.Minecraft;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity;
import legacyconnector.vb1_7_3.net.minecraft.src.*;
import legacyconnector.vb1_7_3.net.minecraft.world.World;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.server.ServerStartedEvent;

/** Actual registered settings widgets and original naming-screen input callbacks, with CPU drawing capture. */
public final class MoCreaturesGuiSelfTestB173 {
    private MoCreaturesGuiSelfTestB173(){}
    public static void register(IEventBus bus){if(Boolean.getBoolean("legacyconnector.entitySelfTest"))bus.addListener((ServerStartedEvent event)->{try{run(event.getServer().overworld());}catch(RuntimeException|LinkageError error){org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173GuiTest").error("B1.7.3 MOC GUI TEST failed",error);}});}
    private static void require(boolean condition,String detail){if(!condition)throw new IllegalStateException("B1.7.3 MOC GUI TEST: "+detail);}
    private static WidgetInt firstNumber(Widget widget){if(widget instanceof WidgetInt number)return number;for(int i=0;i<widget.getNumChildren();i++){var found=firstNumber(widget.getChild(i));if(found!=null)return found;}return null;}
    private static void run(net.minecraft.server.level.ServerLevel level){
        var selected=ContentB173.entities().stream().filter(r->r.type().getPackageName().equals("legacyconnector_b173.mocreatures")).findFirst();if(selected.isEmpty())return;
        var registration=selected.get();var drawn=new ArrayList<String>();int[] fills={0};var previous=GuiModScreen.currentscreen;
        var sink=(GuiDrawB173.Sink)java.lang.reflect.Proxy.newProxyInstance(MoCreaturesGuiSelfTestB173.class.getClassLoader(),new Class<?>[]{GuiDrawB173.Sink.class},(proxy,method,args)->{
            if(method.getName().equals("text"))drawn.add((String)args[0]);if(method.getName().equals("fill"))fills[0]++;
            if(method.getReturnType()==int.class)return method.getName().equals("width")?((String)args[0]).length()*6:1;return null;
        });
        GuiDrawB173.install(sink);
        try{
            var base=registration.mod().instance;var settings=(ModSettings)base.getClass().getField("MoCSettings").get(base);var screen=(ModSettingScreen)base.getClass().getField("MoCScreen").get(base);
            var gui=new GuiModScreen(null,screen.thewidget);GuiModScreen.show(gui);gui.init(Minecraft.mcc$instance(),640,360);gui.render(0,0,0);
            require(fills[0]>0&&!drawn.isEmpty(),"actual settings widget tree draws visible controls/text");
            // MoCreatures' root holds navigation buttons. Follow its original callback into the numeric settings page.
            WidgetInt number=firstNumber(gui.mainwidget);
            if(number==null){var queue=new ArrayDeque<Widget>();queue.add(gui.mainwidget);while(!queue.isEmpty()&&number==null){var widget=queue.removeFirst();if(widget instanceof legacyconnector.vb1_7_3.de.matthiasmann.twl.Button button&&button.getModel().hasActionCallbacks()&&!button.getText().equals("Back")){
                        gui.mcc$mouseClicked(button.getX()+2,button.getY()+2,0);gui.mcc$mouseReleased(button.getX()+2,button.getY()+2,0);
                        var opened=GuiModScreen.currentscreen;if(opened!=null&&opened!=gui){opened.init(Minecraft.mcc$instance(),640,360);number=firstNumber(opened.mainwidget);if(number==null)GuiModScreen.back();else gui=opened;}
                    }for(int i=0;i<widget.getNumChildren();i++)queue.add(widget.getChild(i));}}
            require(number!=null,"actual original settings navigation opens numeric control");
            var file=ModSettings.getAppDir("minecraft/mods/"+settings.backendname+"/guiconfig.properties").toPath();byte[] previousFile=java.nio.file.Files.isRegularFile(file)?java.nio.file.Files.readAllBytes(file):null;
            int old=number.value.get(ModSettingScreen.guicontext);
            try{var slider=number.s;gui.mcc$mouseClicked(slider.getX()+slider.getWidth()-1,slider.getY()+5,0);gui.mcc$mouseReleased(slider.getX()+slider.getWidth()-1,slider.getY()+5,0);require(number.value.get(ModSettingScreen.guicontext)==number.value.max,"actual pointer input updates original setting through callbacks");}
            finally{number.value.set(old,ModSettingScreen.guicontext);if(previousFile!=null)java.nio.file.Files.write(file,previousFile);else java.nio.file.Files.deleteIfExists(file);}
            var loader=registration.type().getClassLoader();Class<?> guiType=Class.forName(registration.type().getPackageName()+".MoCGUI",true,loader);
            for(String entityName:List.of("EntityKitty","EntityHorse","EntityBigCat","EntityDolphin","EntityShark")){
                var type=Class.forName(registration.type().getPackageName()+"."+entityName,true,loader);var entity=(LivingEntity)type.getConstructor(World.class).newInstance(World.mcc$wrap(level));
                Screen nameGui=(Screen)guiType.getConstructor(LivingEntity.class,String.class).newInstance(entity,"");nameGui.init(Minecraft.mcc$instance(),640,360);
                require(legacyconnector.vb1_7_3.org.lwjgl.input.Keyboard.areRepeatEventsEnabled(),"original naming screen enables key repeat");
                nameGui.mcc$keyPressed('A',30);nameGui.mcc$keyPressed('\u2603',0);nameGui.mcc$keyPressed('B',48);nameGui.mcc$keyPressed('\b',14);
                for(char character:"12345678901234567890".toCharArray())nameGui.mcc$keyPressed(character,0);
                nameGui.render(0,0,0);var button=(legacyconnector.vb1_7_3.net.minecraft.client.gui.widget.ButtonWidget)nameGui.mcc$buttons().getFirst();
                nameGui.mcc$mouseClicked(button.x+2,button.y+2,0);nameGui.mcc$mouseReleased(button.x+2,button.y+2,0);
                require(type.getField("name").get(entity).equals("A12345678901234"),"original "+entityName+" naming callback filters characters/backspace/15-character limit");
                nameGui.removed();require(!legacyconnector.vb1_7_3.org.lwjgl.input.Keyboard.areRepeatEventsEnabled(),"original naming close disables repeat");
            }
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/B173GuiTest").info("B1.7.3 MOC GUI TEST: actual settings navigation/drawing/pointer callbacks and five original naming-screen keyboard/button callbacks passed (headless; GUI networking tested separately)");
        }catch(ReflectiveOperationException|java.io.IOException error){throw new IllegalStateException("B1.7.3 MOC GUI TEST failed",error);}
        finally{GuiDrawB173.install(null);GuiModScreen.currentscreen=previous;legacyconnector.vb1_7_3.org.lwjgl.input.Keyboard.enableRepeatEvents(false);}
    }
}
