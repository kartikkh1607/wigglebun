# Play Store listing (v1.0)

Everything to paste into Play Console. Graphics are built by `python store/make_assets.py`.
Limits (Play Console, Nov 2026): app name 30 characters, short description 80, full description 4,000.

## Main store listing

**App name** (25): `Wigglebun: Desk Break Pet`

**Short description** (71): `A cute desk pet that gets tired when you sit and perks up when you move`

**Full description**

```
Sitting for hours? Meet Bun, the little desk pet that feels it too.

The longer you sit, the more tired and grey Bun gets. Stand up, stretch or take a short walk, and Bun's colour comes back with a happy little jump. No guilt, no nagging: just a small friend who is glad when you move.

HOW IT WORKS
• Your pet is your sitting meter. One look tells you how long it has been.
• Gentle reminders during your work hours only. Outside them, your pet sleeps.
• Guided 1–2 minute breaks: stand up, neck stretch, shoulder rolls, wrist stretch, eye rest, a short walk, a glass of water.
• Walking resets the timer by itself if you allow movement detection. Nothing to log.
• Busy? Snooze for 10 minutes, or skip.

SMALL REWARDS THAT ADD UP
• Earn sparks for every break.
• Adopt new pets: Pip, Toast and Nimbus.
• Dress them up with accessories.
• Keep a streak with three breaks a day and watch your week fill up.

MADE FOR DESK DAYS
• Home screen widget, so your pet is always in sight.
• Set your own work days, hours and how often to be reminded (every 20 to 90 minutes).
• Works with large fonts, TalkBack and "Remove animations".

PRIVATE BY DESIGN
• No account, no sign-up.
• Your breaks, streak and movement data stay on your phone.
• No ads.

SUBSCRIPTION
Wigglebun needs a subscription or a one-time purchase. New subscribers on the yearly plan get a 7-day free trial; we remind you two days before it ends. Cancel anytime in Google Play › Subscriptions.

Wigglebun is a habit helper, not a medical app. It does not diagnose, treat or prevent any condition. Stretch gently and stop if anything hurts.
```

**Graphics**
- App icon: `design/play_store_icon_512.png` (512×512, 32-bit PNG)
- Feature graphic: `store/feature_graphic.png` (1024×500, no alpha)
- Phone screenshots, in this order: `store/screenshots/01_tired.png` … `06_profile.png` (1080×1920)

**Category:** Health & Fitness. **Tags:** pick from Play's list (e.g. Activity tracking / Wellness) in the console.

**Contact details:** your support email (Play shows it publicly), website `https://wigglebun-legal.vercel.app`.

**Privacy policy URL:** `https://wigglebun-legal.vercel.app/privacy`

## App content (Policy → App content)

These are proposed answers. Read each console question before ticking: the wording there decides.

**Ads:** No, the app contains no ads.

**App access:** "All or some functionality is restricted". Instructions for the reviewer:
```
The app needs a subscription after a short onboarding (wake the pet, name it, one 1-minute stretch).
On the paywall, choose the yearly plan and "Start 7-day free trial" with the review account's
Play test payment method. No login or account is needed. Without a purchase, closing the paywall
shows a locked Home; Collection, the pet's page and Settings stay open.
```

**Content rating questionnaire:** category Utility/Productivity or Health; no violence, no user-generated content, no sharing of location, digital purchases: yes.

**Target audience:** 18 and over (keeps the app out of the Families policy). Not designed for children.

**Data safety**
- Does the app collect or share any of the required user data types? **Yes** (because of purchases).
- Is all collected data encrypted in transit? **Yes.**
- Can users request deletion? There is no account; app data is removed by uninstalling or clearing storage. Answer the console's question accordingly.
- Data types:
  - **Financial info → Purchase history:** collected, not shared. Purpose: app functionality (unlocking premium). Processed by RevenueCat as a service provider. Required.
  - **Device or other IDs:** collected, not shared. RevenueCat keeps an anonymous app user ID to restore purchases. Purpose: app functionality. Required.
- **Not collected** (stays on the device, never sent anywhere): physical activity from movement detection and the step counter, break history, streaks, work hours, the pet's name. Google does not count data that is only processed on the device as collected.
- Check RevenueCat's own Data safety guide against these answers before submitting.

**Health apps declaration:** Health and fitness → **Activity and fitness**. Not a medical device, no health records, no Health Connect.

**Permissions to justify if asked**
- `ACTIVITY_RECOGNITION`: so walking resets the sit timer automatically; asked only from a card in the app, processed on the device.
- `POST_NOTIFICATIONS`: break reminders and the trial-end reminder.
- No exact alarms, no location, no foreground service.

**Government apps / Financial features / News:** No.

## Before the first production upload
- Screenshots must show the app as shipped: re-run `python store/make_assets.py` after UI changes.
- The description says "we remind you two days before it ends": true only with notifications allowed.
- Closed test: 12 testers for 14 days in a row if the developer account is a personal one created after 13 Nov 2023.
