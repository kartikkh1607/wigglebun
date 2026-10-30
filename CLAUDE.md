# CLAUDE.md — Wigglebun (codename: `wigglebun`)

> App name: **Wigglebun** · Play Store title: "Wigglebun: Desk Break Pet" · Starter pet: **Bun** · A break is called a **"Bun Break"** in copy and notifications.

> Project brief for Claude Code. Read this whole file before writing any code.
> Owner: Kartik. Platform: **Android only**, native **Kotlin + Jetpack Compose**.

---

## 1. What we are building

A cute virtual-pet app that gets desk workers and students to **stand up, stretch and move** during long sitting sessions.

**Core loop (one sentence):** the longer you sit, the more tired and grey your pet looks; take a short break (stand, walk, stretch) and its colours come back and it celebrates.

**Who it is for:** people who sit at a computer for hours (developers, students, office workers). Primary market: US / UK / EU English speakers. Localise later.

**Inspiration (patterns only, never copy their art, names or exact layouts):**
- *Waterllama* (iOS): the character itself is the progress bar. A plain silhouette fills with coloured bands as you drink. Many collectible characters, all built from silhouettes.
- *Finch* (Android + iOS): pet with a name, daily goals that give currency, Done / Snooze / Skip on a goal, widget card, soft paywall with 7-day trial.
- *Standland* (iOS only): creatures wake up when you move and fall asleep when you don't. Proves the idea; not on Android.

**Our edge:** Android-native, automatic movement detection (no manual logging), pet = sitting meter, snooze that respects meetings.

---

## 2. Hard constraints

1. **Zero spend before launch.** Do not add any paid SDK, paid API, paid font, paid asset or backend service. Only free/open-source libraries.
2. **No own backend, no login.** All app data stays on device (Room + DataStore). The only server-side piece is **RevenueCat** for purchase verification/acknowledgement (free under $2,500 monthly tracked revenue, then 1%). Declare purchase history in the Play "Data safety" form.
3. **No AI/LLM features in v1.**
4. **Target SDK 36** (Google Play requires API 36 for new apps and updates from 31 Aug 2026). `targetSdk = 36`, `minSdk = 26`, `compileSdk = 37` (current Compose / Navigation / core-ktx require minCompileSdk 37; compiling against 37 does not change runtime behaviour).
5. **Play Billing Library 8 or newer** (current is 9.x). Required for new apps.
6. All user-facing text in `res/values/strings.xml` from day one (localisation later).
7. Respect battery: no always-on foreground service in v1, no exact alarms (`SCHEDULE_EXACT_ALARM` must not be requested).

---

## 3. Tech stack

| Area | Choice |
|---|---|
| Language | Kotlin (latest stable), coroutines + Flow |
| UI | Jetpack Compose, Material 3 as base, heavily custom-themed |
| Architecture | Single Activity, MVVM + unidirectional data flow (`UiState` data classes, `ViewModel` exposes `StateFlow`) |
| DI | Hilt |
| Navigation | Navigation Compose, bottom tab bar |
| Local DB | Room |
| Settings | DataStore (Preferences) |
| Background | WorkManager + inexact `AlarmManager` (`setAndAllowWhileIdle`) |
| Movement | Google Play services Activity Recognition **Transition API**: `WALKING` / `RUNNING` enter+exit and `STILL` enter+exit (`ON_FOOT` is not a supported Transition API activity). Step counter (`TYPE_STEP_COUNTER`) as a best-effort fallback |
| Widget | Jetpack Glance (`androidx.glance:glance-appwidget`, stable 1.2.x) |
| Billing | RevenueCat Android SDK (wraps Google Play Billing Library 8+/9.x; does server-side verification + acknowledgement so no own backend is needed) |
| Images | Vector drawables (SVG imported via Android Studio "Vector Asset") + PNG for widget mood images |
| UI tests | Compose `@Preview` for every screen + Roborazzi screenshot tests on Robolectric (so Claude can render and look at the UI). Reference PNGs are checked in at `app/src/test/screenshots/`: `./gradlew recordRoborazziDebug` writes them, `./gradlew verifyRoborazziDebug` fails on any difference (diffs in `app/build/outputs/roborazzi/`) |

Ask Kartik before adding any dependency not in this table.

---

## 4. Module / package layout

Single `app` module is fine for v1.

```
com.kartikkhandelwal.wigglebun
├── data/
│   ├── db/            Room entities, DAOs, AppDatabase
│   ├── prefs/         DataStore wrappers
│   └── repo/          Repositories (SitRepository, BreakRepository, PetRepository, WalletRepository)
├── domain/
│   ├── sitting/       SittingEngine (pure Kotlin, unit-tested)
│   └── model/         Pet, Mood, BreakType, BreakLog, Item
├── activity/          ActivityRecognition receiver + registration
├── notify/            Notification channels, break reminder builder, action receivers (Done/Snooze/Skip)
├── widget/            Glance widget + receiver
├── billing/           BillingManager wrapper
├── ui/
│   ├── theme/         Color.kt, Type.kt, Shape.kt, Dimens.kt, Theme.kt (design tokens live here)
│   ├── components/    ChunkyButton, PillCard, RewardBadge, ProgressPill, TabBar, BottomSheet
│   ├── pet/           PetView (silhouette fill renderer), PetAnimations
│   ├── onboarding/
│   ├── home/
│   ├── breaksession/
│   ├── collection/
│   ├── pet/profile/
│   ├── settings/
│   └── paywall/
└── MainActivity.kt
```

---

## 5. Domain logic

### 5.1 Sitting engine (most important logic, keep it pure and unit-tested)

v1 rule set, simple and robust:

- User sets **work hours** (default Mon–Fri 09:00–18:00) and a **break interval** (default 45 min, range 20–90).
- During work hours a **sit timer** runs.
- The sit timer **resets** when any of these happen:
  - user completes a break in the app (Done),
  - Activity Recognition (primary signal) reports a walking/running period that lasts ≥ 2 minutes. We subscribe to `WALKING` and `RUNNING` enter+exit plus `STILL` enter+exit; `ON_FOOT` is not supported by the Transition API. The period ends on the last moving exit or a `STILL` enter, and the timer restarts at that moment,
  - phone step counter (if available, best effort) shows ≥ 100 new steps since the timer started. The sensor only counts while a listener is registered, and we have no foreground service, so it counts only while the app process is alive during work hours.
- When the timer reaches the interval → send a break reminder notification.
- **Snooze** pushes the reminder by 10 min (max 3 snoozes in a row), and only once the reminder is due. **Skip** resets the timer without reward.
- Outside work hours: nothing runs, pet sleeps peacefully (no guilt).

Known limitation to handle gracefully: `STILL` means the *phone* is still, not the person. That is why the engine relies on "movement resets the timer" rather than "STILL means sitting".

### 5.2 Pet mood from sit timer

| Sit time vs interval | Mood | Visual |
|---|---|---|
| 0–50% | `HAPPY` | energy level 100→75%, dot eyes, small nose-mouth, bouncy idle |
| 50–100% | `TIRED` | energy level 75→25%, flat-line eyes, flat mouth, slower idle, 2° lean |
| >100% | `STIFF` | energy level ≤20%, flat eyes, frown, 7° slump, sweat drop |
| during a break | `STRETCH` | full energy, arms up, happy ^ ^ eyes (pose frames take over) |
| just finished a break | `CELEBRATE` | full energy, ^ ^ eyes, open smile, jump + confetti, 3 s, then `HAPPY` |
| outside work hours | `SLEEPING` | full energy, closed curved eyes, "z z" |

Energy level is continuous: `energy = clamp(1 - sitMinutes / (interval * 1.2), 0.15, 1)`. Animate changes with a slow tween (≥ 600 ms) so the colour visibly drains/refills.

### 5.3 Break types (user can toggle which appear on Home)

Stand up · 2-min walk · Neck stretch · Shoulder rolls · Wrist stretch · Eyes rest (20-20-20) · Drink water. Each has a colour, a duration, a coin reward (default 5) and 4–6 simple step illustrations.

### 5.4 Economy

- Currency: one soft currency (working name: "sparks"). Earned only by completing breaks and streak milestones.
- A break type earns its spark reward only the first time it is completed each day; repeat Done resets the timer and celebrates but gives no sparks.
- Spend on: new pets (collection) in v1; outfits in v2. Pet prices (`domain/rewards/PetPrices.kt`): Pip 75, Toast 175, Nimbus 300 sparks. Targets are in work days; `EconomyTest` pins them (see the table below).
- Streak: number of work days in a row with at least N breaks (default 4).
- Streak bonuses (`domain/rewards/RewardRules.kt`): the 3 / 7 / 14 / 30-day milestones (20 / 50 / 100 / 250 sparks) pay **once per lifetime** (ledger key `milestone:<days>`), so breaking a streak on purpose and rebuilding it never pays them again. After 30 days, every further 30 days of the **same unbroken streak** (day 60, 90, …) pays +250 (key `streak:<days>:<start>`). DB v4 migrated older installs: each milestone's first per-streak payment was renamed to its lifetime key, so nothing is paid twice and no sparks are taken back.
- Work days to afford each pet, bought cheapest first (`EconomyTest`; every day a work day):

  | User | Breaks a day | Sparks a day | Pip (75) | Toast (+175) | Nimbus (+300) |
  |---|---|---|---|---|---|
  | Light | 3 types, below the streak goal | 15 | day 5 | day 17 | day 37 |
  | Normal | 4 types incl. Walk, streak kept | 25 + milestones | day 3 | day 8 | day 16 |
  | Heavy | all 7 types, streak kept | 40 + milestones | day 2 | day 6 | day 12 |

### 5.5 Premium (hard paywall with free trial)

- Model: **hard paywall + free trial.** The app's core loop requires an active trial or subscription.
- Paywall appears in the **first session, right after one value moment** (pet wakes up, user names it, does the first 1-minute stretch, pet celebrates). Most trial starts happen on install day, so this first paywall is the most important screen in the app.
- Products: yearly subscription with a **7-day free trial** (default, highlighted), monthly subscription, lifetime one-time. Prices decided in Play Console. The trial length lives in code in one place, `billing/Trial.kt` (`FREE_TRIAL_DAYS`); the Play Console offer's free phase must match it.
- In Play Console, make sure the yearly base plan has an offer with a **free phase**; in code, check the offer actually has a free trial phase before showing "Start free trial" text.
- **Trial-end reminder:** Android does not send one. Schedule our own local notification 2 days before trial ends (`Trial.END_REMINDER_DAYS_BEFORE`), i.e. on day 5 of the 7 ("Your free trial ends on …"). Honest and reduces refunds/1-star reviews.
- Paywall must show: price after trial, billing period, how to cancel (Play Store → Subscriptions), restore purchases, terms + privacy links.
- If the user closes the paywall: show a calm "locked" home with the sleeping pet and a single "Start free trial" button. No dark patterns, no fake timers.
- Entitlement checks go through RevenueCat (`CustomerInfo`), cached for offline use.

---

## 6. Screens for v1 (build in this order)

1. **Splash** – logo only.
2. **Onboarding**
   - Wake-up moment: pet asleep as a round blob → user taps → pet stretches awake and introduces itself, asks the user's name, user names the pet. The name field allows at most `MAX_PET_NAME_LENGTH` (12, `domain/model/Pet.kt`) characters.
   - One question per screen: work hours, break interval. Bottom sheets for pickers. "Skip" always visible.
   - Permissions are **not** asked here (ask just-in-time, see Settings/Home cards).
   - First 1-minute stretch with the pet → celebrate → **paywall with free trial** (see 5.5).
3. **Home** (tab 1)
   - Top half: desk scene background `design/desk_scene.jpg` (convert to WebP, put in `res/drawable-nodpi`, crop to fill ~340 dp height) with the pet standing on the floor in the centre (pet = sitting meter, see 5.2). Menu button top-left, sparks counter pill top-right.
   - Under it: pill showing "Time since last break: 32 min" + thin progress. The minutes take their own width and the mood chip ("Bun is getting tired") sits at the right edge in the rest of the row.
   - "Breaks for today" list: cards with icon, title, reward, chunky check button.
   - Tapping a card opens a **focused card overlay** with three big actions: Skip · Done · Snooze.
4. **Break session** – full-screen guided stretch with pet doing the move, step text, countdown ring, "Done" → celebrate + reward.
5. **Collection** (tab 2) – grid of pet silhouettes; locked ones shown as grey silhouettes with a price; tap to preview.
6. **Pet profile** (tab 3, tab label = pet's name) – name, streak card, total breaks, weekly bar chart.
7. **Menu / Settings** – cards: "Add the widget", "Enable notifications", "Enable movement detection" (each opens the permission flow), work hours, interval, break types toggles, Premium card ("Try 7 days free", from `Trial.FREE_TRIAL_DAYS`), privacy policy, about ("Made with love in India").
8. **Paywall** – pet illustration, 3 benefits, plan selector (yearly highlighted), "Start free trial" CTA, price after trial in plain text, how to cancel, restore purchases, terms/privacy links.

**Not in v1:** friends, seasons/events, outfits, furniture, adventure map, AI coach, iOS.

---

## 7. Pet rendering (the heart of the UI)

**Source of truth: `design/bun_pet.svg`** (viewBox 0 0 200 210, layered groups: `feet`, `arms_side`, `silhouette` = body + ears, `ears_inner`, `eyes`, `cheeks`, `mouth`). Draw the pet in Compose `Canvas` with these exact paths/shapes (port the SVG path data to `Path`/`PathParser`), not as one flattened VectorDrawable, because layers animate independently.

**Mechanic = energy level (NOT colour bands).** The pet's own colour drains like a battery as the user sits:
- draw `silhouette` in grey `#E9E5DE`,
- clip to `silhouette` and draw the pet colour `#F5DC8B` from `fillTop` to the bottom, where `fillTop = 36 + (1 - energy) * 160` (viewBox units); draw a 4-unit darker edge `#EBCB6A` at `fillTop` when energy < 1,
- `feet` and `arms_side` are outside the clip: colour them pet-yellow only if `fillTop` is above them (feet ≈ y 185, arms ≈ y 128), else grey,
- `ears_inner`, `eyes`, `cheeks`, `mouth` always drawn on top; eye/mouth shape per mood (see 5.2). Exact per-mood geometry: `design/pet_moods/bun_<mood>.svg` (happy, tired, stiff, sleep, celebrate, stretch) and `design/mockups_source/Pet.dc.html`.

Animation:
- Idle: breathing (scaleY 1.00↔1.03 around the feet, ~2.5 s), random blink every 3–6 s (eyes scaleY → 0.1 for 120 ms), small bob; slower and smaller when TIRED/STIFF.
- Lean: rotate everything except `feet` around (100, 192) by the mood's lean angle, spring-animated.
- Celebrate: squash & stretch jump (scale 1.1×0.85 → 0.92×1.1 → 1×1) + Canvas confetti, 3 s.
- Break session poses: `design/neck_stretch_frames.svg`, `design/stand_up_frames.svg`, `design/more_stretch_frames.svg` (shoulder rolls, wrist stretch, march in place). Each pose is the same layers with a transform (rotation, or scaleX/scaleY about the feet) and different arm/eye layers. Implement poses as data (`PoseSpec(rotation, scaleX, scaleY, arms, eyes)`) and animate between them with springs, holding each pose ~1.2 s. Do **not** invent new body shapes.
- Collection pets (Pip, Toast, Nimbus, ...): same system, own silhouette + own colour.
- Performance: no gradients; test on a mid-range device; keep frame time < 16 ms. `PetGeometry` builds every path, stroke and paint once per `PetSpec` (remembered; size-independent because drawing happens in viewBox units), so a frame only issues draw calls. Toast's crust adds one small offscreen layer and one path stroke per frame; precomputing the stroke as a fill path would change pixels (Skia strokes at the canvas scale), so measure on a phone before optimising it.

---

## 8. Widget (Glance)

- Small (2×2): pet mood image + "32 min" since last break + tap → app.
- Medium (4×2): pet + time + "Take a break" button (opens Break session).
- Large font sizes (`WidgetTextFit` in `widget/WidgetContent.kt`): text is never shrunk below the user's font scale and never truncated; the text column takes priority over the pet. Above 100% (e.g. 130%) the medium pet narrows to 56 dp so "since last break" and "Take a break" fit whole. From 160% (e.g. 200%) the pet is hidden and the "since last break" line is left out, as on the small widget; TalkBack still reads "32 min since last break. Bun, tired". Roborazzi references: `widget_medium_font_1_0/1_3/2_0`, `widget_medium_sleeping_font_2_0`, `widget_small_font_2_0`; Home at 200%: `home_*font_2_0*`.
- Glance cannot run Compose animations; use one static PNG per mood per pet (480×504, transparent) in `res/drawable-nodpi`. Every pet's `<pet>_mood_<mood>.png` is rendered from `PetView` by `WidgetPetImageTest` as a Roborazzi reference: `recordRoborazziDebug` rewrites them, `verifyRoborazziDebug` fails when they no longer match `PetView`. Don't copy the older `design/widget_png` or `design/pets/<pet>/widget_png` exports over them.
- The widget shows the active pet (the one "with you" in Collection); choosing or adopting a pet redraws it at once.
- Update via WorkManager every 15 min during work hours and immediately after a break/snooze.

---

## 9. Notifications

- Channel `break_reminders` (default importance).
- Reminder text varies and is written from the pet's point of view, e.g. "[PetName] is getting stiff… 2-minute stretch?"
- Actions: **Done** (marks break, reward), **Snooze 10 min**, **Skip**. Handled by a `BroadcastReceiver` → repository → refresh widget.
- Ask `POST_NOTIFICATIONS` just-in-time (Android 13+).

---

## 10. Permissions

| Permission | When asked | Why (shown to user before the system dialog) |
|---|---|---|
| `POST_NOTIFICATIONS` | Menu card or first break reminder setup | So your pet can remind you |
| `ACTIVITY_RECOGNITION` | Menu card "Enable movement detection" | So walking resets the timer automatically |

App must work fully (timer + manual Done) if both are denied.

---

## 11. Design system

Reference mockups: `design/mockups_source/*.dc.html` (HTML source of every mockup screen, 390×844 phone artboards; read them for exact layout, spacing, sizes and copy; they need a runtime to render, so read them as code) and any screenshots Kartik adds in `design/mockups/`. Match them. Full file map: `design/README.md`.

**Colours** (`ui/theme/Color.kt`)

| Token | Hex | Use |
|---|---|---|
| `Cream` | `#FFF4E8` | app background |
| `Card` | `#FFFFFF` | cards, tab bar |
| `CardEdge` | `#EFE3D4` | chunky edge under cards |
| `Ink` | `#2B2A3A` | text, primary button |
| `InkEdge` | `#14131F` | primary button edge |
| `InkMuted` | `#6B6A7B` | secondary text (passes 4.5:1 on Cream and Card) |
| `Track` | `#F1EBE3` | progress track |
| `DoneGreen` / edge | `#3BAA6E` / `#2C8655` | done buttons, checks |
| `Sun` / edge | `#FFC857` / `#D69E2A` | highlight button, sparks icon |
| `Coral` | `#E5603A` | over-interval state, active tab icon |
| `Night` | `#2F2E45` | onboarding sleep screen |
| `BreakSky` | `#EAF3FD` | break session background |
| `PetYellow` / edge | `#F5DC8B` / `#EBCB6A` | Bun body |
| `PetGrey` | `#E9E5DE` | drained body |
| `PetPink` | `#F6A98A` | inner ears |
| `PetBlush` | `#F7A58B` | cheeks |

Break-type colours (icons, chips): Walk `#7DD3B0`, Neck `#7FB8F5`, Stand `#FFC857`, Eyes `#B9A3F0`, Wrist `#F68A6B`, Water `#F4A3C0`. Use their light tints for icon tiles: `#CDEFE0`, `#CFE3FB`, `#FFE3A3`, `#E6DDFB`, `#FDDCD2`, `#FCE1EC`.

The weekly chart (Pet profile, "Last 7 days") uses category colours, not one per break type: Movement `#7DD3B0` (Stand up, Walk), Stretch `#7FB8F5` (Neck, Shoulders, Wrist), Eyes `#B9A3F0`, Water `#F4A3C0`; stacked Stretch, Movement, Eyes, Water from the bottom so Stretch blue and Eyes lilac never touch.

Mood chip backgrounds: happy `#DDF3E9`, tired `#FFF0CC`, stiff `#FDDCD2`. Timer bar: happy `#4DB88A`, tired `#F2A93B`, stiff `#E5603A`.

**Type** (Google Fonts, bundled in `res/font`, free OFL licence)
- Fredoka SemiBold 600: display 32 sp, title 22 sp, button 18 sp
- Nunito ExtraBold 800: card title 17 sp; Bold 700: labels 13–15 sp; SemiBold 600: body 15 sp

**Shape & spacing**
- Radius: cards 24 dp, icon tiles 16 dp, small buttons 16–18 dp, focused card 28 dp, bottom sheets 28 dp top, pills fully rounded
- Screen padding 16 dp, card padding 12–16 dp, list gap 10 dp

**Chunky buttons / cards:** solid offset "edge" below (cards 3–4 dp `CardEdge`, buttons 4–5 dp in the darker shade). On press: translateY by the edge height and remove the edge (80 ms), release springs back.

**Motion:** no linear easing; pop-ins with slight overshoot; Home list items stagger in (40 ms apart); energy changes tween ≥ 600 ms.

**Pet names in text:** a name never changes a card's height. A sentence with the name in it ("Bun is getting tired", "Choose Bun") uses `PetNameSentence`, which shortens only the name with "…" when it would add a line ("Wigglebo… is getting tired"); the words around it still wrap at large font sizes, and TalkBack reads the whole name. A name shown alone (titles, collection cards, tab label) uses `maxLines = 1` with `TextOverflow.Ellipsis`. Screenshot tests use `TestPetNames.LONG` / `WIDEST` (12 letters).

---

## 12. Play Store readiness checklist

- [ ] Target SDK 36, Billing Library ≥ 8
- [ ] Privacy policy page (host free on GitHub Pages or Vercel)
- [ ] Data safety form: activity data processed on device only, not collected/shared
- [ ] **Health apps declaration** (Play Console → Policy → App content). Every app must fill it; this app likely falls under "Activity and Fitness"
- [ ] Closed test with **12 testers for 14 continuous days** if the developer account is a personal account created after 13 Nov 2023
- [ ] Store listing: icon, feature graphic 1024×500, screenshots, optional YouTube preview video

---

## 13. How Claude should work in this repo

1. Work **milestone by milestone** (section 14). Finish, build, and show before moving on.
2. After every change: `./gradlew assembleDebug` must pass; run unit tests for `domain/`; `./gradlew lintDebug` must pass (it catches APIs missing on older Android versions, which the Robolectric tests on API 36 cannot).
3. For every UI change: add/update a `@Preview` and a Roborazzi screenshot test, render it, **look at the PNG yourself**, fix obvious issues, then show Kartik.
   After every UI change run `./gradlew verifyRoborazziDebug` so unintended changes to existing screens fail the build. When a change is intended, re-record (`recordRoborazziDebug`) and tell Kartik exactly which reference images changed (`git status app/src/test/screenshots`).
   **CI is the source of truth** (`.github/workflows/android.yml`): every PR and every push to `main` runs `assembleDebug lintDebug verifyRoborazziDebug` on a Linux runner; on failure the screenshot diffs are uploaded as the `roborazzi-diffs` artifact. Reference images are recorded and verified on Linux only. Local Windows runs can show tiny font-rendering differences: don't re-record from Windows; record on Linux (CI image or a Linux machine) instead.
4. When Kartik attaches reference screenshots or mockups, match spacing, radius, hierarchy and motion feel; never copy another app's artwork or brand.
5. Keep composables small and stateless; state lives in ViewModels.
6. No hard-coded strings, colours or dimensions inside screens; use theme tokens.
7. Explain non-obvious Android behaviour (background limits, permissions) in short code comments.
8. Never add paid services (RevenueCat is the one approved exception: free until the app earns). Ask before any new dependency.
9. **Branches and PRs only.** Always work on a branch and open a PR for Kartik to review. Never push to, merge into or fast-forward `main` without Kartik's explicit approval for that PR. This applies with extra care to anything touching Room migrations (`data/db/`, `app/schemas/`), which can't be undone on installed devices. One PR per requested item unless Kartik says otherwise.

---

## 14. Milestones

| # | Milestone | Done when |
|---|---|---|
| M1 | Project skeleton: Gradle, Hilt, theme tokens, bottom tabs with empty screens, Room + DataStore | App runs, tabs switch, previews render |
| M2 | `PetView` with silhouette fill, moods, idle + blink + celebrate | Preview shows all 5 moods with a placeholder pet |
| M3 | Sitting engine + work hours + interval + reminders with Done/Snooze/Skip | Unit tests pass; reminder fires on device |
| M4 | Activity Recognition + step counter resets | Walking resets timer on a real phone |
| M5 | Home screen (scene, timer pill, break cards, focused overlay) | Matches approved mockup |
| M6 | Break session screen + rewards + streak | Completing a break updates pet, coins, streak |
| M7 | Glance widget (small + medium) | Widget reflects mood within 15 min and instantly after a break |
| M8 | Collection + unlock with sparks; Pet profile tab (name, streak card, total breaks, weekly chart) | Can unlock and switch pets; profile shows the streak, total breaks and this week's chart |
| M9 | Onboarding (wake-up, naming, settings) + just-in-time permission cards | Fresh install flows end to end |
| M10 | RevenueCat + paywall + restore + trial-end reminder | Test purchases work with license testers; entitlement survives offline restart |
| M11 | Polish, accessibility (TalkBack labels, contrast), privacy policy, store assets | Ready for closed testing |

---

## 15. Open decisions (ask Kartik)

- ~~App name~~ Decided: Wigglebun (pending domain + trademark check by Kartik)
- ~~Final colour palette and font~~ Decided: see section 11
- Prices (~~trial length~~ decided: 7 days)
- Which 3 pets ship in v1
- Streak milestone amounts (3 / 7 / 14 / 30 days → 20 / 50 / 100 / 250 sparks, once per lifetime, then +250 per further 30 days of one streak; amounts are placeholders in `domain/rewards/RewardRules.kt`)
- ~~Pet prices~~ Decided: Pip 75 / Toast 175 / Nimbus 300 sparks (`domain/rewards/PetPrices.kt`)
