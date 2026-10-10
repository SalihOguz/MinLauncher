# MinLauncher: ideation

Olauncher's home screen, kept as it is, with room for 15 apps. A drawer that puts what you need right above the keyboard, and a place to see and clean up everything else. No blockers, no timers.

Interactive mockups: https://claude.ai/artifact/LakBZN4dLuTsr8MkrcQAzH

Target device: Samsung Galaxy Note 20 Ultra, Android 13 (One UI 5), about 411 × 882 dp.

## Decided

### Home
- Auto-fit up to 15 apps; drag to reorder (hold, then drag; hold and release opens the app's sheet).
- Keeps Olauncher's look (see the table below).
- No compact clock: the status bar already shows the time.
- Big clock is a setting: auto / always / never. Proposed default: auto, which shows it with up to 11 apps and hides it from 12.
- Date stays (Samsung's status bar doesn't show it), with a weather line under it. Tapping the weather line opens the weather screen.
- Calendar line: off until the user picks which calendars to use. It shows today's next event only; on a free day, or after the last event, nothing.
- Wallpaper comes from the system; home is transparent over it. No wallpaper features. Optional dim % for readability.
- Gestures: up drawer, down notifications, left Camera, right Phone, double-tap lock, hold empty space opens Manage. Tapping the clock or date opens Clock or Calendar.

### Drawer
- One screen; the keyboard is just up or down. It opens with the keyboard up (setting).
- The list grows upward from the search bar. From the bar upward, while typing: the web row ("Search Brave for …"), then matching apps (best match right above the web row). When not typing: Pinned, Recent, New, Folders (+ New folder), All apps A–Z.
- "Only match opens itself" is a setting, off by default, so the web option is always reachable.
- Proposed: the Enter key is a search key that always searches the web in Brave (setting: or open the top app). No live web suggestions while typing.
- Search matches the start of the name, the start of any word, initials, search words, folder names and small typos, and ignores symbols.
- Pinned: no icons, drag to reorder. Proposed layout: two text columns (8 apps in 4 rows); the alternative is a single list.
- Everything else in the drawer always has monochrome icons (`AdaptiveIconDrawable.getMonochrome()`, greyscale fallback), so names never shift.
- Recent: 5, as an experiment. Skips apps on home, pinned, on a swipe, games, and any app with "Show in Recent" off (a per-app switch, off by default for games).
- New: stays in New until opened, however long that takes. ✦ marker for 3 days after install.
- Rename apps and add search words.
- Folders: user-created (+ New folder in the drawer and in the sheet's Move to folder); Unsorted is the inbox; for unfiled apps, a folder suggestion from `ApplicationInfo.category`. Rename, reorder and delete in Settings.
- A–Z rail when the keyboard is down: letters bend away from the finger and the list filters to that letter. Tapping empty space clears the filter.
- Hidden apps leave the drawer and search; they stay in Library under Hidden.

### Long-press sheet
Install date, last opened, size, folder, search words; Open · App info · Uninstall; Pin/Unpin · Add to/Remove from home · Move to folder (+ New folder) · Rename & words · Hide; Remove from New / Remove from Recent when they apply; Show in Recent switch; folder suggestion for unfiled apps.

### Manage (Library · Stats · Settings)
- Opened by holding empty space on home, or from the drawer. Nothing in it pops up on its own.
- **Library:** full colour icons; sort by least used, newest, largest, A–Z; filters Unused 60+ days and Hidden; batch uninstall (Android confirms each one; system apps go to App info › Disable).
- **Stats:** how each drawer visit ended (typed, pinned, recent, web, folder, rail, keyboard closed first), screen time and unlocks today, most opened apps.
- **Suggestions** live only in Stats, each with one action and "Not now" (hidden for 30 days). Examples: pin an app you keep typing for; move a rarely used home app to Pinned; unpin a pinned app untouched for 30 days; turn off Recent if it's barely used; review apps unused for 60+ days.

### Glanceable bits
W1 weather line · W2 minimal weather screen (Open-Meteo, a typed city avoids the location permission) · W3 Brave search from the drawer · W4 calendar line · W5 screen time and unlocks in Stats. No widget page. Gentle-interrupt reminders stay parked.

### Build
Fresh Kotlin + Jetpack Compose, Room, DataStore. No ads, no analytics; the only network call is weather. Proposed: GitHub Actions builds an APK on each push.

## Olauncher's home look (xxhdpi resources)

| Element | Value |
|---|---|
| App names | 30 sp, `sans-serif-light`, white, shadow dy 2 dp, radius 4 dp |
| Vertical padding per name | 10 dp |
| Side margin | 24 dp, left aligned |
| Clock | 66 sp `sans-serif-light`, 56 dp from top |
| Date | 18 sp, `EEE, dd MMM` |
| App list | paddingTop 112 dp, paddingBottom 48 dp, `center_vertical` |

Auto-fit on the Note 20 Ultra: rows run about 60 dp → 52 dp with the big clock (up to 11 apps), and about 46 dp at 15 with the clock hidden. Text stays 30 sp. With the clock set to always, 15 apps need about 25 sp text.

## Settings and defaults

| Group | Setting | Default |
|---|---|---|
| Home | Apps on home (1–15) | 8 |
| Home | Big clock: auto / always / never | auto |
| Home | Date | on |
| Home | Weather line (city or approximate location) | on |
| Home | Calendar line + calendars | off |
| Home | Alignment | left |
| Home | Text size | 30 sp |
| Home | Wallpaper dim % | 0 |
| Home | Status bar | on |
| Gestures | Swipe left / right app | Camera / Phone |
| Gestures | Double-tap to lock | on |
| Drawer | Opens with the keyboard | on |
| Drawer | Only match opens itself | off |
| Drawer | Enter key: web search / top app | web |
| Drawer | Web search browser | Brave |
| Drawer | Pinned layout: two columns / list | two |
| Drawer | Recent: off / 5 / 10 | 5 |
| Drawer | A–Z rail | on |
| Drawer | Drawer dim % | — |
| Apps | Folders, hidden apps, renamed apps and search words, apps that skip Recent | games skip Recent |
| Look | Theme / font | system / light |
| Data | Usage access (Stats, Library sizes) | off |
| Data | Export / import settings | — |

## Build order

| Step | Ships |
|---|---|
| v0.1 | Home (Olauncher look, auto-fit to 15, big clock auto, drag to reorder, gestures, double-tap lock); drawer (keyboard up, apps + web row, Enter searches Brave, Pinned with drag); long-press sheet; basic settings |
| v0.2 | New + ✦, Recent rules + Show in Recent switch, search words, initials and typos, launch log + drawer-visit counter |
| v0.3 | Browse: mono icons, folders + New folder + Unsorted + category suggestions, A–Z rail, hidden apps |
| v0.4 | Manage: Library + Stats with suggestions, usage access |
| v0.5 | Weather line and screen, calendar line with calendar picker |

## App ID options

The app ID is permanent and rarely seen (Play URL, Android app settings). The display name and developer name can change; the ID can't without reinstalling.

- **A1 `com.craftusta.minlauncher`:** if Craftusta publishes it, as a portfolio piece under the agency's Play account. Ties the app to the agency for good.
- **A2 `io.github.salihoguz.minlauncher`:** personal, free, matches this repo; the usual choice for open-source apps and F-Droid. Recommended if it's for you.
- **A3 a product domain later:** only if MinLauncher becomes its own product.

Google is rolling out developer verification, which registers app IDs to a verified developer, so register the ID under the identity that should own it. Installing your own builds on your own phone is unaffected.

## Left to decide before v0.1

1. Who is it for, and which app ID? Just you (A2, Android 13+ only, simplest), or published on Play (A1 or A2, Android 8+, plus Play policy work for double-tap lock, which uses an accessibility service)?
2. Big clock default: auto or never?
3. Enter key: web search or the top app?
4. Pinned layout: two text columns or a single list?

## Round 1 findings (Olauncher source)

- Home is 8 fixed views (`homeApp1`…`homeApp8`), hence the 8-app limit.
- The ✦ new marker lasts 1 hour after install (`now - firstInstallTime < ONE_HOUR`).
- The drawer is sorted alphabetically only.
- Uninstall, App info, Hide and Rename sit behind a long-press with no hint.
