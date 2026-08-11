# Conversion visualizer

A build-time tool that turns the unit-conversion data and the test results into
one self-contained HTML page, so you can see at a glance which conversions
exist, which are tested, and which disagree with each other.

It is a debugging and design aid. Nothing in `units/` depends on it.

---

## Running it

```bash
./gradlew :units-graph-viz:vizServe
```

That runs the test suite, regenerates the page and serves it on a random port,
printing the address to open. `Ctrl+C` stops the server.

To generate the page without serving it:

```bash
./gradlew :units-graph-viz:visualize
```

The output is a single file:

```
tools/units-graph-viz/build/reports/viz/index.html
```

### If you are working over SSH

The page is built on the remote machine but your browser is local, so
`file://` will not find it. Pin the port so the tunnel can name it:

```bash
./gradlew :units-graph-viz:vizServe -PvizPort=8080
ssh -L 8080:localhost:8080 you@remote
```

`VizServer` binds to loopback only and refuses paths outside its root.

---

## What the page shows

### Coverage

One matrix per dimension. Each row converts into the columns, so the cell at
row `ft`, column `m` is the conversion `ft → m`.

| Color | Meaning |
|---|---|
| green | a test covers this conversion and it passed |
| red | a test covers it and it failed |
| gray | the algorithm can produce it, but no test exercises it |
| pale | no conversion exists between these units |

The number in a cell is how many hops the chosen route takes.

**Click a card** to enlarge it. In the enlarged view, hovering a cell shows the
conversion in plain English, the equation, the hop chain and every test that
touched it. **Click a cell** to pin that panel so it stays put while you move
around. Click it again to release.

A pinned cell also offers **Show every route**, which lists all the ways to get
between those two units and flags any that disagree with the shortest one.
Disagreement means a conversion somewhere along one of those routes is wrong.

### Conversion graphs

The same data drawn as graphs, one per dimension, showing only the conversions
written by hand in `conversions.json`, before the algorithm derives anything
from them.

- Node color is the unit system: SI, English, or system-agnostic.
- A solid edge is a `linear:` conversion, a dashed edge is a `function:` one.
- The badge on each card says whether the graph is a tree, has cycles, or has
  duplicate edges.

**Click a card** to enlarge it. Then:

- **drag** a unit to untangle the drawing, **scroll** to zoom, **drag the
  background** to pan
- **click an edge** for the conversion exactly as it was authored, constants
  and all
- **click two units** to list every route between them, with the composed
  factor for each. Hovering a route lights it up on the graph.

If the routes disagree, the panel says so and color-codes them by the answer
they produce. That is the fastest way to find a bad constant.

### Summary

The **Summary** button opens a whole-project read-out: overall coverage, pass
rate, a breakdown of every conversion slot, per-dimension coverage, how long
the chosen routes are, and a "worth a look" section listing failures, untested
dimensions and unreachable units. Table headings sort.

---

## Tests

```bash
./gradlew :graph-viz:test :units-graph-viz:test
```