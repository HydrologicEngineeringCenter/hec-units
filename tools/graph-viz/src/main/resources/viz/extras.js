
  // Grabbing data and saving some small things
  var exUnits = typeof UNITS !== 'undefined' ? UNITS : {};
  var exHasData = typeof INDEX !== 'undefined';

  function exStore(key, value) {
    try {
      if (value === undefined) { return localStorage.getItem(key); }
      localStorage.setItem(key, value);
    } catch (e) { }
    return null;
  }

  // suggest which untested conversions would be the most helpful to add
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

  // Measure how big gap is between inconsistencies between routes
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

  // Building the summary panel
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

  // copy button and pop-up
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

  // Remember some information across refreshes
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

  // Keyboard shortcuts
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
