# Visual refresh: three colour directions (mockups only)

Mockups for the polish pass before M10. Nothing here is used by the app yet; once Kartik picks a
direction, the app work changes `ui/theme/Color.kt` tokens and the screens that use them.

| File | What it is |
|---|---|
| `previews/direction_a_honey_loft.png` | Direction A: Home, Collection, Settings |
| `previews/direction_b_garden_studio.png` | Direction B |
| `previews/direction_c_bold_playroom.png` | Direction C |
| `previews/icons.png` | The new break and Settings icons, large and at tile size (same in every direction) |
| `icons/*.svg` | Icon sources, 48-unit grid, ready for Android Studio's Vector Asset import |
| `mockup.html` | Source of every preview (`?d=a`, `b`, `c` or `icons`); the scene and pets are drawn in it |
| `render.py` | `python design/refresh/render.py` re-renders `previews/` with headless Chrome |

## The same in every direction

1. **A background per tab, cards stay white.** Each card's chunky edge is a darker shade of the
   background it sits on, so the edge still reads on colour.
2. **Home's scene flows into the list.** The floor colour carries on below the scene, under the
   timer pill and the break cards, with faint floorboards. Bun stands on a rug, so it is standing
   *somewhere*.
3. **Bun stands clear of the desk.** The scene is redrawn as our own flat art (no more
   `desk_scene.jpg`): the desk and laptop moved left, window and a floor plant on the right, a wall
   clock on the open wall above Bun. Bun keeps its place and size (`Dimens.Home.PetTop` 116,
   `PetSize` 190); its ears now have plain wall behind them.
4. **Filled break icons**, one per type, in one style (below). 36 dp inside the 48 dp tile (the line
   icons were 26 dp).
5. **Settings grouped**: one card per section with inset dividers, a coloured icon tile per row,
   row titles 17 sp ExtraBold (card-title size), values in Fredoka 22 sp.

## The three directions

| Token (proposed) | A Honey Loft | B Garden Studio | C Bold Playroom |
|---|---|---|---|
| Home wall | `#FFDDBB` | `#D8EAD9` | `#C8D1F3` |
| Home floor (scene + list) | `#F6E3C8` | `#F3EADC` | `#EFCB9C` |
| Home card edge | `#E9CFAA` | `#E3D5C0` | `#DCB27F` |
| Collection background / edge | `#FCE6DC` / `#F0CDBF` | `#DDF1E6` / `#BFE0CE` | `#BFE6D5` / `#9DD1BB` |
| Pet tab background / edge | `#FFF0CC` / `#F2DCA9` | `#E2EEFB` / `#C7DAF1` | `#FBE19B` / `#E9C76E` |
| Settings background / edge | `#F4EDE3` / `#E4D8C8` | `#ECE9F7` / `#D6D1EC` | `#D3DAF6` / `#B7C1EA` |
| Rug | `#F9C3AE` | `#CFE3FB` | `#F59C82` |

The scene's other colours (desk, frame, clock, mug, pot) are in `mockup.html` (`D.<a|b|c>.scene`);
they become `Scene*` tokens in `Color.kt` for the chosen direction.

**Contrast (WCAG AA).** Ink `#2B2A3A` is at least 9.1:1 on every tab background in every direction.
Secondary text that sits straight on a tab background ("2 of 7 done", the Collection subtitle)
needs a darker muted: today's `InkMuted` `#6B6A7B` falls below 4.5:1 there (A: on Home and
Collection, 4.2 to 4.4; B: 4.4 everywhere; C: 3.5 to 4.1). `#56556A` passes everywhere (lowest
4.7:1, C's floor). Proposal: change `InkMuted` itself to `#56556A` (7.2:1 on white cards, 6.7:1 on
Cream, 6.5:1 on BreakSky), so one muted token works on every surface.

## Icon style

- 48-unit grid with about 2 units of margin. No gradients, no outlines, so they import as plain
  VectorDrawables.
- The main shape is filled in the break colour, on a copy of itself 2 units lower in a darker shade:
  the same chunky edge as our cards and buttons.
- Ink `#2B2A3A` for faces, laces and arrows (round caps); pet cheeks `#F7A58B`; one white highlight
  at 65 % opacity, top left.

| Break | Fill | Edge shade | Deep (small details) |
|---|---|---|---|
| Stand up | `#FFC857` | `#E9A93A` | `#D08A24` |
| 2-min walk | `#7DD3B0` | `#5BBE96` | `#3E9E78` |
| Neck stretch, Shoulder rolls | `#7FB8F5` | `#5E9FE6` | |
| Wrist stretch | `#F68A6B` | `#E36F50` | `#C9573A` |
| Eyes rest | `#B9A3F0` | `#A08AE4` | `#7E66C9` |
| Drink water | `#F4A3C0` | `#E683A7` | `#CF6890` |

Tiles keep the light tints from CLAUDE.md §11.

## Notes for the app work (after a direction is picked)

- The scene becomes a Compose Canvas drawing from tokens, cached with `drawWithCache` (it never
  animates, so it costs nothing per frame), drawn into the scene's height plus the status bar inset as
  today.
- Home is one `LazyColumn` with the scene as its first item. The floor colour goes on the list's
  background; the floorboards must scroll with the list, so they are drawn per item (or offset by the
  list's scroll position). They are optional: the plain floor colour alone already removes the hard
  edge.
- Almost every screenshot reference changes (backgrounds, icons, muted text), so that PR re-records
  them on Linux (CI) and lists every changed file.
