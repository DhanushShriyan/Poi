# Architecture and change isolation

Poi uses feature modules and a repository boundary so that a requested change can remain inside the smallest relevant area.

```text
                         ┌──────────────────────┐
                         │         app          │
                         │ navigation + wiring  │
                         └──────────┬───────────┘
                                    │
             ┌──────────────┬───────┼─────────┬──────────────┐
             │              │       │         │              │
        discover          plans   create    profile          │
             └──────────────┴───────┴─────────┴──────────────┘
                                    │
             ┌──────────────────────┼──────────────────────┐
             │                      │                      │
       core:designsystem       core:data              core:model
       theme/components      repository boundary     plain models
```

## Dependency rules

1. A feature module never imports another feature module.
2. Product data types live only in `core:model`.
3. Screens use `EventRepository`; they do not know whether data is local, Supabase, or another backend.
4. Shared colors, typography, and reusable UI live only in `core:designsystem`.
5. `app` contains navigation and dependency assembly, not feature behaviour.
6. Service credentials are injected at build time and are never committed.

These rules prevent a visual change in Create from affecting discovery logic, and prevent a backend migration from requiring screen rewrites.

## State ownership

- Durable product state: `EventRepository`.
- Screen-only input such as search text and selected filters: the owning feature.
- Navigation state: `app`.
- Local test persistence: `SharedPreferences` inside `LocalEventRepository`.
- Future cloud persistence: a new repository implementation selected by `PoiApplication`.

## Cloud migration boundary

To add cloud sync, implement `EventRepository` in a new data source such as `SupabaseEventRepository`. Do not change the method signatures until the cloud schema has been reviewed. Switch the implementation in `PoiApplication`; feature screens remain unchanged.

## Testing strategy

- `core:model`: model behaviour and time-window rules.
- `core:data`: search/filter rules and repository tests.
- Feature modules: UI/state tests should live with the screen they cover.
- `app`: navigation and end-to-end smoke tests.

Run `gradlew test assembleDebug` before every handoff.

