// For every hero: wear the suit, switch on every boolean DataVar one by one (as the client's key press would, then synced), tick a few times
// with the server's own handlers, and record whether the value stuck (the var's predicate accepted it) and whether it was still on 4 ticks later.
var DataVar = Java.type("com.fiskmods.heroes.common.data.var.DataVar");
var fields = Vars.class.getFields();
var boolVars = [];
for (var i = 0; i < fields.length; i++) {
  var v = fields[i].get(null);
  if (v instanceof DataVar && String(v.getDefault()) === "false") boolVars.push({ name: fields[i].getName(), v: v });
}
join("VarSweep"); flat(200, 200, 100); tp(200.5, 101, 200.5);
var VR = {};
var steps = [];
Java.from(Hero.REGISTRY.getKeys().toArray()).map(String).filter(function (k) { return k.indexOf("fisktag") < 0; }).forEach(function (k) {
  steps.push(function () { strip(); wear(k); VR[k] = { stuck: [], held: [], errors: [] }; return 3; });
  boolVars.forEach(function (b) {
    steps.push(function () {
      try { b.v.set(L(), true).sync(); } catch (e) { VR[k].errors.push(b.name + " set: " + String(e).substring(0, 100)); return 0; }
      var on = b.v.get(L()) === true;
      if (on) VR[k].stuck.push(b.name);
      b.on = on; return 4;
    });
    steps.push(function () {
      try {
        if (b.on && b.v.get(L()) === true) VR[k].held.push(b.name);
        b.v.set(L(), false).sync();
      } catch (e) { VR[k].errors.push(b.name + " reset: " + String(e).substring(0, 100)); }
    });
  });
});
steps.push(function () { out("var sweep done for " + Object.keys(VR).length + " heroes"); });
scenario(steps);
"started " + steps.length + " steps, " + boolVars.length + " boolean vars"
