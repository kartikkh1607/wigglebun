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
| `app_icon_foreground.svg` | Adaptive icon foreground: Bun's head with a chunky edge and two sparks (108×108, safe zone respected). Also the splash's icon | Ported by hand to `res/drawable/ic_launcher_foreground.xml` |
| `app_icon_background.svg` | Adaptive icon background: night-sky radial gradient | `res/drawable/ic_launcher_background.xml` |
| `splash_wordmark.py` | Prints "Wigglebun" in Fredoka SemiBold as path data (needs `fonttools`) | `python design/splash_wordmark.py` → `res/drawable/splash_wordmark.xml` |
| `launcher_monochrome.py` | Writes the themed-icon layer: the icon's silhouette with the face cut out (needs `shapely`) | `python design/launcher_monochrome.py` → `res/drawable/ic_launcher_monochrome.xml` |
| `play_store_icon_512.png` / `.svg` | 512×512 Play Store listing icon: both launcher layers, cropped to what a launcher shows (`rsvg-convert` makes the PNG) | Play Console store listing |
| `previews/*.png` | PNG renders of the SVGs above | Just for looking |
| `ui_v2/*.dc.html` | The UI v2 refresh: Home, Collection, Settings, Locked Home, onboarding (wake-up, name, hours, interval, stretch, celebrate, paywall); palette in `ui_v2/README.md` | Layout reference, read as code. Wins over `mockups_source` where they differ |
| `ui_v2/previews/*.png` | Headless-browser renders of the ui_v2 screens at 2× (390×844 artboards). `paywall.png` shows `{{ … }}` where the mockup's runtime fills in the plans and button | Just for looking, and for PR comparisons |
| `ui_v2/icons/*.svg` | Full-colour break and Settings icons | Converted to `res/drawable/ic_break_*.xml` and `ic_settings_*.xml` (drawn untinted) |
| `mockups_source/*.dc.html` | HTML source of the first mockups (Home tired/stiff, break card, break session, wake-up onboarding, paywall, collection, pet moods, tokens) | Layout reference — read as code. Still the source for the focused card, `Pet`, `PetMoods` and `Tokens` |
| `mockups/` | Put screenshots of the canvas here | Visual reference |
| `reference/` | Original Gemini / AI Studio concept images | Inspiration only — do not ship these files |
