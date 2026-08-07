package mil.army.usace.hec.graph.viz.view;

import java.util.List;

public final class SearchView {

    public record Option(String test, String label) {
    }

    public record Group(String label, List<Option> options) {
    }

    private static final String BAR = """
        <div class="toolbar" data-grid="{{grid}}">
        {{find}}
        {{filter}}
        <span class="count"></span>
        </div>
        """;

    private static final String FIND = """
        <label class="find{{size}}">
        <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="11" cy="11" r="7"/>
        <path d="M16.5 16.5 21 21"/></svg>
        <input type="search" id="{{id}}" placeholder="{{placeholder}}">
        <button type="button" class="clearfind" aria-label="clear">&times;</button>
        </label>
        """;

    private static final String FILTER = """
        <div class="filter"{{id}}>
        <button type="button" class="filterbtn" aria-expanded="false">
        <svg viewBox="0 0 24 24" aria-hidden="true"><path d="M4 6h16M7 12h10M10 18h4"/></svg>
        Filters<span class="fnum"></span>
        </button>
        <button type="button" class="fwipe" disabled
        title="Clear every filter" aria-label="Clear every filter">&times;</button>
        <div class="filtermenu" hidden>
        {{body}}
        <button type="button" class="fclear">Clear filters</button>
        </div>
        </div>
        """;

    private static final String GROUP = """
        <div class="fgroup"><div class="flbl">{{label}}</div>{{options}}</div>
        """;

    private static final String OPTION = """
        <label class="fopt"><input type="checkbox" data-test="{{test}}"><span>{{label}}</span></label>
        """;

    private static final String TAB = """
        <div class="tabpane" id="tab-find">
        <div class="wiki">
        <div class="modes">
        <button type="button" class="mode active" data-mode="conv">Conversions</button>
        <button type="button" class="mode" data-mode="unit">Units</button>
        </div>
        <div class="findbar" data-mode="conv">
        {{from}}<span class="arrow findsep">&rarr;</span>{{to}}
        </div>
        <div class="findbar" data-mode="unit" hidden>{{unit}}</div>
        <div class="findtools">{{filter}}<span class="count" id="wcount"></span></div>
        <div class="wikibody">
        <div class="resultcol">
        <div id="wlist" class="results"></div>
        <nav class="pager" id="wpage" hidden aria-label="Result pages">
        <button type="button" class="pgo" data-step="-1" aria-label="Previous results">
        &lsaquo;</button>
        <span class="pgat" id="wpageat" role="status"></span>
        <button type="button" class="pgo" data-step="1" aria-label="Next results">
        &rsaquo;</button>
        </nav>
        </div>
        <aside id="winfo" class="infopage"></aside>
        </div>
        </div>
        </div>
        """;

    private static final String FIND_FILTER = """
        <div class="fgroup"><div class="flbl">dimension</div>
        <select id="wdim"><option value="">any dimension</option></select></div>
        <div class="fpart" data-mode="conv">
        {{status}}
        {{kind}}
        <div class="fgroup"><div class="flbl">route length</div>
        <div class="frow">
        <select id="whopmode">
        <option value="any">any</option>
        <option value="eq">exactly</option>
        <option value="min">at least</option>
        <option value="max">at most</option>
        </select>
        <input type="number" id="whopn" min="1" max="30" value="1" disabled>
        <span class="funit">hops</span>
        </div>
        </div>
        </div>
        <div class="fpart" data-mode="unit" hidden>
        <div class="fgroup"><div class="flbl">system</div><div id="wsys"></div></div>
        {{coverage}}
        </div>
        """;

    private SearchView() {
    }

    public static String bar(String grid, String placeholder, List<Group> groups) {
        return Html.fill(BAR)
            .put("grid", grid)
            .raw("find", find(grid + "q", placeholder, ""))
            .raw("filter", filter(null, groups(groups)))
            .render();
    }

    public static String tab() {
        return Html.fill(TAB)
            .raw("from", find("wfrom", "From unit - blank for any", " big"))
            .raw("to", find("wto", "To unit - blank for any", " big"))
            .raw("unit", find("wunit", "Search units - try foot, Length, or acre", " big"))
            .raw("filter", filter("wfilter", Html.fill(FIND_FILTER)
                .raw("status", group(new Group("test result", List.of(
                    new Option("passed", "passed"),
                    new Option("failed", "failed"),
                    new Option("untested", "not tested"),
                    new Option("missing", "no conversion exists")))))
                .raw("kind", group(new Group("how it was made", List.of(
                    new Option("direct", "written by hand"),
                    new Option("derived", "derived by chaining"),
                    new Option("linear", "hand-written, linear"),
                    new Option("function", "hand-written, function")))))
                .raw("coverage", group(new Group("coverage", List.of(
                    new Option("hasfail", "has a failing conversion"),
                    new Option("hasuntested", "has an untested conversion"),
                    new Option("isolated", "converts to nothing")))))
                .render()))
            .render();
    }

    private static String find(String id, String placeholder, String size) {
        return Html.fill(FIND)
            .put("size", size)
            .put("id", id)
            .put("placeholder", placeholder)
            .render();
    }

    private static String filter(String id, String body) {
        return Html.fill(FILTER)
            .raw("id", id == null ? "" : " id=\"" + Html.escape(id) + "\"")
            .raw("body", body)
            .render();
    }

    private static String groups(List<Group> groups) {
        return Html.each(groups, SearchView::group);
    }

    private static String group(Group group) {
        return Html.fill(GROUP)
            .put("label", group.label())
            .raw("options", Html.each(group.options(), option -> Html.fill(OPTION)
                .put("test", option.test())
                .put("label", option.label())
                .render()))
            .render();
    }
}
