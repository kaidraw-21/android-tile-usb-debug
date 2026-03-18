package com.snap.tiles.data

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.util.Log

@SuppressLint("StaticFieldLeak")
object PrefsManager {

    private const val TAG = "PrefsManager"

    private lateinit var appContext: Context

    private const val PREFS_TILE_CONFIG = "tile_configs"
    private const val PREFS_TILE_RUNTIME = "tile_prefs"
    private const val PREFS_FLOAT_CONFIG = "float_config"
    private const val PREFS_FLOAT_POS = "float_pos"
    private const val PREFS_WIDGET = "widget_config"

    private const val KEY_SUFFIX_LABEL = "_label"
    private const val KEY_SUFFIX_ACTIONS = "_actions"
    private const val KEY_SUFFIX_ENABLED = "_enabled"
    private fun slotPrefix(slot: Int) = "slot_$slot"

    private const val PREFS_FIXED_TILE = "fixed_tile_prefs"
    private const val KEY_FIXED_PREFIX = "fixed_"

    const val KEY_SAVED_A11Y = "saved_a11y"
    const val KEY_SHOW_FLOAT = "show_float"
    const val KEY_CONTROLLED_SLOT = "controlled_slot"
    const val DEFAULT_CONTROLLED_SLOT = 1
    const val KEY_POS_X = "pos_x"
    const val KEY_POS_Y = "pos_y"
    const val DEFAULT_POS_X = 100
    const val DEFAULT_POS_Y = 200
    const val KEY_BUTTON_SIZE = "button_size"
    const val DEFAULT_BUTTON_SIZE = "MEDIUM"

    fun init(context: Context) {
        Log.d(TAG, "init()")
        appContext = context.applicationContext
    }

    val tileConfig: SharedPreferences by lazy {
        Log.d(TAG, "tileConfig lazy init")
        appContext.getSharedPreferences(PREFS_TILE_CONFIG, Context.MODE_PRIVATE)
    }

    val tileRuntime: SharedPreferences by lazy {
        Log.d(TAG, "tileRuntime lazy init")
        appContext.getSharedPreferences(PREFS_TILE_RUNTIME, Context.MODE_PRIVATE)
    }

    val floatConfig: SharedPreferences by lazy {
        Log.d(TAG, "floatConfig lazy init")
        appContext.getSharedPreferences(PREFS_FLOAT_CONFIG, Context.MODE_PRIVATE)
    }

    val floatPos: SharedPreferences by lazy {
        Log.d(TAG, "floatPos lazy init")
        appContext.getSharedPreferences(PREFS_FLOAT_POS, Context.MODE_PRIVATE)
    }

    fun getSlotLabel(slot: Int): String {
        val defaultRes = TileConfigRepo.defaultLabelRes(slot)
        val default = appContext.getString(defaultRes)
        val result = tileConfig.getString("${slotPrefix(slot)}$KEY_SUFFIX_LABEL", default) ?: default
        Log.d(TAG, "getSlotLabel(slot=$slot) -> $result")
        return result
    }

    fun getSlotActions(slot: Int): List<Action> {
        val raw = tileConfig.getStringSet("${slotPrefix(slot)}$KEY_SUFFIX_ACTIONS", emptySet()) ?: emptySet()
        val result = raw.mapNotNull { runCatching { Action.valueOf(it) }.getOrNull() }
        Log.d(TAG, "getSlotActions(slot=$slot) -> $result")
        return result
    }

    fun isSlotEnabled(slot: Int): Boolean {
        val result = tileConfig.getBoolean("${slotPrefix(slot)}$KEY_SUFFIX_ENABLED", false)
        Log.d(TAG, "isSlotEnabled(slot=$slot) -> $result")
        return result
    }

    fun saveSlot(config: TileConfig) {
        Log.d(TAG, "saveSlot(slot=${config.slotIndex}, label=${config.label}, actions=${config.actions}, enabled=${config.enabled})")
        val prefix = slotPrefix(config.slotIndex)
        tileConfig.edit()
            .putString("${prefix}$KEY_SUFFIX_LABEL", config.label)
            .putStringSet("${prefix}$KEY_SUFFIX_ACTIONS", config.actions.map { it.name }.toSet())
            .putBoolean("${prefix}$KEY_SUFFIX_ENABLED", config.enabled)
            .apply()
    }

    val fixedTilePrefs: SharedPreferences by lazy {
        Log.d(TAG, "fixedTilePrefs lazy init")
        appContext.getSharedPreferences(PREFS_FIXED_TILE, Context.MODE_PRIVATE)
    }

    fun isFixedTileEnabled(actionName: String): Boolean {
        val result = fixedTilePrefs.getBoolean("$KEY_FIXED_PREFIX$actionName", true)
        Log.d(TAG, "isFixedTileEnabled($actionName) -> $result")
        return result
    }

    fun setFixedTileEnabled(actionName: String, enabled: Boolean) {
        Log.d(TAG, "setFixedTileEnabled($actionName, $enabled)")
        fixedTilePrefs.edit().putBoolean("$KEY_FIXED_PREFIX$actionName", enabled).apply()
    }

    private const val KEY_CACHED_USB_BEFORE_DEV_OFF = "cached_usb_before_dev_off"

    fun getSavedA11y(): String? {
        val result = tileRuntime.getString(KEY_SAVED_A11Y, null)
        Log.d(TAG, "getSavedA11y() -> $result")
        return result
    }

    fun getCachedUsbBeforeDevOff(): Boolean? {
        if (!tileRuntime.contains(KEY_CACHED_USB_BEFORE_DEV_OFF)) return null
        return tileRuntime.getBoolean(KEY_CACHED_USB_BEFORE_DEV_OFF, false)
    }

    fun setCachedUsbBeforeDevOff(value: Boolean?) {
        Log.d(TAG, "setCachedUsbBeforeDevOff($value)")
        if (value == null) {
            tileRuntime.edit().remove(KEY_CACHED_USB_BEFORE_DEV_OFF).apply()
        } else {
            tileRuntime.edit().putBoolean(KEY_CACHED_USB_BEFORE_DEV_OFF, value).apply()
        }
    }

    fun setSavedA11y(value: String?) {
        Log.d(TAG, "setSavedA11y(value=$value)")
        tileRuntime.edit().putString(KEY_SAVED_A11Y, value).apply()
    }

    // Float tile ID: "FIXED_USB_DEBUGGING", "FIXED_DEVELOPER_MODE", "FIXED_ACCESSIBILITY", "SLOT_1"…"SLOT_5"
    const val KEY_FLOAT_TILE_ID = "float_tile_id"
    const val DEFAULT_FLOAT_TILE_ID = "FIXED_USB_DEBUGGING"
    const val KEY_FLOAT_ICON_INDEX = "float_icon_index"

    fun getFloatTileId(): String =
        floatConfig.getString(KEY_FLOAT_TILE_ID, DEFAULT_FLOAT_TILE_ID) ?: DEFAULT_FLOAT_TILE_ID

    fun setFloatTileId(id: String) {
        Log.d(TAG, "setFloatTileId($id)")
        floatConfig.edit().putString(KEY_FLOAT_TILE_ID, id).apply()
    }

    fun getFloatIconIndex(): Int = floatConfig.getInt(KEY_FLOAT_ICON_INDEX, 0)

    fun setFloatIconIndex(index: Int) {
        Log.d(TAG, "setFloatIconIndex($index)")
        floatConfig.edit().putInt(KEY_FLOAT_ICON_INDEX, index).apply()
    }

    fun isFloatVisible(): Boolean {
        val result = floatConfig.getBoolean(KEY_SHOW_FLOAT, false)
        Log.d(TAG, "isFloatVisible() -> $result")
        return result
    }

    fun setFloatVisible(visible: Boolean) {
        Log.d(TAG, "setFloatVisible(visible=$visible)")
        floatConfig.edit().putBoolean(KEY_SHOW_FLOAT, visible).apply()
    }

    fun getControlledSlot(): Int {
        val result = floatConfig.getInt(KEY_CONTROLLED_SLOT, DEFAULT_CONTROLLED_SLOT)
        Log.d(TAG, "getControlledSlot() -> $result")
        return result
    }

    fun setControlledSlot(slot: Int) {
        Log.d(TAG, "setControlledSlot(slot=$slot)")
        floatConfig.edit().putInt(KEY_CONTROLLED_SLOT, slot).apply()
    }

    fun getFloatPosition(): Pair<Int, Int> {
        val result = floatPos.getInt(KEY_POS_X, DEFAULT_POS_X) to floatPos.getInt(KEY_POS_Y, DEFAULT_POS_Y)
        Log.d(TAG, "getFloatPosition() -> $result")
        return result
    }

    fun saveFloatPosition(x: Int, y: Int) {
        Log.d(TAG, "saveFloatPosition(x=$x, y=$y)")
        floatPos.edit().putInt(KEY_POS_X, x).putInt(KEY_POS_Y, y).apply()
    }

    fun getButtonSize(): String {
        val result = floatConfig.getString(KEY_BUTTON_SIZE, DEFAULT_BUTTON_SIZE) ?: DEFAULT_BUTTON_SIZE
        Log.d(TAG, "getButtonSize() -> $result")
        return result
    }

    fun setButtonSize(size: String) {
        Log.d(TAG, "setButtonSize(size=$size)")
        floatConfig.edit().putString(KEY_BUTTON_SIZE, size).apply()
    }

    // ── Widget config ──────────────────────────────────────────────────────────

    val widgetPrefs: SharedPreferences by lazy {
        appContext.getSharedPreferences(PREFS_WIDGET, Context.MODE_PRIVATE)
    }

    /** tileId format: "FIXED_USB_DEBUGGING" or "SLOT_1" */
    fun saveWidgetConfig(widgetId: Int, tileId: String, label: String, iconRes: Int) {
        widgetPrefs.edit()
            .putString("tile_$widgetId", tileId)
            .putString("label_$widgetId", label)
            .putInt("icon_$widgetId", iconRes)
            .apply()
    }

    /** Returns Triple(tileId, label, iconRes) or null if not configured */
    fun getWidgetConfig(widgetId: Int): Triple<String, String, Int>? {
        val tileId = widgetPrefs.getString("tile_$widgetId", null) ?: return null
        val label = widgetPrefs.getString("label_$widgetId", null) ?: return null
        val iconRes = widgetPrefs.getInt("icon_$widgetId", 0).takeIf { it != 0 } ?: return null
        return Triple(tileId, label, iconRes)
    }

    fun deleteWidgetConfig(widgetId: Int) {
        widgetPrefs.edit()
            .remove("tile_$widgetId")
            .remove("label_$widgetId")
            .remove("icon_$widgetId")
            .apply()
    }
}
