// Moon level: generate chunks (on the server thread), count blocks, read the provider (gravity, sky, breathable air). Needs galacticraft-dims.js semantics.
var DM = Java.type("legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager");
var CM = Java.type("micdoodle8.mods.galacticraft.core.util.ConfigManagerCore");
var WB = Java.type("org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122");
var LW = Java.type("legacyconnector.v1_12_2.net.minecraft.world.World");
var BR = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var BP = Java.type("net.minecraft.core.BlockPos");
var dimId = (typeof DIM_ID === "undefined") ? CM.idDimensionMoon : DIM_ID;
var level = DM.mcc$level(dimId);
var o = "level " + level.dimension().location() + "\n";
var t0 = java.lang.System.nanoTime();
var counts = {};
var top = 0;
for (var cx = -2; cx <= 2; cx++) for (var cz = -2; cz <= 2; cz++) {
  var ch = level.getChunk(cx, cz);
  var pos = new BP.MutableBlockPos();
  for (var x = 0; x < 16; x++) for (var z = 0; z < 16; z++) for (var y = 0; y < 256; y++) {
    var st = ch.getBlockState(pos.set(x | 0, y | 0, z | 0));
    if (st.isAir()) continue;
    var k = "" + BR.BLOCK.getKey(st.getBlock());
    counts[k] = (counts[k] || 0) + 1;
    if (y > top) top = y;
  }
}
o += "generated in " + ((java.lang.System.nanoTime() - t0) / 1e6) + " ms, top y " + top + "\n" + JSON.stringify(counts) + "\n";
var w = WB.wrap(level);
var prov = LW.mcc$get$provider(w);
o += "provider " + prov.getClass().getName() + " dim " + prov.getDimension() + " sky " + prov.hasSkyLight() + "\n";
try { o += "gravity " + prov.getGravity() + " celestial " + prov.getCelestialBody() + "\n"; } catch (e) { o += "gravity EXC " + e + "\n"; }
try { o += "breathable at spawn: " + Java.type("micdoodle8.mods.galacticraft.core.util.OxygenUtil").isAABBInBreathableAirBlock(w, new (Java.type("net.minecraft.util.math.AxisAlignedBB"))(0, 80, 0, 1, 82, 1)) + "\n"; } catch (e) { o += "breath EXC " + e + "\n"; }
o;
