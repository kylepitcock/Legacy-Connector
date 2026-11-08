package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayDeque;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Beta model rendering state, shared by the client adapter and graphics-free callback tests. */
public final class RendererContextB173 {
    public record Style(boolean texture2d,boolean lighting,boolean depthTest,boolean depthWrite,boolean blend,int blendSrc,int blendDst,int depthFunc,Matrix4f textureMatrix){}
    public record Vertex(double x,double y,double z,double u,double v,int argb,float nx,float ny,float nz){}
    public interface Extras {
        default void item(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity entity,legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack,Matrix4f transform,Style style){}
        default void text(String text,float x,float y,int argb,boolean shadow,Matrix4f transform,Style style){}
        default void quad(List<Vertex> vertices,Matrix4f transform,ResourceLocation texture,Style style){}
        default int width(String text){return text==null?0:text.replaceAll("§.","").length()*6;}
        default legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity camera(){return null;}
        default float yaw(){return 0;}
        default float pitch(){return 0;}
    }
    private static final Extras EMPTY=new Extras(){};
    private static final ThreadLocal<State> CURRENT=new ThreadLocal<>();
    private static final class State {
        final ArrayDeque<Matrix4f> poses=new ArrayDeque<>();
        final ArrayDeque<Matrix4f> texturePoses=new ArrayDeque<>();
        final Function<String,ResourceLocation> textures;
        final BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink;
        final Extras extras;
        final java.util.Map<Integer,String> textureIds=new java.util.HashMap<>();
        ResourceLocation texture=ResourceLocation.withDefaultNamespace("missingno");
        int argb=-1;
        int matrixMode=5888,blendSrc=770,blendDst=771,depthFunc=515;
        boolean texture2d=true,lighting=true,depthTest=true,depthWrite=true,blend;
        State(Function<String,ResourceLocation> textures,BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink,Extras extras){this.textures=textures;this.sink=sink;this.extras=extras;poses.push(new Matrix4f());texturePoses.push(new Matrix4f());}
        ArrayDeque<Matrix4f> stack(){return matrixMode==5890?texturePoses:poses;}
    }
    public static final class Scope implements AutoCloseable {
        private final State previous;private boolean closed;
        private Scope(State previous){this.previous=previous;}
        @Override public void close(){if(!closed){closed=true;if(previous==null)CURRENT.remove();else CURRENT.set(previous);}}
    }
    private RendererContextB173(){}
    public static Scope open(Function<String,ResourceLocation> textures,BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink){return open(textures,sink,EMPTY);}
    public static Scope open(Function<String,ResourceLocation> textures,BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink,Extras extras){var previous=CURRENT.get();CURRENT.set(new State(textures,sink,extras));return new Scope(previous);}
    public static boolean active(){return CURRENT.get()!=null;}
    public static Matrix4f matrix(){return new Matrix4f(CURRENT.get().poses.peek());}
    public static int argb(){return CURRENT.get().argb;}
    public static ResourceLocation texture(){return CURRENT.get().texture;}
    public static Style style(){var s=CURRENT.get();return new Style(s.texture2d,s.lighting,s.depthTest,s.depthWrite,s.blend,s.blendSrc,s.blendDst,s.depthFunc,new Matrix4f(s.texturePoses.peek()));}
    public static void push(){var stack=CURRENT.get().stack();stack.push(new Matrix4f(stack.peek()));}
    public static void pop(){var stack=CURRENT.get().stack();if(stack.size()<=1)throw new IllegalStateException("Unbalanced Beta renderer matrix pop");stack.pop();}
    public static void matrixMode(int mode){if(mode!=5888&&mode!=5890)throw new UnsupportedOperationException("Beta render matrix mode "+mode);CURRENT.get().matrixMode=mode;}
    public static void loadIdentity(){CURRENT.get().stack().peek().identity();}
    public static void translate(double x,double y,double z){CURRENT.get().stack().peek().translate((float)x,(float)y,(float)z);}
    public static void scale(float x,float y,float z){CURRENT.get().stack().peek().scale(x,y,z);}
    public static void rotate(float degrees,float x,float y,float z){float length=(float)Math.sqrt(x*x+y*y+z*z);if(length>0)CURRENT.get().stack().peek().rotate((float)Math.toRadians(degrees),x/length,y/length,z/length);}
    public static void transform(Matrix4f matrix){CURRENT.get().stack().peek().mul(matrix);}
    public static void enable(int cap,boolean enabled){var s=CURRENT.get();switch(cap){case 3553->s.texture2d=enabled;case 2896->s.lighting=enabled;case 2929->s.depthTest=enabled;case 3042->s.blend=enabled;case 32826,2977,3008,2884->{}default->throw new UnsupportedOperationException("Beta render capability "+cap);}}
    public static void blendFunc(int src,int dst){var s=CURRENT.get();s.blendSrc=src;s.blendDst=dst;}
    public static void depthMask(boolean write){CURRENT.get().depthWrite=write;}
    public static void depthFunc(int func){CURRENT.get().depthFunc=func;}
    public static void color(float r,float g,float b,float a){CURRENT.get().argb=(channel(a)<<24)|(channel(r)<<16)|(channel(g)<<8)|channel(b);}
    private static int channel(float value){return Math.round(Math.max(0,Math.min(1,value))*255);}
    public static void bind(String path){var s=CURRENT.get();var texture=s.textures.apply(path);s.texture=texture==null?ResourceLocation.withDefaultNamespace("missingno"):texture;}
    public static int textureId(String path){var s=CURRENT.get();for(var entry:s.textureIds.entrySet())if(entry.getValue().equals(path))return entry.getKey();int id=s.textureIds.size()+1;s.textureIds.put(id,path);return id;}
    public static void bind(int id){var path=CURRENT.get().textureIds.get(id);if(path==null)throw new IllegalArgumentException("Unknown Beta model texture "+id);bind(path);}
    public static void item(legacyconnector.vb1_7_3.net.minecraft.entity.LivingEntity entity,legacyconnector.vb1_7_3.net.minecraft.item.ItemStack stack){var s=CURRENT.get();s.extras.item(entity,stack,matrix(),style());}
    public static void text(String text,float x,float y,int argb,boolean shadow){var s=CURRENT.get();s.extras.text(text,x,y,argb,shadow,matrix(),style());}
    public static int width(String text){return CURRENT.get().extras.width(text);}
    public static void quad(List<Vertex> vertices){if(vertices.size()!=4)throw new IllegalArgumentException("Beta quad requires four vertices");var s=CURRENT.get();s.extras.quad(List.copyOf(vertices),matrix(),s.texture,style());}
    public static void draw(ModelGeometryB173.Draw draw){var s=CURRENT.get();s.sink.accept(new ModelGeometryB173.Draw(draw.cuboids(),new Matrix4f(s.poses.peek()).mul(draw.transform()),draw.scale()),s.texture);}
}
