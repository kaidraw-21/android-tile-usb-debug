package com.snap.tiles.executor

import android.content.ContentResolver
import android.content.Context
import android.provider.Settings
import android.util.Log
import com.snap.tiles.data.PrefsManager
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.data.remote.ActionRegistry
import com.snap.tiles.data.remote.LinkedSetting
import com.snap.tiles.data.remote.RemoteAction
import com.snap.tiles.data.remote.SettingType
import com.snap.tiles.data.remote.SettingWrite
import com.snap.tiles.data.remote.StateCheck
import com.snap.tiles.data.remote.ValueType

/**
 * Generic executor that reads/writes Android Settings based on [RemoteAction] JSON config.
 * Handles special cases (Accessibility cache, DevMode↔USB) via [RemoteAction.customHandler].
 */
object DynamicActionExecutor {

    private const val TAG = "DynExecutor"

    // ── Read state ──────────────────────────────────────────────

    fun getState(actionId: String, resolver: ContentResolver): Boolean {
        val action = ActionRegistry.getAction(actionId) ?: return false
        return getState(action, resolver)
    }

    fun getState(action: RemoteAction, resolver: ContentResolver): Boolean {
        val result = when (action.safeStateCheck) {
            StateCheck.NOT_EMPTY -> {
                val v = readString(action.safeSettingType, action.safeSettingKey, resolver)
                !v.isNullOrEmpty()
            }
            StateCheck.ALL_LINKED_ON -> {
                val main = readRaw(action, resolver) == action.safeOnValue
                val linked = action.safeLinkedSettings.all { ls ->
                    readRawLinked(action.safeSettingType, ls, resolver) == ls.onValue
                }
                main && linked
            }
            StateCheck.DEFAULT -> {
                readRaw(action, resolver) == action.safeOnValue
            }
        }
        Log.d(TAG, "getState(${action.id}) -> $result")
        return result
    }

    // ── Write state ─────────────────────────────────────────────

    fun setState(actionId: String, resolver: ContentResolver, targetOn: Boolean,
                 visited: MutableSet<String> = mutableSetOf()) {
        if (!visited.add(actionId)) {
            Log.d(TAG, "setState($actionId) skipped — already visited")
            return
        }
        val action = ActionRegistry.getAction(actionId) ?: return
        setState(action, resolver, targetOn, visited)
    }

    fun setState(action: RemoteAction, resolver: ContentResolver, targetOn: Boolean,
                 visited: MutableSet<String> = mutableSetOf()) {
        Log.d(TAG, "setState(${action.id}, on=$targetOn)")
        visited.add(action.id)

        // Custom handler (e.g. DEVELOPER_MODE special logic)
        if (action.customHandler != null) {
            handleCustom(action, resolver, targetOn)
            return
        }

        // Cache action (e.g. Accessibility — save/restore string value)
        if (action.cacheKey != null) {
            handleCacheAction(action, resolver, targetOn)
            return
        }

        // Side effects
        val effects = if (targetOn) action.sideEffects?.safeOnEnable else action.sideEffects?.safeOnDisable
        effects?.forEach { writeSetting(it, resolver) }

        // Main setting
        writeValue(action.safeSettingType, action.safeSettingKey, action.safeValueType,
            if (targetOn) action.safeOnValue else action.safeOffValue, resolver)

        // Linked settings
        action.safeLinkedSettings.forEach { ls ->
            writeValue(action.safeSettingType, ls.key, ls.valueType,
                if (targetOn) ls.onValue else ls.offValue, resolver)
        }
    }

    fun toggleAll(actionIds: List<String>, resolver: ContentResolver, context: Context, targetOn: Boolean) {
        if (actionIds.isEmpty()) return
        Log.d(TAG, "toggleAll(ids=$actionIds, targetOn=$targetOn)")

        // Each action is toggled independently — visited guards against double-write
        // (e.g. if same setting appears via multiple paths)
        val visited = mutableSetOf<String>()
        actionIds.forEach { setState(it, resolver, targetOn, visited) }

        // Collect affected IDs for tile refresh
        val affected = mutableSetOf<String>()
        affected.addAll(actionIds)
        actionIds.forEach { id ->
            affected.addAll(ActionRegistry.getDescendantIds(id))
            var pid = ActionRegistry.getParentId(id)
            while (pid != null) { affected.add(pid!!); pid = ActionRegistry.getParentId(pid!!) }
            val remoteAction = ActionRegistry.getAction(id)
            if (remoteAction != null) {
                remoteAction.safeLinkedSettings.forEach { ls -> affected.add(ls.key) }
                remoteAction.sideEffects?.safeOnEnable?.forEach { affected.add(it.key) }
                remoteAction.sideEffects?.safeOnDisable?.forEach { affected.add(it.key) }
            }
        }

        TileConfigRepo.requestRefreshAffectedTiles(context, affected)
    }

    /** Keep a11y cache fresh — call on tile refresh when accessibility is ON */
    fun refreshA11yCache(resolver: ContentResolver) {
        val current = Settings.Secure.getString(resolver, "enabled_accessibility_services")
        if (!current.isNullOrEmpty()) {
            val saved = PrefsManager.getSavedA11y()
            if (saved != current) {
                Log.d(TAG, "refreshA11yCache: updating -> $current")
                PrefsManager.setSavedA11y(current)
            }
        }
    }

    // ── Internals ───────────────────────────────────────────────

    private fun handleCacheAction(action: RemoteAction, resolver: ContentResolver, targetOn: Boolean) {
        val key = action.safeSettingKey
        if (targetOn) {
            // Restore from cache
            val saved = PrefsManager.tileRuntime.getString(action.cacheKey, null)
            if (!saved.isNullOrEmpty() && saved != "null") {
                writeString(action.safeSettingType, key, saved, resolver)
            }
        } else {
            // Cache current value then clear
            val current = readString(action.safeSettingType, key, resolver)
            if (!current.isNullOrEmpty()) {
                PrefsManager.tileRuntime.edit().putString(action.cacheKey, current).apply()
                Log.d(TAG, "cached ${action.cacheKey} = $current")
            }
            writeString(action.safeSettingType, key, null, resolver)
        }
    }

    private fun handleCustom(action: RemoteAction, resolver: ContentResolver, targetOn: Boolean) {
        when (action.customHandler) {
            "DEVELOPER_MODE" -> {
                // Only write the developer mode setting itself — no cascade to children
                Settings.Global.putInt(resolver, action.safeSettingKey, if (targetOn) 1 else 0)
            }
            else -> {
                Log.w(TAG, "Unknown customHandler: ${action.customHandler}")
                writeValue(action.safeSettingType, action.safeSettingKey, action.safeValueType,
                    if (targetOn) action.safeOnValue else action.safeOffValue, resolver)
            }
        }
    }

    private fun readRaw(action: RemoteAction, resolver: ContentResolver): String? {
        return when (action.safeValueType) {
            ValueType.INT -> readInt(action.safeSettingType, action.safeSettingKey, resolver)?.toString()
            ValueType.FLOAT -> readFloat(action.safeSettingType, action.safeSettingKey, resolver)?.toString()
            ValueType.STRING -> readString(action.safeSettingType, action.safeSettingKey, resolver)
        }
    }

    private fun readRawLinked(type: SettingType, ls: LinkedSetting, resolver: ContentResolver): String? {
        return when (ls.valueType) {
            ValueType.INT -> readInt(type, ls.key, resolver)?.toString()
            ValueType.FLOAT -> readFloat(type, ls.key, resolver)?.toString()
            ValueType.STRING -> readString(type, ls.key, resolver)
        }
    }

    private fun writeSetting(sw: SettingWrite, resolver: ContentResolver) {
        writeValue(sw.settingType, sw.key, sw.valueType, sw.value, resolver)
    }

    private fun writeValue(type: SettingType, key: String, vt: ValueType, value: String, resolver: ContentResolver) {
        when (vt) {
            ValueType.INT -> {
                val v = value.toIntOrNull() ?: 0
                when (type) {
                    SettingType.GLOBAL -> Settings.Global.putInt(resolver, key, v)
                    SettingType.SECURE -> Settings.Secure.putInt(resolver, key, v)
                }
            }
            ValueType.FLOAT -> {
                val v = value.toFloatOrNull() ?: 0f
                when (type) {
                    SettingType.GLOBAL -> Settings.Global.putFloat(resolver, key, v)
                    SettingType.SECURE -> Settings.Secure.putFloat(resolver, key, v)
                }
            }
            ValueType.STRING -> writeString(type, key, value.ifEmpty { null }, resolver)
        }
    }

    private fun writeString(type: SettingType, key: String, value: String?, resolver: ContentResolver) {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.putString(resolver, key, value)
            SettingType.SECURE -> Settings.Secure.putString(resolver, key, value)
        }
    }

    private fun readInt(type: SettingType, key: String, resolver: ContentResolver): Int? = runCatching {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.getInt(resolver, key, 0)
            SettingType.SECURE -> Settings.Secure.getInt(resolver, key, 0)
        }
    }.getOrNull()

    private fun readFloat(type: SettingType, key: String, resolver: ContentResolver): Float? = runCatching {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.getFloat(resolver, key, 0f)
            SettingType.SECURE -> Settings.Secure.getFloat(resolver, key, 0f)
        }
    }.getOrNull()

    private fun readString(type: SettingType, key: String, resolver: ContentResolver): String? = runCatching {
        when (type) {
            SettingType.GLOBAL -> Settings.Global.getString(resolver, key)
            SettingType.SECURE -> Settings.Secure.getString(resolver, key)
        }
    }.getOrNull()
}
