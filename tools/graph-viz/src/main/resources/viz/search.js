// Search tab

  var hasIndex = typeof INDEX !== 'undefined';
  var UNIT = typeof UNITS !== 'undefined' ? UNITS : {};

  // text matching
  function norm(text) {
    return (text || '').toLowerCase().trim();
  }

  function terms(query) {
    return norm(query).split(/[\s,>-]+/).filter(Boolean);
  }

  function hasAll(haystack, list) {
    for (var i = 0; i < list.length; i++) {
      if (haystack.indexOf(list[i]) < 0) {
        return false;
      }
    }
    return true;
  }

  function quoteRe(text) {
    return String(text).replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
  }

  function markTerms(text, list) {
    var source = String(text);
    if (!list.length) {
      return escText(source);
    }
    var alternatives = [];
    list.forEach(function (term) {
      var flat = quoteRe(term);
      var up = quoteRe(raised(term));
      if (up !== flat) { alternatives.push(up); }
      alternatives.push(flat);
    });
    var safe = alternatives.join('|');

    var out = '';
    var at = 0;
    source.replace(new RegExp(safe, 'ig'), function (hit, index) {
      out += escText(source.slice(at, index)) + '<mark>' + escText(hit) + '</mark>';
      at = index + hit.length;
      return hit;
    });
    return out + escText(source.slice(at));
  }

  function cssValue(value) {
    return String(value).replace(/["\\]/g, '\\$&');
  }

  function systemName(system) {
    return !system || system === 'NULL' ? 'system-agnostic' : system;
  }

  var NOTHING = '<div class="emptystate"><b>Nothing matches</b>'
              + 'Try a shorter query, or clear the filters.</div>';

  // search box
  var allFinds = [];
  var ofindApi = null;

  function wireFind(label, onInput) {
    var input = label.querySelector('input');
    var clear = label.querySelector('.clearfind');

    function sync() {
      label.classList.toggle('filled', input.value.length > 0);
      onInput(input.value);
    }
    input.addEventListener('input', sync);
    clear.addEventListener('click', function () {
      input.value = '';
      sync();
      input.focus();
    });
    input.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && input.value) {
        event.stopPropagation();
        input.value = '';
        sync();
      }
    });

    function reset() {
      if (!input.value) {
        return;
      }
      input.value = '';
      sync();
    }

    var api = {input: input, sync: sync, reset: reset};
    allFinds.push(api);
    return api;
  }

  // filter menu
  var allMenus = [];

  function wireMenu(wrap, onChange) {
    var button = wrap.querySelector('.filterbtn');
    var menu = wrap.querySelector('.filtermenu');
    var badge = wrap.querySelector('.fnum');
    var wipe = wrap.querySelector('.fwipe');

    function close() {
      menu.hidden = true;
      button.setAttribute('aria-expanded', 'false');
    }

    button.addEventListener('click', function (event) {
      event.stopPropagation();
      menu.hidden = !menu.hidden;
      button.setAttribute('aria-expanded', menu.hidden ? 'false' : 'true');
    });
    menu.addEventListener('click', function (event) {
      event.stopPropagation();
    });
    document.addEventListener('click', close);
    document.addEventListener('keydown', function (event) {
      if (event.key === 'Escape' && !menu.hidden) {
        close();
        button.focus();
      }
    });

    menu.addEventListener('change', onChange);

    function clearAll() {
      var had = false;
      menu.querySelectorAll('input[type=checkbox]').forEach(function (box) {
        if (box.checked) { box.checked = false; had = true; }
      });
      menu.querySelectorAll('select').forEach(function (select) {
        if (select.selectedIndex !== 0) { select.selectedIndex = 0; had = true; }
      });
      if (had) { onChange(); }
    }

    menu.querySelector('.fclear').addEventListener('click', clearAll);

    wipe.addEventListener('click', function (event) {
      event.stopPropagation();
      clearAll();
      button.focus();
    });

    var api = {
      menu: menu,
      close: close,
      clear: clearAll,
      count: function (active) {
        badge.textContent = active > 0 ? active : '';
        wrap.classList.toggle('on', active > 0);
        wipe.disabled = active === 0;
      }
    };
    allMenus.push(api);
    return api;
  }

  function checkedIn(root) {
    var names = [];
    root.querySelectorAll('input[type=checkbox]').forEach(function (box) {
      if (box.checked) {
        names.push(box.dataset.test);
      }
    });
    return names;
  }

  function asSet(names) {
    var set = {};
    names.forEach(function (name) {
      set[name] = true;
    });
    return set;
  }

  // Card grids
  var EXCLUSIVE = {untested: ['complete'], complete: ['untested'],
                   tree: ['cyclic', 'dup'], cyclic: ['tree', 'dup'], dup: ['tree', 'cyclic']};

  function wireGrid(bar) {
    var pane = bar.closest('.tabpane');
    var cards = Array.prototype.slice.call(pane.querySelectorAll('.card'));
    var count = bar.querySelector('.count');
    var query = '';
    var active = {};

    var empty = document.createElement('div');
    empty.className = 'emptystate';
    empty.innerHTML = '<b>Nothing matches</b>Try a shorter query, or clear the filters.';
    empty.hidden = true;
    pane.appendChild(empty);

    function keeps(card) {
      var list = terms(query);
      if (list.length && !hasAll(card.dataset.find || '', list)) {
        return false;
      }
      if (active.failed && +card.dataset.failed === 0) { return false; }
      if (active.untested && +card.dataset.untested === 0) { return false; }
      if (active.complete && +card.dataset.untested !== 0) { return false; }
      if ((active.tree || active.cyclic || active.dup)
          && !active[card.dataset.shape]) { return false; }
      return true;
    }

    function apply() {
      var shown = 0;
      cards.forEach(function (card) {
        var keep = keeps(card);
        if (keep) {
          shown++;
        }
        card.classList.toggle('hidden', !keep);
      });
      count.textContent = shown === cards.length
        ? cards.length + ' dimensions'
        : shown + ' of ' + cards.length;
      empty.hidden = shown > 0;
    }

    var menu = wireMenu(bar.querySelector('.filter'), function (event) {
      var box = event && event.target;
      if (box && box.checked && EXCLUSIVE[box.dataset.test]) {
        EXCLUSIVE[box.dataset.test].forEach(function (other) {
          var sibling = menu.menu.querySelector('[data-test="' + other + '"]');
          if (sibling) {
            sibling.checked = false;
          }
        });
      }
      var names = checkedIn(menu.menu);
      active = asSet(names);
      menu.count(names.length);
      apply();
    });

    wireFind(bar.querySelector('.find'), function (value) {
      query = value;
      apply();
    });

    apply();
  }

  document.querySelectorAll('.toolbar').forEach(wireGrid);

  // Search tab
  var wlist = document.getElementById('wlist');
  var winfo = document.getElementById('winfo');
  var wcount = document.getElementById('wcount');
  var wpage = document.getElementById('wpage');
  var wpageat = document.getElementById('wpageat');
  var wmenu = null;
  var MAX_RESULTS = 400;
  var page = 0;

  var CONV_HINT = '<div class="empty"><b>Pick a conversion</b>'
                + 'Everything the build knows about it appears here: what it computes, the '
                + 'route it takes, every test that touched it, and how it was written.</div>';

  var UNIT_HINT = '<div class="empty"><b>Pick a unit</b>'
                + 'Its dimension, system, aliases, direct conversions and test coverage all '
                + 'appear here.</div>';

  var mode = 'conv';
  var qFrom = '';
  var qTo = '';
  var qUnit = '';
  var picked = {};
  var systems = {};
  var dimension = '';
  var hopMode = 'any';
  var hopCount = 1;

  var UNIT_ONLY = {hasfail: true, hasuntested: true, isolated: true};

  var noRoute = null;

  function missingRows() {
    if (noRoute) { return noRoute; }
    var reached = {};
    INDEX.forEach(function (row) { reached[row.f + '\t' + row.t] = true; });

    var byDimension = {};
    Object.keys(UNIT).forEach(function (id) {
      var dim = UNIT[id].d;
      (byDimension[dim] = byDimension[dim] || []).push(id);
    });

    noRoute = [];
    Object.keys(byDimension).forEach(function (dim) {
      var ids = byDimension[dim];
      ids.forEach(function (from) {
        ids.forEach(function (to) {
          if (from !== to && !reached[from + '\t' + to]) {
            noRoute.push({f: from, t: to, fn: UNIT[from].n, tn: UNIT[to].n,
                          d: dim, s: 'missing', h: null, k: null});
          }
        });
      });
    });
    return noRoute;
  }

  function unitSide(name) {
    return name.indexOf('sys:') === 0 || !!UNIT_ONLY[name];
  }

  function readFilters() {
    var names = checkedIn(wmenu.menu);
    var hopn = document.getElementById('whopn');

    picked = asSet(names);
    systems = {};
    names.forEach(function (name) {
      if (name.indexOf('sys:') === 0) {
        systems[name.slice(4)] = true;
      }
    });

    dimension = document.getElementById('wdim').value;
    hopMode = document.getElementById('whopmode').value;
    hopCount = parseInt(hopn.value, 10) || 1;
    hopn.disabled = hopMode === 'any';

    var active = names.filter(function (name) {
      return unitSide(name) === (mode === 'unit');
    }).length;
    if (dimension) {
      active++;
    }
    if (mode === 'conv' && hopMode !== 'any') {
      active++;
    }
    wmenu.count(active);
  }

  function hopKeeps(hops) {
    if (hopMode === 'any') {
      return true;
    }
    if (hops === null || hops === undefined) {
      return false;
    }
    if (hopMode === 'eq') {
      return hops === hopCount;
    }
    return hopMode === 'min' ? hops >= hopCount : hops <= hopCount;
  }

  // decide what matches the search query
  function convKeeps(row, fromList, toList) {
    if (fromList.length && !hasAll(norm(row.f + ' ' + row.fn), fromList)) {
      return false;
    }
    if (toList.length && !hasAll(norm(row.t + ' ' + row.tn), toList)) {
      return false;
    }
    if (dimension && row.d !== dimension) {
      return false;
    }
    if ((picked.passed || picked.failed || picked.untested || picked.missing)
        && !picked[row.s]) {
      return false;
    }
    if (picked.direct && !picked.derived && !row.k) { return false; }
    if (picked.derived && !picked.direct && row.k) { return false; }
    if (picked.linear !== picked['function']) {
      if (picked.linear && row.k !== 'linear') { return false; }
      if (picked['function'] && row.k !== 'function') { return false; }
    }
    return hopKeeps(row.h);
  }

  function unitKeeps(id, list) {
    var unit = UNIT[id];
    if (list.length) {
      var hay = norm([id, unit.n, unit.d, systemName(unit.y), unit.x]
                     .concat(unit.a || []).join(' '));
      if (!hasAll(hay, list)) {
        return false;
      }
    }
    if (dimension && unit.d !== dimension) {
      return false;
    }
    if (Object.keys(systems).length && !systems[unit.y]) {
      return false;
    }
    if (picked.hasfail && unit.c[1] === 0) { return false; }
    if (picked.hasuntested && unit.c[2] === 0) { return false; }
    if (picked.isolated && unit.nb.length > 0) { return false; }
    return true;
  }

  function unitTone(unit) {
    if (unit.c[1] > 0) { return 'failed'; }
    if (unit.c[2] > 0) { return 'untested'; }
    return unit.c[0] > 0 ? 'passed' : 'missing';
  }

  function rank(value, query) {
    var wanted = norm(query);
    if (!wanted) {
      return 0;
    }
    var text = norm(value);
    if (text === wanted) {
      return 2;
    }
    return text.indexOf(wanted) === 0 ? 1 : 0;
  }

  function byRank(rankOf) {
    return function (a, b) {
      return rankOf(b) - rankOf(a);
    };
  }

  function tally(shown, total, noun) {
    if (wcount) {
      wcount.textContent = shown + ' of ' + total + ' ' + noun;
    }
  }

  function pageSlice(list) {
    var pages = Math.max(1, Math.ceil(list.length / MAX_RESULTS));
    page = Math.max(0, Math.min(page, pages - 1));
    return list.slice(page * MAX_RESULTS, (page + 1) * MAX_RESULTS);
  }

  function showPager(total) {
    if (!wpage) {
      return;
    }
    var pages = Math.max(1, Math.ceil(total / MAX_RESULTS));
    wpage.hidden = pages < 2;
    if (pages < 2) {
      return;
    }
    wpageat.textContent = (page * MAX_RESULTS + 1) + '–'
      + Math.min(total, (page + 1) * MAX_RESULTS) + ' of ' + total;
    wpage.querySelector('[data-step="-1"]').disabled = page === 0;
    wpage.querySelector('[data-step="1"]').disabled = page >= pages - 1;
  }

  function selectResult(el, show) {
    wlist.querySelectorAll('.res').forEach(function (other) {
      other.classList.remove('on');
    });
    el.classList.add('on');
    show();
  }

  // actually showing the results
  function drawConversions() {
    var rows = picked.missing ? INDEX.concat(missingRows()) : INDEX;
    var fromList = terms(qFrom);
    var toList = terms(qTo);
    var hits = [];
    rows.forEach(function (row, i) {
      if (convKeeps(row, fromList, toList)) {
        hits.push(i);
      }
    });
    hits.sort(byRank(function (i) {
      return rank(rows[i].f, qFrom) + rank(rows[i].t, qTo);
    }));

    wlist.innerHTML = pageSlice(hits).map(function (i, n) {
      var row = rows[i];
      return '<div class="res" data-i="' + i + '" style="--i:' + n + '">'
           + '<span class="dot ' + row.s + '"></span>'
           + '<span class="pairs">' + markTerms(raised(row.f), fromList) + ARROW
           + markTerms(raised(row.t), toList) + '</span>'
           + '<span class="dim">' + escText(row.d) + '</span></div>';
    }).join('') || NOTHING;

    tally(hits.length, rows.length, 'conversions');
    showPager(hits.length);
    wlist.querySelectorAll('.res').forEach(function (el) {
      el.addEventListener('click', function () {
        selectResult(el, function () { showConversion(rows[+el.dataset.i]); });
      });
    });
  }

  function drawUnits() {
    var list = terms(qUnit);
    var ids = Object.keys(UNIT).sort().filter(function (id) {
      return unitKeeps(id, list);
    });
    ids.sort(byRank(function (id) {
      return rank(id, qUnit);
    }));

    wlist.innerHTML = pageSlice(ids).map(function (id, n) {
      var unit = UNIT[id];
      return '<div class="res" data-u="' + escText(id) + '" style="--i:' + n + '">'
           + '<span class="dot ' + unitTone(unit) + '"></span>'
           + '<span class="pairs">' + markTerms(raised(id), list)
           + '<span class="sub">' + markTerms(unit.n, list) + '</span></span>'
           + '<span class="dim">' + escText(unit.d) + '</span></div>';
    }).join('') || NOTHING;

    tally(ids.length, Object.keys(UNIT).length, 'units');
    showPager(ids.length);
    wlist.querySelectorAll('.res').forEach(function (el) {
      el.addEventListener('click', function () {
        selectResult(el, function () { showUnit(el.dataset.u); });
      });
    });
  }

  function redraw() {
    if (!wlist || !hasIndex) {
      return;
    }
    readFilters();
    if (mode === 'unit') {
      drawUnits();
    } else {
      drawConversions();
    }
  }

  // Any change to the question starts over at the first page
  function draw() {
    page = 0;
    redraw();
  }

  // right panel for when you click on a result
  function renderedDetail(from, to) {
    var out = '';
    out += detailHtml(from, to);
    var edge = document.querySelector('#tab-seed path[data-a="' + cssValue(from)
             + '"][data-b="' + cssValue(to) + '"]')
            || document.querySelector('#tab-seed path[data-a="' + cssValue(to)
             + '"][data-b="' + cssValue(from) + '"]');
    if (edge && edge.dataset.detail) {
      out += '<div class="info-sec"><div class="lbl">as written in conversions.json</div>'
           + edge.dataset.detail + '</div>';
    }
    return out;
  }

  function factsList(pairs) {
    return '<dl class="info-facts">' + pairs.map(function (pair) {
      return '<dt>' + escText(pair[0]) + '</dt><dd>' + escText(pair[1]) + '</dd>';
    }).join('') + '</dl>';
  }

  function graphCardFor(dimension) {
    var host = document.querySelector('#tab-seed .cy[data-group="'
                                      + cssValue(dimension) + '"]');
    return host ? host.closest('.seedcard') : null;
  }

  function openGraph(dimension, from, to) {
    var card = graphCardFor(dimension);
    if (!card) { return; }
    navLeave();
    navMoving = true;
    var tab = document.querySelector('.tab[data-pane="tab-seed"]');
    if (tab) { tab.click(); }
    navMoving = false;
    open(card, {from: from, to: to});
  }

  function showConversion(row) {
    var from = UNIT[row.f] || {};
    var to = UNIT[row.t] || {};

    function unitLine(id, unit) {
      return raised(id) + (unit.n ? ' - ' + unit.n : '')
           + (unit.y ? ' (' + systemName(unit.y) + ')' : '');
    }

    var jumps = '<div class="info-sec jumps">'
      + (graphCardFor(row.d)
         ? '<button type="button" class="gograph">Graph' + ARROW
           + escText(row.d) + '</button>'
         : '')
      + '<button type="button" class="goconv">Converter' + ARROW
      + sup(escText(row.f)) + '</button></div>';

    winfo.innerHTML = (renderedDetail(row.f, row.t)
        || '<div class="empty">No rendered formula for this pair.</div>')
      + '<div class="info-sec"><div class="lbl">facts</div>'
      + factsList([
          ['dimension', row.d],
          ['from', unitLine(row.f, from)],
          ['to', unitLine(row.t, to)],
          ['status', row.s],
          ['route', row.h ? row.h + (row.h === 1 ? ' hop' : ' hops') : 'not reachable'],
          ['kind', row.k ? 'written by hand (' + row.k + ':)' : 'derived by chaining']
        ])
      + '</div>' + jumps;
    winfo.scrollTop = 0;

    var toGraph = winfo.querySelector('.gograph');
    if (toGraph) {
      toGraph.addEventListener('click', function () {
        openGraph(row.d, row.f, row.t);
      });
    }
    winfo.querySelector('.goconv').addEventListener('click', function () {
      openConverter(row.f, row.t);
    });
  }

  function showUnit(id) {
    var unit = UNIT[id];
    if (!unit) {
      return;
    }
    var aliases = unit.a || [];
    var neighbors = unit.nb || [];
    var total = unit.c[0] + unit.c[1] + unit.c[2];

    var conversions = neighbors.length
      ? '<div class="nbs">' + neighbors.map(function (other) {
          return '<button type="button" class="nb" data-to="' + escText(other) + '">'
               + sup(escText(id)) + ARROW + sup(escText(other)) + '</button>';
        }).join('') + '</div>'
        + '<div class="info-note">written by hand, one step. Click one to open it.</div>'
      : '<div class="info-note">none - nothing converts directly to or from this unit.</div>';

    winfo.innerHTML = '<div class="fx">'
      + '<div class="fx-head">' + sup(escText(id))
      + '<span class="chip kind">' + escText(systemName(unit.y)) + '</span></div>'
      + '<div class="fx-names">' + escText(unit.n) + ' · ' + escText(unit.d) + '</div>'
      + (unit.x ? '<div class="info-text">' + escText(unit.x) + '</div>' : '')
      + '<div class="info-sec"><div class="lbl">facts</div>'
      + factsList([
          ['abbreviation', raised(id)],
          ['name', unit.n],
          ['dimension', unit.d],
          ['system', systemName(unit.y)],
          ['also known as', aliases.length ? aliases.join(', ') : 'nothing else'],
          ['direct conversions', neighbors.length],
          ['conversions in all', total]
        ])
      + '</div>'
      + '<div class="info-sec"><div class="lbl">test coverage</div>'
      + '<div class="info-tally"><span class="chip passed">' + unit.c[0] + ' passed</span>'
      + '<span class="chip failed">' + unit.c[1] + ' failed</span>'
      + '<span class="chip untested">' + unit.c[2] + ' untested</span></div>'
      + '<div class="info-note">both directions of every conversion this unit '
      + 'takes part in.</div></div>'
      + '<div class="info-sec"><div class="lbl">direct conversions</div>'
      + conversions + '</div></div>';
    winfo.scrollTop = 0;

    var toConverter = document.createElement('button');
    toConverter.type = 'button';
    toConverter.className = 'goconv';
    toConverter.innerHTML = 'Converter' + ARROW + sup(escText(id));
    toConverter.addEventListener('click', function () { openConverter(id, null); });
    winfo.insertAdjacentHTML('beforeend', '<div class="info-sec jumps"></div>');
    winfo.querySelector('.jumps').appendChild(toConverter);

    winfo.querySelectorAll('.nb').forEach(function (button) {
      button.addEventListener('click', function () {
        openFind({from: id, to: button.dataset.to});
      });
    });
  }

  function setMode(next) {
    var changed = mode !== next;
    mode = next;
    document.querySelectorAll('#tab-find .mode').forEach(function (button) {
      button.classList.toggle('active', button.dataset.mode === next);
    });
    document.querySelectorAll('#tab-find .findbar').forEach(function (bar) {
      bar.hidden = bar.dataset.mode !== next;
    });
    wmenu.menu.querySelectorAll('.fpart').forEach(function (part) {
      part.hidden = part.dataset.mode !== next;
    });
    if (changed) {
      winfo.innerHTML = next === 'unit' ? UNIT_HINT : CONV_HINT;
    }
  }

  function setFind(id, value) {
    var input = document.getElementById(id);
    input.value = value;
    input.closest('.find').classList.toggle('filled', value.length > 0);
  }

  function openFind(opts) {
    var tab = document.querySelector('.tab[data-pane="tab-find"]');
    if (!tab || !wlist || !hasIndex) {
      return;
    }
    if (!opts.keepPlace) { navLeave(); }
    navMoving = true;
    tab.click();
    navMoving = false;
    setMode(opts.mode || 'conv');
    wmenu.menu.querySelectorAll('input[type=checkbox]').forEach(function (box) {
      box.checked = false;
    });
    qFrom = opts.from || '';
    qTo = opts.to || '';
    qUnit = opts.unit || '';
    setFind('wfrom', qFrom);
    setFind('wto', qTo);
    setFind('wunit', qUnit);
    [opts.status, opts.kind, opts.system && 'sys:' + opts.system].forEach(function (test) {
      if (!test) { return; }
      var box = wmenu.menu.querySelector('[data-test="' + cssValue(test) + '"]');
      if (box) { box.checked = true; }
    });
    document.getElementById('wdim').value = opts.dim || '';
    document.getElementById('whopmode').value = opts.hops ? 'eq' : 'any';
    document.getElementById('whopn').value = opts.hops || 1;
    draw();
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  // Return buttons to go back to where you were before you jumped somewhere else

  var navStack = [];
  var NAV_DEPTH = 20;
  var navMoving = false;
  var navButton = null;

  function navFindState() {
    var chosen = wlist.querySelector('.res.on');
    return {
      mode: mode, from: qFrom, to: qTo, unit: qUnit,
      tests: checkedIn(wmenu.menu),
      dim: document.getElementById('wdim').value,
      hopMode: document.getElementById('whopmode').value,
      hopn: document.getElementById('whopn').value,
      at: page,
      pickUnit: chosen ? chosen.dataset.u : null,
      pickRow: chosen ? chosen.dataset.i : null,
      label: chosen ? chosen.querySelector('.pairs').textContent.trim() : ''
    };
  }

  function navConvertState() {
    var side = document.getElementById('cvwork');
    var unit = document.getElementById('cvunit');
    return {
      unit: unit ? unit.value : '',
      value: document.getElementById('cvvalue').value,
      showing: side ? side.dataset.showing || null : null
    };
  }

  function navHere() {
    var tab = document.querySelector('.tab.active');
    if (!tab) {
      return null;
    }
    var place = {pane: tab.dataset.pane, tabName: tab.textContent.trim(),
                 card: null, summary: false};
    if (place.pane === 'tab-find' && wlist && wmenu && hasIndex) {
      place.find = navFindState();
    } else if (place.pane === 'tab-convert' && document.getElementById('cvvalue')) {
      place.convert = navConvertState();
    }
    if (typeof overlay !== 'undefined' && overlay
        && overlay.classList.contains('open') && lastCard) {
      place.card = lastCard;
    }
    if (summary && summary.classList.contains('open')) {
      place.summary = true;
    }
    return place;
  }

  function navLabel(place) {
    if (place.find) {
      if (place.find.label) {
        return place.find.label;
      }
      if (place.find.mode === 'unit') {
        return place.find.unit || 'units';
      }
      var a = place.find.from;
      var b = place.find.to;
      return a || b ? raised(a || '?') + ' → ' + raised(b || '?') : 'conversions';
    }
    if (place.convert) {
      return place.convert.showing
        ? raised(place.convert.unit) + ' → ' + raised(place.convert.showing)
        : raised(place.convert.unit) || 'the converter';
    }
    if (place.summary) {
      return 'the test summary';
    }
    if (place.card) {
      return place.card.querySelector('h2').textContent.trim();
    }
    return place.tabName;
  }

  function navLead(place) {
    if (place.summary) { return 'Back to'; }
    if (place.card) { return 'Back to the graph'; }
    if (place.convert) { return 'Back to the converter'; }
    if (place.find) { return 'Back to the search'; }
    return 'Back to';
  }

  function navLeave() {
    var place = navHere();
    if (!place) {
      return;
    }
    navStack.push(place);
    if (navStack.length > NAV_DEPTH) {
      navStack.shift();
    }
    navPaint();
  }

  function navClear() {
    navStack.length = 0;
    navPaint();
  }

  function navPaint() {
    if (!navStack.length) {
      if (navButton) { navButton.hidden = true; }
      return;
    }
    if (!navButton) {
      navButton = document.createElement('button');
      navButton.type = 'button';
      navButton.id = 'goback';
      navButton.className = 'goback';
      navButton.addEventListener('click', navBack);
      document.body.appendChild(navButton);
    }
    var place = navStack[navStack.length - 1];
    navButton.innerHTML = '<span class="gb-arrow" aria-hidden="true">&larr;</span>'
      + '<span class="gb-lead">' + escText(navLead(place)) + '</span>'
      + '<span class="gb-what">' + escText(navLabel(place)) + '</span>'
      + (navStack.length > 1
         ? '<span class="gb-depth">' + navStack.length + '</span>' : '');
    navButton.hidden = false;
  }

  function navRestoreFind(state) {
    setMode(state.mode);
    qFrom = state.from;
    qTo = state.to;
    qUnit = state.unit;
    setFind('wfrom', qFrom);
    setFind('wto', qTo);
    setFind('wunit', qUnit);
    wmenu.menu.querySelectorAll('input[type=checkbox]').forEach(function (box) {
      box.checked = state.tests.indexOf(box.dataset.test) >= 0;
    });
    document.getElementById('wdim').value = state.dim;
    document.getElementById('whopmode').value = state.hopMode;
    document.getElementById('whopn').value = state.hopn;
    page = state.at;
    redraw();

    var chosen = state.pickUnit
      ? wlist.querySelector('.res[data-u="' + cssValue(state.pickUnit) + '"]')
      : state.pickRow
        ? wlist.querySelector('.res[data-i="' + cssValue(state.pickRow) + '"]')
        : null;
    if (chosen) {
      chosen.click();
      chosen.scrollIntoView({block: 'center'});
    }
  }

  function navBack() {
    var place = navStack.pop();
    if (!place) {
      return;
    }
    navPaint();

    if (typeof overlay !== 'undefined' && overlay
        && overlay.classList.contains('open')) {
      close();
    }
    if (summary && summary.classList.contains('open')) {
      showSummary(false);
    }

    navMoving = true;
    var tab = document.querySelector('.tab[data-pane="' + cssValue(place.pane) + '"]');
    if (tab) { tab.click(); }
    navMoving = false;

    if (place.find) {
      navRestoreFind(place.find);
    } else if (place.convert && typeof openConverter === 'function') {
      openConverter(place.convert.unit, null, true);
      document.getElementById('cvvalue').value = place.convert.value;
      exRunConverter();
      if (place.convert.showing) { exShowWork(place.convert.showing); }
    }

    if (place.card) {
      open(place.card);
    } else if (place.summary) {
      showSummary(true);
    } else {
      window.scrollTo({top: 0, behavior: 'smooth'});
    }
  }

  function resetSearches() {
    allFinds.forEach(function (find) { find.reset(); });
    allMenus.forEach(function (menu) { menu.clear(); });
    if (typeof exResetConverter === 'function') {
      exResetConverter();
    }
    if (winfo && hasIndex) {
      winfo.innerHTML = mode === 'unit' ? UNIT_HINT : CONV_HINT;
    }
  }

  var lastPane = null;

  document.querySelectorAll('.tab').forEach(function (tab) {
    tab.addEventListener('click', function () {
      if (!navMoving) { navClear(); }
      if (lastPane === tab.dataset.pane) {
        return;
      }
      lastPane = tab.dataset.pane;
      resetSearches();
    });
  });

  function fillCategories() {
    var dims = [];
    var kinds = [];
    Object.keys(UNIT).forEach(function (id) {
      var unit = UNIT[id];
      if (unit.d && dims.indexOf(unit.d) < 0) { dims.push(unit.d); }
      if (kinds.indexOf(unit.y) < 0) { kinds.push(unit.y); }
    });

    var select = document.getElementById('wdim');
    dims.sort().forEach(function (name) {
      var option = document.createElement('option');
      option.value = name;
      option.textContent = name;
      select.appendChild(option);
    });

    document.getElementById('wsys').innerHTML = kinds.sort().map(function (system) {
      return '<label class="fopt"><input type="checkbox" data-test="sys:' + escText(system)
           + '"><span>' + escText(systemName(system)) + '</span></label>';
    }).join('');
  }

  if (wlist && winfo && hasIndex) {
    fillCategories();
    wmenu = wireMenu(document.getElementById('wfilter'), draw);
    winfo.innerHTML = CONV_HINT;

    wireFind(document.getElementById('wfrom').closest('.find'), function (value) {
      qFrom = value;
      draw();
    });
    wireFind(document.getElementById('wto').closest('.find'), function (value) {
      qTo = value;
      draw();
    });
    wireFind(document.getElementById('wunit').closest('.find'), function (value) {
      qUnit = value;
      draw();
    });
    document.getElementById('whopn').addEventListener('input', draw);

    document.querySelectorAll('#tab-find .mode').forEach(function (button) {
      button.addEventListener('click', function () {
        if (mode !== button.dataset.mode) {
          setMode(button.dataset.mode);
          draw();
        }
      });
    });

    if (wpage) {
      wpage.addEventListener('click', function (event) {
        var button = event.target.closest('.pgo');
        if (!button || button.disabled) {
          return;
        }
        page += +button.dataset.step;
        redraw();
        wlist.scrollTop = 0;
      });
    }

    draw();
  }

  document.querySelectorAll('.legend [data-status], .legend [data-system],'
                          + ' .legend [data-kind]').forEach(function (key) {
    key.classList.add('clickable');
    key.addEventListener('click', function () {

      var inOverlay = !!key.closest('#overlay');
      var dim = inOverlay && otitle ? otitle.textContent.trim() : '';

      // Written down before the close, or there is nothing left to describe.
      navLeave();
      if (overlay && overlay.classList.contains('open')) { close(); }
      if (key.dataset.system) {
        openFind({mode: 'unit', system: key.dataset.system, dim: dim, keepPlace: true});
      } else if (key.dataset.kind) {
        openFind({kind: key.dataset.kind, dim: dim, keepPlace: true});
      } else {
        openFind({status: key.dataset.status, dim: dim, keepPlace: true});
      }
    });
  });

  document.querySelectorAll('#sbody .sum-table tbody tr').forEach(function (row) {
    var name = row.querySelector('.name');
    var hop = row.querySelector('.hopn');
    var state = row.dataset.state;
    if (!name && !hop && !state) {
      return;
    }
    if (name || hop) {
      row.classList.add('clickable');
    }
    row.addEventListener('click', function () {
      navLeave();
      showSummary(false);
      if (name) {
        openFind({dim: name.textContent, keepPlace: true});
      } else if (hop) {
        openFind({hops: parseInt(hop.textContent, 10), keepPlace: true});
      } else {
        openFind({status: state, keepPlace: true});
      }
    });
  });

  var ofind = document.getElementById('ofind');
  if (ofind) {
    ofindApi = wireFind(ofind, function (value) {
      var list = terms(value);
      stage.querySelectorAll('td[data-from]').forEach(function (cell) {
        var hit = list.length
               && hasAll(norm(cell.dataset.from + ' ' + cell.dataset.to), list);
        cell.classList.toggle('found', !!hit);
        cell.classList.toggle('faded', list.length > 0 && !hit);
      });
      stage.querySelectorAll('th').forEach(function (th) {
        var label = norm(th.textContent);
        th.classList.toggle('found', list.length > 0 && !!label && hasAll(label, list));
      });
    });
  }
