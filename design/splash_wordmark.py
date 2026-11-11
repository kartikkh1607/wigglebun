"""Prints the path data of res/drawable/splash_wordmark.xml: "Wigglebun" in Fredoka SemiBold.

Needs fontTools (pip install fonttools). Run from the repo root:
    python design/splash_wordmark.py
"""
from fontTools.pens.svgPathPen import SVGPathPen
from fontTools.pens.transformPen import TransformPen
from fontTools.ttLib import TTFont

TEXT, SIZE, WIDTH, BASELINE = "Wigglebun", 30.0, 200.0, 50.0

font = TTFont("app/src/main/res/font/fredoka_semibold.ttf")
glyphs, cmap = font.getGlyphSet(), font.getBestCmap()
scale = SIZE / font["head"].unitsPerEm
x = (WIDTH - sum(glyphs[cmap[ord(c)]].width for c in TEXT) * scale) / 2
parts = []
for c in TEXT:
    glyph = glyphs[cmap[ord(c)]]
    pen = SVGPathPen(glyphs, ntos=lambda v: ("%.2f" % v).rstrip("0").rstrip("."))
    glyph.draw(TransformPen(pen, (scale, 0, 0, -scale, x, BASELINE)))
    parts.append(pen.getCommands())
    x += glyph.width * scale
print(" ".join(parts))
