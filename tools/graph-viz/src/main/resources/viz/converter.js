
// The converter: a measure, a value, a unit at each end.

  function cvEl(id) {
    return document.getElementById(id);
  }

  function cvDim() {
    var select = cvEl('cvdim');
    return select ? select.value : '';
  }

  function cvUnitsIn(dim) {
    return Object.keys(exUnits).filter(function (id) {
      return !dim || exUnits[id].d === dim;
    }).sort();
  }

  // Picking a unit answers the measure question too, so the select follows it.
  function cvSyncDim(id) {
    var select = cvEl('cvdim');
    var unit = exUnits[id];
    if (select && unit && select.value !== unit.d) {
      select.value = unit.d;
    }
  }

  function cvFillDims() {
    var select = cvEl('cvdim');
    if (!select) {
      return;
    }
    var counts = {};
    Object.keys(exUnits).forEach(function (id) {
      counts[exUnits[id].d] = (counts[exUnits[id].d] || 0) + 1;
    });
    Object.keys(counts).sort().forEach(function (dim) {
      if (counts[dim] < 2) {
        return;                          // nothing in it to convert between
      }
      var option = document.createElement('option');
      option.value = dim;
      option.textContent = dim + '  (' + counts[dim] + ')';
      select.appendChild(option);
    });
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
                  hops: found[0].path.length - 1});
      }
    });
    return out.sort(function (a, b) { return a.hops - b.hops || a.to.localeCompare(b.to); });
  }

  function exAnswer(value, from, to) {
    if (!exUnits[from] || !exUnits[to] || typeof routes !== 'function') {
      return null;
    }
    var found = routes(from, to);
    return found.length ? found[0].m * value + found[0].b : null;
  }

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
    return '<div class="cv-hint"><b>Pick both units</b>'
         + 'and the arithmetic behind the answer appears here, hop by hop.</div>';
  }

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

  // `chosen` names one particular route; without it the shortest one is used.
  function exDerivation(from, to, value, chosen) {
    var found = typeof routes === 'function' ? routes(from, to) : [];
    if (!found.length && !chosen) {
      return '<div class="empty">No route connects these units.</div>';
    }
    var route = chosen || found[0];
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
      + '<div class="cvwork-note">'
      + (chosen ? 'This route, ' : 'Shortest of ' + found.length + ' route'
         + (found.length === 1 ? '' : 's') + ', ')
      + steps.length + ' hop' + (steps.length === 1 ? '' : 's')
      + '. Each hop multiplies in one constant '
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

  var workReturn = null;

  function exCloseWork() {
    var box = document.getElementById('routework');
    if (!box) {
      return false;
    }
    box.classList.remove('in');
    setTimeout(function () { box.remove(); }, 200);
    if (workReturn && workReturn.focus) { workReturn.focus(); }
    workReturn = null;
    return true;
  }

  function exOpenWork(from, to, route) {
    exCloseWork();
    workReturn = document.activeElement;

    var box = document.createElement('div');
    box.id = 'routework';
    box.setAttribute('role', 'dialog');
    box.setAttribute('aria-modal', 'true');
    box.setAttribute('aria-label', 'How this route is worked out');
    box.innerHTML = '<div class="rw-card"><div class="rw-head">'
      + '<h3>' + sup(escText(from)) + ARROW + sup(escText(to))
      + '<span class="rw-hops">' + (route.path.length - 1)
      + (route.path.length === 2 ? ' hop' : ' hops') + '</span></h3>'
      + '<button type="button" class="rw-x" aria-label="Close">✕</button></div>'
      + '<div class="rw-body">' + exDerivation(from, to, 1, route) + '</div></div>';

    document.body.appendChild(box);
    requestAnimationFrame(function () {
      requestAnimationFrame(function () { box.classList.add('in'); });
    });
    box.querySelector('.rw-x').focus();
    box.addEventListener('click', function (event) {
      if (event.target === box || event.target.closest('.rw-x')) {
        exCloseWork();
      }
    });
  }

  function exFilled() {
    ['cvvalue', 'cvfrom', 'cvto'].forEach(function (id) {
      var field = cvEl(id);
      if (field) {
        field.closest('.cv-field,.cv-combo').classList.toggle('filled',
                                                              field.value.length > 0);
      }
    });
  }

  function exResultShow(text, unit, muted) {
    var out = cvEl('cvresult');
    if (!out) {
      return;
    }
    out.classList.toggle('muted', !!muted);
    out.innerHTML = muted
      ? escText(text)
      : '<button type="button" class="cv-take" data-copy="' + escText(text)
        + '" title="Copy this value">' + escText(text) + '</button>'
        + (unit ? '<span class="u">' + sup(escText(unit)) + '</span>' : '');
  }

  function exRunConverter() {
    var input = cvEl('cvvalue');
    var list = cvEl('cvout');
    if (!input || !list) {
      return;
    }
    exFilled();

    var from = cvEl('cvfrom').value.trim();
    var to = cvEl('cvto').value.trim();
    var value = parseFloat(input.value);
    var dim = cvDim();

    var pool = cvUnitsIn(dim);
    var count = cvEl('cvcount');
    if (count) {
      count.textContent = dim ? '' : pool.length + ' units in all';
    }

    if (!exUnits[from]) {
      exResultShow(from ? 'no unit called ' + raised(from) : 'pick a unit to convert from',
                   '', true);
      list.innerHTML = '<div class="empty">'
        + (dim ? 'Pick a unit from <b>' + escText(dim) + '</b> to convert.'
               : 'Pick a measure above, or type a unit - try ft, m<sup>3</sup>, or cfs.')
        + '</div>';
      exWorkShow(null, null, null);
      return;
    }
    if (!isFinite(value)) {
      exResultShow('enter a number', '', true);
      list.innerHTML = '<div class="empty">Enter a number to convert.</div>';
      exWorkShow(null, null, null);
      return;
    }

    var answer = to ? exAnswer(value, from, to) : null;
    if (to && !exUnits[to]) {
      exResultShow('no unit called ' + raised(to), '', true);
    } else if (to && answer === null) {
      exResultShow('no route to ' + raised(to), '', true);
    } else if (to) {
      exResultShow(num(answer), to, false);
    } else {
      exResultShow('pick a unit to convert to', '', true);
    }

    exDrawList(from, to, value);
    exWorkShow(from, to, value);
  }

  /*
   * The rows only change when the unit being converted from does. Rebuilding
   * them for a new digit or a new destination would replay every row's entrance
   * animation, which reads as the whole panel flickering.
   */
  function exDrawList(from, to, value) {
    var list = cvEl('cvout');
    var results = exConvert(value, from);
    if (!results.length) {
      list.innerHTML = '<div class="empty">Nothing converts from '
        + sup(escText(from)) + '.</div>';
      list.dataset.sig = '';
      return;
    }

    var sig = from + ' ' + cvDim();
    if (list.dataset.sig === sig) {
      var items = list.querySelectorAll('.cv-item');
      results.forEach(function (r, i) {
        if (!items[i]) {
          return;
        }
        var text = num(r.value);
        items[i].querySelector('.cv-val').textContent = text;
        items[i].querySelector('.cv-copy').dataset.copy = text;
      });
    } else {
      list.dataset.sig = sig;
      list.innerHTML = '<div class="cv-note">every ' + escText(exUnits[from].d)
        + ' unit &middot; click one to convert into it</div>'
        + results.map(function (r, i) {
            return '<div class="cv-item"><button type="button" class="cv-row"'
              + ' style="--i:' + i + '" data-to="' + escText(r.to)
              + '" title="Convert into this unit">'
              + '<span class="cv-val">' + escText(num(r.value)) + '</span>'
              + '<span class="u">' + sup(escText(r.to)) + '</span>'
              + '<span class="cv-hops">' + r.hops + (r.hops === 1 ? ' hop' : ' hops')
              + '</span></button>'
              + '<button type="button" class="cv-copy" data-copy="' + escText(num(r.value))
              + '" title="Copy this value">⧉</button></div>';
          }).join('');
    }

    list.querySelectorAll('.cv-row').forEach(function (row) {
      var on = row.dataset.to === to;
      row.classList.toggle('open', on);
      row.setAttribute('aria-pressed', String(on));
    });
  }

  // Only a new pair is worth fading in; a new digit just replaces the numbers.
  function exWorkShow(from, to, value) {
    var side = cvEl('cvwork');
    if (!side) {
      return;
    }
    if (!from || !to || !exUnits[to] || !isFinite(value)) {
      side.innerHTML = exWorkHint();
      delete side.dataset.showing;
      return;
    }
    var pair = from + ' ' + to;
    var fresh = side.dataset.showing !== pair;
    side.innerHTML = exDerivation(from, to, value);
    side.dataset.showing = pair;
    if (fresh) {
      if (side.firstElementChild) { side.firstElementChild.classList.add('fresh'); }
      side.scrollTop = 0;
    }
  }

  function exSetPair(from, to) {
    if (from !== null) { cvEl('cvfrom').value = from; }
    if (to !== null) { cvEl('cvto').value = to; }
    cvSyncDim(from || to);
    exRunConverter();
  }

  // The answer becomes the new input, so swapping keeps the same real quantity:
  // 1 ft to 0.3048 m turns into 0.3048 m to 1 ft rather than 1 m to something.
  function exSwap() {
    var from = cvEl('cvfrom');
    var to = cvEl('cvto');
    var value = cvEl('cvvalue');
    if (!from || !to || !value) {
      return;
    }
    var answer = exAnswer(parseFloat(value.value), from.value.trim(), to.value.trim());
    var was = from.value;
    from.value = to.value;
    to.value = was;
    if (answer !== null && isFinite(answer)) {
      value.value = num(answer);
    }
    exComboClose();
    exRunConverter();
  }

  function exResetConverter() {
    var value = cvEl('cvvalue');
    var from = cvEl('cvfrom');
    var to = cvEl('cvto');
    if (!value || !from || !to) {
      return;
    }
    if (from.value === '' && to.value === '' && value.value === '1' && !cvDim()) {
      return;
    }
    value.value = '1';
    from.value = '';
    to.value = '';
    if (cvEl('cvdim')) { cvEl('cvdim').value = ''; }
    exComboClose();
    exRunConverter();
  }

  function exComboItems(filter, skip) {
    var dim = cvDim();
    var byDimension = {};
    cvUnitsIn(dim).forEach(function (id) {
      var unit = exUnits[id];
      var hay = (id + ' ' + (unit.n || '') + ' ' + (unit.d || '')).toLowerCase();
      if (id === skip || (filter && hay.indexOf(filter) < 0)) {
        return;
      }
      (byDimension[unit.d] = byDimension[unit.d] || []).push(id);
    });

    var groups = Object.keys(byDimension).sort();
    if (!groups.length) {
      return '<div class="cv-none">No unit matches that.</div>';
    }
    return groups.map(function (name) {
      return '<div class="cv-group">' + escText(name) + '</div>'
        + byDimension[name].map(function (id) {
            return '<button type="button" role="option" class="cv-opt" data-unit="'
              + escText(id) + '"><span class="u">' + sup(escText(id)) + '</span>'
              + '<span class="cv-optname">' + escText(exUnits[id].n || '') + '</span>'
              + '</button>';
          }).join('');
    }).join('');
  }

  function exComboOpen(id, showAll) {
    var input = cvEl(id);
    var list = cvEl(id + 'list');
    var other = cvEl(id === 'cvfrom' ? 'cvto' : 'cvfrom');
    var filter = showAll ? '' : input.value.trim().toLowerCase();

    exComboClose();
    list.innerHTML = exComboItems(filter, other ? other.value.trim() : '');
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
    document.querySelectorAll('.cvlist').forEach(function (list) {
      if (!list.hidden) {
        list.hidden = true;
        var input = cvEl(list.id.replace('list', ''));
        if (input) { input.setAttribute('aria-expanded', 'false'); }
      }
    });
  }

  function exComboPick(id, unit) {
    var input = cvEl(id);
    input.value = unit;
    exComboClose();
    cvSyncDim(unit);
    input.focus();
    exRunConverter();
  }

  function openConverter(from, to, keepPlace) {
    var tab = document.querySelector('.tab[data-pane="tab-convert"]');
    if (!tab || !cvEl('cvfrom') || !exUnits[from]) {
      return;
    }
    if (!keepPlace) { navLeave(); }
    navMoving = true;
    tab.click();
    navMoving = false;

    var value = cvEl('cvvalue');
    if (!parseFloat(value.value)) { value.value = '1'; }
    exComboClose();
    exSetPair(from, to || '');
    window.scrollTo({top: 0, behavior: 'smooth'});
  }

  function exComboKeys(id) {
    var input = cvEl(id);
    var list = cvEl(id + 'list');

    input.addEventListener('input', function () {
      exComboOpen(id, false);
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
          exComboOpen(id, !input.value.trim());
        }
        var first = list.querySelector('.cv-opt');
        if (first) { first.focus(); }
        return;
      }
      if (event.key === 'Enter' && !list.hidden) {
        var only = list.querySelector('.cv-opt');
        if (only) {
          event.preventDefault();
          exComboPick(id, only.dataset.unit);
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
        exComboPick(id, document.activeElement.dataset.unit);
      }
    });

    // mousedown, not click: the list is rebuilt on close, so by click time the
    // element under the cursor may already be gone.
    list.addEventListener('mousedown', function (event) {
      var option = event.target.closest('.cv-opt');
      if (option) {
        event.preventDefault();
        exComboPick(id, option.dataset.unit);
      }
    });
  }

  function exWireCombo() {
    if (!cvEl('cvfrom') || !cvEl('cvto')) {
      return;
    }
    cvFillDims();
    exComboKeys('cvfrom');
    exComboKeys('cvto');

    cvEl('cvdim').addEventListener('change', function () {
      var dim = cvDim();
      ['cvfrom', 'cvto'].forEach(function (id) {
        var field = cvEl(id);
        if (dim && field.value && exUnits[field.value]
            && exUnits[field.value].d !== dim) {
          field.value = '';               // it does not measure this any more
        }
      });
      exComboClose();
      exRunConverter();
    });

    cvEl('cvswap').addEventListener('click', exSwap);

    document.querySelectorAll('.cvpick').forEach(function (button) {
      button.addEventListener('mousedown', function (event) {
        event.preventDefault();           // keep the caret in the field
        var id = button.dataset.opens;
        var open = !cvEl(id + 'list').hidden;
        exComboClose();
        if (!open) {
          exComboOpen(id, true);          // the arrow always shows everything
        }
      });
    });

    document.querySelectorAll('.cvclear').forEach(function (button) {
      button.addEventListener('mousedown', function (event) {
        event.preventDefault();
      });
      button.addEventListener('click', function () {
        var field = cvEl(button.dataset.clears);
        field.value = '';
        exComboClose();
        exRunConverter();
        field.focus();
      });
    });

    document.addEventListener('mousedown', function (event) {
      if (!event.target.closest('.cv-combo')) { exComboClose(); }
    });
  }

  function exWireResults() {
    var pane = cvEl('tab-convert');
    if (!pane || pane.dataset.wired) {
      return;
    }
    pane.dataset.wired = '1';

    pane.addEventListener('click', function (event) {
      var row = event.target.closest('.cv-row');
      if (row) {
        exSetPair(null, row.dataset.to);
      }
    });
  }

  document.addEventListener('click', function (event) {
    var graph = event.target.closest('.cvgraph');
    if (!graph) {
      return;
    }
    var unit = exUnits[graph.dataset.from];
    exCloseWork();
    if (unit && typeof openGraph === 'function'
        && typeof graphCardFor === 'function' && graphCardFor(unit.d)) {
      openGraph(unit.d, graph.dataset.from, graph.dataset.to);
    } else {
      exToast('No conversion graph exists for '
              + (unit ? unit.d : 'this dimension') + '.');
    }
  });
