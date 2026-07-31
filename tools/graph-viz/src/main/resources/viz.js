/*
 * Click-to-enlarge for the coverage matrices.
 *
 * The trick here is that nothing is re-rendered: the overlay reuses the exact
 * table already sitting in the card, just cloned into a container where it can
 * be drawn much larger. That means no graph data has to be serialised into the
 * page and no matrix-building logic exists twice. Each cell already carries its
 * own description in a data attribute, so selecting one needs no lookup either.
 *
 * The one thing that cannot be precomputed is "every route between these two
 * units" - there are far too many to bake into the page - so the seed
 * conversions are embedded instead (see SEED) and routes are walked on demand.
 */
(function () {
  var overlay = document.getElementById('overlay');
  var stage = document.getElementById('ostage');
  var otitle = document.getElementById('otitle');
  var otally = document.getElementById('otally');
  var odetail = document.getElementById('odetail');

  var HINT = '<div class="empty">Hover a cell to preview its conversion. '
           + '<b>Click</b> to pin it, click the same cell again to release it.</div>';

  var MAX_ROUTES = 60;
  var MAX_HOPS = 7;

  /*
   * The pinned cell, or null in "free mode".
   *
   * These are two genuinely different modes rather than a preference:
   *   free   - the panel follows the cursor
   *   pinned - the panel holds one conversion and ignores the cursor entirely
   *
   * The second is what makes the equations usable. If hovering kept overwriting
   * the panel there would be no way to read a conversion while moving the mouse
   * to compare it against another cell.
   */
  var pinned = null;

  if (!overlay || !stage) {
    return;
  }

  /* ---------------------------------------------------------- modal layers */

  /*
   * Two classes, two jobs: .open mounts the layer (display), .in - added one
   * frame later - runs the fade/rise transition. display:none cannot animate,
   * so the swap has to happen across a frame boundary.
   */
  function raise(layer) {
    layer.classList.add('open');
    requestAnimationFrame(function () {
      requestAnimationFrame(function () {
        layer.classList.add('in');
      });
    });
    document.body.style.overflow = 'hidden';
  }

  function lower(layer, after) {
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

  /* ---------------------------------------------------------------- routes */

  // Adjacency built from the direct conversions, both ways round. Only the
  // forward direction is embedded; inverting y = m*x + b gives the other.
  var adjacency = null;

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

  /*
   * Every simple route from one unit to another.
   *
   * Depth-first, refusing to revisit a unit already on the path - a route that
   * doubles back cannot be shorter and would make the search unbounded. The two
   * caps keep a densely connected dimension from producing thousands of routes.
   *
   * Factors compose as you go: applying y = m2*x + b2 after y = m1*x + b1 gives
   * m1*m2 for the scale and m2*b1 + b2 for the offset.
   */
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

    // The shortest route's factor is the reference. Any route that disagrees is
    // worth seeing: two routes between the same units must give the same answer,
    // so a mismatch means one of the conversions along the way is wrong.
    var reference = found[0].m;
    var html = '';

    found.forEach(function (route, index) {
      var hops = route.path.length - 1;
      var chosen = hops === chosenHops;
      var off = reference !== 0 ? Math.abs(route.m - reference) / Math.abs(reference) : 0;
      var disagrees = off > 1e-9;

      html += '<div class="rt' + (chosen ? ' chosen' : '') + '" style="--i:' + index + '">'
            + '<span class="hops">' + hops + (hops === 1 ? ' hop' : ' hops') + '</span>'
            + '<span class="via">' + route.path.join(' → ') + '</span>'
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

  /* ----------------------------------------------------------------- panel */

  function detailFor(cell) {
    var html = cell.dataset.detail
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
    // Drives the "pinned" badge in CSS, so the panel says why it is not
    // responding to the cursor.
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

    // Toggle rather than a one-way reveal: the route list can be long, and once
    // you have read it you want the equation back without re-selecting the cell.
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

  /* --------------------------------------------------------------- overlay */

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
    // Row labels sit to the left of every row and the header row sits above
    // every column, so neither is a square cell and both come off the top.
    var forRowLabels = 170;
    var forColumnLabels = 80;
    var padding = 48;

    var perColumn = (box.width - forRowLabels - padding) / columns;
    var perRow = (box.height - forColumnLabels - padding) / rows;

    // Minus the 2px border-spacing that sits between every pair of cells.
    var size = Math.floor(Math.min(perColumn, perRow)) - 3;

    // Floor keeps a wide matrix legible on a small window (it scrolls instead of
    // shrinking to nothing). Ceiling stops a 2x2 becoming two absurd slabs.
    size = Math.max(20, Math.min(size, 120));

    table.style.setProperty('--cell', size + 'px');
  }

  var oaxis = document.getElementById('oaxis');
  var lastCard = null;

  function open(card) {
    lastCard = card;
    otitle.textContent = card.querySelector('h2').textContent;
    pinned = null;

    var svg = card.querySelector('svg.nl');
    if (svg) {
      // Seed mode: the drawing is cloned, then hydrated into a live simulation.
      overlay.classList.add('seedmode');
      var badge = card.querySelector('.badge');
      otally.innerHTML = badge ? badge.outerHTML : '';
      oaxis.textContent = 'drag units · scroll to zoom · click an edge for its formula';
      stage.innerHTML = svg.outerHTML;
      raise(overlay);
      requestAnimationFrame(function () {
        seedApi = hydrateSeed(stage.querySelector('svg.nl'));
      });
      return;
    }

    overlay.classList.remove('seedmode');
    oaxis.textContent = 'row → column';
    otally.innerHTML = card.querySelector('.tally').innerHTML;
    stage.innerHTML = card.querySelector('table').outerHTML;

    var corner = stage.querySelector('th.corner');
    if (corner) {
      corner.textContent = 'from ↓';
    }

    show(HINT);
    raise(overlay);
    // Sizing has to happen after the overlay is visible, or the stage still
    // measures zero. A frame later is guaranteed to be after layout.
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
    // tabIndex plus the keydown handler make a div behave like a button, so the
    // enlarged view is reachable without a mouse.
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

  /* ---------------------------------------------------------- table sorting */

  /*
   * Click a header to sort by it: first click descending, second ascending,
   * third back to the order the page was generated in.
   *
   * The original row order is captured once up front, which is what makes the
   * third click a genuine reset rather than another sort.
   */
  function makeSortable(table) {
    var body = table.tBodies[0];
    var headers = table.querySelectorAll('thead th');
    if (!body || !headers.length) {
      return;
    }
    var original = Array.prototype.slice.call(body.rows);
    var sortedBy = -1;
    var direction = 0;                      // 0 none, 1 descending, 2 ascending

    function valueOf(row, column) {
      var cell = row.cells[column];
      if (!cell) {
        return '';
      }
      var text = cell.textContent.trim();
      // Strip units and symbols so "22.73%" and "2 hops" still compare as numbers.
      var number = parseFloat(text.replace(/[^0-9.eE+-]/g, ''));
      return isNaN(number) ? text.toLowerCase() : number;
    }

    function apply() {
      headers.forEach(function (header) {
        header.classList.remove('asc', 'desc');
      });

      var rows = original.slice();
      if (direction !== 0) {
        rows.sort(function (a, b) {
          var x = valueOf(a, sortedBy);
          var y = valueOf(b, sortedBy);
          var order = (typeof x === 'number' && typeof y === 'number')
                    ? x - y
                    : String(x).localeCompare(String(y));
          return direction === 1 ? -order : order;
        });
        headers[sortedBy].classList.add(direction === 1 ? 'desc' : 'asc');
      }
      rows.forEach(function (row) {
        body.appendChild(row);
      });
    }

    headers.forEach(function (header, column) {
      if (!header.textContent.trim()) {
        return;                             // spacer column, nothing to sort by
      }
      header.classList.add('sortable-col');
      header.addEventListener('click', function () {
        if (sortedBy !== column) {
          sortedBy = column;
          direction = 1;
        } else {
          direction = (direction + 1) % 3;
          if (direction === 0) {
            sortedBy = -1;
          }
        }
        apply();
      });
    });
  }

  document.querySelectorAll('table.sortable').forEach(makeSortable);

  /* ------------------------------------------------------------ seed graphs
     The enlarged copy of a seed drawing becomes live: a force simulation lays
     out cyclic graphs (trees keep their computed layout), nodes drag, the
     canvas pans and zooms, an edge shows its formula in the panel, and two
     clicked units list every route between them. */

  var seedApi = null;

  var SEED_HINT = '<div class="empty"><b>Click two units</b> to list every route between '
                + 'them. <b>Click an edge</b> for its formula. Drag units to untangle, '
                + 'drag the background to pan, scroll to zoom.</div>';

  var GROUP_COLORS = ['#38bdf8', '#f87171', '#fbbf24', '#a78bfa', '#34d399', '#fb923c'];
  var MAX_PATHS = 4000;
  var MAX_HOPS = 14;
  var SVG_NS = 'http://www.w3.org/2000/svg';

  function hydrateSeed(svg) {
    var W = stage.clientWidth;
    var H = stage.clientHeight;
    svg.setAttribute('viewBox', '0 0 ' + W + ' ' + H);
    var root = svg.querySelector('.nl-root');
    var isTree = svg.dataset.tree === '1';

    var groups = Array.prototype.slice.call(svg.querySelectorAll('.nl-node'));
    var lines = Array.prototype.slice.call(svg.querySelectorAll('.nl-edge'));

    var PX = 110;
    var PY = 80;
    var N = groups.map(function (el, i) {
      var node = {el: el, id: el.dataset.id, name: el.dataset.name,
                  vx: 0, vy: 0, fixed: false};
      if (isTree) {
        // Trees keep their computed shape through a home spring rather than
        // being frozen, so they wobble and spring back like the others.
        node.hx = PX + parseFloat(el.dataset.nx) * (W - 2 * PX);
        node.hy = PY + parseFloat(el.dataset.ny) * (H - 2 * PY);
        node.x = node.hx;
        node.y = node.hy;
      } else {
        var angle = 2 * Math.PI * i / groups.length - Math.PI / 2;
        node.x = W / 2 + Math.cos(angle) * Math.min(W, H) * 0.32;
        node.y = H / 2 + Math.sin(angle) * Math.min(W, H) * 0.32;
      }
      node.ix = node.x;
      node.iy = node.y;
      return node;
    });
    var indexOf = {};
    N.forEach(function (node, i) { indexOf[node.id] = i; });

    var E = lines.map(function (el) {
      var hit = document.createElementNS(SVG_NS, 'path');
      hit.setAttribute('class', 'nl-hit');
      el.parentNode.insertBefore(hit, el.nextSibling);
      return {el: el, hit: hit,
              s: indexOf[el.dataset.a], t: indexOf[el.dataset.b],
              bow: parseFloat(el.dataset.bow) || 0,
              m: el.dataset.m ? parseFloat(el.dataset.m) : null,
              b: el.dataset.k ? parseFloat(el.dataset.k) : null,
              detail: el.dataset.detail || ''};
    });
    var ADJ = N.map(function () { return []; });
    E.forEach(function (edge, i) {
      ADJ[edge.s].push({ei: i, other: edge.t});
      ADJ[edge.t].push({ei: i, other: edge.s});
    });

    var pickA = null;
    var pickB = null;
    var selEdge = null;
    var allowRevisit = false;
    var paths = [];

    function busy() { return selEdge !== null || pickA !== null; }

    /* ----------------------------------------------------------- drawing */

    function arcPath(a, b, bow, lo, hi) {
      if (!bow) {
        return 'M ' + a.x.toFixed(1) + ' ' + a.y.toFixed(1)
             + ' L ' + b.x.toFixed(1) + ' ' + b.y.toFixed(1);
      }
      // Perpendicular from a stable endpoint ordering, so a reversed pair
      // cannot cancel its opposite bow and collapse onto itself.
      var rx = hi.x - lo.x;
      var ry = hi.y - lo.y;
      var d = Math.hypot(rx, ry) || 1;
      var mx = (a.x + b.x) / 2 + (-ry / d) * bow * 2;
      var my = (a.y + b.y) / 2 + (rx / d) * bow * 2;
      return 'M ' + a.x.toFixed(1) + ' ' + a.y.toFixed(1)
           + ' Q ' + mx.toFixed(1) + ' ' + my.toFixed(1)
           + ' ' + b.x.toFixed(1) + ' ' + b.y.toFixed(1);
    }

    function draw() {
      E.forEach(function (edge) {
        var a = N[edge.s];
        var b = N[edge.t];
        var lo = N[Math.min(edge.s, edge.t)];
        var hi = N[Math.max(edge.s, edge.t)];
        var d = arcPath(a, b, edge.bow * 30, lo, hi);
        edge.el.setAttribute('d', d);
        edge.hit.setAttribute('d', d);
      });
      N.forEach(function (node) {
        node.el.setAttribute('transform', 'translate(' + node.x + ',' + node.y + ')');
      });
    }

    /* ---------------------------------------- force layout with annealing */

    var sim = null;
    var alpha = 1;
    var LEN = Math.max(120, Math.min(W, H) / (1.7 + N.length * 0.10));

    function tick() {
      N.forEach(function (n) { n.fx2 = 0; n.fy2 = 0; });
      for (var a = 0; a < N.length; a++) {
        for (var b = a + 1; b < N.length; b++) {
          var dx = N[b].x - N[a].x;
          var dy = N[b].y - N[a].y;
          var d2 = Math.max(dx * dx + dy * dy, 25);
          var d = Math.sqrt(d2);
          var f = (isTree ? 6000 : 16000) / d2;
          N[a].fx2 -= f * dx / d; N[a].fy2 -= f * dy / d;
          N[b].fx2 += f * dx / d; N[b].fy2 += f * dy / d;
        }
      }
      if (!isTree) {
        E.forEach(function (edge) {
          var na = N[edge.s];
          var nb = N[edge.t];
          var dx = nb.x - na.x;
          var dy = nb.y - na.y;
          var d = Math.hypot(dx, dy) || 1;
          var f = 0.05 * (d - LEN);
          na.fx2 += f * dx / d; na.fy2 += f * dy / d;
          nb.fx2 -= f * dx / d; nb.fy2 -= f * dy / d;
        });
      }
      N.forEach(function (n) {
        if (isTree) {
          n.fx2 += (n.hx - n.x) * 0.08;
          n.fy2 += (n.hy - n.y) * 0.08;
        } else {
          n.fx2 += (W / 2 - n.x) * 0.010;
          n.fy2 += (H / 2 - n.y) * 0.010;
        }
        if (n.fixed) { n.vx = n.vy = 0; return; }
        n.vx = (n.vx + n.fx2) * 0.62;
        n.vy = (n.vy + n.fy2) * 0.62;
        n.x = Math.max(70, Math.min(W - 70, n.x + n.vx * alpha));
        n.y = Math.max(45, Math.min(H - 45, n.y + n.vy * alpha));
      });
    }

    function step() {
      tick();
      draw();
      if (dragNode === null) { alpha *= 0.962; }
      if (alpha < 0.002 && dragNode === null) { sim = null; return; }
      sim = requestAnimationFrame(step);
    }

    function reheat() {
      alpha = Math.max(alpha, 0.22);
      if (!sim) { sim = requestAnimationFrame(step); }
    }

    /* -------------------------------------------------------- highlights */

    function clearMarks() {
      lines.forEach(function (l) { l.classList.remove('dim', 'sel', 'on-route'); });
      groups.forEach(function (g) { g.classList.remove('dim', 'pick-a', 'pick-b'); });
    }

    function neighbourhood(i) {
      clearMarks();
      if (i === null) { return; }
      var near = {};
      near[i] = true;
      E.forEach(function (e) {
        if (e.s === i) { near[e.t] = true; }
        if (e.t === i) { near[e.s] = true; }
      });
      groups.forEach(function (g, j) { g.classList.toggle('dim', !near[j]); });
      lines.forEach(function (l, j) {
        l.classList.toggle('dim', E[j].s !== i && E[j].t !== i);
      });
    }

    function selectEdge(j) {
      if (selEdge === j) {
        reset();                          // clicking the same edge releases it
        return;
      }
      pickA = pickB = null;
      paths = [];
      selEdge = j;
      clearMarks();
      lines.forEach(function (l, k) {
        l.classList.toggle('sel', k === j);
        l.classList.toggle('dim', k !== j);
      });
      groups.forEach(function (g, k) {
        g.classList.toggle('dim', k !== E[j].s && k !== E[j].t);
      });
      syncBadges();
      panelShow(clearButton() + (E[j].detail || '<div class="empty">no formula</div>'), true);
    }

    function reset() {
      selEdge = null;
      pickA = pickB = null;
      paths = [];
      clearMarks();
      svg.classList.remove('picking');
      syncBadges();
      panelShow(SEED_HINT, false);
    }

    /* Shown whenever something is selected, so clearing never means hunting for
       empty canvas or the reset button. */
    function clearButton() {
      return '<button type="button" class="clearpick">Clear selection</button>';
    }

    /* ------------------------------------------------------------ routes */

    /*
     * Toggle semantics: clicking a selected unit clears it, a new one fills the
     * next free slot, and a third when both are taken starts over from it.
     */
    function nodeClicked(i) {
      selEdge = null;
      if (pickA === i) {
        pickA = pickB;                    // promote B so the survivor stays picked
        pickB = null;
      } else if (pickB === i) {
        pickB = null;
      } else if (pickA === null) {
        pickA = i;
      } else if (pickB === null) {
        pickB = i;
      } else {
        pickA = i;
        pickB = null;
      }
      // Interacting means the reader is aiming at things, so let the layout
      // settle rather than drifting under the cursor.
      alpha = Math.min(alpha, 0.12);
      flash(i);
      refreshPicks();
    }

    /*
     * A "1" or "2" above a picked unit.
     *
     * It is appended inside the node's own <g>, which already carries the
     * translate the simulation writes each frame - so the badge follows the
     * node for free, with nothing to update on tick.
     */
    function setBadge(i, order) {
      var el = groups[i];
      var existing = el.querySelector('.pick-badge');
      if (existing) {
        if (existing.dataset.n === String(order)) { return; }   // already right
        existing.remove();
      }
      if (!order) { return; }

      var badge = document.createElementNS(SVG_NS, 'g');
      badge.setAttribute('class', 'pick-badge p' + order);
      badge.dataset.n = String(order);
      var ring = document.createElementNS(SVG_NS, 'circle');
      ring.setAttribute('cx', 0);
      ring.setAttribute('cy', -38);
      ring.setAttribute('r', 13);
      var label = document.createElementNS(SVG_NS, 'text');
      label.setAttribute('y', -33);
      label.textContent = order;
      badge.appendChild(ring);
      badge.appendChild(label);
      el.appendChild(badge);
    }

    function syncBadges() {
      groups.forEach(function (el, j) {
        setBadge(j, j === pickA ? 1 : (j === pickB ? 2 : 0));
      });
    }

    /* One-shot pulse. The class is re-added a frame later so it restarts even
       when the node was already marked. */
    function flash(i) {
      var el = groups[i];
      el.classList.remove('pop');
      requestAnimationFrame(function () { el.classList.add('pop'); });
      setTimeout(function () { el.classList.remove('pop'); }, 430);
    }

    function refreshPicks() {
      clearMarks();
      svg.classList.toggle('picking', pickA !== null && pickB === null);
      if (pickA !== null) { groups[pickA].classList.add('pick-a'); }
      if (pickB !== null) { groups[pickB].classList.add('pick-b'); }
      syncBadges();
      paths = (pickA !== null && pickB !== null) ? findPaths(pickA, pickB) : [];
      drawPanel();
    }

    /* Edge-based walk, so two parallel conversions count as two routes. */
    function findPaths(from, to) {
      var out = [];
      var truncated = false;
      var usedE = {};
      var seenN = {};
      seenN[from] = true;
      (function walk(cur, path) {
        if (out.length >= MAX_PATHS) { truncated = true; return; }
        if (path.length && cur === to) { out.push(path.slice()); return; }
        if (path.length >= MAX_HOPS) { return; }
        ADJ[cur].forEach(function (step2) {
          if (usedE[step2.ei]) { return; }
          if (!allowRevisit && seenN[step2.other] && step2.other !== to) { return; }
          usedE[step2.ei] = true;
          var fresh = !seenN[step2.other];
          if (fresh) { seenN[step2.other] = true; }
          path.push({ei: step2.ei, from: cur, to: step2.other});
          walk(step2.other, path);
          path.pop();
          delete usedE[step2.ei];
          if (fresh) { delete seenN[step2.other]; }
        });
      })(from, []);
      out.truncated = truncated;
      out.sort(function (p, q) { return p.length - q.length; });
      return out;
    }

    /* Compose y = m*x + b along a route; inverted when travelling backwards. */
    function compose(path) {
      var m = 1;
      var b = 0;
      for (var i = 0; i < path.length; i++) {
        var edge = E[path[i].ei];
        if (edge.m === null || isNaN(edge.m)) { return null; }
        var em;
        var eb;
        if (path[i].from === edge.s) { em = edge.m; eb = edge.b; }
        else {
          if (edge.m === 0) { return null; }
          em = 1 / edge.m; eb = -edge.b / edge.m;
        }
        m = em * m;
        b = em * b + eb;
      }
      return {m: m, b: b};
    }

    function factorHtml(result, A, B) {
      if (!result) { return 'not a simple scale + offset - cannot compose numerically'; }
      var out = '<span class="u">' + escText(B) + '</span> = <span class="u">'
              + escText(A) + '</span>';
      if (result.m !== 1) { out += ' × ' + num(result.m); }
      if (result.b !== 0) { out += (result.b >= 0 ? ' + ' : ' − ') + num(Math.abs(result.b)); }
      return out;
    }

    function escText(s) {
      return String(s).replace(/[&<>"]/g, function (c) {
        return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;'}[c];
      });
    }

    /* Abbreviations are terse by necessity; the full names remove the doubt. */
    function namesLine(a, b) {
      var first = N[a].name;
      var second = b === null ? null : N[b].name;
      if (!first) {
        return '';
      }
      return '<div class="fx-names">' + escText(first)
           + (second ? ' to ' + escText(second) : '') + '</div>';
    }

    function drawPanel() {
      if (pickA === null) {
        panelShow(SEED_HINT, false);
        return;
      }
      var A = N[pickA].id;
      if (pickB === null) {
        panelShow(clearButton() + '<h4><span class="pk p1">1</span>' + escText(A)
                + ' → ?</h4>' + namesLine(pickA, null)
                + '<div class="pth-sub">now click the destination unit</div>', true);
        return;
      }
      var B = N[pickB].id;

      // Routes grouped by the numeric result they produce - if there is more
      // than one group, some direct conversion along the way is lossy or wrong.
      var results = paths.map(compose);
      var keyOf = function (r) {
        return r ? r.m.toPrecision(10) + '|' + (r.b === 0 ? 0 : r.b.toPrecision(10)) : 'n/a';
      };
      var order = [];
      results.forEach(function (r) {
        var k = keyOf(r);
        if (order.indexOf(k) < 0) { order.push(k); }
      });

      var head = clearButton()
        + '<h4><span class="pk p1">1</span>' + escText(A)
        + ' → <span class="pk p2">2</span>' + escText(B) + '</h4>'
        + namesLine(pickA, pickB)
        + '<div class="pth-sub">' + paths.length + ' route' + (paths.length === 1 ? '' : 's')
        + (paths.truncated ? ' (capped at ' + MAX_PATHS + ')' : '')
        + (paths.length ? ' · shortest ' + paths[0].length + ' hop'
           + (paths[0].length === 1 ? '' : 's') + ', longest '
           + paths[paths.length - 1].length : '') + '</div>'
        + '<label id="optoggle"><input type="checkbox"' + (allowRevisit ? ' checked' : '')
        + '> include routes that revisit a unit</label>';

      if (!paths.length) {
        panelShow(head + '<div class="empty">No route exists between these two units.</div>', true);
        wireToggle();
        return;
      }

      if (order.length > 1) {
        head += '<div class="pth-warn"><b>These routes disagree.</b> ' + order.length
              + ' different results across ' + paths.length + ' routes - the colour dot marks '
              + 'each group. One or more direct conversions on the odd routes out is lossy '
              + 'or wrong.</div>';
      } else {
        head += '<div class="pth-ok">✓ all ' + paths.length + ' route'
              + (paths.length === 1 ? 's' : 's') + ' agree</div>';
      }

      var rows = paths.map(function (p, i) {
        var r = results[i];
        var dot = order.length > 1
          ? '<span class="grp" style="background:'
            + GROUP_COLORS[order.indexOf(keyOf(r)) % GROUP_COLORS.length] + '"></span>'
          : '';
        var chain = [A].concat(p.map(function (h) { return N[h.to].id; })).join(' → ');
        return '<div class="prow' + (i === 0 ? ' best' : '') + '" data-i="' + i
             + '" style="--i:' + i + '">'
             + '<div class="top"><span class="hops">' + p.length + ' hop'
             + (p.length === 1 ? '' : 's') + '</span>'
             + '<span class="route">' + dot + escText(chain) + '</span></div>'
             + '<div class="res">' + factorHtml(r, A, B) + '</div></div>';
      }).join('');

      panelShow(head + rows, true);
      wireToggle();
      odetail.querySelectorAll('.prow').forEach(function (row) {
        row.addEventListener('mouseenter', function () { showRoute(paths[+row.dataset.i]); });
        row.addEventListener('mouseleave', function () { refreshPicksLight(); });
      });
    }

    function wireToggle() {
      var box = odetail.querySelector('#optoggle input');
      if (box) {
        box.addEventListener('change', function () {
          allowRevisit = box.checked;
          paths = findPaths(pickA, pickB);
          drawPanel();
        });
      }
    }

    function refreshPicksLight() {
      clearMarks();
      if (pickA !== null) { groups[pickA].classList.add('pick-a'); }
      if (pickB !== null) { groups[pickB].classList.add('pick-b'); }
      syncBadges();
    }

    function showRoute(p) {
      clearMarks();
      var onE = {};
      var onN = {};
      onN[pickA] = true;
      p.forEach(function (h) { onE[h.ei] = true; onN[indexOf[N[h.to].id]] = true; });
      lines.forEach(function (l, j) {
        l.classList.toggle('on-route', !!onE[j]);
        l.classList.toggle('dim', !onE[j]);
      });
      groups.forEach(function (g, j) { g.classList.toggle('dim', !onN[j]); });
      refreshPicksLightMarksOnly();
    }

    function refreshPicksLightMarksOnly() {
      if (pickA !== null) { groups[pickA].classList.add('pick-a'); }
      if (pickB !== null) { groups[pickB].classList.add('pick-b'); }
      syncBadges();
    }

    /* -------------------------------------------------- pan, zoom, drag */

    var scale = 1;
    var tx = 0;
    var ty = 0;

    var MIN_SCALE = 0.45;
    var MAX_SCALE = 3.5;

    /*
     * Keep at least a margin of the drawing overlapping the viewport.
     *
     * The bound has to grow with the zoom: a centre-based limit fought the
     * zoom-toward-cursor, because magnifying a corner legitimately throws the
     * centre far outside. Bounding the edges instead means the limit only
     * engages when the drawing is genuinely about to leave the screen.
     */
    function clampView() {
      scale = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale));
      var margin = Math.min(W, H) * 0.3;
      tx = Math.min(W - margin, Math.max(margin - W * scale, tx));
      ty = Math.min(H - margin, Math.max(margin - H * scale, ty));
    }

    function applyView() {
      clampView();
      root.setAttribute('transform', 'translate(' + tx + ',' + ty + ') scale(' + scale + ')');
    }

    var dragNode = null;
    var panning = false;
    var last = null;
    var moved = false;
    var downNode = null;
    var downAt = null;

    function onWheel(event) {
      event.preventDefault();

      // Clamp the scale first and pan by the factor actually applied. Panning
      // by the requested factor and clamping afterwards was the jerk at the
      // limits: the view slid without the zoom happening.
      var delta = event.deltaY * (event.deltaMode === 1 ? 16 : 1);
      var next = Math.min(MAX_SCALE, Math.max(MIN_SCALE, scale * Math.exp(-delta * 0.0015)));
      var k = next / scale;
      if (Math.abs(k - 1) < 1e-6) {
        return;                           // at a limit: change nothing, move nothing
      }

      var r = stage.getBoundingClientRect();
      var mx = event.clientX - r.left;
      var my = event.clientY - r.top;
      tx = mx - (mx - tx) * k;
      ty = my - (my - ty) * k;
      scale = next;
      applyView();
    }

    function onDown(event) {
      if (dragNode === null) {
        panning = true;
        moved = false;
        downNode = null;
        stage.classList.add('panning');
        last = [event.clientX, event.clientY];
      }
    }

    function onMove(event) {
      if (dragNode !== null) {
        if (!moved) {
          if (Math.hypot(event.clientX - downAt[0], event.clientY - downAt[1]) < 5) {
            return;
          }
          moved = true;
          N[dragNode].fixed = true;
        }
        var r = stage.getBoundingClientRect();
        N[dragNode].x = (event.clientX - r.left - tx) / scale;
        N[dragNode].y = (event.clientY - r.top - ty) / scale;
        reheat();
        draw();
      } else if (panning) {
        if (!moved) {
          if (Math.hypot(event.clientX - last[0], event.clientY - last[1]) < 5) {
            return;
          }
          moved = true;
        }
        tx += event.clientX - last[0];
        ty += event.clientY - last[1];
        last = [event.clientX, event.clientY];
        applyView();
      }
    }

    function onUp() {
      if (dragNode !== null) {
        N[dragNode].fixed = false;
        var was = dragNode;
        dragNode = null;
        reheat();
        if (!moved && downNode !== null) { nodeClicked(was); }
      } else if (panning && !moved) {
        reset();                            // a plain background click clears
      }
      panning = false;
      downNode = null;
      stage.classList.remove('panning');
    }

    function onPanelClick(event) {
      if (event.target.closest('.clearpick')) {
        reset();
      }
    }
    odetail.addEventListener('click', onPanelClick);

    stage.addEventListener('wheel', onWheel, {passive: false});
    stage.addEventListener('mousedown', onDown);
    window.addEventListener('mousemove', onMove);
    window.addEventListener('mouseup', onUp);

    groups.forEach(function (el, i) {
      el.addEventListener('mousedown', function (event) {
        event.stopPropagation();
        dragNode = i;
        downNode = i;
        moved = false;
        downAt = [event.clientX, event.clientY];
      });
      el.addEventListener('mouseenter', function () {
        if (busy()) {
          return;
        }
        neighbourhood(i);
        panelShow('<div class="fx"><div class="fx-head"><span class="u">'
          + escText(N[i].id) + '</span></div><div class="fx-names">'
          + escText(N[i].name || '') + '</div></div>', false);
      });
      el.addEventListener('mouseleave', function () {
        if (!busy()) {
          clearMarks();
          panelShow(SEED_HINT, false);
        }
      });
    });

    E.forEach(function (edge, j) {
      edge.hit.addEventListener('mouseenter', function () {
        edge.el.classList.add('hot');
        if (!busy()) { panelShow(edge.detail || SEED_HINT, false); }
      });
      edge.hit.addEventListener('mouseleave', function () {
        edge.el.classList.remove('hot');
        if (!busy()) { panelShow(SEED_HINT, false); }
      });
      edge.hit.addEventListener('click', function (event) {
        event.stopPropagation();
        selectEdge(j);
      });
      edge.hit.addEventListener('mousedown', function (event) {
        event.stopPropagation();
      });
    });

    var resetButton = document.getElementById('oreset');
    if (resetButton) {
      resetButton.onclick = function () {
        N.forEach(function (n) {
          n.x = n.ix; n.y = n.iy; n.vx = n.vy = 0; n.fixed = false;
        });
        scale = 1; tx = 0; ty = 0;
        applyView();
        reset();
        draw();
        alpha = isTree ? 0.5 : 1;
        if (!sim) { sim = requestAnimationFrame(step); }
      };
    }

    panelShow(SEED_HINT, false);
    applyView();
    draw();
    alpha = isTree ? 0.5 : 1;
    sim = requestAnimationFrame(step);

    return {
      busy: busy,
      reset: reset,
      destroy: function () {
        if (sim) { cancelAnimationFrame(sim); sim = null; }
        if (resetButton) { resetButton.onclick = null; }
        odetail.removeEventListener('click', onPanelClick);
        stage.removeEventListener('wheel', onWheel);
        stage.removeEventListener('mousedown', onDown);
        window.removeEventListener('mousemove', onMove);
        window.removeEventListener('mouseup', onUp);
        stage.classList.remove('panning');
      }
    };
  }

  /* ------------------------------------------------------------------ tabs */

  var tabs = document.querySelectorAll('.tab');
  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      tabs.forEach(function (t) { t.classList.toggle('active', t === tab); });
      document.querySelectorAll('.tabpane').forEach(function (pane) {
        pane.classList.toggle('active', pane.id === tab.dataset.pane);
      });
    });
  });

  /* --------------------------------------------------------------- summary */

  // The summary is rendered into the page up front rather than built here: it is
  // static, so there is nothing to compute at open time and the markup stays in
  // the same place as everything else.
  var summary = document.getElementById('summary');
  var openSummary = document.getElementById('sumopen');

  function showSummary(show) {
    if (!summary) {
      return;
    }
    if (show) {
      raise(summary);
    } else {
      lower(summary);
    }
  }

  if (openSummary && summary) {
    openSummary.addEventListener('click', function () {
      showSummary(true);
    });
    document.getElementById('sclose').addEventListener('click', function () {
      showSummary(false);
    });
    summary.addEventListener('click', function (event) {
      if (event.target === summary) {
        showSummary(false);
      }
    });
  }

  // Clicking the backdrop closes; clicking anything inside it must not.
  overlay.addEventListener('click', function (event) {
    if (event.target === overlay) {
      close();
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') {
      return;
    }
    // Close only the topmost thing: the summary, then an active selection in
    // the graph, then the overlay itself.
    if (summary && summary.classList.contains('open')) {
      showSummary(false);
      return;
    }
    if (seedApi && seedApi.busy()) {
      seedApi.reset();
      return;
    }
    close();
  });

  var resizeTimer = null;
  window.addEventListener('resize', function () {
    if (!overlay.classList.contains('open')) {
      return;
    }
    if (seedApi) {
      // The layout is sized to the stage, so a resize needs a fresh hydration.
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(function () {
        if (seedApi && lastCard) {
          seedApi.destroy();
          stage.innerHTML = lastCard.querySelector('svg.nl').outerHTML;
          seedApi = hydrateSeed(stage.querySelector('svg.nl'));
        }
      }, 200);
      return;
    }
    fit();
  });

  /* ------------------------------------------------------------ highlights */

  function clearHover() {
    stage.querySelectorAll('.hi').forEach(function (element) {
      element.classList.remove('hi');
    });
  }

  function highlight(cell) {
    clearHover();

    // Light up the cell, its row label, and its column label. Finding the column
    // means counting how far along the row the cell sits, then reading the header
    // cell at the same offset - without that you lose track of which pair a cell
    // in the middle of a large grid belongs to.
    var row = cell.parentElement;
    var column = Array.prototype.indexOf.call(row.children, cell);
    var head = stage.querySelector('thead tr');

    cell.classList.add('hi');
    row.children[0].classList.add('hi');
    if (head && head.children[column]) {
      head.children[column].classList.add('hi');
    }
  }

  stage.addEventListener('mouseover', function (event) {
    if (seedApi) {
      return;
    }
    var cell = event.target.closest('td[title]');
    if (!cell) {
      return;
    }
    // The ring always follows the cursor - that is just position feedback. The
    // panel only follows it in free mode.
    highlight(cell);
    if (!pinned) {
      show(detailFor(cell));
    }
  });

  /*
   * Clearing on mouseout as well as mouseleave.
   *
   * mouseleave only fires at the edge of the stage, so leaving an edge or corner
   * cell straight out of the table left it highlighted with nothing under the
   * cursor. mouseout fires whenever a cell is left at all; the relatedTarget
   * check makes sure moving between two cells is not treated as leaving.
   */
  stage.addEventListener('mouseout', function (event) {
    if (seedApi) {
      return;
    }
    var cell = event.target.closest('td[title]');
    if (!cell) {
      return;
    }
    var into = event.relatedTarget && event.relatedTarget.closest
             ? event.relatedTarget.closest('td[title]')
             : null;
    if (into === cell) {
      return;
    }
    clearHover();
    if (!pinned && !into) {
      show(HINT);
    }
  });

  stage.addEventListener('mouseleave', function () {
    if (seedApi) {
      return;
    }
    clearHover();
    if (!pinned) {
      show(HINT);
    }
  });

  stage.addEventListener('click', function (event) {
    if (seedApi) {
      return;
    }
    var cell = event.target.closest('td[title]');
    if (!cell) {
      return;
    }

    // Clicking the pinned cell again releases it, so one control both locks and
    // unlocks and there is no separate button to hunt for.
    if (pinned === cell) {
      pinned = null;
      cell.classList.remove('pin');
      show(detailFor(cell));   // cursor is still here, so free mode shows this cell
      return;
    }

    if (pinned) {
      pinned.classList.remove('pin');
    }
    pinned = cell;
    cell.classList.add('pin');
    show(detailFor(cell));
  });
})();
