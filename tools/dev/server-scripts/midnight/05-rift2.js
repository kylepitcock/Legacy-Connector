var res = "";
EB = T("org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122");
var found = [];
server.getAllLevels().forEach(function (l) {
  l.getAllEntities().forEach(function (e) {
    if (String(e.getType().builtInRegistryHolder().key().location()) == "midnight:rift") {
      var r = EB.wrap(e);
      res += P("rift", function () { return String(e.level().dimension().location()) + " " + e.position() + " open=" + r.isOpen() + " unstable=" + r.isUnstable() + " used=" + r.wasUsed() + " bridgeValid=" + r.isBridgeValid() + " endpointLoaded=" + r.isEndpointLoaded() + " endpointDim=" + r.getEndpointDimension() + " age=" + e.tickCount + " removed=" + e.isRemoved(); });
    }
  });
});
res + "player " + sp.position() + " " + sp.level().dimension().location();
