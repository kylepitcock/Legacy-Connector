package legacyconnector.vb1_7_3.net.minecraft.src;

import java.util.ArrayList;
import legacyconnector.vb1_7_3.de.matthiasmann.twl.Widget;
/** Numeric control state; no native TWL drawing/input backend yet. */
public class WidgetSlider extends Widget {
    private float value,min,max=100;private String format="%s";
    private final ArrayList<Runnable> callbacks=new ArrayList<>();
    public float getValue(){return value;}
    public void setValue(float v){v=Math.max(min,Math.min(max,v));if(Float.compare(value,v)!=0){value=v;for(Runnable r:new ArrayList<>(callbacks))r.run();}}
    public void setMinMaxValue(float a,float b){min=a;max=b;}
    public void setFormat(String text){format=text;}public String getFormat(){return format;}
    public void addCallback(Runnable r){callbacks.add(r);}public void removeCallback(Runnable r){callbacks.remove(r);}
}
