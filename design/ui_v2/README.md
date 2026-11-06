# UI v2 screen designs

Visual direction for the refresh: Garden Studio (PR #30, direction B), taken further. These are layout and visual references, read as code like `design/mockups_source/*.dc.html`. Pets are drawn by `PetView` (CLAUDE.md §7); the pets in these files are references only.

| File | Screen |
|---|---|
| `Main.dc.html` | Home: room scene that flows into the list, speech bubble instead of the mood chip, streak goal in the timer card |
| `Collection.dc.html` | Collection: "next pet" progress card, locked silhouettes with prices |
| `Settings.dc.html` | Settings: "Bun & you" card, grouped sections, "Turn on" action for an off permission |
| `Locked.dc.html` | Home after the paywall is closed: night room, sleeping pet, one trial button |
| `WakeUp.dc.html` | Onboarding 1: wake-up at night |
| `Name.dc.html` | Onboarding 2: "Hi! That was some nap!" and naming on one screen, name suggestions |
| `Hours.dc.html` | Onboarding 3: work hours with a 24-hour awake/asleep bar |
| `Interval.dc.html` | Onboarding 4: nudge interval, pet after a break vs after sitting |
| `Stretch.dc.html` | Onboarding 5: first stretch, colour refilling, countdown ring |
| `Celebrate.dc.html` | Onboarding 6: celebration, sparks and streak progress |
| `Paywall.dc.html` | Onboarding 7: paywall with trial timeline and plan cards |

Phone artboards are 390×844. Fonts: Fredoka SemiBold and Nunito (already in `res/font`). Icons in `icons/` are the PR #30 break and Settings icons.

Palette used:

| Use | Hex |
|---|---|
| Home wall / lower wall | `#D6EBDA` / `#C8E2CF` |
| Home floor / seams / card edge | `#F4E8D6` / `#EADCC6` / `#E3D3BC` |
| Rug / rug edge | `#CFE3FB` / `#B2D0F2` |
| Collection / edge | `#DDF1E6` / `#BFE0CE` |
| Settings / edge | `#ECE9F7` / `#D6D1EC` |
| Break session and paywall top | `#EAF3FD` / `#E2EEFB` |
| Onboarding night | `#2F3352` wall, `#2A2C45` floor |
| Muted text on tinted backgrounds | `#56556A` |
