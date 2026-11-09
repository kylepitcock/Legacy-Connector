package legacyconnector.vb1_7_3.net.minecraft.client.gui.widget;

import legacyconnector.vb1_7_3.net.minecraft.client.font.TextRenderer;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.DrawContext;
import legacyconnector.vb1_7_3.net.minecraft.client.gui.screen.Screen;
import legacyconnector.vb1_7_3.net.minecraft.util.CharacterUtils;

/** Beta single-line text field: append/backspace editing, focus, clipboard paste and a blinking end caret. */
public class TextFieldWidget extends DrawContext {
    private final Screen parent;
    private final TextRenderer textRenderer;
    private final int x,y,width,height;
    private String text;
    private int maxLength,focusedTicks;
    public boolean focused;
    public boolean enabled=true;

    public TextFieldWidget(Screen parent,TextRenderer textRenderer,int x,int y,int width,int height,String text){
        this.parent=parent;this.textRenderer=textRenderer;this.x=x;this.y=y;this.width=width;this.height=height;setText(text);
    }
    public void setText(String text){this.text=text==null?"":text;}
    public String getText(){return text;}
    public void tick(){focusedTicks++;}
    public void setMaxLength(int length){maxLength=length;}
    public void setFocused(boolean focused){if(focused&&!this.focused)focusedTicks=0;this.focused=focused;}
    public void mouseClicked(int mouseX,int mouseY,int button){setFocused(enabled&&mouseX>=x&&mouseX<x+width&&mouseY>=y&&mouseY<y+height);}
    public void keyPressed(char character,int key){
        if(!enabled||!focused)return;
        if(character=='\t'&&parent!=null)parent.handleTab();
        if(character==22){
            String clipboard=Screen.getClipboard();if(clipboard==null)clipboard="";
            // Beta's clipboard path uses a separate 32-character cap, regardless of setMaxLength.
            int count=Math.min(32-text.length(),clipboard.length());if(count>0)text+=clipboard.substring(0,count);
        }
        if(key==14&&!text.isEmpty())text=text.substring(0,text.length()-1);
        if(CharacterUtils.VALID_CHARACTERS.indexOf(character)>=0&&(maxLength==0||text.length()<maxLength))text+=character;
    }
    public void render(){
        fill(x-1,y-1,x+width+1,y+height+1,0xFFA0A0A0);fill(x,y,x+width,y+height,0xFF000000);
        String caret=enabled&&focused&&focusedTicks/6%2==0?"_":"";
        textRenderer.drawWithShadow(text+caret,x+4,y+(height-8)/2,enabled?0xE0E0E0:0x707070);
    }
}
