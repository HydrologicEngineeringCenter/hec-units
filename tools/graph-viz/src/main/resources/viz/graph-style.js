// How the conversion graph is drawn

  function token(name) {
    return getComputedStyle(document.documentElement).getPropertyValue(name).trim();
  }

  function fontStack(name, fallback) {
    var value = token(name).replace(/['"]/g, '').trim();
    return /^[\w\- ]+(?:\s*,\s*[\w\- ]+)*$/.test(value) ? value : fallback;
  }

// Setting up the Cytoscape style
  function cyStyle(enlarged) {
    var mono = fontStack('--mono', 'monospace');
    function pillWidth(ele) { return 26 + 13 * String(ele.data('label')).length; }
    return [
      {selector: 'node', style: {
        'shape': 'round-rectangle',
        'corner-radius': 23,
        'width': pillWidth,
        'height': 46,
        'background-color': '#cbd5e1',
        'border-color': '#64748b',
        'border-width': enlarged ? 2 : 1.5,
        'label': 'data(label)',
        'color': '#374151',
        'font-family': mono,
        'font-size': enlarged ? 20 : 15,
        'font-weight': 'bold',
        'text-valign': 'center',
        'text-halign': 'center',
        'transition-property': 'opacity, border-width, border-color',
        'transition-duration': '0.11s',
        'transition-timing-function': 'ease-out'
      }},
      {selector: 'node.t-si', style: {
        'background-color': '#a9d0fd', 'border-color': '#2563eb', 'color': '#16304f'}},
      {selector: 'node.t-english', style: {
        'background-color': '#fdb9b9', 'border-color': '#dc2626', 'color': '#511919'}},

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

      {selector: '.entering', style: {'opacity': 0}},
      {selector: 'edge.hot', style: {'line-color': token('--edge-pick'), 'width': 4}},

      {selector: 'edge.cycle', style: {
        'line-color': token('--glow'), 'width': 4.5, 'opacity': 1, 'z-index': 12,
        'overlay-color': token('--glow'), 'overlay-opacity': 0.18,
        'overlay-padding': 7,
        'transition-property': 'line-color, width, overlay-opacity, overlay-padding',
        'transition-duration': '0.4s',
        'transition-timing-function': 'ease-out'}},

      {selector: 'node.cycle', style: {
        'border-color': token('--glow'), 'border-width': 3.5,
        'overlay-color': token('--glow'), 'overlay-opacity': 0.18,
        'overlay-padding': 7, 'overlay-corner-radius': 999,
        'transition-property':
          'border-color, border-width, overlay-opacity, overlay-padding',
        'transition-duration': '0.4s',
        'transition-timing-function': 'ease-out'}},

      {selector: 'edge.sel', style: {
        'line-color': token('--edge-pick'), 'width': 4.5, 'opacity': 1}},

      {selector: 'edge.on-route', style: {'opacity': 1}},

      {selector: 'node.pick-a', style: {
        'border-color': token('--pick-1'), 'border-width': 4, 'opacity': 1}},
      {selector: 'node.pick-b', style: {
        'border-color': token('--pick-2'), 'border-width': 4, 'opacity': 1}},

      {selector: 'node.bdg', style: {
        'shape': 'ellipse', 'width': 26, 'height': 26,
        'border-color': '#0f172a', 'border-width': 2,
        'label': 'data(label)', 'color': '#0f172a',
        'font-family': fontStack('--sans', 'sans-serif'),
        'font-size': 15, 'font-weight': 700,
        'text-valign': 'center', 'text-halign': 'center',
        'events': 'no', 'z-index': 9}},
      {selector: 'node.bdg.p1', style: {'background-color': token('--pick-1')}},
      {selector: 'node.bdg.p2', style: {'background-color': token('--pick-2')}},

      {selector: 'node.hover', style: {
        'border-width': 3.5,
        'overlay-color': token('--hover-plain'),
        'overlay-opacity': parseFloat(token('--hover-veil')) || 0.16,
        'overlay-padding': 7,
        'overlay-corner-radius': 999}},
      {selector: 'node.t-si.hover', style: {'overlay-color': token('--hover-si')}},
      {selector: 'node.t-english.hover', style: {'overlay-color': token('--hover-english')}},
      {selector: 'node.preview', style: {
        'border-color': token('--pick-2'), 'border-width': 3.5}}
    ];
  }
  var PRESET = {name: 'preset', fit: false, animate: false};
