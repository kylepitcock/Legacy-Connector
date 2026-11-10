package org.kylepitcock.legacyconnector.vb1_7_3.client;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.*;
import net.minecraft.network.chat.Component;
import org.kylepitcock.legacyconnector.vb1_7_3.*;

/** Client terminal for an original screen executing on the initiating player's server thread. */
public final class ServerGuiScreenB173 extends Screen {
    private final int id;private CompoundTag frame;private int mouseX,mouseY,pendingKey=-1;private boolean serverClosed,suspendRemoval;
    public ServerGuiScreenB173(CompoundTag frame){super(Component.empty());id=frame.getInt("id");this.frame=frame;}
    public int sessionId(){return id;}
    public void suspendForNativeScreen(){suspendRemoval=true;}
    public void frame(CompoundTag frame){this.frame=frame;}
    public void closeFromServer(){serverClosed=true;if(minecraft!=null&&minecraft.screen==this)minecraft.setScreen(null);}
    private void send(String action,java.util.function.Consumer<CompoundTag> values){
        if(minecraft==null||minecraft.getConnection()==null)return;CompoundTag tag=new CompoundTag();tag.putInt("id",id);tag.putString("action",action);tag.putInt("width",width);tag.putInt("height",height);tag.putInt("x",mouseX);tag.putInt("y",mouseY);values.accept(tag);GuiSessionsB173.sendInput(tag);
    }
    @Override protected void init(){send("resize",tag->{int[] metrics=new int[256];for(int i=0;i<metrics.length;i++)metrics[i]=font.width(Character.toString((char)i));tag.putIntArray("metrics",metrics);});}
    @Override public void tick(){send("tick",tag->{});}
    @Override public void render(GuiGraphics graphics,int x,int y,float partial){
        mouseX=x;mouseY=y;super.render(graphics,x,y,partial);ClientGuiB173.begin(graphics);
        try{var sink=new ClientGuiB173();for(Tag raw:frame.getList("commands",Tag.TAG_COMPOUND)){CompoundTag command=(CompoundTag)raw;int[] v=command.getIntArray("values");switch(command.getString("op")){
            case "blit"->sink.blit(v[0],v[1],v[2],v[3],v[4],v[5]);case "fill"->sink.fill(v[0],v[1],v[2],v[3],v[4]);case "gradient"->sink.fillGradient(v[0],v[1],v[2],v[3],v[4],v[5]);
            case "text"->sink.text(command.getString("text"),v[0],v[1],v[2],v[3]!=0);case "bind"->sink.bind(sink.textureId(command.getString("path")));
            case "color"->sink.color(command.getFloat("r"),command.getFloat("g"),command.getFloat("b"),command.getFloat("a"));case "push"->sink.push();case "pop"->sink.pop();case "translate"->sink.translate(command.getFloat("x"),command.getFloat("y"),command.getFloat("z"));case "scale"->sink.scale(command.getFloat("x"),command.getFloat("y"),command.getFloat("z"));default->{}
        }}}finally{ClientGuiB173.end();}
    }
    @Override public boolean mouseClicked(double x,double y,int button){mouseX=(int)x;mouseY=(int)y;send("press",tag->tag.putInt("button",button));return true;}
    @Override public boolean mouseReleased(double x,double y,int button){mouseX=(int)x;mouseY=(int)y;send("release",tag->tag.putInt("button",button));return true;}
    @Override public boolean mouseDragged(double x,double y,int button,double dx,double dy){mouseX=(int)x;mouseY=(int)y;send("drag",tag->tag.putInt("button",button));return true;}
    @Override public boolean mouseScrolled(double x,double y,double horizontal,double vertical){mouseX=(int)x;mouseY=(int)y;send("scroll",tag->tag.putDouble("scroll",vertical));return true;}
    @Override public boolean keyPressed(int key,int scan,int modifiers){if(KeysB173.types(key,modifiers)){pendingKey=key;return true;}key(KeysB173.character(key,modifiers),KeysB173.lwjgl(key));return true;}
    @Override public boolean charTyped(char character,int modifiers){key(character,pendingKey<0?0:KeysB173.lwjgl(pendingKey));pendingKey=-1;return true;}
    private void key(char character,int key){send("key",tag->{tag.putInt("character",character);tag.putInt("key",key);if(character==22){String clipboard=minecraft.keyboardHandler.getClipboard();tag.putString("clipboard",clipboard.substring(0,Math.min(32767,clipboard.length())));}});}
    @Override public void removed(){if(suspendRemoval){suspendRemoval=false;return;}if(!serverClosed)send("close",tag->{});}
    @Override public boolean isPauseScreen(){return false;}
}
