package com.dyf.calamitybar.client;

import com.dyf.calamitybar.DYFCalamityBar;
import com.dyf.calamitybar.ModSounds;
import com.dyf.calamitybar.RageConfig;
import com.dyf.calamitybar.network.ModNetworking;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;

/**
 * Client-side rage meter state and HUD rendering.
 *
 * <p>The meter is a 51-frame horizontal bar. Frames 0-40 are the normal fill
 * ramp (rage 0-100); frames 40-49 are the "just filled" flash animation which
 * plays over {@link RageConfig#FULL_FLASH_DURATION_SECONDS} when the meter
 * reaches full, settling on frame 50 (the "activation complete" frame).
 *
 * <p>{@link #displayRage} lags behind the server-authoritative {@link #targetRage}
 * on the way down so that activating Rage Mode drains the bar smoothly instead
 * of snapping it to zero.
 */
public final class RageHud {
    private RageHud() {
    }

    private static final ResourceLocation[] FRAMES = new ResourceLocation[51];
    private static final Random RANDOM = new Random();

    static {
        for (int i = 0; i < FRAMES.length; i++) {
            FRAMES[i] = ResourceLocation.fromNamespaceAndPath(
                DYFCalamityBar.MOD_ID,
                String.format("textures/ui/rage_meter/rage_meter_%04d.png", i)
            );
        }
    }

    /** Server-authoritative rage value (0-100). */
    private static float targetRage;
    /** Smoothly-animated value actually drawn (0-100). */
    private static float displayRage;
    /** Whether the meter is currently considered full (drives the flash trigger). */
    private static boolean wasFull;
    /** Game time (ticks) at which the full-flash began, or -1 when not playing. */
    private static long fullFlashStartTick = -1;
    /** Game time (ticks) at which the activation shake ends, or -1 when idle. */
    private static long shakeEndTick = -1;

    public static void setRage(float value) {
        targetRage = Math.max(0.0f, Math.min(RageConfig.MAX_RAGE, value));
    }

    public static float getRage() {
        return targetRage;
    }

    public static boolean isFull() {
        return targetRage >= RageConfig.MAX_RAGE - 0.5f;
    }

    /** Plays the sound cue associated with a server-sent event. */
    public static void handleEvent(byte event) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (event) {
            case ModNetworking.EVENT_FULL ->
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.RAGE_FULL.get(), 1.0f));
            case ModNetworking.EVENT_ACTIVATE -> {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.RAGE_ACTIVE.get(), 1.0f));
                startShake(minecraft);
            }
            case ModNetworking.EVENT_END ->
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.RAGE_END.get(), 1.0f));
            default -> {
            }
        }
    }

    private static void startShake(Minecraft minecraft) {
        if (minecraft.level != null) {
            shakeEndTick = minecraft.level.getGameTime()
                + (long) (RageConfig.rageModeDurationSeconds * 20);
        }
    }

    /** Called once per client tick to advance animations. */
    public static void tick() {
        // Drain smoothly toward the server value (activation empties the bar).
        if (displayRage > targetRage) {
            float step = RageConfig.drainRate() / 20.0f;
            displayRage = Math.max(targetRage, displayRage - step);
        } else if (displayRage < targetRage) {
            // Fill: the server already sends small increments every tick, so snap up.
            displayRage = targetRage;
        }

        // Trigger the full-flash on the rising edge of "full".
        boolean full = targetRage >= RageConfig.MAX_RAGE - 0.5f;
        if (full && !wasFull) {
            wasFull = true;
            Minecraft minecraft = Minecraft.getInstance();
            fullFlashStartTick = minecraft.level != null ? minecraft.level.getGameTime() : -1;
        } else if (!full) {
            wasFull = false;
            fullFlashStartTick = -1;
        }
    }

    public static void render(GuiGraphics graphics, float delta) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui
            || minecraft.screen instanceof ChatScreen) {
            return;
        }

        int frame = computeFrame(minecraft, delta);

        int shakeX = 0;
        int shakeY = 0;
        if (minecraft.level != null && shakeEndTick >= 0) {
            long remaining = shakeEndTick - minecraft.level.getGameTime();
            if (remaining <= 0) {
                shakeEndTick = -1;
            } else {
                float progress = remaining / (RageConfig.rageModeDurationSeconds * 20.0f);
                float magnitude = RageConfig.shakeMagnitude * progress;
                shakeX = Math.round((RANDOM.nextFloat() * 2.0f - 1.0f) * magnitude);
                shakeY = Math.round((RANDOM.nextFloat() * 2.0f - 1.0f) * magnitude);
            }
        }

        int x = RageConfig.meterX + shakeX;
        int y = graphics.guiHeight() - RageConfig.meterHeight - RageConfig.meterY + shakeY;

        graphics.blit(
            FRAMES[frame],
            x,
            y,
            0.0f,
            0.0f,
            RageConfig.meterWidth,
            RageConfig.meterHeight,
            RageConfig.meterWidth,
            RageConfig.meterHeight
        );
    }

    private static int computeFrame(Minecraft minecraft, float delta) {
        boolean full = targetRage >= RageConfig.MAX_RAGE - 0.5f;

        if (full) {
            // Full-flash animation (40..49), then hold the "activation complete" frame (50).
            if (fullFlashStartTick >= 0 && minecraft.level != null) {
                float durationTicks = RageConfig.FULL_FLASH_DURATION_SECONDS * 20.0f;
                float elapsed = (minecraft.level.getGameTime() - fullFlashStartTick) + delta;
                if (elapsed >= durationTicks) {
                    fullFlashStartTick = -1;
                } else {
                    float progress = elapsed / durationTicks;
                    int span = RageConfig.FULL_FLASH_END_FRAME - RageConfig.FULL_FLASH_START_FRAME;
                    int frame = RageConfig.FULL_FLASH_START_FRAME + (int) (progress * span);
                    return clampFrame(frame, RageConfig.FULL_FLASH_START_FRAME, RageConfig.FULL_FLASH_END_FRAME);
                }
            }
            return RageConfig.ACTIVE_FRAME;
        }

        // Normal fill ramp: rage 0..100 -> frames 0..FILL_MAX_FRAME.
        int frame = (int) ((displayRage / RageConfig.MAX_RAGE) * RageConfig.FILL_MAX_FRAME);
        return clampFrame(frame, 0, RageConfig.FILL_MAX_FRAME);
    }

    private static int clampFrame(int frame, int min, int max) {
        return Math.max(min, Math.min(max, frame));
    }
}
