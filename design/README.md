# Wigglebun design assets

Everything the app needs visually. Vectors are the source of truth; PNGs are exports.

| Path | What it is | Where it goes in the app |
|---|---|---|
| `bun_pet.svg` | Bun, layered (feet, arms_side, silhouette, ears_inner, eyes, cheeks, mouth). viewBox 0 0 200 210 | Port path data into `ui/pet/PetView` (Compose Canvas) |
| `pet_moods/bun_<mood>.svg` | Bun in 6 moods: happy, tired, stiff, sleep, celebrate, stretch. Shows energy-drain fill, eyes, mouth, lean, extras | Reference for `PetView` mood specs |
| `widget_png/bun_mood_<mood>.png` | 480×504 transparent PNG per mood (older exports; the feet tilt with the lean) | Superseded: the widget's copies in `res/drawable-nodpi` are rendered from `PetView` by `WidgetPetImageTest` |
| `neck_stretch_frames.svg` | 4 poses: neutral, tilt right 20°, neutral, tilt left 20° | `PoseSpec` data for Neck stretch |
| `stand_up_frames.svg` | 4 poses: sit, crouch, stand, reach up (squash/stretch about the feet) | `PoseSpec` data for Stand up |
| `more_stretch_frames.svg` | Row 1 shoulder rolls, row 2 wrist stretch, row 3 march in place (2-min walk) | `PoseSpec` data |
| `desk_scene.jpg` | Home screen background (1104×960) | Convert to WebP → `res/drawable-nodpi/desk_scene.webp` |
| `pets/` | Collection pets Pip, Toast, Nimbus in Bun's layer system (see `pets/README.md`) | `ui/pet/PipSpec.kt`, `ToastSpec.kt`, `NimbusSpec.kt` |
| `app_icon_foreground.svg` | Adaptive icon foreground (108×108, safe zone respected) | Android Studio → New → Image Asset → Launcher Icons (Adaptive), background colour `#FFF4E8` |
| `launcher_monochrome.py` | Writes the themed-icon layer: the icon's silhouette with the face cut out (needs `shapely`) | `python design/launcher_monochrome.py` → `res/drawable/ic_launcher_monochrome.xml` |
| `play_store_icon_512.png` / `.svg` | 512×512 Play Store listing icon | Play Console store listing |
| `previews/*.png` | PNG renders of the SVGs above | Just for looking |
| `mockups_source/*.dc.html` | HTML source of every mockup screen (Home tired/stiff, break card, break session, wake-up onboarding, paywall, collection, pet moods, tokens) | Layout reference — read as code |
| `mockups/` | Put screenshots of the canvas here | Visual reference |
| `reference/` | Original Gemini / AI Studio concept images | Inspiration only — do not ship these files |
