var Y0 = 150, X0 = 1000, Z0 = 1000;
var Caps = Java.type("net.neoforged.neoforge.capabilities.Capabilities");
var NFS = Java.type("net.neoforged.neoforge.fluids.FluidStack");
var FluidAction = Java.type("net.neoforged.neoforge.fluids.capability.IFluidHandler$FluidAction");
var SimAction = Java.type("net.neoforged.neoforge.items.IItemHandler");
var Dirs = Java.type("net.minecraft.core.Direction").values();
function capReport(tag, x, y, z) {
  var lv = L.lvl(), p = new L.BP(x, y, z), o = [];
  var items = [], fluids = [], energy = [];
  Java.from(Dirs).forEach(function (d) {
    var ih = lv.getCapability(Caps.ItemHandler.BLOCK, p, d); if (ih) items.push(d + ":" + ih.getSlots());
    var fh = lv.getCapability(Caps.FluidHandler.BLOCK, p, d); if (fh) fluids.push(d + ":" + fh.getTanks());
    var eh = lv.getCapability(Caps.EnergyStorage.BLOCK, p, d); if (eh) energy.push(d + ":" + eh.getEnergyStored() + "/" + eh.getMaxEnergyStored() + (eh.canReceive() ? "R" : "") + (eh.canExtract() ? "E" : ""));
  });
  var n = lv.getCapability(Caps.ItemHandler.BLOCK, p, null); var nf = lv.getCapability(Caps.FluidHandler.BLOCK, p, null); var ne = lv.getCapability(Caps.EnergyStorage.BLOCK, p, null);
  L.say("CAPS " + tag + " items[" + items.join(",") + "] fluids[" + fluids.join(",") + "] energy[" + energy.join(",") + "] null-side(items=" + (n != null) + " fluids=" + (nf != null) + " energy=" + (ne != null) + ")");
}
if (tick == 0) {
  L.platform(X0 - 4, Y0, Z0, 40, 30, 8);
  G.names = ["GRINDER", "ENGINE", "PUMP", "RESERVOIR", "ELECTRICMOTOR", "GENERATOR", "FERMENTER", "EXTRACTOR", "COMPACTOR", "BLASTFURNACE", "SCALECHEST", "CRAFTER", "FILLINGSTATION", "BIGFURNACE", "PIPE", "FUELLINE", "DYNAMO", "BOILER", "STEAMTURBINE", "FRACTIONATOR", "BUCKETFILLER", "SORTING", "ITEMFILTER", "HEATER", "COMPOSTER"];
  G.names.forEach(function (n, i) {
    var x = X0 + (i % 12) * 3, z = Z0 + 2 + Math.floor(i / 12) * 4;
    var r = L.place(L.stackOf(n), x, Y0, z, null, 0);
    if (r.r != "CONSUME") L.say("place " + n + " -> " + r.r);
  });
}
if (tick == 40) {
  G.names.forEach(function (n, i) {
    var x = X0 + (i % 12) * 3, z = Z0 + 2 + Math.floor(i / 12) * 4;
    capReport(n + " " + (L.te(x, Y0, z) ? L.te(x, Y0, z).getClass().getSimpleName() : "no te"), x, Y0, z);
  });
}
L.run(tick);
