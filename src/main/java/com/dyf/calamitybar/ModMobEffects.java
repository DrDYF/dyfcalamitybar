package com.dyf.calamitybar;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Rage Mode and Adrenaline Mode mob effects, registered on the NeoForge mod
 * event bus. Icons are resolved from
 * {@code assets/dyfcalamitybar/textures/mob_effect/*.png}. The holders are
 * used directly with the 1.21.1 {@code Holder}-based effect APIs.
 */
public final class ModMobEffects {
    private ModMobEffects() {
    }

    public static final DeferredRegister<MobEffect> EFFECTS =
        DeferredRegister.create(Registries.MOB_EFFECT, DYFCalamityBar.MOD_ID);

    /** The Rage Mode effect; treated as a beneficial buff (shown in the buff row). */
    public static final DeferredHolder<MobEffect, MobEffect> RAGE_MODE =
        EFFECTS.register("rage_mode", RageModeEffect::new);

    /** The Adrenaline Mode effect; treated as a beneficial buff (shown in the buff row). */
    public static final DeferredHolder<MobEffect, MobEffect> ADRENALINE_MODE =
        EFFECTS.register("adrenaline_mode", AdrenalineModeEffect::new);
}