package legacyconnector.vb1_7_3.de.matthiasmann.twl;

import legacyconnector.vb1_7_3.de.matthiasmann.twl.model.*;
public class Button extends TextWidget {
    private ButtonModel model;
    public Button(){this(new SimpleButtonModel());}public Button(String text){this();setText(text);}
    public Button(ButtonModel model){setModel(model);}
    public ButtonModel getModel(){return model;}
    public void setModel(ButtonModel value){if(model!=null)model.disconnect();model=java.util.Objects.requireNonNull(value);model.connect();}
    public void setEnabled(boolean v){super.setEnabled(v);model.setEnabled(v);}
    public void addCallback(Runnable r){model.addActionCallback(r);}public void removeCallback(Runnable r){model.removeActionCallback(r);}
    public boolean hasCallbacks(){return model.hasActionCallbacks();}
}
