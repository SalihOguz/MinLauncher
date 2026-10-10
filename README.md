<p align="center">
  <img src="docs/images/logo.svg" width="112" alt="MinLauncher logo: a light M with a small pink dot">
</p>

<h1 align="center">MinLauncher</h1>

<p align="center">
  A calm, text-first Android launcher with room for all the apps you actually use,<br>
  and every other app one thumb-move away.
</p>

<p align="center">
  <a href="https://github.com/SalihOguz/MinLauncher/releases"><img src="https://img.shields.io/github/v/release/SalihOguz/MinLauncher?label=download" alt="Latest release"></a>
  <img src="https://img.shields.io/badge/Android-9%2B-3DDC84" alt="Android 9 or newer">
  <a href="LICENSE"><img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="License: GPL-3.0"></a>
</p>

---

MinLauncher keeps the look that made [Olauncher](https://github.com/tanujnotes/Olauncher) popular (app names in light text over your wallpaper, a big clock, nothing else) and fixes what gets in the way when you use a minimal launcher every day:

| The problem with most minimal launchers | What MinLauncher does |
|---|---|
| Only 8 apps fit on home | Up to **28**: one column up to 14, then two balanced columns. Spacing shrinks before text does. |
| New apps vanish into a long list | A **New** section keeps them until you open them once, with a ✦ for their first 3 days. |
| A plain text list is hard to browse | Small grey **icons**, **folders** that open in place, and an **A–Z rail** that scrolls the list. |
| Search opens the first match before you've finished typing | Matches are listed right above your thumb, with **Search the web** at the bottom. Nothing opens by itself unless you turn that on. |
| Uninstall and App info are buried | **Hold any app** for Open, App info, Uninstall, pin, rename, hide, move to folder. **Library** lists everything to clean up, several apps at a time. |
| "Digital wellbeing" means blockers and timers | **No blockers, no timers, no pop-ups.** Stats and suggestions are there when you go and look, and never interrupt. |

## Features

### Home
- Your apps as a clean list of names in Olauncher's style, up to 28 of them. Hold one and drag to reorder.
- Big clock (auto, always or never). Auto shows it while there's room. Tap the clock for alarms, the date for your calendar.
- **Weather line:** "18° Partly cloudy · rain around 15:00". The rain warning only appears when rain is likely in the next 12 hours. Tap it for the next 24 hours and the week.
- **Calendar line:** today's next event from the calendars you pick, and nothing on a free day. Tap it to open the event.
- Separate wallpaper dim for home and for the drawer.

| Gesture | Does |
|---|---|
| Swipe up | App drawer |
| Swipe down | Notifications |
| Swipe left / right | Camera / Phone |
| Double-tap empty space | Lock the screen (fingerprint and face unlock keep working) |
| Hold empty space | Settings |

### Drawer
- Opens with the keyboard up and the search bar right above it, so most apps are a few letters away.
- **Forgiving search:** start of a name, any word in it, initials (`kn` → Keep Notes), your own search words (`bank` → your bank's app), small typos (`alrmy` → Alarmy). Accents and symbols are ignored (`edevlet` → e-Devlet).
- **Enter searches the web** in Brave, or in your browser with Brave Search. You can switch Enter to "open the top app" instead.
- With the keyboard down, from your thumb upward:
  - **Pinned:** two columns of apps you choose, drag to reorder.
  - **Recent:** your last 5 apps, skipping home apps, pinned apps and games.
  - **New:** apps you've installed but not opened yet.
  - **Folders:** yours, opened in place. MinLauncher suggests one from Android's app category.
  - **All apps:** with the A–Z rail along the edge. ↓ at the bottom of the rail jumps back to Pinned.
- The list never jumps under your finger when the keyboard closes.

### Hold any app
Open · App info · Uninstall (or Disable, for apps that came with the phone) · Pin · Add to home · Rename & search words · Hide · Move to folder · Remove from New or Recent · Show in Recent. The menu also shows when the app was installed, when you last opened it, and how big it is.

### Manage: Library, Stats, Settings
- **Library:** every app, hidden ones too, with colour icons, how much each is used and its size. Sort by least used, newest, largest or A–Z. Filter to apps unused for 60+ days, or to hidden apps. Hold to select several and uninstall them in a row.
- **Stats:** screen time and unlocks today, how your drawer visits end (typed, pinned, recent, folder, rail…), and your most opened apps. On top, a few gentle **suggestions**, each with one button and "Not now":
  - pin an app you keep typing for;
  - unpin one you never open;
  - move a rarely used home app to Pinned;
  - turn off Recent if you don't use it;
  - review apps you haven't touched in 60 days.
- **Settings:** everything above, plus **Export / Import settings** as one file, for a new phone or a reinstall.

## Install

1. Download the APK from [Releases](https://github.com/SalihOguz/MinLauncher/releases) on your phone and open it. Android will ask you to allow installs from your browser or file manager.
2. Press Home and choose MinLauncher, or go to Android's **Settings › Apps › Default apps › Home app**. MinLauncher's own Settings shows a shortcut while it isn't the default yet.
3. Optional extras, each turned on from MinLauncher's Settings when you want it:
   - **Double-tap to lock:** turn on the MinLauncher service under Accessibility. On Android 13+, if it's greyed out, open MinLauncher's App info, tap ⋮ › *Allow restricted settings*, and try again.
   - **Usage access:** gives Library and Stats Android's own usage records (screen time, unlocks, full app sizes, unused apps). Without it they count what you open from MinLauncher.
   - **Weather:** approximate location or a city you type.
   - **Calendar line:** pick which calendars to read.

Works on Android 9 and newer, including work profiles.

## Privacy

MinLauncher has no ads, no analytics, no accounts and no tracking. Everything it records stays on your phone:

- Settings live in the app's private storage.
- Two small logs (how drawer visits end, which apps you open from MinLauncher) feed Stats. They are kept for 60 days and never leave the phone. They are not included in exports.
- The only network traffic is to [Open-Meteo](https://open-meteo.com) for weather. MinLauncher sends a position rounded to about 10 km, or the city name you search for. No weather set up means no network traffic at all.

| Permission | Why |
|---|---|
| See all apps | To list your apps. Every launcher needs this. |
| Request uninstalls | Uninstall from the app menu and Library. Android still asks you to confirm each one. |
| Expand status bar | Swipe down to open notifications. |
| Set alarm | Tap the clock to open your alarms. |
| Internet | Weather, only if you set it up. |
| Approximate location | Weather for where you are, only if you choose that. Asked for at the time. |
| Read calendar | The calendar line, only for calendars you pick. Asked for at the time. |
| Usage access | Library and Stats, only if you turn it on in Android's settings. |
| Accessibility service | Double-tap to lock, only if you turn it on. It reads nothing from other apps. |

## Build from source

You need JDK 17 and the Android SDK (compile SDK 35).

```sh
./gradlew testDebugUnitTest   # unit tests for the launcher's rules
./gradlew assembleRelease     # app/build/outputs/apk/release/app-release.apk
```

Every push runs the tests and builds an APK on GitHub Actions. The newest one is always on the rolling [`dev-build`](https://github.com/SalihOguz/MinLauncher/releases/tag/dev-build) pre-release. Pushing a version tag (`git tag v1.0.0 && git push --tags`) publishes a proper release.

### Code tour

| Folder | What's in it |
|---|---|
| `app/src/main/java/.../domain` | The launcher's rules in plain Kotlin, with unit tests: home layout, search matching, Recent and New, folders, the A–Z rail, stats, suggestions, weather wording, today's event. |
| `.../data` | Installed apps, settings (DataStore), the on-device logs, usage records, icons, weather and calendar sources. |
| `.../ui` | Jetpack Compose screens: home, drawer, app menu, Manage (Library, Stats, Settings), weather. |
| `.../system` | Talking to Android: launching apps, App info, uninstall, web search, the lock service. |
| `docs/spec.md` | Design notes: every behaviour, number and default in detail. |

### Signing

Without a release key, builds are signed with the debug key committed in `app/debug.keystore`. That's fine for trying things out, but anyone can sign an APK with that key. For releases people will rely on, the workflow signs with a private key kept in the repository's secrets:

```sh
keytool -genkeypair -v -keystore release.jks -alias minlauncher -keyalg RSA -keysize 4096 -validity 10000
base64 -w0 release.jks   # the value for RELEASE_KEYSTORE_BASE64
```

Add these under **Settings › Secrets and variables › Actions**: `RELEASE_KEYSTORE_BASE64`, `RELEASE_KEYSTORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`. Keep `release.jks` and its passwords somewhere safe: every future update must be signed with the same key. An APK signed with a different key can't install over the old one, so export your settings, uninstall, install, then import.

## Contributing

Bug reports and ideas are welcome in [Issues](https://github.com/SalihOguz/MinLauncher/issues). Pull requests too; see [CONTRIBUTING.md](CONTRIBUTING.md). MinLauncher follows a few principles:

- **Calm by default.** Nothing pops up on its own; extras are opt-in.
- **Text first.** The home screen stays names over your wallpaper.
- **Your phone, your data.** No tracking, and the network only for weather you asked for.
- **No blockers.** It helps you find and tidy apps; it doesn't police you.

## Thanks

MinLauncher's design borrows ideas, but no code, from [Olauncher](https://github.com/tanujnotes/Olauncher), [Minimo](https://github.com/VaibhavLakhera/minimo-launcher), Indistractable, Before Launcher and ProLauncher. Weather data by [Open-Meteo.com](https://open-meteo.com) ([CC BY 4.0](https://creativecommons.org/licenses/by/4.0/)).

## License

[GNU GPL v3.0](LICENSE). You can use, study, share and change MinLauncher. If you share a changed version, share its source under the same license.
