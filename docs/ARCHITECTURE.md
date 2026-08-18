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
             ┌────────────────┬─────┴─────────────┬────────────────┐
             │                │                   │                │
       core:designsystem  core:update         core:data       core:model
       theme/components   app delivery     repository boundary plain models
```

## Dependency rules

1. A feature module never imports another feature module.
2. Product data types live only in `core:model`.
3. Screens use `EventRepository`; they do not know whether data is local, Supabase, or another backend.
4. Shared colors, typography, and reusable UI live only in `core:designsystem`.
5. `app` contains navigation and dependency assembly, not feature behaviour.
6. Service credentials are injected at build time and are never committed.
7. Update delivery lives in `core:update`; feature modules never depend on it.
8. Identity and roles are exposed only through `core:auth`; `feature:admin` never owns credentials.
9. Admin screens check the local role, while the future cloud API must independently authorize every privileged operation.

These rules prevent a visual change in Create from affecting discovery logic, and prevent a backend migration from requiring screen rewrites.

## State ownership

- Durable product state: `EventRepository`.
- Identity and role state: `AuthRepository`.
- Screen-only input such as search text and selected filters: the owning feature.
- Navigation state: `app`.
- Release discovery and installer handoff: `core:update`.
- Local test persistence: `SharedPreferences` inside `LocalEventRepository`.
- Future cloud persistence: a new repository implementation selected by `PoiApplication`.

## Identity and administration

`feature:auth` owns guest and sign-in presentation. `core:auth` owns the repository contract and the offline preview implementation. `feature:admin` owns moderation and event editing, while `app` exposes those routes only to an administrator session. Replacing `LocalAuthRepository` with a server-backed implementation should not require changes to feature screens.

## Cloud migration boundary

To add cloud sync, implement `EventRepository` in a new data source such as `SupabaseEventRepository`. Do not change the method signatures until the cloud schema has been reviewed. Switch the implementation in `PoiApplication`; feature screens remain unchanged.

## Testing strategy

- `core:model`: model behaviour and time-window rules.
- `core:data`: search/filter rules and repository tests.
- Feature modules: UI/state tests should live with the screen they cover.
- `app`: navigation and end-to-end smoke tests.

Run `gradlew test assembleDebug` before every handoff.
