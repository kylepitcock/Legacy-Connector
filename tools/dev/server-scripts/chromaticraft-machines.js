var out = "";
var CT = T("Reika.ChromatiCraft.Registry.ChromaTiles");
var CE = T("Reika.ChromatiCraft.Registry.CrystalElement");
var TILES = ["COLLECTOR", "ITEMCOLLECTOR", "FABRICATOR", "RITUAL", "STAND", "CHARGER", "TICKER", "FURNACE", "ENCHANTER", "BREWER", "TANK", "MINER", "INFUSER", "LAMP", "FENCE", "BEACON", "REPROGRAMMER", "STORAGE", "INSERTER", "PATTERNS"];
if (tick == 0) {
  var level = server.overworld();
  sp = realPlayer("CCMach"); lw = lworld(level); p = legacy(sp);
  var Blocks = T("net.minecraft.world.level.block.Blocks");
  for (var cx = -1; cx <= 3; cx++) for (var cz = -1; cz <= 3; cz++) level.setChunkForced(cx, cz, true);
  for (var cx = -1; cx <= 3; cx++) for (var cz = -1; cz <= 3; cz++) level.getChunk(cx, cz);
  for (var x = -8; x <= 60; x++) for (var z = -8; z <= 24; z++) {
    level.setBlock(bp(x, 200, z), Blocks.STONE.defaultBlockState(), 2);
    for (var y = 201; y < 215; y++) level.setBlock(bp(x, y, z), Blocks.AIR.defaultBlockState(), 2);
  }
  sp.teleportTo(level, 8.5, 201, 8.5, 0, 0);
  T("Reika.ChromatiCraft.Magic.Progression.ProgressionManager").instance.maxPlayerProgression(p, false);
  T("Reika.ChromatiCraft.Magic.Progression.ChromaResearchManager").instance.maxPlayerResearch(p, false);
  sp.setGameMode(T("net.minecraft.world.level.GameType").CREATIVE);
  placed = {};
  out += P("place machines", function () {
    var s = "";
    TILES.forEach(function (name, i) {
      var tile = CT[name];
      var x = 20 + (i % 10) * 3, z = 2 + Math.floor(i / 10) * 4;
      try {
        var rr = placeTile(sp, level, lw, tile, x, 201, z);
        placed[name] = [x, 201, z];
        s += name + "=" + (rr[1] ? rr[1].getClass().getSimpleName() : "NOTILE(" + rr[0].split(" ")[0] + ")") + " ";
      } catch (e) { s += name + "=EXC(" + e + ")"; try { var st = e.getStackTrace(); for (var q = 0; q < 8 && q < st.length; q++) s += "\n     at " + st[q]; } catch (x) {} s += "\n"; }
    });
    return s;
  });
  out += P("fluids registered", function () {
    var BI = T("net.minecraft.core.registries.BuiltInRegistries"); var l = [];
    BI.FLUID.keySet().forEach(function (k) { if (k.getNamespace() == "chromaticraft") l.push(k.getPath()); });
    return l.join(",");
  });
  out += P("pylon (chargeable)", function () {
    var CS = T("Reika.ChromatiCraft.Registry.ChromaStructures");
    CS.PYLON.getArray(lw, 0, 201, 0, CE.BLUE).place();
    lw.setBlock(0, 201, 0, CT.PYLON.getBlock(), CT.PYLON.getBlockMetadata(), 3);
    pyl = lw.getTileEntity(0, 201, 0); pyl.setColor(CE.BLUE);
    pyl.validateMultiblock(CS.PYLON.getArray(lw, 0, 201, 0, CE.BLUE));
    return "pylon " + pyl + " color " + pyl.getColor() + " energy " + pyl.getEnergy(CE.BLUE);
  });
  out += P("chest next to item collector", function () {
    var pos = placed["ITEMCOLLECTOR"];
    level.setBlock(bp(pos[0], 202, pos[2]), Blocks.CHEST.defaultBlockState(), 3);
    chestPos = bp(pos[0], 202, pos[2]);
    var te = lw.getTileEntity(pos[0], pos[1], pos[2]);
    var CEs = CE.values();
    for (var i = 0; i < CEs.length; i++) te.setEnergy(CEs[i], te.getMaxStorage(CEs[i]));
    return "chest at " + chestPos + " collector energy set; required " + te.getRequiredEnergy();
  });
  out += P("chroma fluid source", function () {
    var BI = T("net.minecraft.core.registries.BuiltInRegistries"); var RL = T("net.minecraft.resources.ResourceLocation");
    var b = BI.BLOCK.get(RL.parse("chromaticraft:chromaticraft_block_chroma"));
    level.setBlock(bp(40, 205, 10), b.defaultBlockState(), 3);
    fluidPos = bp(40, 205, 10);
    return "block " + b + " state " + level.getBlockState(fluidPos) + " fluidstate " + level.getFluidState(fluidPos);
  });
}
if (tick == 3) {
  out += P("charge player at pylon", function () {
    sp.teleportTo(server.overworld(), 4.5, 201, 4.5, 0, 0);
    var PEB = T("Reika.ChromatiCraft.Magic.PlayerElementBuffer").instance;
    var before = PEB.getPlayerContent(p, CE.BLUE);
    var ok = 0;
    for (var i = 0; i < 20; i++) if (T("Reika.ChromatiCraft.Auxiliary.ChromaAux").chargePlayerFromPylon(p, pyl, CE.BLUE, 10, false)) ok++;
    return "BLUE before " + before + " after " + PEB.getPlayerContent(p, CE.BLUE) + " charge calls ok " + ok + " pylon energy " + pyl.getEnergy(CE.BLUE);
  });
  out += P("drop item for collector", function () {
    var pos = placed["ITEMCOLLECTOR"];
    var IE = T("net.minecraft.world.entity.item.ItemEntity");
    var st = new (T("net.minecraft.world.item.ItemStack"))(T("net.minecraft.world.item.Items").DIAMOND, 5);
    var e = new IE(server.overworld(), pos[0] + 2.5, 202, pos[2] + 0.5, st);
    server.overworld().addFreshEntity(e);
    return "dropped";
  });
}
if (tick == 120) {
  out += P("ASSERT item collector", function () {
    var be = server.overworld().getBlockEntity(chestPos);
    var s = "chest items:";
    var n = 0;
    if (be) for (var i = 0; i < be.getContainerSize(); i++) { var it = be.getItem(i); if (!it.isEmpty()) { s += " " + it; n++; } }
    var pos = placed["ITEMCOLLECTOR"];
    var te = lw.getTileEntity(pos[0], pos[1], pos[2]);
    var ents = server.overworld().getEntitiesOfClass(T("net.minecraft.world.entity.item.ItemEntity").class, new (T("net.minecraft.world.phys.AABB"))(pos[0] - 10, 200, pos[2] - 10, pos[0] + 10, 215, pos[2] + 10)).size();
    return (n > 0 ? "PASS" : "FAIL") + " " + s + "; tile " + te + "; loose item entities " + ents;
  });
  out += P("ASSERT chroma fluid flow", function () {
    var level = server.overworld(); var cnt = 0; var s = "";
    for (var dx = -6; dx <= 6; dx++) for (var dz = -6; dz <= 6; dz++) for (var dy = -6; dy <= 0; dy++) { var fs = level.getFluidState(fluidPos.offset(dx, dy, dz)); if (!fs.isEmpty()) cnt++; }
    return (cnt > 1 ? "PASS" : "FAIL") + " fluid cells near source: " + cnt + " source state " + level.getFluidState(fluidPos);
  });
}
if (tick == 130) {
  out += P("activate each machine", function () {
    var level = server.overworld(); var s = "";
    sp.setGameMode(T("net.minecraft.world.level.GameType").SURVIVAL);
    Object.keys(placed).forEach(function (name) {
      var pos = placed[name]; var bpos = bp(pos[0], pos[1], pos[2]);
      sp.setPos(pos[0] + 0.5, pos[1] + 1, pos[2] + 3.5);
      try {
        var hit = new (T("net.minecraft.world.phys.BlockHitResult"))(new (T("net.minecraft.world.phys.Vec3"))(pos[0] + 0.5, pos[1] + 0.5, pos[2] + 0.5), T("net.minecraft.core.Direction").UP, bpos, false);
        var r = level.getBlockState(bpos).useWithoutItem(level, sp, hit);
        sp.closeContainer();
        s += name + ":" + r + " ";
      } catch (e) { s += name + ":EXC(" + e + ") "; }
    });
    return s;
  });
}
out;
