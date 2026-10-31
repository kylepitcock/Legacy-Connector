var out = "";
var CT = T("Reika.ChromatiCraft.Registry.ChromaTiles");
function putTile(lw, tile, x, y, z) {
  var ok = lw.setBlock(x, y, z, tile.getBlock(), tile.getBlockMetadata(), 3);
  return lw.getTileEntity(x, y, z);
}
if (tick == 0) {
  var level = server.overworld();
  sp = realPlayer("CCPlayer");
  lw = lworld(level);
  p = legacy(sp);
  var Blocks = T("net.minecraft.world.level.block.Blocks");
  for (var cx = -1; cx <= 2; cx++) for (var cz = -1; cz <= 2; cz++) level.setChunkForced(cx, cz, true);
  for (var cx = -1; cx <= 2; cx++) for (var cz = -1; cz <= 2; cz++) level.getChunk(cx, cz);
  for (var x = -8; x <= 24; x++) for (var z = -8; z <= 24; z++) {
    level.setBlock(bp(x, 200, z), Blocks.STONE.defaultBlockState(), 2);
    for (var y = 201; y < 215; y++) level.setBlock(bp(x, y, z), Blocks.AIR.defaultBlockState(), 2);
  }
  sp.teleportTo(level, 8.5, 201, 8.5, 0, 0);
  out += P("research-walk", function () {
    // a fresh player walks the journal: take the next available fragments, advance the level when it unlocks
    var CRM = T("Reika.ChromatiCraft.Magic.Progression.ChromaResearchManager").instance;
    var PM = T("Reika.ChromatiCraft.Magic.Progression.ProgressionManager").instance;
    var s = "start level " + CRM.getPlayerResearchLevel(p) + " fragments " + CRM.getFragments(p).size() + "\n";
    var given = 0, rounds = 0;
    for (; rounds < 200; rounds++) {
      var nx = Java.from(CRM.getNextResearchesFor(p).toArray());
      if (nx.length == 0) {
        // all reachable fragments given; try stepping the level (needs the progress stages)
        var lv = CRM.getPlayerResearchLevel(p);
        var adv = Java.from(CRM.getResearchLevelAdvancementBlocks(p).toArray());
        if (adv.length > 0) {
          adv.forEach(function (a) { try { a.stepPlayerTo(p); } catch (e) {} });
        }
        CRM.checkForUpgrade(p);
        if (String(CRM.getPlayerResearchLevel(p)) == String(lv) && !CRM.stepPlayerResearchLevel(p, lv)) break;
        continue;
      }
      nx.forEach(function (r) { if (CRM.givePlayerFragment(p, r, false)) given++; });
    }
    return s + "rounds " + rounds + " fragments given " + given + " final level " + CRM.getPlayerResearchLevel(p) + " missing " + CRM.getMissingResearch(p).size();
  });
  T("Reika.ChromatiCraft.Magic.Progression.ProgressionManager").instance.maxPlayerProgression(p, false);
  T("Reika.ChromatiCraft.Magic.Progression.ChromaResearchManager").instance.maxPlayerResearch(p, false);
  sp.setGameMode(T("net.minecraft.world.level.GameType").CREATIVE);
  out += P("pylon", function () {
    var CS = T("Reika.ChromatiCraft.Registry.ChromaStructures");
    var CE = T("Reika.ChromatiCraft.Registry.CrystalElement");
    var arr = CS.PYLON.getArray(lw, 0, 201, 0, CE.BLACK);
    arr.place();
    s0 = "minY " + arr.getMinY() + " maxY " + arr.getMaxY() + " minX " + arr.getMinX() + " maxX " + arr.getMaxX() + " | ";
    lw.setBlock(0, 201, 0, CT.PYLON.getBlock(), CT.PYLON.getBlockMetadata(), 3);
    putPylon = lw.getTileEntity(0, 201, 0);
    putPylon.setColor(CE.BLACK);
    pylons = [putPylon];
    [[CE.RED, 0, -9], [CE.BROWN, 0, 9]].forEach(function (pc) {
      CS.PYLON.getArray(lw, pc[1], 201, pc[2], pc[0]).place();
      lw.setBlock(pc[1], 201, pc[2], CT.PYLON.getBlock(), CT.PYLON.getBlockMetadata(), 3);
      var pt = lw.getTileEntity(pc[1], 201, pc[2]); pt.setColor(pc[0]); pylons.push(pt);
    });
    pylons.forEach(function (q) { q.validateMultiblock(CS.PYLON.getArray(lw, q.xCoord, 201, q.zCoord, q.getColor())); });
    var s = s0 + "pylon struct " + arr.getSize() + " blocks; tile at base " + putPylon;
    T("Reika.DragonAPI.Instantiable.Data.BlockStruct.FilledBlockArray").logMismatches = true;
    var arr2 = CS.PYLON.getArray(lw, 0, 201, 0, CE.BLACK);
    s += "\n match " + arr2.matchInWorld() + " errors " + arr2.countErrors();
    var cl = T("Reika.ChromatiCraft.TileEntity.Networking.TileEntityCrystalPylon").getPowerCrystalLocations();
    s += "\n crystal offsets " + cl;
    var cnt = {};
    for (var yy = 192; yy <= 215; yy++) for (var xx = -4; xx <= 4; xx++) for (var zz = -4; zz <= 4; zz++) {
      var st = level.getBlockState(bp(xx, yy, zz)); var k = String(st.getBlock());
      if (k.indexOf("block_rune") >= 0) s += "\n   rune " + xx + "," + yy + "," + zz;
      if (k.indexOf("chromaticraft") >= 0) { cnt[k] = (cnt[k] || 0) + 1; if ((k.indexOf("pylon") >= 0 && k.indexOf("struct") < 0) || k.indexOf("tileentity") >= 0) s += "\n   " + xx + "," + yy + "," + zz + " " + st; }
      var t = lw.getTileEntity(xx, yy, zz); if (t) { s += "\n   TE " + xx + "," + yy + "," + zz + " " + t; if (!putPylon) putPylon = t; } }
    s += "\n" + JSON.stringify(cnt);
    return s;
  });
  out += P("table", function () {
    for (var yy = 201; yy < 206; yy++) level.setBlock(bp(8, yy, 0), Blocks.STONE.defaultBlockState(), 2);
    var rr = placeTile(sp, level, lw, CT.TABLE, 8, 206, 0); table = rr[1];
    return "table result " + rr[0] + " " + table + " tier " + (table ? table.getTier() : null);
  });
  out += P("recipe", function () {
    var CRT = T("Reika.ChromatiCraft.Auxiliary.RecipeManagers.RecipesCastingTable");
    var rs = Java.from(CRT.instance.getAllRecipes().toArray());
    var rec = null;
    rs.forEach(function (r) { if (!rec && String(r.type) == "CRAFTING" && String(r.getOutput()).indexOf("chroma.glow@0") >= 0) rec = r; });
    recipe = rec;
    var arr = Java.from(rec.getInputArray()).map(function (o) { return (o && o.get) ? o.get(0) : o; });
    var s = "out " + rec.getOutput() + " slots:";
    for (var i = 0; i < arr.length; i++) { s += " " + i + "=" + arr[i]; if (arr[i]) { var c = arr[i].copy(); if (c.getItemDamage() == 32767) c.setItemDamage(0); table.setInventorySlotContents(i, c); } }
    return s + "; match " + rec.match(table);
  });
}
if (tick == 5) {
  out += P("trigger", function () {
    var dbg = "owned " + table.isOwnedByPlayer(p) + " canRun " + table.getActiveRecipe().canRunRecipe(table, p) + " placer " + table.getPlacer() + " player " + p + " inWorld " + T("Reika.DragonAPI.Libraries.ReikaEntityHelper").isInWorld(p) + " ";
    var r = table.triggerCrafting(p);
    return dbg + "trigger " + r + " crafting " + table.isCrafting() + " ready " + table.isReadyToCraft() + " energy " + table.getRequiredEnergy();
  });
}
if (tick == 30 || tick == 100) {
  out += P("net@" + tick, function () {
    var CN = T("Reika.ChromatiCraft.Magic.Network.CrystalNetworker").instance;
    var CE = T("Reika.ChromatiCraft.Registry.CrystalElement");
    var s = "pylon " + putPylon + " energy " + (putPylon && putPylon.getEnergy ? putPylon.getEnergy(CE.BLACK) : "?") + " color " + (putPylon && putPylon.getColor ? putPylon.getColor() : "?") + "\n";
    s += "table class " + table.getClass() + " energy " + table.getEnergy() + "\n";
    s += "flows " + CN.getActiveFlows(lw) + "\n";
    s += "pylons " + pylons.map(function (q) { return q.getColor() + ":" + q.getEnergy(q.getColor()) + (q.hasStructure() ? "S" : "-"); }).join(" ") + " req " + table.getRequiredEnergy() + " recipe needs " + recipe.getInputElements(false) + " conn RED " + CN.checkConnectivity(CE.RED, table) + "\n";
    s += "nearby RED " + CN.getNearbyPylons(lw, 4, 201, 0, CE.RED, 40, false) + " all " + CN.getAllNearbyPylons(table, 40, false) + " ticks " + table.ticksExisted + " pylonTicks " + pylons[0].ticksExisted + " getReceiveRange " + table.getReceiveRange() + "\n";
    var tf = CN.getClass().getDeclaredField("tiles"); tf.setAccessible(true); s += "tiles map " + tf.get(CN) + "\n";
    var m = CN.getClass().getDeclaredMethod("checkLOS", T("Reika.ChromatiCraft.Magic.Interfaces.CrystalTransmitter").class, T("Reika.ChromatiCraft.Magic.Interfaces.CrystalReceiver").class); m.setAccessible(true);
    s += "LOS pylon0->table " + m.invoke(CN, pylons[0], table) + " pylon1->table " + m.invoke(CN, pylons[1], table) + "\n";
    s += "canMakeConnection " + CN.canMakeConnection(pylons[1], table, CE.RED) + " canReceiveFrom " + table.canReceiveFrom(pylons[1]) + " canTransmitTo " + pylons[1].canTransmitTo(table) + " isCondRED " + pylons[1].isConductingElement(CE.RED) + " range " + pylons[1].getSendRange() + " dist " + pylons[1].getDistanceSqTo(8, 206, 0) + "\n";
    s += "conn BLACK " + CN.checkConnectivity(CE.BLACK, table) + " hasFlow " + CN.hasFlowTo(table, CE.BLACK, lw) + "\n";
    return s;
  });
}
if (tick == 120) {
  out += P("ASSERT cast output", function () {
    var AABB = T("net.minecraft.world.phys.AABB");
    var ents = server.overworld().getEntitiesOfClass(T("net.minecraft.world.entity.item.ItemEntity").class, new AABB(0, 195, -15, 20, 215, 15));
    var found = [];
    ents.forEach(function (e) { found.push(e.getItem().toString()); });
    var slots = []; for (var i = 0; i < 9; i++) slots.push(String(table.getStackInSlot(i)));
    var glow = found.filter(function (f) { return f.indexOf("glow") >= 0; });
    return (glow.length > 0 ? "PASS" : "FAIL") + " glow stacks: " + glow.join(", ") + " (of " + found.length + " items) slots " + slots.join(",");
  });
}
if (tick > 5 && tick % 40 == 0 && tick <= 400) {
  out += P("table@" + tick, function () {
    return "crafting " + table.isCrafting() + " ctick " + table.getCraftingTick() + " energy " + table.getEnergy() + " slot0 " + table.getStackInSlot(0);
  });
}
out;
