// The coverage matrices, built here rather than shipped as markup.

  var mxPairs = null;                 // a joined "from,to" key is fragile; nested map instead

  function mxIndex() {
    if (mxPairs) {
      return mxPairs;
    }
    mxPairs = {};
    if (typeof INDEX !== 'undefined') {
      INDEX.forEach(function (row) {
        (mxPairs[row.f] = mxPairs[row.f] || {})[row.t] = row;
      });
    }
    return mxPairs;
  }

  function mxRow(from, to) {
    var byFrom = mxIndex()[from];
    return byFrom ? byFrom[to] : null;
  }

  function mxUnits(group) {
    var out = [];
    if (typeof UNITS === 'undefined') {
      return out;
    }
    Object.keys(UNITS).forEach(function (id) {
      if (UNITS[id].d === group) { out.push(id); }
    });
    return out.sort();
  }

  // Building the table
  function buildMatrix(host) {
    if (host.dataset.built) {
      return;
    }
    var group = host.dataset.group;
    var units = mxUnits(group);
    if (!units.length) {
      return;
    }
    host.dataset.built = '1';

    var out = '<table class="matrix">'
      + '<caption class="vh">' + escText(group)
      + ' coverage: each row converts into each column</caption>'
      + '<thead><tr><th class="corner" scope="col"></th>';
    units.forEach(function (id) {
      out += '<th scope="col">' + sup(escText(id)) + '</th>';
    });
    out += '</tr></thead><tbody>';

    units.forEach(function (from) {
      out += '<tr><th scope="row">' + sup(escText(from)) + '</th>';
      units.forEach(function (to) {
        if (from === to) {
          out += '<td class="self"></td>';
          return;
        }
        var row = mxRow(from, to);
        var state = row ? row.s : 'missing';
        out += '<td class="' + state + '" title="' + escText(raised(from)) + ' → '
             + escText(raised(to)) + ': ' + state + '" data-from="' + escText(from)
             + '" data-to="' + escText(to) + '">'
             + (row && row.h !== null && row.h !== undefined
                ? '<span class="lab">' + row.h + '</span>' : '')
             + '</td>';
      });
      out += '</tr>';
    });
    host.innerHTML = out + '</tbody></table>';
  }

  function watchMatrices() {
    var hosts = document.querySelectorAll('.card .mx');
    if (!hosts.length) {
      return;
    }
    if (!window.IntersectionObserver) {
      hosts.forEach(buildMatrix);
      return;
    }
    var watcher = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          buildMatrix(entry.target);
          watcher.unobserve(entry.target);
          if (typeof refitThumbs === 'function') { refitThumbs(); }
        }
      });
    }, {rootMargin: '250px'});
    hosts.forEach(function (host) { watcher.observe(host); });
  }

  // what you see on the right panel
  function mxUnitSpan(id) {
    return '<span class="u">' + sup(escText(id)) + '</span>';
  }

  function mxChip(state) {
    var label = state === 'passed' ? 'passed'
              : state === 'failed' ? 'failed' : 'not tested';
    var cls = state === 'passed' ? 'passed' : state === 'failed' ? 'failed' : 'untested';
    return '<span class="chip ' + cls + '">' + label + '</span>';
  }

  function mxEquation(e) {
    if (!e) {
      return '';
    }
    if (e[0] === '!') {
      return '<span class="warn">' + escText(e[1]) + '</span>';
    }
    var out = '';
    if (e[0]) { out += '<span class="op">×</span>' + escText(e[0]); }
    if (e[1]) {
      out += '<span class="op">' + escText(e[1].charAt(0)) + '</span>'
           + escText(e[1].slice(1));
    }
    return out;
  }

  function mxTests(row) {
    var tests = row.x && row.x.q;
    if (!tests || !tests.length) {
      return '<div class="fx-tests"><div class="lbl">no test covers this pair</div></div>';
    }
    var rows = tests.map(function (t) {
      if (!t.length) {
        return '<div class="tc bad"><div class="tc-top">could not evaluate</div></div>';
      }
      return '<div class="tc ' + (t[6] ? 'ok' : 'bad') + '">'
        + '<div class="tc-top">' + escText(t[0]) + ' ' + mxUnitSpan(row.f)
        + '<span class="arrow">→</span>' + escText(t[1]) + ' ' + mxUnitSpan(row.t)
        + '<span class="tc-mark">' + (t[6] ? '✓ passed' : '✗ failed') + '</span></div>'
        + '<div class="tc-sub"><span>expected <b>' + escText(t[2]) + '</b></span>'
        + '<span>error <b>' + escText(t[3]) + '</b>' + escText(t[4]) + '</span>'
        + '<span>tolerance ±' + escText(t[5]) + '</span></div></div>';
    }).join('');
    return '<div class="fx-tests"><div class="lbl">' + tests.length + ' test case'
         + (tests.length === 1 ? '' : 's') + '</div>' + rows + '</div>';
  }

  function detailHtml(from, to) {
    var row = mxRow(from, to);
    if (!row || !row.x) {
      return '';
    }
    var chain = row.x.c
      ? '<div class="fx-where"><span class="kw">via</span>'
        + row.x.c.map(mxUnitSpan).join('<span class="arrow">→</span>')
        + '<span class="hopcount">' + row.h + (row.h === 1 ? ' hop' : ' hops')
        + '</span></div>'
      : '';

    return '<div class="fx">'
      + '<div class="fx-head">' + mxUnitSpan(row.f) + '<span class="arrow">→</span>'
      + mxUnitSpan(row.t) + mxChip(row.s) + '</div>'
      + (row.fn && row.tn
         ? '<div class="fx-names">' + escText(row.fn + ' to ' + row.tn) + '</div>' : '')
      + '<div class="fx-eq">' + mxUnitSpan(row.t) + '<span class="eq">=</span>'
      + mxUnitSpan(row.f) + mxEquation(row.x.e) + '</div>'
      + chain + mxTests(row) + '</div>';
  }

  // Arrow-key navigation
  function wireGridKeys(table) {
    var grid = Array.prototype.map.call(table.querySelectorAll('tbody tr'), function (tr) {
      return Array.prototype.slice.call(tr.querySelectorAll('td'));
    });
    if (!grid.length) {
      return;
    }

    // Every cell stays out of the tab order; clicking one is what starts the
    // arrows, and focus() works regardless of tabIndex.
    var here = {r: 0, c: 0};
    grid.forEach(function (line, r) {
      line.forEach(function (cell, c) {
        cell.tabIndex = -1;
        if (here.r === 0 && here.c === 0 && !cell.classList.contains('self')) {
          here = {r: r, c: c};
        }
      });
    });

    function moveTo(r, c) {
      if (!grid[r] || !grid[r][c]) {
        return;
      }
      here = {r: r, c: c};
      grid[r][c].focus();
    }

    function locate(cell) {
      for (var r = 0; r < grid.length; r++) {
        var c = grid[r].indexOf(cell);
        if (c >= 0) { return {r: r, c: c}; }
      }
      return null;
    }

    table.addEventListener('mousedown', function (event) {
      var cell = event.target.closest('td');
      var at = cell && locate(cell);
      if (at) { moveTo(at.r, at.c); }
    });

    table.addEventListener('keydown', function (event) {
      var cell = event.target.closest('td') || grid[here.r][here.c];
      var at = locate(cell);
      if (!at) {
        return;
      }
      var moves = {ArrowUp: [at.r - 1, at.c], ArrowDown: [at.r + 1, at.c],
                   ArrowLeft: [at.r, at.c - 1], ArrowRight: [at.r, at.c + 1],
                   Home: [at.r, 0], End: [at.r, grid[at.r].length - 1],
                   PageUp: [0, at.c], PageDown: [grid.length - 1, at.c]};
      if (Object.prototype.hasOwnProperty.call(moves, event.key)) {
        event.preventDefault();
        moveTo(moves[event.key][0], moves[event.key][1]);
        return;
      }
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        cell.click();
      }
    });

    table.addEventListener('focusin', function (event) {
      var cell = event.target.closest('td[title]');
      if (cell) {
        cell.dispatchEvent(new MouseEvent('mouseover', {bubbles: true}));
      }
    });
  }

