# Collection pets: Pip, Toast, Nimbus

Same system as Bun (`design/bun_pet.svg`): viewBox `0 0 200 210`, same layer names, same face
positions (eyes y 106, cheeks y 118, mouth y 112), same feet (y 192), same pivot (100, 192) for
mood lean, same energy fill: grey `#E9E5DE` silhouette, pet colour clipped from
`fillTop = 36 + (1 - energy) * 160`, 4-unit edge colour at `fillTop` when energy < 1.
Feet are coloured when `fillTop < 185`, arms when `fillTop < 128` (same as Bun).
Feet stay planted in every mood (only the rest leans).

| Pet | Body | Edge | Notes |
|---|---|---|---|
| Pip (bird) | `#9EDCC3` | `#7FC9AC` | 3-feather tuft is part of the silhouette. **Beak replaces the mouth**: closed beak `#F4A261` in every mood except CELEBRATE (open beak). Wing-shaped `arms_side`. |
| Toast (bread) | `#EFC587` | `#DDAF6B` | Extra `crust` layer: the silhouette stroked 12 wide, clipped inside (6-unit band), `#C98F4E`. The crust drains with the fill: grey `#D6CFC4` above `fillTop`, crust colour below. The wide dome hides Bun's raised-arm position, so Toast's `arms_up` come out from the sides (y 118). |
| Nimbus (cloud) | `#C9B8F2` | `#B39FE6` | Silhouette = base + three circles (union). `arms_up` sit lower and wider (y 86). |

No `ears_inner` on any of the three. Cheeks `#F7A58B`, ink `#2B2A3A` as Bun.

Per-pet extras (viewBox units):
- Sweat drop (STIFF) start point: Pip (150, 74), Toast (140, 64), Nimbus (160, 80). Same drop shape as Bun.
- Sleep "z z" first z baseline: Pip (150, 40), Toast (176, 30), Nimbus (150, 32); second z at (+16, -18).
- `arms_up` ovals for STRETCH / CELEBRATE are in each `<name>_pet.svg` (hidden group `arms_up`).

Files per pet:
- `<name>/<name>_pet.svg`: layered base (happy), the source of truth for PetSpec.
- `<name>/pet_moods/<name>_<mood>.svg`: happy, tired (0.55), stiff (0.18), sleep, celebrate, stretch.
- `<name>/widget_png/<name>_mood_<mood>.png`: 480×504 transparent, for the Glance widget (M7).
- `<name>/previews/<name>_pet.png`.
- `all_moods_preview.png`, `collection_preview.png` (full / tired / locked silhouette): just for looking.
- `generate_pets.py`: regenerates everything (needs `cairosvg`): `python generate_pets.py <out_dir>`.

Break poses: reuse Bun's PoseSpec data. Arm positions in poses may need per-pet offsets
(especially Toast, whose dome covers the area above y ~104). Check every pose per pet in a
screenshot; don't invent new body shapes.
