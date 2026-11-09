package legacyconnector.vb1_7_3.net.minecraft.nbt;

import java.io.*;
import net.minecraft.nbt.NbtAccounter;
import org.kylepitcock.legacyconnector.vb1_7_3.NbtB173;

/** Original binary NBT format, including gzip compression. */
public final class NbtIo {
    private NbtIo(){}
    public static NbtCompound readCompressed(InputStream in)throws IOException{
        return NbtB173.fromModern(net.minecraft.nbt.NbtIo.readCompressed(in,NbtAccounter.unlimitedHeap()));
    }
    public static void writeCompressed(NbtCompound tag,OutputStream out)throws IOException{
        net.minecraft.nbt.NbtIo.writeCompressed(NbtB173.toModern(tag),out);
    }
    public static NbtCompound read(DataInput in)throws IOException{return NbtB173.fromModern(net.minecraft.nbt.NbtIo.read(in));}
    public static void write(NbtCompound tag,DataOutput out)throws IOException{net.minecraft.nbt.NbtIo.write(NbtB173.toModern(tag),out);}
}
