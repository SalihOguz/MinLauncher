# MinLauncher spec

Olauncher's home screen, kept as it is, with room for up to 28 apps. A drawer that puts what you need right above the keyboard, and a place to see and clean up everything else. No blockers, no timers.

- Interactive mockups: https://claude.ai/artifact/LakBZN4dLuTsr8MkrcQAzH
- App ID: `io.github.salihoguz.minlauncher`
- First user: Samsung Galaxy Note 20 Ultra, Android 13 (One UI 5), about 411 × 882 dp. Shared later, maybe on F-Droid.
- Stack: Kotlin, Jetpack Compose, DataStore (Room when the launch log arrives in v0.2). minSdk 28, so the APK also installs on older phones if it's shared later.

## Home

- Olauncher's look:

  | Element | Value |
  |---|---|
  | App names | 30 sp, `sans-serif-light`, white, shadow dy 2 dp, radius 4 dp |
  | Row padding | 10 dp above and below (about 60 dp per row) |
  | Side margin | 24 dp, left aligned |
  | Clock | 66 sp light, about 32 dp below the status bar |
  | Date | 18 sp, `EEE, dd MMM` |
  | App list | 48 dp bottom padding, centred vertically in the free space |

- **Up to 14 apps:** one column. **15–28 apps:** two balanced columns, filled top to bottom, left column first. Adding or removing a home app re-balances the columns.
- **Auto-fit:** spacing shrinks first, text only if needed. Rows max out at about 60 dp.
- **Big clock** (setting: auto, always, never; default auto): auto shows it whenever the list is 11 rows or fewer, which is 1–11 apps and 15–22 apps. Otherwise the date (and weather line) moves to the top.
- No compact clock; the status bar already shows the time.
- Drag to reorder: hold an app, then drag. Hold and release opens the app's sheet.
- Home is transparent over the system wallpaper. Optional dim % for readability.
- Weather line under the date (v0.5). Calendar line (v0.5): off until calendars are picked, shows today's next event, nothing on a free day.

### Gestures

| Gesture | Action |
|---|---|
| Swipe up | Drawer |
| Swipe down | Notifications |
| Swipe left | Camera |
| Swipe right | Phone |
| Double-tap empty space | Lock screen (accessibility service) |
| Hold empty space | Settings (Manage in v0.4) |
| Tap clock / date | Clock / Calendar |
| Tap weather line | Weather screen (v0.5) |

## Drawer

- One screen; the keyboard is up or down. **Opens with the keyboard up** (setting, default on).
- The list never resizes with the keyboard. The keyboard and search bar float over it, and the keyboard's room is kept for the whole visit, so closing the keyboard never moves an app under your finger. Home fades out while the drawer is open.
- Names are 26 sp with the home screen's soft shadow, rows about 50 dp. Background dim is a setting (default 30%).
- The list grows upward from the search bar, which sits above the keyboard.
- **While typing:** from the bar upward, "Search Brave for …", then matching apps, best match right above the web row.
- **Enter is a search key and always searches the web** (setting: open the top app instead). Web search opens Brave with its own search engine, or the default browser with Brave Search if Brave isn't installed.
- **Only match opens itself:** setting, default off.
- No live web suggestions while typing.
- **When not typing,** from the bar upward: Pinned, Recent (v0.2), New (v0.2), Folders (v0.3), All apps A–Z.
- **Pinned:** two text columns, no icons, hold and drag to reorder. 8 pinned apps take 4 rows.
- **Icons:** every other drawer row gets a monochrome icon (v0.3, `AdaptiveIconDrawable.getMonochrome()` with a greyscale fallback). Names never shift.
- **Search matching,** best first: start of the name, start of any word, the app's search words (`bank` → Halkbank), initials (`kn` → Keep Notes), inside the name (from 3 letters), small typos (from 4 letters: `alrmy` → Alarmy). Ignores symbols and accents (`edevlet` → e-Devlet).
- Scrolling the list hides the keyboard.
- **Recent (v0.2):** 5 apps (setting: off, 5, 10), most recently opened from MinLauncher first. Skips apps on home, pinned apps, games, and any app with "Show in Recent" off (per-app switch, off by default for games). "Remove from Recent" hides an app until it's opened again.
- **New (v0.2):** apps installed after MinLauncher started keeping track, never opened from it; they stay until opened once (or removed, pinned, or put on home). ✦ marker for 3 days after install, wherever the name appears.
- **Folders (v0.3):** user-created (+ New folder in the drawer and in Move to folder); Unsorted is the inbox; folder suggestion from `ApplicationInfo.category` for unfiled apps.
- **A–Z rail (v0.3):** when the keyboard is down. Letters bend away from the finger and the list filters to that letter. Tapping empty space clears the filter.
- **Hidden apps:** gone from the drawer and search. Listed in Settings (v0.1), later in Library.

## Long-press sheet

- **v0.1:** install date; Open · App info · Uninstall; Pin/Unpin · Add to/Remove from home · Rename · Hide.
- **v0.2:** last opened; Rename & search words; Remove from New / Remove from Recent; Show in Recent switch.
- **Later:** size, folder; Move to folder; folder suggestion.

## Manage (v0.4): Library · Stats · Settings

- Opened by holding empty space on home, or from the drawer. Nothing in it pops up on its own.
- **Library:** full colour icons; sort by least used, newest, largest, A–Z; filters Unused 60+ days and Hidden; batch uninstall (Android confirms each one; system apps go to App info › Disable).
- **Stats:** how each drawer visit ended (typed, pinned, recent, web, folder, rail, keyboard closed first), screen time and unlocks today, most opened apps.
- **Suggestions** live only in Stats, each with one action and "Not now" (hidden for 30 days). Examples: pin an app you keep typing for; move a rarely used home app to Pinned; unpin a pinned app untouched for 30 days; turn off Recent if barely used; review apps unused for 60+ days.

## Settings and defaults

| Group | Setting | Default | Ships |
|---|---|---|---|
| Home | Big clock: auto / always / never | auto | v0.1 |
| Home | Weather line; city or approximate location | on | v0.5 |
| Home | Calendar line + calendars | off | v0.5 |
| Home | Alignment, text size, wallpaper dim, status bar | left, 30 sp, 0, on | later |
| Gestures | Double-tap to lock | on | v0.1 |
| Gestures | Swipe left / right app | Camera / Phone | later |
| Drawer | Opens with the keyboard | on | v0.1 |
| Drawer | Only match opens itself | off | v0.1 |
| Drawer | Enter key: web search / top app | web search | v0.1 |
| Drawer | Recent: off / 5 / 10 | 5 | v0.2 |
| Drawer | Background dim | 30% | v0.1 |
| Drawer | A–Z rail | on | v0.3 |
| Apps | Hidden apps | — | v0.1 |
| Apps | Folders, renamed apps and search words, apps that skip Recent | games skip Recent | v0.2–v0.3 |
| Look | Theme, font light / regular | system, light | later |
| Data | Usage access, export / import settings | off | v0.4 |

## Build order

| Step | Ships |
|---|---|
| v0.1 | Home (Olauncher look, auto-fit, two columns 15–28, big clock rule, drag to reorder, gestures, double-tap lock); drawer (keyboard up, apps + web row, Enter searches the web, Pinned grid with drag, All apps A–Z); long-press sheet; rename; hide; basic settings |
| v0.2 | New + ✦, Recent rules + Show in Recent switch, search words, typo matching, launch log + drawer-visit counter |
| v0.3 | Browse: mono icons, folders + New folder + Unsorted + category suggestions, A–Z rail |
| v0.4 | Manage: Library + Stats with suggestions, usage access |
| v0.5 | Weather line and screen, calendar line with calendar picker |

## Getting builds onto the phone

GitHub Actions builds an optimized (R8) APK on every push. It attaches the APK to a pre-release called `dev-build` and uploads it as a workflow artifact. Every build is signed with the same committed key (not a store key), so a new APK installs over the old one.

## Parked

- ProLauncher-style "gentle interrupt" reminders.
- A page of real Android widgets.
- Wallpaper features (daily wallpaper).
