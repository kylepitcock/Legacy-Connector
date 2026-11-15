// Issue #152, node in a jar: builds the 3x3x3 structure TC4's WandManager.fitNodeJar expects and reports each check.
// Submit lib.js first, then this file; result: tools/dev/server-script.sh 'JARTEST.run()'
var JARTEST = (function () {
  var WM = Java.type("thaumcraft.common.items.wands.WandManager");
  var OD = Java.type("legacyconnector.v1_7_10.net.minecraftforge.oredict.OreDictionary");
  var BP = Java.type("net.minecraft.core.BlockPos");
  var Sup = Java.extend(Java.type("java.util.function.Supplier"), { get: function () { return JARTEST.body(); } });
  function body() {
    var o = [];
    lvl.setChunkForced(0, 0, true);
    var gy = lvl.getHeight(Java.type("net.minecraft.world.level.levelgen.Heightmap").Types.WORLD_SURFACE, 3, 3);
    var x = 2, y = gy + 3, z = 2;
    for (var dy = 0; dy < 3; dy++) for (var i = 0; i < 3; i++) for (var j = 0; j < 3; j++) {
      var by = y + dy;
      if (dy == 2) w.setBlock(x + i, by, z + j, LBlocks.wooden_slab, 0, 3);
      else w.setBlock(x + i, by, z + j, LBlocks.glass, 0, 3);
    }
    var gb = w.getBlock(x, y, z);
    o.push("glass readback: " + gb + " == Blocks.glass: " + (gb == LBlocks.glass));
    var sb = w.getBlock(x, y + 2, z), sm = w.getBlockMetadata(x, y + 2, z);
    o.push("slab readback: " + sb + " meta=" + sm + " == wooden_slab: " + (sb == LBlocks.wooden_slab));
    var st = new LItemStack(sb, 1, sm);
    var ores = OD.getOres("slabWood");
    var hit = false; for (var k = 0; k < ores.size(); k++) if (OD.itemMatches(ores.get(k), st, false)) hit = true;
    o.push("slabWood ores=" + ores.size() + " matches slab stack: " + hit);
    var MB2 = Java.type("net.minecraft.world.level.block.Blocks");
    lvl.setBlock(new BP(x, y + 2, z), MB2.OAK_SLAB.defaultBlockState().setValue(Java.type("net.minecraft.world.level.block.SlabBlock").TYPE, Java.type("net.minecraft.world.level.block.state.properties.SlabType").TOP), 3);
    sb = w.getBlock(x, y + 2, z); sm = w.getBlockMetadata(x, y + 2, z);
    o.push("modern top oak slab readback: " + sb + " meta=" + sm + " == wooden_slab: " + (sb == LBlocks.wooden_slab));
    o.push("fitNodeJar (no node): " + WM.fitNodeJar(w, x, y, z));
    return o.join("\n");
  }
  return { body: body, run: function () { return "" + server.submit(new Sup()).get(); } };
})();
"loaded";
