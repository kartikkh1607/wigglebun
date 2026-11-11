"""Builds the Play Store graphics from the checked-in reference screenshots and the app's fonts.

Needs ImageMagick (`convert`), librsvg (`rsvg-convert`) and fontTools. Run from the repo root:
    python store/make_assets.py
Outputs: store/screenshots/0N_*.png (1080x1920) and store/feature_graphic.png (1024x500, no alpha).
Re-run after `recordRoborazziDebug` changes a screen shown here.
"""
import base64
import subprocess
from pathlib import Path

from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

ROOT = Path(__file__).resolve().parent.parent
SHOTS = ROOT / "app/src/test/screenshots"
OUT = ROOT / "store"
FREDOKA = ROOT / "app/src/main/res/font/fredoka_semibold.ttf"
NUNITO = ROOT / "app/src/main/res/font/nunito_bold.ttf"
INK = "#2B2A3A"

# (file, reference screenshot, background, caption)
SCREENS = [
    ("01_tired", "home_tired", "#D6EBDA", "Sit too long and\nyour pet gets tired"),
    ("02_stretch", "session_neck", "#EAF3FD", "1–2 minute\nguided stretches"),
    ("03_celebrate", "session_celebrate_first", "#FFE3A3", "Move, and the\ncolour comes back"),
    ("04_breaks", "home_happy", "#F4E8D6", "Small breaks,\nall through your day"),
    ("05_collection", "collection_mockup", "#DDF1E6", "Earn sparks,\nadopt new pets"),
    ("06_profile", "pet_profile_busy_week", "#E2EEFB", "Keep your streak\ngoing"),
]


def text_path(font_file, text, size, x_centre, baseline):
    """One line of text as SVG path data, centred on x_centre."""
    font = TTFont(font_file)
    glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
    scale = size / font["head"].unitsPerEm
    x = x_centre - sum(glyphs[cmap[ord(c)]].width for c in text) * scale / 2
    parts = []
    for c in text:
        glyph = glyphs[cmap[ord(c)]]
        pen = SVGPathPen(glyphs, ntos=lambda v: ("%.1f" % v))
        glyph.draw(TransformPen(pen, (scale, 0, 0, -scale, x, baseline)))
        parts.append(pen.getCommands())
        x += glyph.width * scale
    return " ".join(parts)


def screenshot(name, source, background, caption):
    # 1080x1920: the caption on top, the screen below with rounded corners, running off the bottom.
    lines = caption.split("\n")
    paths = "".join(
        f'<path d="{text_path(FREDOKA, line, 84, 540, 190 + i * 104)}" fill="{INK}"/>' for i, line in enumerate(lines)
    )
    # Embedded: rsvg-convert won't load an image from another folder.
    shot = "data:image/png;base64," + base64.b64encode((SHOTS / f"{source}.png").read_bytes()).decode()
    w, h, x, y = 860, 1861, 110, 400  # 1170x2532 scaled to 860 wide
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" width="1080" height="1920">
<rect width="1080" height="1920" fill="{background}"/>{paths}
<clipPath id="c"><rect x="{x}" y="{y}" width="{w}" height="{h}" rx="64"/></clipPath>
<rect x="{x - 10}" y="{y - 10}" width="{w + 20}" height="{h + 20}" rx="74" fill="{INK}"/>
<image xlink:href="{shot}" x="{x}" y="{y}" width="{w}" height="{h}" clip-path="url(#c)"/></svg>'''
    tmp = OUT / f"{name}.svg"
    tmp.write_text(svg)
    target = OUT / "screenshots" / f"{name}.png"
    subprocess.run(["rsvg-convert", "--unlimited", str(tmp), "-o", str(target)], check=True)
    # Play wants no alpha on screenshots either.
    subprocess.run(["convert", str(target), "-background", background, "-alpha", "remove", "-alpha", "off", str(target)], check=True)
    tmp.unlink()


def feature_graphic():
    icon = (ROOT / "design/app_icon_foreground.svg").read_text()
    art = icon[icon.index(">", icon.index("<svg")) + 1 : icon.rindex("</svg>")]
    title = text_path(FREDOKA, "Wigglebun", 104, 700, 245)
    line = text_path(NUNITO, "The desk pet that gets you moving", 33, 700, 312)
    svg = f'''<svg xmlns="http://www.w3.org/2000/svg" width="1024" height="500">
<defs><radialGradient id="night" cx="260" cy="250" r="700" gradientUnits="userSpaceOnUse"><stop offset="0" stop-color="#4A4870"/><stop offset="1" stop-color="#252437"/></radialGradient></defs>
<rect width="1024" height="500" fill="url(#night)"/>
<g transform="translate(30 20) scale(4.26)">{art}</g>
<path d="{title}" fill="#FFF4E8"/><path d="{line}" fill="#D9D7EC"/></svg>'''
    tmp = OUT / "feature_graphic.svg"
    tmp.write_text(svg)
    target = OUT / "feature_graphic.png"
    subprocess.run(["rsvg-convert", str(tmp), "-o", str(target)], check=True)
    subprocess.run(["convert", str(target), "-background", "#252437", "-alpha", "remove", "-alpha", "off", "PNG24:" + str(target)], check=True)
    tmp.unlink()


if __name__ == "__main__":
    for screen in SCREENS:
        screenshot(*screen)
    feature_graphic()
