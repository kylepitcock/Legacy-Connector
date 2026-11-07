// 1.6.4 RF check: a mod tile implementing the old monolithic cofh IEnergyHandler (canInterface) must answer the shim's canConnectEnergy
// and be exposed as NeoForge energy. run: put EnderIO-1.6.4 (or any 1.6.4 mod with an IEnergyHandler tile) in run-subtest/mods/1.6.4, then
// ./gradlew runServerSubTest -PautostopTicks=400 -PserverTickScriptFrom=100 -PserverTickScript=<abs path of this file>
// optional -Dlegacyconnector.rfProbe=<legacy mod id>:<tile class> (default EnderIO:crazypants.enderio.conduit.TileConduitBundle)
var spec = String(java.lang.System.getProperty("legacyconnector.rfProbe") || "EnderIO:crazypants.enderio.conduit.TileConduitBundle").split(":");
var out = "";
if (tick == 0) {
  var LA = Java.type("org.kylepitcock.legacyconnector.api.LegacyApi");
  var opt = LA.classOf(spec[0], spec[1]);
  if (!opt.isPresent()) out += "class not found: " + spec + "\n";
  else {
    var cls = opt.get();
    var names = [];
    var ms = cls.getMethods();
    for (var i = 0; i < ms.length; i++) { var n = ms[i].getName(); if (n == "canInterface" || n == "canConnectEnergy" || n == "receiveEnergy" || n == "extractEnergy") names.push(n + "/" + ms[i].getParameterCount()); }
    out += "methods of " + cls.getName() + ": " + names.join(", ") + "\n";
    var ifaces = cls.getInterfaces(); var il = []; for (var k = 0; k < ifaces.length; k++) il.push(ifaces[k].getName());
    out += "interfaces: " + il.join(", ") + "\n";
    try {
      var tile = cls.getDeclaredConstructor().newInstance();
      var FD = Java.type("legacyconnector.v1_7_10.net.minecraftforge.common.util.ForgeDirection");
      var Dir = Java.type("net.minecraft.core.Direction");
      var EB = Java.type("org.kylepitcock.legacyconnector.v1_7_10.bridge.EnergyBridge");
      out += "canConnectEnergy(NORTH) = " + tile.canConnectEnergy(FD.NORTH) + "\n";
      var st = EB.forTile(tile, Dir.NORTH);
      out += "EnergyBridge.forTile = " + st + "\n";
      if (st != null) out += "receive 1000 simulate = " + st.receiveEnergy(1000, true) + ", capacity " + st.getMaxEnergyStored() + "\n";
    } catch (e) { out += "EXC " + e + "\n"; }
  }
}
out;
