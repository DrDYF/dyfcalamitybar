package com.dyf.calamitybar;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModSounds {
    private ModSounds() {
    }

    public static final DeferredRegister<SoundEvent> SOUNDS =
        DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, DYFCalamityBar.MOD_ID);

    public static final RegistryObject<SoundEvent> RAGE_FULL = register("ragefull");
    public static final RegistryObject<SoundEvent> RAGE_END = register("rageend");
    public static final RegistryObject<SoundEvent> RAGE_ACTIVE = register("rageactive");

    public static final RegistryObject<SoundEvent> ADRENALINE_FULL = register("adrenalinefull");
    public static final RegistryObject<SoundEvent> ADRENALINE_ACTIVE = register("adrenalineactivate");
    public static final RegistryObject<SoundEvent> ADRENALINE_LOSS = register("adrenalineloss");

    private static RegistryObject<SoundEvent> register(String name) {
        return SOUNDS.register(name, () ->
            SoundEvent.createVariableRangeEvent(ResourceLocation.fromNamespaceAndPath(DYFCalamityBar.MOD_ID, name)));
    }
}