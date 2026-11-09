package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.*;
import legacyconnector.vb1_7_3.net.minecraft.client.util.ScreenScaler;
import net.minecraft.nbt.*;
import org.joml.Matrix4f;

/** Graphics-free capture of original game-hook drawing; native client replay owns all GPU state. */
public final class GameHookHudB173 {
    public static final int MAX_COMMANDS=512;
    private GameHookHudB173(){}
    public static CompoundTag capture(ServerGameHooksB173.Context context,Runnable callback){
        var capture=new Capture(context.metrics());
        var options=new legacyconnector.vb1_7_3.net.minecraft.client.option.GameOptions();options.guiScale=context.scale();
        var scaler=new ScreenScaler(options,context.width(),context.height());
        try(var renderer=RendererContextB173.open(path->MobTexturesB173.resolve(null,path.replace("%blur%","").replace("%clamp%","")),(draw,texture)->{},new RendererContextB173.Extras(){
            @Override public void quad(List<RendererContextB173.Vertex> vertices,Matrix4f matrix,net.minecraft.resources.ResourceLocation texture,RendererContextB173.Style style){
                if(vertices.size()!=4)return;var command=capture.op("quad");if(command==null)return;
                var points=new ListTag();for(var vertex:vertices){var point=new CompoundTag();point.putDouble("x",vertex.x());point.putDouble("y",vertex.y());point.putDouble("z",vertex.z());point.putDouble("u",vertex.u());point.putDouble("v",vertex.v());point.putInt("color",vertex.argb());points.add(point);}
                command.put("vertices",points);putMatrix(command,"matrix",matrix);command.putString("texture",texture.toString());command.putInt("color",RendererContextB173.argb());command.put("style",style(style));
            }
            @Override public void text(String text,float x,float y,int color,boolean shadow,Matrix4f matrix,RendererContextB173.Style style){
                var command=capture.op("text",(int)x,(int)y,color,shadow?1:0);if(command!=null){command.putString("text",bounded(text));putMatrix(command,"matrix",matrix);}
            }
            @Override public int width(String text){return capture.width(text);}
        })){
            GuiDrawB173.withSink(capture,callback);
        }
        var frame=new CompoundTag();frame.putInt("width",scaler.getScaledWidth());frame.putInt("height",scaler.getScaledHeight());
        // A truncated command list could contain an unmatched pose push. Discard drawing as a whole, after gameplay completes.
        frame.put("commands",capture.overflow?new ListTag():capture.commands);return frame;
    }
    private static String bounded(String text){return text.substring(0,Math.min(4096,text.length()));}
    private static CompoundTag style(RendererContextB173.Style style){
        var tag=new CompoundTag();tag.putBoolean("blend",style.blend());tag.putInt("src",style.blendSrc());tag.putInt("dst",style.blendDst());
        tag.putBoolean("depthTest",style.depthTest());tag.putBoolean("depthWrite",style.depthWrite());tag.putInt("depthFunc",style.depthFunc());putMatrix(tag,"textureMatrix",style.textureMatrix());return tag;
    }
    private static void putMatrix(CompoundTag tag,String name,Matrix4f matrix){var list=new ListTag();for(float value:matrix.get(new float[16]))list.add(FloatTag.valueOf(value));tag.put(name,list);}
    public static Matrix4f matrix(CompoundTag tag,String name){var list=tag.getList(name,Tag.TAG_FLOAT);if(list.size()!=16)return new Matrix4f();float[] values=new float[16];for(int i=0;i<16;i++)values[i]=list.getFloat(i);return new Matrix4f().set(values);}
    private static final class Capture implements GuiDrawB173.Sink {
        final ListTag commands=new ListTag();final int[] metrics;final Map<Integer,String> paths=new HashMap<>();boolean overflow;
        Capture(int[] metrics){this.metrics=metrics;}
        CompoundTag op(String name,int... values){if(commands.size()>=MAX_COMMANDS){overflow=true;return null;}var tag=new CompoundTag();tag.putString("op",name);tag.putIntArray("values",values);commands.add(tag);return tag;}
        public void blit(int x,int y,int u,int v,int width,int height){op("blit",x,y,u,v,width,height);}
        public void fill(int a,int b,int c,int d,int color){op("fill",a,b,c,d,color);}
        public void fillGradient(int a,int b,int c,int d,int top,int bottom){op("gradient",a,b,c,d,top,bottom);}
        public void text(String text,int x,int y,int color,boolean shadow){var tag=op("text",x,y,color,shadow?1:0);if(tag!=null)tag.putString("text",bounded(text));}
        public int width(String text){int width=0;for(char c:text.toCharArray())width+=c<metrics.length?metrics[c]:6;return width;}
        public int textureId(String path){int id=paths.size()+1;if(id<=MAX_COMMANDS&&path.length()<=1024)paths.put(id,path);return id;}
        public int load(java.awt.image.BufferedImage image){throw new UnsupportedOperationException("Dynamic HUD bitmap capture is not implemented");}
        public void bind(int id){var path=paths.get(id);if(path==null)return;var tag=op("bind");if(tag!=null)tag.putString("path",path);}
        public void color(float r,float g,float b,float a){var tag=op("color");if(tag!=null){tag.putFloat("r",r);tag.putFloat("g",g);tag.putFloat("b",b);tag.putFloat("a",a);}}
        public void push(){op("push");}public void pop(){op("pop");}
        public void translate(float x,float y,float z){vector("translate",x,y,z);}public void scale(float x,float y,float z){vector("scale",x,y,z);}
        private void vector(String op,float x,float y,float z){var tag=op(op);if(tag!=null){tag.putFloat("x",x);tag.putFloat("y",y);tag.putFloat("z",z);}}
        public void click(){op("click");}
    }
}
