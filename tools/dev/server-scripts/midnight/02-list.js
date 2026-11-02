var BI = T("net.minecraft.core.registries.BuiltInRegistries");
var a = [];
BI.ITEM.keySet().forEach(function (k) { if (k.getNamespace() == "midnight") a.push(k.getPath()); });
"items " + a.length + ": " + a.join(" ");
