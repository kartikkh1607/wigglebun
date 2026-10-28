"""Writes app/src/main/res/drawable/ic_launcher_monochrome.xml (themed icon, Android 13+).

Bun's silhouette from app_icon_foreground.svg (ears + body) with the face strokes cut out.
The face strokes (stroke 2.6, round caps; nose stroke 1.2) are turned into outlines and
unioned, so the nose, the line and the smile become one hole. The silhouette stays exact SVG
arcs drawn with sweep=1 (positive area); the holes are written with negative area, so nonZero
filling cuts them out.

Needs shapely (pip install shapely). Run from the repo root:
    python design/launcher_monochrome.py
If you change the face in app_icon_foreground.svg, change the strokes below to match.
"""
from pathlib import Path

from shapely.geometry import LineString, Polygon
from shapely.geometry.polygon import orient
from shapely.ops import unary_union

OUT = Path(__file__).resolve().parent.parent / "app/src/main/res/drawable/ic_launcher_monochrome.xml"


def quad(p0, p1, p2, n=48):
    pts = []
    for i in range(n + 1):
        t = i / n
        a, b, c = (1 - t) ** 2, 2 * (1 - t) * t, t * t
        pts.append((a * p0[0] + b * p1[0] + c * p2[0], a * p0[1] + b * p1[1] + c * p2[1]))
    return pts


HALF = 2.6 / 2
strokes = [
    LineString(quad((42, 56), (46, 51), (50, 56))),  # left eye
    LineString(quad((58, 56), (62, 51), (66, 56))),  # right eye
    LineString(quad((46, 65), (54, 72), (62, 65))),  # smile
    LineString([(54, 62.5), (54, 68.5)]),  # nose line
]
shapes = [s.buffer(HALF, quad_segs=12, cap_style="round", join_style="round") for s in strokes]
shapes.append(Polygon([(51.2, 60), (56.8, 60), (54, 63.2)]).buffer(0.6, quad_segs=12, join_style="round"))  # nose
face = unary_union(shapes).simplify(0.01)


def fmt(v):
    s = f"{v:.2f}".rstrip("0").rstrip(".")
    return "0" if s == "-0" else s


def ring(coords):
    coords = list(coords)[:-1]
    head = coords[0]
    body = " ".join(f"{fmt(x)},{fmt(y)}" for x, y in coords[1:])
    return f"M{fmt(head[0])},{fmt(head[1])} L{body} Z"


holes = []
for poly in getattr(face, "geoms", [face]):
    # Negative signed area in raw (y-down) coordinates = opposite winding to the arcs.
    poly = orient(poly, sign=-1.0)
    holes.append(ring(poly.exterior.coords))
    for interior in poly.interiors:  # an enclosed island inside a hole is filled again
        holes.append(ring(interior.coords))

silhouette = (
    "M24,38 a9,9 0,1 1,18 0 a9,9 0,1 1,-18 0 Z "
    "M66,38 a9,9 0,1 1,18 0 a9,9 0,1 1,-18 0 Z "
    "M27,59 a27,24 0,1 1,54 0 a27,24 0,1 1,-54 0 Z"
)
path_data = silhouette + " " + " ".join(holes)

XML = f"""<?xml version="1.0" encoding="utf-8"?>
<!-- Themed-icon layer (Android 13+): Bun's silhouette from design/app_icon_foreground.svg with
     the eyes, nose and smile cut out. The launcher tints it, so only the shape matters. Ears and
     body are the SVG's circles and ellipse; the cut-outs are the face strokes (2.6 wide, round
     caps) outlined and merged into one shape each, wound the other way so nonZero leaves holes.
     VectorPath: the outlines are long polylines, but launchers rasterise and cache icons, so the
     path is parsed once, not every frame. -->
<vector xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    android:width="108dp"
    android:height="108dp"
    android:viewportWidth="108"
    android:viewportHeight="108">
    <!-- The SVG's ears reach 35.4 units from the centre, past the 33-unit safe-zone circle, so
         circle masks touched them. Scaled to 0.9 (shapes unchanged) and nudged down 1.5:
         the furthest point is now 31 units out. -->
    <group
        android:pivotX="54"
        android:pivotY="54"
        android:scaleX="0.9"
        android:scaleY="0.9"
        android:translateY="1.5">
        <path
            android:fillColor="#FF000000"
            android:fillType="nonZero"
            tools:ignore="VectorPath"
            android:pathData="{path_data}" />
    </group>
</vector>
"""

OUT.write_text(XML, encoding="utf-8", newline="\n")
print(f"wrote {OUT} ({len(holes)} holes)")
