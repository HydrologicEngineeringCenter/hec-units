
// Deals with the results, making them readable
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

  // Look up one step in a conversion
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

  // Panel to show work and steps
  function exU(id) {
    return '<span class="u">' + sup(escText(id)) + '</span>';
  }

  function exLine(value, hop) {
    var out = escText(num(value));
    if (hop.m !== 1) {
      out += ' <span class="op">×</span> ' + escText(num(hop.m));
    }
    if (hop.b !== 0) {
      out += ' <span class="op">' + (hop.b > 0 ? '+' : '−') + '</span> '
           + escText(num(Math.abs(hop.b)));
    }
    return out;
  }


  function exWorkHint() {
    return '<div class="cv-hint"><b>Pick a result</b>'
         + 'to see the math steps taken to complete the conversion.</div>';
  }

  // fetches source file information for the step
  function exFormula(from, to) {
    if (typeof FORMULA === 'undefined' || !FORMULA[from]) {
      return null;
    }
    return FORMULA[from][to] || null;
  }

  // build info card for unit
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


  // tells you where the number shown is from
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

  // helps follow the math steps and display the steps in the conversion
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
      + '<div class="cvwork-q">Convert ' + escText(num(value)) + ' ' + exU(from)
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
        + escText(num(hop.storedM))
        + (hop.storedB !== 0
           ? ' <span class="op">' + (hop.storedB > 0 ? '+' : '−') + '</span> '
             + escText(num(Math.abs(hop.storedB))) : '')
        + (hop.reversed
           ? ', so this direction uses the inverse: <span class="op">×</span> '
             + escText(num(hop.m))
             + (hop.b !== 0 ? ' <span class="op">' + (hop.b > 0 ? '+' : '−') + '</span> '
                + escText(num(Math.abs(hop.b))) : '')
           : '')
        + '</div>'
        + exWhere(hop)
        + '<div class="cvstep-eq">' + escText(num(step.before)) + ' ' + exU(step.from)
        + '<span class="eq">=</span>' + exLine(step.before, hop)
        + '<span class="eq">=</span><b>' + escText(num(step.after)) + '</b> '
        + exU(step.to) + '</div>'
        + '</li>';
    });

    html += '</ol>'
      + '<div class="cvwork-sum"><div class="cvwork-lbl">the whole route, in one step</div>'
      + '<div class="cvstep-eq">' + exU(to) + '<span class="eq">=</span>' + exU(from)
      + '<span class="op">×</span>' + escText(num(route.m))
      + (route.b !== 0 ? '<span class="op">' + (route.b > 0 ? '+' : '−') + '</span>'
         + escText(num(Math.abs(route.b))) : '')
      + '</div>'
      + '<div class="cvstep-eq answer">' + escText(num(value)) + ' ' + exU(from)
      + '<span class="eq">=</span><b>' + escText(num(running)) + '</b> ' + exU(to)
      + '</div></div>'
      + '<div class="cv-units"><div class="cvwork-lbl">the two units</div>'
      + exUnitCard(from) + exUnitCard(to) + '</div>'
      + '</div>';
    return html;
  }

  // handling click to see results
  function exWireResults() {
    var body = document.querySelector('.cv-body');
    if (!body || body.dataset.wired) {
      return;
    }
    body.dataset.wired = '1';

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
            + '<span class="cv-val">' + escText(num(r.value)) + '</span>'
            + '<span class="u">' + sup(escText(r.to)) + '</span>'
            + '<span class="cv-hops">' + r.hops + (r.hops === 1 ? ' hop' : ' hops')
            + '</span><span class="cv-more" aria-hidden="true">show the work</span>'
            + '</button>'
            + '<button type="button" class="cv-copy" data-copy="' + escText(num(r.value))
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

  // The unit picker for the conversion menu
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

  function openConverter(from, to, keepPlace) {
    var tab = document.querySelector('.tab[data-pane="tab-convert"]');
    var unit = document.getElementById('cvunit');
    var value = document.getElementById('cvvalue');
    if (!tab || !unit || !exUnits[from]) {
      return;
    }
    if (!keepPlace) { navLeave(); }
    navMoving = true;
    tab.click();
    navMoving = false;
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
