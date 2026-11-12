package com.example.ui.screens

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MotionPhotosOff
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.ThemePreset
import com.example.data.preferences.MelloUserPreferences
import com.example.ui.theme.MelloThemeColors
import com.example.ui.theme.paletteForPreset
import com.example.ui.viewmodel.MelloUiState
import com.example.util.ReminderNotificationHelper

@Composable
fun ProfileScreen(
    uiState: MelloUiState,
    preferences: MelloUserPreferences,
    onSelectTheme: (ThemePreset) -> Unit,
    onUpdateDisplayName: (String) -> Unit = {},
    onSetNotificationsEnabled: (Boolean) -> Unit,
    onSetReducedMotion: (Boolean) -> Unit,
    onSignInFromDemo: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    modifier: Modifier = Modifier
) {
    val palette = MelloThemeColors.palette
    val context = LocalContext.current
    val effectiveDisplayName = preferences.customDisplayName.ifBlank { uiState.userDisplayName }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedNameText by remember(effectiveDisplayName) { mutableStateOf(effectiveDisplayName) }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        onSetNotificationsEnabled(granted)
    }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            containerColor = palette.surface,
            title = {
                Text(
                    text = "Edit Display Name",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            },
            text = {
                OutlinedTextField(
                    value = editedNameText,
                    onValueChange = { editedNameText = it.take(40) },
                    label = { Text("Display name") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = palette.primary,
                        unfocusedBorderColor = palette.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("profile_name_input")
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onUpdateDisplayName(editedNameText.trim())
                        showEditNameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                    modifier = Modifier.testTag("save_display_name_button")
                ) {
                    Text("Save", color = palette.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            containerColor = palette.surface,
            title = {
                Text(
                    text = "Delete Account & Data?",
                    style = MaterialTheme.typography.titleLarge,
                    color = palette.textPrimary
                )
            },
            text = {
                Text(
                    text = "This will permanently remove all of your tasks, daily habits, and streak logs from Mello and sign you out.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirmDialog = false
                        onDeleteAccount()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = palette.danger)
                ) {
                    Text("Delete Everything", color = palette.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = palette.textSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(palette.background),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Column {
                Text(
                    text = "Profile & Studio",
                    style = MaterialTheme.typography.headlineLarge,
                    color = palette.textPrimary
                )
                Text(
                    text = "Personalize your theme, reminders & cloud account",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.textSecondary
                )
            }
        }

        // 1. User Account Card
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(58.dp)
                                .clip(CircleShape)
                                .background(palette.primarySoft)
                        ) {
                            Text(
                                text = effectiveDisplayName.take(1).uppercase(),
                                style = MaterialTheme.typography.headlineMedium,
                                color = palette.primary
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = effectiveDisplayName,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = palette.textPrimary,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        editedNameText = effectiveDisplayName
                                        showEditNameDialog = true
                                    },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .testTag("edit_display_name_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Edit,
                                        contentDescription = "Edit display name",
                                        tint = palette.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (uiState.isDemoMode) "Demo Mode • Local Session" else uiState.userEmail.ifBlank { "Google Account Connected" },
                                style = MaterialTheme.typography.bodySmall,
                                color = palette.textSecondary
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Surface(
                                color = if (uiState.isDemoMode) palette.pastelButter else palette.pastelMint,
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.CloudDone,
                                        contentDescription = null,
                                        tint = if (uiState.isDemoMode) palette.pastelButterText else palette.pastelMintText,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = if (uiState.isDemoMode) "Demo Preview" else "Cloud Synced",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.isDemoMode) palette.pastelButterText else palette.pastelMintText
                                    )
                                }
                            }
                        }
                    }

                    if (uiState.isDemoMode) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onSignInFromDemo,
                            colors = ButtonDefaults.buttonColors(containerColor = palette.primary),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("profile_connect_google_button")
                        ) {
                            Text(
                                text = "Sign in with Google for Cloud Sync",
                                style = MaterialTheme.typography.titleSmall,
                                color = palette.onPrimary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // 2. Theme Selector (6 Presets)
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Palette,
                            contentDescription = null,
                            tint = palette.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Theme Presets",
                            style = MaterialTheme.typography.titleLarge,
                            color = palette.textPrimary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Updates colors, surfaces & accents across Mello",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.textSecondary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        ThemePreset.entries.forEach { preset ->
                            val isSelected = preferences.themePreset == preset
                            val previewPalette = paletteForPreset(preset)
                            Surface(
                                color = if (isSelected) palette.primarySoft else palette.surfaceMuted,
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) palette.primary else palette.border
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { onSelectTheme(preset) }
                                    .testTag("theme_preset_${preset.id}")
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        // Color swatch trio
                                        Row(horizontalArrangement = Arrangement.spacedBy((-6).dp)) {
                                            SwatchCircle(previewPalette.background)
                                            SwatchCircle(previewPalette.primary)
                                            SwatchCircle(previewPalette.pastelPink)
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = preset.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = palette.textPrimary
                                            )
                                            Text(
                                                text = preset.subtitle,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = palette.textSecondary
                                            )
                                        }
                                    }

                                    if (isSelected) {
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(24.dp)
                                                .clip(CircleShape)
                                                .background(palette.primary)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Selected theme",
                                                tint = palette.onPrimary,
                                                modifier = Modifier.size(14.dp)
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

        // 3. Notifications & App Preferences
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = "Preferences & Notifications",
                        style = MaterialTheme.typography.titleLarge,
                        color = palette.textPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Task Reminders Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.NotificationsActive,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Task Reminder Notifications",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Local Android notifications when task reminders are set",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Switch(
                            checked = preferences.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                if (enabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                                    !ReminderNotificationHelper.hasNotificationPermission(context)
                                ) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    onSetNotificationsEnabled(enabled)
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.onPrimary,
                                checkedTrackColor = palette.primary
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Reduced Motion Toggle
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.MotionPhotosOff,
                                contentDescription = null,
                                tint = palette.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Reduced Motion",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = palette.textPrimary
                                )
                                Text(
                                    text = "Minimize spring and progress ring animations",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = palette.textSecondary
                                )
                            }
                        }
                        Switch(
                            checked = preferences.reducedMotion,
                            onCheckedChange = onSetReducedMotion,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = palette.onPrimary,
                                checkedTrackColor = palette.primary
                            )
                        )
                    }
                }
            }
        }

        // 4. Sign Out & Account Deletion
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = palette.surface),
                border = BorderStroke(1.dp, palette.border),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Account Actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = palette.textPrimary
                    )

                    OutlinedButton(
                        onClick = onSignOut,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, palette.border),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("sign_out_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null,
                            tint = palette.textPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (uiState.isDemoMode) "Exit Demo Mode" else "Sign Out",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.textPrimary
                        )
                    }

                    OutlinedButton(
                        onClick = { showDeleteConfirmDialog = true },
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, palette.danger.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("delete_account_button")
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DeleteForever,
                            contentDescription = null,
                            tint = palette.danger,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Delete Account & Data",
                            style = MaterialTheme.typography.titleSmall,
                            color = palette.danger
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SwatchCircle(color: androidx.compose.ui.graphics.Color) {
    val palette = MelloThemeColors.palette
    Box(
        modifier = Modifier
            .size(22.dp)
            .clip(CircleShape)
            .background(color)
            .border(1.dp, palette.border, CircleShape)
    )
}
