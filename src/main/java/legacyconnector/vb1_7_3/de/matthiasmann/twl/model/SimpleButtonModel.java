package legacyconnector.vb1_7_3.de.matthiasmann.twl.model;

import java.util.ArrayList;
public class SimpleButtonModel implements ButtonModel {
    private boolean selected,pressed,armed,hover,enabled=true;
    private final ArrayList<Runnable> actions=new ArrayList<>(),states=new ArrayList<>();
    public boolean isSelected(){return selected;}public boolean isPressed(){return pressed;}public boolean isArmed(){return armed;}public boolean isHover(){return hover;}public boolean isEnabled(){return enabled;}
    private void changed(){for(Runnable r:new ArrayList<>(states))r.run();}
    public void setSelected(boolean v){if(selected!=v){selected=v;changed();}}
    public void setPressed(boolean v){if(pressed!=v){pressed=v;changed();if(!v&&armed&&enabled)fireActionCallback();}}
    public void setArmed(boolean v){if(armed!=v){armed=v;changed();}}
    public void setHover(boolean v){if(hover!=v){hover=v;changed();}}
    public void setEnabled(boolean v){if(enabled!=v){enabled=v;changed();}}
    public void addActionCallback(Runnable r){actions.add(java.util.Objects.requireNonNull(r));}
    public void removeActionCallback(Runnable r){actions.remove(r);}
    public void fireActionCallback(){for(Runnable r:new ArrayList<>(actions))r.run();}
    public boolean hasActionCallbacks(){return !actions.isEmpty();}
    public void addStateCallback(Runnable r){states.add(java.util.Objects.requireNonNull(r));}
    public void removeStateCallback(Runnable r){states.remove(r);}
    public void connect(){}public void disconnect(){}
}
