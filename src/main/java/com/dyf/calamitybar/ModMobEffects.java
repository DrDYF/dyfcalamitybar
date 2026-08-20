package com.dyf.calamitybar;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;

public final class ModMobEffects {
    private ModMobEffects() {
    }

    /**
     * The Rage Mode effect. Its icon texture is resolved from
     * {@code assets/dyfcalamitybar/textures/mob_effect/rage_mode.png} and it
     * is treated as a beneficial effect (shown in the buff row).
     */
    public static final Holder<MobEffect> RAGE_MODE = Registry.registerForHolder(
        BuiltInRegistries.MOB_EFFECT,
        ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "rage_mode"),
        new RageModeEffect()
    );

    /**
     * The Adrenaline Mode effect. Its icon texture is resolved from
     * {@code assets/dyfcalamitybar/textures/mob_effect/adrenaline_mode.png}.
     */
    public static final Holder<MobEffect> ADRENALINE_MODE = Registry.registerForHolder(
        BuiltInRegistries.MOB_EFFECT,
        ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, "adrenaline_mode"),
        new AdrenalineModeEffect()
    );

    /** Referenced from {@link DYFCalamityBar#onInitialize()} to force class init / registration. */
    public static void init() {
    }
}
