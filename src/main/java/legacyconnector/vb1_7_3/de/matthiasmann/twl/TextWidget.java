package legacyconnector.vb1_7_3.de.matthiasmann.twl;

public class TextWidget extends Widget {
    private String text="";
    public String getText(){return text;}
    public void setText(String value){text=value==null?"":value;}
}
