// Mines every meta of a ChromatiCraft block (default TIEREDORE) with an iron pickaxe as a survival player (run with LIBS=lib).
var out = "";
if (tick == 0) {
  sp = realPlayer("CCOre"); sp.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL); p = legacy(sp);
  level = server.overworld(); lw = lworld(level);
  T("Reika.ChromatiCraft.Magic.Progression.ProgressionManager").instance.maxPlayerProgression(p, false);
  var Blocks = T("net.minecraft.world.level.block.Blocks");
  for (var x = -2; x < 40; x++) for (var z = -2; z < 6; z++) { level.setBlock(bp(x, 200, z), Blocks.STONE.defaultBlockState(), 2); for (var y = 201; y < 205; y++) level.setBlock(bp(x, y, z), Blocks.AIR.defaultBlockState(), 2); }
  sp.teleportTo(level, 1.5, 201, 1.5, 0, 0);
  var CB = T("Reika.ChromatiCraft.Registry.ChromaBlocks");
  var Hand = T("net.minecraft.world.InteractionHand");
  sp.setItemInHand(Hand.MAIN_HAND, new (T("net.minecraft.world.item.ItemStack"))(T("net.minecraft.world.item.Items").IRON_PICKAXE, 1));
  var AABB = T("net.minecraft.world.phys.AABB"), IE = T("net.minecraft.world.entity.item.ItemEntity");
  var blk = CB.TIEREDORE.getBlockInstance();
  out += P("tieredore", function () {
    var s = "";
    for (var m = 0; m < 16; m++) {
      var pos = bp(m * 2, 201, 2);
      lw.setBlock(m * 2, 201, 2, blk, m, 3);
      var st = level.getBlockState(pos);
      var drops = [];
      for (var k = 0; k < 6; k++) {
        level.getEntitiesOfClass(IE.class, new AABB(pos).inflate(4)).forEach(function (e) { e.discard(); });
        level.setBlock(pos, st, 3);
        sp.gameMode.destroyBlock(pos);
        level.getEntitiesOfClass(IE.class, new AABB(pos).inflate(4)).forEach(function (e) { drops.push(String(e.getItem())); e.discard(); });
      }
      s += m + " " + st.getBlock() + " drops(6 breaks): " + drops.join(",") + "\n";
    }
    return s;
  });
}
out;
