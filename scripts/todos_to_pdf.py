"""
Converts TODOs.md into a beautifully formatted PDF using fpdf2.
Cross-platform: Windows / macOS / Linux.
Usage:  python scripts/todos_to_pdf.py
Output: TODOs.pdf  (project root)
"""
import re
import sys
import platform
import urllib.request
from pathlib import Path

try:
    from fpdf import FPDF
    from fpdf.enums import XPos, YPos, RenderStyle
except ModuleNotFoundError as exc:
    if exc.name == "fpdf":
        print(
            "Fehler: Das Python-Paket 'fpdf2' ist nicht installiert.\n"
            "Installiere die Abhaengigkeit mit:\n"
            "  python3 -m pip install -r requirements.txt",
            file=sys.stderr,
        )
        sys.exit(1)
    raise

ROOT   = Path(__file__).resolve().parent.parent
INPUT  = ROOT / "TODOs.md"
OUTPUT = ROOT / "TODOs.pdf"

# ── Cross-platform font resolution ──────────────────────────────────────────
_SYSTEM = platform.system()   # "Windows" | "Darwin" | "Linux"

_FONT_DIRS: dict = {
    "Windows": [Path("C:/Windows/Fonts")],
    "Darwin":  [
        Path("/Library/Fonts"),
        Path("/System/Library/Fonts/Supplemental"),
        Path("/System/Library/Fonts"),
        Path.home() / "Library/Fonts",
    ],
    "Linux": [
        Path("/usr/share/fonts"),
        Path("/usr/local/share/fonts"),
        Path.home() / ".fonts",
        Path.home() / ".local/share/fonts",
    ],
}

# Ordered preferences per platform: (regular, bold, italic)
_FONT_PREFS: dict = {
    "Windows": [
        ("segoeui.ttf",  "segoeuib.ttf",  "segoeuii.ttf"),
        ("arial.ttf",    "arialbd.ttf",   "ariali.ttf"),
    ],
    "Darwin": [
        ("Arial.ttf",        "Arial Bold.ttf",        "Arial Italic.ttf"),
        ("Helvetica.ttf",    "Helvetica-Bold.ttf",    "Helvetica-Oblique.ttf"),
    ],
    "Linux": [
        ("DejaVuSans.ttf",             "DejaVuSans-Bold.ttf",     "DejaVuSans-Oblique.ttf"),
        ("LiberationSans-Regular.ttf", "LiberationSans-Bold.ttf", "LiberationSans-Italic.ttf"),
        ("NotoSans-Regular.ttf",       "NotoSans-Bold.ttf",       "NotoSans-Italic.ttf"),
    ],
}


def _find_font_file(name: str) -> "Path | None":
    for d in _FONT_DIRS.get(_SYSTEM, _FONT_DIRS["Linux"]):
        if not d.is_dir():
            continue
        p = d / name
        if p.exists():
            return p
        found = next(d.rglob(name), None)
        if found:
            return found
    return None


def _download_dejavu() -> "tuple[Path, Path, Path]":
    """Download DejaVu Sans into ~/.cache/vebo_todos_pdf/ as last resort."""
    cache = Path.home() / ".cache" / "vebo_todos_pdf"
    cache.mkdir(parents=True, exist_ok=True)
    base = ("https://github.com/dejavu-fonts/dejavu-fonts/"
            "raw/refs/heads/main/fonts/")
    files = [
        ("DejaVuSans.ttf",         cache / "DejaVuSans.ttf"),
        ("DejaVuSans-Bold.ttf",    cache / "DejaVuSans-Bold.ttf"),
        ("DejaVuSans-Oblique.ttf", cache / "DejaVuSans-Oblique.ttf"),
    ]
    for fname, dest in files:
        if not dest.exists():
            print(f"  Lade {fname} herunter ...", flush=True)
            urllib.request.urlretrieve(base + fname, dest)
    return files[0][1], files[1][1], files[2][1]


def resolve_fonts() -> "tuple[Path, Path, Path]":
    """Return (regular, bold, italic) TTF paths; download DejaVu if nothing found."""
    for reg, bold, italic in _FONT_PREFS.get(_SYSTEM, _FONT_PREFS["Linux"]):
        r, b, i = _find_font_file(reg), _find_font_file(bold), _find_font_file(italic)
        if r and b and i:
            return r, b, i
    print("Keine System-Schrift gefunden – lade DejaVu Sans ...")
    return _download_dejavu()


# ── Suppress harmless fpdf2 font-subsetting noise ───────────────────────────
class _FilteredStderr:
    """Drop the 'MERG NOT subset' lines that fpdf2 prints during font subsetting."""
    _MUTED = ("MERG NOT subset", "don't know how to subset")

    def __init__(self, orig):
        self._orig = orig

    def write(self, text: str) -> int:
        if not any(kw in text for kw in self._MUTED):
            return self._orig.write(text)
        return len(text)

    def flush(self):
        self._orig.flush()

    def fileno(self):          # needed by some tools that inspect stderr
        return self._orig.fileno()

# ── Colour palette ──────────────────────────────────────────────────────────
C_PAGE_BG   = (248, 250, 252)
C_HDR_DARK  = (10,  18,  35)
C_HDR_MID   = (22,  36,  60)
C_HDR_FG    = (241, 245, 249)
C_HDR_MUTED = (148, 163, 184)
C_ACCENT    = (99,  102, 241)
C_META      = (107, 114, 128)
C_BODY      = (17,  24,  39)
C_OPEN_BG   = (255, 255, 255)
C_STRIPE    = (249, 250, 251)
C_DONE_BG   = (240, 253, 244)
C_BORDER    = (229, 231, 235)
C_SHADOW    = (218, 224, 233)
C_DONE_TICK = (22,  163,  74)
C_OPEN_BOX  = (203, 213, 225)
C_PROG_BG   = (51,  65,  85)
C_PROG_FG   = (99,  102, 241)

MARGIN   = 14
ITEM_GAP = 0.5

# ── Section definitions ─────────────────────────────────────────────────────
SECTION_MAP = {
    "\U0001f41b": ("[BUGS]",   (220,  38,  38)),
    "\u2705":     ("[TESTS]",  (22,  163,  74)),
    "\U0001f527": ("[TECH]",   (217, 119,   6)),
    "\U0001f4d0": ("[CODE]",   (124,  58, 237)),
    "\u2728":     ("[FEAT]",   (234,  88,  12)),
    "\U0001f3a8": ("[UI/UX]",  (219,  39, 119)),
    "\U0001f4e6": ("[BUILD]",  (3,   105, 161)),
}
_DEFAULT_COLOR = (79, 70, 229)


def section_info(title: str):
    for emoji, (label, color) in SECTION_MAP.items():
        if emoji in title:
            return label, re.sub(re.escape(emoji), "", title).strip(), color
    return "[?]", title, _DEFAULT_COLOR


def strip_non_latin(s: str) -> str:
    return re.sub(
        r"[^\x00-\x7F\u00C0-\u024F\u2013\u2014\u2018\u2019\u201C\u201D\u2022\u00B7]",
        "", s)


def clean(s: str) -> str:
    s = re.sub(r"`([^`]+)`", r"\1", s)
    s = re.sub(r"\*\*([^*]+)\*\*", r"\1", s)
    s = s.replace("\u2192", "->").replace("\u2013", "-")
    return strip_non_latin(s).strip()


def parse_todos(lines):
    total = done = 0
    sections: dict = {}
    current = None
    for ln in lines:
        h2 = re.match(r"^##\s+(.+)", ln)
        if h2:
            current = h2.group(1).strip()
            sections[current] = [0, 0]
        m = re.match(r"^- \[([ xX])\]", ln)
        if m:
            total += 1
            is_done = m.group(1).lower() == "x"
            if is_done:
                done += 1
            if current:
                sections[current][0] += 1
                if is_done:
                    sections[current][1] += 1
    return total, done, sections


# ── PDF ──────────────────────────────────────────────────────────────────────
class TodoPDF(FPDF):

    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        self.section_color = list(_DEFAULT_COLOR)

    # ── Rounded rect helper ───────────────────────────────────────────────────
    def rounded_rect(self, x: float, y: float, w: float, h: float,
                     r: float, style: str = "F"):
        _map = {"F": RenderStyle.F, "D": RenderStyle.D,
                "FD": RenderStyle.DF, "DF": RenderStyle.DF}
        self._draw_rounded_rect(x, y, w, h, _map.get(style, RenderStyle.F), True, r)

    def setup_fonts(self, font_reg: Path, font_bold: Path, font_italic: Path):
        self.add_font("Segoe", "",  str(font_reg))
        self.add_font("Segoe", "B", str(font_bold))
        self.add_font("Segoe", "I", str(font_italic))

    def header(self):
        """Paint page background on every page."""
        self.set_fill_color(*C_PAGE_BG)
        self.rect(0, 0, self.w, self.h, "F")

    def footer(self):
        y = self.h - 11
        self.set_draw_color(*C_BORDER)
        self.set_line_width(0.2)
        self.line(MARGIN, y, self.w - MARGIN, y)
        self.set_font("Segoe", "I", 7.5)
        self.set_text_color(*C_META)
        self.set_xy(MARGIN, y + 1)
        self.cell(0, 5,
                  f"VEBO Lagersystem  \u00b7  TODO-Liste  \u00b7  Seite {self.page_no()}",
                  align="C")

    # ── Title block ───────────────────────────────────────────────────────────
    def draw_title_block(self, total: int, done: int):
        open_c = total - done
        pct    = int(done / total * 100) if total else 0
        hh     = 44

        # Two-tone header
        self.set_fill_color(*C_HDR_DARK)
        self.rect(0, 0, self.w, hh, "F")
        self.set_fill_color(*C_HDR_MID)
        self.rect(0, 28, self.w, hh - 28, "F")
        # Indigo accent strip at top
        self.set_fill_color(*C_ACCENT)
        self.rect(0, 0, self.w, 3, "F")

        # Title
        self.set_xy(MARGIN, 5.5)
        self.set_font("Segoe", "B", 19)
        self.set_text_color(*C_HDR_FG)
        self.cell(0, 11, "TODO-Liste   \u2014   VEBO Lagersystem",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        # Subtitle
        self.set_x(MARGIN)
        self.set_font("Segoe", "I", 8)
        self.set_text_color(*C_HDR_MUTED)
        self.cell(0, 5,
                  f"Stand: 31.03.2026    {total} Eintraege    "
                  f"{open_c} offen    {done} erledigt    {pct}% abgeschlossen",
                  new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        # Progress bar
        bx, by = MARGIN, 36.5
        bw, bh = self.w - 2 * MARGIN, 4.5
        self.set_fill_color(*C_PROG_BG)
        self.rounded_rect(bx, by, bw, bh, 2.2, "F")
        if total and done:
            fw = max(bh, bw * done / total)
            self.set_fill_color(*C_PROG_FG)
            self.rounded_rect(bx, by, fw, bh, 2.2, "F")

        self.set_y(hh + 6)

    # ── Section banner ────────────────────────────────────────────────────────
    def draw_section_header(self, title: str, sec_total: int, sec_done: int):
        label, clean_title, color = section_info(title)
        self.section_color = list(color)

        self.ln(5)
        if self.get_y() > self.h - 40:
            self.add_page()

        uw    = self.w - 2 * MARGIN
        y0    = self.get_y()
        sh    = 11
        r     = 3
        shade = tuple(max(0, c - 55) for c in color)

        # Banner
        self.set_fill_color(*color)
        self.rounded_rect(MARGIN, y0, uw, sh, r, "F")

        # Left label badge
        bw = 20
        self.set_fill_color(*shade)
        self.rounded_rect(MARGIN + 2, y0 + 2, bw, sh - 4, r - 1, "F")
        self.set_font("Segoe", "B", 7)
        self.set_text_color(255, 255, 255)
        self.set_xy(MARGIN + 2, y0 + 2.5)
        self.cell(bw, sh - 5, label, align="C")

        # Title text
        self.set_font("Segoe", "B", 10)
        self.set_xy(MARGIN + bw + 6, y0 + 2)
        self.cell(uw - bw - 30, sh - 4, clean_title)

        # Right count badge  done/total
        cnt_w = 22
        cx    = MARGIN + uw - cnt_w - 2
        self.set_fill_color(*shade)
        self.rounded_rect(cx, y0 + 2, cnt_w, sh - 4, r - 1, "F")
        self.set_font("Segoe", "B", 8)
        self.set_xy(cx, y0 + 2.5)
        self.cell(cnt_w, sh - 5, f"{sec_done}/{sec_total}", align="C")

        self.set_y(y0 + sh + 2)

    # ── TODO row ──────────────────────────────────────────────────────────────
    def _row_height(self, text: str, text_w: float) -> float:
        self.set_font("Segoe", "", 8.5)
        sw      = self.get_string_width(text)
        n_lines = max(1, int(sw / text_w) + 1)
        return max(9.0, n_lines * 4.6 + 3.5)

    def draw_todo_item(self, text: str, checked: bool, row_index: int):
        uw      = self.w - 2 * MARGIN
        left_bar = 2.5
        pad      = 2.0
        cb_size  = 4.5
        text_x   = MARGIN + left_bar + pad + cb_size + 2
        text_w   = uw - left_bar - pad - cb_size - 5

        row_h = self._row_height(text, text_w)

        if self.get_y() + row_h + 1 > self.h - 14:
            self.add_page()

        x0, y0 = MARGIN, self.get_y()
        bg = C_DONE_BG if checked else (C_STRIPE if row_index % 2 == 0 else C_OPEN_BG)

        # Shadow
        self.set_fill_color(*C_SHADOW)
        self.rect(x0 + 0.5, y0 + 0.5, uw, row_h, "F")

        # Row background
        self.set_fill_color(*bg)
        self.rect(x0, y0, uw, row_h, "F")

        # Left accent bar (section colour)
        self.set_fill_color(*self.section_color)
        self.rect(x0, y0, left_bar, row_h, "F")

        # Bottom divider
        self.set_draw_color(*C_BORDER)
        self.set_line_width(0.1)
        self.line(x0 + left_bar, y0 + row_h, x0 + uw, y0 + row_h)

        # Checkbox
        cb_x = x0 + left_bar + pad
        cb_y = y0 + (row_h - cb_size) / 2
        if checked:
            self.set_fill_color(*C_DONE_TICK)
            self.rounded_rect(cb_x, cb_y, cb_size, cb_size, 1.2, "F")
            self.set_font("Segoe", "B", 9)
            self.set_text_color(255, 255, 255)
            self.set_xy(cb_x, cb_y + 0.1)
            self.cell(cb_size, cb_size, "v", align="C")
        else:
            self.set_fill_color(*C_OPEN_BG)
            self.set_draw_color(*C_OPEN_BOX)
            self.set_line_width(0.5)
            self.rounded_rect(cb_x, cb_y, cb_size, cb_size, 1.2, "FD")
            self.set_line_width(0.1)
            self.set_draw_color(*C_BORDER)

        # Text
        self.set_font("Segoe", "", 8.5)
        if checked:
            self.set_text_color(148, 185, 148)
        else:
            self.set_text_color(*C_BODY)
        self.set_xy(text_x, y0 + 2.2)
        self.multi_cell(text_w, 4.6, text, new_x=XPos.LMARGIN, new_y=YPos.NEXT)

        self.set_y(y0 + row_h + ITEM_GAP)


# ── Build ─────────────────────────────────────────────────────────────────────
def build(md_path: Path, out_path: Path):
    lines = md_path.read_text(encoding="utf-8").splitlines()
    total, done, section_counts = parse_todos(lines)

    font_reg, font_bold, font_italic = resolve_fonts()
    print(f"  Schrift: {font_reg.name}  ({_SYSTEM})", flush=True)

    pdf = TodoPDF(orientation="P", unit="mm", format="A4")
    pdf.setup_fonts(font_reg, font_bold, font_italic)
    pdf.set_margins(MARGIN, MARGIN, MARGIN)
    pdf.set_auto_page_break(auto=False)
    pdf.add_page()
    pdf.draw_title_block(total, done)

    row_index = 0
    for line in lines:
        h2 = re.match(r"^##\s+(.+)", line)
        if h2:
            sec_title = h2.group(1).strip()
            counts    = section_counts.get(sec_title, [0, 0])
            pdf.draw_section_header(sec_title, counts[0], counts[1])
            row_index = 0
            continue

        m = re.match(r"^- \[([ xX])\]\s*(.*)", line)
        if m:
            checked = m.group(1).lower() == "x"
            text    = clean(m.group(2).strip())
            pdf.draw_todo_item(text, checked, row_index)
            row_index += 1

    # Suppress harmless fpdf2 font-subsetting noise during output
    _orig, sys.stderr = sys.stderr, _FilteredStderr(sys.stderr)
    try:
        pdf.output(str(out_path))
    finally:
        sys.stderr = _orig

    print(f"OK  PDF erstellt: {out_path}  ({total} Eintrage, {done} erledigt)")


if __name__ == "__main__":
    if not INPUT.exists():
        print(f"Fehler: {INPUT} nicht gefunden", file=sys.stderr)
        sys.exit(1)
    build(INPUT, OUTPUT)

