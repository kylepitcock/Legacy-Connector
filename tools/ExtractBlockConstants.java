import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.zip.*;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

/**
 * Writes src/main/resources/mccompat/legacy/1.7.10/block-constants.txt: for every vanilla 1.7.10 block class, the
 * overrides of getRenderType / isOpaqueCube / renderAsNormalBlock / getRenderBlockPass that just return a constant
 * ("net/minecraft/block/BlockBush func_149645_b 1"). The shim's vanilla shells do not carry vanilla logic, so
 * Block.getRenderType() etc. consult this table by class hierarchy.
 *
 * usage (see tools/extract_block_constants.sh): java -cp asm:asm-tree ExtractBlockConstants.java <client-1.7.10.jar> <joined.srg> <out>
 */
public class ExtractBlockConstants {
    static final Set<String> WANTED = Set.of("func_149645_b", "func_149662_c", "func_149686_d", "func_149701_w");

    public static void main(String[] args) throws Exception {
        Map<String, String> classes = new HashMap<>();   // notch -> srg internal name
        Map<String, String> methods = new HashMap<>();   // notchOwner.notchName+desc -> srg method name
        for (String line : Files.readAllLines(Path.of(args[1]))) {
            String[] p = line.split(" ");
            if (p[0].equals("CL:")) classes.put(p[1], p[2]);
            else if (p[0].equals("MD:")) {
                String notch = p[1].substring(0, p[1].lastIndexOf('/')) + p[1].substring(p[1].lastIndexOf('/')); // owner/name
                String srgName = p[3].substring(p[3].lastIndexOf('/') + 1);
                if (WANTED.contains(srgName)) methods.put(p[1] + " " + p[2], srgName);
            }
        }
        List<String> out = new ArrayList<>();
        try (ZipFile zip = new ZipFile(args[0])) {
            for (var e = zip.entries(); e.hasMoreElements(); ) {
                ZipEntry entry = e.nextElement();
                if (!entry.getName().endsWith(".class")) continue;
                ClassNode node = new ClassNode();
                try (InputStream in = zip.getInputStream(entry)) {
                    new ClassReader(in).accept(node, ClassReader.SKIP_DEBUG | ClassReader.SKIP_FRAMES);
                }
                String srgClass = classes.get(node.name);
                if (srgClass == null || !srgClass.startsWith("net/minecraft/block/")) continue;
                for (MethodNode m : node.methods) {
                    String srgName = methods.get(node.name + "/" + m.name + " " + m.desc);
                    if (srgName == null) continue;
                    List<AbstractInsnNode> real = new ArrayList<>();
                    for (AbstractInsnNode i : m.instructions) if (i.getOpcode() >= 0) real.add(i);
                    if (real.size() != 2 || real.get(1).getOpcode() != Opcodes.IRETURN) continue;
                    AbstractInsnNode c = real.get(0);
                    Integer v = null;
                    int op = c.getOpcode();
                    if (op >= Opcodes.ICONST_M1 && op <= Opcodes.ICONST_5) v = op - Opcodes.ICONST_0;
                    else if (c instanceof IntInsnNode ii) v = ii.operand;
                    else if (c instanceof LdcInsnNode l && l.cst instanceof Integer i) v = i;
                    if (v != null) out.add(srgClass + " " + srgName + " " + v);
                }
            }
        }
        Collections.sort(out);
        Files.createDirectories(Path.of(args[2]).getParent());
        Files.write(Path.of(args[2]), out);
        System.out.println(out.size() + " constant overrides");
    }
}
