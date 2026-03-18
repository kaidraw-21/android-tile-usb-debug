package com.snap.tiles.ui.screens

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Drawable
import android.net.Uri
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.snap.tiles.R
import com.snap.tiles.data.Action
import com.snap.tiles.data.CustomSlotInfo
import com.snap.tiles.data.FixedTileInfo
import com.snap.tiles.data.PrefsManager
import com.snap.tiles.data.TileConfig
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.float.FloatingTileService
import com.snap.tiles.service.FloatingButtonService
import com.snap.tiles.ui.theme.Success

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(onEditSlot: (Int) -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scrollState = rememberScrollState()

    var hasWriteSecure by remember { mutableStateOf(false) }
    var hasOverlay by remember { mutableStateOf(false) }
    var tiles by remember { mutableStateOf(listOf<TileConfig>()) }
    var isFloatEnabled by remember { mutableStateOf(PrefsManager.isFloatVisible()) }

    // Recheck permissions + float state on every resume
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            hasWriteSecure = checkWriteSecureSettings(context.contentResolver)
            hasOverlay = Settings.canDrawOverlays(context)
            tiles = (1..TileConfigRepo.SLOT_COUNT).map { TileConfigRepo.get(it) }
            // If user came back from overlay permission settings
            if (PrefsManager.isFloatVisible() && hasOverlay) {
                isFloatEnabled = true
                FloatingTileService.start(context)
            }
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(scrollState)
                .padding(horizontal = 24.dp).padding(top = 16.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(32.dp)
        ) {
            if (!hasWriteSecure) PermissionRequiredBanner()
            PermissionsSection(
                hasWriteSecure = hasWriteSecure,
                showOverlay = isFloatEnabled,
                hasOverlay = hasOverlay,
                onRefresh = {
                    hasWriteSecure = checkWriteSecureSettings(context.contentResolver)
                    hasOverlay = Settings.canDrawOverlays(context)
                    tiles = (1..TileConfigRepo.SLOT_COUNT).map { TileConfigRepo.get(it) }
                }
            )
            FloatingButtonSection()
            FixedTilesSection(context)
            CustomTilesSection(tiles, onEditSlot)
            FloatingTileSection(
                isEnabled = isFloatEnabled,
                hasOverlayPermission = hasOverlay,
                tiles = tiles,
                onToggle = { newEnabled ->
                    if (newEnabled) {
                        if (!Settings.canDrawOverlays(context)) {
                            // Request permission first — save intent so we resume correctly
                            PrefsManager.setFloatVisible(true)
                            isFloatEnabled = true
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        } else {
                            PrefsManager.setFloatVisible(true)
                            isFloatEnabled = true
                            FloatingTileService.start(context)
                        }
                    } else {
                        PrefsManager.setFloatVisible(false)
                        isFloatEnabled = false
                        FloatingTileService.stop(context)
                    }
                }
            )
        }
    }
}

@Composable
private fun PermissionRequiredBanner() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(28.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.permission_required_title), fontWeight = FontWeight.Bold, fontSize = 16.sp, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.permission_required_desc), fontSize = 13.sp, color = MaterialTheme.colorScheme.onErrorContainer.copy(alpha = 0.8f), lineHeight = 18.sp)
            }
        }
    }
}

@Composable
private fun PermissionsSection(
    hasWriteSecure: Boolean,
    showOverlay: Boolean,
    hasOverlay: Boolean,
    onRefresh: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(stringResource(R.string.section_permissions))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                PermissionRow(
                    label = stringResource(R.string.permission_write_secure),
                    isGranted = hasWriteSecure,
                    showRefresh = !hasWriteSecure,
                    onRefresh = onRefresh
                )
                if (showOverlay) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    PermissionRow(
                        label = stringResource(R.string.permission_overlay),
                        isGranted = hasOverlay,
                        showRefresh = false,
                        onRefresh = {}
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionRow(
    label: String,
    isGranted: Boolean,
    showRefresh: Boolean,
    onRefresh: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (isGranted) Success else MaterialTheme.colorScheme.error))
            Text(label, fontWeight = FontWeight.Medium, fontSize = 15.sp)
        }
        if (showRefresh) {
            FilledTonalButton(
                onClick = onRefresh,
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 0.dp),
                modifier = Modifier.height(32.dp)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(stringResource(R.string.btn_refresh_permission), fontSize = 12.sp)
            }
        } else {
            Icon(
                Icons.Default.CheckCircle, null,
                tint = if (isGranted) Success.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun FixedTilesSection(context: Context) {
    val fixedTiles = TileConfigRepo.fixedTiles

    // Read cached a11y services for chip display — refresh on every resume
    val a11yApps = remember { mutableStateOf<List<A11yAppInfo>>(emptyList()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            a11yApps.value = getActiveA11yApps(context)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(stringResource(R.string.section_default_tiles))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest), shape = RoundedCornerShape(16.dp)) {
            Column {
                fixedTiles.forEachIndexed { index, info ->
                    FixedTileRow(info = info)
                    // Show a11y service chips under the Accessibility row
                    if (info.action == Action.ACCESSIBILITY && a11yApps.value.isNotEmpty()) {
                        A11yServiceChips(a11yApps.value)
                    }
                    if (index < fixedTiles.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FixedTileRow(info: FixedTileInfo) {
    val label = stringResource(info.labelRes)
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(10.dp),
            color = MaterialTheme.colorScheme.primaryContainer) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(info.iconRes), contentDescription = label, modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
        }
        Text(label, fontWeight = FontWeight.Bold, fontSize = 16.sp, modifier = Modifier.weight(1f))
    }
}

@Composable
private fun CustomTilesSection(tiles: List<TileConfig>, onEditSlot: (Int) -> Unit) {
    val context = LocalContext.current
    var mutableTiles by remember(tiles) { mutableStateOf(tiles) }
    val slotInfoMap = remember { TileConfigRepo.customSlots.associateBy { it.slotIndex } }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(stringResource(R.string.section_custom_tiles))
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest), shape = RoundedCornerShape(16.dp)) {
            Column {
                mutableTiles.forEachIndexed { index, config ->
                    val slotInfo = slotInfoMap[config.slotIndex]
                    val hasActions = config.actions.isNotEmpty()
                    CustomTileSlotRow(config = config, slotInfo = slotInfo, hasActions = hasActions,
                        isHighlighted = hasActions && config.enabled,
                        onToggle = { newEnabled ->
                            TileConfigRepo.setEnabled(context, config.slotIndex, newEnabled)
                            mutableTiles = mutableTiles.toMutableList().also { it[index] = config.copy(enabled = newEnabled) }
                        },
                        onClick = { onEditSlot(config.slotIndex) })
                    if (index < mutableTiles.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f), modifier = Modifier.padding(horizontal = 20.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CustomTileSlotRow(config: TileConfig, slotInfo: CustomSlotInfo?, hasActions: Boolean, isHighlighted: Boolean, onToggle: (Boolean) -> Unit, onClick: () -> Unit) {
    val slotLabel = slotInfo?.let { stringResource(it.labelRes) } ?: "Slot ${config.slotIndex}"
    Row(
        modifier = Modifier.fillMaxWidth()
            .clickable(onClick = onClick).padding(horizontal = 20.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (slotInfo != null) {
            Surface(modifier = Modifier.size(36.dp), shape = RoundedCornerShape(10.dp),
                color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(painter = painterResource(slotInfo.iconRes), contentDescription = slotLabel, modifier = Modifier.size(20.dp),
                        tint = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (hasActions) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                val customLabel = config.label.takeIf { it.isNotBlank() && it != slotLabel }
                Text(
                    text = customLabel ?: slotLabel,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                if (customLabel != null) {
                    Text(
                        text = slotLabel,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                }
                @OptIn(ExperimentalLayoutApi::class)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    config.actions.forEach { action ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isHighlighted) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                            tonalElevation = 0.dp
                        ) {
                            Text(
                                stringResource(action.labelRes),
                                fontSize = 11.sp,
                                color = if (isHighlighted) MaterialTheme.colorScheme.onPrimaryContainer
                                        else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Column(modifier = Modifier.weight(1f)) {
                Text(slotLabel, fontWeight = FontWeight.Medium, fontSize = 15.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(stringResource(R.string.tap_to_configure), fontStyle = FontStyle.Italic, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
            }
        }
        Switch(checked = config.enabled && hasActions, onCheckedChange = if (hasActions) onToggle else null, enabled = hasActions,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White, checkedTrackColor = MaterialTheme.colorScheme.primary,
                uncheckedThumbColor = MaterialTheme.colorScheme.surfaceContainerLowest, uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest, uncheckedBorderColor = Color.Transparent,
                disabledUncheckedThumbColor = MaterialTheme.colorScheme.surfaceContainerLowest, disabledUncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest, disabledUncheckedBorderColor = Color.Transparent
            ), modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun FloatingButtonSection() {
    val context = LocalContext.current
    var enabled by remember { mutableStateOf(PrefsManager.isFloatVisible()) }
    var selectedSlot by remember { mutableStateOf(PrefsManager.getControlledSlot()) }
    var selectedSize by remember { mutableStateOf(PrefsManager.getButtonSize()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(stringResource(R.string.section_float_button))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Toggle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.float_button_title),
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            stringResource(R.string.float_button_desc),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Switch(
                        checked = enabled,
                        onCheckedChange = { on ->
                            if (on) {
                                if (!Settings.canDrawOverlays(context)) {
                                    Toast.makeText(context, context.getString(R.string.float_overlay_required), Toast.LENGTH_LONG).show()
                                    context.startActivity(
                                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}"))
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } else {
                                    enabled = true
                                    PrefsManager.setFloatVisible(true)
                                    FloatingButtonService.start(context)
                                }
                            } else {
                                enabled = false
                                PrefsManager.setFloatVisible(false)
                                FloatingButtonService.stop(context)
                            }
                        },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f))

                // Slot selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.float_controlled_slot).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    // Horizontal scroll to prevent overflow when many slots are shown
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        TileConfigRepo.customSlots.forEach { slotInfo ->
                            val isSelected = slotInfo.slotIndex == selectedSlot
                            Surface(
                                onClick = {
                                    selectedSlot = slotInfo.slotIndex
                                    PrefsManager.setControlledSlot(slotInfo.slotIndex)
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(slotInfo.iconRes),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        stringResource(slotInfo.labelRes),
                                        fontSize = 13.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }

                // Size selector
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.float_button_size).uppercase(),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "SMALL" to "S",
                            "MEDIUM" to "M",
                            "LARGE" to "L"
                        ).forEach { (key, label) ->
                            val isSelected = selectedSize == key
                            Surface(
                                onClick = {
                                    selectedSize = key
                                    PrefsManager.setButtonSize(key)
                                    if (enabled) {
                                        FloatingButtonService.stop(context)
                                        FloatingButtonService.start(context)
                                    }
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = if (isSelected) null else ButtonDefaults.outlinedButtonBorder,
                                modifier = Modifier.size(44.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        label,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        fontSize = 14.sp,
                                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.2.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(16.dp))
        HorizontalDivider(modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
    }
}

// ── Floating Tile Section ─────────────────────────────────────────────────

@Composable
private fun FloatingTileSection(
    isEnabled: Boolean,
    hasOverlayPermission: Boolean,
    tiles: List<TileConfig>,
    onToggle: (Boolean) -> Unit
) {
    val context = LocalContext.current

    var selectedTileId by remember { mutableStateOf(PrefsManager.getFloatTileId()) }
    var selectedIconIndex by remember { mutableStateOf(PrefsManager.getFloatIconIndex()) }

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        SectionHeader(stringResource(R.string.section_floating_tile))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column {
                // Header row with switch
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        modifier = Modifier.size(36.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isEnabled && hasOverlayPermission)
                            MaterialTheme.colorScheme.primaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                painter = painterResource(
                                    FloatingTileService.FLOAT_ICONS.getOrElse(selectedIconIndex) { FloatingTileService.FLOAT_ICONS[0] }
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = if (isEnabled && hasOverlayPermission)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stringResource(R.string.float_tile_title),
                            fontWeight = FontWeight.Bold, fontSize = 16.sp
                        )
                        Text(
                            stringResource(R.string.float_tile_desc),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = isEnabled,
                        onCheckedChange = onToggle,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            uncheckedBorderColor = Color.Transparent
                        ),
                        modifier = Modifier.height(24.dp)
                    )
                }

                // Expanded config — only when ON and permission granted
                if (isEnabled && hasOverlayPermission) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    FloatTileSelector(
                        selectedTileId = selectedTileId,
                        tiles = tiles,
                        onSelect = { id ->
                            selectedTileId = id
                            PrefsManager.setFloatTileId(id)
                            FloatingTileService.refresh(context)
                        }
                    )
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    FloatIconSelector(
                        selectedIndex = selectedIconIndex,
                        onSelect = { index ->
                            selectedIconIndex = index
                            PrefsManager.setFloatIconIndex(index)
                            FloatingTileService.stop(context)
                            FloatingTileService.start(context)
                        }
                    )
                } else if (isEnabled && !hasOverlayPermission) {
                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.1f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                    Text(
                        stringResource(R.string.float_need_permission),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FloatTileSelector(
    selectedTileId: String,
    tiles: List<TileConfig>,
    onSelect: (String) -> Unit
) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.float_select_tile),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Fixed tiles
            TileConfigRepo.fixedTiles.forEach { info ->
                val id = "FIXED_${info.action.name}"
                val isSelected = selectedTileId == id
                TileChip(
                    label = stringResource(info.labelRes),
                    isSelected = isSelected,
                    onClick = { onSelect(id) }
                )
            }
            // Custom tiles
            tiles.forEach { config ->
                val slotInfo = TileConfigRepo.customSlots.firstOrNull { it.slotIndex == config.slotIndex }
                val id = "SLOT_${config.slotIndex}"
                val isSelected = selectedTileId == id
                val label = config.label.takeIf { it.isNotBlank() }
                    ?: slotInfo?.let { stringResource(it.labelRes) }
                    ?: "Slot ${config.slotIndex}"
                TileChip(
                    label = label,
                    isSelected = isSelected,
                    onClick = { onSelect(id) }
                )
            }
        }
    }
}

@Composable
private fun TileChip(label: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = if (isSelected) Modifier
        else Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FloatIconSelector(selectedIndex: Int, onSelect: (Int) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            stringResource(R.string.float_select_icon),
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 0.8.sp
        )
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FloatingTileService.FLOAT_ICONS.forEachIndexed { index, iconRes ->
                val isSelected = selectedIndex == index
                Surface(
                    onClick = { onSelect(index) },
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = if (isSelected) null
                    else androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                    )
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            painter = painterResource(iconRes),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────

private fun checkWriteSecureSettings(resolver: ContentResolver): Boolean {
    return runCatching {
        Settings.Global.putInt(resolver, "__test__", 0)
        Settings.Global.putString(resolver, "__test__", null)
        true
    }.getOrDefault(false)
}

// --- Accessibility service chips ---

private data class A11yAppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?
)

private fun getActiveA11yApps(context: Context): List<A11yAppInfo> {
    val raw = com.snap.tiles.data.PrefsManager.getSavedA11y()
    Log.d("HomeScreen", "a11y cached: $raw")
    if (raw.isNullOrEmpty() || raw == "null") return emptyList()
    val pm = context.packageManager
    return raw.split(":")
        .filter { it.isNotBlank() }
        .mapNotNull { it.split("/").firstOrNull()?.trim()?.takeIf(String::isNotEmpty) }
        .distinct()
        .mapNotNull { pkg ->
            try {
                @Suppress("DEPRECATION")
                val appInfo = pm.getApplicationInfo(pkg, 0)
                A11yAppInfo(
                    packageName = pkg,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = pm.getApplicationIcon(appInfo)
                )
            } catch (_: Exception) {
                Log.w("HomeScreen", "Cannot resolve package: $pkg")
                null
            }
        }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun A11yServiceChips(apps: List<A11yAppInfo>) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(start = 72.dp, end = 20.dp, bottom = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        apps.forEach { app ->
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    app.icon?.let { drawable ->
                        Image(
                            bitmap = drawable.toBitmap(width = 48, height = 48).asImageBitmap(),
                            contentDescription = app.label,
                            modifier = Modifier.size(16.dp).clip(RoundedCornerShape(3.dp))
                        )
                    }
                    Text(
                        app.label,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
