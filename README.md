# TapRise – Android Kotlin Port

A full Kotlin/Android port of the TapRise HTML clicker game. Same game mechanics and layout structure, built natively for Android.

## Project Structure

```
TapRise/
├── app/
│   ├── build.gradle
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/taprise/game/
│       │   ├── GameData.kt          — All data classes, constants, definitions
│       │   ├── GameState.kt         — Mutable game state
│       │   ├── GameEngine.kt        — Pure game logic (no UI)
│       │   ├── GameViewModel.kt     — ViewModel, ticks, LiveData events
│       │   ├── MainActivity.kt      — Main activity, tap button, header, tabs
│       │   ├── ForgeAndCombatFragments.kt
│       │   ├── ExpeditionsAlchemyMarketFragments.kt
│       │   └── DailyAndPrestigeFragments.kt
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml
│           │   ├── tab_item.xml
│           │   ├── fragment_forge.xml
│           │   ├── fragment_combat.xml
│           │   └── fragment_generic.xml   (reused for Expeditions, Alchemy, Market, Daily, Prestige)
│           ├── drawable/             — Custom backgrounds, progress bars
│           └── values/               — colors.xml, strings.xml, themes.xml
├── build.gradle
├── settings.gradle
└── gradle.properties
```

## How to Open in Android Studio

1. Open Android Studio
2. Choose **Open** → select the `TapRise/` folder
3. Let Gradle sync (it will download dependencies automatically)
4. Run on a device or emulator (minSdk 24, API 24+)

## Game Features Implemented

| Feature | Status |
|---------|--------|
| Tap button with floater animations | ✅ |
| XP bar and leveling system | ✅ |
| 6 Forge upgrades (Coin/CPS/Multiplier) | ✅ |
| Combat system with auto-tick | ✅ |
| Hero upgrades (ATK/DEF/HP/CRIT) | ✅ |
| 5 Expeditions with timers | ✅ |
| Alchemy (slot-based crafting, 5 recipes) | ✅ |
| Relic Market with price simulation | ✅ |
| Daily quests (6 quest types) | ✅ |
| Prestige/Ascension system | ✅ |
| 14 Achievements | ✅ |
| Toast notifications | ✅ |
| All game ticks (CPS, combat, market, regen) | ✅ |

## Architecture

- **GameData.kt** – All immutable constants (upgrade definitions, enemy pool, etc.)
- **GameState.kt** – All mutable state (coins, level, inventory, etc.)
- **GameEngine.kt** – Pure Kotlin logic; no Android dependencies. Handles all calculations.
- **GameViewModel.kt** – Bridges engine to UI via LiveData; runs Handler-based ticks
- **Fragments** – One per tab; implement `Refreshable` interface for on-demand UI updates

## Notes

- No persistence/save system yet — add SharedPreferences or Room for save/load
- Sparklines in the market use Unicode block characters (▁▂▃▄▅▆▇█) instead of SVG
- Floating tap indicators use a FrameLayout overlay on the window
