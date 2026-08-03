// Handler for other .js files
  var tabs = document.querySelectorAll('.tab');
  tabs.forEach(function (tab) {
    tab.addEventListener('click', function () {
      tabs.forEach(function (t) { t.classList.toggle('active', t === tab); });
      document.querySelectorAll('.tabpane').forEach(function (pane) {
        pane.classList.toggle('active', pane.id === tab.dataset.pane);
      });
    });
  });

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

  overlay.addEventListener('click', function (event) {
    if (event.target === overlay) {
      close();
    }
  });

  document.addEventListener('keydown', function (event) {
    if (event.key !== 'Escape') {
      return;
    }
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
      clearTimeout(resizeTimer);
      resizeTimer = setTimeout(function () {
        if (seedApi && lastCard) {
          var group = lastCard.querySelector('.cy').dataset.group;
          seedApi.destroy();
          stage.innerHTML = '<div id="ocy"></div>';
          seedApi = hydrateSeed(document.getElementById('ocy'), group);
        }
      }, 200);
      return;
    }
    fit();
  });
