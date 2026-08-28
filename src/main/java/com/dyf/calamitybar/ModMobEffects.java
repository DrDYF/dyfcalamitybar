package com.dyf.calamitybar;

import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * Rage Mode and Adrenaline Mode mob effects, registered on the Forge registry
 * bus. Icons are resolved from
 * {@code assets/dyfcalamitybar/textures/mob_effect/*.png}.
 */
public final class ModMobEffects {
    private ModMobEffects() {
    }

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, DYFCalamityBar.MOD_ID);

    /** The Rage Mode effect; treated as a beneficial buff (shown in the buff row). */
    public static final RegistryObject<MobEffect> RAGE_MODE =
        EFFECTS.register("rage_mode", RageModeEffect::new);

    /** The Adrenaline Mode effect; treated as a beneficial buff (shown in the buff row). */
    public static final RegistryObject<MobEffect> ADRENALINE_MODE =
        EFFECTS.register("adrenaline_mode", AdrenalineModeEffect::new);
}