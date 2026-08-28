# DYF Calamity Bar — Forge 1.20.1

This is the **Forge 1.20.1** (MinecraftForge `47.4.10`) port of DYF Calamity Bar.
The Fabric 1.21.1 version lives on the `master` branch.

Adds the Terraria Calamity **Rage Mode** and **Adrenaline** mechanics:

- **Rage (怒气)** — fills while hostiles are nearby (+35% damage when full); press **G** to activate
  Rage Mode (5s buff, screen shake, radial particle burst).
- **Adrenaline (肾上腺素)** — charges while a boss (Ender Dragon / Wither) is within range and you
  haven't taken damage recently; decays otherwise. A hit while the bar is full halves that damage and
  empties the bar. Press **H** to activate (+150% damage, lightning-bolt particle burst).
- Both meters render above the hotbar with full-bar flash animations and 6 sound cues.
- Configuration: `config/dyfcalamitybar.json`, or press **U** in game for the live edit panel
  (Server / Rage / Adrenaline + Client / UI sections, bilingual).

## Setup (Forge 1.20.1)

- Requires JDK 17+ (Minecraft 1.20.1 targets Java 17)
- `git clone --branch forge1.20.1 https://github.com/DrDYF/dyfcalamitybar.git`
- `gradlew genIntellijRuns` (IntelliJ IDEA) or `gradlew eclipse` (Eclipse)
- Run the generated `runClient` configuration, or `gradlew runClient`
- `gradlew build` produces `build/libs/dyfcalamitybar-1.0.0.jar`

## Port notes (Fabric → Forge)

| Fabric 1.21.1 | Forge 1.20.1 |
|---|---|
| `LivingEntityMixin` (damage) | `LivingDamageEvent` |
| Fabric networking payloads | single `SimpleChannel`, 6 payloads |
| `HudRenderCallback` | custom overlay via `RegisterGuiOverlaysEvent` |
| `Registry.register`/`registerForHolder` | `DeferredRegister` (`ForgeRegistries`) |
| `FabricLoader.getConfigDir()` | `FMLPaths.CONFIGDIR` |
| `fabric.mod.json` | `META-INF/mods.toml` |

Gameplay, config format, keybindings and assets are identical to the Fabric version.

## License

MIT — see `LICENSE`.