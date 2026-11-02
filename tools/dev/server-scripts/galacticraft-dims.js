// Galacticraft dimensions: ids, levels created on first use. Submit with tools/dev/server-script.sh -f (PROBE_DIR=run-subtest/<dir> of the running -PserverScript server).
var DM = Java.type("legacyconnector.v1_12_2.net.minecraftforge.common.DimensionManager");
var CM = Java.type("micdoodle8.mods.galacticraft.core.util.ConfigManagerCore");
var o = "moon id " + CM.idDimensionMoon + " overworld " + CM.idDimensionOverworld + "\n";
var ids = DM.getIDs();
var s = [];
for (var i = 0; i < ids.length; i++) s.push(ids[i] + ":" + DM.getProviderType(ids[i]).getName());
o += s.join(" ") + "\n";
var lv = DM.mcc$level(CM.idDimensionMoon);
o += "moon level " + lv + "\n";
o;
