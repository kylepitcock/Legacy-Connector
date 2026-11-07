package org.kylepitcock.legacyconnector.vb1_7_3;

import java.nio.file.*;
import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.src.*;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.*;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.model.*;

/** Exercises settings state and the original GUI API callback contracts, without claiming rendering. */
final class GuiApiSelfTestB173 {
    private static int callbackValue;
    private static void accept(int value){callbackValue+=value;}
    static void run(){
        String originalContext=ModSettings.currentContext;
        String name="legacyconnector-selftest-"+UUID.randomUUID();
        String context="selftest-"+UUID.randomUUID();
        ModSettings settings=new ModSettings(name),reload=null;
        try{
            var number=new SettingInt("integer",7,0,5,100);
            var decimal=new SettingFloat("float",1.25f,0f,.25f,10f);
            var flag=new SettingBoolean("boolean",true);
            var multi=new SettingMulti("multi",1,"first","second");
            settings.append(number);settings.append(decimal);settings.append(flag);settings.append(multi);
            Setting<?> erased=number;
            require(erased.get().equals(7)&&number.get("unknown")==7,"typed default and fallback");
            settings.load();number.set(13,"");decimal.set(1.62f,"");flag.set(false,"");multi.set("first","");
            require(number.get("")==15&&decimal.get("")==1.5f,"original step rounding");
            ModSettings.contextDatadirs.put(context,"mods/legacyconnector-selftest-contexts/"+context);
            number.set(30,context);flag.set(true,context);
            require(number.get("")==15&&!flag.get("")&&number.get(context)==30&&flag.get(context),"context isolation");
            reload=new ModSettings(name);var loaded=new SettingInt("integer",1);var loadedFlag=new SettingBoolean("boolean",true);var loadedMulti=new SettingMulti("multi","first","second");
            reload.append(loaded);reload.append(loadedFlag);reload.append(loadedMulti);reload.load();
            require(loaded.get("")==15&&!loadedFlag.get("")&&loadedMulti.getLabel("").equals("first"),"properties reload");
            reload.load(context);require(loaded.get(context)==30&&loadedFlag.get(context)&&loaded.get("")==15,"context reload");
            var path=ModSettings.getAppDir("minecraft/mods/"+name+"/guiconfig.properties").toPath();
            require(path.startsWith(legacyconnector.vb1_7_3.net.minecraft.client.Minecraft.getRunDirectory().toPath()),"active instance path");
            var left=new Widget();var right=new Widget();var child=new Widget();left.add(child);right.add(child);
            require(left.getNumChildren()==0&&right.getChild(0)==child&&child.getParent()==right,"widget reparenting");
            callbackValue=0;Runnable action=new ModAction(GuiApiSelfTestB173.class,"accept",int.class).setDefaultArguments(3);
            ButtonModel model=new SimpleButtonModel();var button=new Button(model);button.addCallback(action);
            model.setArmed(true);model.setPressed(true);model.setPressed(false);
            require(callbackValue==3&&button instanceof TextWidget,"Runnable action and Button hierarchy");
            button.removeCallback(action);model.fireActionCallback();require(callbackValue==3,"callback removal");
            var control=new WidgetBoolean(flag,"Boolean");control.b.getModel().fireActionCallback();
            require(flag.get("")&&control.b.getText().endsWith("true"),"button writes setting and updates text");
            WidgetSetting.all.remove(control);
            org.slf4j.LoggerFactory.getLogger("LegacyConnector/b1.7.3").info("B1.7.3 GUIAPI TEST: typed defaults, rounding, context isolation, properties reload, widget hierarchy and Runnable callbacks passed (TWL rendering unsupported)");
        }finally{
            ModSettings.all.remove(settings);if(reload!=null)ModSettings.all.remove(reload);
            ModSettings.currentContext=originalContext;
            deleteConfig("mods/"+name);
            deleteConfig("mods/legacyconnector-selftest-contexts/"+context+"/"+name);
            ModSettings.contextDatadirs.remove(context);
        }
    }
    private static void deleteConfig(String relative){try{Path directory=ModSettings.getAppDir("minecraft/"+relative).toPath();Files.deleteIfExists(directory.resolve("guiconfig.properties"));Files.deleteIfExists(directory);}catch(java.io.IOException e){throw new IllegalStateException(e);}}
    private static void require(boolean condition,String label){if(!condition)throw new IllegalStateException("GuiAPI self-test: "+label);}
}
