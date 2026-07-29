/*
 * Click-to-enlarge for the coverage matrices.
 *
 * The trick here is that nothing is re-rendered: the overlay reuses the exact
 * table already sitting in the card, just cloned into a container where it can
 * be drawn much larger. That means no graph data has to be serialised into the
 * page and no matrix-building logic exists twice. Each cell already carries its
 * own description in a data attribute, so selecting one needs no lookup either.
 */
(function () {
  var overlay = document.getElementById('overlay');
  var stage = document.getElementById('ostage');
  var otitle = document.getElementById('otitle');
  var otally = document.getElementById('otally');
  var odetail = document.getElementById('odetail');

  var HINT = '<div class="empty">Hover a cell to preview its conversion. '
           + '<b>Click</b> to pin it, click the same cell again to release it.</div>';

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

  /*
   * Pick a cell size that fills the available space.
   *
   * A fixed size cannot work: most dimensions have only 2-4 units, so a size
   * chosen to keep the 13x13 matrix on screen leaves the small ones as a
   * postage stamp in the middle of a large empty panel. So measure the stage
   * and divide what is left after the labels.
   */
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
    var forRowLabels = 160;
    var forColumnLabels = 46;
    var padding = 48;

    var perColumn = (box.width - forRowLabels - padding) / columns;
    var perRow = (box.height - forColumnLabels - padding) / rows;

    // Minus the 2px border-spacing that sits between every pair of cells.
    var size = Math.floor(Math.min(perColumn, perRow)) - 3;

    // Floor keeps a 13-column matrix legible on a small window (it scrolls
    // instead of shrinking to nothing). Ceiling stops a 2x2 from becoming two
    // absurd slabs filling half the screen.
    size = Math.max(20, Math.min(size, 120));

    table.style.setProperty('--cell', size + 'px');
  }

  function detailFor(cell) {
    return cell.dataset.detail
        || '<div class="empty">' + cell.getAttribute('title') + '</div>';
  }

  function show(html) {
    odetail.innerHTML = html;
    // Drives the "pinned" badge in CSS, so the panel says why it is not
    // responding to the cursor.
    odetail.classList.toggle('locked', pinned !== null);
  }

  function open(card) {
    otitle.textContent = card.querySelector('h2').textContent;
    otally.innerHTML = card.querySelector('.tally').innerHTML;
    stage.innerHTML = card.querySelector('table').outerHTML;
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

  // Clicking the backdrop closes; clicking anything inside it must not.
  overlay.addEventListener('click', function (event) {
    if (event.target === overlay) {
      close();
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key === 'Escape') {
      close();
    }
  });

  window.addEventListener('resize', function () {
    if (overlay.classList.contains('open')) {
      fit();
    }
  });

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
