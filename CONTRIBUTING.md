# Contributing

Thanks for wanting to make MinLauncher better.

## Reporting a bug

Open an [issue](https://github.com/SalihOguz/MinLauncher/issues/new/choose) with your phone, Android version, what you did, what you expected and what happened. A screenshot or screen recording helps a lot.

## Suggesting a feature

Open an issue first and describe the problem it solves, before writing code. MinLauncher stays small on purpose, and a short talk saves a pull request that can't be merged. Ideas that fit best:

- make finding, opening or tidying apps faster;
- stay out of the way: nothing pops up on its own, extras are opt-in;
- keep everything on the phone.

Things MinLauncher won't add: app blockers, timers, ads, analytics, accounts.

## Pull requests

1. Fork, then branch from `main`.
2. Keep the change focused on one thing.
3. Put rules (what shows where, what counts as what) in `domain/` as plain Kotlin with a unit test, and keep Android and Compose code in `data/`, `ui/` and `system/`.
4. Run the tests and a build:

   ```sh
   ./gradlew testDebugUnitTest assembleRelease
   ```

5. If behaviour changes, update [docs/spec.md](docs/spec.md) and, for user-facing changes, the README.
6. Describe what changed and why, and how you tried it on a phone.

GitHub Actions builds every pull request; the APK is attached to the run.

By contributing you agree that your work is shared under the project's [GPL-3.0 license](LICENSE).
