// CYTOSCAPE GRAPH VISUALIZATION

// Setup
  var seedApi = null;

  var cycleOn = vizStore('viz-cycle') !== 'off';

  var cycleButton = document.getElementById('ocycle');
  if (cycleButton) {
    cycleButton.classList.toggle('off', !cycleOn);
    cycleButton.setAttribute('aria-pressed', String(cycleOn));
    cycleButton.addEventListener('click', function () {
      cycleOn = !cycleOn;
      vizStore('viz-cycle', cycleOn ? 'on' : 'off');
      cycleButton.classList.toggle('off', !cycleOn);
      cycleButton.setAttribute('aria-pressed', String(cycleOn));
      if (seedApi && seedApi.idle) { seedApi.idle(cycleOn); }
    });
  }
  var hasGraphs = typeof GRAPHS !== 'undefined';

  var SEED_HINT = '<div class="empty"><b>Click two units</b> to list every route between '
                + 'them. <b>Click an edge</b> for its formula. Drag units to untangle, '
                + 'drag the background to pan, scroll to zoom.</div>';

  var GROUP_COLORS = ['#38bdf8', '#f87171', '#fbbf24', '#a78bfa', '#34d399', '#fb923c'];
  var MAX_PATHS = 4000;
  // Named apart from the cap in overlay.js: the five files share one
  // closure, so two identically named vars would be a single variable.
  var SEED_MAX_HOPS = 14;

  function escText(value) {
    return String(value).replace(/[&<>"]/g, function (c) {
      return {'&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;'}[c];
    });
  }

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
    requestAnimationFrame(function () { host.classList.add('ready'); });
  }

  function restyleGraphs() {
    if (seedApi) { seedApi.restyle(); }
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
    var paths = [];
    var dragNode = null;
    var flow = null;                  // the route-flow animation frame
    var flowPath = null;              // the hops the arrows are running along
    var cycle = null;                 // the idle walk through the routes
    var cycleAt = 0;
    var lead = null;                  // the pause before the first route lights
    var flowHint = false;
    var flowInk = token('--accent-deep');
    var phase = 0;
    var FLOW_STEPS = 22;              // segments a bowed hop is flattened into
    var enterTimers = [];

    // Above the graph and ignoring the mouse, so it never blocks a click.
    var flowCanvas = document.createElement('canvas');
    flowCanvas.className = 'flowlayer';
    host.appendChild(flowCanvas);
    var flowCtx = flowCanvas.getContext('2d');

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
      stopFlow();
      stopCycle();
      cy.elements().removeClass('dim sel on-route pick-a pick-b hot');
    }

    // Dims other nodes and edges when hovering over a different node
    function neighborhood(i) {
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

    var pulseSeq = {};

    function pulse(ele, colour, reach) {
      var id = ele.id();
      var mine = (pulseSeq[id] || 0) + 1;
      pulseSeq[id] = mine;

      ele.stop();
      ele.clearQueue();
      ele.style({'overlay-color': colour, 'overlay-padding': 2,
                 'overlay-opacity': 0.42, 'overlay-corner-radius': 999});
      ele.animate({style: {'overlay-padding': reach, 'overlay-opacity': 0}},
                  {duration: 430, easing: 'ease-out', complete: function () {
                     if (pulseSeq[id] !== mine) {
                       return;            // a newer ring owns this element now
                     }
                     ele.removeStyle('overlay-color overlay-padding overlay-opacity '
                                     + 'overlay-corner-radius');
                   }});
    }

    function pulseNode(ele, colour) {
      pulse(ele, colour, 24);
    }

    function pulseEdge(ele) {
      pulse(ele, token('--edge-pick'), 20);
    }

    function selectEdge(j) {
      pulseEdge(E[j].ele);
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
      var was = i === pickA ? 1 : i === pickB ? 2 : 0;
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
      // Releasing a pick keeps the color it is giving up, so the ring always
      // says which of the two slots the click was about.
      var now = i === pickA ? 1 : i === pickB ? 2 : 0;
      pulseNode(N[i].ele, token((now || was) === 2 ? '--pick-2' : '--pick-1'));
      refreshPicks();
    }

    function refreshPicks() {
      clearMarks();
      cy.nodes().removeClass('hover preview');
      markPicks();
      paths = (pickA !== null && pickB !== null) ? findPaths(pickA, pickB) : [];
      // With one unit chosen there is no route to show yet, so the flow points
      // outward along every conversion leaving it: these are the next steps.
      if (pickA !== null && pickB === null) {
        startFlow(ADJ[pickA].map(function (step) {
          return {ei: step.ei, from: pickA, to: step.other};
        }), 'hint');
      }
      drawPanel();
      startCycle();
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
      ele.style({opacity: 0, width: 8, height: 8});
      ele.animate({style: {opacity: 1, width: 26, height: 26}},
                  {duration: 220, easing: 'ease-out'});
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
        if (path.length >= SEED_MAX_HOPS) { return; }
        ADJ[cur].forEach(function (step) {
          if (usedE[step.ei]) { return; }
          if (seenN[step.other] && step.other !== to) { return; }
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
      var steps = stepsOf(path);
      if (!steps || !steps.length) {
        return null;
      }
      var last = steps[steps.length - 1];
      return {m: last.runM, b: last.runB};
    }

    // Present the right side panel results
    function stepsOf(path) {
      var m = 1;
      var b = 0;
      var steps = [];
      for (var i = 0; i < path.length; i++) {
        var edge = E[path[i].ei];
        if (edge.m === null || isNaN(edge.m)) { return null; }
        var em;
        var eb;
        var reversed = path[i].from !== edge.s;
        if (!reversed) { em = edge.m; eb = edge.b; }
        else {
          if (edge.m === 0) { return null; }
          em = 1 / edge.m; eb = -edge.b / edge.m;
        }
        m = em * m;
        b = em * b + eb;
        steps.push({from: N[path[i].from].id, to: N[path[i].to].id,
                    m: em, b: eb, runM: m, runB: b, reversed: reversed});
      }
      return steps;
    }

    function stepsHtml(path) {
      var steps = stepsOf(path);
      if (!steps) {
        return '';
      }
      return '<div class="steps"><div class="steps-lbl">every constant this '
        + 'route multiplies in</div>'
        + steps.map(function (step, i) {
            return '<div class="step">'
              + '<span class="step-n">' + (i + 1) + '</span>'
              + '<span class="step-pair"><span class="u">' + sup(escText(step.from))
              + '</span>' + ARROW + '<span class="u">' + sup(escText(step.to))
              + '</span></span>'
              + '<span class="step-calc">'
              + '<span class="step-op">× ' + num(step.m)
              + (step.b !== 0 ? (step.b >= 0 ? ' + ' : ' − ') + num(Math.abs(step.b)) : '')
              + (step.reversed ? '<em title="stored the other way round and '
                 + 'inverted here">inv</em>' : '')
              + '</span>'
              + '<span class="step-run">= ' + num(step.runM) + '</span>'
              + '</span></div>';
          }).join('')
        + '</div>';
    }

    function factorHtml(result, A, B) {
      if (!result) { return 'not a simple scale + offset - cannot compose numerically'; }
      var out = '<span class="u">' + sup(escText(B)) + '</span> = <span class="u">'
              + sup(escText(A)) + '</span>';
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

    // the results panel (right sidebar)
    function drawPanel() {
      if (pickA === null) {
        panelShow(SEED_HINT, false);
        return;
      }
      var A = N[pickA].id;
      if (pickB === null) {
        panelShow(clearButton() + '<h4><span class="pk p1">1</span>' + sup(escText(A))
                + ARROW + '?</h4>' + namesLine(pickA, null)
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
        + '<h4><span class="pk p1">1</span>' + sup(escText(A))
        + ARROW + '<span class="pk p2">2</span>' + sup(escText(B)) + '</h4>'
        + namesLine(pickA, pickB)
        + '<div class="pth-sub">' + paths.length + ' route' + (paths.length === 1 ? '' : 's')
        + (paths.truncated ? ' (capped at ' + MAX_PATHS + ')' : '')
        + (paths.length ? ' · shortest ' + paths[0].length + ' hop'
           + (paths[0].length === 1 ? '' : 's') + ', longest '
           + paths[paths.length - 1].length : '') + '</div>';

      if (!paths.length) {
        panelShow(head + '<div class="empty">No route exists between these two units.</div>', true);
        return;
      }

      if (order.length > 1) {
        head += '<div class="pth-warn"><b>These routes disagree.</b> ' + order.length
              + ' different results across ' + paths.length + ' routes - the color dot marks '
              + 'each group. One or more direct conversions on the odd routes out is lossy '
              + 'or wrong.</div>';
      } else {
        head += '<div class="pth-ok">' + (paths.length === 1
                ? '✓ one route, nothing to disagree with'
                : '✓ all ' + paths.length + ' routes agree') + '</div>';
      }

      var rows = paths.map(function (p, i) {
        var r = results[i];
        var dot = order.length > 1
          ? '<span class="grp" style="background:'
            + GROUP_COLORS[order.indexOf(keyOf(r)) % GROUP_COLORS.length] + '"></span>'
          : '';
        var chain = [A].concat(p.map(function (h) { return N[h.to].id; }))
                       .map(function (id) { return sup(escText(id)); }).join(ARROW);
        return '<div class="prow' + (i === 0 ? ' best' : '') + '" data-i="' + i
             + '" style="--i:' + i + '">'
             + '<div class="top"><span class="hops">' + p.length + ' hop'
             + (p.length === 1 ? '' : 's') + '</span>'
             + '<span class="route">' + dot + chain + '</span></div>'
             + '<div class="res">' + factorHtml(r, A, B) + '</div>'
             + budgetHtml(r, results[0])
             + '<button type="button" class="explain" aria-expanded="false">'
             + 'explain this number</button>'
             + '<div class="steps-wrap" hidden>' + stepsHtml(p) + '</div></div>';
      }).join('');

      panelShow(head + rows, true);
      // Hovering previews a route; clicking holds it, so the arrows keep
      // running while the mouse goes elsewhere
      var held = null;
      odetail.querySelectorAll('.explain').forEach(function (button) {
        button.addEventListener('click', function (event) {
          event.stopPropagation();          // not a route pick
          var wrap = button.nextElementSibling;
          wrap.hidden = !wrap.hidden;
          button.setAttribute('aria-expanded', String(!wrap.hidden));
          button.textContent = wrap.hidden ? 'explain this number' : 'hide the steps';
        });
      });

      odetail.querySelectorAll('.prow').forEach(function (row) {
        var index = +row.dataset.i;
        row.addEventListener('mouseenter', function () {
          if (held === null) { showRoute(paths[index]); }
        });
        row.addEventListener('mouseleave', function () {
          if (held === null) {
            clearMarks();
            markPicks();
            startCycle();             // back to idle
          }
        });
        row.addEventListener('click', function () {
          odetail.querySelectorAll('.prow.held').forEach(function (other) {
            other.classList.remove('held');
          });
          held = held === index ? null : index;
          row.classList.toggle('held', held === index);
          showRoute(paths[index]);
        });
      });
    }

    // Arrows animation
    function curveOf(hop) {
      var edge = E[hop.ei].ele;
      var forward = hop.from === E[hop.ei].s;
      var a = forward ? edge.sourceEndpoint() : edge.targetEndpoint();
      var b = forward ? edge.targetEndpoint() : edge.sourceEndpoint();
      var mid = edge.midpoint();
      return {a: a, b: b,
              c: {x: 2 * mid.x - (a.x + b.x) / 2, y: 2 * mid.y - (a.y + b.y) / 2}};
    }

    function polyOf(hop) {
      var curve = curveOf(hop);
      var points = [];
      var runs = [0];
      for (var i = 0; i <= FLOW_STEPS; i++) {
        var t = i / FLOW_STEPS;
        var u = 1 - t;
        points.push({x: u * u * curve.a.x + 2 * u * t * curve.c.x + t * t * curve.b.x,
                     y: u * u * curve.a.y + 2 * u * t * curve.c.y + t * t * curve.b.y});
        if (i) {
          var dx = points[i].x - points[i - 1].x;
          var dy = points[i].y - points[i - 1].y;
          runs.push(runs[i - 1] + Math.sqrt(dx * dx + dy * dy));
        }
      }
      return {points: points, runs: runs, length: runs[runs.length - 1]};
    }

    function alongPoly(poly, distance) {
      var i = 1;
      while (i < poly.runs.length && poly.runs[i] < distance) { i++; }
      if (i >= poly.runs.length) { return null; }
      var a = poly.points[i - 1];
      var b = poly.points[i];
      var span = poly.runs[i] - poly.runs[i - 1] || 1;
      var t = (distance - poly.runs[i - 1]) / span;
      return {x: a.x + (b.x - a.x) * t, y: a.y + (b.y - a.y) * t,
              angle: Math.atan2(b.y - a.y, b.x - a.x)};
    }

    function sizeFlow() {
      var ratio = window.devicePixelRatio || 1;
      flowCanvas.width = Math.max(1, Math.round(host.clientWidth * ratio));
      flowCanvas.height = Math.max(1, Math.round(host.clientHeight * ratio));
      flowCanvas.style.width = host.clientWidth + 'px';
      flowCanvas.style.height = host.clientHeight + 'px';
      flowCtx.setTransform(ratio, 0, 0, ratio, 0, 0);
    }

    function drawFlow() {
      flowCtx.clearRect(0, 0, host.clientWidth, host.clientHeight);
      if (!flowPath || !flowPath.length) { return; }

      var zoom = cy.zoom();
      var pan = cy.pan();
      var size = (flowHint ? 5.5 : 7.5) * zoom;
      var gap = (flowHint ? 30 : 24) * zoom;

      var thin = (flowHint ? 2.1 : 2.9) * zoom;
      var wide = thin * 3.1;
      var alpha = flowHint ? 0.6 : 1;
      flowCtx.lineCap = 'round';
      flowCtx.lineJoin = 'round';
      flowCtx.strokeStyle = flowInk;

      var carry = phase % gap;
      flowPath.forEach(function (hop) {
        var poly = polyOf(hop);
        var span = poly.length * zoom;
        for (var d = carry; d < span; d += gap) {
          var at = alongPoly(poly, d / zoom);
          if (!at) { break; }
          flowCtx.save();
          flowCtx.translate(at.x * zoom + pan.x, at.y * zoom + pan.y);
          flowCtx.rotate(at.angle);
          flowCtx.beginPath();
          flowCtx.moveTo(-size, -size * 0.7);
          flowCtx.lineTo(0, 0);
          flowCtx.lineTo(-size, size * 0.7);
          flowCtx.lineWidth = wide;
          flowCtx.globalAlpha = alpha * 0.22;
          flowCtx.stroke();
          flowCtx.lineWidth = thin;
          flowCtx.globalAlpha = alpha;
          flowCtx.stroke();
          flowCtx.restore();
        }
        carry = Math.max(0, carry + Math.ceil((span - carry) / gap) * gap - span);
      });

      if (!flowHint) {
        ring(pickA, token('--pick-1'), zoom, pan);
        ring(pickB, token('--pick-2'), zoom, pan);
      }
      flowCtx.globalAlpha = 1;
    }

    function ring(index, color, zoom, pan) {
      if (index === null || !N[index]) { return; }
      var node = N[index].ele;
      var beat = (Math.sin(phase / 16) + 1) / 2;            // 0..1, slow
      var radius = (Math.max(node.outerWidth(), node.outerHeight()) / 2 + 7 + beat * 7) * zoom;
      flowCtx.beginPath();
      flowCtx.arc(node.position('x') * zoom + pan.x, node.position('y') * zoom + pan.y,
                  radius, 0, Math.PI * 2);
      flowCtx.strokeStyle = color;
      flowCtx.lineWidth = 2 * zoom;
      flowCtx.globalAlpha = 0.5 - beat * 0.32;
      flowCtx.stroke();
      flowCtx.strokeStyle = flowInk;
    }

    function startFlow(path, extra) {
      stopFlow();
      if (!path || !path.length) { return; }
      flowPath = path;
      flowHint = extra === 'hint';
      flowInk = token('--accent-deep');
      (function tickFlow() {
        phase += flowHint ? 0.5 : 0.85;
        drawFlow();
        flow = requestAnimationFrame(tickFlow);
      })();
    }

    function stopFlow() {
      if (flow) {
        cancelAnimationFrame(flow);
        flow = null;
      }
      flowPath = null;
      flowCtx.clearRect(0, 0, host.clientWidth, host.clientHeight);
    }

    function budgetHtml(result, best) {
        if (!result || !best || !isFinite(result.m) || !isFinite(best.m) || best.m === 0) {
            return '';
        }
        var off = Math.abs(result.m - best.m) / Math.abs(best.m);
        if (off < 1e-12) {
            return '<div class="budget agrees">agrees with the shortest route</div>';
        }
        var tone = off > 1e-6 ? 'bad' : 'warn';
        return '<div class="budget ' + tone + '">differs from the shortest route by '
             + '<b>' + (off * 100).toPrecision(3) + '%</b>'
             + (tone === 'bad' ? ' - one of them uses a wrong or rounded constant' : '')
             + '</div>';
    }

    // Idle route cycling animations
    var CYCLE_MS = 2000;              // how long each route stays lit
    var CYCLE_LEAD = 500;             // a beat before the first one comes on

    function litPath(p) {
      var onEdge = {};
      var onNode = {};
      p.forEach(function (h) {
        onEdge[h.ei] = true;
        onNode[h.to] = true;
      });
      delete onNode[pickB];           // the picks keep their own colors

      cy.batch(function () {
        E.forEach(function (edge, j) { edge.ele.toggleClass('cycle', !!onEdge[j]); });
        N.forEach(function (node, j) { node.ele.toggleClass('cycle', !!onNode[j]); });
      });

      odetail.querySelectorAll('.prow').forEach(function (row) {
        row.classList.toggle('lit', +row.dataset.i === cycleAt);
      });
    }

    function startCycle() {
      stopCycle();
      if (!cycleOn || !paths.length || pickA === null || pickB === null) {
        return;
      }
      cycleAt = 0;
      lead = setTimeout(function () {
        lead = null;
        litPath(paths[0]);
        if (paths.length === 1) {
          return;                     // nothing to cycle to
        }
        cycle = setInterval(function () {
          cycleAt = (cycleAt + 1) % paths.length;
          litPath(paths[cycleAt]);
        }, CYCLE_MS);
      }, CYCLE_LEAD);
    }

    function stopCycle() {
      if (lead) { clearTimeout(lead); lead = null; }
      if (cycle) { clearInterval(cycle); cycle = null; }
      cy.elements().removeClass('cycle');
      odetail.querySelectorAll('.prow.lit').forEach(function (row) {
        row.classList.remove('lit');
      });
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
      startFlow(p);
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
      neighborhood(i);
      panelShow('<div class="fx"><div class="fx-head"><span class="u">'
        + sup(escText(N[i].id)) + '</span></div><div class="fx-names">'
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
    sizeFlow();
    cy.on('resize', sizeFlow);

    cy.zoom(1);
    cy.pan({x: 0, y: 0});
    draw();
    alpha = data.tree ? 0.5 : 1;
    sim = requestAnimationFrame(step);

    // The graph assembles itself: units land one after another, then the
    // conversions between them draw in.
    cy.elements().addClass('entering');
    N.forEach(function (node, i) {
      enterTimers.push(setTimeout(function () {
        node.ele.removeClass('entering');
      }, 30 + i * 15));
    });
    enterTimers.push(setTimeout(function () {
      cy.edges().removeClass('entering');
    }, 60 + N.length * 15));

    return {
      busy: busy,
      reset: reset,
      idle: function (on) {
        if (on) { startCycle(); } else { stopCycle(); }
      },
      resize: function () {
        cy.resize();
        if (typeof sizeFlow === 'function') { sizeFlow(); }
      },
      restyle: function () {
        flowInk = token('--accent-deep');
        cy.style(cyStyle(true)).update();
      },
      pick: function (fromId, toId) {
        if (!(fromId in indexOf) || !(toId in indexOf)) { return; }
        pickA = indexOf[fromId];
        pickB = indexOf[toId];
        refreshPicks();
        var shortest = odetail.querySelector('.prow');
        if (shortest) { shortest.click(); }
      },
      destroy: function () {
        stopFlow();
        stopCycle();
        if (sim) { cancelAnimationFrame(sim); sim = null; }
        if (resetButton) { resetButton.onclick = null; }
        odetail.removeEventListener('click', onPanelClick);
        enterTimers.forEach(clearTimeout);
        flowCanvas.remove();
        cy.destroy();
      }
    };
  }
