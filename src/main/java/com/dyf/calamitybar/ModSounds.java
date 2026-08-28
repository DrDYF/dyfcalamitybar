package com.dyf.calamitybar;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound events for the rage and adrenaline meter cues, registered on the
 * NeoForge mod event bus. The {@code sounds.json} asset maps each id to its
 * {@code .ogg} file.
 */
public final class ModSounds {
    private ModSounds() {
    }

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(Registries.SOUND_EVENT, DYFCalamityBar.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> RAGE_FULL = register("ragefull");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAGE_END = register("rageend");
    public static final DeferredHolder<SoundEvent, SoundEvent> RAGE_ACTIVE = register("rageactive");

    public static final DeferredHolder<SoundEvent, SoundEvent> ADRENALINE_FULL = register("adrenalinefull");
    public static final DeferredHolder<SoundEvent, SoundEvent> ADRENALINE_ACTIVE = register("adrenalineactivate");
    public static final DeferredHolder<SoundEvent, SoundEvent> ADRENALINE_LOSS = register("adrenalineloss");

    private static DeferredHolder<SoundEvent, SoundEvent> register(String name) {
        return SOUNDS.register(name, () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, name)));
    }
}