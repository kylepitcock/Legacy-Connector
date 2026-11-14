// Generates chunks around the origin and counts non-rock blocks (ores) at y=1, y=0 and y=-1, and the vertical pairs (0,-1) and (1,0) of
// them: a vein cut at y=0 shows as many ore blocks at y=0 and few ores directly below (#82, CustomOreGen).
var R = 10;
var T_ = function (n) { return Java.type(n); };
var level = server.overworld();
var Blocks = T_("net.minecraft.world.level.block.Blocks");
if (typeof ring === "undefined") { ring = 0; c1 = 0; c0 = 0; cm1 = 0; pair01 = 0; pair10 = 0; }
function ore(b) {
  return !(b.isAir() || b.is(Blocks.STONE) || b.is(Blocks.DEEPSLATE) || b.is(Blocks.BEDROCK) || b.is(Blocks.WATER) || b.is(Blocks.LAVA)
    || b.is(Blocks.DIRT) || b.is(Blocks.GRAVEL) || b.is(Blocks.GRANITE) || b.is(Blocks.DIORITE) || b.is(Blocks.ANDESITE)
    || b.is(Blocks.TUFF) || b.is(Blocks.SAND) || b.is(Blocks.SANDSTONE) || b.is(Blocks.GRASS_BLOCK) || b.is(Blocks.CALCITE) || b.is(Blocks.CLAY)
    || b.is(Blocks.NETHERRACK) || b.is(Blocks.COBBLESTONE) || b.is(Blocks.MOSSY_COBBLESTONE) || b.is(Blocks.INFESTED_STONE)
    || !b.getFluidState().isEmpty());
}
var __i = 0;
if (ring <= R && tick % 2 == 0) {
  for (var cx = -ring; cx <= ring; cx++) for (var cz = -ring; cz <= ring; cz++) {
    if (Math.max(Math.abs(cx), Math.abs(cz)) != ring) continue;
    var chunk = level.getChunk(cx, cz);
    for (var x = 0; x < 16; x++) for (var z = 0; z < 16; z++) {
      var bx = cx * 16 + x, bz = cz * 16 + z;
      var P = T_("net.minecraft.core.BlockPos");
      var a = ore(chunk.getBlockState(new P(bx, 1, bz))), b = ore(chunk.getBlockState(new P(bx, 0, bz))), c = ore(chunk.getBlockState(new P(bx, -1, bz)));
      if (a) c1++; if (b) c0++; if (c) cm1++; if (b && c) pair01++; if (a && b) pair10++;
    }
  }
  ring++;
}
if (ring == R + 1) { ring++; "ORECROSS y1=" + c1 + " y0=" + c0 + " ym1=" + cm1 + " pair(0,-1)=" + pair01 + " pair(1,0)=" + pair10; }
