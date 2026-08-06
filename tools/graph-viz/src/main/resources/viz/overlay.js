  // this file handles the individual matrix view

  var overlay = document.getElementById('overlay');
  var stage = document.getElementById('ostage');
  var otitle = document.getElementById('otitle');
  var otally = document.getElementById('otally');
  var odetail = document.getElementById('odetail');

  var HINT = '<div class="empty">Hover a cell to preview its conversion. '
           + '<b>Click</b> to pin it, click the same cell again to release it.</div>';

  /* Raises an exponent in already-escaped markup: m3 reads as m with a raised
     3. Only text between tags is touched, so a <mark> the search inserted is
     left intact. Same rule as Labels.java: digits count as a power only when
     they directly follow a letter. */
  function sup(escaped) {
    return String(escaped).replace(/(^|>)([^<]+)/g, function (all, before, text) {
      return before + text.replace(/([A-Za-z])(\d+)/g, '$1<sup>$2</sup>');
    });
  }

  /* The same rule for somewhere a tag cannot go: a value that will be escaped,
     or text handed to a canvas. Mirrors Labels.plain in the Java side. */
  var RAISED = '\u2070\u00b9\u00b2\u00b3\u2074\u2075\u2076\u2077\u2078\u2079';

  function raised(text) {
    return String(text).replace(/([A-Za-z])(\d+)/g, function (all, letter, digits) {
      var out = '';
      for (var i = 0; i < digits.length; i++) {
        out += RAISED.charAt(+digits.charAt(i));
      }
      return letter + out;
    });
  }

  // Every rendered conversion points the same way with the same glyph.
  var ARROW = '<span class="arrow">→</span>';

  var MAX_ROUTES = 60;
  var MAX_HOPS = 7;

  var pinned = null;

  if (!overlay || !stage) {
    return;
  }
  
  var trapReturn = null;

  function trapFocus(layer) {
    trapReturn = document.activeElement;
    layer.addEventListener('keydown', onTrapKey);
    var first = layer.querySelector('button,[href],input,select,[tabindex]:not([tabindex="-1"])');
    if (first) { first.focus(); }
  }

  function onTrapKey(event) {
    if (event.key !== 'Tab') {
      return;
    }
    var layer = event.currentTarget;
    var focusable = Array.prototype.filter.call(
      layer.querySelectorAll('button,[href],input,select,[tabindex]:not([tabindex="-1"])'),
      function (el) { return el.offsetParent !== null; });
    if (!focusable.length) {
      return;
    }
    var first = focusable[0];
    var last = focusable[focusable.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  }

  function releaseFocus(layer) {
    layer.removeEventListener('keydown', onTrapKey);
    if (trapReturn && trapReturn.focus) { trapReturn.focus(); }
    trapReturn = null;
  }

  function raise(layer) {
    layer.classList.add('open');
    layer.setAttribute('aria-hidden', 'false');
    if (layer === overlay && typeof pnOnOpen === 'function') { pnOnOpen(); }
    trapFocus(layer);
    requestAnimationFrame(function () {
      requestAnimationFrame(function () {
        layer.classList.add('in');
      });
    });
    document.body.style.overflow = 'hidden';
  }

  function lower(layer, after) {
    layer.setAttribute('aria-hidden', 'true');
    releaseFocus(layer);
    layer.classList.remove('in');
    var done = false;
    function finish() {
      if (done) {
        return;
      }
      done = true;
      layer.classList.remove('open');
      document.body.style.overflow = '';
      if (after) {
        after();
      }
    }
    layer.addEventListener('transitionend', function handler(event) {
      if (event.target === layer) {
        layer.removeEventListener('transitionend', handler);
        finish();
      }
    });
    setTimeout(finish, 400);          // safety net if the transition never fires
  }

  var adjacency = null;

  // Find every route for the matrix
  function graph() {
    if (adjacency) {
      return adjacency;
    }
    adjacency = {};
    if (typeof SEED === 'undefined') {
      return adjacency;
    }
    SEED.forEach(function (edge) {
      link(edge[0], edge[1], edge[2], edge[3]);
      if (edge[2] !== 0) {
        link(edge[1], edge[0], 1 / edge[2], -edge[3] / edge[2]);
      }
    });
    return adjacency;
  }

  function link(from, to, m, b) {
    (adjacency[from] = adjacency[from] || []).push({to: to, m: m, b: b});
  }

  // Finding paths from every node
  function routes(from, to) {
    var edges = graph();
    var found = [];
    var onPath = {};

    function walk(node, path, m, b) {
      if (found.length >= MAX_ROUTES || path.length > MAX_HOPS + 1) {
        return;
      }
      if (node === to && path.length > 1) {
        found.push({path: path.slice(), m: m, b: b});
        return;
      }
      (edges[node] || []).forEach(function (edge) {
        if (onPath[edge.to]) {
          return;
        }
        onPath[edge.to] = true;
        path.push(edge.to);
        walk(edge.to, path, m * edge.m, edge.m * b + edge.b);
        path.pop();
        onPath[edge.to] = false;
      });
    }

    onPath[from] = true;
    walk(from, [from], 1, 0);
    found.sort(function (a, b2) {
      return a.path.length - b2.path.length;
    });
    return found;
  }

  // turn the routes into a panel
  function num(value) {
    if (!isFinite(value)) {
      return String(value);
    }
    if (value === Math.round(value) && Math.abs(value) < 1e15) {
      return String(value);
    }
    return Number(value.toPrecision(12)).toString();
  }

  function renderRoutes(container, from, to, chosenHops) {
    var found = routes(from, to);
    if (!found.length) {
      container.innerHTML = '<div class="more">No route found in the direct '
                          + 'conversions.</div>';
      return;
    }

    var reference = found[0].m;
    var html = '';

    found.forEach(function (route, index) {
      var hops = route.path.length - 1;
      var chosen = hops === chosenHops;
      var off = reference !== 0 ? Math.abs(route.m - reference) / Math.abs(reference) : 0;
      var disagrees = off > 1e-9;

      html += '<div class="rt' + (chosen ? ' chosen' : '') + '" style="--i:' + index + '">'
            + '<span class="hops">' + hops + (hops === 1 ? ' hop' : ' hops') + '</span>'
            + '<span class="via">'
            + route.path.map(function (id) { return sup(escText(id)); }).join(ARROW)
            + '</span>'
            + '<span class="fac' + (disagrees ? ' disagree' : '') + '">× ' + num(route.m)
            + (route.b !== 0 ? (route.b > 0 ? ' + ' : ' − ') + num(Math.abs(route.b)) : '')
            + (disagrees ? '   — disagrees with the shortest route' : '')
            + '</span></div>';
    });

    if (found.length >= MAX_ROUTES) {
      html += '<div class="more">Showing the first ' + MAX_ROUTES + ' routes.</div>';
    }
    container.innerHTML = html;
  }

  // Right side panel
  function detailFor(cell) {
    var html = detailHtml(cell.dataset.from, cell.dataset.to)
            || '<div class="empty">' + cell.getAttribute('title') + '</div>';

    if (cell.dataset.from && cell.dataset.to && typeof SEED !== 'undefined') {
      html += '<div class="fx-paths">'
            + '<button type="button" class="pathbtn">Show every route</button>'
            + '<div class="routes"></div></div>';
    }
    return html;
  }

  function panelShow(html, locked) {
    odetail.innerHTML = html;
    odetail.classList.toggle('locked', !!locked);
  }

  function show(html) {
    panelShow(html, pinned !== null);
  }

  odetail.addEventListener('click', function (event) {
    if (!event.target.classList.contains('pathbtn') || !pinned) {
      return;
    }
    var routeList = odetail.querySelector('.routes');

    if (routeList.innerHTML !== '') {
      routeList.innerHTML = '';
      event.target.textContent = 'Show every route';
      return;
    }

    event.target.textContent = 'Hide routes';
    renderRoutes(routeList, pinned.dataset.from, pinned.dataset.to,
                 parseInt(pinned.querySelector('.lab')
                          ? pinned.querySelector('.lab').textContent : '0', 10));
  });

  // Make matrix fill the screen
  function fit() {
    var table = stage.querySelector('table.matrix');
    if (!table) {
      return;
    }
    var head = table.querySelector('thead tr');
    var columns = head ? head.children.length - 1 : 0;
    var rows = table.querySelectorAll('tbody tr').length;
    if (!columns || !rows) {
      return;
    }

    var box = stage.getBoundingClientRect();

    var forRowLabels = 170;
    var forColumnLabels = 80;
    var padding = 48;

    var perColumn = (box.width - forRowLabels - padding) / columns;
    var perRow = (box.height - forColumnLabels - padding) / rows;

    var size = Math.floor(Math.min(perColumn, perRow)) - 3;

    size = Math.max(20, Math.min(size, 120));

    table.style.setProperty('--cell', size + 'px');
  }

  var oaxis = document.getElementById('oaxis');
  var lastCard = null;

  // Actually open the selected card
  function open(card, preselect) {
    lastCard = card;
    otitle.textContent = card.querySelector('h2').textContent;
    pinned = null;

    var host = card.querySelector('.cy');
    if (host) {
      overlay.classList.add('seedmode');
      var badge = card.querySelector('.badge');
      otally.innerHTML = badge ? badge.outerHTML : '';
      oaxis.textContent = 'drag units · scroll to zoom · click an edge for its formula';
      stage.innerHTML = '<div id="ocy"></div>';
      raise(overlay);
      requestAnimationFrame(function () {
        seedApi = hydrateSeed(document.getElementById('ocy'), host.dataset.group);
        if (preselect) { seedApi.pick(preselect.from, preselect.to); }
      });
      return;
    }

    overlay.classList.remove('seedmode');
    oaxis.textContent = 'row → column · arrow keys move · Enter pins';
    otally.innerHTML = card.querySelector('.tally').innerHTML;
    var mount = card.querySelector('.mx');
    if (mount) { buildMatrix(mount); }
    stage.innerHTML = card.querySelector('table').outerHTML;

    // The card's copy is scaled down to fit its thumbnail, and that inline
    // transform rides along in the clone. The enlarged view sizes itself with
    // fit() instead, so the scale has to come back off here.
    stage.querySelector('table').style.transform = '';
    wireGridKeys(stage.querySelector('table'));

    var corner = stage.querySelector('th.corner');
    if (corner) {
      corner.textContent = 'from ↓';
    }

    show(HINT);
    raise(overlay);
    requestAnimationFrame(fit);
  }

  function close() {
    pinned = null;
    if (seedApi) {
      seedApi.destroy();
      seedApi = null;
    }
    lower(overlay, function () {
      stage.innerHTML = '';
      overlay.classList.remove('seedmode');
    });
  }

  document.querySelectorAll('.card').forEach(function (card) {
    card.tabIndex = 0;
    card.addEventListener('click', function () {
      open(card);
    });
    card.addEventListener('keydown', function (event) {
      if (event.key === 'Enter' || event.key === ' ') {
        event.preventDefault();
        open(card);
      }
    });
  });

  document.getElementById('oclose').addEventListener('click', close);

  /* A matrix preview is scaled down to fit its window, so a thirty unit
     dimension and a three unit one produce the same size card and neither
     needs a scrollbar. Layout is untouched by a transform, so the table stays
     centered and only its painted size changes. */
  /*
   * Scale each matrix down until it fits its card.
   *
   * Everything here is a guard against measuring at the wrong moment. A table
   * inside a hidden tab measures zero, which used to divide to Infinity and
   * settle on scale(1) - the table then overflowed and looked cut off. A table
   * measured before its font has loaded is narrower than it will be, so the
   * scale comes out too generous and it overflows once the real font arrives.
   * Both leave a thumbnail clipped, and both are intermittent, which is why it
   * worked sometimes.
   */
  function fitThumbs() {
    document.querySelectorAll('.thumb.scroll').forEach(function (box) {
      var table = box.querySelector('table');
      if (!table) {
        return;
      }
      table.style.transform = '';

      var width = table.offsetWidth;
      var height = table.offsetHeight;
      if (!width || !height || !box.clientWidth || !box.clientHeight) {
        table.dataset.unfitted = '1';        // measured while hidden; try again later
        return;
      }
      delete table.dataset.unfitted;

      var room = Math.min(box.clientWidth / width, box.clientHeight / height, 1);
      table.style.transform = 'scale(' + room.toFixed(3) + ')';
    });
  }

  /* Anything that changes how wide the text is, or makes a hidden card visible,
     invalidates a measurement taken earlier. */
  function refitThumbs() {
    requestAnimationFrame(fitThumbs);
  }

  watchMatrices();
  fitThumbs();
  window.addEventListener('resize', fitThumbs);

  // The first measurement can land before the monospace font does, and every
  // column would then be measured too narrow.
  if (document.fonts && document.fonts.ready) {
    document.fonts.ready.then(refitThumbs);
  }
  // Switching tabs is the moment a card that measured zero becomes measurable.
  document.querySelectorAll('.tab').forEach(function (tab) {
    tab.addEventListener('click', refitThumbs);
  });
