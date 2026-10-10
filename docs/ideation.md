# MinLauncher: ideation

A calmer Olauncher you can actually live in: the same text-only home screen, room for up to 15 apps, and a drawer that helps you find, recognise and clean up apps you forgot you had. No app blockers, no timers.

Interactive mockups: https://claude.ai/artifact/LakBZN4dLuTsr8MkrcQAzH

## Why Olauncher feels limited (checked in its source)

| Pain | Cause in Olauncher |
|---|---|
| Max 8 home apps | Home is eight fixed views (`homeApp1`…`homeApp8` in `fragment_home.xml`), not a list |
| New apps get lost | The ✦ marker shows only for 1 hour after install (`now - firstInstallTime < ONE_HOUR`) |
| Can't browse | Drawer has one order: alphabetical. No recents, install date, icons or folders |
| Uninstall / app info hard to find | They exist, but only behind a long-press with no hint |

## Guiding idea: a friction ladder

Each layer costs a little more effort and shows a little more information. The fast layers stay text-only.

1. **Home**: the 8–15 apps you picked. Text only.
2. **Search**: swipe up, type. Text only.
3. **Browse**: close the keyboard. Small monochrome icons, New, Recent, folders.
4. **Library**: every app with last-used, install date and size. Cleanup lives here.

## 1. Home (up to 15 apps, same look)

Squeeze spacing first, shrink text only after that.

- **1A Auto-fit** (recommended): pick 1–15; rows are sized to the free height. Text never exceeds today's size and has a readable floor.
- **1B Compact clock above 10 apps** (recommended with 1A): clock and date fold into one line. In the mockup, 15 apps then fit at 100% of today's text size, rows ≈45 dp (Android recommends 48 dp touch targets, so 15 is about the ceiling).
- **1C Two columns** (option): full text size; long names may truncate.
- **1D Second home page** (skip): clashes with left/right swipe shortcuts.

## 2–4. Drawer: search when you know, browse when you don't

Search mode:
- Search bar above the keyboard; results grow upward toward the thumb.
- Empty query shows Recent and New right above the keyboard.
- Matches initials (`km` → Keep Notes), user-added search words (`bank` → Revolut), folder names, small typos.
- Single match auto-opens; no match offers a Play Store search.

Options:
- **2A New until settled** (recommended): a new app stays in New (✦) until opened 3 times, filed into a folder, or added to home.
- **2B Rename + search words** (recommended): give an app words like `hiking`, `bike` so search finds it.
- **3A Long-press sheet** (recommended): install date, last opened, size, folder; Open · App info · Uninstall; Move to folder · Add to home · Rename · Hide.
- **4A Folders with Unsorted as inbox** (recommended): one folder per app, accordion inline so the drawer stays one list. New apps land in Unsorted.
- **4B Folder suggestions from `ApplicationInfo.category`** (option): Game, Social, Maps, Audio, Video, Image, News, Productivity. Many apps declare none, so suggestion only.
- **4C Tags instead of folders** (skip for now).

Icons:

| Style | Recognition | Calm | Note |
|---|---|---|---|
| None | Low | High | Current state |
| **Monochrome** | Good | High | Android 13+ themed-icon layer (`AdaptiveIconDrawable.getMonochrome()`), greyscale fallback. Recommended, Browse + Library only |
| Greyscale | Good | Medium | Works for every app; grey tiles add weight |
| Colour | Best | Low | Built to grab attention |

Recent apps guardrails: exclude home apps, cap at 5, show only with empty search, optional per-app "never show in Recent" (not a blocker), count launcher launches only (no permission).

## Library (cleanup)

- Sort: least used, newest, largest, A–Z. Filter: unused 60+ days.
- Multi-select uninstall; Android still confirms each app, the launcher queues them.
- Never pops up on its own.

| Data | Source | Permission |
|---|---|---|
| Install date | `PackageInfo.firstInstallTime` | None |
| Last opened from launcher | own launch log | None |
| Last opened any way | `UsageStatsManager` | Usage access (optional) |
| App size | `StorageStatsManager` | Usage access (optional) |

## Build path

- **B1 Fork Olauncher** (not recommended): GPLv3, Android Views, hard-coded 8 slots; most code would be replaced.
- **B2 Fork Minimo** (fast start): MIT, Kotlin + Compose + Room; already has folders, optional icons, unlimited favourites, app info/uninstall. Needs restyling and settings/billing stripped.
- **B3 Fresh Kotlin + Compose, borrowing from Minimo** (recommended): `LauncherApps` for apps and package events, Room for folders/search words/launch history, DataStore for settings.

| Step | Ships |
|---|---|
| v0.1 | Home with auto-fit + compact clock, gestures, search drawer, long-press sheet (replaces Olauncher day to day) |
| v0.2 | New-until-settled, Recent with guardrails, search words, initials/typo matching |
| v0.3 | Browse mode, monochrome icons, folders with Unsorted inbox |
| v0.4 | Library: sorts, unused filter, batch uninstall, optional usage access |

## Open decisions

1. Home: 1A+1B or 1C?
2. Search bar above the keyboard, or at the top like Olauncher?
3. Icons in Browse/Library only, or in search results too?
4. Recent: off, 5 or 10?
5. B3 (fresh) or B2 (fork Minimo)? Publish it? Open source?
6. Phone's Android version (monochrome icons need 13+).
