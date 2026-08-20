package com.dyf.calamitybar.mixin;

import com.dyf.calamitybar.AdrenalineManager;
import com.dyf.calamitybar.ModMobEffects;
import com.dyf.calamitybar.RageConfig;
import com.dyf.calamitybar.RageManager;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Damage hooks applied around {@link LivingEntity#actuallyHurt}:
 *
 * <ul>
 *   <li>When the attacker is a player under Rage Mode / Adrenaline Mode, the
 *       damage is multiplied (Rage +35%, then Adrenaline +150% on top, so the
 *       Adrenaline bonus is applied after all other bonuses).</li>
 *   <li>When the victim is a player, the adrenaline bar reacts to the hit
 *       (full bar: damage halved and bar emptied; otherwise damage-scaled
 *       loss), and the possibly-halved damage is what actually applies.</li>
 * </ul>
 *
 * <p>{@code actuallyHurt} has two mutually exclusive call sites in
 * {@code hurt} (the invulnerability-cooldown excess-damage path and the normal
 * path), so this wrapper fires exactly once per hit.
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @WrapOperation(
        method = "hurt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/entity/LivingEntity;actuallyHurt(Lnet/minecraft/world/damagesource/DamageSource;F)V"
        )
    )
    private void calamitybar$applyDamageModifiers(
        LivingEntity instance,
        DamageSource source,
        float amount,
        Operation<Void> original
    ) {
        if (source.getEntity() instanceof Player player && !player.level().isClientSide) {
            RageManager.onPlayerDealtDamage(player);
            if (player.hasEffect(ModMobEffects.RAGE_MODE)) {
                amount *= RageConfig.rageDamageMultiplier;
            }
            if (player.hasEffect(ModMobEffects.ADRENALINE_MODE)) {
                amount *= RageConfig.adrenalineDamageMultiplier;
            }
        }

        // Player as the victim: adrenaline reacts to incoming damage.
        if (instance instanceof ServerPlayer receiver && !instance.level().isClientSide) {
            amount = AdrenalineManager.onPlayerHurt(receiver, amount);
        }

        original.call(instance, source, amount);
    }
}