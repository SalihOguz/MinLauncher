# MinLauncher: ideation

Olauncher's home screen, kept as it is, with room for 15 apps. A drawer that puts what you need right above the keyboard, and a Library for cleaning up. No blockers, no timers.

Interactive mockups: https://claude.ai/artifact/LakBZN4dLuTsr8MkrcQAzH

Target device: Samsung Galaxy Note 20 Ultra, Android 13 (One UI 5), about 411 × 882 dp.

## Decided

- **Home:** auto-fit up to 15 apps; compact clock from 12 apps (above 11); drag to reorder. Keeps Olauncher's fonts, sizes and positions.
- **Gestures:** swipe up drawer, swipe down notifications, swipe left Camera, swipe right Phone, double-tap lock, hold empty space for settings. Tap clock/date opens Clock/Calendar.
- **Drawer:** search bar above the keyboard.
- **Pinned** apps in the drawer (secondary apps like banks), drag to reorder.
- **Recent:** 5 apps, as an experiment. Skips apps on home, pinned apps, swipe-gesture apps, games, and any app with "Show in Recent" off. The switch is per app and off by default for games.
- **New:** an app stays in New until opened once (it then shows in Recent). ✦ marker shows for 3 days after install, wherever the app appears.
- **Rename + search words** per app.
- **Long-press sheet:** install date, last opened, size, folder, search words; Open · App info · Uninstall; Pin/Unpin · Add to/Remove from home · Move to folder · Rename & words · Hide; Remove from New / Remove from Recent when they apply; Show in Recent switch.
- **Folders** with Unsorted as the inbox. When you long-press an unfiled app, suggest a folder from `ApplicationInfo.category`.
- **Icons:** monochrome in the browse list; full colour in Library.
- **Hidden apps:** gone from the drawer and search, still listed in Library under Hidden.
- **Tags instead of folders:** skipped.
- **Build:** fresh, Kotlin + Jetpack Compose. Borrow from Minimo (MIT) only where useful.

## Olauncher's home look (from its source, xxhdpi resources)

| Element | Value |
|---|---|
| App names | 30 sp, `sans-serif-light`, white, shadow dy 2 dp, radius 4 dp |
| Vertical padding per name | 10 dp |
| Side margin | 24 dp, left aligned |
| Clock | 66 sp `sans-serif-light`, 56 dp from top |
| Date | 18 sp, `EEE, dd MMM` |
| App list | paddingTop 112 dp, paddingBottom 48 dp, `center_vertical` |

Auto-fit on the Note 20 Ultra: squeeze spacing first, shrink text only if needed.
- 1–11 apps, big clock: rows go from about 60 dp to 54 dp; text stays 30 sp.
- 12–15 apps, compact one-line clock: rows go down to about 48 dp at 15; text still stays 30 sp.

## Drawer

One screen; the keyboard is just up or down. The list grows upward from the search bar (thumb reach on a 6.9" phone), in this order from the bar:

1. Search bar
2. Pinned (about 4 fit with Recent above the keyboard)
3. Recent (5)
4. New
5. Folders (accordion, inline), including Unsorted
6. All apps A–Z

About 470 dp stays visible above the keyboard: 9 rows at 44 dp, so 4 Pinned + 5 Recent fit exactly. Drawer rows are 44 dp, smaller than home's.

Search: matches start of name, start of any word, initials (`km` → Keep Notes), search words (`bank` → Revolut), folder names, small typos; ignores symbols (`edevlet` → e-Devlet). The best match sits next to the search bar. A single match opens by itself. With no app match, Enter searches the web in Brave.

Keyboard down (browse): monochrome icons and an A–Z rail on the right edge (Indistract-style: letters bend away from the finger). The rail filters the list to that letter, so results stay near the thumb.

### Search vs browse (proposal)

- **D1, recommended:** open with the keyboard up. Pinned and Recent sit right above it, so there's nothing to close the keyboard for. Scrolling hides the keyboard for browsing.
- **D2, setting:** open with the list.
- **D3, recommended:** a local counter records how each drawer visit ended (typed, pinned, recent, folder, A–Z, web). After two weeks, look at the counts in Library and pick D1 or D2. Needs no permission.

## Widgets (proposal)

- **W1, recommended:** glance line under the date ("18° · rain from 15:00"). It joins the one-line clock in compact mode. Tap opens W2.
- **W2, recommended:** minimal weather screen with an animated monochrome icon, now, the next hours and tomorrow. Data from Open-Meteo (free, no key). A typed city avoids the location permission.
- **W3, recommended:** Brave web search from the drawer, which replaces the search widget.
- **W4, option:** next calendar event in the glance line (needs calendar permission).
- **W5, option:** screen time and unlocks in the Library header (usage access).
- **W6, skip:** a page of real Android widgets.

## Library

- Full colour icons.
- Sort: least used, newest, largest, A–Z. Filters: Unused 60+ days, Hidden.
- Batch uninstall: Android confirms each app; the launcher queues them. System apps are pointed to App info › Disable.
- Optional header: screen time and unlocks today, drawer-visit stats.

| Data | Source | Permission |
|---|---|---|
| Install date | `PackageInfo.firstInstallTime` | None |
| Last opened from launcher, drawer stats | own launch log (Room) | None |
| Last opened any way, screen time, unlocks | `UsageStatsManager` | Usage access (optional) |
| App size | `StorageStatsManager` | Usage access (optional) |
| Folder suggestion | `ApplicationInfo.category` | None |
| Monochrome icon | `AdaptiveIconDrawable.getMonochrome()` (API 33) | None |

## Settings (proposal)

- **Home:** apps on home (1–15); compact clock threshold (12); alignment; text size (30 sp); clock, date, 24h and the apps they open; glance line; wallpaper dim %; daily wallpaper; status bar.
- **Gestures:** swipe left app, swipe right app, double-tap lock, swipe down notifications.
- **Drawer:** opens with keyboard or list; auto-open single match; Recent off/5/10; mono icons browse-only or always; A–Z rail; drawer dim %; web search browser; ignore symbols.
- **Apps:** folders; hidden apps (opens Library); renamed apps and search words; apps that skip Recent.
- **Look:** theme; font light or regular.
- **Data:** usage access; weather city or location; export/import settings.

Left out from Minimo: notification dots, battery level, icons on home, landscape mode, search bar styling, billing.

## Build order

| Step | Ships |
|---|---|
| v0.1 | Home (Olauncher look, auto-fit to 15, compact clock, drag reorder, gestures, double-tap lock), drawer with keyboard up and search bar above it, Pinned, long-press sheet, basic settings |
| v0.2 | New + ✦, Recent rules + Show in Recent switch, search words/initials/typos, Brave web search, drawer-visit counter |
| v0.3 | Browse (keyboard down): mono icons, folders + Unsorted + category suggestions, A–Z rail, hidden apps |
| v0.4 | Library: colour icons, sorts, filters, batch uninstall, usage access, screen time, drawer stats |
| v0.5 | Glance line + weather screen |

Stack: Kotlin, Jetpack Compose, Room, DataStore, coroutines/Flow. No ads, no analytics. The only network call is weather.

## Parked

- ProLauncher-style "gentle interrupt" reminders. They need foreground-app detection (usage-access polling or an accessibility service) and an overlay. The user's time sinks are inside Brave, which a launcher sees only as "Brave".

## Open questions (round 2)

1. Start with the keyboard up (D1) plus the visit counter (D3)?
2. With icons only when browsing, names shift right when the keyboard closes. Is that fine, or show icons always?
3. Never-opened apps: stay in New until opened or removed, or move to Unsorted after 30 days?
4. Is about 4 pinned apps enough?
5. Glance line: weather only, or weather plus next event? Weather from a typed city or from approximate location?
6. Keep Olauncher's daily wallpaper feature?
7. App ID `io.github.salihoguz.minlauncher`?

## Round 1 findings (Olauncher source)

- Home is 8 fixed views (`homeApp1`…`homeApp8`), hence the 8-app limit.
- The ✦ new marker lasts 1 hour after install (`now - firstInstallTime < ONE_HOUR`).
- The drawer is sorted alphabetically only.
- Uninstall, App info, Hide and Rename sit behind a long-press with no hint.
