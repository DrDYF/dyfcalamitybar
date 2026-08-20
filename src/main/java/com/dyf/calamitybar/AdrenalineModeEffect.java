package com.dyf.calamitybar;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * The Adrenaline Mode effect. Its damage bonus is applied multiplicatively by
 * {@link com.dyf.calamitybar.mixin.LivingEntityMixin} after all other damage
 * bonuses; the effect itself only acts as a marker and a HUD indicator.
 */
public class AdrenalineModeEffect extends MobEffect {
    public AdrenalineModeEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x2E9BD6);
    }
}