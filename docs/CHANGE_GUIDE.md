# Change guide

Use this map to keep future changes surgical.

| Requested change | Primary location |
|---|---|
| Event fields or new event status | `core/model` |
| Local persistence, seed data, cloud sync | `core/data` |
| Brand colors, type, shared cards | `core/designsystem` |
| Search, filters, feed, event details | `feature/discover` |
| Interested/going calendar and memories | `feature/plans` |
| Event publishing form | `feature/create` |
| Profile, privacy, safety settings | `feature/profile` |
| Bottom navigation or new screen route | `app` |
| App icon, manifest, version | `app/src/main/res` and `app/build.gradle.kts` |

## Safe change procedure

1. Identify the owning module from the table.
2. Change only that module unless a shared contract genuinely changes.
3. If a shared contract changes, update `core:model` or `core:data` first and compile before touching UI.
4. Add or update a test beside the behaviour.
5. Run all tests and build the APK.
6. Compare `git diff --stat` with the requested scope; investigate unrelated files.

Avoid moving code between modules during an ordinary feature request. Refactoring and product changes should be separate commits.

