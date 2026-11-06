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
  - phone step counter (if available, best effort) shows ≥ 100 new steps since the timer started. The sensor only counts while a listener is registered, and we have no foreground service, so it counts only while the app process is alive during work hours. Steps don't reset the timer while a walk is open (a reset is stamped when its batch arrives, which could beat the walk's real end); if the walk was too short, they still count once it has ended.
- When the timer reaches the interval → send a break reminder notification.
- **Snooze** pushes the reminder by 10 min (max 3 snoozes in a row), and only once the reminder is due. **Skip** resets the timer without reward.
- Outside work hours: nothing runs, pet sleeps peacefully (no guilt).

Known limitation to handle gracefully: `STILL` means the *phone* is still, not the person. That is why the engine relies on "movement resets the timer" rather than "STILL means sitting".

Lost exit events: a walk still open **45 minutes** after it started is closed at that point and resets the timer like a walk (`SittingEngine.MAX_OPEN_WALK`). Like every movement reset it logs no break, so it never earns sparks, a break type for the day or streak credit. A failed re-registration with Play services does not end tracking while the earlier registration can still be live (same boot and app version, see `MovementTracker`); it is retried on the next check.

### 5.2 Pet mood from sit timer

| Sit time vs interval | Mood | Visual |
|---|---|---|
| 0–50% | `HAPPY` | energy level 100→75%, dot eyes, small nose-mouth, bouncy idle |
| 50–100% | `TIRED` | energy level 75→25%, flat-line eyes, flat mouth, slower idle, 2° lean |
| >100% | `STIFF` | energy level ≤20%, flat eyes, frown, 7° slump, sweat drop |
| during a break | `STRETCH` | energy fills from what sitting had drained it to (read when the session opens) up to full over the whole session, arms up, happy ^ ^ eyes (pose frames take over); the "Colour coming back!" chip says so, and is left out when the pet began at full energy |
| just finished a break | `CELEBRATE` | full energy, ^ ^ eyes, open smile, jump + confetti, 3 s, then `HAPPY` |
| outside work hours | `SLEEPING` | full energy, closed curved eyes, "z z" |

Energy level is continuous: `energy = clamp(1 - sitMinutes / (interval * 1.2), 0.15, 1)`. Animate changes with a slow tween (≥ 600 ms) so the colour visibly drains/refills.

### 5.3 Break types (user can toggle which appear on Home)

Stand up · 2-min walk · Neck stretch · Shoulder rolls · Wrist stretch · Eyes rest (20-20-20) · Drink water. Each has a colour, a duration, a coin reward (default 5) and simple step illustrations, each step with a short title and a line of text: 2–4 steps in v1.0 (`ui/breaksession/BreakRoutines.kt`; Drink water has no prop art and Eyes rest only moves the eyes), 4–6 each in v1.1.

### 5.4 Economy

- Currency: one soft currency (working name: "sparks"). Earned only by completing breaks and streak milestones.
- **Break days.** "Day" for "Breaks for today", the once-a-day spark reward, the streak and the weekly chart starts at midnight, except when work hours cross midnight (end earlier than start, e.g. 22:00–06:00): then it starts at the shift start and is named after the evening the shift began, as `WorkSchedule` names the shift. So a break at 23:40 and one at 00:10 are the same day. Hours that end exactly at midnight keep midnight. `domain/sitting/BreakDays.kt` applies a history of boundaries (`DayBoundaryHistory`, recorded by `SettingsRepository.setSchedule` and adopted on app start for hours saved before it existed), so a change of hours never moves a break already done: the day in progress keeps its name until the new boundary starts a later day (switching to nights on Monday afternoon makes Monday last until Tuesday 22:00), and a date that a change skips (back to daytime hours on Tuesday afternoon, during Monday's shift day) has no breaks and is not a missed work day. Day lengths follow the wall clock, so DST days are 23 or 25 hours. Outside night-shift hours, Home's list is headed "Last shift" instead of "Breaks for today" (it is the last shift's breaks until the next shift starts); during the shift, and always for daytime hours, it says "Breaks for today". The day-of-change for work days and the streak rule (`DailyGoalHistory`) is the break day too.
- A break type earns its spark reward only the first time it is completed each day; repeat Done resets the timer and celebrates but gives no sparks.
- Spend on: new pets (collection) and accessories (4 in v1, then 4 more post-launch; see §6 and §7). Pet prices (`domain/rewards/PetPrices.kt`): Pip 75, Toast 150, Nimbus 300 sparks. Targets are in work days; `EconomyTest` pins them (see the table below).
- Streak: number of work days in a row with at least **3 completed breaks** (`RewardRules.DAILY_GOAL`), whichever types are switched on; repeats of one type count, so one type switched on is enough. The switches never change the goal. A change to the rule applies from the day an install first sees it and never re-judges a past day (`domain/rewards/DailyGoalHistory.kt`): days before an install adopted the 3-break rule keep the original one (4 different types).
- Streak bonuses (`domain/rewards/RewardRules.kt`): the 3 / 7 / 14 / 30-day milestones (20 / 50 / 100 / 250 sparks) pay **once per lifetime** (ledger key `milestone:<days>`), so breaking a streak on purpose and rebuilding it never pays them again. After 30 days, every further 30 days of the **same unbroken streak** (day 60, 90, …) pays +250 (key `streak:<days>:<start>`). DB v4 migrated older installs: each milestone's first per-streak payment was renamed to its lifetime key, so nothing is paid twice and no sparks are taken back.
- Work days to afford each pet, bought cheapest first (`EconomyTest`; every day a work day):

  | User | Breaks a day | Sparks a day | Pip (75) | Toast (+150) | Nimbus (+300) |
  |---|---|---|---|---|---|
  | No streak | 1–2 breaks, below the streak goal (pinned: 2 types) | 10 | day 8 | day 23 | day 53 |
  | Light | 3 types, just meets the streak goal | 15 + milestones | day 4 | day 11 | day 24 |
  | Normal | 4 types incl. Walk, streak kept | 25 + milestones | day 3 | day 7 | day 15 |
  | Heavy | all 7 types, streak kept | 40 + milestones | day 2 | day 6 | day 12 |

### 5.5 Premium (hard paywall with free trial)

- Model: **hard paywall + free trial.** The app's core loop requires an active trial or subscription.
- Paywall appears in the **first session, right after one value moment** (pet wakes up, user names it, does the first 1-minute stretch, pet celebrates). Most trial starts happen on install day, so this first paywall is the most important screen in the app.
- Products: yearly subscription with a **7-day free trial** (default, highlighted), monthly subscription, lifetime one-time. Prices (USD, set in Play Console, which converts them for other countries): yearly **$29.99** after the trial, monthly **$4.99**, lifetime **$59.99**. The app always shows the store's localised price, never a hard-coded one. The trial length lives in code in one place, `billing/Trial.kt` (`FREE_TRIAL_DAYS`); the Play Console offer's free phase must match it.
- In Play Console, make sure the yearly base plan has an offer with a **free phase**; in code, check the offer actually has a free trial phase before showing "Start free trial" text.
- **Trial-end reminder:** Android does not send one. Schedule our own local notification 2 days before trial ends (`Trial.END_REMINDER_DAYS_BEFORE`): 5 days after the trial starts, i.e. on day 6 counting install day as day 1 (the trial ends at the same time of day on day 8) ("Your free trial ends on …"). Honest and reduces refunds/1-star reviews.
- Paywall must show: price after trial, billing period, how to cancel (Play Store → Subscriptions), restore purchases, terms + privacy links.
- If the user closes the paywall: show a calm "locked" home with the sleeping pet and a single "Start free trial" button. No dark patterns, no fake timers.
- Entitlement checks go through RevenueCat (`CustomerInfo`), cached for offline use.
- **Closed test:** Play's test subscriptions lapse within hours (a 3-minute trial, then at most 6 renewals), so the `closedTest` build type shows the paywall and lets testers buy with test accounts, but closing it doesn't lock (`BuildConfig.PAYWALL_LOCKS = false`). `release` (production) always locks: `ReleaseBuildTest` (`src/testRelease`) fails CI otherwise. Never promote a closedTest release to production in Play Console; upload a release build.

---

## 6. Screens for v1 (build in this order)

1. **Splash** – logo only: Android 12+'s system splash, the launcher icon on cream (`res/values-v31/themes.xml`). Android 8–11 show the cream window background. No splash library.
2. **Onboarding**
   - First-launch gate (`ui/onboarding/AppGate.kt`): `onboarding_done` in the backed-up settings. It is decided once, on the first app start after it exists: a sit timer already saved means an install from before onboarding, which never sees it. A new install keeps seeing it until it is finished, even if the app is killed halfway.
   - Wake-up moment (`design/ui_v2/WakeUp.dc.html`, `Name.dc.html`): the night room (`NightRoomScene`) with the pet asleep as a round blob, no step dots → user taps (or "Reduce animations" goes straight on) → pet stretches for 1.6 s → the naming screen, in the day room: the pet says hello in a speech bubble, the white panel (step 1 of 4) asks what to call it, with a field and four suggestion names (Mochi, Peanut, Biscuit, Noodle) that fill it when tapped. Back from naming finds the pet asleep again. The name field allows at most `MAX_PET_NAME_LENGTH` (12, `domain/model/Pet.kt`) characters.
   - One question per screen (`design/ui_v2/Hours.dc.html`, `Interval.dc.html`, shared frame `OnboardingPanelScreen`): a green wall with a scene, Back top-left, "Skip" always visible top-right, and a white panel (steps 2 and 3 of 4) with the question and the answer. The scene shrinks on a short phone or with a big font (never below 45%) so the whole question stays on screen at normal font on 360×640; above 115% font Continue scrolls with the panel instead of staying pinned. **Hours:** the pet and a 24-hour bar that follows the answer (yellow where the pet is awake, sun and moons where there is room, overnight hours light both ends), day circles, then Starts and Ends rows that open a clock sheet. **Interval:** the pet after a break and after sitting, and a slider on the screen (20–90 min in steps of 5, "Recommended" at 45); Settings keeps its own sheet for the same choice. Work hours, as in Settings: with no work day ticked, Continue is off and "Pick at least one work day." shows above it at once. The same start and end is checked only on Continue (it's often a step on the way, 9–18 → 18–18 → 18–02): Continue keeps the step and shows the reason above it; the next edit clears it.
   - Permissions are **not** asked here (ask just-in-time, see Settings/Home cards in `ui/setup/`: tap → explanation sheet → the system's dialog). Home offers one card at a time and only once ("Not now" or a refusal hides it there); Settings always has all three, and sends a refused permission to the phone's settings.
   - First 1-minute stretch with the pet (a real Stand up break: it logs and earns its sparks) → celebrate → **paywall with free trial** (see 5.5). Until M10 the paywall is `ui/paywall/PaywallPlaceholderScreen.kt`, which offers nothing to buy.
   - Skip moves on one step and keeps that step's default, so it never bypasses the stretch and paywall. Interrupted onboarding restarts at the wake-up; answers already given stay.
3. **Home** (tab 1)
   - Top: the room (`ui/home/RoomScene.kt`, a Canvas drawn from `RoomPalette` tokens: walls, window, shelf, desk, plant, rug; groups anchored left/centre/right so it works from 360 to 430 dp, and below 390 dp the shelf plant slides left up to 20 dp so the speech bubble never covers it; `design/desk_scene.jpg` is history only). The pet (170 dp) stands on the rug (pet = sitting meter, see 5.2) with a `SpeechBubble` above it in its own voice, one line per mood (`PetLine`; none while asleep). Menu button top-left, sparks counter pill top-right.
   - Under it, on the floor colour: the timer card, "32 min" + "since your last break" + a 12 dp bar in the mood colour, the "Take a 2-min break" button when stiff, and on work days a "Streak goal" row (3 segments for `DAILY_GOAL`, "2 of 3", a flame chip with the streak days, hidden at 0; from `StreakRepository`, shared with Pet profile).
   - "Breaks for today" list: cards with icon tile, title, spark reward chip, chunky check button. A done card reads "Done · +5 sparks" with what the ledger actually paid today, just "Done" for a repeat.
   - Tapping a card opens a **focused card overlay** with three big actions: Skip · Done · Snooze.
4. **Break session** (`design/ui_v2/Stretch.dc.html`, `Celebrate.dc.html`) – full-screen guided stretch. Top: Close, a pill with the break's icon and "Stand up · 1 min", one bar per step (finished full, the current one filling). Then the countdown ring (a soft disc, the step's arc, the pet inside doing the move with its colour filling over the whole session, from its current energy to full) with the timer pill on its foot and the chip "Colour coming back!" ("Paused" while paused). Then the white panel: "STEP 2 OF 4", the step's title, its text, the "Move gently" note on the first step of a moving break, and Pause (a fixed 112 dp) + Next step (the last step: green Done). `SessionLayout` gives the panel what it needs and shrinks the ring first (never below 45%); then the panel's text scrolls above the pinned buttons. Above 130% font the ring, timer and text scroll together above the pinned buttons. **Celebration:** green wall with a ray burst from the pet's halo, the pet jumping on its rug, "You did it!", "Bun's colour is back…", a white card "+5 sparks" with "Save up to adopt Pip at 75" (or "You can adopt Pip!", or "Save up for new accessories" once every pet is adopted; only when the break paid; `BreakSessionViewModel.nextPetHint`), a card per streak bonus, the streak card ("1 of 3 breaks for your streak" + "today"), Continue. The scene shrinks on a short phone (never below 45%); above 115% font Continue scrolls with the content.
5. **Collection** (tab 2, `design/ui_v2/Collection.dc.html`) – header over soft mint circles, Pets | Accessories switch, then on Pets a "next pet" card and the grid of pets: owned ones in colour, the one with you framed in Ink with a warm glow, locked ones as grey silhouettes with a price; tap to preview.
   - **Next pet card:** the cheapest pet not adopted yet ("Saving up for Pip" under a quarter of its price, then "Pip is almost yours", "You can adopt Pip!" once affordable), "30 / 75" and a bar of the sparks saved toward it, and "About 3 more days of breaks" ("About a day of breaks to go" for one day or less). Days are at the user's own pace (UI v2 decision D4, `domain/rewards/SparksPace.kt`): sparks missing ÷ the average break rewards a day over the last 7 finished days that had a break (today counts once it is over; streak bonuses left out, so a one-off milestone can't promise the pet too soon), 15 a day until there is a finished break day, so install day's first stretch (5 sparks) still shows Pip about 5 days away, not 14. Hidden once every pet is adopted; tapping it opens that pet's sheet.
   - **Accessories (v1: 4 items)**, the ongoing spark sink once every pet is owned: bought once with sparks (50, 75, 100, 125) and worn by any pet, one per slot (head, face, neck). Collection has a Pets | Accessories switch; worn items show on Home, in break sessions, on the profile and in Collection, **not on the widget** (its mood images stay one static PNG per pet and mood).
   - **Post-launch update:** 4 more accessories (150, 200, 250, 300), so a Normal user keeps something to buy for about 8 weeks after owning every pet.
6. **Pet profile** (tab 3, tab label = pet's name) – name, streak card, total breaks, weekly bar chart.
7. **Menu / Settings** (`design/ui_v2/Settings.dc.html`) – on lilac: a "Bun & you" card on Night (a still `PetView` of the pet with you, "5-day streak · 42 breaks so far" from `StreakRepository`, no streak part at 0, "No breaks yet" before the first; tap opens the Pet tab), then grouped cards (`GroupedCard` + `SettingsRow`):
   - **Your setup:** Notifications, Movement detection, Widget, each with why under it ("So Bun can remind you", "Walking resets the timer", "Bun on your home screen") and on the right a small Ink "Turn on" ("Add" for the widget) while off, or an "On" / "Added" chip. Each off row opens the permission flow; refused in the phone's settings, the line under it says so and the flow sends there.
   - **Work hours:** day circles, then Starts and Ends. Work hours are edited freely (18–18 on the way from 9–18 to 18–02 is fine); "Save work hours" shows under the card while they differ from what's stored. With no work day ticked, Save is off and the reason shows by it at once. The same start and end is checked only on Save, which then refuses and shows the reason by the button.
   - **Breaks:** "Remind me every" (opens the interval sheet), then the break-type switches. The interval and the switches save straight away; the last break type can't be switched off.
   - **About:** privacy policy (no URL yet, `PRIVACY_POLICY_URL` in `ui/settings/SettingsScreen.kt`), then "Made with love in India" and the version.
   - From 160% font a row's button, chip or value moves under its text, so titles never break mid-word.
   - The Premium badge on the "Bun & you" card ("Try 7 days free" from `Trial.FREE_TRIAL_DAYS` until entitled) arrives with M10c.
8. **Paywall** – pet illustration, 3 benefits (approved copy, in this order: "Your pet gets tired as you sit and perks up when you move" · "Gentle nudges and 1–2 minute guided stretches" · "Earn sparks for breaks to adopt new pets and accessories"), plan selector (yearly highlighted), "Start free trial" CTA, price after trial in plain text, how to cancel, restore purchases, terms/privacy links.

**Not in v1:** friends, seasons/events, outfits beyond the 4 v1 accessories, furniture, adventure map, AI coach, iOS.

**Moved to v1.1 (decided 2026-11-04, to ship sooner):** richer break routines (4–6 steps for every type, cup art for Drink water, a body pose for Eyes rest), haptics and sounds on Done and the celebration, 2.0× font screenshots for Break session, Collection, Onboarding and Pet profile (v1.0 fixes only what the accessibility pass finds), a custom splash or the core-splashscreen library. Also later: the 4 post-launch accessories, localisation, the large widget, a store video, an in-app review prompt.

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
- **Accessories (M8b):** `AccessoryId` (price in the enum: flower clip 50, round glasses 75, knit scarf 100, beanie 125) in three slots (HEAD, FACE, NECK; one per slot). Art: `design/accessories/acc_*.svg`, ported to `ui/pet/AccessorySpecs`; `PetSpec.accessoryAnchors` moves each slot onto a pet (only HEAD differs). Drawn inside the body transforms, never drained; order NECK (after the body), FACE (before the mouth, so Pip's beak shows), HEAD. While a NECK item is worn and energy < 1, the fill edge line is drawn again over it, clipped to the silhouette, so the meter never looks stuck behind the scarf (`accessories_fill_edge_scarf`; `accessories_fill_edge_no_neck` pins that nothing changes without one). Bought once (ledger key `accessory:<key>`, `PetRepository.buy`), worn by any pet (each pet's outfit in settings, `worn_<pet>`); `ActivePet.wearing` carries the active pet's outfit to Home, break sessions, the profile and the Collection (Pets | Accessories switch). **Not on the widget** (it only receives a `PetId`). `EconomyTest.workDaysToEachAccessoryAfterThePets` pins when each becomes affordable after the pets.
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

Reference mockups: `design/ui_v2/*.dc.html` first (the UI v2 refresh: Home, Collection, Settings, Locked Home and the onboarding screens through the paywall; README there), then `design/mockups_source/*.dc.html` for what ui_v2 doesn't cover (the focused card, `Pet`, `PetMoods`, `Tokens`). Both are HTML sources of 390×844 phone artboards: read them for exact layout, spacing, sizes and copy; they need a runtime to render, so read them as code. Also any screenshots Kartik adds in `design/mockups/`. Match them. Full file map: `design/README.md`.

**Colours** (`ui/theme/Color.kt`)

| Token | Hex | Use |
|---|---|---|
| `Cream` | `#FFF4E8` | window, splash, first-launch loading box; onboarding until its ui_v2 restyle |
| `Card` | `#FFFFFF` | cards, tab bar |
| `CardEdge` | `#EFE3D4` | chunky edge on Cream |
| `Ink` | `#2B2A3A` | text, primary button |
| `InkEdge` | `#14131F` | primary button edge |
| `InkMuted` | `#56556A` | secondary text: 7.2:1 on Card, at least 5.8:1 on every surface below |
| `Track` | `#F1EBE3` | progress track |
| `DoneGreen` / edge | `#3BAA6E` / `#2C8655` | done buttons, checks |
| `Sun` / edge | `#FFC857` / `#D69E2A` | highlight button, sparks icon |
| `Coral` | `#E5603A` | over-interval state |
| `Night` | `#2F2E45` | onboarding sleep screen |
| `BreakSky` | `#EAF3FD` | break session background |
| `PetYellow` / edge | `#F5DC8B` / `#EBCB6A` | Bun body |
| `PetGrey` | `#E9E5DE` | drained body |
| `PetPink` | `#F6A98A` | inner ears |
| `PetBlush` | `#F7A58B` | cheeks |

**Surfaces** (`ui/theme/Surfaces.kt`, from `design/ui_v2/README.md`). Each screen paints its own background and provides it as `LocalSurface` (`OnSurface(Surfaces.X) { … }`); `PillCard`, `ChunkySurface`, `SparksPill`, `TopIconButton`, `SkipPill`, `SheetPanel`, `GroupedCard` and `SpeechBubble` take its edge by default, so screens don't pass edge colours around.

| Surface | Background / edge | Where |
|---|---|---|
| `Home` | `#F4E8D6` / `#E3D3BC` | Home's floor (timer card, break list); seams `#EADCC6` |
| `HomeWall` | `#D6EBDA` / `#BCDAC4` | Home's room scene; lower wall `#C8E2CF`; rug `#CFE3FB` / `#B2D0F2` |
| `Collection` | `#DDF1E6` / `#BFE0CE` | Collection |
| `Settings` | `#ECE9F7` / `#D6D1EC` | Settings |
| `Profile` | `#E2EEFB` / `#C7DAF1` | Pet profile (no ui_v2 mockup: #30 direction B's sky); the paywall's top |
| `Session` | `#EAF3FD` / `#C7DAF1` | Break session (ring arc `#5E9FE6`, track `#C7DAF1`, disc `#DCEAFB`); its celebration is on `HomeWall` |
| — | `#2F3352` wall, `#2A2C45` floor | Onboarding's night room (`NightRoomWall`, `NightRoomFloor`) |

**Tab bar** (design/ui_v2 nav): full-colour icons, never tinted (coral house, four-square grid, lilac gear `#B9A3F0`, `#9C82E6` when selected); the Pet tab shows a small still `PetView` of the active pet. The selected tab gets a 60×36 pill and the bar a 2 dp top border in that tab's colours: Home `#FFE3D8` / `#EFE6DA`, Collection `#D3F0E1` / `#CFE8DA`, Pet `#FFF0CC` / `#D5E5F7`, Settings `#E6DDFB` / `#DDD8EF`.

**Shared pieces** (`ui/components`): `TopIconButton` (Back / Close / Menu), `SkipPill`, `SheetPanel`, `StepDots` (Ink, current step 28×8, steps to come `#E3DCD2`), `GroupedCard` + `SettingsRow` (divider `#F1EEF8`), `StatusChip` ("On" `#D3F0E1` / `#1F6B48`), `SpeechBubble`.

Break-type colours (chips): Walk `#7DD3B0`, Neck `#7FB8F5`, Stand `#FFC857`, Eyes `#B9A3F0`, Wrist `#F68A6B`, Water `#F4A3C0`. Their light tints are the icon tiles: `#CDEFE0`, `#CFE3FB`, `#FFE3A3`, `#E6DDFB`, `#FDDCD2`, `#FCE1EC`. Break and Settings icons are the full-colour vectors from `design/ui_v2/icons` (`ic_break_*`, `ic_settings_*`), drawn untinted at about 3/4 of their tile (38 in 50).

The weekly chart (Pet profile, "Last 7 days") uses category colours, not one per break type: Movement `#7DD3B0` (Stand up, Walk), Stretch `#7FB8F5` (Neck, Shoulders, Wrist), Eyes `#B9A3F0`, Water `#F4A3C0`; stacked Stretch, Movement, Eyes, Water from the bottom so Stretch blue and Eyes lilac never touch.

Home's streak flame chip `#FFE9DF` / text `#9A3D20`, spark reward chip `#FFF4D6`. Timer bar: happy `#4DB88A`, tired `#F2A93B`, stiff `#E5603A`.

**Type** (Google Fonts, bundled in `res/font`, free OFL licence)
- Fredoka SemiBold 600: display 32 sp, title 22 sp, button 18 sp
- Nunito ExtraBold 800: card title 17 sp; Bold 700: labels 13–15 sp; SemiBold 600: body 15 sp

**Shape & spacing**
- Radius: cards 24 dp, icon tiles 16 dp, small buttons 16–18 dp, focused card 28 dp, bottom sheets 28 dp top, pills fully rounded
- Screen padding 16 dp, card padding 12–16 dp, list gap 10 dp

**Chunky buttons / cards:** solid offset "edge" below (cards 3–4 dp `CardEdge`, buttons 4–5 dp in the darker shade). On press: translateY by the edge height and remove the edge (80 ms), release springs back.

**Motion:** no linear easing; pop-ins with slight overshoot; Home list items stagger in (40 ms apart); energy changes tween ≥ 600 ms. With the phone's "Remove animations" on (animator duration scale 0, `LocalReducedMotion` in `ui/motion/ReducedMotion.kt`), the app's own motion stops too: the pet holds its still pose (no frame loop), Home cards show at once, the wake-up tap skips the stretch and each break step holds its first pose. Compose's springs and tweens already follow the scale; timings that aren't motion (the celebration's 3 s) stay. New motion that runs on its own loop or delay reads `animationsEnabled()`.

**Pet names in text:** a name never changes a card's height. A sentence with the name in it ("Bun is getting tired", "Choose Bun") uses `PetNameSentence`, which shortens only the name with "…" when it would add a line ("Wigglebo… is getting tired"); the words around it still wrap at large font sizes, and TalkBack reads the whole name. A name shown alone (titles, collection cards, tab label) uses `maxLines = 1` with `TextOverflow.Ellipsis`. Screenshot tests use `TestPetNames.LONG` / `WIDEST` (12 letters).

---

## 12. Play Store readiness checklist

- [ ] Target SDK 36, Billing Library ≥ 8
- [ ] Release signing: Play App Signing, with the upload key kept outside git. `app/build.gradle.kts` reads `WIGGLEBUN_UPLOAD_STORE_FILE`, `_STORE_PASSWORD`, `_KEY_ALIAS` and `_KEY_PASSWORD` from `~/.gradle/gradle.properties`; without them release builds are unsigned (as in CI). `./gradlew bundleRelease` makes the production AAB, `bundleClosedTest` the closed-test one
- [ ] versionCode goes up by 1 for every AAB uploaded to Play, on any track; versionName is `1.0.0` (closed-test builds show `1.0.0-closedtest`)
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
   **CI is the source of truth** (`.github/workflows/android.yml`): every PR and every push to `main` runs `assembleDebug lintDebug verifyRoborazziDebug`, then `assembleRelease compileClosedTestKotlin` and `ReleaseBuildTest`, on a Linux runner; on failure the screenshot diffs are uploaded as the `roborazzi-diffs` artifact. Reference images are recorded and verified on Linux only. Local Windows runs can show tiny font-rendering differences: don't re-record from Windows; record on Linux (CI image or a Linux machine) instead.
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
| M8b | Accessories: 4 v1 items, buy with sparks, wear on any pet (Home, break session, profile, Collection; not the widget) | Can buy and wear each item on all 4 pets; screenshots for every pet × item |
| M9 | Onboarding (wake-up, naming, settings) + just-in-time permission cards | Fresh install flows end to end |
| M10 | RevenueCat + paywall + restore + trial-end reminder | Test purchases work with license testers; entitlement survives offline restart |
| M11 | Polish, accessibility (TalkBack labels, contrast), privacy policy, store assets | Ready for closed testing |
| M12 | Release candidate: the full phone checklist on a real phone and on an Android 8 emulator, license-tester purchase / restore / cancel / lapse, OEM battery check | Closed-test AAB uploaded |

v1.0 build order (approved 2026-11-04): release foundations + cleanup → privacy policy + terms → UI v2 (`design/ui_v2`, one PR each: respect "Remove animations" → foundations (palette, surfaces, icons, tab bar, shared pieces) → Home → Collection → Settings → onboarding wake-up + name → onboarding hours + interval → break session + celebration) → M10a RevenueCat layer (only once the Play Console app and products exist) → M10b paywall (`design/ui_v2/Paywall.dc.html`) → M10c gating (Locked Home) → M10d trial-end reminder → M11 accessibility + large fonts → M11 store listing → M12.

**M11 accessibility list (found while reviewing, fix in M11):**
- Settings, Work hours: at 200% font on a small phone (360 dp) the day circles (M T W T F S S) touch each other and their 48 dp tap targets may overlap. Needs a layout that keeps the circles apart (smaller circles, or wrapping to two rows), with a 200% / 360 dp screenshot. The same `DayChips` is on onboarding's Hours screen.
- Naming screen at 130% font (360 dp): the speech bubble's "nap!" wraps alone onto its own line. Rebalance the line break (or the bubble width).
- Wake-up at 200% font on 360×640: the last body line sits partly behind the button; it can be reached by scrolling, but the squeeze could leave it whole.
- Onboarding Hours at 200% font (360 dp): the 24-hour bar's labels ("12 AM", "9 AM") crowd together.
- Onboarding Interval at 200% font (360 dp): the "After 45 min sitting" chip is clipped by the panel's top edge.
- Break session at 200% font on 360×640: the ring, timer and text scroll together, so the instruction is below the first screen and has to be scrolled to while the timer runs. A smaller ring or a one-line timer row would show more of it.

---

## 15. Open decisions (ask Kartik)

- ~~App name~~ Decided: Wigglebun (pending domain + trademark check by Kartik)
- ~~Final colour palette and font~~ Decided: see section 11
- ~~Prices~~ Decided: yearly $29.99 with a 7-day free trial, monthly $4.99, lifetime $59.99 (see §5.5)
- ~~Which 3 pets ship in v1~~ Decided: Pip, Toast, Nimbus
- ~~Streak milestone amounts~~ Decided, final for v1.0: 3 / 7 / 14 / 30 days → 20 / 50 / 100 / 250 sparks, once per lifetime, then +250 per further 30 days of one streak (`domain/rewards/RewardRules.kt`)
- ~~Paywall benefit lines~~ Decided: the 3 in §6.8 (the mockup's original lines overpromised: no calendar, no custom breaks, pets come from sparks)
- ~~Pet prices~~ Decided: Pip 75 / Toast 150 / Nimbus 300 sparks (`domain/rewards/PetPrices.kt`)
