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

  /* ---------------------------------------------------------------- routes */

  // Adjacency built from the seed conversions, both ways round. Only the
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
      container.innerHTML = '<div class="more">No route found in the hand-written '
                          + 'conversions.</div>';
      return;
    }

    // The shortest route's factor is the reference. Any route that disagrees is
    // worth seeing: two routes between the same units must give the same answer,
    // so a mismatch means one of the conversions along the way is wrong.
    var reference = found[0].m;
    var html = '';

    found.forEach(function (route) {
      var hops = route.path.length - 1;
      var chosen = hops === chosenHops;
      var off = reference !== 0 ? Math.abs(route.m - reference) / Math.abs(reference) : 0;
      var disagrees = off > 1e-9;

      html += '<div class="rt' + (chosen ? ' chosen' : '') + '">'
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

  function show(html) {
    odetail.innerHTML = html;
    // Drives the "pinned" badge in CSS, so the panel says why it is not
    // responding to the cursor.
    odetail.classList.toggle('locked', pinned !== null);
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

  function open(card) {
    otitle.textContent = card.querySelector('h2').textContent;
    otally.innerHTML = card.querySelector('.tally').innerHTML;
    stage.innerHTML = card.querySelector('table').outerHTML;

    var corner = stage.querySelector('th.corner');
    if (corner) {
      corner.textContent = 'from ↓';
    }

    pinned = null;
    show(HINT);
    overlay.classList.add('open');
    // Stop the page behind the overlay from scrolling with the wheel.
    document.body.style.overflow = 'hidden';
    // Sizing has to happen after the overlay is visible, or the stage still
    // measures zero. A frame later is guaranteed to be after layout.
    requestAnimationFrame(fit);
  }

  function close() {
    overlay.classList.remove('open');
    document.body.style.overflow = '';
    stage.innerHTML = '';
    pinned = null;
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
    summary.classList.toggle('open', show);
    document.body.style.overflow = show ? 'hidden' : '';
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
    // Close only the topmost thing, so Escape out of the summary does not also
    // dismiss a matrix that was open behind it.
    if (summary && summary.classList.contains('open')) {
      showSummary(false);
      return;
    }
    close();
  });

  window.addEventListener('resize', function () {
    if (overlay.classList.contains('open')) {
      fit();
    }
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
    clearHover();
    if (!pinned) {
      show(HINT);
    }
  });

  stage.addEventListener('click', function (event) {
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
