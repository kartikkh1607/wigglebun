"""Generates Pip / Toast / Nimbus assets in Bun's layer system (viewBox 0 0 200 210)."""
import cairosvg, os, sys, json
OUT = sys.argv[1]
INK="#2B2A3A"; GREY="#E9E5DE"; CHEEK="#F7A58B"; SWEAT="#7FB8F5"; ZCOL="#B9A3F0"

FEET='<ellipse cx="80" cy="192" rx="13" ry="10"/><ellipse cx="120" cy="192" rx="13" ry="10"/>'
ARMS_SIDE='<ellipse cx="36" cy="132" rx="10" ry="17" transform="rotate(28 36 132)"/><ellipse cx="164" cy="132" rx="10" ry="17" transform="rotate(-28 164 132)"/>'
ARMS_UP='<ellipse cx="44" cy="76" rx="10" ry="19" transform="rotate(-22 44 76)"/><ellipse cx="156" cy="76" rx="10" ry="19" transform="rotate(22 156 76)"/>'
CHEEKS=f'<g id="cheeks" fill="{CHEEK}"><ellipse cx="72" cy="118" rx="9" ry="5.5"/><ellipse cx="128" cy="118" rx="9" ry="5.5"/></g>'

EYES = {
 "dots":  f'<g id="eyes" fill="{INK}"><circle cx="84" cy="106" r="5.5"/><circle cx="116" cy="106" r="5.5"/></g>',
 "flat":  f'<g id="eyes" fill="none" stroke="{INK}" stroke-width="3.6" stroke-linecap="round"><path d="M78 106 L90 106"/><path d="M110 106 L122 106"/></g>',
 "closed":f'<g id="eyes" fill="none" stroke="{INK}" stroke-width="3.6" stroke-linecap="round"><path d="M78 104 Q84 110 90 104"/><path d="M110 104 Q116 110 122 104"/></g>',
 "happy": f'<g id="eyes" fill="none" stroke="{INK}" stroke-width="3.6" stroke-linecap="round"><path d="M78 108 Q84 100 90 108"/><path d="M110 108 Q116 100 122 108"/></g>',
}
MOUTHS = {
 "smile": f'<path id="mouth" d="M96 112 Q100 117 104 112 Z" fill="{INK}" stroke="{INK}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>',
 "flat":  f'<path id="mouth" d="M96 114 L104 114" fill="none" stroke="{INK}" stroke-width="3" stroke-linecap="round"/>',
 "frown": f'<path id="mouth" d="M95 117 Q100 112 105 117" fill="none" stroke="{INK}" stroke-width="3" stroke-linecap="round"/>',
 "sleep": f'<path id="mouth" d="M98 114 Q100 116 102 114" fill="none" stroke="{INK}" stroke-width="3" stroke-linecap="round"/>',
 "open":  f'<path id="mouth" d="M92 111 Q100 125 108 111 Z" fill="{INK}" stroke="{INK}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>',
}
BEAK = "#F4A261"
PIP_MOUTHS = {  # Pip's beak replaces the mouth layer.
 "closed": f'<path id="mouth" d="M92 112 Q100 107 108 112 Q100 123 92 112 Z" fill="{BEAK}"/>',
 "open":   f'<g id="mouth" fill="{BEAK}"><path d="M91 110 Q100 104 109 110 Q100 114 91 110 Z"/><path d="M94 115 Q100 113 106 115 Q100 125 94 115 Z"/></g>',
}
CONFETTI = ('<g id="confetti"><rect x="22" y="38" width="10" height="6" rx="2" fill="#F68A6B" transform="rotate(-25 27 41)"/>'
 '<rect x="168" y="46" width="10" height="6" rx="2" fill="#7FB8F5" transform="rotate(30 173 49)"/><circle cx="18" cy="98" r="5" fill="#7DD3B0"/>'
 '<circle cx="184" cy="104" r="5" fill="#B9A3F0"/><rect x="44" y="14" width="9" height="5" rx="2" fill="#F4A3C0" transform="rotate(20 48 16)"/>'
 '<rect x="148" y="10" width="9" height="5" rx="2" fill="#7DD3B0" transform="rotate(-15 152 12)"/></g>')

PETS = {
 "pip": dict(name="Pip", color="#9EDCC3", edge="#7FC9AC",
   sil='<path d="M100 54 C123 54 137 72 145 94 C155 117 166 133 166 153 C166 178 140 190 100 190 C60 190 34 178 34 153 C34 133 45 117 55 94 C63 72 77 54 100 54 Z"/>'
       '<ellipse cx="100" cy="47" rx="7.5" ry="11"/><ellipse cx="88" cy="52" rx="6.5" ry="10" transform="rotate(-32 88 52)"/><ellipse cx="112" cy="52" rx="6.5" ry="10" transform="rotate(32 112 52)"/>',
   arms_side='<ellipse cx="37" cy="134" rx="9" ry="19" transform="rotate(38 37 134)"/><ellipse cx="163" cy="134" rx="9" ry="19" transform="rotate(-38 163 134)"/>',
   arms_up='<ellipse cx="46" cy="80" rx="9" ry="20" transform="rotate(-28 46 80)"/><ellipse cx="154" cy="80" rx="9" ry="20" transform="rotate(28 154 80)"/>',
   sweat=(150, 74), z=(150, 40)),
 "toast": dict(name="Toast", color="#EFC587", edge="#DDAF6B", crust="#C98F4E", crust_grey="#D6CFC4",
   sil='<path d="M42 104 L42 174 Q42 188 56 188 L144 188 Q158 188 158 174 L158 104 C176 98 182 78 170 60 C158 44 132 38 100 38 C68 38 42 44 30 60 C18 78 24 98 42 104 Z"/>',
   arms_side=ARMS_SIDE,
   # The dome hides Bun's raised-arm position, so Toast raises its arms from the sides.
   arms_up='<ellipse cx="28" cy="118" rx="9" ry="18" transform="rotate(-52 28 118)"/><ellipse cx="172" cy="118" rx="9" ry="18" transform="rotate(52 172 118)"/>',
   sweat=(140, 64), z=(176, 30)),
 "nimbus": dict(name="Nimbus", color="#C9B8F2", edge="#B39FE6",
   sil='<path d="M38 142 C38 110 62 98 100 98 C138 98 162 110 162 142 C162 176 140 190 100 190 C60 190 38 176 38 142 Z"/>'
       '<circle cx="63" cy="104" r="26"/><circle cx="100" cy="74" r="38"/><circle cx="137" cy="104" r="26"/>',
   arms_side=ARMS_SIDE,
   arms_up='<ellipse cx="34" cy="86" rx="10" ry="19" transform="rotate(-30 34 86)"/><ellipse cx="166" cy="86" rx="10" ry="19" transform="rotate(30 166 86)"/>',
   sweat=(160, 80), z=(150, 32)),
}

MOODS = {  # energy, lean (deg, around the feet), eyes, mouth, arms, extra
 "happy":     dict(energy=1.0,  lean=0,  eyes="dots",   mouth="smile", arms="side"),
 "tired":     dict(energy=0.55, lean=2,  eyes="flat",   mouth="flat",  arms="side"),
 "stiff":     dict(energy=0.18, lean=7,  eyes="flat",   mouth="frown", arms="side", sweat=True),
 "sleep":     dict(energy=1.0,  lean=0,  eyes="closed", mouth="sleep", arms="side", z=True),
 "celebrate": dict(energy=1.0,  lean=-4, eyes="happy",  mouth="open",  arms="up",   confetti=True),
 "stretch":   dict(energy=1.0,  lean=0,  eyes="happy",  mouth="smile", arms="up"),
}

def crust_layers(p, fill_top, full):
    band = p["sil"].replace('<path ', '<path fill="none" stroke-width="12" stroke-linejoin="round" ')
    if full:
        return f'<g id="crust" clip-path="url(#sil)" stroke="{p["crust"]}">{band}</g>'
    return (f'<g id="crust_drained" clip-path="url(#sil)" stroke="{p["crust_grey"]}">{band}</g>'
            f'<g id="crust" clip-path="url(#filled)"><g clip-path="url(#sil)" stroke="{p["crust"]}">{band}</g></g>')

def mood_svg(key, mood):
    p, m = PETS[key], MOODS[mood]
    fill_top = round(36 + (1 - m["energy"]) * 160, 1)
    feet = p["color"] if fill_top < 185 else GREY
    arms_col = p["color"] if fill_top < 128 else GREY
    arms = p["arms_up"] if m["arms"] == "up" else p["arms_side"]
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 210" width="400" height="420">',
         f'<defs><clipPath id="sil">{p["sil"]}</clipPath><clipPath id="filled"><rect y="{fill_top}" width="200" height="210"/></clipPath></defs>']
    if m.get("confetti"): s.append(CONFETTI)
    # Feet stay planted; everything else leans around (100, 192).
    s.append(f'<g id="feet" fill="{feet}">{FEET}</g>')
    s.append(f'<g transform="rotate({m["lean"]} 100 192)">')
    s.append(f'<g id="arms_{m["arms"]}" fill="{arms_col}">{arms}</g>')
    s.append(f'<g id="silhouette" clip-path="url(#sil)"><rect width="200" height="210" fill="{GREY}"/>'
             f'<rect y="{fill_top}" width="200" height="210" fill="{p["color"]}"/>')
    if m["energy"] < 1: s.append(f'<rect y="{fill_top}" width="200" height="4" fill="{p["edge"]}"/>')
    s.append('</g>')
    if "crust" in p: s.append(crust_layers(p, fill_top, m["energy"] >= 1))
    s.append(EYES[m["eyes"]]); s.append(CHEEKS)
    if key == "pip": s.append(PIP_MOUTHS["open" if m["mouth"] == "open" else "closed"])
    else: s.append(MOUTHS[m["mouth"]])
    if m.get("sweat"):
        x, y = p["sweat"]; s.append(f'<path id="sweat" d="M{x} {y} Q{x-7} {y+13} {x} {y+18} Q{x+7} {y+13} {x} {y} Z" fill="{SWEAT}"/>')
    s.append('</g>')
    if m.get("z"):
        x, y = p["z"]
        s.append(f'<g id="zzz" fill="{ZCOL}" font-family="sans-serif" font-weight="700"><text x="{x}" y="{y}" font-size="24">z</text><text x="{x+16}" y="{y-18}" font-size="16">z</text></g>')
    s.append('</svg>')
    return "".join(s)

def base_svg(key):
    p = PETS[key]
    s = [f'<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 210" width="400" height="420">',
         f'<!-- Wigglebun pet "{p["name"]}". Same layer system as bun_pet.svg. Fill: draw #silhouette grey {GREY}, clip to it, '
         f'draw {p["color"]} from fillTop = 36 + (1 - energy) * 160 down, 4-unit edge {p["edge"]} at fillTop. -->',
         f'<g id="feet" fill="{p["color"]}">{FEET}</g>',
         f'<g id="arms_side" fill="{p["color"]}">{p["arms_side"]}</g>',
         f'<g id="arms_up" fill="{p["color"]}" display="none">{p["arms_up"]}</g>',
         f'<g id="silhouette" fill="{p["color"]}">{p["sil"]}</g>']
    if "crust" in p:
        band = p["sil"].replace('<path ', '<path fill="none" stroke-width="12" stroke-linejoin="round" ')
        s.append(f'<defs><clipPath id="sil">{p["sil"]}</clipPath></defs><g id="crust" clip-path="url(#sil)" stroke="{p["crust"]}">{band}</g>')
    s.append(EYES["dots"]); s.append(CHEEKS)
    s.append(PIP_MOUTHS["closed"] if key == "pip" else MOUTHS["smile"])
    s.append('</svg>')
    return "".join(s)

for key in PETS:
    d = f"{OUT}/{key}"; os.makedirs(f"{d}/pet_moods", exist_ok=True); os.makedirs(f"{d}/widget_png", exist_ok=True); os.makedirs(f"{d}/previews", exist_ok=True)
    open(f"{d}/{key}_pet.svg", "w").write(base_svg(key))
    cairosvg.svg2png(bytestring=base_svg(key).encode(), write_to=f"{d}/previews/{key}_pet.png", output_width=400, output_height=420)
    for mood in MOODS:
        svg = mood_svg(key, mood)
        widget_mood = "sleep" if mood == "sleep" else mood
        open(f"{d}/pet_moods/{key}_{mood}.svg", "w").write(svg)
        cairosvg.svg2png(bytestring=svg.encode(), write_to=f"{d}/widget_png/{key}_mood_{widget_mood}.png", output_width=480, output_height=504)
json.dump({k: {kk: vv for kk, vv in v.items() if kk not in ("sil", "arms_side", "arms_up")} for k, v in PETS.items()}, open(f"{OUT}/pets_meta.json", "w"), indent=2)
