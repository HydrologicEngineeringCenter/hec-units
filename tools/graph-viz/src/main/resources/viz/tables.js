// This file mainly allows the tables in the summary page to be sorted
  function makeSortable(table) {
    var body = table.tBodies[0];
    var headers = table.querySelectorAll('thead th');
    if (!body || !headers.length) {
      return;
    }
    var original = Array.prototype.slice.call(body.rows);
    var sortedBy = -1;
    var direction = 0;                      // 0 none, 1 descending, 2 ascending

    // Decide whether a column is numeric or text
    function valueOf(row, column) {
      var cell = row.cells[column];
      if (!cell) {
        return '';
      }
      var text = cell.textContent.trim();
      var number = parseFloat(text.replace(/[^0-9.eE+-]/g, ''));
      return isNaN(number) ? text.toLowerCase() : number;
    }

    // Apply a sort, rebuild the table
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
