// For every hero: wear the suit, fire every registered Interaction on the server for the common input types, record which ones listen and which throw.
var Interaction = Java.type("com.fiskmods.heroes.common.interaction.Interaction");
var InteractionType = Java.type("com.fiskmods.heroes.common.interaction.InteractionType");
var InteractionInfo = Java.type("com.fiskmods.heroes.common.interaction.InteractionInfo");
var LSide = Java.type("legacyconnector.v1_7_10.cpw.mods.fml.relauncher.Side");
join("Sweeper"); flat(100,100,100); tp(100.5,101,100.5);
var allKeys = Java.from(Hero.REGISTRY.getKeys().toArray()).map(String).filter(function (k) { return k.indexOf("fisktag") < 0; });
var RES = {};
var steps = [];
allKeys.forEach(function (k) {
  steps.push(function () { strip(); wear(k); return 3; });
  steps.push(function () {
    var ok = [], bad = [];
    var types = [InteractionType.KEY_PRESS, InteractionType.RIGHT_CLICK_AIR];
    var reg = Interaction.REGISTRY;
    for (var i = 0; i < reg.size(); i++) {
      var it = reg.get(i), name = it.getClass().getSimpleName();
      types.forEach(function (ty) {
        try {
          var info = new InteractionInfo(ty, 0, 0, 0, 0, 0, 0);
          if (it.tryListen(L(), L(), info, LSide.SERVER)) ok.push(name + "/" + ty);
        } catch (e) { bad.push(name + "/" + ty + ": " + String(e).substring(0, 160)); }
      });
    }
    RES[k] = { worn: worn(), ok: ok, bad: bad };
    return 2;
  });
});
steps.push(function () { out("sweep done " + Object.keys(RES).length); });
scenario(steps);
"started " + steps.length
