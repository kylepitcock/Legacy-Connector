/* Legacy Connector compatibility site. Single page, hash routes: #/mods[/<mc>], #/automated[/<mc>], #/about.
   Data: data/mods.json, data/reports.json, data/probe.json (fixed formats). window.LC_PRELOAD = {mods, reports, probe} skips fetching (tests). */
(function () {
  "use strict";
  var REPO = "kylepitcock/Legacy-Connector";
  var LABEL = { perfect: "Fully playable", playable: "Mostly playable", works: "Basics work", partial: "Partly working", loads: "Starts, untested", fails: "Does not start", crashes: "Crashes the game", untested: "Not tested" };
  var TIER = ["community", "maintainer", "client", "corpus", "probe"];
  var ORDER = ["perfect", "playable", "works", "partial", "loads", "fails", "crashes"];
  // legacy Minecraft versions with a tab: supported ones first, planned ones greyed out until they have data
  var SUPPORTED = ["1.7.10", "1.12.2"];
  var PLANNED = ["1.6.4", "1.5.2", "1.4.7", "1.20.1", "1.19.2", "1.18.2", "1.16.5", "1.2.5", "b1.7.3"]; // roadmap order (docs/wiki/Version-Roadmap.md)
  // Plain-language result categories of the automated check (probe.py "kind"). "unresolved" is the older, unsplit cause of runs made before the split.
  var KINDS = [
    { key: "links", label: "Ready to test", color: "var(--links)", hint: "translates, and nothing it needs is missing" },
    { key: "stubs", label: "Partly placeholder", color: "var(--stubs)", hint: "translates, but 10+ of its features are empty placeholders" },
    { key: "gap_shim", label: "Missing game features", color: "var(--gap-shim)", hint: "needs old Minecraft/Forge features we have not built yet" },
    { key: "gap_mod", label: "Needs another mod", color: "var(--gap-mod)", hint: "depends on another mod's code that is not available or does not match" },
    { key: "gap_lib", label: "Needs removed code", color: "var(--gap-lib)", hint: "uses old Java/library code that no longer exists" },
    { key: "unresolved", label: "Has missing pieces", color: "var(--unresolved)", hint: "something it needs is missing (older runs, cause not split)" },
    { key: "obsolete", label: "Not needed", color: "var(--obsolete)", hint: "engine patches with nothing to do on 1.21.1, and 1.20.1 libraries replaced by their native 1.21.1 build" }
  ];
  var RESLABEL = { links: "Ready", stubs: "Placeholder", gap_shim: "Game features", gap_mod: "Other mod", gap_lib: "Old code", unresolved: "Missing", obsolete: "Not needed" };
  // everything except "not needed" is a mod the layer has to handle; only "links" counts as ready
  function missingCount(c) { return (c.gap_shim || 0) + (c.gap_mod || 0) + (c.gap_lib || 0) + (c.unresolved || 0); }
  var PAGE_MODS = 80, PAGE_PROBE = 100, HISTORY_SHOWN = 8;

  var state = { route: "mods", q: "", status: "all", sort: "name", tab: "", all: [], modsShown: PAGE_MODS,
                runs: [], res: {}, rmc: "", rq: "", rfilter: "layer", rShown: 40, pmc: "", run: null, kind: "all", pq: "", psort: "name", pShown: PAGE_PROBE, histAll: false, loaded: false };

  // ---------- utils ----------
  function $(id) { return document.getElementById(id); }
  function el(tag, cls, text) { var e = document.createElement(tag); if (cls) e.className = cls; if (text != null) e.textContent = text; return e; }
  function svg(tag, attrs) { var e = document.createElementNS("http://www.w3.org/2000/svg", tag); for (var k in attrs) e.setAttribute(k, attrs[k]); return e; }
  function norm(s) { return String(s || "").toLowerCase().replace(/[^a-z0-9]+/g, ""); }
  function safeLink(url) { return /^https?:\/\//i.test(url || "") ? url : ""; }
  function reportUrl(name) {
    var u = "https://github.com/" + REPO + "/issues/new?template=compatibility_report.yml";
    if (name) u += "&mod=" + encodeURIComponent(name) + "&title=" + encodeURIComponent("[Compat] " + name);
    return u;
  }
  function ext(a, href) { a.href = href; a.target = "_blank"; a.rel = "noopener"; return a; }
  // "1.8 - 1.12.2" or "1.7.10 (forge)" -> the version a report is filed under; unknown text stays empty
  function mcOf(s) {
    s = String(s || "");
    var known = SUPPORTED.concat(PLANNED);
    for (var i = 0; i < known.length; i++) if (s.indexOf(known[i]) >= 0) return known[i];
    return "";
  }
  function rank(status) { var i = ORDER.indexOf(status); return i < 0 ? ORDER.length : i; }
  function byVersion(a, b) {
    var x = a.split("."), y = b.split(".");
    for (var i = 0; i < Math.max(x.length, y.length); i++) { var d = (+x[i] || 0) - (+y[i] || 0); if (d) return d; }
    return 0;
  }
  function total(c) { return (c.links || 0) + (c.stubs || 0) + missingCount(c) + (c.obsolete || 0); }
  function applicable(c) { return (c.links || 0) + (c.stubs || 0) + missingCount(c); }
  function rate(c) { var a = applicable(c); return a ? 100 * (c.links || 0) / a : 0; }
  function pct(n, t) { return t ? (100 * n / t).toFixed(1) + "%" : "0%"; }

  // ---------- theme ----------
  function setTheme(t, save) {
    document.documentElement.setAttribute("data-theme", t);
    var b = $("theme");
    if (b) b.setAttribute("aria-label", t === "dark" ? "Switch to light theme" : "Switch to dark theme");
    if (save) { try { localStorage.setItem("lc-theme", t); } catch (e) {} }
  }

  // ---------- tabs (roving tabindex, arrow keys) ----------
  // items: [{id, label, count, disabled, title}]
  function makeTabs(box, items, current, onSelect, panelId) {
    box.textContent = "";
    items.forEach(function (it) {
      var b = el("button", "tab", it.label);
      b.type = "button";
      b.setAttribute("role", "tab");
      b.dataset.id = it.id;
      if (it.count != null) b.appendChild(el("small", null, String(it.count)));
      if (it.title) b.title = it.title;
      if (panelId) b.setAttribute("aria-controls", panelId);
      if (it.disabled) { b.disabled = true; b.setAttribute("aria-disabled", "true"); }
      else {
        b.setAttribute("aria-selected", String(it.id === current));
        b.tabIndex = it.id === current ? 0 : -1;
        b.onclick = function () { onSelect(it.id); };
      }
      box.appendChild(b);
    });
    box.onkeydown = function (e) {
      if (e.key !== "ArrowRight" && e.key !== "ArrowLeft" && e.key !== "Home" && e.key !== "End") return;
      var tabs = Array.prototype.filter.call(box.querySelectorAll("[role=tab]"), function (t) { return !t.disabled; });
      var i = tabs.indexOf(document.activeElement);
      if (i < 0) return;
      i = e.key === "Home" ? 0 : e.key === "End" ? tabs.length - 1 : (i + (e.key === "ArrowRight" ? 1 : -1) + tabs.length) % tabs.length;
      e.preventDefault();
      tabs[i].focus();
      tabs[i].click();
      var again = box.querySelector("[role=tab][data-id='" + tabs[i].dataset.id + "']");
      if (again) again.focus();
    };
  }

  // ---------- merge of mod sources (ranking logic) ----------
  // one row per mod and legacy version: the same mod name can have a different result on 1.7.10 and 1.12.2
  function merge(maintainer, community) {
    var byKey = {}, withMc = {};
    function slot(name, mc) {
      var k = norm(name) + "|" + mc;
      if (!byKey[k]) byKey[k] = { name: name, mc: mc, entries: [] };
      return byKey[k];
    }
    maintainer.forEach(function (m) {
      var mc = mcOf(m.mc) || "1.7.10";
      withMc[norm(m.name) + "|" + mc] = true;
      slot(m.name, mc).entries.push({ source: m.source || "maintainer", version: m.version, mc: mc, status: m.status, notes: m.notes || "", date: m.date });
    });
    community.forEach(function (r) {
      var mc = mcOf(r.mc);
      if (!mc) {
        // no usable version in the report: file it where the mod already is, else under 1.7.10 (the first supported version)
        mc = SUPPORTED.filter(function (v) { return withMc[norm(r.mod) + "|" + v]; })[0] || "1.7.10";
      }
      slot(r.mod, mc).entries.push({ source: "community", version: "", mc: mc, status: r.status, notes: r.details, issue: r.issue,
        date: r.date, lc: r.lc, link: safeLink(r.link), open: r.open });
    });
    return Object.keys(byKey).map(function (k) {
      var m = byKey[k];
      // Weight: a user's report beats the maintainer's hand-tested result, which beats the in-game test, which beats the
      // automated startup test, which beats the link probe.
      // Within the winning tier the most common status wins, ties go to the newest entry. In-game tests take their best
      // result: a plain session only shows that content loads, a self test of the same mod shows that it functions.
      var pool = [], tierUsed = "";
      TIER.some(function (tier) {
        pool = m.entries.filter(function (e) { return e.source === tier; });
        tierUsed = tier;
        return pool.length > 0;
      });
      if (tierUsed === "client") {
        var best = Math.min.apply(null, pool.map(function (e) { return rank(e.status); }));
        pool = pool.filter(function (e) { return rank(e.status) === best; });
      }
      var counts = {}, newest = {};
      pool.forEach(function (e) {
        counts[e.status] = (counts[e.status] || 0) + 1;
        if ((e.date || "") >= (newest[e.status] || "")) newest[e.status] = e.date || "";
      });
      m.status = ORDER.slice().sort(function (a, b) {
        return (counts[b] || 0) - (counts[a] || 0) || (newest[b] || "").localeCompare(newest[a] || "") || ORDER.indexOf(a) - ORDER.indexOf(b);
      })[0];
      if (!counts[m.status]) m.status = "untested";
      // the maintainer's survival-ready verdict ("perfect") outranks a plain Works from any tier
      if ((m.status === "works" || m.status === "playable") && m.entries.some(function (e) { return e.source === "maintainer" && e.status === "perfect"; })) m.status = "perfect";
      else if (m.status === "works" && m.entries.some(function (e) { return e.source === "maintainer" && e.status === "playable"; })) m.status = "playable";
      m.entries.sort(function (a, b) { return TIER.indexOf(a.source) - TIER.indexOf(b.source) || (b.date || "").localeCompare(a.date || ""); });
      m.newest = m.entries.reduce(function (d, e) { return (e.date || "") > d ? e.date : d; }, "");
      return m;
    });
  }

  function versionsWithData() { var seen = {}; state.all.forEach(function (m) { seen[m.mc] = true; }); return seen; }
  function tabMods() { return state.all.filter(function (m) { return m.mc === state.tab; }); }
  function runsOf(mc) {
    return state.runs.filter(function (r) { return r.mc === mc; })
      // newest first; on the same day and version the larger population (the main corpus) leads, a smaller labelled one (CI) follows
      .sort(function (a, b) { return (b.date || "").localeCompare(a.date || "") || byVersion(b.version || "0", a.version || "0") || total(b.counts) - total(a.counts); });
  }
  function probeVersions() { return Array.from(new Set(state.runs.map(function (r) { return r.mc; }))).sort(byVersion); }

  // ---------- routing ----------
  function parseHash() {
    var h = decodeURIComponent((location.hash || "").replace(/^#\/?/, ""));
    var parts = h.split("/").filter(Boolean);
    if (!parts.length) return { route: "mods", mc: "" };
    if (/^\d/.test(parts[0])) return { route: "mods", mc: parts[0] }; // legacy links: #1.12.2
    var route = ["mods", "automated", "resources", "about"].indexOf(parts[0]) >= 0 ? parts[0] : "mods";
    return { route: route, mc: parts[1] || "" };
  }
  function setHash(route, mc) {
    var h = "#/" + route + (mc ? "/" + mc : "");
    if (history.replaceState) history.replaceState(null, "", location.pathname + location.search + h); else location.hash = h;
  }
  function applyRoute() {
    var p = parseHash();
    state.route = p.route;
    if (state.loaded) {
      if (p.route === "mods") {
        var seen = versionsWithData();
        if (seen[p.mc] && p.mc !== state.tab) { state.tab = p.mc; state.status = "all"; state.modsShown = PAGE_MODS; }
      } else if (p.route === "resources") {
        if (p.mc && state.res[p.mc] && p.mc !== state.rmc) { state.rmc = p.mc; state.rShown = 40; }
      } else if (p.route === "automated") {
        if (p.mc && probeVersions().indexOf(p.mc) >= 0 && p.mc !== state.pmc) { state.pmc = p.mc; state.run = runsOf(p.mc)[0]; state.kind = "all"; state.pShown = PAGE_PROBE; }
      }
    }
    Array.prototype.forEach.call(document.querySelectorAll("[data-view]"), function (v) { v.hidden = v.dataset.view !== state.route; });
    Array.prototype.forEach.call(document.querySelectorAll("#mainnav a"), function (a) {
      if (a.dataset.route === state.route) a.setAttribute("aria-current", "page"); else a.removeAttribute("aria-current");
    });
    $("glance").hidden = state.route === "automated" || state.route === "resources";
    document.querySelector(".hero").classList.toggle("compact", state.route === "automated" || state.route === "resources");
    var titles = { mods: "Mod compatibility", automated: "Automated results", resources: "Missing resources", about: "Report a mod and how it works" };
    document.title = titles[state.route] + " | Legacy Connector";
    if (state.loaded) renderRoute();
  }

  function renderRoute() {
    renderGlance();
    if (state.route === "mods") { renderTabs(); renderStats(); renderChips(); renderList(); }
    else if (state.route === "automated") renderAutomated();
    else if (state.route === "resources") renderResources();
  }

  // ---------- donut ----------
  // counts {links, unresolved, obsolete}; opts {size, R, r, interactive, active, centre, label}
  function donut(c, o) {
    var S = o.size, cx = S / 2, cy = S / 2, R = o.R, r = o.r, t = total(c);
    var root = svg("svg", { "class": o.cls || "donut", viewBox: "0 0 " + S + " " + S, role: "img",
      "aria-label": o.label || ("Share of mods per result: " + KINDS.map(function (k) { return (c[k.key] || 0) + " " + k.label.toLowerCase(); }).join(", ")) });
    var angle = -Math.PI / 2, paths = {};
    function p(rad, a) { return (cx + rad * Math.cos(a)).toFixed(2) + " " + (cy + rad * Math.sin(a)).toFixed(2); }
    if (!t) root.appendChild(svg("circle", { cx: cx, cy: cy, r: (R + r) / 2, fill: "none", stroke: "var(--line)", "stroke-width": R - r }));
    KINDS.forEach(function (k) {
      var n = c[k.key] || 0;
      if (!n) return;
      var frac = n / t, a1 = angle, a2 = angle + frac * 2 * Math.PI - (frac === 1 ? 0.0001 : 0);
      var large = a2 - a1 > Math.PI ? 1 : 0;
      var d = "M" + p(R, a1) + " A" + R + " " + R + " 0 " + large + " 1 " + p(R, a2) + " L" + p(r, a2) + " A" + r + " " + r + " 0 " + large + " 0 " + p(r, a1) + " Z";
      var path = svg("path", { d: d, fill: k.color });
      var title = svg("title", {}); title.textContent = k.label + ": " + n + " (" + pct(n, t) + ")";
      path.appendChild(title);
      if (o.interactive) {
        path.setAttribute("tabindex", "0"); path.setAttribute("role", "button");
        path.setAttribute("aria-label", k.label + ": " + n + " (" + pct(n, t) + "). Filter the list.");
        path.onclick = function () { o.interactive(k.key); };
        path.onkeydown = function (e) { if (e.key === "Enter" || e.key === " ") { e.preventDefault(); o.interactive(k.key); } };
        if (o.active && o.active !== "all" && o.active !== k.key) path.setAttribute("class", "dim");
      }
      root.appendChild(path);
      paths[k.key] = path;
      angle += frac * 2 * Math.PI;
    });
    if (o.centre !== false) {
      var big = svg("text", { x: cx, y: cy + (o.sub ? 4 : 6), "text-anchor": "middle", "font-size": o.big || 30 });
      big.textContent = t ? Math.round(rate(c)) + "%" : "n/a";
      root.appendChild(big);
      if (o.sub) {
        var sub = svg("text", { x: cx, y: cy + 24, "text-anchor": "middle", "font-size": 12, "class": "sub" });
        sub.textContent = o.sub;
        root.appendChild(sub);
      }
    }
    return root;
  }

  function stackBar(c) {
    var t = total(c), bar = el("div", "bar");
    bar.setAttribute("role", "img");
    bar.setAttribute("aria-label", KINDS.map(function (k) { return pct(c[k.key] || 0, t) + " " + k.label.toLowerCase(); }).join(", "));
    KINDS.forEach(function (k) { var s = el("span"); s.style.width = (t ? 100 * (c[k.key] || 0) / t : 0) + "%"; s.style.background = k.color; bar.appendChild(s); });
    return bar;
  }

  // ---------- hero summary ----------
  function renderGlance() {
    var box = $("glance");
    box.textContent = "";
    probeVersions().forEach(function (mc) {
      var runs = runsOf(mc), r = runs[0], c = r.counts;
      var a = el("a", "sumcard");
      a.href = "#/automated/" + mc;
      a.setAttribute("aria-label", "Minecraft " + mc + ": " + Math.round(rate(c)) + " percent of applicable mods are ready to test. Open automated results.");
      a.appendChild(donut(c, { size: 84, R: 40, r: 26, big: 17, label: "Ready to test: " + Math.round(rate(c)) + " percent" }));
      a.appendChild(el("div", "mc", "MC " + mc + " · automated"));
      var big = el("div", "big", pct(c.links || 0, applicable(c)));
      big.appendChild(el("small", null, "ready to test"));
      a.appendChild(big);
      var sub = el("div", "sub", (c.links || 0) + " of " + applicable(c) + " applicable mods tested " + r.date + (c.obsolete ? " (" + c.obsolete + (mc === "1.20.1" ? " libraries replaced by their native build" : " engine patches") + " skipped)" : ""));
      a.appendChild(sub);
      box.appendChild(a);
    });
    // in-game results from the mod list
    var seen = versionsWithData();
    var live = SUPPORTED.filter(function (v) { return seen[v]; });
    if (state.all.length) {
      var good = 0, n = 0;
      state.all.forEach(function (m) { n++; if (m.status === "perfect" || m.status === "playable" || m.status === "works") good++; });
      var a2 = el("a", "sumcard plain");
      a2.href = "#/mods";
      a2.appendChild(el("div", "mc", "Mod list · " + live.join(" + ")));
      var b2 = el("div", "big", String(n)); b2.appendChild(el("small", null, "mods rated"));
      a2.appendChild(b2);
      a2.appendChild(el("div", "sub", good + " work or better in game or in tests. Player reports outrank everything else."));
      box.appendChild(a2);
    }
  }

  // ---------- legend ----------
  function legendNode(open) {
    var d = el("details", "legend" + (open ? " open" : ""));
    if (open) d.open = true;
    d.appendChild(el("summary", null, "What the ratings mean"));
    var tiers = el("div", "tiers");
    [["perfect", "You can play it in survival. The maintainer played it and only small problems are known."],
     ["playable", "Played in survival and most of it works. Some mechanics or abilities are known to be broken or were not tried."],
     ["works", "The basics work: its blocks, items and mobs showed up and behaved in a test (by hand, or by the in-game test that found problems in under 10% of them). Deeper mechanics were not checked."],
     ["partial", "It loads and you can use part of it, but some features are known to be broken."],
     ["loads", "It starts without errors and passes the startup check, but nobody has played it yet."],
     ["fails", "The game starts but the mod is disabled because it failed while loading."],
     ["crashes", "Using it takes the whole game down."]].forEach(function (t) {
      var row = el("div", "tier");
      row.appendChild(el("span", "badge " + t[0], LABEL[t[0]]));
      row.appendChild(el("span", null, t[1]));
      tiers.appendChild(row);
    });
    var f = el("div", "tier");
    f.appendChild(el("span", "badge fails", "Fails"));
    var fs = el("span"); fs.appendChild(document.createTextNode("Disabled at load. "));
    fs.appendChild(el("b", null, "Crashes")); fs.appendChild(document.createTextNode(" takes the game down."));
    f.appendChild(fs);
    tiers.appendChild(f);
    d.appendChild(tiers);
    var note = el("p", "note");
    note.appendChild(el("b", null, "Most “Works” entries are simply not tested thoroughly yet"));
    note.appendChild(document.createTextNode(", they are not known to be broken."));
    d.appendChild(note);
    var det = el("details");
    det.appendChild(el("summary", null, "Where a badge comes from"));
    var p = el("p");
    function b(t) { p.appendChild(el("b", null, t)); }
    function t(s) { p.appendChild(document.createTextNode(s)); }
    t("Results are per legacy Minecraft version (tab): the same mod can differ on 1.7.10 and 1.12.2. A mod's badge comes from the highest-ranked source: ");
    b("player reports"); t(" (issues labelled "); p.appendChild(el("code", null, "compatibility")); t(") outrank ");
    b("maintainer"); t(" results, which outrank the "); b("in-game test");
    t(" (a real modpack session or the self test that places every block, uses every item and spawns every mob), which outranks the automated ");
    b("startup test"); t(", which outranks the offline "); b("automated check");
    t(" (translates the jar without starting the game and checks that everything it uses exists). The startup test only checks that a mod loads, so any player report replaces it.");
    det.appendChild(p);
    d.appendChild(det);
    return d;
  }

  // ---------- mods view ----------
  function renderTabs() {
    var seen = versionsWithData();
    var live = SUPPORTED.concat(Object.keys(seen).filter(function (v) { return SUPPORTED.indexOf(v) < 0; }).sort(byVersion));
    var items = live.map(function (v) { return { id: v, label: "MC " + v, count: state.all.filter(function (m) { return m.mc === v; }).length }; });
    PLANNED.filter(function (v) { return !seen[v]; }).forEach(function (v) { items.push({ id: v, label: "MC " + v, count: "planned", disabled: true, title: "Planned: no results yet" }); });
    makeTabs($("tabs"), items, state.tab, selectTab, "list");
    versionNote($("vnote-mods"), state.tab);
  }

  // Notes that belong to one legacy version (shown under its tab): 1.20.1 is experimental and its results mean something different from the older versions'.
  function versionNote(host, mc) {
    if (!host) return;
    host.textContent = "";
    host.hidden = mc !== "1.20.1" && mc !== "b1.7.3";
    if (host.hidden) return;
    function p(parts) {
      var e = el("p");
      parts.forEach(function (x) { e.appendChild(typeof x === "string" ? document.createTextNode(x) : x); });
      return e;
    }
    function code(t) { return el("code", null, t); }
    function b(t) { return el("b", null, t); }
    if (mc === "b1.7.3") {
      host.appendChild(p([b("Alpha (server-tested). "), "Beta 1.7.3 ModLoader and Forge 1.0 mods go in ", code("mods/b1.7.3/"), ". The layer was contributed by Zekken01. The results here come from that contributor's headless server tests (the mods translate, load, register their content and run their own callbacks); none was tested in a graphical client yet."]));
      host.appendChild(p([b("Archive audit. "), "An offline scan of a 1,040-file Beta 1.7.3 mod archive (1,222 mod payloads) found 544 payloads that need ModLoader, 74 ModLoaderMP and 42 Forge; 548 payloads replace no base game classes (the easiest to support), 572 replace some, 54 are libraries or resources and 20 bundle API patches. The full summary is in ", code("docs/data/b173-archive-audit-summary.json"), "."]));
      return;
    }
    host.appendChild(p([b("Alpha, on by default. "), "Forge 1.20.1 support is remapped in place onto NeoForge 1.21.1 (there is no old game to emulate) and is on by default (it only does something when a Forge 1.20.1 jar is installed); ", code("experimental.forge1201=false"), " in the Legacy Connector config turns it off. Early alpha: a first set of mods works (Terramity and Another Furniture run), most mods below do not link yet: the Minecraft 1.20.1 to 1.21.1 migration covers the common cases, not all of them."]));
    host.appendChild(p([b("What the automated result means here. "), b("Ready to test"), " means every reference of the translated jar resolves against Minecraft 1.21.1, NeoForge and the other jars of the run, including the layer's Forge facades. It is a link check only. A server start that registers a mod's content is a higher rating (", b("Starts, untested"), " and up, from the in-game test). Mods that fail the link check can still start, since unresolved code may sit on paths that never run, so a red result is not a verdict (on this tab 201CDoes not start201D on an automated row means unresolved references, not a failed start)."]));
    host.appendChild(p([b("Libraries. "), "Architectury, Curios, Patchouli, GeckoLib, Cloth Config, Resourceful Lib/Config, Puzzles Lib, Citadel, Jade and a few more use the ", b("native NeoForge 1.21.1 build"), " (same mod id): install it in the mods folder and the 1.20.1 jar is skipped (counted as \u201CNot needed\u201D). Without the 1.21.1 build the pack's 1.20.1 jar is translated as a fallback; with neither, the log names the build to install. Rewrites exist for Architectury, Curios and Patchouli; GeckoLib content mods are not fully supported yet."]));
  }

  function selectTab(v) {
    state.tab = v; state.status = "all"; state.modsShown = PAGE_MODS;
    setHash("mods", v);
    renderTabs(); renderStats(); renderChips(); renderList();
    var t = document.querySelector("#tabs [aria-selected=true]"); if (t) t.focus();
  }

  function setStatus(s) { state.status = s; state.modsShown = PAGE_MODS; renderStats(); renderChips(); renderList(); }

  function renderStats() {
    var box = $("stats");
    box.textContent = "";
    var mods = tabMods();
    var counts = { perfect: 0, playable: 0, works: 0, partial: 0, loads: 0, fails: 0, crashes: 0 };
    mods.forEach(function (m) { if (counts[m.status] != null) counts[m.status]++; });
    [["Mods listed", mods.length, "all", ""], ["Fully playable", counts.perfect, "perfect", "survival-ready"], ["Mostly playable", counts.playable, "playable", "most gameplay works"], ["Basics work", counts.works, "works", "not tested deeply"], ["Partly working", counts.partial, "partial", "some features broken"],
     ["Starts, untested", counts.loads, "loads", "passes the startup check"], ["Does not start", counts.fails, "fails", "disabled at startup"],
     ["Crashes the game", counts.crashes, "crashes", "takes the game down"]].forEach(function (s) {
      var d = el("button", "stat");
      d.type = "button";
      d.setAttribute("aria-pressed", String(state.status === s[2]));
      var b = el("b", null, String(s[1]));
      if (s[2] !== "all") b.style.color = "var(--" + s[2] + ")";
      d.appendChild(b);
      d.appendChild(el("span", null, s[0]));
      if (s[3]) d.appendChild(el("small", null, s[3]));
      d.onclick = function () { setStatus(state.status === s[2] ? "all" : s[2]); };
      box.appendChild(d);
    });
  }

  function renderChips() {
    var box = $("chips");
    box.textContent = "";
    var mods = tabMods();
    [["all", "All"], ["perfect", "Fully playable"], ["playable", "Mostly playable"], ["works", "Basics work"], ["partial", "Partly working"], ["loads", "Starts, untested"], ["fails", "Does not start"], ["crashes", "Crashes"]].forEach(function (c) {
      var n = c[0] === "all" ? mods.length : mods.filter(function (m) { return m.status === c[0]; }).length;
      var b = el("button", "chip", c[1] + " " + n);
      b.type = "button";
      b.setAttribute("aria-pressed", String(state.status === c[0]));
      b.onclick = function () { setStatus(c[0]); };
      box.appendChild(b);
    });
  }

  var SORTS = {
    name: function (a, b) { return a.name.localeCompare(b.name); },
    namez: function (a, b) { return b.name.localeCompare(a.name); },
    most: function (a, b) { return b.entries.length - a.entries.length || a.name.localeCompare(b.name); },
    fewest: function (a, b) { return a.entries.length - b.entries.length || a.name.localeCompare(b.name); },
    best: function (a, b) { return rank(a.status) - rank(b.status) || b.entries.length - a.entries.length || a.name.localeCompare(b.name); },
    worst: function (a, b) { return rank(b.status) - rank(a.status) || b.entries.length - a.entries.length || a.name.localeCompare(b.name); },
    newest: function (a, b) { return (b.newest || "").localeCompare(a.newest || "") || a.name.localeCompare(b.name); }
  };

  function modNode(m) {
    var d = el("details", "mod");
    var s = el("summary");
    s.appendChild(el("span", "badge " + m.status, LABEL[m.status]));
    s.appendChild(el("span", "name", m.name));
    s.appendChild(el("span", "meta", m.entries.length + (m.entries.length === 1 ? " result" : " results")));
    d.appendChild(s);
    // entries are built when the entry is first opened: keeps a long list cheap
    var built = false;
    d.addEventListener("toggle", function () {
      if (!d.open || built) return;
      built = true;
      var body = el("div", "body");
      m.entries.forEach(function (en) {
        var row = el("div", "entry");
        var top = el("div", "top");
        top.appendChild(el("span", "badge " + en.status, LABEL[en.status] || en.status));
        var who = en.source === "maintainer" ? "Maintainer" : en.source === "client" ? "In-game test" : en.source === "corpus" ? "Startup test" : en.source === "probe" ? "Automated check" : "Player report";
        top.appendChild(el("span", null, who + (en.version ? " · " + en.version : "")));
        if (en.issue) top.appendChild(ext(el("a", null, "#" + en.issue), "https://github.com/" + REPO + "/issues/" + en.issue));
        if (en.date) top.appendChild(el("span", "meta", en.date));
        if (en.link) { var l = ext(el("a", null, "mod page"), en.link); l.rel = "noopener nofollow"; top.appendChild(l); }
        row.appendChild(top);
        if (en.notes) row.appendChild(el("p", null, en.notes));
        body.appendChild(row);
      });
      var r = ext(el("a", "btn ghost", "Report this mod"), reportUrl(m.name));
      r.style.marginTop = "10px";
      body.appendChild(r);
      d.appendChild(body);
    });
    return d;
  }

  function renderList() {
    var list = $("list");
    list.textContent = "";
    var q = norm(state.q);
    var mods = tabMods();
    var shown = mods.filter(function (m) {
      return (state.status === "all" || m.status === state.status) && (!q || norm(m.name).indexOf(q) >= 0);
    }).sort(SORTS[state.sort] || SORTS.name);
    if (!shown.length) {
      var e = el("div", "empty", mods.length ? "No mod matches. " : "No data yet. ");
      e.appendChild(ext(el("a", null, "Report it"), reportUrl(state.q.trim())));
      list.appendChild(e);
      return;
    }
    var frag = document.createDocumentFragment();
    shown.slice(0, state.modsShown).forEach(function (m) { frag.appendChild(modNode(m)); });
    list.appendChild(frag);
    if (shown.length > state.modsShown) {
      var wrap = el("div", "more");
      var more = el("button", "btn alt", "Show more (" + (shown.length - state.modsShown) + " left of " + shown.length + ")");
      more.type = "button";
      more.onclick = function () { state.modsShown += PAGE_MODS; renderList(); };
      wrap.appendChild(more);
      list.appendChild(wrap);
    }
  }

  // ---------- automated view ----------
  function renderCompare() {
    var box = $("compare");
    box.textContent = "";
    var mcs = probeVersions();
    if (!mcs.length) { box.appendChild(el("p", "empty", "No probe results published yet.")); return; }
    mcs.forEach(function (mc) {
      var runs = runsOf(mc), r = runs[0], c = r.counts, t = total(c);
      var card = el("div", "cmp");
      card.appendChild(donut(c, { size: 120, R: 56, r: 36, big: 24, label: "MC " + mc + ": " + Math.round(rate(c)) + " percent of applicable mods are ready to test" }));
      card.appendChild(el("h3", null, "Minecraft " + mc));
      var big = el("div", "big", pct(c.links || 0, applicable(c)));
      card.appendChild(big);
      var sub = el("div", "sub", (c.links || 0) + " of " + applicable(c) + " applicable mods ready to test · " + t + " checked");
      card.appendChild(sub);
      var foot = el("div", "foot");
      foot.appendChild(stackBar(c));
      if (runs[1]) {
        var d = rate(c) - rate(runs[1].counts);
        var dl = el("span", "small");
        var dv = el("span", "delta " + (d > 0.05 ? "up" : d < -0.05 ? "down" : ""), (d > 0 ? "+" : "") + d.toFixed(1) + " pts");
        dl.appendChild(dv); dl.appendChild(document.createTextNode(" since " + runs[1].date));
        foot.appendChild(dl);
      }
      var go = el("button", "btn ghost", "Details");
      go.type = "button";
      go.onclick = function () { selectProbe(mc); document.getElementById("ptabs").scrollIntoView({ behavior: "smooth", block: "start" }); };
      foot.appendChild(go);
      card.appendChild(foot);
      box.appendChild(card);
    });
  }

  function selectProbe(mc) {
    state.pmc = mc; state.run = runsOf(mc)[0]; state.kind = "all"; state.pShown = PAGE_PROBE; state.histAll = false;
    setHash("automated", mc);
    renderAutomated();
    var t = document.querySelector("#ptabs [aria-selected=true]"); if (t && document.activeElement && document.activeElement.closest && document.activeElement.closest("#ptabs")) t.focus();
  }

  function renderRunSelect() {
    var sel = $("run");
    sel.textContent = "";
    var runs = runsOf(state.pmc);
    runs.forEach(function (r, i) {
      var o = el("option", null, (r.version ? "Connector " + r.version + ", " : "") + r.date + (r.source ? ", " + r.source + " only" : ", " + total(r.counts) + " mods") + (i === 0 ? " (latest)" : ""));
      o.value = String(i);
      sel.appendChild(o);
    });
    sel.value = String(Math.max(0, runs.indexOf(state.run)));
    sel.onchange = function () { state.run = runs[+sel.value]; state.kind = "all"; state.pShown = PAGE_PROBE; renderChart(); renderPList(); renderRunInfo(); };
    renderRunInfo();
  }
  function renderRunInfo() {
    var host = $("runinfo");
    host.textContent = "";
    var r = state.run;
    if (!r) return;
    host.appendChild(document.createTextNode(total(r.counts) + " mods checked"));
    // what changed since the previous run of this version (probe.py "vs"): fixed and regressed mods, so a release that breaks a mod is visible
    if (r.vs) {
      var fixed = r.vs.fixedCount || 0, bad = r.vs.regressedCount || 0;
      var tip = function (list) { return list.map(function (x) { return x[0]; }).join(", "); };
      var up = el("span", "delta up", " \u25B2 " + fixed + " fixed"); up.title = fixed ? tip(r.vs.fixed) : "nothing changed for the better";
      var down = el("span", "delta down", " \u25BC " + bad + " got worse"); down.title = bad ? tip(r.vs.regressed) : "nothing got worse";
      host.appendChild(document.createTextNode(" \u00B7 since " + (r.vs.version ? "Connector " + r.vs.version + ", " : "") + r.vs.date + ":"));
      host.appendChild(up); host.appendChild(document.createTextNode(","));
      host.appendChild(down);
    }
    // rendering smoke test: references that point at nothing (probe.py "assets")
    if (r.assets && r.assets.checked) {
      var ar = el("div", "small");
      ar.style.marginTop = "4px";
      ar.textContent = "Resource check: " + r.assets.modsWithLayerProblems + " of " + r.assets.checked + " mods lose " + r.assets.layerRefs +
        " model/texture references in translation (possible missing textures); " + r.assets.modsWithOwnProblems + " more have " + r.assets.ownRefs + " that are missing in the mod's own jar too.";
      host.appendChild(ar);
    }
    // the environment the run used, for telling a change of status from a change of tooling
    if (r.env) {
      var bits = [];
      if (r.env.neoforge) bits.push("NeoForge " + r.env.neoforge);
      if (r.env.shim) bits.push("shim " + r.env.shim);
      if (r.env.jars) bits.push(r.env.jars + " jars");
      if (bits.length) { var e = el("div", "small"); e.style.marginTop = "4px"; e.textContent = "Run environment: " + bits.join(" \u00B7 "); host.appendChild(e); }
    }
  }

  function setKind(k) { state.kind = state.kind === k ? "all" : k; state.pShown = PAGE_PROBE; renderChart(); renderPList(); }

  function renderChart() {
    var host = $("chart");
    host.textContent = "";
    var run = state.run;
    if (!run) { host.appendChild(el("p", "empty", "No probe run for this version yet.")); return; }
    var c = run.counts, t = total(c);
    host.appendChild(donut(c, { cls: "pie", size: 250, R: 116, r: 68, big: 34, sub: "ready to test", interactive: setKind, active: state.kind }));
    var box = el("div", "legendbox");
    KINDS.forEach(function (k) {
      var n = c[k.key] || 0;
      if (!n && k.key === "unresolved") return; // only older runs have this unsplit category
      var row = el("button", "row");
      row.type = "button";
      row.setAttribute("aria-pressed", String(state.kind === k.key));
      var dot = el("span", "dot"); dot.style.background = k.color;
      var label = el("span"); label.appendChild(document.createTextNode(k.label));
      label.appendChild(document.createElement("br"));
      label.appendChild(el("small", null, k.hint));
      row.appendChild(dot); row.appendChild(label); row.appendChild(el("b", null, n + " · " + pct(n, t)));
      row.onclick = function () { setKind(k.key); };
      box.appendChild(row);
    });
    host.appendChild(box);
  }

  function sparkline(runsAsc) {
    var W = 600, H = 130, L = 34, Rr = 14, T = 12, B = 26;
    var root = svg("svg", { "class": "spark", viewBox: "0 0 " + W + " " + H, role: "img", "aria-label": "Share ready to test per run, " + runsAsc.length + " runs" });
    [0, 50, 100].forEach(function (g) {
      var y = T + (H - T - B) * (1 - g / 100);
      root.appendChild(svg("line", { x1: L, x2: W - Rr, y1: y, y2: y, "class": "grid" }));
      var tx = svg("text", { x: L - 6, y: y + 4, "text-anchor": "end" }); tx.textContent = g + "%"; root.appendChild(tx);
    });
    var pts = runsAsc.map(function (r, i) {
      return [L + (W - L - Rr) * (runsAsc.length > 1 ? i / (runsAsc.length - 1) : 0.5), T + (H - T - B) * (1 - rate(r.counts) / 100), r];
    });
    root.appendChild(svg("polyline", { "class": "line", points: pts.map(function (p) { return p[0].toFixed(1) + "," + p[1].toFixed(1); }).join(" ") }));
    pts.forEach(function (p, i) {
      if (pts.length <= 40 || i === pts.length - 1) {
        var c = svg("circle", { cx: p[0].toFixed(1), cy: p[1].toFixed(1), r: pts.length > 20 ? 2.5 : 4 });
        var ti = svg("title", {}); ti.textContent = p[2].date + ": " + rate(p[2].counts).toFixed(1) + "% ready"; c.appendChild(ti);
        root.appendChild(c);
      }
    });
    [0, pts.length - 1].forEach(function (i, k) {
      if (k && i === 0) return;
      var tx = svg("text", { x: pts[i][0], y: H - 6, "text-anchor": i === 0 && pts.length > 1 ? "start" : "end" });
      tx.textContent = pts[i][2].date; root.appendChild(tx);
    });
    return root;
  }

  function renderTrend() {
    var host = $("trend");
    host.textContent = "";
    var runs = runsOf(state.pmc);
    if (!runs.length) return;
    if (runs.length > 1) host.appendChild(sparkline(runs.slice().reverse()));
    var table = el("table");
    var thead = el("thead"), head = el("tr");
    ["Date", "Connector", "Mods", "Ready", "Placeholder", "Missing", "Not needed", "Ready share", ""].forEach(function (h, i) { head.appendChild(el("th", i >= 2 && i <= 7 ? "num" : "", h)); });
    thead.appendChild(head); table.appendChild(thead);
    var tb = el("tbody");
    (state.histAll ? runs : runs.slice(0, HISTORY_SHOWN)).forEach(function (r) {
      var c = r.counts, t = total(c), tr = el("tr");
      [r.date, r.version || "-", t, c.links || 0, c.stubs || 0, missingCount(c), c.obsolete || 0, rate(c).toFixed(1) + "%"].forEach(function (v, i) {
        tr.appendChild(el("td", i >= 2 ? "num" : "", String(v)));
      });
      var td = el("td"); td.appendChild(stackBar(c)); tr.appendChild(td);
      tb.appendChild(tr);
    });
    table.appendChild(tb);
    var wrap = el("div", "tablewrap"); wrap.appendChild(table);
    host.appendChild(wrap);
    if (runs.length > HISTORY_SHOWN) {
      var more = el("div", "more");
      var b = el("button", "btn alt", state.histAll ? "Show fewer runs" : "Show all " + runs.length + " runs");
      b.type = "button";
      b.onclick = function () { state.histAll = !state.histAll; renderTrend(); };
      more.appendChild(b); host.appendChild(more);
    }
  }

  function renderPList() {
    var host = $("plist");
    host.textContent = "";
    var run = state.run;
    var info = $("filterinfo");
    if (!run) { info.textContent = ""; return; }
    if (!run.mods || !run.mods.length) {
      host.appendChild(el("p", "empty", "The per-mod list of this run was dropped to keep the data small; only the counts are kept."));
      info.textContent = "";
      return;
    }
    var q = state.pq.toLowerCase();
    var rows = run.mods.filter(function (m) {
      return (state.kind === "all" || m[2] === state.kind) && (!q || String(m[0]).toLowerCase().indexOf(q) >= 0);
    });
    if (state.psort === "result") {
      var order = { gap_shim: 0, gap_mod: 1, gap_lib: 2, unresolved: 3, stubs: 4, links: 5, obsolete: 6 };
      rows = rows.slice().sort(function (a, b) { return (order[a[2]] != null ? order[a[2]] : 7) - (order[b[2]] != null ? order[b[2]] : 7) || String(a[0]).localeCompare(String(b[0])); });
    } else {
      rows = rows.slice().sort(function (a, b) { return String(a[0]).localeCompare(String(b[0])); });
    }
    info.textContent = rows.length + " of " + run.mods.length + (state.kind !== "all" ? " (" + RESLABEL[state.kind] + ")" : "");
    if (!rows.length) { host.appendChild(el("p", "empty", "No mods match.")); return; }
    var table = el("table");
    var thead = el("thead"), head = el("tr");
    [["Mod", ""], ["Version", "col-ver"], ["Result", ""], ["Detail", ""]].forEach(function (h) { head.appendChild(el("th", h[1], h[0])); });
    thead.appendChild(head); table.appendChild(thead);
    var tb = el("tbody");
    rows.slice(0, state.pShown).forEach(function (m) {
      var tr = el("tr");
      tr.appendChild(el("td", "", m[0]));
      tr.appendChild(el("td", "col-ver", m[1]));
      var td = el("td"); td.appendChild(el("span", "res " + m[2], RESLABEL[m[2]] || m[2])); tr.appendChild(td);
      var why = el("td", "why", m[3] || "");
      // rendering smoke test (m[4] lost in translation, m[5] missing in the mod's own jar too, m[6] examples)
      if (m[4]) {
        var lost = el("div", "small");
        lost.style.color = "var(--unresolved)";
        lost.textContent = m[4] + " model/texture reference(s) missing after translation" + (m[5] ? " (+" + m[5] + " missing in the mod itself)" : "") + (m[6] && m[6][0] ? ": " + String(m[6][0]).replace("[layer] ", "") : "");
        why.appendChild(lost);
      } else if (m[5]) {
        var own = el("div", "small");
        own.textContent = m[5] + " model/texture reference(s) are missing in the mod's own jar too";
        why.appendChild(own);
      }
      tr.appendChild(why);
      tb.appendChild(tr);
    });
    table.appendChild(tb);
    var wrap = el("div", "tablewrap"); wrap.appendChild(table);
    host.appendChild(wrap);
    if (rows.length > state.pShown) {
      var more = el("div", "more");
      var b = el("button", "btn alt", "Show more (" + (rows.length - state.pShown) + " left of " + rows.length + ")");
      b.type = "button";
      b.onclick = function () { state.pShown += PAGE_PROBE; renderPList(); };
      more.appendChild(b); host.appendChild(more);
    }
  }

  function renderAutomated() {
    renderCompare();
    var has = state.runs.length > 0;
    $("pdetail").hidden = !has;
    if (!has) { $("ptabs").textContent = ""; return; }
    makeTabs($("ptabs"), probeVersions().map(function (mc) { return { id: mc, label: "MC " + mc, count: runsOf(mc).length + (runsOf(mc).length === 1 ? " run" : " runs") }; }), state.pmc, selectProbe, "pdetail");
    versionNote($("vnote-auto"), state.pmc);
    renderRunSelect(); renderChart(); renderTrend(); renderPList();
  }

  // ---------- missing resources ----------
  // data/resources.json: {runs: {<mc>: {version, date, checked, mods: [{name, version, layer, mod, layerItems, modItems}]}}}; an item is
  // [kind (texture|model), where, what, missing reference]. "layer" = lost in translation, "mod" = missing in the mod's own jar too.
  function resVersions() { return Object.keys(state.res).sort(byVersion); }
  function isVanillaRef(ref) { return ref.indexOf(":") < 0 || /^minecraft:/i.test(ref); }

  function renderResources() {
    var versions = resVersions();
    var summary = $("rsummary");
    summary.textContent = "";
    if (!versions.length) {
      $("rtabs").textContent = "";
      summary.appendChild(el("p", "empty", "No resource check results published yet."));
      $("rvanilla").textContent = ""; $("rlist").textContent = ""; $("rinfo").textContent = "";
      return;
    }
    if (!state.res[state.rmc]) state.rmc = versions[0];
    makeTabs($("rtabs"), versions.map(function (mc) { return { id: mc, label: "MC " + mc, count: state.res[mc].mods.length + " mods" }; }), state.rmc,
      function (mc) { state.rmc = mc; state.rShown = 40; setHash("resources", mc); renderResources(); }, "rsummary");
    var run = state.res[state.rmc];
    var lostMods = run.mods.filter(function (m) { return m.layer > 0; });
    var lost = lostMods.reduce(function (a, m) { return a + m.layer; }, 0);
    var own = run.mods.reduce(function (a, m) { return a + m.mod; }, 0);
    var head = el("div", "chart");
    var big = el("div");
    big.appendChild(el("div", "bignum", String(lostMods.length)));
    big.appendChild(el("div", "small", "of " + run.checked + " mods lose " + lost + " model/texture reference" + (lost === 1 ? "" : "s") + " in translation"));
    head.appendChild(big);
    var rest = el("div");
    rest.appendChild(el("p", null, (run.mods.length - lostMods.length) + " more mods have " + own + " reference" + (own === 1 ? "" : "s") + " that are missing in their own jars too (not the layer's doing)."));
    rest.appendChild(el("p", "small", "Checked with Connector " + (run.version || "?") + " on " + (run.date || "?") + ". Up to " + 300 + " lost and 40 own references are listed per mod."));
    head.appendChild(rest);
    summary.appendChild(head);
    renderVanillaGaps(run);
    renderResList(run);
  }

  function renderVanillaGaps(run) {
    var host = $("rvanilla");
    host.textContent = "";
    var byRef = {};
    run.mods.forEach(function (m) {
      (m.layerItems || []).forEach(function (it) {
        if (!isVanillaRef(it[3])) return;
        var key = it[0] + "|" + it[3].replace(/^minecraft:/i, "");
        var e = byRef[key] || (byRef[key] = { kind: it[0], ref: it[3].replace(/^minecraft:/i, ""), mods: {} });
        e.mods[m.name] = true;
      });
    });
    var list = Object.keys(byRef).map(function (k) { return byRef[k]; });
    list.forEach(function (e) { e.n = Object.keys(e.mods).length; });
    list.sort(function (a, b) { return b.n - a.n || a.ref.localeCompare(b.ref); });
    if (!list.length) { host.appendChild(el("p", "empty", "None found in this run.")); return; }
    var table = el("table"), thead = el("thead"), hr = el("tr");
    ["Vanilla file", "Kind", "Mods", "Needed by"].forEach(function (h, i) { hr.appendChild(el("th", i === 2 ? "num" : "", h)); });
    thead.appendChild(hr); table.appendChild(thead);
    var tb = el("tbody");
    list.slice(0, 60).forEach(function (e) {
      var tr = el("tr");
      var c = el("td"); c.appendChild(el("code", null, e.ref)); tr.appendChild(c);
      tr.appendChild(el("td", "", e.kind));
      tr.appendChild(el("td", "num", String(e.n)));
      var names = Object.keys(e.mods).sort();
      tr.appendChild(el("td", "why", names.slice(0, 4).join(", ") + (names.length > 4 ? " +" + (names.length - 4) + " more" : "")));
      tb.appendChild(tr);
    });
    table.appendChild(tb);
    host.appendChild(table);
    if (list.length > 60) host.appendChild(el("p", "small", "Showing the 60 most needed of " + list.length + "."));
  }

  function renderResList(run) {
    var host = $("rlist");
    host.textContent = "";
    var q = state.rq.toLowerCase();
    var onlyLost = state.rfilter === "layer";
    var rows = run.mods.filter(function (m) {
      if (onlyLost && !m.layer) return false;
      if (!q) return true;
      if (m.name.toLowerCase().indexOf(q) >= 0) return true;
      return (m.layerItems || []).concat(onlyLost ? [] : (m.modItems || [])).some(function (it) { return (it[1] + " " + it[2]).toLowerCase().indexOf(q) >= 0; });
    });
    $("rinfo").textContent = rows.length + " of " + run.mods.length + " mods";
    if (!rows.length) { host.appendChild(el("p", "empty", "No mods match.")); return; }
    rows.slice(0, state.rShown).forEach(function (m) {
      var d = el("details", "mod");
      var s = el("summary");
      s.appendChild(el("span", "name", m.name));
      s.appendChild(el("span", "meta", m.version || ""));
      if (m.layer) s.appendChild(el("span", "res layer", m.layer + " lost"));
      if (m.mod && !onlyLost) s.appendChild(el("span", "res own", m.mod + " own"));
      d.appendChild(s);
      var built = false;
      d.addEventListener("toggle", function () {
        if (!d.open || built) return;
        built = true;
        var body = el("div", "body");
        var items = (m.layerItems || []).map(function (it) { return it.concat(["layer"]); });
        if (!onlyLost) items = items.concat((m.modItems || []).map(function (it) { return it.concat(["own"]); }));
        var table = el("table"), thead = el("thead"), hr = el("tr");
        ["", "Kind", "Where", "What is missing"].forEach(function (h) { hr.appendChild(el("th", "", h)); });
        thead.appendChild(hr); table.appendChild(thead);
        var tb = el("tbody");
        items.forEach(function (it) {
          var tr = el("tr");
          var v = el("td"); v.appendChild(el("span", "res " + it[4], it[4] === "layer" ? "Lost" : "Mod's own")); tr.appendChild(v);
          tr.appendChild(el("td", "", it[0]));
          tr.appendChild(el("td", "why", it[1]));
          var w = el("td", "why"); w.appendChild(document.createTextNode(it[2].replace(/ ?[^ ]*$/, "") + " ")); w.appendChild(el("code", null, it[3])); tr.appendChild(w);
          tb.appendChild(tr);
        });
        table.appendChild(tb);
        body.appendChild(table);
        if (m.layer > (m.layerItems || []).length) body.appendChild(el("p", "small", "Showing " + (m.layerItems || []).length + " of " + m.layer + " lost references."));
        d.appendChild(body);
      });
      host.appendChild(d);
    });
    if (rows.length > state.rShown) {
      var more = el("div", "more");
      var b = el("button", "btn alt", "Show more (" + (rows.length - state.rShown) + " left of " + rows.length + ")");
      b.type = "button";
      b.onclick = function () { state.rShown += 40; renderResList(run); };
      more.appendChild(b); host.appendChild(more);
    }
  }

  // ---------- boot ----------
  // Scheduled jobs cannot push to the protected master branch: they write the data files to the branch "site-data", which is read
  // first; the copy next to this page (master) is the fallback.
  var DATA_BRANCH = "https://raw.githubusercontent.com/kylepitcock/Legacy-Connector/site-data/docs/";
  function load(path) {
    function local() {
      return fetch(path, { cache: "no-cache" }).then(function (r) { return r.ok ? r.json() : {}; }).catch(function () { return {}; });
    }
    return fetch(DATA_BRANCH + path, { cache: "no-cache" }).then(function (r) { return r.ok ? r.json() : local(); }).catch(local);
  }

  function init() {
    setTheme(document.documentElement.getAttribute("data-theme") || "light", false);
    $("theme").onclick = function () { setTheme(document.documentElement.getAttribute("data-theme") === "dark" ? "light" : "dark", true); };
    Array.prototype.forEach.call(document.querySelectorAll(".report-link"), function (a) { ext(a, reportUrl("")); });
    ext($("report-top"), reportUrl(""));
    $("legend-mods").appendChild(legendNode(false));
    $("legend-about").appendChild(legendNode(true));
    $("q").addEventListener("input", function (e) { state.q = e.target.value; state.modsShown = PAGE_MODS; renderList(); });
    $("sort").addEventListener("change", function (e) { state.sort = e.target.value; renderList(); });
    $("pq").addEventListener("input", function (e) { state.pq = e.target.value; state.pShown = PAGE_PROBE; renderPList(); });
    $("psort").addEventListener("change", function (e) { state.psort = e.target.value; renderPList(); });
    $("rq").addEventListener("input", function (e) { state.rq = e.target.value; state.rShown = 40; if (state.res[state.rmc]) renderResList(state.res[state.rmc]); });
    $("rfilter").addEventListener("change", function (e) { state.rfilter = e.target.value; state.rShown = 40; if (state.res[state.rmc]) renderResList(state.res[state.rmc]); });
    window.addEventListener("hashchange", applyRoute);
    $("list").textContent = "Loading…";
    applyRoute();

    var P = window.LC_PRELOAD;
    if (P) boot([P.mods || {}, P.reports || {}, P.probe || {}, P.resources || {}]);
    else Promise.all([load("data/mods.json"), load("data/reports.json"), load("data/probe.json"), load("data/resources.json")]).then(boot);
  }

  function boot(res) {
    state.all = merge(res[0].mods || [], res[1].reports || []);
    state.runs = res[2].runs || [];
    state.res = (res[3] && res[3].runs) || {};
    var rwant = parseHash();
    var rv = resVersions();
    state.rmc = rwant.route === "resources" && state.res[rwant.mc] ? rwant.mc : rv[0] || "";
    var want = parseHash();
    var seen = versionsWithData();
    state.tab = seen[want.mc] && want.route === "mods" ? want.mc : SUPPORTED[0];
    var mcs = probeVersions();
    state.pmc = want.route === "automated" && mcs.indexOf(want.mc) >= 0 ? want.mc : mcs[0] || "";
    state.run = state.pmc ? runsOf(state.pmc)[0] : null;
    var newest = state.runs.reduce(function (a, r) { return r.date > a ? r.date : a; }, "");
    $("updated").textContent = newest ? "Last automated probe run: " + newest : "";
    state.loaded = true;
    renderRoute();
  }

  if (document.readyState === "loading") document.addEventListener("DOMContentLoaded", init); else init();
})();
