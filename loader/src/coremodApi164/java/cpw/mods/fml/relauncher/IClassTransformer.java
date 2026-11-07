package cpw.mods.fml.relauncher;

/** FML 1.5.2's class transformer interface (before the launch wrapper); a two-argument transform, run through the three-argument one. */
public interface IClassTransformer extends net.minecraft.launchwrapper.IClassTransformer {
    byte[] transform(String name, byte[] bytes);

    @Override
    default byte[] transform(String name, String transformedName, byte[] basicClass) {
        return transform(name, basicClass);
    }
}
