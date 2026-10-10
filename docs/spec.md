# MinLauncher spec

Olauncher's home screen, kept as it is, with room for up to 28 apps. A drawer that puts what you need right above the keyboard, and a place to see and clean up everything else. No blockers, no timers.

- Interactive mockups: https://claude.ai/artifact/LakBZN4dLuTsr8MkrcQAzH
- App ID: `io.github.salihoguz.minlauncher`
- First user: Samsung Galaxy Note 20 Ultra, Android 13 (One UI 5), about 411 × 882 dp. Shared later, maybe on F-Droid.
- Stack: Kotlin, Jetpack Compose, DataStore for settings, small CSV files on the phone for the drawer-visit and launch logs (kept 60 days). minSdk 28, so the APK also installs on older phones if it's shared later.

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
- Home is transparent over the system wallpaper. Wallpaper dim for home (default 0%) is its own setting, apart from the drawer's.
- Weather line under the date (v0.5). Calendar line (v0.5): off until calendars are picked, shows today's next event, nothing on a free day.

### Gestures

| Gesture | Action |
|---|---|
| Swipe up | Drawer |
| Swipe down | Notifications |
| Swipe left | Camera |
| Swipe right | Phone |
| Double-tap empty space | Lock screen (accessibility service) |
| Hold empty space | Manage, on the Settings tab |
| Tap clock / date | Clock / Calendar |
| Tap weather line | Weather screen (v0.5) |

## Drawer

- One screen; the keyboard is up or down. **Opens with the keyboard up** (setting, default on).
- The list never resizes with the keyboard. The keyboard and search bar float over it. When MinLauncher puts the keyboard away (you dragged or tapped the list), the keyboard's room stays, so nothing moves under your finger. When you put it away with Back, the list follows it down and scrolls to the bottom. Home fades out while the drawer is open.
- Names are 26 sp with the home screen's soft shadow, rows about 50 dp. Wallpaper dim for the drawer is a setting (default 45%), separate from home's; it fades in as one full-screen layer. The list fades out just above the search bar, so names never show through it.
- The list grows upward from the search bar, which sits above the keyboard.
- **While typing:** from the bar upward, "Search Brave for …", then matching apps, best match right above the web row.
- **Enter is a search key and always searches the web** (setting: open the top app instead). Web search opens Brave with its own search engine, or the default browser with Brave Search if Brave isn't installed.
- **Only match opens itself:** setting, default off.
- No live web suggestions while typing.
- **When not typing,** from the bar upward: Pinned, Recent (v0.2), New (v0.2), Folders (v0.3), All apps A–Z.
- **Pinned:** two text columns, no icons, hold and drag to reorder. 8 pinned apps take 4 rows.
- **Icons:** every drawer row except Pinned gets a small grey version of the app's own icon, all in the same rounded-square shape (adaptive icons are trimmed to it; old square icons fill it; round or odd old icons sit on a white tile). Icons are always there, so names never shift.
- **Search matching,** best first: start of the name, start of any word, the app's search words (`bank` → Halkbank), initials (`kn` → Keep Notes), inside the name (from 3 letters), small typos (from 4 letters: `alrmy` → Alarmy). Ignores symbols and accents (`edevlet` → e-Devlet).
- Scrolling the list hides the keyboard.
- **Recent (v0.2):** 5 apps (setting: off, 5, 10), most recently opened from MinLauncher first. Skips apps on home, pinned apps, games, and any app with "Show in Recent" off (per-app switch, off by default for games). "Remove from Recent" hides an app until it's opened again.
- **New (v0.2):** apps installed after MinLauncher started keeping track, never opened from it; they stay until opened once (or removed, pinned, or put on home). ✦ marker for 3 days after install, wherever the name appears.
- **Folders (v0.3):** user-created, from "New folder" in the drawer, "+ New folder" in Move to folder, or Settings. Each folder row shows its app count and opens in place (tap again to close). Open folders show an open-folder icon and a thin line joining them to their apps, which line up under the folder's name. An app in no folder is only in All apps. An unfiled app's sheet suggests a folder from `ApplicationInfo.category` (Games, Media, Social, News, Travel, Work), reusing an existing folder of that name. Settings: rename, delete (apps just leave the folder), move up/down. Names up to 24 letters.
- **A–Z rail (v0.3):** #, A–Z and ↓ down the right edge while the keyboard is down and nothing is typed (setting, default on). Letters bend away from the finger and a bubble shows the letter. It only scrolls: the first app under the letter (or the next letter with apps) comes to the top of the screen. # is for names that don't start with A–Z; ↓ jumps to the bottom (Pinned). Opening an app from All apps after using the rail counts as a rail visit in Stats.
- **Manage button (v0.4):** four small squares at the right end of the search bar while nothing is typed; opens Manage on Library.
- **Hidden apps:** gone from the drawer and search. Listed in Library › Hidden (v0.4).

## Long-press sheet

- **v0.1:** install date; Open · App info · Uninstall; Pin/Unpin · Add to/Remove from home · Rename · Hide.
- **v0.2:** last opened; Rename & search words; Remove from New / Remove from Recent; Show in Recent switch.
- **v0.3:** folder in the details line; Move to folder (the folders, No folder, + New folder); folder suggestion for unfiled apps.
- **v0.4:** size (app + data with usage access, otherwise the installed files); apps that came with the phone show "Disable", which opens App info.

## Manage (v0.4): Library · Stats · Settings

- Opened by holding empty space on home (Settings tab), or from the drawer's Manage button (Library tab). Nothing in it pops up on its own. Tabs across the top, Done or Back to leave.
- **Usage access** (off until turned on in Android's settings, from Settings › Data or the hints in Library and Stats): Android's own records of app use. It adds screen time, unlocks, full app sizes and "unused" for apps never opened from MinLauncher. Without it, "used" means opened from MinLauncher, sizes are the installed files only, and apps can only count as unused once MinLauncher has kept track for 60 days. Records cover the main profile; work apps use MinLauncher's own.
- **Library:** every app, hidden ones included, with full colour icons. Each row: name, then use (time on screen in 30 days, or when last used/opened), size, and tags (hidden, system, work). Sort: least used (default), newest, largest, A–Z. Filters with counts: All, Unused 60+ days, Hidden. Tap opens the app's sheet. Hold to start selecting, tap to add more, then "Uninstall N": Android confirms each one in turn; apps that came with the phone open App info instead, where they can be disabled.
- **Stats:**
  - Today (usage access): time in apps as one big number, unlocks, and the 5 apps used longest. MinLauncher itself doesn't count.
  - Drawer, last 30 days: number of visits and how each ended (typed a name, pinned, recent, new, folder, A–Z rail, scrolled to it, web search, left without opening), as bars with counts and shares, plus how often the keyboard was put away first.
  - Most opened from MinLauncher, last 30 days: top 8 apps (home, drawer and sheet opens), counted from v0.4 on.
- **Suggestions** live only at the top of Stats, at most 5, each with one action and "Not now" (hidden for 30 days):
  - Pin an app typed to open 5+ times in 2 weeks (not on home or pinned).
  - Unpin a pinned app not opened in 30+ days and pinned 30+ days ago.
  - Move a home app opened 2 times or fewer in 30 days to Pinned (needs a month of launch records; skips apps placed in the last 30 days).
  - Turn off Recent when under 3% of 40+ visits in 30 days ended there.
  - Review 3+ unused apps: opens Library › Unused.
- **Settings tab:** everything from before, plus Apps (Hidden apps › Library) and Data (Usage access, Export settings, Import settings). Export writes one JSON file with home, pinned, folders, names, search words and every setting; Import replaces everything after a confirmation. The visit and launch logs stay out of backups.

## Settings and defaults

| Group | Setting | Default | Ships |
|---|---|---|---|
| Home | Big clock: auto / always / never | auto | v0.1 |
| Home | Weather line; city or approximate location | on | v0.5 |
| Home | Calendar line + calendars | off | v0.5 |
| Home | Wallpaper dim | 0% | v0.4.1 |
| Home | Alignment, text size, status bar | left, 30 sp, on | later |
| Gestures | Double-tap to lock | on | v0.1 |
| Gestures | Swipe left / right app | Camera / Phone | later |
| Drawer | Opens with the keyboard | on | v0.1 |
| Drawer | Only match opens itself | off | v0.1 |
| Drawer | Enter key: web search / top app | web search | v0.1 |
| Drawer | Recent: off / 5 / 10 | 5 | v0.2 |
| Drawer | Wallpaper dim | 45% | v0.1, own setting from v0.4.1 |
| Drawer | A–Z rail | on | v0.3 |
| Apps | Hidden apps | — | v0.1 |
| Apps | Folders, renamed apps and search words, apps that skip Recent | games skip Recent | v0.2–v0.3 |
| Look | Theme, font light / regular | system, light | later |
| Data | Usage access (Android setting), export / import settings | off | v0.4 |

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
