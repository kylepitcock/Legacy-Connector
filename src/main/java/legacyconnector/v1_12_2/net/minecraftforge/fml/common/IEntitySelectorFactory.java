package legacyconnector.v1_12_2.net.minecraftforge.fml.common;

import com.google.common.base.Predicate;
import java.util.List;
import java.util.Map;
import legacyconnector.v1_12_2.net.minecraft.command.ICommandSender;
import legacyconnector.v1_12_2.net.minecraft.entity.Entity;
import legacyconnector.v1_12_2.net.minecraft.util.math.Vec3d;

/** A mod's own target selector arguments: filters for the entities a selector with them names. */
public interface IEntitySelectorFactory {
    List<Predicate<Entity>> createPredicates(Map<String, String> arguments, String mainSelector, ICommandSender sender, Vec3d position);
}
