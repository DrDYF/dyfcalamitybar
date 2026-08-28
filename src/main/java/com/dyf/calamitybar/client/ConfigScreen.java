package com.dyf.calamitybar.client;

import com.dyf.calamitybar.RageConfig;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Checkbox;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * In-game configuration panel. Opens with the U key (rebindable). It mirrors
 * the config file: two tabs — Server (服务端, gameplay) and Client (客户端, UI) —
 * and under each of them an independent Rage and Adrenaline group. "Save"
 * writes the values back to {@link RageConfig} and the config file, and they
 * take effect immediately because the rest of the mod reads the fields live.
 *
 * <p>The field list is scrollable (mouse wheel) like a web page: content is
 * clipped to the area between the tabs and the bottom buttons, so scrolled-off
 * rows never overlap the header or the Save/Done buttons.
 *
 * <p>All user-facing text is localized via translation keys.
 */
public final class ConfigScreen extends Screen {
    private static final String PREFIX = "screen.dyfcalamitybar.";

    private static final int TAB_SERVER = 0;
    private static final int TAB_CLIENT = 1;

    private final Screen parent;
    private int tab = TAB_SERVER;

    // Server > Rage (gameplay) fields.
    private EditBox detectionRange;
    private EditBox fillMultiplier;
    private EditBox fillDenominator;
    private EditBox bossMultiplier;
    private EditBox decayRate;
    private EditBox combatDelay;
    private EditBox buffDuration;
    private EditBox rageDamageMultiplier;

    // Server > Adrenaline (gameplay) fields.
    private EditBox adrenalineRange;
    private EditBox maxAdrenaline;
    private EditBox adrenalineFill;
    private EditBox chargePause;
    private EditBox fullDamageMultiplier;
    private EditBox adrenalineDuration;
    private EditBox adrenalineDamageMultiplier;
    private Checkbox clearOnHurt;

    // Client > Rage (UI) fields.
    private EditBox meterX;
    private EditBox meterY;
    private EditBox meterWidth;
    private EditBox meterHeight;
    private EditBox shakeMagnitude;

    // Client > Adrenaline (UI) fields.
    private EditBox adrenalineMeterX;
    private EditBox adrenalineMeterY;
    private EditBox adrenalineMeterWidth;
    private EditBox adrenalineMeterHeight;
    private EditBox adrenalineShakeMagnitude;

    private Button serverTabButton;
    private Button clientTabButton;

    private List<AbstractWidget> serverFields;
    private List<AbstractWidget> clientFields;

    private static final Predicate<String> FLOAT_FILTER = s -> s.isEmpty() || s.matches("-?\\d*\\.?\\d*");
    private static final Predicate<String> INT_FILTER = s -> s.isEmpty() || s.matches("-?\\d*");

    // Fixed (unscrolled) base Y positions per tab.
    private static final int GROUP_Y = 60;
    private static final int RAGE_FIELD_0_Y = 76;
    private static final int ADRENALINE_GROUP_Y = 252;
    private static final int ADRENALINE_FIELD_0_Y = 268;
    private static final int CLIENT_ADRENALINE_GROUP_Y = 186;
    private static final int CLIENT_ADRENALINE_FIELD_0_Y = 202;

    private static final int SERVER_CONTENT_BOTTOM = ADRENALINE_FIELD_0_Y + 7 * 22 + 18;
    private static final int CLIENT_CONTENT_BOTTOM = CLIENT_ADRENALINE_FIELD_0_Y + 4 * 22 + 18;

    private static final int VIEWPORT_TOP = 56;
    private static final int VIEWPORT_BOTTOM_MARGIN = 40;

    /** How many pixels the scrollable content has been shifted upward. */
    private double scroll;
    private final Map<AbstractWidget, Integer> baseYs = new LinkedHashMap<>();

    public ConfigScreen(Screen parent) {
        super(Component.translatable(PREFIX + "config"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int left = (this.width - 360) / 2;
        int editX = left + 150;
        int editW = 202;
        int labelX = left + 6;

        serverTabButton = Button.builder(Component.translatable(PREFIX + "tab.server"), b -> setTab(TAB_SERVER))
            .bounds(left, 34, 176, 20).build();
        clientTabButton = Button.builder(Component.translatable(PREFIX + "tab.client"), b -> setTab(TAB_CLIENT))
            .bounds(left + 184, 34, 176, 20).build();
        addRenderableWidget(serverTabButton);
        addRenderableWidget(clientTabButton);

        // Server > Rage.
        int y = RAGE_FIELD_0_Y;
        detectionRange = field(editX, y, editW, str(RageConfig.detectionRangeBlocks), FLOAT_FILTER);
        fillMultiplier = field(editX, y += 22, editW, str(RageConfig.fillMultiplier), FLOAT_FILTER);
        fillDenominator = field(editX, y += 22, editW, str(RageConfig.fillDenominatorSeconds), FLOAT_FILTER);
        bossMultiplier = field(editX, y += 22, editW, str(RageConfig.bossFillMultiplier), FLOAT_FILTER);
        decayRate = field(editX, y += 22, editW, str(RageConfig.decayPerSecond), FLOAT_FILTER);
        combatDelay = field(editX, y += 22, editW, str(RageConfig.outOfCombatDelayTicks), INT_FILTER);
        buffDuration = field(editX, y += 22, editW, str(RageConfig.rageModeDurationSeconds), FLOAT_FILTER);
        rageDamageMultiplier = field(editX, y += 22, editW, str(RageConfig.rageDamageMultiplier), FLOAT_FILTER);

        // Server > Adrenaline.
        y = ADRENALINE_FIELD_0_Y;
        adrenalineRange = field(editX, y, editW, str(RageConfig.adrenalineDetectionRangeBlocks), FLOAT_FILTER);
        maxAdrenaline = field(editX, y += 22, editW, str(RageConfig.maxAdrenaline), FLOAT_FILTER);
        adrenalineFill = field(editX, y += 22, editW, str(RageConfig.adrenalineFillPerSecond), FLOAT_FILTER);
        chargePause = field(editX, y += 22, editW, str(RageConfig.adrenalineChargePauseTicks), INT_FILTER);
        clearOnHurt = checkbox(labelX, y + 22, RageConfig.adrenalineClearOnHurt);
        fullDamageMultiplier = field(editX, y += 44, editW, str(RageConfig.adrenalineFullDamageMultiplier), FLOAT_FILTER);
        adrenalineDuration = field(editX, y += 22, editW, str(RageConfig.adrenalineModeDurationSeconds), FLOAT_FILTER);
        adrenalineDamageMultiplier = field(editX, y += 22, editW, str(RageConfig.adrenalineDamageMultiplier), FLOAT_FILTER);

        // Client > Rage (UI).
        y = RAGE_FIELD_0_Y;
        meterX = field(editX, y, editW, str(RageConfig.meterX), INT_FILTER);
        meterY = field(editX, y += 22, editW, str(RageConfig.meterY), INT_FILTER);
        meterWidth = field(editX, y += 22, editW, str(RageConfig.meterWidth), INT_FILTER);
        meterHeight = field(editX, y += 22, editW, str(RageConfig.meterHeight), INT_FILTER);
        shakeMagnitude = field(editX, y += 22, editW, str(RageConfig.shakeMagnitude), FLOAT_FILTER);

        // Client > Adrenaline (UI).
        y = CLIENT_ADRENALINE_FIELD_0_Y;
        adrenalineMeterX = field(editX, y, editW, str(RageConfig.adrenalineMeterX), INT_FILTER);
        adrenalineMeterY = field(editX, y += 22, editW, str(RageConfig.adrenalineMeterY), INT_FILTER);
        adrenalineMeterWidth = field(editX, y += 22, editW, str(RageConfig.adrenalineMeterWidth), INT_FILTER);
        adrenalineMeterHeight = field(editX, y += 22, editW, str(RageConfig.adrenalineMeterHeight), INT_FILTER);
        adrenalineShakeMagnitude = field(editX, y += 22, editW, str(RageConfig.adrenalineShakeMagnitude), FLOAT_FILTER);

        serverFields = List.of(
            detectionRange, fillMultiplier, fillDenominator, bossMultiplier, decayRate, combatDelay,
            buffDuration, rageDamageMultiplier, adrenalineRange, maxAdrenaline, adrenalineFill,
            chargePause, clearOnHurt, fullDamageMultiplier, adrenalineDuration, adrenalineDamageMultiplier
        );
        clientFields = List.of(
            meterX, meterY, meterWidth, meterHeight, shakeMagnitude,
            adrenalineMeterX, adrenalineMeterY, adrenalineMeterWidth, adrenalineMeterHeight,
            adrenalineShakeMagnitude
        );

        int bottomY = this.height - 32;
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "save"), b -> applyAndSave())
            .bounds(left, bottomY, 176, 20).build());
        addRenderableWidget(Button.builder(Component.translatable(PREFIX + "done"), b -> this.onClose())
            .bounds(left + 184, bottomY, 176, 20).build());

        applyScroll();
    }

    /** Fields are input-only children; they are rendered manually inside the scissor clip region. */
    private EditBox field(int x, int y, int w, String value, Predicate<String> filter) {
        EditBox box = new EditBox(this.font, x, y, w, 18, Component.empty());
        box.setMaxLength(16);
        box.setFilter(filter);
        box.setValue(value);
        baseYs.put(box, y);
        addWidget(box);
        return box;
    }

    /** A self-labelled toggle, also rendered manually inside the scissor clip region. */
    private Checkbox checkbox(int x, int y, boolean selected) {
        Checkbox box = new Checkbox(x, y, 20, 20,
            Component.translatable(PREFIX + "field.adrenaline_clear_on_hurt"), selected);
        baseYs.put(box, y);
        addWidget(box);
        return box;
    }

    private void setTab(int tab) {
        this.tab = tab;
        applyScroll();
    }

    private int viewportBottom() {
        return this.height - VIEWPORT_BOTTOM_MARGIN;
    }

    private int contentBottom() {
        return tab == TAB_SERVER ? SERVER_CONTENT_BOTTOM : CLIENT_CONTENT_BOTTOM;
    }

    private double maxScroll() {
        return Math.max(0, contentBottom() - viewportBottom());
    }

    /** Repositions every field and hides the ones scrolled outside the clip region. */
    private void applyScroll() {
        this.scroll = Math.max(0, Math.min(this.scroll, maxScroll()));
        int shift = (int) scroll;
        int vBottom = viewportBottom();
        boolean server = tab == TAB_SERVER;
        for (Map.Entry<AbstractWidget, Integer> e : baseYs.entrySet()) {
            AbstractWidget box = e.getKey();
            int y = e.getValue() - shift;
            box.setY(y);
            boolean onTab = server ? serverFields.contains(box) : clientFields.contains(box);
            boolean inView = y + 18 > VIEWPORT_TOP && y < vBottom;
            box.visible = onTab && inView;
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double amount) {
        this.scroll -= amount * 12.0;
        applyScroll();
        return true;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        super.render(graphics, mouseX, mouseY, delta);

        graphics.drawCenteredString(this.font, this.title, this.width / 2, 12, 0xFFFFFF);

        int left = (this.width - 360) / 2;
        int labelX = left + 6;
        int s = (int) scroll;

        graphics.enableScissor(left - 8, VIEWPORT_TOP, left + 368, viewportBottom());
        // Render the (visible) input widgets first, then their labels.
        for (AbstractWidget box : baseYs.keySet()) {
            if (box.visible) {
                box.render(graphics, mouseX, mouseY, delta);
            }
        }

        if (tab == TAB_SERVER) {
            group(graphics, labelX, GROUP_Y - s, "tab.rage");
            int y = RAGE_FIELD_0_Y + 5 - s;
            label(graphics, labelX, y, "field.detection_range");
            label(graphics, labelX, y += 22, "field.fill_multiplier");
            label(graphics, labelX, y += 22, "field.fill_denominator");
            label(graphics, labelX, y += 22, "field.boss_multiplier");
            label(graphics, labelX, y += 22, "field.decay_rate");
            label(graphics, labelX, y += 22, "field.combat_delay");
            label(graphics, labelX, y += 22, "field.buff_duration");
            label(graphics, labelX, y += 22, "field.rage_damage_multiplier");

            group(graphics, labelX, ADRENALINE_GROUP_Y - s, "tab.adrenaline");
            y = ADRENALINE_FIELD_0_Y + 5 - s;
            label(graphics, labelX, y, "field.adrenaline_range");
            label(graphics, labelX, y += 22, "field.max_adrenaline");
            label(graphics, labelX, y += 22, "field.adrenaline_fill");
            label(graphics, labelX, y += 22, "field.charge_pause");
            y += 22; // 受伤清空 checkbox renders its own label
            label(graphics, labelX, y += 22, "field.full_damage_multiplier");
            label(graphics, labelX, y += 22, "field.adrenaline_duration");
            label(graphics, labelX, y += 22, "field.adrenaline_damage_multiplier");
        } else {
            group(graphics, labelX, GROUP_Y - s, "tab.rage");
            int y = RAGE_FIELD_0_Y + 5 - s;
            label(graphics, labelX, y, "field.meter_x");
            label(graphics, labelX, y += 22, "field.meter_y");
            label(graphics, labelX, y += 22, "field.meter_width");
            label(graphics, labelX, y += 22, "field.meter_height");
            label(graphics, labelX, y += 22, "field.shake_magnitude");

            group(graphics, labelX, CLIENT_ADRENALINE_GROUP_Y - s, "tab.adrenaline");
            y = CLIENT_ADRENALINE_FIELD_0_Y + 5 - s;
            label(graphics, labelX, y, "field.adrenaline_meter_x");
            label(graphics, labelX, y += 22, "field.adrenaline_meter_y");
            label(graphics, labelX, y += 22, "field.adrenaline_meter_width");
            label(graphics, labelX, y += 22, "field.adrenaline_meter_height");
            label(graphics, labelX, y += 22, "field.adrenaline_shake_magnitude");
        }
        graphics.disableScissor();
    }

    private void group(GuiGraphics graphics, int x, int y, String key) {
        graphics.drawString(this.font, Component.translatable(PREFIX + key), x, y, 0xFFD45A);
    }

    private void label(GuiGraphics graphics, int x, int y, String key) {
        graphics.drawString(this.font, Component.translatable(PREFIX + key), x, y, 0xFFFFFF);
    }

    private void applyAndSave() {
        RageConfig.detectionRangeBlocks = parseDouble(detectionRange.getValue(), RageConfig.detectionRangeBlocks);
        RageConfig.fillMultiplier = parseDouble(fillMultiplier.getValue(), RageConfig.fillMultiplier);
        RageConfig.fillDenominatorSeconds = parseDouble(fillDenominator.getValue(), RageConfig.fillDenominatorSeconds);
        RageConfig.bossFillMultiplier = parseFloat(bossMultiplier.getValue(), RageConfig.bossFillMultiplier);
        RageConfig.decayPerSecond = parseFloat(decayRate.getValue(), RageConfig.decayPerSecond);
        RageConfig.outOfCombatDelayTicks = parseInt(combatDelay.getValue(), RageConfig.outOfCombatDelayTicks);
        RageConfig.rageModeDurationSeconds = parseFloat(buffDuration.getValue(), RageConfig.rageModeDurationSeconds);
        RageConfig.rageDamageMultiplier = parseFloat(rageDamageMultiplier.getValue(), RageConfig.rageDamageMultiplier);

        RageConfig.adrenalineDetectionRangeBlocks = parseDouble(adrenalineRange.getValue(), RageConfig.adrenalineDetectionRangeBlocks);
        RageConfig.maxAdrenaline = parseFloat(maxAdrenaline.getValue(), RageConfig.maxAdrenaline);
        RageConfig.adrenalineFillPerSecond = parseFloat(adrenalineFill.getValue(), RageConfig.adrenalineFillPerSecond);
        RageConfig.adrenalineChargePauseTicks = parseInt(chargePause.getValue(), RageConfig.adrenalineChargePauseTicks);
        RageConfig.adrenalineClearOnHurt = clearOnHurt.selected();
        RageConfig.adrenalineFullDamageMultiplier = parseFloat(fullDamageMultiplier.getValue(), RageConfig.adrenalineFullDamageMultiplier);
        RageConfig.adrenalineModeDurationSeconds = parseFloat(adrenalineDuration.getValue(), RageConfig.adrenalineModeDurationSeconds);
        RageConfig.adrenalineDamageMultiplier = parseFloat(adrenalineDamageMultiplier.getValue(), RageConfig.adrenalineDamageMultiplier);

        RageConfig.meterX = parseInt(meterX.getValue(), RageConfig.meterX);
        RageConfig.meterY = parseInt(meterY.getValue(), RageConfig.meterY);
        RageConfig.meterWidth = parseInt(meterWidth.getValue(), RageConfig.meterWidth);
        RageConfig.meterHeight = parseInt(meterHeight.getValue(), RageConfig.meterHeight);
        RageConfig.shakeMagnitude = parseFloat(shakeMagnitude.getValue(), RageConfig.shakeMagnitude);

        RageConfig.adrenalineMeterX = parseInt(adrenalineMeterX.getValue(), RageConfig.adrenalineMeterX);
        RageConfig.adrenalineMeterY = parseInt(adrenalineMeterY.getValue(), RageConfig.adrenalineMeterY);
        RageConfig.adrenalineMeterWidth = parseInt(adrenalineMeterWidth.getValue(), RageConfig.adrenalineMeterWidth);
        RageConfig.adrenalineMeterHeight = parseInt(adrenalineMeterHeight.getValue(), RageConfig.adrenalineMeterHeight);
        RageConfig.adrenalineShakeMagnitude = parseFloat(adrenalineShakeMagnitude.getValue(), RageConfig.adrenalineShakeMagnitude);
        RageConfig.save();
    }

    @Override
    public void onClose() {
        if (this.minecraft != null) {
            this.minecraft.setScreen(this.parent);
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static String str(double v) {
        return Double.toString(v);
    }

    private static String str(float v) {
        return Float.toString(v);
    }

    private static String str(int v) {
        return Integer.toString(v);
    }

    private static double parseDouble(String s, double def) {
        try {
            return Double.parseDouble(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static float parseFloat(String s, float def) {
        try {
            return Float.parseFloat(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }

    private static int parseInt(String s, int def) {
        try {
            return Integer.parseInt(s.trim());
        } catch (NumberFormatException e) {
            return def;
        }
    }
}