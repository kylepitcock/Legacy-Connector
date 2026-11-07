package org.kylepitcock.legacyconnector.vb1_7_3;

import java.util.ArrayDeque;
import java.util.function.BiConsumer;
import java.util.function.Function;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/** Beta model rendering state, shared by the client adapter and graphics-free callback tests. */
public final class RendererContextB173 {
    private static final ThreadLocal<State> CURRENT=new ThreadLocal<>();
    private static final class State {
        final ArrayDeque<Matrix4f> poses=new ArrayDeque<>();
        final Function<String,ResourceLocation> textures;
        final BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink;
        ResourceLocation texture=ResourceLocation.withDefaultNamespace("missingno");
        int argb=-1;
        State(Function<String,ResourceLocation> textures,BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink){this.textures=textures;this.sink=sink;poses.push(new Matrix4f());}
    }
    public static final class Scope implements AutoCloseable {
        private final State previous;private boolean closed;
        private Scope(State previous){this.previous=previous;}
        @Override public void close(){if(!closed){closed=true;if(previous==null)CURRENT.remove();else CURRENT.set(previous);}}
    }
    private RendererContextB173(){}
    public static Scope open(Function<String,ResourceLocation> textures,BiConsumer<ModelGeometryB173.Draw,ResourceLocation> sink){var previous=CURRENT.get();CURRENT.set(new State(textures,sink));return new Scope(previous);}
    public static boolean active(){return CURRENT.get()!=null;}
    public static Matrix4f matrix(){return new Matrix4f(CURRENT.get().poses.peek());}
    public static int argb(){return CURRENT.get().argb;}
    public static void push(){var s=CURRENT.get();s.poses.push(new Matrix4f(s.poses.peek()));}
    public static void pop(){var s=CURRENT.get();if(s.poses.size()<=1)throw new IllegalStateException("Unbalanced Beta renderer matrix pop");s.poses.pop();}
    public static void translate(double x,double y,double z){CURRENT.get().poses.peek().translate((float)x,(float)y,(float)z);}
    public static void scale(float x,float y,float z){CURRENT.get().poses.peek().scale(x,y,z);}
    public static void rotate(float degrees,float x,float y,float z){float length=(float)Math.sqrt(x*x+y*y+z*z);if(length>0)CURRENT.get().poses.peek().rotate((float)Math.toRadians(degrees),x/length,y/length,z/length);}
    public static void transform(Matrix4f matrix){CURRENT.get().poses.peek().mul(matrix);}
    public static void color(float r,float g,float b,float a){CURRENT.get().argb=(channel(a)<<24)|(channel(r)<<16)|(channel(g)<<8)|channel(b);}
    private static int channel(float value){return Math.round(Math.max(0,Math.min(1,value))*255);}
    public static void bind(String path){var s=CURRENT.get();var texture=s.textures.apply(path);s.texture=texture==null?ResourceLocation.withDefaultNamespace("missingno"):texture;}
    public static void draw(ModelGeometryB173.Draw draw){var s=CURRENT.get();s.sink.accept(new ModelGeometryB173.Draw(draw.cuboids(),new Matrix4f(s.poses.peek()).mul(draw.transform()),draw.scale()),s.texture);}
}
