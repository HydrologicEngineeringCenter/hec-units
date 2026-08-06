// The detail panel: how wide it is, and whether it is showing.
//
// The width is clamped against the overlay's own box rather than fixed pixels,
// so a long constant can never push the panel wider than the window it lives
// in, and a remembered width can never outgrow a smaller screen.

  var pnWrap = document.getElementById('ostagewrap');
  var pnPanel = document.getElementById('opanel');
  var pnSplit = document.getElementById('osplit');

  var PN_MIN = 260;                   // narrower than this and the steps wrap badly
  var PN_MAX_SHARE = 0.72;            // the stage must keep something to show
  var PN_DEFAULT = 430;

  var pnSize = PN_DEFAULT;

  function pnStore(key, value) {
    try {
      if (value === undefined) { return localStorage.getItem(key); }
      localStorage.setItem(key, value);
    } catch (e) { }
    return null;
  }

  function pnClamp(value) {
    var total = pnWrap.getBoundingClientRect().width;
    if (total < 1) {
      // The overlay is closed, so there is nothing to measure against. Clamping
      // now would shrink a remembered width to the minimum and keep it there;
      // the real clamp happens when the overlay opens.
      return Math.max(PN_MIN, Math.round(value));
    }
    var min = Math.min(PN_MIN, Math.max(120, total - 120));
    var max = Math.max(PN_MIN, Math.round(total * PN_MAX_SHARE));
    return Math.max(min, Math.min(max, Math.round(value)));
  }

  function pnSetSize(value, remember) {
    pnSize = pnClamp(value);
    pnPanel.style.setProperty('--pn', pnSize + 'px');
    if (remember !== false) {
      pnStore('viz-panel', pnSize);
    }
    pnRestage();
  }

  /* The stage changed shape, so whatever is drawn in it has to be told. The
     matrix recomputes its cell size; cytoscape has to be asked explicitly
     because it only watches the window, not its container. */
  var pnRestageFrame = null;

  function pnRestage() {
    if (pnRestageFrame) {
      cancelAnimationFrame(pnRestageFrame);
    }
    pnRestageFrame = requestAnimationFrame(function () {
      pnRestageFrame = null;
      if (typeof seedApi !== 'undefined' && seedApi && seedApi.resize) {
        seedApi.resize();
      } else if (typeof fit === 'function') {
        fit();
      }
    });
  }

  function pnDrag(event) {
    if (pnWrap.classList.contains('folded')) {
      return;
    }
    event.preventDefault();
    var right = pnWrap.getBoundingClientRect().right;
    pnWrap.classList.add('sizing');

    function move(moved) {
      var point = moved.touches ? moved.touches[0] : moved;
      pnSetSize(right - point.clientX);
    }

    function done() {
      pnWrap.classList.remove('sizing');
      document.removeEventListener('mousemove', move);
      document.removeEventListener('mouseup', done);
      document.removeEventListener('touchmove', move);
      document.removeEventListener('touchend', done);
    }

    document.addEventListener('mousemove', move);
    document.addEventListener('mouseup', done);
    document.addEventListener('touchmove', move, {passive: false});
    document.addEventListener('touchend', done);
  }

  /* Called when the overlay opens: only now does the wrapper have a width to
     clamp against, and only now is restaging meaningful. */
  function pnOnOpen() {
    if (pnWrap) {
      pnSetSize(pnSize, false);
    }
  }

  if (pnWrap && pnPanel && pnSplit) {
    var saved = parseInt(pnStore('viz-panel'), 10);
    if (saved > 0) { pnSize = saved; }
    if (pnStore('viz-panel-hidden') === '1') { pnWrap.classList.add('folded'); }

    pnSplit.addEventListener('mousedown', pnDrag);
    pnSplit.addEventListener('touchstart', pnDrag, {passive: false});
    pnSplit.addEventListener('dblclick', function () { pnSetSize(PN_DEFAULT); });

    // The splitter is a control, so it answers to the keyboard as well.
    pnSplit.addEventListener('keydown', function (event) {
      var step = event.shiftKey ? 64 : 16;
      if (event.key === 'ArrowLeft' || event.key === 'ArrowRight') {
        event.preventDefault();
        pnSetSize(pnSize + (event.key === 'ArrowLeft' ? step : -step));
      } else if (event.key === 'Home') {
        event.preventDefault();
        pnSetSize(PN_DEFAULT);
      }
    });

    var fold = document.getElementById('opanelfold');
    fold.addEventListener('click', function () {
      var folded = pnWrap.classList.toggle('folded');
      pnStore('viz-panel-hidden', folded ? '1' : '0');
      fold.setAttribute('aria-expanded', String(!folded));
      fold.title = folded ? 'Show the panel' : 'Hide the panel';
      fold.innerHTML = folded ? '&lsaquo;' : '&rsaquo;';
      pnRestage();
    });
    if (pnWrap.classList.contains('folded')) {
      fold.setAttribute('aria-expanded', 'false');
      fold.title = 'Show the panel';
      fold.innerHTML = '&lsaquo;';
    }

    // A window resize can invalidate the clamp, so re-apply it rather than
    // letting a stale pixel width outgrow the overlay.
    window.addEventListener('resize', function () {
      if (pnWrap.offsetParent !== null) { pnSetSize(pnSize, false); }
    });

    pnPanel.style.setProperty('--pn', pnSize + 'px');
  }
