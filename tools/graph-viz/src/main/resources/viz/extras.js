
  var exUnits = typeof UNITS !== 'undefined' ? UNITS : {};
  var exHasData = typeof INDEX !== 'undefined';

  function exStore(key, value) {
    try {
      if (value === undefined) { return localStorage.getItem(key); }
      localStorage.setItem(key, value);
    } catch (e) { }
    return null;
  }

  function exSuggestions(limit) {
    if (!exHasData) {
      return [];
    }
    var failingDims = {};
    INDEX.forEach(function (row) {
      if (row.s === 'failed') { failingDims[row.d] = true; }
    });

    return INDEX.filter(function (row) {
      return row.s === 'untested' && row.h;
    }).map(function (row) {
      var cover = exUnits[row.f] && exUnits[row.t]
        ? (exUnits[row.f].c[0] === 0 ? 1 : 0) + (exUnits[row.t].c[0] === 0 ? 1 : 0)
        : 0;
      var score = row.h * 2 + cover * 3 + (failingDims[row.d] ? 2 : 0);
      var why = [];
      if (row.h >= 4) { why.push(row.h + ' hops compound rounding'); }
      if (cover) { why.push(cover === 2 ? 'neither unit is covered' : 'one unit is uncovered'); }
      if (failingDims[row.d]) { why.push('dimension has a failure'); }
      if (!why.length) { why.push(row.h + (row.h === 1 ? ' hop' : ' hops')); }
      return {row: row, score: score, why: why.join(' · ')};
    }).sort(function (a, b) {
      return b.score - a.score || a.row.f.localeCompare(b.row.f);
    }).slice(0, limit);
  }


  function exNeverTested() {
    return Object.keys(exUnits).filter(function (id) {
      return exUnits[id].c[0] === 0;
    }).sort();
  }

  var exBudget = null;

  function exPrecision() {
    if (exBudget || typeof routes !== 'function') {
      return exBudget || [];
    }
    var byDimension = {};
    Object.keys(exUnits).forEach(function (id) {
      (byDimension[exUnits[id].d] = byDimension[exUnits[id].d] || []).push(id);
    });

    exBudget = [];
    Object.keys(byDimension).forEach(function (dim) {
      var units = byDimension[dim].sort();
      var worst = 0;
      var pair = null;
      var checked = 0;
      units.forEach(function (a) {
        units.forEach(function (b) {
          if (a === b) { return; }
          var found = routes(a, b);
          if (found.length < 2) { return; }
          checked++;
          var lo = found[0].m;
          var hi = found[0].m;
          found.forEach(function (route) {
            lo = Math.min(lo, route.m);
            hi = Math.max(hi, route.m);
          });
          if (!lo || !isFinite(lo) || !isFinite(hi)) { return; }
          var spread = Math.abs(hi - lo) / Math.abs(lo);
          if (spread > worst) {
            worst = spread;
            pair = {f: a, t: b, n: found.length};
          }
        });
      });
      if (checked) {
        exBudget.push({dim: dim, worst: worst, pair: pair, pairs: checked});
      }
    });
    exBudget.sort(function (a, b) { return b.worst - a.worst; });
    return exBudget;
  }

  function exPrecisionHtml() {
    var rows = exPrecision();
    if (!rows.length) {
      return '';
    }
    var bad = rows.filter(function (row) { return row.worst > 1e-9; });
    return '<h4>Precision budget</h4>'
      + '<div class="sum-note">For every pair with more than one route, how far '
      + 'apart the routes land. Anything above zero means at least one route is '
      + 'wrong, and the gap is the smallest that error can be. '
      + (bad.length
         ? '<b>' + bad.length + ' of ' + rows.length + ' dimensions disagree.</b>'
         : 'Every dimension agrees with itself.') + '</div>'
      + '<table class="sum-table"><thead><tr><th>dimension</th>'
      + '<th>pairs with a choice</th><th>worst disagreement</th><th>where</th>'
      + '</tr></thead><tbody>'
      + rows.map(function (row) {
          var tone = row.worst > 1e-6 ? 'bad' : row.worst > 1e-9 ? 'warn' : 'ok';
          return '<tr' + (row.pair && row.worst > 1e-9
                  ? ' class="clickable suggest" data-from="' + escText(row.pair.f)
                    + '" data-to="' + escText(row.pair.t) + '"' : '') + '>'
            + '<td class="name">' + escText(row.dim) + '</td>'
            + '<td class="n">' + row.pairs + '</td>'
            + '<td class="n ' + tone + '">'
            + (row.worst > 1e-9 ? (row.worst * 100).toPrecision(3) + '%' : 'exact')
            + '</td>'
            + '<td class="p why">' + (row.pair && row.worst > 1e-9
                ? sup(escText(row.pair.f)) + ' → ' + sup(escText(row.pair.t))
                  + ' (' + row.pair.n + ' routes)'
                : '—') + '</td></tr>';
        }).join('')
      + '</tbody></table>';
  }

  function exBuildSummaryExtras() {
    var mount = document.getElementById('sumextra');
    if (!mount || !exHasData || mount.dataset.built) {
      return;
    }
    mount.dataset.built = '1';

    var picks = exSuggestions(8);
    var never = exNeverTested();
    var html = '';

    if (picks.length) {
      html += '<h4>What to test next</h4>'
        + '<div class="sum-note">Ranked by how much a test would tell you: longer '
        + 'chains carry more constants, uncovered units are blind spots, and a '
        + 'dimension with a failure is where a wrong constant is likely hiding.</div>'
        + '<table class="sum-table"><tbody>'
        + picks.map(function (pick) {
            return '<tr class="clickable suggest" data-from="' + escText(pick.row.f)
              + '" data-to="' + escText(pick.row.t) + '">'
              + '<td class="name">' + sup(escText(pick.row.f)) + ' <span class="arrow"></span> '
              + sup(escText(pick.row.t)) + '</td>'
              + '<td class="hopn">' + pick.row.h + (pick.row.h === 1 ? ' hop' : ' hops') + '</td>'
              + '<td class="p why">' + escText(pick.why) + '</td></tr>';
          }).join('')
        + '</tbody></table>';
    }

    html += exPrecisionHtml();

    if (never.length) {
      html += '<h4>Units no passing test touches</h4>'
        + '<div class="sum-note">' + never.length + ' of ' + Object.keys(exUnits).length
        + ' units take part in no conversion that a test exercises. Click one to '
        + 'see what it connects to.</div><div class="nevers">'
        + never.map(function (id) {
            return '<button type="button" class="never" data-unit="' + escText(id) + '">'
                 + sup(escText(id)) + '</button>';
          }).join('') + '</div>';
    }
    mount.innerHTML = html;

    mount.querySelectorAll('.suggest').forEach(function (row) {
      row.addEventListener('click', function () {
        showSummary(false);
        openFind({mode: 'conv', from: row.dataset.from, to: row.dataset.to});
      });
    });
    mount.querySelectorAll('.never').forEach(function (button) {
      button.addEventListener('click', function () {
        showSummary(false);
        openFind({mode: 'unit', unit: button.dataset.unit});
      });
    });
  }

  /* Uses the same route walk the graph explorer does, so the number shown here
     is the number the algorithm would produce - not a second opinion. */
  function exConvert(value, from) {
    var out = [];
    var dim = exUnits[from] && exUnits[from].d;
    if (!dim || typeof routes !== 'function') {
      return out;
    }
    Object.keys(exUnits).forEach(function (to) {
      if (to === from || exUnits[to].d !== dim) {
        return;
      }
      var found = routes(from, to);
      if (found.length) {
        out.push({to: to, value: found[0].m * value + found[0].b,
                  hops: found[0].path.length - 1, path: found[0].path});
      }
    });
    return out.sort(function (a, b) { return a.hops - b.hops || a.to.localeCompare(b.to); });
  }

  function exNumber(value) {
    if (!isFinite(value)) {
      return String(value);
    }
    if (value === Math.round(value) && Math.abs(value) < 1e15) {
      return String(value);
    }
    return Number(value.toPrecision(12)).toString();
  }


  function exHop(from, to) {
    if (typeof SEED === 'undefined') {
      return null;
    }
    for (var i = 0; i < SEED.length; i++) {
      if (SEED[i][0] === from && SEED[i][1] === to) {
        return {m: SEED[i][2], b: SEED[i][3], reversed: false,
                storedM: SEED[i][2], storedB: SEED[i][3], storedFrom: from, storedTo: to};
      }
    }
    for (var j = 0; j < SEED.length; j++) {
      if (SEED[j][0] === to && SEED[j][1] === from && SEED[j][2]) {
        var m = SEED[j][2];
        var b = SEED[j][3];
        return {m: 1 / m, b: -b / m, reversed: true,
                storedM: m, storedB: b, storedFrom: to, storedTo: from};
      }
    }
    return null;
  }

  function exU(id) {
    return '<span class="u">' + sup(escText(id)) + '</span>';
  }

  /* value × m + b, written the way a person would write it. */
  function exLine(value, hop) {
    var out = escText(exNumber(value));
    if (hop.m !== 1) {
      out += ' <span class="op">×</span> ' + escText(exNumber(hop.m));
    }
    if (hop.b !== 0) {
      out += ' <span class="op">' + (hop.b > 0 ? '+' : '−') + '</span> '
           + escText(exNumber(Math.abs(hop.b)));
    }
    return out;
  }


  function exWorkHint() {
    return '<div class="cv-hint"><b>Pick a result</b>'
         + 'to see the math steps taken to complete the conversion.</div>';
  }

  /* What the file says for this hop, with its named constants resolved. */
  function exFormula(from, to) {
    if (typeof FORMULA === 'undefined' || !FORMULA[from]) {
      return null;
    }
    return FORMULA[from][to] || null;
  }

  function exUnitCard(id) {
    var unit = exUnits[id];
    if (!unit) {
      return '';
    }
    return '<div class="cv-unit"><div class="cv-unit-h">' + exU(id) + '</div>'
      + '<div class="cv-unit-n">' + escText(unit.n || '') + '</div>'
      + '<dl class="cv-unit-d">'
      + '<dt>measures</dt><dd>' + escText(unit.d || '') + '</dd>'
      + '<dt>system</dt><dd>' + escText(unit.y === 'NULL' ? 'system-agnostic' : unit.y) + '</dd>'
      + (unit.x ? '<dt>description</dt><dd>' + escText(unit.x) + '</dd>' : '')
      + (unit.a && unit.a.length
         ? '<dt>also written</dt><dd>' + escText(unit.a.join(', ')) + '</dd>' : '')
      + '<dt>connects to</dt><dd>' + (unit.nb || []).length + ' unit'
      + ((unit.nb || []).length === 1 ? '' : 's') + ' directly</dd>'
      + '</dl></div>';
  }


  /* "x 0.3048" is half an answer; the other half is that it is m_per_ft, and
     that is the line you would go and check. */
  function exWhere(hop) {
    var formula = exFormula(hop.storedFrom, hop.storedTo);
    if (!formula) {
      return '';
    }
    var names = Object.keys(formula.w || {});
    return '<div class="cvstep-raw">as written: <code>' + escText(formula.r) + '</code></div>'
      + (names.length
         ? '<div class="cvstep-where"><span class="kw">where</span>'
           + names.map(function (name) {
               return '<span class="cv-const"><i>' + escText(name) + '</i> = '
                    + escText(formula.w[name]) + '</span>';
             }).join('<span class="sep">,</span>')
           + '</div>'
         : '');
  }

  function exDerivation(from, to, value) {
    var found = typeof routes === 'function' ? routes(from, to) : [];
    if (!found.length) {
      return '<div class="empty">No route connects these units.</div>';
    }
    var route = found[0];
    var path = route.path;

    var steps = [];
    var running = value;
    for (var i = 0; i < path.length - 1; i++) {
      var hop = exHop(path[i], path[i + 1]);
      if (!hop) {
        return '<div class="empty">One hop on this route is not a simple '
             + 'scale and offset, so it cannot be written out.</div>';
      }
      var before = running;
      running = running * hop.m + hop.b;
      steps.push({from: path[i], to: path[i + 1], hop: hop, before: before, after: running});
    }

    var html = '<div class="cvwork">'
      + '<div class="cvwork-top">'
      + '<div class="cvwork-q">Convert ' + escText(exNumber(value)) + ' ' + exU(from)
      + ' to ' + exU(to) + '</div>'
      + '<button type="button" class="cvgraph" data-from="' + escText(from)
      + '" data-to="' + escText(to) + '">Graph<span class="arrow"></span>'
      + escText(exUnits[from].d) + '</button></div>'
      + '<div class="cvwork-note">Shortest of ' + found.length + ' route'
      + (found.length === 1 ? '' : 's') + ', ' + steps.length + ' hop'
      + (steps.length === 1 ? '' : 's') + '. Each hop multiplies in one constant '
      + 'stored in <code>conversions.json</code>.</div>'
      + '<ol class="cvsteps">';

    steps.forEach(function (step, n) {
      var hop = step.hop;
      html += '<li class="cvstep">'
        + '<div class="cvstep-head"><span class="cvstep-n">' + (n + 1) + '</span>'
        + exU(step.from) + '<span class="arrow"></span>' + exU(step.to) + '</div>'
        + '<div class="cvstep-src">stored as ' + exU(hop.storedFrom)
        + '<span class="arrow"></span>' + exU(hop.storedTo) + ' <span class="op">×</span> '
        + escText(exNumber(hop.storedM))
        + (hop.storedB !== 0
           ? ' <span class="op">' + (hop.storedB > 0 ? '+' : '−') + '</span> '
             + escText(exNumber(Math.abs(hop.storedB))) : '')
        + (hop.reversed
           ? ', so this direction uses the inverse: <span class="op">×</span> '
             + escText(exNumber(hop.m))
             + (hop.b !== 0 ? ' <span class="op">' + (hop.b > 0 ? '+' : '−') + '</span> '
                + escText(exNumber(Math.abs(hop.b))) : '')
           : '')
        + '</div>'
        + exWhere(hop)
        + '<div class="cvstep-eq">' + escText(exNumber(step.before)) + ' ' + exU(step.from)
        + '<span class="eq">=</span>' + exLine(step.before, hop)
        + '<span class="eq">=</span><b>' + escText(exNumber(step.after)) + '</b> '
        + exU(step.to) + '</div>'
        + '</li>';
    });

    html += '</ol>'
      + '<div class="cvwork-sum"><div class="cvwork-lbl">the whole route, in one step</div>'
      + '<div class="cvstep-eq">' + exU(to) + '<span class="eq">=</span>' + exU(from)
      + '<span class="op">×</span>' + escText(exNumber(route.m))
      + (route.b !== 0 ? '<span class="op">' + (route.b > 0 ? '+' : '−') + '</span>'
         + escText(exNumber(Math.abs(route.b))) : '')
      + '</div>'
      + '<div class="cvstep-eq answer">' + escText(exNumber(value)) + ' ' + exU(from)
      + '<span class="eq">=</span><b>' + escText(exNumber(running)) + '</b> ' + exU(to)
      + '</div></div>'
      + '<div class="cv-units"><div class="cvwork-lbl">the two units</div>'
      + exUnitCard(from) + exUnitCard(to) + '</div>'
      + '</div>';
    return html;
  }


  /* Clicking a result opens the whole derivation beneath it. One at a time -
     several open at once turns the page into a wall of arithmetic. */
  function exWireResults() {
    var body = document.querySelector('.cv-body');
    if (!body || body.dataset.wired) {
      return;
    }
    body.dataset.wired = '1';

    /* Bound to the pair of columns rather than to either one: the rows live in
       the list and the graph button lives in the side panel, and the working
       moves between them as it is rebuilt. */
    body.addEventListener('click', function (event) {
      var graph = event.target.closest('.cvgraph');
      if (graph) {
        var unit = exUnits[graph.dataset.from];
        if (unit && typeof openGraph === 'function'
            && typeof graphCardFor === 'function' && graphCardFor(unit.d)) {
          openGraph(unit.d, graph.dataset.from, graph.dataset.to);
        } else {
          exToast('No conversion graph exists for '
                  + (unit ? unit.d : 'this dimension') + '.');
        }
        return;
      }

      var row = event.target.closest('.cv-row');
      if (row) {
        exShowWork(row.dataset.to);
      }
    });
  }

  /* The working goes in the side panel, where there is room to lay it out
     properly, and stays put while you look down the list. */
  function exShowWork(to) {
    var side = document.getElementById('cvwork');
    var list = document.getElementById('cvout');
    if (!side) {
      return;
    }
    var from = document.getElementById('cvunit').value.trim();
    var value = parseFloat(document.getElementById('cvvalue').value);

    list.querySelectorAll('.cv-row').forEach(function (row) {
      var on = row.dataset.to === to;
      row.classList.toggle('open', on);
      row.setAttribute('aria-expanded', String(on));
    });

    side.innerHTML = exDerivation(from, to, value);
    side.dataset.showing = to;
    side.scrollTop = 0;
  }

  function exRunConverter() {
    var input = document.getElementById('cvvalue');
    var unit = document.getElementById('cvunit');
    var list = document.getElementById('cvout');
    if (!input || !list) {
      return;
    }
    var from = unit.value.trim();
    var value = parseFloat(input.value);

    if (!exUnits[from]) {
      list.innerHTML = '<div class="empty">' + (from
        ? 'No unit called ' + escText(from) + '.'
        : 'Type a unit to convert from - try ft, m<sup>3</sup>, or cfs.') + '</div>';
      return;
    }
    if (!isFinite(value)) {
      list.innerHTML = '<div class="empty">Enter a number to convert.</div>';
      return;
    }
    var results = exConvert(value, from);
    if (!results.length) {
      list.innerHTML = '<div class="empty">Nothing converts from '
        + sup(escText(from)) + '.</div>';
      return;
    }
    list.innerHTML = '<div class="cv-note">' + results.length + ' unit'
      + (results.length === 1 ? '' : 's') + ' in ' + escText(exUnits[from].d) + '</div>'
      + results.map(function (r, i) {
          return '<div class="cv-item"><button type="button" class="cv-row"'
            + ' style="--i:' + i + '" data-to="' + escText(r.to)
            + '" aria-expanded="false" title="Show how this was worked out">'
            + '<span class="cv-val">' + escText(exNumber(r.value)) + '</span>'
            + '<span class="u">' + sup(escText(r.to)) + '</span>'
            + '<span class="cv-hops">' + r.hops + (r.hops === 1 ? ' hop' : ' hops')
            + '</span><span class="cv-more" aria-hidden="true">show the work</span>'
            + '</button>'
            + '<button type="button" class="cv-copy" data-copy="' + escText(exNumber(r.value))
            + '" title="Copy this value">⧉</button></div>';
        }).join('');

    // Keep the open working in step with the value being typed.
    var side = document.getElementById('cvwork');
    if (side && side.dataset.showing
        && results.some(function (r) { return r.to === side.dataset.showing; })) {
      exShowWork(side.dataset.showing);
    } else if (side) {
      side.innerHTML = exWorkHint();
      delete side.dataset.showing;
    }
  }

  function exComboItems(filter) {
    var byDimension = {};
    Object.keys(exUnits).sort().forEach(function (id) {
      var unit = exUnits[id];
      var hay = (id + ' ' + (unit.n || '') + ' ' + (unit.d || '')).toLowerCase();
      if (filter && hay.indexOf(filter) < 0) {
        return;
      }
      (byDimension[unit.d] = byDimension[unit.d] || []).push(id);
    });

    var groups = Object.keys(byDimension).sort();
    if (!groups.length) {
      return '<div class="cv-none">No unit matches that.</div>';
    }
    return groups.map(function (dim) {
      return '<div class="cv-group">' + escText(dim) + '</div>'
        + byDimension[dim].map(function (id) {
            return '<button type="button" role="option" class="cv-opt" data-unit="'
              + escText(id) + '"><span class="u">' + sup(escText(id)) + '</span>'
              + '<span class="cv-optname">' + escText(exUnits[id].n || '') + '</span>'
              + '</button>';
          }).join('');
    }).join('');
  }

  /*
   * Opening on focus was the whole problem: picking an option calls focus() to
   * put the cursor back, which fired the focus handler, which reopened the list
   * you had just chosen from. The same fight made the arrow show a filtered
   * list, because focus() re-ran the filtered open straight after it.
   *
   * So the list only opens when it is asked to - the arrow, typing, or Down.
   */
  function exComboOpen(showAll) {
    var input = document.getElementById('cvunit');
    var list = document.getElementById('cvlist');
    var filter = showAll ? '' : input.value.trim().toLowerCase();
    list.innerHTML = exComboItems(filter);
    list.hidden = false;
    input.setAttribute('aria-expanded', 'true');

    var current = list.querySelector('[data-unit="' + cssValue(input.value.trim()) + '"]');
    if (current) {
      current.classList.add('here');
      current.scrollIntoView({block: 'center'});
    } else {
      list.scrollTop = 0;
    }
  }

  function exComboClose() {
    var list = document.getElementById('cvlist');
    if (list && !list.hidden) {
      list.hidden = true;
      document.getElementById('cvunit').setAttribute('aria-expanded', 'false');
    }
  }

  function exComboPick(unit) {
    var input = document.getElementById('cvunit');
    input.value = unit;
    exComboClose();
    input.focus();
    exRunConverter();
  }

  /* Open the converter on a given unit, and if a target is named, land on its
     working rather than making the reader find the row again. */
  function openConverter(from, to) {
    var tab = document.querySelector('.tab[data-pane="tab-convert"]');
    var unit = document.getElementById('cvunit');
    var value = document.getElementById('cvvalue');
    if (!tab || !unit || !exUnits[from]) {
      return;
    }
    tab.click();
    unit.value = from;
    if (!parseFloat(value.value)) { value.value = '1'; }
    exComboClose();
    exRunConverter();
    if (to) {
      exShowWork(to);
    }
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  function exWireCombo() {
    var input = document.getElementById('cvunit');
    var pick = document.getElementById('cvpick');
    var list = document.getElementById('cvlist');
    if (!input || !list) {
      return;
    }

    pick.addEventListener('mousedown', function (event) {
      event.preventDefault();                 // keep the caret in the field
      var wasOpen = !list.hidden;
      exComboClose();
      if (!wasOpen) {
        exComboOpen(true);                    // the arrow always shows everything
      }
    });

    input.addEventListener('input', function () {
      exComboOpen(false);
      exRunConverter();
    });

    input.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') {
        event.preventDefault();
        exComboClose();
        return;
      }
      if (event.key === 'ArrowDown') {
        event.preventDefault();
        if (list.hidden) {
          exComboOpen(input.value.trim() ? false : true);
        }
        var first = list.querySelector('.cv-opt');
        if (first) { first.focus(); }
        return;
      }
      if (event.key === 'Enter' && !list.hidden) {
        var only = list.querySelector('.cv-opt');
        if (only) {
          event.preventDefault();
          exComboPick(only.dataset.unit);
        }
      }
    });

    list.addEventListener('keydown', function (event) {
      var options = Array.prototype.slice.call(list.querySelectorAll('.cv-opt'));
      var at = options.indexOf(document.activeElement);
      if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
        event.preventDefault();
        var next = options[at + (event.key === 'ArrowDown' ? 1 : -1)];
        if (next) {
          next.focus();
        } else if (event.key === 'ArrowUp') {
          input.focus();
        }
      } else if (event.key === 'Escape' || event.key === 'Tab') {
        exComboClose();
        if (event.key === 'Escape') { input.focus(); }
      } else if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        exComboPick(document.activeElement.dataset.unit);
      }
    });

    // mousedown, not click: the list is rebuilt on close, so by click time the
    // element under the cursor may already be gone.
    list.addEventListener('mousedown', function (event) {
      var option = event.target.closest('.cv-opt');
      if (option) {
        event.preventDefault();
        exComboPick(option.dataset.unit);
      }
    });

    document.addEventListener('mousedown', function (event) {
      if (!event.target.closest('.cv-combo')) { exComboClose(); }
    });
  }


  function exCopy(text, said) {
    function tell() {
      if (typeof edToast === 'function') { edToast(said); } else { exToast(said); }
    }
    if (navigator.clipboard) {
      navigator.clipboard.writeText(text).then(tell, function () { exToast('Could not copy'); });
    } else {
      exToast('Copying is unavailable here');
    }
  }

  var exToastEl = null;
  var exToastTimer = null;

  function exToast(message) {
    if (!exToastEl) {
      exToastEl = document.createElement('div');
      exToastEl.id = 'toast';
      exToastEl.setAttribute('role', 'status');
      exToastEl.addEventListener('click', exHideToast);
      document.body.appendChild(exToastEl);
    }
    exToastEl.textContent = message;
    exToastEl.classList.add('show');
    clearTimeout(exToastTimer);
    exToastTimer = setTimeout(exHideToast, 2600);
  }

  function exHideToast() {
    clearTimeout(exToastTimer);
    if (exToastEl) { exToastEl.classList.remove('show'); }
  }

  /* It confirmed something about the screen you were on; going somewhere else
     is the end of that. */
  document.addEventListener('click', function (event) {
    if (event.target.closest('.tab,.card,#oclose,#sclose,.mode,#sumopen')) {
      exHideToast();
    }
  }, true);

  document.addEventListener('click', function (event) {
    var button = event.target.closest('[data-copy]');
    if (button) {
      exCopy(button.dataset.copy, 'Copied ' + button.dataset.copy);
    }
  });

  var exRestoring = false;

  function exWriteUrl() {
    if (exRestoring || !exHasData) {
      return;
    }
    var parts = [];
    var tab = document.querySelector('.tab.active');
    if (tab) { parts.push('tab=' + tab.dataset.pane.replace('tab-', '')); }

    var overlay = document.getElementById('overlay');
    if (overlay && overlay.classList.contains('open')) {
      parts.push((overlay.classList.contains('seedmode') ? 'graph=' : 'matrix=')
                 + encodeURIComponent(document.getElementById('otitle').textContent));
    }
    var newHash = '#' + parts.join('&');
    if (newHash !== location.hash) {
      history.replaceState(null, '', newHash);
    }
  }

  function exReadUrl() {
    if (!location.hash || location.hash.length < 2 || !exHasData) {
      return false;
    }
    exRestoring = true;
    var query = {};
    location.hash.slice(1).split('&').forEach(function (pair) {
      var eq = pair.indexOf('=');
      if (eq > 0) {
        query[pair.slice(0, eq)] = decodeURIComponent(pair.slice(eq + 1));
      }
    });

    if (query.tab) {
      var tab = document.querySelector('.tab[data-pane="tab-' + cssValue(query.tab) + '"]');
      if (tab) { tab.click(); }
    }
    var name = query.matrix || query.graph;
    if (name) {
      var kind = query.graph ? '.seedcard' : '.card:not(.seedcard)';
      var card = document.querySelector(kind + '[data-name="' + cssValue(name) + '"]');
      if (card) { card.click(); }
    }
    exRestoring = false;
    return true;
  }

  function exRememberTab() {
    document.querySelectorAll('.tab').forEach(function (tab) {
      tab.addEventListener('click', function () {
        exStore('viz-tab', tab.dataset.pane);
        exWriteUrl();
      });
    });
  }

  function exRestoreTab() {
    var saved = exStore('viz-tab');
    if (!saved) {
      return;
    }
    var tab = document.querySelector('.tab[data-pane="' + cssValue(saved) + '"]');
    if (tab) { tab.click(); }
  }

  var EX_HELP = [
    ['Anywhere', [
      [['c'], 'go to Coverage'],
      [['g'], 'go to Conversion Graphs'],
      [['f'], 'go to Search'],
      [['v'], 'go to the Converter'],
      [['s'], 'open the test-suite summary'],
      [['t'], 'switch between light and dark'],
      [['?'], 'open this list'],
      [['Esc'], 'close whatever is open']]],
    ['On a coverage matrix', [
      [['click'], 'a card to enlarge it'],
      [['click', 'or', 'Tab'], 'land on a cell - the arrows continue from there'],
      [['↑', '↓', '←', '→'], 'move cell by cell; the detail follows'],
      [['Enter'], 'pin a cell so its detail stays while you look elsewhere'],
      [['Home', 'End'], 'jump to the start or end of a row'],
      [['PgUp', 'PgDn'], 'jump to the top or bottom of a column']]],
    ['On a conversion graph', [
      [['click'], 'two units to list every route between them'],
      [['click'], 'a route to hold it and keep the arrows running'],
      [['drag'], 'a unit to untangle, scroll to zoom']]],
    ['In Search', [
      [['/'], 'jump into the search box'],
      [['click'], 'any legend color to search for it']]]];

  /* Raised and lowered like the other overlays, rather than appearing and
     vanishing on the spot. */
  function exCloseHelp() {
    var box = document.getElementById('keyhelp');
    if (!box) {
      return;
    }
    box.classList.remove('in');
    setTimeout(function () { box.remove(); }, 200);
    if (exHelpReturn && exHelpReturn.focus) { exHelpReturn.focus(); }
    exHelpReturn = null;
  }

  var exHelpReturn = null;

  function exHelp() {
    var box = document.getElementById('keyhelp');
    if (box) {
      exCloseHelp();
      return;
    }
    exHelpReturn = document.activeElement;
    box = document.createElement('div');
    box.id = 'keyhelp';
    box.setAttribute('role', 'dialog');
    box.setAttribute('aria-modal', 'true');
    box.setAttribute('aria-label', 'Keyboard shortcuts');
    box.innerHTML = '<div class="kh-card"><div class="kh-head">'
      + '<h3>Keyboard &amp; tips</h3>'
      + '<button type="button" class="kh-x" aria-label="Close">✕</button></div>'
      + EX_HELP.map(function (section) {
          return '<h4>' + escText(section[0]) + '</h4><dl>'
            + section[1].map(function (entry) {
                return '<dt>' + entry[0].map(function (key) {
                    return '<kbd>' + escText(key) + '</kbd>';
                  }).join('') + '</dt><dd>' + escText(entry[1]) + '</dd>';
              }).join('') + '</dl>';
        }).join('')
      + '</div>';
    document.body.appendChild(box);
    requestAnimationFrame(function () {
      requestAnimationFrame(function () { box.classList.add('in'); });
    });
    box.querySelector('.kh-x').focus();
    box.addEventListener('click', function (event) {
      if (event.target === box || event.target.closest('.kh-x')) {
        exCloseHelp();
      }
    });
  }

  function exTypingIn(target) {
    return target && (target.tagName === 'INPUT' || target.tagName === 'SELECT'
                      || target.tagName === 'TEXTAREA' || target.isContentEditable);
  }

  document.addEventListener('keydown', function (event) {
    if (event.metaKey || event.ctrlKey || event.altKey) {
      return;
    }
    if (event.key === 'Escape') {
      exCloseHelp();
      return;
    }
    if (exTypingIn(event.target)) {
      return;
    }

    var panes = {c: 'tab-coverage', g: 'tab-seed', f: 'tab-find', v: 'tab-convert'};
    if (panes[event.key]) {
      var tab = document.querySelector('.tab[data-pane="' + panes[event.key] + '"]');
      if (tab) { event.preventDefault(); tab.click(); }
      return;
    }
    if (event.key === '/') {
      var box = document.querySelector('.tabpane.active input[type=search]');
      if (box) { event.preventDefault(); box.focus(); box.select(); }
      return;
    }
    if (event.key === 's') {
      var summary = document.getElementById('sumopen');
      if (summary) { event.preventDefault(); summary.click(); }
      return;
    }
    if (event.key === 't') {
      var theme = document.getElementById('themetoggle');
      if (theme) { event.preventDefault(); theme.click(); }
      return;
    }
    if (event.key === '?') {
      event.preventDefault();
      exHelp();
    }
  });

  if (exHasData) {
    exRememberTab();
    if (!exReadUrl()) {
      exRestoreTab();
    }

    var cvValue = document.getElementById('cvvalue');
    if (cvValue) {
      cvValue.addEventListener('input', exRunConverter);
      exWireCombo();
      exWireResults();
      document.getElementById('cvwork').innerHTML = exWorkHint();
      exRunConverter();
    }

    var keysopen = document.getElementById('keysopen');
    if (keysopen) {
      keysopen.addEventListener('click', exHelp);
    }

    var sumopen = document.getElementById('sumopen');
    if (sumopen) {
      sumopen.addEventListener('click', function () {
        setTimeout(exBuildSummaryExtras, 0);
      });
    }
  }
