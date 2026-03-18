package com.snap.tiles.ui

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.lifecycle.lifecycleScope
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.executor.DynamicActionExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ShortcutActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        lifecycleScope.launch {
            val fixedActionName = intent.getStringExtra("fixed_action")
            val targetOn = if (fixedActionName != null) {
                handleFixedTile(fixedActionName)
            } else {
                handleCustomTile(intent.getIntExtra("slot_index", -1))
            }
            if (targetOn != null) updateShortcutLabel(targetOn)
            finish()
        }
    }

    private suspend fun handleFixedTile(actionName: String): Boolean? {
        val label = TileConfigRepo.fixedTiles.firstOrNull { it.action.name == actionName }
            ?.let { getString(it.labelRes) } ?: actionName
        val targetOn = withContext(Dispatchers.IO) {
            val isOn = DynamicActionExecutor.getState(actionName, contentResolver)
            val target = !isOn
            DynamicActionExecutor.toggleAll(listOf(actionName), contentResolver, applicationContext, target)
            target
        }
        val stateStr = if (targetOn) "ON" else "OFF"
        Toast.makeText(applicationContext, "$label: $stateStr", Toast.LENGTH_SHORT).show()
        return targetOn
    }

    private suspend fun handleCustomTile(slotIndex: Int): Boolean? {
        if (slotIndex == -1) return null
        val config = TileConfigRepo.get(slotIndex)
        val actionIds = config.actionIds
        if (actionIds.isEmpty()) return null

        val label = config.label.takeIf { it.isNotBlank() }
            ?: TileConfigRepo.customSlots.firstOrNull { it.slotIndex == slotIndex }
                ?.let { getString(it.labelRes) }
            ?: "Slot $slotIndex"

        val targetOn = withContext(Dispatchers.IO) {
            val isOn = actionIds.all { DynamicActionExecutor.getState(it, contentResolver) }
            val target = !isOn
            DynamicActionExecutor.toggleAll(actionIds, contentResolver, applicationContext, target)
            target
        }
        val stateStr = if (targetOn) "ON" else "OFF"
        Toast.makeText(applicationContext, "$label: $stateStr", Toast.LENGTH_SHORT).show()
        return targetOn
    }

    private fun updateShortcutLabel(targetOn: Boolean) {
        val shortcutId = intent.getStringExtra("shortcut_id") ?: return
        val baseLabel = intent.getStringExtra("shortcut_label") ?: return
        val iconRes = intent.getIntExtra("shortcut_icon_res", 0).takeIf { it != 0 } ?: return

        val newLabel = if (targetOn) "$baseLabel · ON" else baseLabel
        runCatching {
            val updated = ShortcutInfoCompat.Builder(this, shortcutId)
                .setShortLabel(newLabel)
                .setIcon(buildShortcutIcon(this, iconRes))
                .setIntent(intent)
                .build()
            ShortcutManagerCompat.updateShortcuts(this, listOf(updated))
        }
    }
}
