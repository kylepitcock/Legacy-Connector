// Reads the rig of galacticraft-machines.js.
var lv = GC.level(0);
var X = 202, Y = 200, Z = 202;
var col = GC.te(lv, X, Y, Z), comp = GC.te(lv, X + 1, Y, Z);
var o = "";
if (col) o += "collector: oxygen " + col.getOxygenStored() + "/" + col.getMaxOxygenStored() + " lastCollected " + col.lastOxygenCollected + " active " + col.active + " energy " + col.getEnergyStoredGC() + " disabled " + col.disabled + " hasEnough " + col.hasEnoughEnergyToRun + " facing " + col.getFront() + " out " + col.getOxygenOutputDirections() + "\n";
if (comp) o += "compressor: oxygen " + comp.getOxygenStored() + "/" + comp.getMaxOxygenStored() + " energy " + comp.getEnergyStoredGC() + " in " + comp.getOxygenInputDirections() + " facing " + comp.getFront() + "\n";
o;
