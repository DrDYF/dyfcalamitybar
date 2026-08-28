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
 * Client-side adrenaline meter state and HUD rendering. The animation logic is
 * identical to {@link RageHud} (fill ramp 0-40, full-flash 40-49, hold frame 50)
 * but the textures come from {@code textures/ui/adrenaline_meter/} and the meter
 * is drawn to the right of the rage meter.
 *
 * <p>{@link #displayAdrenaline} lags behind the server-authoritative
 * {@link #targetAdrenaline} on the way down so that activating Adrenaline Mode
 * drains the bar smoothly instead of snapping it to zero.
 */
public final class AdrenalineHud {
    private AdrenalineHud() {
    }

    private static final ResourceLocation[] FRAMES = new ResourceLocation[51];
    private static final Random RANDOM = new Random();

    static {
        for (int i = 0; i < FRAMES.length; i++) {
            FRAMES[i] = ResourceLocation.fromNamespaceAndPath(
                DYFCalamityBar.MOD_ID,
                String.format("textures/ui/adrenaline_meter/adrenaline_meter_%04d.png", i)
            );
        }
    }

    /** Server-authoritative adrenaline value (0-maxAdrenaline). */
    private static float targetAdrenaline;
    /** Smoothly-animated value actually drawn (0-maxAdrenaline). */
    private static float displayAdrenaline;
    /** Whether the meter is currently considered full (drives the flash trigger). */
    private static boolean wasFull;
    /** Whether the activation drain animation is running (only activates on use, not on damage). */
    private static boolean draining;
    /** Game time (ticks) at which the full-flash began, or -1 when not playing. */
    private static long fullFlashStartTick = -1;
    /** Game time (ticks) at which the activation shake ends, or -1 when idle. */
    private static long shakeEndTick = -1;

    public static void setAdrenaline(float value) {
        targetAdrenaline = Math.max(0.0f, Math.min(RageConfig.maxAdrenaline, value));
    }

    public static float getAdrenaline() {
        return targetAdrenaline;
    }

    public static boolean isFull() {
        return targetAdrenaline >= RageConfig.maxAdrenaline - 1.0f;
    }

    /** Plays the sound cue associated with a server-sent event. */
    public static void handleEvent(byte event) {
        Minecraft minecraft = Minecraft.getInstance();
        switch (event) {
            case ModNetworking.ADRENALINE_EVENT_FULL ->
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.ADRENALINE_FULL.get(), 1.0f));
            case ModNetworking.ADRENALINE_EVENT_ACTIVATE -> {
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.ADRENALINE_ACTIVE.get(), 1.0f));
                startShake(minecraft);
                draining = true;
                targetAdrenaline = 0.0f;
            }
            case ModNetworking.ADRENALINE_EVENT_LOSS ->
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(ModSounds.ADRENALINE_LOSS.get(), 1.0f));
            default -> {
            }
        }
    }

    private static void startShake(Minecraft minecraft) {
        if (minecraft.level != null) {
            shakeEndTick = minecraft.level.getGameTime()
                + (long) (RageConfig.adrenalineModeDurationSeconds * 20);
        }
    }

    /** Called once per client tick to advance animations. */
    public static void tick() {
        // Only the activation event drains the bar smoothly (over the buff duration).
        // Taking damage clears it instantly, and filling snaps up to the server value.
        if (draining && displayAdrenaline > targetAdrenaline) {
            float step = RageConfig.adrenalineDrainRate() / 20.0f;
            displayAdrenaline = Math.max(targetAdrenaline, displayAdrenaline - step);
            if (displayAdrenaline <= targetAdrenaline) {
                draining = false;
            }
        } else {
            draining = false;
            displayAdrenaline = targetAdrenaline;
        }

        // Trigger the full-flash on the rising edge of "full".
        boolean full = targetAdrenaline >= RageConfig.maxAdrenaline - 1.0f;
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
                float progress = remaining / (RageConfig.adrenalineModeDurationSeconds * 20.0f);
                float magnitude = RageConfig.adrenalineShakeMagnitude * progress;
                shakeX = Math.round((RANDOM.nextFloat() * 2.0f - 1.0f) * magnitude);
                shakeY = Math.round((RANDOM.nextFloat() * 2.0f - 1.0f) * magnitude);
            }
        }

        int x = RageConfig.adrenalineMeterX + shakeX;
        int y = graphics.guiHeight() - RageConfig.adrenalineMeterHeight - RageConfig.adrenalineMeterY + shakeY;

        graphics.blit(
            FRAMES[frame],
            x,
            y,
            0.0f,
            0.0f,
            RageConfig.adrenalineMeterWidth,
            RageConfig.adrenalineMeterHeight,
            RageConfig.adrenalineMeterWidth,
            RageConfig.adrenalineMeterHeight
        );
    }

    private static int computeFrame(Minecraft minecraft, float delta) {
        boolean full = targetAdrenaline >= RageConfig.maxAdrenaline - 1.0f;

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

        // Normal fill ramp: adrenaline 0..max -> frames 0..FILL_MAX_FRAME.
        int frame = (int) ((displayAdrenaline / RageConfig.maxAdrenaline) * RageConfig.FILL_MAX_FRAME);
        return clampFrame(frame, 0, RageConfig.FILL_MAX_FRAME);
    }

    private static int clampFrame(int frame, int min, int max) {
        return Math.max(min, Math.min(max, frame));
    }
}