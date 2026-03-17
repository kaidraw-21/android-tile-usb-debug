package com.snap.tiles.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.snap.tiles.data.Action
import com.snap.tiles.data.TileConfig
import com.snap.tiles.data.TileConfigRepo
import com.snap.tiles.R

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EditTileScreen(
    slotIndex: Int,
    returnedActions: Set<Action>?,
    onReturnedActionsConsumed: () -> Unit,
    onBack: () -> Unit,
    onAddAction: (currentSelected: Set<Action>) -> Unit,
    onSaved: () -> Unit
) {
    val config = remember { TileConfigRepo.get(slotIndex) }
    var label by remember { mutableStateOf(config.label) }
    var selectedActions by remember { mutableStateOf(config.actions.toSet()) }
    var enabled by remember { mutableStateOf(config.enabled) }
    val title = stringResource(R.string.edit_slot_title, slotIndex)

    // When returning from AddActionScreen, apply the returned selections
    LaunchedEffect(returnedActions) {
        if (returnedActions != null) {
            selectedActions = returnedActions
            onReturnedActionsConsumed()
        }
    }

    Scaffold(topBar = {
        CenterAlignedTopAppBar(
            title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
            navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_back)) } },
            actions = {
                Button(onClick = {
                    TileConfigRepo.save(TileConfig(slotIndex, label.ifBlank { "Slot $slotIndex" }, selectedActions.toList(), enabled || selectedActions.isNotEmpty()))
                    onSaved()
                }, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer),
                    shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp), modifier = Modifier.padding(end = 8.dp)
                ) { Text(stringResource(R.string.btn_save), fontWeight = FontWeight.SemiBold, fontSize = 14.sp) }
            },
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.8f))
        )
    }) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp).padding(top = 24.dp, bottom = 32.dp), verticalArrangement = Arrangement.spacedBy(32.dp)) {
            // LABEL
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(stringResource(R.string.section_label))
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextField(value = label, onValueChange = { label = it }, modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
                        colors = TextFieldDefaults.colors(unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow, focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLowest,
                            unfocusedIndicatorColor = MaterialTheme.colorScheme.surfaceContainerLow, focusedIndicatorColor = MaterialTheme.colorScheme.tertiaryContainer),
                        shape = RoundedCornerShape(8.dp), singleLine = true)
                }
            }
            // ACTIONS
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SectionHeader(stringResource(R.string.section_actions))
                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer), shape = RoundedCornerShape(16.dp)) {
                    Column(modifier = Modifier.padding(24.dp)) {
                        Text(stringResource(R.string.section_actions), fontSize = 11.sp, letterSpacing = 1.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f), fontWeight = FontWeight.Medium)
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.actions_description), fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.height(20.dp))
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            selectedActions.forEach { action ->
                                ActionChip(action, true, onToggle = { selectedActions = selectedActions - action }, onRemove = { selectedActions = selectedActions - action })
                            }
                            Surface(modifier = Modifier.size(44.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceContainerLowest,
                                border = ButtonDefaults.outlinedButtonBorder, onClick = { onAddAction(selectedActions) }) {
                                Box(contentAlignment = Alignment.Center) { Icon(Icons.Default.Add, stringResource(R.string.cd_add_action), tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp)) }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionChip(action: Action, isSelected: Boolean, onToggle: () -> Unit, onRemove: (() -> Unit)? = null) {
    val label = stringResource(action.labelRes)
    Surface(onClick = onToggle, shape = RoundedCornerShape(50),
        color = if (isSelected) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, fontSize = 14.sp,
                color = if (isSelected) MaterialTheme.colorScheme.onTertiaryContainer else MaterialTheme.colorScheme.onSurface)
            if (isSelected && onRemove != null) {
                Icon(Icons.Default.Close, stringResource(R.string.cd_remove), modifier = Modifier.size(18.dp).clip(CircleShape).clickable { onRemove() },
                    tint = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.6f))
            } else if (!isSelected) {
                Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp), tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            }
        }
    }
}
