package com.dyf.calamitybar;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

public final class ModSounds {
    private ModSounds() {
    }

    public static final SoundEvent RAGE_FULL = register("ragefull");
    public static final SoundEvent RAGE_END = register("rageend");
    public static final SoundEvent RAGE_ACTIVE = register("rageactive");

    public static final SoundEvent ADRENALINE_FULL = register("adrenalinefull");
    public static final SoundEvent ADRENALINE_ACTIVE = register("adrenalineactivate");
    public static final SoundEvent ADRENALINE_LOSS = register("adrenalineloss");

    private static SoundEvent register(String name) {
        ResourceLocation id = DYFCalamityBar.id(name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    /** Referenced from {@link DYFCalamityBar#onInitialize()} to force class init / registration. */
    public static void init() {
    }
}
