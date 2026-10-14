#!/usr/bin/env python3
"""
Gherkin Spec Viewer — generates a self-contained HTML page from .feature files,
with step‑definition look‑up in Kotlin Cucumber sources.

Features:
  · Syntax-highlighted step lines (Given / When / Then / And / But / *).
  · Sidebar index of features with scroll-tracking highlight.
  · Click any step to see it in a detail panel — shows the Gherkin step
    AND the matching Kotlin Cucumber step‑definition code side‑by‑side.
  · Foldable Rule and Scenario sections (collapsible via <details>).
  · Collapse / expand all rules and scenarios with one click.
  · Dark theme, responsive layout, self‑contained (no external deps).

Usage:
    python3 spec-viewer.py [--features-dir DIR] [--output FILE]
                          [--exclude PATTERN ...]
                          [--step-defs DIR ...] [--all] [--open]
"""

import argparse
import glob
import os
import re
import sys
import textwrap
import webbrowser

# ═══════════════════════════════════════════════════════════════════════════
#  Gherkin feature-file parser
# ═══════════════════════════════════════════════════════════════════════════

_RULE_RE = re.compile(r"^\s*Rule:\s*(.*)")
_EXAMPLE_RE = re.compile(r"^\s*(?:Scenario|Example):\s*(.*)")
_BACKGROUND_RE = re.compile(r"^\s*Background:\s*(.*)")
_FEATURE_RE = re.compile(r"^\s*Feature:\s*(.*)")
_TAG_RE = re.compile(r"^\s*@(\S+)")
_DOCSTRING_BOUNDARY = re.compile(r"^\s*\"{3}")


def _keyword_hint(line: str) -> str | None:
    m = re.match(r"^\s*(Feature|Background|Rule|Scenario|Example):\s*", line)
    if m:
        return m.group(1)
    m = re.match(r"^\s*(Given|When|Then|And|But)\b", line)
    if m:
        return m.group(1)
    return None


class _GherkinDoc:
    """Parsed .feature file."""

    def __init__(self, source: str):
        self.source = source
        self.filename = os.path.basename(source)
        self.feature_name = ""
        self.description: list[str] = []
        self.tags: list[str] = []
        self.children: list["_Section"] = []


class _Section:
    BACKGROUND = "Background"
    RULE = "Rule"
    EXAMPLE = "Example"

    def __init__(self, kind: str, title: str = ""):
        self.kind = kind
        self.title = title
        self.description: list[str] = []
        self.tags: list[str] = []
        self.body: list[str] = []
        self.examples: list["_Section"] = []


def parse_features(directory: str, exclude: list[str]) -> list[_GherkinDoc]:
    docs: list[_GherkinDoc] = []
    for fpath in sorted(glob.glob(os.path.join(directory, "*.feature"))):
        if any(p in fpath for p in exclude):
            continue
        with open(fpath, encoding="utf-8") as fh:
            text = fh.read()
        doc = _parse_single(fpath, text)
        if doc is not None:
            docs.append(doc)
    return docs


def _parse_single(path: str, text: str) -> _GherkinDoc | None:
    lines = text.splitlines()
    doc = _GherkinDoc(path)
    current: _Section | None = None
    cur_ex: _Section | None = None
    in_ds = False
    ds_buf: list[str] = []

    def flush():
        nonlocal current, cur_ex
        if current is not None:
            doc.children.append(current)
            current = None
            cur_ex = None

    i = 0
    while i < len(lines):
        raw = lines[i]
        s = raw.strip()

        if _DOCSTRING_BOUNDARY.match(s):
            if in_ds:
                in_ds = False
                ds_buf.append(raw)
                t = cur_ex if cur_ex is not None else current
                if t is not None:
                    t.body.extend(ds_buf)
                ds_buf.clear()
            else:
                in_ds = True
                ds_buf = [raw]
            i += 1
            continue
        if in_ds:
            ds_buf.append(raw)
            i += 1
            continue

        if not s or s.startswith("#"):
            t = cur_ex if cur_ex is not None else current
            if t is not None:
                t.body.append(raw)
            elif not doc.children:
                doc.description.append(raw)
            i += 1
            continue

        m = _TAG_RE.match(s)
        if m:
            tag = m.group(1)
            if cur_ex is not None:
                cur_ex.tags.append(tag)
            elif current is not None:
                current.tags.append(tag)
            else:
                doc.tags.append(tag)
            i += 1
            continue

        m = _FEATURE_RE.match(raw)
        if m:
            flush()
            doc.feature_name = m.group(1).strip()
            i += 1
            continue

        m = _BACKGROUND_RE.match(raw)
        if m:
            flush()
            current = _Section(_Section.BACKGROUND, m.group(1).strip())
            i += 1
            continue

        m = _RULE_RE.match(raw)
        if m:
            flush()
            current = _Section(_Section.RULE, m.group(1).strip())
            cur_ex = None
            i += 1
            continue

        m = _EXAMPLE_RE.match(raw)
        if m:
            if current is not None and current.kind == _Section.RULE:
                ex = _Section(_Section.EXAMPLE, m.group(1).strip())
                if current.tags:
                    ex.tags = list(current.tags)
                current.examples.append(ex)
                cur_ex = ex
            else:
                flush()
                current = _Section(_Section.EXAMPLE, m.group(1).strip())
                cur_ex = current
            i += 1
            continue

        t = cur_ex if cur_ex is not None else current
        if t is not None:
            t.body.append(raw)
        elif not doc.children:
            doc.description.append(raw)
        i += 1

    flush()
    return doc


# ═══════════════════════════════════════════════════════════════════════════
#  Kotlin step‑definition parser
# ═══════════════════════════════════════════════════════════════════════════

_CUCUMBER_ANNOTATION = re.compile(
    r'@(Given|When|Then)\(\s*"((?:[^"\\]|\\.)*)"\s*\)'
)

# Cucumber expression parameter → regex group
_CUCUMBER_PARAM = {
    "string": r'"((?:[^"\\]|\\.)*)"',
    "int": r"(-?\d+)",
    "float": r"(-?\d+\.\d+)",
    "word": r"(\S+)",
}


def _cucumber_pattern_to_regex(pattern: str) -> re.Pattern:
    """Convert a Cucumber expression (with {string}, {int}, …) to a compiled regex."""
    parts: list[str] = []
    pos = 0
    for m in re.finditer(r"\{(\w+)\}", pattern):
        # literal text before this parameter
        if m.start() > pos:
            parts.append(re.escape(pattern[pos : m.start()]))
        param = m.group(1)
        if param in _CUCUMBER_PARAM:
            parts.append(_CUCUMBER_PARAM[param])
        else:
            # unknown parameter – match non-whitespace
            parts.append(r"(\S+)")
        pos = m.end()
    if pos < len(pattern):
        parts.append(re.escape(pattern[pos:]))
    return re.compile("^" + "".join(parts) + "$")


class StepDef:
    """A single Cucumber step‑definition."""

    def __init__(self, keyword: str, pattern: str, filepath: str, line_num: int, code: str):
        self.keyword = keyword  # Given / When / Then
        self.pattern = pattern
        self.regex = _cucumber_pattern_to_regex(pattern)
        self.filepath = filepath
        self.line_num = line_num
        self.code = code  # full Kotlin function text (including the annotation line)

    def match(self, step_text: str) -> bool:
        """Return True if *step_text* matches this step‑definition (keyword‑insensitive)."""
        return bool(self.regex.match(step_text))


def parse_step_definitions(dirs: list[str]) -> list[StepDef]:
    """Scan Kotlin files under *dirs* for Cucumber step definitions."""
    defs: list[StepDef] = []
    seen_signatures: set[tuple[str, int]] = set()
    for d in dirs:
        if not os.path.isdir(d):
            continue
        for fpath in sorted(glob.glob(os.path.join(d, "**", "*.kt"), recursive=True)):
            _extract_defs_from_file(fpath, defs, seen_signatures)
    return defs


def _extract_defs_from_file(fpath: str, defs: list[StepDef], seen: set):
    with open(fpath, encoding="utf-8") as fh:
        lines = fh.readlines()

    i = 0
    while i < len(lines):
        line = lines[i]
        m = _CUCUMBER_ANNOTATION.search(line)
        if not m:
            i += 1
            continue

        keyword = m.group(1)   # Given / When / Then
        pattern = m.group(2)   # the text inside the quotes

        # Detect the function definition – may be on the same line or the next
        func_start = i
        j = i
        if "fun " not in lines[j]:
            j += 1
        if j >= len(lines) or "fun " not in lines[j]:
            i += 1
            continue

        func_line = j
        fline = lines[func_line]
        # Expression-bodied function:  fun foo() = expr  (no braces needed)
        if re.search(r"\bfun\b.*=", fline) and "{" not in fline:
            # entire function is on one line
            code_lines = [lines[func_start]] + [fline]
            code = "".join(code_lines).rstrip()
            sig = (fpath, func_line)
            if sig not in seen:
                seen.add(sig)
                defs.append(StepDef(keyword, pattern, fpath, func_line + 1, code))
            i = func_line + 1
            continue

        # Collect function body (match braces)
        brace_depth = 0
        started = False
        end = func_line
        for k in range(func_line, len(lines)):
            end = k
            for ch in lines[k]:
                if ch == "{":
                    brace_depth += 1
                    started = True
                elif ch == "}":
                    brace_depth -= 1
            if started and brace_depth == 0:
                break

        code_lines = lines[func_start : end + 1]
        code = "".join(code_lines).rstrip()

        sig = (fpath, func_line)
        if sig not in seen:
            seen.add(sig)
            defs.append(StepDef(keyword, pattern, fpath, func_line + 1, code))
        i = end + 1


# ═══════════════════════════════════════════════════════════════════════════
#  Step‑definition matching
# ═══════════════════════════════════════════════════════════════════════════

_STEP_KEYWORD_RE = re.compile(r"^(Given|When|Then|And|But|\*)\s+(.*)")


def find_step_defs(step_text: str, defs: list[StepDef]) -> list[StepDef]:
    """Return all step definitions matching *step_text* (ordered by closeness).

    Gherkin steps include the keyword prefix (Given/When/Then/And/But/*)
    but Cucumber step definition patterns do not, so we strip it first.
    """
    m = _STEP_KEYWORD_RE.match(step_text)
    if m:
        step_text = m.group(2)
    matches = [d for d in defs if d.match(step_text)]
    # Sort: longer (more specific) patterns first, then by line number
    matches.sort(key=lambda d: (-len(d.pattern), d.line_num))
    return matches


# ═══════════════════════════════════════════════════════════════════════════
#  HTML generation helpers
# ═══════════════════════════════════════════════════════════════════════════

_ESC_T = str.maketrans({"&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;"})


def _escape(text: str) -> str:
    return text.translate(_ESC_T)


def _highlight_gherkin(line: str) -> str:
    """Return an HTML snippet with <span> highlighting for Gherkin syntax."""
    s = line.lstrip()
    indent = line[: len(line) - len(s)]
    esc = _escape(s)

    if re.match(r"^@", s):
        return f'<span class="kw-tag">{indent}{esc}</span>'
    if s.startswith("#"):
        return f'<span class="kw-comment">{indent}{esc}</span>'

    m = re.match(r"^(Feature|Background|Rule|Scenario|Example):\s*(.*)", s)
    if m:
        kw = m.group(1)
        return f'{indent}<span class="kw-{kw.lower()}">{kw}:</span> {_escape(m.group(2))}'

    m = re.match(r"^(Given|When|Then|And|But)\b(.*)", s)
    if m:
        kw = m.group(1)
        return f'{indent}<span class="kw-{kw.lower()}">{kw}</span> {_escape(m.group(2))}'

    if s.startswith("|"):
        cells = re.findall(r"\|([^|]*?)\|?", s)
        parts = ["|"]
        for c in cells:
            parts.append(f'<span class="kw-table">{_escape(c)}</span>|')
        return indent + "".join(parts)

    if s.startswith('"""'):
        return f'{indent}<span class="kw-docstring">{esc}</span>'
    return indent + esc


def _highlight_kotlin(kotlin_code: str) -> str:
    """Minimal Kotlin syntax highlighting for display in HTML."""
    esc = _escape(kotlin_code)
    # Annotations (@Given, @When, @Then, @Before, etc.)
    esc = re.sub(r"(@\w+)", r'<span class="kw-annot">\1</span>', esc)
    # Keywords
    kws = [
        r"\bfun\b", r"\bclass\b", r"\bprivate\b", r"\bpublic\b",
        r"\bval\b", r"\bvar\b", r"\blet\b", r"\bif\b", r"\belse\b",
        r"\bfor\b", r"\bwhile\b", r"\breturn\b", r"\bcheck\b",
        r"\bimport\b", r"\bpackage\b", r"\blateinit\b", r"\boverride\b",
        r"\bdata\b", r"\bmap\b", r"\blist\b", r"\bset\b", r"\btry\b",
        r"\bcatch\b", r"\bfinally\b", r"\bthrow\b", r"\berror\b",
        r"\btrue\b", r"\bfalse\b", r"\bnull\b", r"\bin\b",
    ]
    for kw in kws:
        esc = re.sub(kw, lambda m: f'<span class="kw-kotlin">{m.group(0)}</span>', esc)
    # String literals
    esc = re.sub(r'"(?:[^"\\]|\\.)*"', lambda m: f'<span class="kw-str">{_escape(m.group(0))}</span>', esc)
    # Comments
    esc = re.sub(r"//.*", lambda m: f'<span class="kw-comment">{_escape(m.group(0))}</span>', esc)
    return esc


def _counter():
    c = [0]
    return lambda: (c.__setitem__(0, c[0] + 1), c[0])[1]


# ═══════════════════════════════════════════════════════════════════════════
#  HTML builder
# ═══════════════════════════════════════════════════════════════════════════


def build_html(docs: list[_GherkinDoc], step_defs: list[StepDef]) -> str:
    next_id = _counter()
    sidebar_items: list[str] = []
    main_sections: list[str] = []

    # Pre‑compute step definition lookup: step text → list of StepDefs
    # We'll match lazily inside the render loop below (but cache to avoid repeats).
    _step_cache: dict[str, list[StepDef]] = {}

    def _lookup_step(text: str) -> list[StepDef]:
        if text not in _step_cache:
            _step_cache[text] = find_step_defs(text, step_defs)
        return _step_cache[text]

    for doc in docs:
        fid = f"f-{next_id()}"
        fname = doc.feature_name or doc.filename
        sidebar_items.append(
            f'<li><a href="#{fid}" class="feature-link">'
            f'<span class="feat-icon">&#x1F4CB;</span> {_escape(fname)}</a></li>'
        )

        parts: list[str] = [
            f'<section id="{fid}" class="feature"'
            f' data-feature="{_escape(fname)}">',
            '<h2 class="feature-title">'
            f'<span class="feat-badge">Feature:</span> {_escape(fname)}</h2>',
        ]

        if doc.tags:
            parts.append(
                '<div class="tags-line">'
                + " ".join(f'<span class="tag">{_escape(t)}</span>' for t in doc.tags)
                + "</div>"
            )

        desc = [p.strip() for p in doc.description if p.strip()]
        if desc:
            parts.append(
                '<div class="feature-desc">'
                + "".join(f"<p>{_escape(l)}</p>" for l in desc)
                + "</div>"
            )

        for child in doc.children:
            parts.append(_render_section(child, next_id, fname, "", _lookup_step))

        parts.append("</section>")
        main_sections.append("\n".join(parts))

    # ── Sidebar ──────────────────────────────────────────────────
    sidebar_html = (
        '<nav id="sidebar">'
        '<div class="sidebar-header">'
        "<h3>Features</h3>"
        '<button id="collapse-all" title="Collapse all">&#x229F;</button>'
        "</div>"
        '<ul id="feature-list">' + "\n".join(sidebar_items) + "</ul>"
        "</nav>"
    )

    # ── Main content ─────────────────────────────────────────────
    summary = f"{len(docs)} feature{'s' if len(docs)!=1 else ''}"
    if step_defs:
        summary += f" · {len(step_defs)} step definition{'s' if len(step_defs)!=1 else ''}"
    main_html = (
        '<main id="content">'
        f"<h1>Gherkin Spec Viewer <span class='summary'>{summary}</span></h1>"
        + "\n".join(main_sections)
        + "</main>"
    )

    # ── Step‑detail panel ───────────────────────────────────────
    detail_html = (
        '<div id="step-detail" class="step-detail-panel">'
        '<div class="step-detail-header">'
        '<span class="detail-title">Step</span>'
        '<span class="detail-context" id="step-detail-context"></span>'
        '<span id="step-detail-close-panel-info">'
        '  <span class="detail-nav-hint">click step to select &middot; click outside to close</span>'
        "</span>"
        '<button id="step-detail-close">&times;</button>'
        "</div>"
        # Gherkin line
        '<div class="detail-section-label">Gherkin</div>'
        '<pre id="step-detail-code" class="highlighted-code"></pre>'
        # Kotlin implementation
        '<div class="detail-section-label stepdef-label">Implementation</div>'
        '<div id="step-detail-stepdef" class="stepdef-container">'
        '<pre id="step-detail-stepdef-code" class="highlighted-code"></pre>'
        '<p id="step-detail-stepdef-meta" class="stepdef-meta"></p>'
        "</div>"
        "</div>"
    )

    css = _css()
    js = _javascript()

    return (
        "<!DOCTYPE html>\n"
        '<html lang="en">\n'
        "<head>\n"
        '<meta charset="utf-8">\n'
        '<meta name="viewport" content="width=device-width, initial-scale=1">\n'
        "<title>Gherkin Spec Viewer</title>\n"
        f"<style>\n{css}\n</style>\n"
        "</head>\n"
        "<body>\n"
        '<div id="app">\n'
        f"{sidebar_html}\n"
        f"{main_html}\n"
        f"{detail_html}\n"
        "</div>\n"
        f"<script>\n{js}\n</script>\n"
        "</body>\n"
        "</html>"
    )


def _render_section(
    child: _Section, next_id, feature_name: str, rule_title: str,
    lookup_step,
) -> str:
    parts: list[str] = []

    if child.kind == _Section.BACKGROUND:
        bid = f"s-{next_id()}"
        body = "".join(
            _render_step_line(l, next_id, feature_name, rule_title, "Background", lookup_step)
            for l in child.body
            if l.strip()
        )
        parts.append(
            '<details class="section-background"'
            f' id="{bid}" open>'
            '<summary class="section-summary background-summary">'
            '<span class="badge background-badge">Background</span>'
            "</summary>"
            f'<div class="section-body">{body}</div>'
            "</details>"
        )

    elif child.kind == _Section.RULE:
        rid = f"s-{next_id()}"
        desc = [l.strip() for l in child.description if l.strip()]
        desc_html = ""
        if desc:
            desc_html = (
                '<div class="rule-desc">'
                + "".join(f"<p>{_escape(l)}</p>" for l in desc)
                + "</div>"
            )
        ex_html = "".join(
            _render_example(ex, next_id, feature_name, child.title, lookup_step)
            for ex in child.examples
        )
        parts.append(
            f'<details class="section-rule" id="{rid}"'
            f' data-rule="{_escape(child.title)}">'
            '<summary class="section-summary rule-summary">'
            f'<span class="badge rule-badge">Rule:</span> {_escape(child.title)}'
            "</summary>"
            f'<div class="section-body">{desc_html}{ex_html}</div>'
            "</details>"
        )

    elif child.kind == _Section.EXAMPLE:
        parts.append(
            _render_example(child, next_id, feature_name, rule_title, lookup_step)
        )

    return "\n".join(parts)


def _render_example(
    ex: _Section, next_id, feature_name: str, rule_title: str, lookup_step,
) -> str:
    eid = f"e-{next_id()}"
    tags_html = ""
    if ex.tags:
        tags_html = (
            '<div class="tags-line">'
            + " ".join(f'<span class="tag">{_escape(t)}</span>' for t in ex.tags)
            + "</div>"
        )

    scenario_title = ex.title
    steps_html = "".join(
        _render_step_line(l, next_id, feature_name, rule_title, scenario_title, lookup_step)
        for l in ex.body
        if l.strip() and not l.strip().startswith("#")
    )

    return (
        '<details class="section-example"'
        f' id="{eid}">'
        '<summary class="section-summary example-summary">'
        f'<span class="badge example-badge">Scenario:</span> {_escape(scenario_title)}'
        "</summary>"
        '<div class="section-body example-body">'
        f"{tags_html}{steps_html}"
        "</div>"
        "</details>"
    )


def _render_step_line(
    line: str, next_id, feature_name: str, rule_title: str,
    scenario_title: str, lookup_step,
) -> str:
    stripped = line.strip()
    if not stripped:
        return ""
    step_id = f"st-{next_id()}"
    highlighted = _highlight_gherkin(line)
    kw = _keyword_hint(line)
    step_type = (kw or "other").lower()

    # Find matching Kotlin step definitions
    matching_defs = lookup_step(stripped)

    data_feature = _escape(feature_name)
    data_rule = _escape(rule_title)
    data_scenario = _escape(scenario_title)
    data_step = _escape(stripped)
    data_stepdef = _escape(matching_defs[0].code) if matching_defs else ""
    data_stepdef_file = _escape(matching_defs[0].filepath) if matching_defs else ""
    data_stepdef_line = str(matching_defs[0].line_num) if matching_defs else "0"

    return (
        '<pre'
        f' id="{step_id}"'
        f' class="step-line step-{step_type}"'
        f' data-step="{data_step}"'
        f' data-feature="{data_feature}"'
        f' data-rule="{data_rule}"'
        f' data-scenario="{data_scenario}"'
        f' data-stepdef="{data_stepdef}"'
        f' data-stepdef-file="{data_stepdef_file}"'
        f' data-stepdef-line="{data_stepdef_line}"'
        f'>{highlighted}</pre>\n'
    )


# ═══════════════════════════════════════════════════════════════════════════
#  CSS
# ═══════════════════════════════════════════════════════════════════════════


def _css() -> str:
    return textwrap.dedent("""\
    :root {
      --bg: #1e1e2e;
      --surface: #252538;
      --border: #3a3a50;
      --text: #cdd6f4;
      --text-muted: #a0a8c0;
      --accent: #7aa2f7;
      --accent2: #c0a0e0;
      --sidebar-bg: #1a1a28;
      --sidebar-fg: #cdd6f4;
      --code-bg: #1e1e30;
      --detail-bg: #2a2a40;
      --hover: #3a3a55;

      --kw-feature: #c0a0e0;
      --kw-background: #7fb0d0;
      --kw-rule: #80c0a0;
      --kw-scenario: #d0b080;
      --kw-given: #70c090;
      --kw-when: #c08060;
      --kw-then: #7090c0;
      --kw-and: #90b070;
      --kw-but: #c07080;
      --kw-tag: #888;
      --kw-comment: #666;
      --kw-table: #a0a0b0;
      --kw-docstring: #70a080;
      --kw-annot: #e0a0a0;
      --kw-kotlin: #80b0d0;
      --kw-str: #b0c070;
    }

    * { margin:0; padding:0; box-sizing:border-box; }

    body {
      font-family: "Segoe UI", system-ui, -apple-system, sans-serif;
      background: var(--bg);
      color: var(--text);
      line-height: 1.5;
      overflow: hidden;
      height: 100vh;
    }

    ::-webkit-scrollbar { width: 8px; }
    ::-webkit-scrollbar-track { background: var(--bg); }
    ::-webkit-scrollbar-thumb { background: var(--border); border-radius: 4px; }
    ::-webkit-scrollbar-thumb:hover { background: #555; }

    #app { display:flex; height:100vh; overflow:hidden; }

    /* ── Sidebar ────────────────────────────────────────────────── */

    #sidebar {
      width: 260px;
      min-width: 200px;
      background: var(--sidebar-bg);
      border-right: 1px solid var(--border);
      display: flex;
      flex-direction: column;
      overflow-y: auto;
      flex-shrink: 0;
    }

    .sidebar-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 12px 16px;
      border-bottom: 1px solid var(--border);
      background: var(--surface);
      position: sticky;
      top: 0; z-index: 2;
    }

    .sidebar-header h3 {
      font-size: 0.95rem;
      font-weight: 600;
      color: var(--sidebar-fg);
    }

    #collapse-all {
      background: transparent;
      border: none;
      color: var(--text-muted);
      font-size: 1rem;
      cursor: pointer;
      padding: 2px 8px;
      border-radius: 4px;
    }
    #collapse-all:hover { background: var(--hover); color: var(--text); }

    #feature-list { list-style:none; padding:4px 0; }

    #feature-list li a {
      display:flex; align-items:center; gap:6px;
      padding:8px 16px;
      color: var(--sidebar-fg);
      text-decoration:none;
      font-size:0.85rem;
      border-left:2px solid transparent;
      transition: border-color 0.15s, background 0.1s;
    }
    #feature-list li a:hover {
      background: var(--hover);
      border-left-color: var(--accent2);
    }
    #feature-list li a.active {
      border-left-color: var(--accent);
      color: var(--text);
      background: rgba(122,162,247,0.08);
    }
    .feat-icon { font-size:0.9rem; opacity:0.7; }

    /* ── Main ────────────────────────────────────────────────────── */

    #content { flex:1; overflow-y:auto; padding:20px 28px; }

    #content h1 {
      font-size:1.3rem; font-weight:700; margin-bottom:20px;
      color: var(--accent);
      border-bottom:1px solid var(--border);
      padding-bottom:10px;
      display:flex; align-items:baseline; gap:12px;
    }
    .summary { font-size:0.75rem; font-weight:400; color:var(--text-muted); }

    /* ── Feature ──────────────────────────────────────────────────── */

    .feature { margin-bottom:24px; scroll-margin-top:12px; }

    .feature-title {
      font-size:1.15rem; font-weight:600; color:var(--text);
      margin:0 0 6px 0; padding:8px 0;
    }

    .feat-badge {
      display:inline-block;
      background:var(--kw-feature); color:#000;
      font-weight:700; font-size:0.7rem;
      padding:0 8px; border-radius:4px;
      margin-right:6px; opacity:0.85;
    }

    .feature-desc p { color:var(--text-muted); margin:4px 0; font-size:0.9rem; }

    .tags-line { display:flex; flex-wrap:wrap; gap:4px; margin:4px 0 8px; }
    .tag {
      display:inline-block; background:#333; color:var(--kw-tag);
      font-size:0.7rem; padding:0 8px; border-radius:3px; font-family:monospace;
    }

    /* ── Sections ────────────────────────────────────────────────── */

    .section-background,
    .section-rule,
    .section-example {
      margin:8px 0; border-radius:6px;
      background:var(--surface);
      border:1px solid var(--border);
    }
    .section-background { border-left:3px solid var(--kw-background); }
    .section-rule       { border-left:3px solid var(--kw-rule); }
    .section-example    { border-left:3px solid var(--kw-scenario); }

    .section-summary {
      padding:8px 12px; font-weight:500;
      cursor:pointer; user-select:none;
      display:flex; align-items:center; gap:6px;
      list-style:none;
    }
    .section-summary::-webkit-details-marker { display:none; }

    .section-summary::before {
      content:"\\u25B6";
      font-size:0.75rem; color:var(--text-muted);
      margin-right:8px; transition:transform 0.15s;
    }
    details[open] > .section-summary::before { content:"\\u25BC"; }

    .section-summary:hover { background:var(--hover); }

    .badge {
      display:inline-block; font-weight:700; font-size:0.68rem;
      padding:0 8px; border-radius:3px; margin-right:4px;
    }
    .background-badge { background:var(--kw-background); color:#000; }
    .rule-badge       { background:var(--kw-rule); color:#000; }
    .example-badge    { background:var(--kw-scenario); color:#000; }

    .section-body { padding:6px 12px 10px; }
    .rule-desc p { color:var(--text-muted); font-size:0.88rem; margin:4px 0 8px; }

    /* ── Steps ────────────────────────────────────────────────────── */

    .step-line {
      display:block;
      font-family:"JetBrains Mono","Fira Code","Cascadia Code",monospace;
      font-size:0.82rem; line-height:1.55;
      padding:2px 8px; margin:2px 0;
      background:var(--code-bg);
      border-radius:3px; cursor:pointer;
      transition: outline 0.08s, background 0.08s;
      white-space:pre-wrap; word-break:break-word;
    }
    .step-line:hover {
      background:var(--hover);
      outline:1px solid var(--accent);
    }
    .step-line.active {
      background:#2d3d55;
      outline:2px solid var(--accent);
    }

    .step-line .kw-feature   { color:var(--kw-feature); font-weight:600; }
    .step-line .kw-background { color:var(--kw-background); font-weight:600; }
    .step-line .kw-rule      { color:var(--kw-rule); font-weight:600; }
    .step-line .kw-scenario,
    .step-line .kw-example   { color:var(--kw-scenario); font-weight:600; }
    .step-line .kw-given     { color:var(--kw-given); font-weight:600; }
    .step-line .kw-when      { color:var(--kw-when); font-weight:600; }
    .step-line .kw-then      { color:var(--kw-then); font-weight:600; }
    .step-line .kw-and       { color:var(--kw-and); font-weight:600; }
    .step-line .kw-but       { color:var(--kw-but); font-weight:600; }
    .step-line .kw-tag       { color:var(--kw-tag); }
    .step-line .kw-comment   { color:var(--kw-comment); font-style:italic; }
    .step-line .kw-table     { color:var(--kw-table); }
    .step-line .kw-docstring { color:var(--kw-docstring); }

    /* ── Step detail panel ────────────────────────────────────────── */

    .step-detail-panel {
      position:fixed;
      bottom:0; right:28px;
      width:580px;
      max-width:92vw;
      max-height:65vh;
      background:var(--detail-bg);
      border:1px solid var(--border);
      border-radius:8px 8px 0 0;
      box-shadow:0 -4px 20px rgba(0,0,0,0.4);
      z-index:100;
      display:none;
      overflow-y:auto;
      flex-direction:column;
    }
    .step-detail-panel.open { display:flex; }

    .step-detail-header {
      display:flex; align-items:center; gap:6px;
      padding:8px 14px;
      background:var(--surface);
      border-bottom:1px solid var(--border);
      font-weight:600; font-size:0.85rem;
      flex-shrink:0;
    }

    .detail-title { color:var(--text); font-weight:600; }
    .detail-context { color:var(--text-muted); font-size:0.78rem; margin-left:8px; }
    .detail-nav-hint { color:var(--text-muted); font-size:0.7rem; opacity:0.6; }

    #step-detail-close-panel-info {
      margin-left:auto;
      display:flex; align-items:center; gap:8px;
    }

    #step-detail-close {
      background:transparent; border:none;
      color:var(--text-muted); font-size:1.2rem;
      cursor:pointer; padding:0 6px;
    }
    #step-detail-close:hover { color:var(--text); }

    .detail-section-label {
      display:flex; align-items:center;
      padding:4px 14px;
      font-size:0.7rem; font-weight:600;
      text-transform:uppercase;
      letter-spacing:0.5px;
      color:var(--text-muted);
      background:var(--surface);
      border-bottom:1px solid var(--border);
      flex-shrink:0;
    }
    .stepdef-label { border-top:1px solid var(--border); }

    #step-detail-code {
      padding:10px 14px;
      font-family:"JetBrains Mono","Fira Code",monospace;
      font-size:0.85rem; line-height:1.6;
      white-space:pre-wrap; word-break:break-word;
      overflow-y:auto; flex:0 1 auto;
      background:var(--code-bg); color:var(--text);
      max-height:120px;
    }

    .stepdef-container {
      flex:1 1 auto;
      overflow-y:auto;
      background:var(--code-bg);
      border-top:1px solid var(--border);
    }

    #step-detail-stepdef-code {
      padding:10px 14px;
      font-family:"JetBrains Mono","Fira Code",monospace;
      font-size:0.82rem; line-height:1.5;
      white-space:pre-wrap; word-break:break-word;
      background:var(--code-bg);
      color:var(--text);
    }

    .stepdef-meta {
      padding:4px 14px;
      font-size:0.7rem;
      color:var(--text-muted);
      background:var(--surface);
      border-top:1px solid var(--border);
      flex-shrink:0;
    }

    /* Kotlin syntax colours */
    .kw-annot  { color:var(--kw-annot); font-weight:600; }
    .kw-kotlin { color:var(--kw-kotlin); font-weight:500; }
    .kw-str    { color:var(--kw-str); }

    /* ── Responsive ──────────────────────────────────────────────── */

    @media (max-width:700px) {
      #sidebar { width:180px; min-width:120px; }
      #content { padding:12px 16px; }
      .step-detail-panel { right:0; width:100vw; border-radius:0; }
    }
    """)


# ═══════════════════════════════════════════════════════════════════════════
#  JavaScript
# ═══════════════════════════════════════════════════════════════════════════


def _javascript() -> str:
    return textwrap.dedent("""\
    document.addEventListener("DOMContentLoaded", function () {

      /* ── Collapse / expand all ──────────────────────────────── */

      var collapseBtn = document.getElementById("collapse-all");
      var allCollapsed = false;
      collapseBtn.addEventListener("click", function () {
        var sel = document.querySelectorAll(
          "#content details.section-rule, #content details.section-example"
        );
        allCollapsed = !allCollapsed;
        for (var i = 0; i < sel.length; i++) sel[i].open = !allCollapsed;
        collapseBtn.textContent = allCollapsed ? "\\u229E" : "\\u229F";
      });

      /* ── Step detail panel ──────────────────────────────────── */

      var panel = document.getElementById("step-detail");
      var codeEl = document.getElementById("step-detail-code");
      var closeBtn = document.getElementById("step-detail-close");
      var contextEl = document.getElementById("step-detail-context");
      var stepdefCodeEl = document.getElementById("step-detail-stepdef-code");
      var stepdefMetaEl = document.getElementById("step-detail-stepdef-meta");

      var stepLines = document.querySelectorAll(".step-line");
      for (var i = 0; i < stepLines.length; i++) {
        stepLines[i].addEventListener("click", function (e) {
          var el = e.currentTarget;
          var rawText = el.getAttribute("data-step");
          if (!rawText) return;

          var prev = document.querySelector(".step-line.active");
          if (prev) prev.classList.remove("active");
          el.classList.add("active");

          // Context breadcrumb
          var ctx = [el.getAttribute("data-feature"),
                     el.getAttribute("data-rule"),
                     el.getAttribute("data-scenario")]
            .filter(function(s){ return s && s.length > 0; })
            .map(function(s, i){ return (i > 0 ? "\\u21B3 " : "") + s; });
          contextEl.textContent = ctx.join("  \\u00B7  ");

          // Gherkin step
          codeEl.textContent = rawText;
          codeEl.innerHTML = window._hlGherkin(rawText);

          // Kotlin step definition
          var stepdef = el.getAttribute("data-stepdef");
          if (stepdef && stepdef.length > 0) {
            stepdefCodeEl.textContent = stepdef;
            stepdefCodeEl.innerHTML = window._hlKotlin(stepdef);
            var defFile = el.getAttribute("data-stepdef-file") || "";
            var defLine = el.getAttribute("data-stepdef-line") || "0";
            stepdefMetaEl.textContent = defFile + ":" + defLine;
          } else {
            stepdefCodeEl.textContent = "(no matching step definition found)";
            stepdefCodeEl.innerHTML = "<span class=\\"kw-comment\\">(no matching step definition found)</span>";
            stepdefMetaEl.textContent = "";
          }

          panel.classList.add("open");
        });
      }

      closeBtn.addEventListener("click", function () {
        panel.classList.remove("open");
        var a = document.querySelector(".step-line.active");
        if (a) a.classList.remove("active");
      });

      document.addEventListener("click", function (e) {
        if (panel.classList.contains("open") &&
            !panel.contains(e.target) &&
            !e.target.classList.contains("step-line")) {
          panel.classList.remove("open");
          var a = document.querySelector(".step-line.active");
          if (a) a.classList.remove("active");
        }
      });

      /* ── Gherkin syntax highlighter ─────────────────────────── */

      window._hlGherkin = function (text) {
        var esc = text.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");

        if (/^@/.test(text)) return "<span class=\\"kw-tag\\">" + esc + "</span>";
        if (/^#/.test(text)) return "<span class=\\"kw-comment\\">" + esc + "</span>";

        var m = text.match(/^(Feature|Background|Rule|Scenario|Example):\\s*(.*)/);
        if (m) return "<span class=\\"kw-" + m[1].toLowerCase() + "\\">" + m[1] + ":</span> " + _e(m[2]);

        var m2 = text.match(/^(Given|When|Then|And|But)\\b(.*)/);
        if (m2) return "<span class=\\"kw-" + m2[1].toLowerCase() + "\\">" + m2[1] + "</span> " + _e(m2[2]);

        if (text.charAt(0) === "|") {
          var cells = text.match(/\\|([^|]*?)\\|?/g);
          if (cells) {
            var parts = ["|"];
            for (var i = 0; i < cells.length; i++) {
              var c = cells[i].replace(/^\\||\\|$/g, "");
              parts.push("<span class=\\"kw-table\\">" + _e(c) + "</span>|");
            }
            return parts.join("");
          }
        }
        if (text.indexOf('\\"\\"\\"') === 0) return "<span class=\\"kw-docstring\\">" + esc + "</span>";
        return esc;
      };

      /* ── Kotlin syntax highlighter ──────────────────────────── */

      window._hlKotlin = function (code) {
        var esc = code.replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");

        // annotations
        esc = esc.replace(/(@\\w+)/g, "<span class=\\"kw-annot\\">$1</span>");

        // keywords
        var kws = ["\\\\bfun\\\\b","\\\\bclass\\\\b","\\\\bprivate\\\\b","\\\\bpublic\\\\b",
                    "\\\\bval\\\\b","\\\\bvar\\\\b","\\\\blet\\\\b","\\\\bif\\\\b","\\\\belse\\\\b",
                    "\\\\bfor\\\\b","\\\\bwhile\\\\b","\\\\breturn\\\\b","\\\\bcheck\\\\b",
                    "\\\\bimport\\\\b","\\\\bpackage\\\\b","\\\\blateinit\\\\b","\\\\boverride\\\\b",
                    "\\\\bdata\\\\b","\\\\bmap\\\\b","\\\\blist\\\\b","\\\\btry\\\\b",
                    "\\\\bcatch\\\\b","\\\\bfinally\\\\b","\\\\bthrow\\\\b","\\\\berror\\\\b",
                    "\\\\btrue\\\\b","\\\\bfalse\\\\b","\\\\bnull\\\\b","\\\\bin\\\\b"];
        for (var i = 0; i < kws.length; i++) {
          var re = new RegExp(kws[i], "g");
          esc = esc.replace(re, "<span class=\\"kw-kotlin\\">$&</span>");
        }

        // string literals
        esc = esc.replace(/"([^"\\\\]|\\\\.)*"/g, function(m){ return "<span class=\\"kw-str\\">" + m + "</span>"; });

        // line comments
        esc = esc.replace(/(\\/\\/.*)/g, "<span class=\\"kw-comment\\">$1</span>");

        return esc;
      };

      window._e = function (s) {
        return (s || "").replace(/&/g,"&amp;").replace(/</g,"&lt;").replace(/>/g,"&gt;").replace(/"/g,"&quot;");
      };

      /* ── Sidebar active highlight on scroll ──────────────────── */

      var featSections = document.querySelectorAll(".feature");
      var featLinks = document.querySelectorAll("#feature-list a");
      var content = document.getElementById("content");

      function updateActive() {
        var curId = null;
        for (var i = 0; i < featSections.length; i++) {
          if (featSections[i].getBoundingClientRect().top <= 140) curId = featSections[i].id;
        }
        for (var j = 0; j < featLinks.length; j++) {
          var link = featLinks[j];
          link.className = link.getAttribute("href") === "#" + curId
            ? "feature-link active" : "feature-link";
        }
      }

      if (content) content.addEventListener("scroll", updateActive);
      setTimeout(updateActive, 100);
    });
    """)


# ═══════════════════════════════════════════════════════════════════════════
#  CLI
# ═══════════════════════════════════════════════════════════════════════════


def main():
    parser = argparse.ArgumentParser(
        description="Generate a self-contained HTML Gherkin spec viewer "
                    "with Kotlin step‑definition cross‑reference.",
        formatter_class=argparse.RawDescriptionHelpFormatter,
        epilog=textwrap.dedent("""\
            Examples:
              %(prog)s
              %(prog)s --open
              %(prog)s --all --open
              %(prog)s --no-stepdefs
              %(prog)s --step-defs factory/src/test/kotlin/steps
              %(prog)s -d factory/src/test/features -o test-specs.html
        """),
    )
    parser.add_argument(
        "--features-dir", "-d",
        default="factory/spec/features",
        help="Directory with .feature files (default: factory/spec/features)",
    )
    parser.add_argument(
        "--output", "-o",
        default="factory/spec/index.html",
        help="Output path (default: factory/spec/index.html)",
    )
    parser.add_argument(
        "--exclude", "-x",
        nargs="*",
        default=[],
        help="Substring patterns to exclude from feature scanning",
    )
    parser.add_argument(
        "--all", "-a",
        action="store_true",
        help="Scan factory/spec/features AND factory/src/test/features",
    )
    parser.add_argument(
        "--step-defs",
        nargs="*",
        default=[],
        help="Directories to scan for Kotlin step‑definition files "
             "(default: auto-detect under factory/)",
    )
    parser.add_argument(
        "--no-stepdefs",
        action="store_true",
        help="Disable step‑definition look‑up (no Kotlin code in detail panel)",
    )
    parser.add_argument(
        "--open",
        action="store_true",
        help="Open the generated HTML in the default browser",
    )
    args = parser.parse_args()

    # ── Collect feature directories ────────────────────────────────
    dirs = [args.features_dir]
    if args.all:
        dirs = ["factory/spec/features", "factory/src/test/features"]

    docs: list[_GherkinDoc] = []
    for d in dirs:
        if not os.path.isdir(d):
            continue
        docs.extend(parse_features(d, args.exclude or []))

    if not docs:
        print("No .feature files found.", file=sys.stderr)
        sys.exit(1)

    # ── Collect step‑definition directories ────────────────────────
    step_def_dirs: list[str] = []
    if not args.no_stepdefs:
        step_def_dirs = list(args.step_defs) if args.step_defs else []
        if not step_def_dirs:
            # Auto‑detect common locations
            candidates = [
                "factory/src/test/kotlin/steps",
                "factory/src/test/kotlin",
            ]
            for c in candidates:
                if os.path.isdir(c):
                    step_def_dirs.append(c)

    step_defs: list[StepDef] = []
    if step_def_dirs:
        step_defs = parse_step_definitions(step_def_dirs)
        print(f"  Step definitions: {len(step_defs)} from {len(step_def_dirs)} dir(s)")

    html = build_html(docs, step_defs)
    out = args.output
    os.makedirs(os.path.dirname(out) or ".", exist_ok=True)
    with open(out, "w", encoding="utf-8") as fh:
        fh.write(html)

    sections_total = sum(len(d.children) for d in docs)
    matched = sum(
        1 for d in docs
        for c in d.children
        for _ in ([c] if c.kind in ("Background", "Example") else c.examples)
    )
    print(
        f"Wrote {out}  "
        f"({len(docs)} feature{'s' if len(docs)!=1 else ''}, "
        f"{sections_total} section{'s' if sections_total!=1 else ''})"
    )
    if step_defs:
        print(f"  Step definitions: {len(step_defs)} total")

    if args.open:
        abs_path = os.path.abspath(out)
        webbrowser.open(f"file://{abs_path}")
        print(f"  Opened {abs_path}")


if __name__ == "__main__":
    main()