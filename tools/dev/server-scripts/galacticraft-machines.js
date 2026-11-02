// Oxygen collector -> compressor rig in the overworld at y 200 (submit galacticraft-lib.js first). Step 1: build. Read with galacticraft-machines-read.js a few seconds later.
var lv = GC.level(0);
var X = 202, Y = 200, Z = 202;
GC.load(lv, X, Z, 2);
GC.fill(lv, X - 4, Y - 1, Z - 4, X + 8, Y - 1, Z + 4, Blocks.STONE);
GC.fill(lv, X - 4, Y, Z - 4, X + 8, Y + 6, Z + 4, Blocks.AIR);
// leaves around the collector (it counts plants within 5 blocks)
GC.fill(lv, X - 3, Y, Z - 3, X + 3, Y + 4, Z + 3, Blocks.OAK_LEAVES);
GC.fill(lv, X - 1, Y, Z - 1, X + 4, Y + 1, Z + 1, Blocks.AIR);
GC.set(lv, X, Y, Z, "galacticraftcore", "collector");
GC.set(lv, X + 1, Y, Z, "galacticraftcore", "oxygen_compressor");
var col = GC.te(lv, X, Y, Z), comp = GC.te(lv, X + 1, Y, Z);
var o = "collector " + col + "\ncompressor " + comp + "\n";
if (col) { col.storage.setEnergyStored(20000); o += "collector energy " + col.getEnergyStoredGC() + " max " + col.getMaxEnergyStoredGC() + "\n"; }
if (comp) { comp.storage.setEnergyStored(20000); o += "compressor energy " + comp.getEnergyStoredGC() + "\n"; }
o;
