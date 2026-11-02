// Helpers for 1.12.2 Galacticraft rigs on a -PserverScript server (submit this file first; globals persist).
var DM = Java.type("legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager");
var CM = Java.type("micdoodle8.mods.galacticraft.core.util.ConfigManagerCore");
var WB = Java.type("org.kylepitcock.legacyconnector.v1_12_2.bridge.WorldBridge1122");
var LW = Java.type("legacyconnector.v1_12_2.net.minecraft.world.World");
var BR = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var BP = Java.type("net.minecraft.core.BlockPos");
var RL = Java.type("net.minecraft.resources.ResourceLocation");
var Blocks = Java.type("net.minecraft.world.level.block.Blocks");
var GC = {
  level: function (dim) { return DM.mcc$level(dim === undefined ? 0 : dim); },
  // load/generate the chunks around (x,z) on the server thread and keep them forced
  load: function (lv, x, z, r) {
    r = r === undefined ? 2 : r;
    for (var cx = (x >> 4) - r; cx <= (x >> 4) + r; cx++) for (var cz = (z >> 4) - r; cz <= (z >> 4) + r; cz++) {
      lv.getChunk(cx, cz);
      lv.setChunkForced(cx, cz, true);
    }
  },
  block: function (ns, name) { return BR.BLOCK.get(RL.fromNamespaceAndPath(ns, name)); },
  set: function (lv, x, y, z, ns, name, state) {
    var b = GC.block(ns, name);
    lv.setBlock(new BP(x, y, z), state === undefined ? b.defaultBlockState() : state, 3);
  },
  fill: function (lv, x1, y1, z1, x2, y2, z2, blk) {
    var st = blk.defaultBlockState();
    for (var x = x1; x <= x2; x++) for (var y = y1; y <= y2; y++) for (var z = z1; z <= z2; z++) lv.setBlock(new BP(x, y, z), st, 2);
  },
  // the hosted 1.12.2 tile entity at a position
  te: function (lv, x, y, z) {
    var be = lv.getBlockEntity(new BP(x, y, z));
    return be == null || !be.legacy ? null : be.legacy();
  },
  state: function (lv, x, y, z) { return lv.getBlockState(new BP(x, y, z)); },
  player: function () { var l = server.getPlayerList().getPlayers(); return l.get(l.size() - 1); }
};
"galacticraft lib ok";
