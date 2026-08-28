# DYF Calamity Bar — NeoForge 1.21.1

This is the **NeoForge 1.21.1** (NeoForge `21.1.249`) port of DYF Calamity Bar.
Other loaders:

- `master` — Fabric 1.21.1
- `forge1.20.1` — Forge 1.20.1 (MinecraftForge `47.4.10`)

Adds the Terraria Calamity **Rage Mode** and **Adrenaline** mechanics:

- **Rage (怒气)** — fills while hostiles are nearby (+35% damage when full); press **G** to activate
  Rage Mode (5s buff, screen shake, radial particle burst).
- **Adrenaline (肾上腺素)** — charges while a boss (Ender Dragon / Wither) is within range and you
  haven't taken damage recently; decays otherwise. A hit while the bar is full halves that damage and
  empties the bar. Press **H** to activate (+150% damage, lightning-bolt particle burst).
- Both meters render above the hotbar with full-bar flash animations and 6 sound cues.
- Configuration: `config/dyfcalamitybar.json`, or press **U** in game for the live edit panel
  (Server / Rage / Adrenaline + Client / UI sections, bilingual).

## Setup (NeoForge 1.21.1)

Built with ModDevGradle 2.0.144.

- Requires JDK 21 (Minecraft 1.21.1 targets Java 21)
- `git clone --branch neoforge1.21.1 https://github.com/DrDYF/dyfcalamitybar.git`
- `gradlew runClient` (or open in IDEA and run the `runClient` task)
- `gradlew build` produces `build/libs/dyfcalamitybar-1.0.0.jar`

## Port notes (Fabric → NeoForge)

| Fabric 1.21.1 | NeoForge 1.21.1 |
|---|---|
| `LivingEntityMixin` (damage) | `LivingDamageEvent.Pre` (`getNewDamage` / `setNewDamage`) |
| Fabric payload registration | `RegisterPayloadHandlersEvent` + `PayloadRegistrar` (same `CustomPacketPayload`s, byte-identical) |
| `ClientPlayNetworking` / `ServerPlayNetworking` | `PacketDistributor.sendToServer` / `sendToPlayer` |
| `HudRenderCallback` | custom overlay via `RegisterGuiLayersEvent` (with `DeltaTracker`) |
| `KeyBindingHelper` | `RegisterKeyMappingsEvent` |
| `Register.register` / `registerForHolder` | `DeferredRegister` + `DeferredHolder` (`Registries.MOB_EFFECT` / `SOUND_EVENT`) |
| `FabricLoader.getConfigDir()` | `FMLPaths.CONFIGDIR` (NeoForge) |
| `fabric.mod.json` | `META-INF/neoforge.mods.toml` (from `src/main/templates`) |

Gameplay, config format, keybindings, payloads and assets are identical across all three loaders.

## License

MIT — see `LICENSE`.