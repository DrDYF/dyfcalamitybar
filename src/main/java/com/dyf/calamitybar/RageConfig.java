package com.dyf.calamitybar;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runtime configuration, loaded from {@code config/dyfcalamitybar.json}. The
 * file is created with an annotated (JSON5-style {@code //} comment) bilingual
 * template on first launch; edit it directly, or press U in game to open the
 * in-game panel and change values live (saved immediately).
 *
 * <p>The file is organised into two categories — {@code server} (gameplay) and
 * {@code client} (UI) — and under each of those, independent {@code rage} and
 * {@code adrenaline} sub-sections.
 *
 * <p>Values are mutable static fields so the rest of the mod can read them with
 * minimal changes, while {@link #load()} manages the file. Older flat and
 * three-section file formats are still read (via fallbacks) and automatically
 * migrated to the current format. Comments are stripped before parsing and
 * missing keys fall back to defaults, so a hand-edited file never breaks the mod.
 */
public final class RageConfig {
    private RageConfig() {
    }

    private static final Logger LOGGER = LoggerFactory.getLogger("dyfcalamitybar-config");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH =
        FabricLoader.getInstance().getConfigDir().resolve("dyfcalamitybar.json");

    /** Rage meter maximum (0-100). Not configurable; the HUD frames assume this scale. */
    public static final float MAX_RAGE = 100.0f;

    /** Full-flash animation length when the meter just becomes full (seconds). Fixed. */
    public static final float FULL_FLASH_DURATION_SECONDS = 0.7f;

    // ---- Server > Rage (怒气模式机制) ----

    /** 判定范围（格)：有敌怪在此距离内才充能、才算“战斗中”。默认 10。 */
    public static double detectionRangeBlocks = 10.0;

    /** 增加比率：充能速度倍率。1.0 = 原版速度，2.0 = 快一倍。 */
    public static double fillMultiplier = 1.0;

    /** 充能公式分母（秒）：越大充得越慢。 */
    public static double fillDenominatorSeconds = 45.0;

    /** Boss 战充能倍率。 */
    public static float bossFillMultiplier = 3.0f;

    /** 衰减比率：每秒掉多少怒气（3.33 ≈ 满条 30 秒清空）。 */
    public static float decayPerSecond = MAX_RAGE / 30.0f;

    /** 脱战宽限（tick，20 = 1 秒）：离开敌人这么久后才开始衰减。 */
    public static int outOfCombatDelayTicks = 20 * 2;

    /** 给予的怒气模式 buff 时长（秒）。同时决定激活动画与抖动的时长。 */
    public static float rageModeDurationSeconds = 5.0f;

    /** 伤害比例：怒气模式下的伤害倍率（1.35 = +35%，与护甲/饰品乘算）。 */
    public static float rageDamageMultiplier = 1.35f;

    // ---- Server > Adrenaline (肾上腺素机制) ----

    /** Boss 判定范围（格）：有 Boss 在此距离内才充能，否则按怒气公式流失。默认 75。 */
    public static double adrenalineDetectionRangeBlocks = 75.0;

    /** 肾上腺素条最大值（0-10000）。 */
    public static float maxAdrenaline = 10000.0f;

    /** 每秒充能点数（1000/3 ≈ 333.33，30 秒充满）。 */
    public static float adrenalineFillPerSecond = 1000.0f / 3.0f;

    /** 受击后充能暂停时长（tick，20 = 1 秒）。 */
    public static int adrenalineChargePauseTicks = 20;

    /** 受伤时是否清空肾上腺素条（true=清空，false=受伤不清空）。默认 true。 */
    public static boolean adrenalineClearOnHurt = true;

    /** 满条受击伤害倍率（0.5 = 减半），同时肾上腺素清零。 */
    public static float adrenalineFullDamageMultiplier = 0.5f;

    /** 肾上腺素模式 buff 时长（秒）。同时决定激活动画与抖动的时长。 */
    public static float adrenalineModeDurationSeconds = 5.0f;

    /** 伤害比例：肾上腺素模式的伤害倍率（2.5 = +150%，在所有加成之后乘算）。 */
    public static float adrenalineDamageMultiplier = 2.5f;

    // ---- Client > Rage (怒气条 UI) ----

    /** UI 位置：怒气条左上角 X（距屏幕左边缘，像素）。 */
    public static int meterX = 5;

    /** UI 位置：怒气条底边距屏幕底部的距离（像素）。 */
    public static int meterY = 10;

    public static int meterWidth = 69;
    public static int meterHeight = 18;

    /** 激活抖动峰值像素偏移（抖动时长 = buff 时长）。 */
    public static float shakeMagnitude = 3.0f;

    // ---- Client > Adrenaline (肾上腺素条 UI) ----

    /**
     * 肾上腺素条左上角 X（像素）。默认 78 = 怒气条 X(5) + 宽度(69) + 4，即两 UI 默认间隔 4。
     */
    public static int adrenalineMeterX = 60;

    /** 肾上腺素条底边距屏幕底部的距离（像素）。 */
    public static int adrenalineMeterY = 0;

    /** 肾上腺素条贴图宽度（像素）。默认 77x34。 */
    public static int adrenalineMeterWidth = 77;

    /** 肾上腺素条贴图高度（像素）。 */
    public static int adrenalineMeterHeight = 34;

    /** 肾上腺素激活抖动峰值像素偏移（抖动时长 = buff 时长）。 */
    public static float adrenalineShakeMagnitude = 3.0f;

    // ---- Frame layout (tied to the 51-frame texture sheet; not configurable) ----

    public static final int FULL_FLASH_START_FRAME = 40;
    public static final int FULL_FLASH_END_FRAME = 49;
    public static final int FILL_MAX_FRAME = 40;
    public static final int ACTIVE_FRAME = 50;

    /** The Rage Mode buff length in ticks, derived from {@link #rageModeDurationSeconds}. */
    public static int rageModeDurationTicks() {
        return Math.max(1, Math.round(rageModeDurationSeconds * 20.0f));
    }

    /** The Adrenaline Mode buff length in ticks, derived from {@link #adrenalineModeDurationSeconds}. */
    public static int adrenalineModeDurationTicks() {
        return Math.max(1, Math.round(adrenalineModeDurationSeconds * 20.0f));
    }

    /**
     * Rage HUD drain speed (rage points/second), derived so the activation
     * animation empties the bar over exactly the buff duration.
     */
    public static float drainRate() {
        return MAX_RAGE / Math.max(0.05f, rageModeDurationSeconds);
    }

    /**
     * Adrenaline HUD drain speed (points/second), derived so the activation
     * animation empties the bar over exactly the buff duration.
     */
    public static float adrenalineDrainRate() {
        return maxAdrenaline / Math.max(0.05f, adrenalineModeDurationSeconds);
    }

    /**
     * Adrenaline decay speed (points/second), reusing rage's decay formula
     * proportionally: the same fraction of the bar per second (default full bar
     * empties in 30 seconds, matching rage's {@link #decayPerSecond}).
     */
    public static float adrenalineDecayPerSecond() {
        return (decayPerSecond / MAX_RAGE) * maxAdrenaline;
    }

    /**
     * Writes the current values to the config file with bilingual comments,
     * organised into server / client categories, each with rage and adrenaline
     * sub-sections. Used both to create the default file and persist edits.
     */
    public static void save() {
        StringBuilder sb = new StringBuilder(4096);
        sb.append("// DYFCalamityBar 怒气 & 肾上腺素配置文件 | Rage & Adrenaline configuration\n");
        sb.append("// 服务端（游戏机制）与客户端（UI）两大类，其下各有怒气模式与肾上腺素两节，互不影响。\n");
        sb.append("// Server (gameplay) and Client (UI) categories, each with Rage and Adrenaline.\n");
        sb.append("// 游戏内按 U 打开控制面板实时修改；点「保存」立即生效。\n");
        sb.append("// Press U in game to edit live; click Save to apply immediately.\n");
        sb.append("{\n");

        // ---- Server > Rage ----
        sb.append("  // 服务端 | Server\n");
        sb.append("  \"server\": {\n");
        sb.append("    // 怒气模式 | Rage\n");
        sb.append("    \"rage\": {\n");
        field(sb, "      ", "detectionRangeBlocks", Double.toString(detectionRangeBlocks),
            "判定范围（格）：有敌怪在此距离内才充能、才算\"战斗中\"。",
            "Detection range (blocks): enemies within this distance count as \"in combat\".");
        field(sb, "      ", "fillMultiplier", Double.toString(fillMultiplier),
            "增加比率：充能速度倍率（1.0=原速，2.0=快一倍）。",
            "Fill multiplier (1.0 = normal, 2.0 = 2x faster).");
        field(sb, "      ", "fillDenominatorSeconds", Double.toString(fillDenominatorSeconds),
            "充能公式分母（秒）：越大充得越慢。",
            "Fill denominator (seconds): larger = slower charge.");
        field(sb, "      ", "bossFillMultiplier", Float.toString(bossFillMultiplier),
            "Boss 战充能倍率。",
            "Boss-fight fill multiplier.");
        field(sb, "      ", "decayPerSecond", Float.toString(decayPerSecond),
            "衰减比率：每秒掉多少怒气（3.33 ≈ 满条 30 秒清空）。",
            "Decay rate (rage/sec): 3.33 ≈ full bar empties in 30 seconds.");
        field(sb, "      ", "outOfCombatDelayTicks", Integer.toString(outOfCombatDelayTicks),
            "脱战宽限（tick，20=1 秒）：离开敌人这么久后才开始衰减。",
            "Out-of-combat grace (ticks, 20=1s): decay starts after this long.");
        field(sb, "      ", "rageModeDurationSeconds", Float.toString(rageModeDurationSeconds),
            "怒气模式 buff 时长（秒）：同时决定激活动画与抖动时长。",
            "Rage Mode buff duration (seconds): also drives the activation animation and shake length.");
        fieldLast(sb, "      ", "rageDamageMultiplier", Float.toString(rageDamageMultiplier),
            "伤害比例：怒气模式伤害倍率（1.35=+35%，乘算）。",
            "Damage multiplier during Rage Mode (1.35=+35%, multiplicative).");
        sb.append("    },\n");

        // ---- Server > Adrenaline ----
        sb.append("    // 肾上腺素 | Adrenaline\n");
        sb.append("    \"adrenaline\": {\n");
        field(sb, "      ", "detectionRangeBlocks", Double.toString(adrenalineDetectionRangeBlocks),
            "Boss 判定范围（格）：有 Boss 在此距离内才充能，否则按怒气公式流失。默认 75。",
            "Boss detection range (blocks): charge only while a boss is within this range, else decay like rage. Default 75.");
        field(sb, "      ", "maxAdrenaline", Float.toString(maxAdrenaline),
            "肾上腺素条最大值（0-10000）。",
            "Adrenaline bar maximum (0-10000).");
        field(sb, "      ", "fillPerSecond", Float.toString(adrenalineFillPerSecond),
            "每秒充能点数（333.33 ≈ 1000/3，30 秒充满）。",
            "Fill rate (points/sec): 333.33 ≈ 1000/3, full bar in 30 seconds.");
        field(sb, "      ", "chargePauseTicks", Integer.toString(adrenalineChargePauseTicks),
            "受击后充能暂停时长（tick，20=1 秒）。",
            "Charge pause after taking damage (ticks, 20=1s).");
        field(sb, "      ", "clearOnHurt", Boolean.toString(adrenalineClearOnHurt),
            "受伤时是否清空肾上腺素条（true=清空，false=不清空）。",
            "Whether taking damage clears the adrenaline bar (true=clear, false=keep).");
        field(sb, "      ", "fullDamageMultiplier", Float.toString(adrenalineFullDamageMultiplier),
            "满条受击伤害倍率（0.5=减半），同时肾上腺素清零。",
            "Damage multiplier when hit with a full bar (0.5=halved); the bar also resets to 0.");
        field(sb, "      ", "modeDurationSeconds", Float.toString(adrenalineModeDurationSeconds),
            "肾上腺素模式 buff 时长（秒）：同时决定激活动画与抖动时长。",
            "Adrenaline Mode buff duration (seconds): also drives the activation animation and shake length.");
        fieldLast(sb, "      ", "damageMultiplier", Float.toString(adrenalineDamageMultiplier),
            "伤害倍率：肾上腺素模式下伤害 +150%（2.5 倍），在所有其他加成之后乘算。",
            "Damage multiplier during Adrenaline Mode (+150% → x2.5), applied after all other bonuses.");
        sb.append("    }\n");
        sb.append("  },\n");

        // ---- Client > Rage ----
        sb.append("  // 客户端 UI | Client UI\n");
        sb.append("  \"client\": {\n");
        sb.append("    // 怒气模式 UI | Rage UI\n");
        sb.append("    \"rage\": {\n");
        field(sb, "      ", "meterX", Integer.toString(meterX),
            "UI 位置：怒气条左上角 X（距屏幕左边缘，像素）。",
            "UI position: rage meter top-left X (pixels from left edge).");
        field(sb, "      ", "meterY", Integer.toString(meterY),
            "UI 位置：怒气条底边距屏幕底部（像素）。",
            "UI position: rage meter bottom offset from screen bottom (pixels).");
        field(sb, "      ", "meterWidth", Integer.toString(meterWidth),
            "怒气条贴图宽度（像素）。",
            "Rage meter texture width (pixels).");
        field(sb, "      ", "meterHeight", Integer.toString(meterHeight),
            "怒气条贴图高度（像素）。",
            "Rage meter texture height (pixels).");
        fieldLast(sb, "      ", "shakeMagnitude", Float.toString(shakeMagnitude),
            "激活抖动幅度：峰值像素偏移（抖动时长 = buff 时长）。",
            "Activation shake magnitude: peak pixel offset (shake length = buff duration).");
        sb.append("    },\n");

        // ---- Client > Adrenaline ----
        sb.append("    // 肾上腺素 UI | Adrenaline UI\n");
        sb.append("    \"adrenaline\": {\n");
        field(sb, "      ", "meterX", Integer.toString(adrenalineMeterX),
            "肾上腺素条左上角 X（像素）：默认 78 = 怒气条 X + 宽 + 4，两 UI 默认间隔 4。",
            "Adrenaline meter top-left X (pixels): default 78 = rage X + width + 4 (4px gap).");
        field(sb, "      ", "meterY", Integer.toString(adrenalineMeterY),
            "肾上腺素条底边距屏幕底部（像素）。",
            "Adrenaline meter bottom offset from screen bottom (pixels).");
        field(sb, "      ", "meterWidth", Integer.toString(adrenalineMeterWidth),
            "肾上腺素条贴图宽度（像素）：默认 77。",
            "Adrenaline meter texture width (pixels): default 77.");
        field(sb, "      ", "meterHeight", Integer.toString(adrenalineMeterHeight),
            "肾上腺素条贴图高度（像素）：默认 34。",
            "Adrenaline meter texture height (pixels): default 34.");
        fieldLast(sb, "      ", "shakeMagnitude", Float.toString(adrenalineShakeMagnitude),
            "肾上腺素激活抖动幅度：峰值像素偏移（抖动时长 = buff 时长）。",
            "Adrenaline activation shake magnitude: peak pixel offset (shake length = buff duration).");
        sb.append("    }\n");
        sb.append("  }\n");

        sb.append("}\n");

        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            Files.writeString(CONFIG_PATH, sb.toString());
        } catch (IOException e) {
            LOGGER.warn("Failed to write {}; keeping in-memory values.", CONFIG_PATH, e);
        }
    }

    private static void field(StringBuilder sb, String pad, String key, String value, String cn, String en) {
        sb.append(pad).append("// ").append(cn).append('\n');
        sb.append(pad).append("// ").append(en).append('\n');
        sb.append(pad).append('"').append(key).append("\": ").append(value).append(",\n\n");
    }

    private static void fieldLast(StringBuilder sb, String pad, String key, String value, String cn, String en) {
        sb.append(pad).append("// ").append(cn).append('\n');
        sb.append(pad).append("// ").append(en).append('\n');
        sb.append(pad).append('"').append(key).append("\": ").append(value).append("\n");
    }

    /** Loads the config file, creating it with defaults if absent. */
    public static void load() {
        if (!Files.exists(CONFIG_PATH)) {
            save();
            return;
        }
        try {
            String json = stripComments(Files.readString(CONFIG_PATH));
            JsonObject root = GSON.fromJson(json, JsonObject.class);
            if (root == null) {
                return;
            }

            JsonObject serverSection = obj(root, "server");
            JsonObject clientSection = obj(root, "client");
            JsonObject sr = obj(serverSection, "rage");
            JsonObject sa = obj(serverSection, "adrenaline");
            JsonObject cr = obj(clientSection, "rage");
            JsonObject ca = obj(clientSection, "adrenaline");
            // Legacy fallbacks: previous format root.rage / root.adrenaline / root.client,
            // and the original flat format (keys directly on root).
            JsonObject oldRage = obj(root, "rage");
            JsonObject oldAdrenaline = obj(root, "adrenaline");
            JsonObject oldClient = obj(root, "client");

            // Server > Rage: current -> legacy section -> legacy flat -> default.
            detectionRangeBlocks = getDouble(sr, "detectionRangeBlocks",
                getDouble(oldRage, "detectionRangeBlocks",
                    getDouble(root, "detectionRangeBlocks", detectionRangeBlocks)));
            fillMultiplier = getDouble(sr, "fillMultiplier",
                getDouble(oldRage, "fillMultiplier",
                    getDouble(root, "fillMultiplier", fillMultiplier)));
            fillDenominatorSeconds = getDouble(sr, "fillDenominatorSeconds",
                getDouble(oldRage, "fillDenominatorSeconds",
                    getDouble(root, "fillDenominatorSeconds", fillDenominatorSeconds)));
            bossFillMultiplier = getFloat(sr, "bossFillMultiplier",
                getFloat(oldRage, "bossFillMultiplier",
                    getFloat(root, "bossFillMultiplier", bossFillMultiplier)));
            decayPerSecond = getFloat(sr, "decayPerSecond",
                getFloat(oldRage, "decayPerSecond",
                    getFloat(root, "decayPerSecond", decayPerSecond)));
            outOfCombatDelayTicks = getInt(sr, "outOfCombatDelayTicks",
                getInt(oldRage, "outOfCombatDelayTicks",
                    getInt(root, "outOfCombatDelayTicks", outOfCombatDelayTicks)));
            rageModeDurationSeconds = getFloat(sr, "rageModeDurationSeconds",
                getFloat(oldRage, "rageModeDurationSeconds",
                    getFloat(root, "rageModeDurationSeconds", rageModeDurationSeconds)));
            rageDamageMultiplier = getFloat(sr, "rageDamageMultiplier",
                getFloat(oldRage, "rageDamageMultiplier",
                    getFloat(root, "rageDamageMultiplier", rageDamageMultiplier)));

            // Server > Adrenaline.
            adrenalineDetectionRangeBlocks = getDouble(sa, "detectionRangeBlocks",
                getDouble(oldAdrenaline, "detectionRangeBlocks",
                    getDouble(root, "adrenalineDetectionRangeBlocks", adrenalineDetectionRangeBlocks)));
            maxAdrenaline = getFloat(sa, "maxAdrenaline",
                getFloat(oldAdrenaline, "maxAdrenaline",
                    getFloat(root, "maxAdrenaline", maxAdrenaline)));
            adrenalineFillPerSecond = getFloat(sa, "fillPerSecond",
                getFloat(oldAdrenaline, "fillPerSecond",
                    getFloat(root, "fillPerSecond", adrenalineFillPerSecond)));
            adrenalineChargePauseTicks = getInt(sa, "chargePauseTicks",
                getInt(oldAdrenaline, "chargePauseTicks",
                    getInt(root, "chargePauseTicks", adrenalineChargePauseTicks)));
            adrenalineClearOnHurt = getBoolean(sa, "clearOnHurt",
                getBoolean(oldAdrenaline, "clearOnHurt",
                    getBoolean(root, "adrenalineClearOnHurt", adrenalineClearOnHurt)));
            adrenalineFullDamageMultiplier = getFloat(sa, "fullDamageMultiplier",
                getFloat(oldAdrenaline, "fullDamageMultiplier",
                    getFloat(root, "fullDamageMultiplier", adrenalineFullDamageMultiplier)));
            adrenalineModeDurationSeconds = getFloat(sa, "modeDurationSeconds",
                getFloat(oldAdrenaline, "modeDurationSeconds",
                    getFloat(root, "modeDurationSeconds", adrenalineModeDurationSeconds)));
            adrenalineDamageMultiplier = getFloat(sa, "damageMultiplier",
                getFloat(oldAdrenaline, "damageMultiplier",
                    getFloat(root, "damageMultiplier", adrenalineDamageMultiplier)));

            // Client > Rage UI.
            meterX = getInt(cr, "meterX",
                getInt(oldClient, "meterX", getInt(root, "meterX", meterX)));
            meterY = getInt(cr, "meterY",
                getInt(oldClient, "meterY", getInt(root, "meterY", meterY)));
            meterWidth = getInt(cr, "meterWidth",
                getInt(oldClient, "meterWidth", getInt(root, "meterWidth", meterWidth)));
            meterHeight = getInt(cr, "meterHeight",
                getInt(oldClient, "meterHeight", getInt(root, "meterHeight", meterHeight)));
            shakeMagnitude = getFloat(cr, "shakeMagnitude",
                getFloat(oldClient, "shakeMagnitude",
                    getFloat(root, "shakeMagnitude", shakeMagnitude)));

            // Client > Adrenaline UI (own position/size; no shared gap variable).
            adrenalineMeterX = getInt(ca, "meterX",
                getInt(root, "adrenalineMeterX", adrenalineMeterX));
            adrenalineMeterY = getInt(ca, "meterY",
                getInt(root, "adrenalineMeterY", adrenalineMeterY));
            adrenalineMeterWidth = getInt(ca, "meterWidth",
                getInt(root, "adrenalineMeterWidth", adrenalineMeterWidth));
            adrenalineMeterHeight = getInt(ca, "meterHeight",
                getInt(root, "adrenalineMeterHeight", adrenalineMeterHeight));
            adrenalineShakeMagnitude = getFloat(ca, "shakeMagnitude",
                getFloat(root, "adrenalineShakeMagnitude", adrenalineShakeMagnitude));

            // Migrate older formats (no top-level "server" category) to the current one.
            if (!root.has("server")) {
                save();
            }
        } catch (IOException | RuntimeException e) {
            LOGGER.warn("Failed to read {}; keeping current values.", CONFIG_PATH, e);
        }
    }

    private static JsonObject obj(JsonObject root, String name) {
        if (root != null && root.has(name) && root.get(name).isJsonObject()) {
            return root.getAsJsonObject(name);
        }
        return null;
    }

    /** Strips {@code //} and {@code /* ... *}{@code /} comments from JSON5 input. */
    private static String stripComments(String json) {
        StringBuilder sb = new StringBuilder(json.length());
        boolean inString = false;
        boolean inLineComment = false;
        boolean inBlockComment = false;
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            char next = i + 1 < json.length() ? json.charAt(i + 1) : '\0';

            if (inLineComment) {
                if (c == '\n') {
                    inLineComment = false;
                    sb.append('\n');
                }
                continue;
            }
            if (inBlockComment) {
                if (c == '*' && next == '/') {
                    inBlockComment = false;
                    i++;
                }
                continue;
            }
            if (inString) {
                sb.append(c);
                if (c == '\\' && i + 1 < json.length()) {
                    sb.append(json.charAt(++i));
                } else if (c == '"') {
                    inString = false;
                }
                continue;
            }
            if (c == '/' && next == '/') {
                inLineComment = true;
                i++;
            } else if (c == '/' && next == '*') {
                inBlockComment = true;
                i++;
            } else {
                sb.append(c);
                if (c == '"') {
                    inString = true;
                }
            }
        }
        return sb.toString();
    }

    private static float getFloat(JsonObject obj, String key, float def) {
        return obj != null && obj.has(key) ? obj.get(key).getAsFloat() : def;
    }

    private static double getDouble(JsonObject obj, String key, double def) {
        return obj != null && obj.has(key) ? obj.get(key).getAsDouble() : def;
    }

    private static int getInt(JsonObject obj, String key, int def) {
        return obj != null && obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    private static boolean getBoolean(JsonObject obj, String key, boolean def) {
        return obj != null && obj.has(key) ? obj.get(key).getAsBoolean() : def;
    }
}