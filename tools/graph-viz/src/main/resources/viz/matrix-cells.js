// File for interacting with cells within the enlarged Matrix
  function clearHover() {
    stage.querySelectorAll('.hi').forEach(function (element) {
      element.classList.remove('hi');
    });
  }

  // light up the cell, row label, and column header
  function highlight(cell) {
    clearHover();

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
    highlight(cell);
    if (!pinned) {
      show(detailFor(cell));
    }
  });

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
