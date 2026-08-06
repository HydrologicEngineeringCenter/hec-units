/*
 * Search: the third tab, plus the filter bars on the two card grids and the
 * highlight box inside an enlarged matrix.
 *
 * Runs off two datasets emitted by SearchIndex.java - INDEX, every conversion,
 * and UNITS, every unit. Rendered detail is never duplicated here; it is read
 * back off the matrix cells and graph edges that already carry it.
 */
  /* ================================================================= search
     One index drives three surfaces: the card grids, the search tab, and the
     in-matrix highlight. Rendered detail is never duplicated here - it already
     sits on the matrix cells and graph edges, and is read back from there. */

  var hasIndex = typeof INDEX !== 'undefined';
  var UNIT = typeof UNITS !== 'undefined' ? UNITS : {};

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

  function markTerms(text, list) {
    var out = escText(text);
    list.forEach(function (term) {
      var safe = term.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
      out = out.replace(new RegExp('(' + safe + ')', 'ig'), '<mark>$1</mark>');
    });
    return out;
  }

  function cssValue(value) {
    return String(value).replace(/["\\]/g, '\\$&');
  }

  function systemName(system) {
    return !system || system === 'NULL' ? 'system-agnostic' : system;
  }

  var NOTHING = '<div class="emptystate"><b>Nothing matches</b>'
              + 'Try a shorter query, or clear the filters.</div>';

  /* Clear button, Escape-to-clear, and the filled state every box shares. */
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
    return {input: input, sync: sync};
  }

  /* ----------------------------------------------------------- filter menus
     One popup everywhere: a button that says how many rules are on, a panel of
     controls, and a clear. The owner decides what the controls mean; this only
     handles opening, closing and counting. */

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
      menu.querySelectorAll('input[type=checkbox]').forEach(function (box) {
        box.checked = false;
      });
      menu.querySelectorAll('select').forEach(function (select) {
        select.selectedIndex = 0;
      });
      onChange();
    }

    menu.querySelector('.fclear').addEventListener('click', clearAll);

    /* The same clear, without having to open the menu to reach it. */
    wipe.addEventListener('click', function (event) {
      event.stopPropagation();
      clearAll();
      button.focus();
    });

    return {
      menu: menu,
      close: close,
      count: function (active) {
        badge.textContent = active > 0 ? active : '';
        wrap.classList.toggle('on', active > 0);
        wipe.disabled = active === 0;
      }
    };
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

  /* ------------------------------------------------------------ card grids */

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

  /* -------------------------------------------------------------- search tab
     Two questions, two shapes. A conversion is a pair, so it gets a box per
     end, either of which may be left blank. A unit is one thing, so it gets one
     box and the categories it is filed under. */

  var wlist = document.getElementById('wlist');
  var winfo = document.getElementById('winfo');
  var wcount = document.getElementById('wcount');
  var wmenu = null;
  var MAX_RESULTS = 400;

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

  /* The matrix has a cell for every ordered pair inside a dimension; the index
     only carries the ones a conversion reaches. The rest are built here, once,
     and only ever shown when someone asks for them by name. */
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

  /* Reads every control into the module state, and reports how many of them are
     doing something in the mode currently showing. */
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

  /* Worst news first, so a red dot always means something to look at. */
  function unitTone(unit) {
    if (unit.c[1] > 0) { return 'failed'; }
    if (unit.c[2] > 0) { return 'untested'; }
    return unit.c[0] > 0 ? 'passed' : 'missing';
  }

  /* Matching is by substring, so "ft" also finds ac-ft and lbm/ft3. Ranking an
     exact abbreviation above a prefix above a substring keeps the conversion
     that was actually asked for at the top. */
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
      wcount.textContent = shown + ' of ' + total + ' ' + noun
        + (shown > MAX_RESULTS ? ' - showing ' + MAX_RESULTS : '');
    }
  }

  function selectResult(el, show) {
    wlist.querySelectorAll('.res').forEach(function (other) {
      other.classList.remove('on');
    });
    el.classList.add('on');
    show();
  }

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

    wlist.innerHTML = hits.slice(0, MAX_RESULTS).map(function (i, n) {
      var row = rows[i];
      return '<div class="res" data-i="' + i + '" style="--i:' + n + '">'
           + '<span class="dot ' + row.s + '"></span>'
           + '<span class="pairs">' + markTerms(raised(row.f), fromList) + ARROW
           + markTerms(raised(row.t), toList) + '</span>'
           + '<span class="dim">' + escText(row.d) + '</span></div>';
    }).join('') || NOTHING;

    tally(hits.length, rows.length, 'conversions');
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

    wlist.innerHTML = ids.slice(0, MAX_RESULTS).map(function (id, n) {
      var unit = UNIT[id];
      return '<div class="res" data-u="' + escText(id) + '" style="--i:' + n + '">'
           + '<span class="dot ' + unitTone(unit) + '"></span>'
           + '<span class="pairs">' + markTerms(raised(id), list)
           + '<span class="sub">' + markTerms(unit.n, list) + '</span></span>'
           + '<span class="dim">' + escText(unit.d) + '</span></div>';
    }).join('') || NOTHING;

    tally(ids.length, Object.keys(UNIT).length, 'units');
    wlist.querySelectorAll('.res').forEach(function (el) {
      el.addEventListener('click', function () {
        selectResult(el, function () { showUnit(el.dataset.u); });
      });
    });
  }

  function draw() {
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

  /* ------------------------------------------------------------ detail pages */

  /* The detail already rendered into the page: the matrix cell holds the
     derived form, the graph edge holds the conversion as authored. */
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

  /* The graph tab groups its cards by dimension, which is what a row carries. */
  function graphCardFor(dimension) {
    var host = document.querySelector('#tab-seed .cy[data-group="'
                                      + cssValue(dimension) + '"]');
    return host ? host.closest('.seedcard') : null;
  }

  function openGraph(dimension, from, to) {
    var card = graphCardFor(dimension);
    if (!card) { return; }
    var tab = document.querySelector('.tab[data-pane="tab-seed"]');
    if (tab) { tab.click(); }
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

  /* ------------------------------------------------------------------ wiring */

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

  /* Opens the search tab with a question already asked. */
  function openFind(opts) {
    var tab = document.querySelector('.tab[data-pane="tab-find"]');
    if (!tab || !wlist || !hasIndex) {
      return;
    }
    tab.click();
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

  /* Dimensions and systems come from the data rather than a list written here,
     so adding a unit cannot leave the filters behind. */
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

    draw();
  }

  /* --------------------------------------------------- legend keys deep-link */

  /* Every key already says what a color means; clicking one asks the question
     it describes. The keys inside the overlay close it on the way out. */
  document.querySelectorAll('.legend [data-status], .legend [data-system],'
                          + ' .legend [data-kind]').forEach(function (key) {
    key.classList.add('clickable');
    key.addEventListener('click', function () {

      var inOverlay = !!key.closest('#overlay');
      var dim = inOverlay && otitle ? otitle.textContent.trim() : '';

      if (overlay && overlay.classList.contains('open')) { close(); }
      if (key.dataset.system) {
        openFind({mode: 'unit', system: key.dataset.system, dim: dim});
      } else if (key.dataset.kind) {
        openFind({kind: key.dataset.kind, dim: dim});
      } else {
        openFind({status: key.dataset.status, dim: dim});
      }
    });
  });

  /* -------------------------------------------------- summary rows deep-link */

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
      showSummary(false);
      if (name) {
        openFind({dim: name.textContent});
      } else if (hop) {
        openFind({hops: parseInt(hop.textContent, 10)});
      } else {
        openFind({status: state});
      }
    });
  });

  /* -------------------------------------------------------- in-matrix search */

  var ofind = document.getElementById('ofind');
  if (ofind) {
    wireFind(ofind, function (value) {
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
