// CYTOSCAPE GRAPH VISUALIZATION

// Setup
  var seedApi = null;
  var hasGraphs = typeof GRAPHS !== 'undefined';

  var SEED_HINT = '<div class="empty"><b>Click two units</b> to list every route between '
                + 'them. <b>Click an edge</b> for its formula. Drag units to untangle, '
                + 'drag the background to pan, scroll to zoom.</div>';

  var GROUP_COLORS = ['#38bdf8', '#f87171', '#fbbf24', '#a78bfa', '#34d399', '#fb923c'];
  var MAX_PATHS = 4000;
  var MAX_HOPS = 14;

  function escText(value) {
    return String(value).replace(/[&<>"]/g, function (c) {
      return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;'}[c];
    });
  }

  function token(name) {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  }

// Setting up the Cytoscape style
  function cyStyle(enlarged) {
    var mono = token('--mono') || 'monospace';
    function pillWidth(ele) { return 26 + 13 * String(ele.data('label')).length; }
    return [
      {selector: 'node', style: {
        'shape': 'round-rectangle',
        'corner-radius': 23,
        'width': pillWidth,
        'height': 46,
        'background-color': '#e5e7eb',
        'border-color': '#6b7280',
        'border-width': enlarged ? 2 : 1.5,
        'label': 'data(label)',
        'color': '#374151',
        'font-family': mono,
        'font-size': enlarged ? 17 : 14,
        'text-valign': 'center',
        'text-halign': 'center',
        'transition-property': 'opacity, border-width, border-color',
        'transition-duration': '0.11s',
        'transition-timing-function': 'ease-out'
      }},
      {selector: 'node.t-si', style: {
        'background-color': '#dbeafe', 'border-color': '#2563eb', 'color': '#1e3a5f'}},
      {selector: 'node.t-english', style: {
        'background-color': '#fee2e2', 'border-color': '#dc2626', 'color': '#5f1e1e'}},

      {selector: 'edge', style: {
        'curve-style': 'straight',
        'line-color': enlarged ? '#64748b' : '#94a3b8',
        'width': enlarged ? 2.5 : 1.6,
        'opacity': 1,
        'transition-property': 'opacity, line-color, width',
        'transition-duration': '0.11s',
        'transition-timing-function': 'ease-out'}},
      {selector: 'edge[bow != 0]', style: {
        'curve-style': 'unbundled-bezier',
        'control-point-distances': function (ele) {
          return [ele.data('bow') * (enlarged ? 60 : 40)];
        },
        'control-point-weights': [0.5]}},
      {selector: 'edge.function', style: {'line-style': 'dashed',
        'line-dash-pattern': enlarged ? [7, 5] : [6, 4]}},

      {selector: '.dim', style: {'opacity': 0.12}},
      {selector: 'edge.hot', style: {'line-color': token('--edge-pick'), 'width': 4}},
      {selector: 'edge.sel', style: {
        'line-color': token('--edge-pick'), 'width': 4.5, 'opacity': 1}},
      {selector: 'edge.on-route', style: {
        'line-color': token('--accent-deep'), 'width': 5, 'opacity': 1}},

      {selector: 'node.pick-a', style: {
        'border-color': token('--pick-1'), 'border-width': 4, 'opacity': 1}},
      {selector: 'node.pick-b', style: {
        'border-color': token('--pick-2'), 'border-width': 4, 'opacity': 1}},

      {selector: 'node.bdg', style: {
        'shape': 'ellipse', 'width': 26, 'height': 26,
        'border-color': '#0f172a', 'border-width': 2,
        'label': 'data(label)', 'color': '#0f172a',
        'font-family': token('--sans') || 'sans-serif',
        'font-size': 15, 'font-weight': 700,
        'text-valign': 'center', 'text-halign': 'center',
        'events': 'no', 'z-index': 9}},
      {selector: 'node.bdg.p1', style: {'background-color': token('--pick-1')}},
      {selector: 'node.bdg.p2', style: {'background-color': token('--pick-2')}},
      {selector: 'node.hover', style: {'border-width': 3.5}},
      {selector: 'node.preview', style: {
        'border-color': token('--pick-2'), 'border-width': 3.5}},
      {selector: 'node.pop', style: {'border-width': 9}}
    ];
  }
  var PRESET = {name: 'preset', fit: false, animate: false};

  // Thumbnails for main menu of graph visualizer
  function initThumb(host) {
    if (host.dataset.ready || !hasGraphs) {
      return;
    }
    var data = GRAPHS[host.dataset.group];
    if (!data) {
      return;
    }
    host.dataset.ready = '1';
    var cy = cytoscape({
      container: host,
      elements: data.elements,
      style: cyStyle(false),
      layout: PRESET,
      userZoomingEnabled: false,
      userPanningEnabled: false,
      boxSelectionEnabled: false,
      autoungrabify: true
    });
    cy.fit(undefined, 8);
    var picture = cy.png({output: 'base64uri', full: true, scale: 2, bg: 'transparent'});
    cy.destroy();
    host.style.backgroundImage = 'url(' + picture + ')';
  }

  if (hasGraphs && window.IntersectionObserver) {
    var watcher = new IntersectionObserver(function (entries) {
      entries.forEach(function (entry) {
        if (entry.isIntersecting) {
          initThumb(entry.target);
          watcher.unobserve(entry.target);
        }
      });
    }, {rootMargin: '250px'});
    document.querySelectorAll('.seedcard .cy').forEach(function (host) {
      watcher.observe(host);
    });
  } else if (hasGraphs) {
    document.querySelectorAll('.seedcard .cy').forEach(initThumb);
  }

  function hydrateSeed(host, group) {
    var data = GRAPHS[group];
    // create cytoscape instance
    var cy = cytoscape({
      container: host,
      elements: data.elements,
      style: cyStyle(true),
      layout: PRESET,
      minZoom: 0.45,
      maxZoom: 3.5,
      textureOnViewport: true,
      boxSelectionEnabled: false
    });

    var W = host.clientWidth;
    var H = host.clientHeight;
    var PX = 110;
    var PY = 80;

    var N = cy.nodes().map(function (node, i, all) {
      var seed = {id: node.id(), name: node.data('name'), ele: node,
                  vx: 0, vy: 0, fixed: false};
      if (data.tree) {
        seed.hx = PX + node.data('nx') * (W - 2 * PX);
        seed.hy = PY + node.data('ny') * (H - 2 * PY);
        seed.x = seed.hx;
        seed.y = seed.hy;
      } else {
        var angle = 2 * Math.PI * i / all.length - Math.PI / 2;
        seed.x = W / 2 + Math.cos(angle) * Math.min(W, H) * 0.32;
        seed.y = H / 2 + Math.sin(angle) * Math.min(W, H) * 0.32;
      }
      seed.ix = seed.x;
      seed.iy = seed.y;
      seed.px = seed.x;
      seed.py = seed.y;
      return seed;
    });
    var indexOf = {};
    N.forEach(function (node, i) { indexOf[node.id] = i; });

    var edgeIndex = {};
    var E = cy.edges().map(function (edge, j) {
      edgeIndex[edge.id()] = j;
      return {s: indexOf[edge.data('source')], t: indexOf[edge.data('target')],
              m: edge.data('m'), b: edge.data('b'),
              detail: edge.data('detail') || '', ele: edge};
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
    var dragNode = null;

    function busy() { return selEdge !== null || pickA !== null; }

    var sim = null;
    var alpha = 1;
    var LEN = Math.max(120, Math.min(W, H) / (1.7 + N.length * 0.10));

    // add some physics simulating for graph
    function tick() {
      N.forEach(function (n) { n.fx2 = 0; n.fy2 = 0; });
      for (var a = 0; a < N.length; a++) {
        for (var b = a + 1; b < N.length; b++) {
          var dx = N[b].x - N[a].x;
          var dy = N[b].y - N[a].y;
          var d2 = Math.max(dx * dx + dy * dy, 25);
          var d = Math.sqrt(d2);
          var f = (data.tree ? 6000 : 16000) / d2;
          N[a].fx2 -= f * dx / d; N[a].fy2 -= f * dy / d;
          N[b].fx2 += f * dx / d; N[b].fy2 += f * dy / d;
        }
      }
      if (!data.tree) {
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
        if (data.tree) {
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

    // apply physics to cytoscape
    function draw() {
      var most = 0;
      cy.batch(function () {
        N.forEach(function (n, i) {
          var moved = Math.max(Math.abs(n.px - n.x), Math.abs(n.py - n.y));
          if (moved > 0.02) {
            n.ele.position({x: n.x, y: n.y});
            n.px = n.x;
            n.py = n.y;
            if (i === pickA && badges[1]) { badges[1].position({x: n.x, y: n.y - 38}); }
            if (i === pickB && badges[2]) { badges[2].position({x: n.x, y: n.y - 38}); }
          }
          most = Math.max(most, moved);
        });
      });
      return most;
    }

    // run physics one tick per frame
    function step() {
      tick();
      draw();
      if (dragNode === null) { alpha *= 0.962; }
      if (alpha < 0.002 && dragNode === null) { sim = null; return; }
      sim = requestAnimationFrame(step);
    }

    // restart physics when mouse dragging
    function reheat() {
      alpha = Math.max(alpha, 0.22);
      if (!sim) { sim = requestAnimationFrame(step); }
    }

    // remove highlight states
    function clearMarks() {
      cy.elements().removeClass('dim sel on-route pick-a pick-b hot');
    }

    // Dims other nodes and edges when hovering over a different node
    function neighbourhood(i) {
      clearMarks();
      if (i === null) { return; }
      var near = {};
      near[i] = true;
      E.forEach(function (e) {
        if (e.s === i) { near[e.t] = true; }
        if (e.t === i) { near[e.s] = true; }
      });
      N.forEach(function (node, j) { node.ele.toggleClass('dim', !near[j]); });
      E.forEach(function (edge, j) { edge.ele.toggleClass('dim', edge.s !== i && edge.t !== i); });
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
      E.forEach(function (edge, k) {
        edge.ele.toggleClass('sel', k === j);
        edge.ele.toggleClass('dim', k !== j);
      });
      N.forEach(function (node, k) {
        node.ele.toggleClass('dim', k !== E[j].s && k !== E[j].t);
      });
      syncBadges();
      panelShow(clearButton() + (E[j].detail || '<div class="empty">no formula</div>'), true);
    }

    // go back to neutral
    function reset() {
      selEdge = null;
      pickA = pickB = null;
      paths = [];
      clearMarks();
      syncBadges();
      panelShow(SEED_HINT, false);
    }

    function clearButton() {
      return '<button type="button" class="clearpick">Clear selection</button>';
    }

    // Node toggling for selecting nodes and choosing paths
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
      alpha = Math.min(alpha, 0.12);
      N[i].ele.flashClass('pop', 430);
      refreshPicks();
    }

    function refreshPicks() {
      clearMarks();
      cy.nodes().removeClass('hover preview');
      markPicks();
      paths = (pickA !== null && pickB !== null) ? findPaths(pickA, pickB) : [];
      drawPanel();
    }

    function markPicks() {
      if (pickA !== null) { N[pickA].ele.addClass('pick-a'); }
      if (pickB !== null) { N[pickB].ele.addClass('pick-b'); }
      syncBadges();
    }

    var badges = {};

    // show little colored (1) and (2) badges when selecting nodes
    function badge(order, i) {
      if (i === null) {
        if (badges[order]) {
          badges[order].remove();
          badges[order] = null;
        }
        return;
      }
      if (badges[order] && badges[order].data('at') === i) {
        return;
      }
      if (badges[order]) {
        badges[order].remove();
      }
      var ele = cy.add({group: 'nodes',
                        data: {id: '__bdg' + order, label: String(order), bow: 0, at: i},
                        position: {x: N[i].x, y: N[i].y - 38},
                        selectable: false, grabbable: false,
                        classes: 'bdg p' + order});
      ele.style('opacity', 0);
      ele.animate({style: {opacity: 1}}, {duration: 180});
      badges[order] = ele;
    }

    function syncBadges() {
      badge(1, pickA);
      badge(2, pickB);
    }

    // Algorithm to find every route between two nodes
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
        ADJ[cur].forEach(function (step) {
          if (usedE[step.ei]) { return; }
          if (!allowRevisit && seenN[step.other] && step.other !== to) { return; }
          usedE[step.ei] = true;
          var fresh = !seenN[step.other];
          if (fresh) { seenN[step.other] = true; }
          path.push({ei: step.ei, from: cur, to: step.other});
          walk(step.other, path);
          path.pop();
          delete usedE[step.ei];
          if (fresh) { delete seenN[step.other]; }
        });
      })(from, []);
      out.truncated = truncated;
      out.sort(function (p, q) { return p.length - q.length; });
      return out;
    }

    // multiply conversion factors to get to a single answer
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

    // Present the right side panel results
    function factorHtml(result, A, B) {
      if (!result) { return 'not a simple scale + offset - cannot compose numerically'; }
      var out = '<span class="u">' + escText(B) + '</span> = <span class="u">'
              + escText(A) + '</span>';
      if (result.m !== 1) { out += ' × ' + num(result.m); }
      if (result.b !== 0) { out += (result.b >= 0 ? ' + ' : ' − ') + num(Math.abs(result.b)); }
      return out;
    }

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
        row.addEventListener('mouseleave', function () {
          clearMarks();
          markPicks();
        });
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

    function showRoute(p) {
      clearMarks();
      var onE = {};
      var onN = {};
      onN[pickA] = true;
      p.forEach(function (h) { onE[h.ei] = true; onN[h.to] = true; });
      E.forEach(function (edge, j) {
        edge.ele.toggleClass('on-route', !!onE[j]);
        edge.ele.toggleClass('dim', !onE[j]);
      });
      N.forEach(function (node, j) { node.ele.toggleClass('dim', !onN[j]); });
      markPicks();
    }

    // All mouse events
    cy.on('grab', 'node', function (event) {
      dragNode = indexOf[event.target.id()];
      N[dragNode].fixed = true;
    });
    cy.on('drag', 'node', function (event) {
      var i = indexOf[event.target.id()];
      var at = event.target.position();
      N[i].x = at.x;
      N[i].y = at.y;
      reheat();
    });
    cy.on('free', 'node', function (event) {
      N[indexOf[event.target.id()]].fixed = false;
      dragNode = null;
      reheat();
    });

    cy.on('tap', 'node', function (event) {
      nodeClicked(indexOf[event.target.id()]);
    });
    cy.on('tap', 'edge', function (event) {
      selectEdge(edgeIndex[event.target.id()]);
    });
    cy.on('tap', function (event) {
      if (event.target === cy) {
        reset();                          // a plain background click clears
      }
    });

    cy.on('mouseover', 'node', function (event) {
      var i = indexOf[event.target.id()];
      if (i === pickA || i === pickB || selEdge !== null) {
        return;                       // already marked; leave its ring alone
      }
      if (pickA !== null) {
        event.target.addClass('preview');
        return;
      }
      event.target.addClass('hover');
      if (sim && alpha > 0.06) {
        return;
      }
      neighbourhood(i);
      panelShow('<div class="fx"><div class="fx-head"><span class="u">'
        + escText(N[i].id) + '</span></div><div class="fx-names">'
        + escText(N[i].name || '') + '</div></div>', false);
    });

    cy.on('mouseout', 'node', function (event) {
      event.target.removeClass('hover preview');
      if (!busy()) {
        clearMarks();
        panelShow(SEED_HINT, false);
      }
    });

    cy.on('mouseover', 'edge', function (event) {
      if (busy()) {
        return;                       // a pick or a selection has the floor
      }
      event.target.addClass('hot');
      panelShow(E[edgeIndex[event.target.id()]].detail || SEED_HINT, false);
    });
    cy.on('mouseout', 'edge', function (event) {
      event.target.removeClass('hot');
      if (!busy()) { panelShow(SEED_HINT, false); }
    });

    function onPanelClick(event) {
      if (event.target.closest('.clearpick')) {
        reset();
      }
    }
    odetail.addEventListener('click', onPanelClick);

    var resetButton = document.getElementById('oreset');
    if (resetButton) {
      resetButton.onclick = function () {
        N.forEach(function (n) {
          n.x = n.ix; n.y = n.iy; n.vx = n.vy = 0; n.fixed = false;
        });
        cy.zoom(1);
        cy.pan({x: 0, y: 0});
        reset();
        draw();
        alpha = data.tree ? 0.5 : 1;
        if (!sim) { sim = requestAnimationFrame(step); }
      };
    }

    panelShow(SEED_HINT, false);
    cy.zoom(1);
    cy.pan({x: 0, y: 0});
    draw();
    alpha = data.tree ? 0.5 : 1;
    sim = requestAnimationFrame(step);

    return {
      busy: busy,
      reset: reset,
      destroy: function () {
        if (sim) { cancelAnimationFrame(sim); sim = null; }
        if (resetButton) { resetButton.onclick = null; }
        odetail.removeEventListener('click', onPanelClick);
        cy.destroy();
      }
    };
  }
