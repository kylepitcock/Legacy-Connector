// tf2 ammo probe 3: a legacy listener on the player's inventoryContainer must hear about a changed slot within a tick or two (#184).
var EB = T("org.kylepitcock.legacyconnector.v1_12_2.bridge.EntityBridge1122");
var EP = T("legacyconnector.v1_12_2.net.minecraft.entity.player.EntityPlayer");
if (typeof SP3 === "undefined") {
  SP3 = realPlayer("Tester");
  HEARD = [];
  var lp = EB.wrap(SP3);
  var ic = EP.mcc$get$inventoryContainer(lp);
  var L = T("legacyconnector.v1_12_2.net.minecraft.inventory.IContainerListener");
  ic.addListener(new L({
    sendAllContents: function (c, l) {},
    sendSlotContents: function (c, i, s) { HEARD.push(i + ":" + s); },
    sendWindowProperty: function (c, a, b) {},
    sendAllWindowProperties: function (c, i) {}
  }));
  SP3.getInventory().setItem(10, byName("rafradek_tf2_weapons:ammo_pistol", 5));
  "listener added, stack placed";
} else "heard=" + HEARD.join(" | ");
