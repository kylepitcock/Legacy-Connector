// Energy bridge probe (RF / Forge Energy / EU / MJ tiles of legacy mods as NeoForge energy), any legacy version.
// run: ./gradlew runServerSubTest -PautostopTicks=400 -PserverTickScriptFrom=100 -PserverTickScript=<abs path of this file>
// The block namespaces to probe come from -Dlegacyconnector.energyNamespaces=a,b (default: the Thermal mods).
// For every block of those namespaces with a block entity: places every state (up to 16), asks Capabilities.EnergyStorage.BLOCK for the
// sideless view and each side, pushes 1000 into each side that takes energy (simulated first), reads the stored amount, pulls 100 back out
// and prints one line per distinct result: "ENERGY-PROBE <block>: sides=<n with a capability> in=<best accepted> stored=<n>/<max> out=<n>".
var NAMESPACES = String(java.lang.System.getProperty("legacyconnector.energyNamespaces") || "thermalexpansion,thermaldynamics,thermalfoundation").split(",");
var R = Java.type("net.minecraft.core.registries.BuiltInRegistries");
var BP = Java.type("net.minecraft.core.BlockPos");
var Dir = Java.type("net.minecraft.core.Direction");
var CAP = Java.type("net.neoforged.neoforge.capabilities.Capabilities");
var out = "";
var X = 300, Z = 300, Y = 150;
function lv() { return server.overworld(); }
function view(pos, side) { return lv().getCapability(CAP.EnergyStorage.BLOCK, pos, side); }
if (tick == 3) {
  lv().getChunk(X >> 4, Z >> 4);
  var i = 0, found = 0, withCap = 0, moved = 0;
  var it = R.BLOCK.iterator();
  while (it.hasNext()) {
    var b = it.next();
    var key = String(R.BLOCK.getKey(b));
    var ns = key.split(":")[0];
    // "*" probes every namespace that is not a game or NeoForge one
    if (NAMESPACES.indexOf("*") >= 0 ? (ns == "minecraft" || ns == "neoforge" || ns == "legacyconnector") : NAMESPACES.indexOf(ns) < 0) continue;
    var states = b.getStateDefinition().getPossibleStates();
    var seen = {};
    for (var s = 0; s < states.size() && s < 16; s++) {
      var pos = new BP(X + (i % 40), Y, Z + Math.floor(i / 40) * 2);
      i++;
      lv().getChunk(pos.getX() >> 4, pos.getZ() >> 4);
      lv().setBlock(pos, states.get(s), 3);
      var be = lv().getBlockEntity(pos);
      if (be == null) continue;
      found++;
      // give a Thermal-style tile (a field "energyStorage" with setEnergyStored) 30000 RF so the extract direction can be measured
      try {
        var lg = be.legacy ? be.legacy() : null;
        for (var k = lg == null ? null : lg.getClass(); k != null; k = k.getSuperclass()) {
          var fld = null;
          try { fld = k.getDeclaredField("energyStorage"); } catch (e1) {}
          if (fld != null) { fld.setAccessible(true); fld.get(lg).setEnergyStored(30000); break; }
        }
      } catch (e2) {}
      var sides = 0, bestIn = -1, bestSide = null, bestOut = -1, bestOutSide = null, cls = "none";
      for (var d = -1; d < 6; d++) {
        var side = d < 0 ? null : Dir.values()[d];
        var c = view(pos, side);
        if (c == null) continue;
        sides++;
        cls = c.getClass().getSimpleName();
        var sim = c.canReceive() ? c.receiveEnergy(1000, true) : -1;
        if (sim > bestIn) { bestIn = sim; bestSide = side; }
        var simOut = c.canExtract() ? c.extractEnergy(100, true) : -1;
        if (simOut > bestOut) { bestOut = simOut; bestOutSide = side; }
      }
      var line = key + ": sides=" + sides + " cap=" + cls;
      if (sides > 0) {
        withCap++;
        var c2 = view(pos, bestSide);
        var before = c2.getEnergyStored();
        var inserted = bestIn > 0 ? c2.receiveEnergy(1000, false) : 0;
        var stored = c2.getEnergyStored();
        var c3 = view(pos, bestOutSide);
        var outSim = bestOut;
        var extracted = bestOut > 0 ? c3.extractEnergy(100, false) : bestOut;
        if (inserted > 0 || extracted > 0) moved++;
        line += " inSide=" + bestSide + " offered=" + bestIn + " in=" + inserted + " stored=" + before + "->" + stored + "/" + c2.getMaxEnergyStored() + " outSide=" + bestOutSide + " outSim=" + outSim + " out=" + extracted + " left=" + c3.getEnergyStored();
      }
      if (!seen[line]) { seen[line] = true; out += "ENERGY-PROBE " + line + " (meta " + s + ")\n"; }
    }
  }
  out += "ENERGY-PROBE summary: " + found + " block entities placed, " + withCap + " expose NeoForge energy, " + moved + " moved energy\n";
}
out;
