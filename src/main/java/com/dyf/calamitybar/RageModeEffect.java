package com.dyf.calamitybar;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The Rage Mode effect. Its damage bonus is applied multiplicatively by
 * {@link com.dyf.calamitybar.mixin.LivingEntityMixin}; the effect itself only
 * acts as a marker and a HUD indicator.
 */
public class RageModeEffect extends MobEffect {
    public RageModeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xE04B3A);
    }
}
