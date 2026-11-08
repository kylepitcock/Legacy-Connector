package legacyconnector.vb1_7_3.de.matthiasmann.twl;

import java.util.ArrayList;
/** TWL widget ownership/state only; rendering and input dispatch are not implemented. */
public class Widget {
    private final ArrayList<Widget> children = new ArrayList<>();
    private Widget parent;
    private boolean visible=true, enabled=true;
    private String theme="";
    private int x,y,width,height;
    public Widget getParent(){return parent;}
    public Widget getRootWidget(){return parent==null?this:parent.getRootWidget();}
    public int getNumChildren(){return children.size();}
    public Widget getChild(int i){return children.get(i);}
    public int getChildIndex(Widget child){return children.indexOf(child);}
    public void add(Widget child){insertChild(child,children.size());}
    public void insertChild(Widget child,int i){
        if(child==null)throw new NullPointerException("child");
        for(Widget p=this;p!=null;p=p.parent)if(p==child)throw new IllegalArgumentException("widget cycle");
        if(i<0||i>children.size())throw new IndexOutOfBoundsException(i);
        if(child.parent!=null)child.parent.removeChild(child);
        children.add(Math.min(i,children.size()),child);child.parent=this;
    }
    public boolean removeChild(Widget child){if(!children.remove(child))return false;child.parent=null;return true;}
    public Widget removeChild(int i){Widget child=children.remove(i);child.parent=null;return child;}
    public void removeAllChildren(){for(Widget child:children)child.parent=null;children.clear();}
    public boolean isVisible(){return visible;} public void setVisible(boolean v){visible=v;}
    public boolean isLocallyEnabled(){return enabled;} public boolean isEnabled(){return enabled&&(parent==null||parent.isEnabled());}
    public void setEnabled(boolean v){enabled=v;}
    public String getTheme(){return theme;} public void setTheme(String v){theme=v;}
    public int getX(){return x;}public int getY(){return y;}public int getWidth(){return width;}public int getHeight(){return height;}
    public boolean setPosition(int a,int b){boolean c=x!=a||y!=b;x=a;y=b;return c;}
    public boolean setSize(int a,int b){boolean c=width!=a||height!=b;width=a;height=b;return c;}
    public int getPreferredWidth(){return width;}public int getPreferredHeight(){return height;}
    protected void layout(){}
}
