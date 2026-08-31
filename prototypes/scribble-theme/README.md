# Poi Scribble Theme Lab

This is an isolated, offline visual prototype. It has a different application ID
(`com.dhanushshriyan.poi.scribble`) and installs beside the production Poi app.

It deliberately contains only local sample events, date navigation, search,
attendance reactions, a plans view, sharing and a profile/theme summary. It does
not connect to Supabase, request location, use production credentials or update Poi.

Build from the repository root:

```powershell
.\gradlew.bat -p .\prototypes\scribble-theme test assembleDebug
```
