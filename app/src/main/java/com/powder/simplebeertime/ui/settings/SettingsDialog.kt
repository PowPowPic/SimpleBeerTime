package com.powder.simplebeertime.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors

@Composable
fun SettingsDialog(
    onDismiss: () -> Unit,
    onLanguageSettingClick: () -> Unit = {},
    onPriceSettingClick: () -> Unit = {},
    onConfirmDeleteAll: () -> Unit = {}
) {
    val showDeleteAllDialog = remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.SettingsDialogBackground,
        titleContentColor = SimpleColors.TextPrimary,
        textContentColor = SimpleColors.TextPrimary,
        iconContentColor = SimpleColors.TextPrimary,
        icon = {
            Icon(
                imageVector = Icons.Filled.Settings,
                contentDescription = stringResource(R.string.settings_cd_dialog)
            )
        },
        title = {
            Text(text = stringResource(R.string.settings_title))
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {

                SettingsRow(
                    icon = Icons.Filled.Language,
                    title = stringResource(R.string.settings_language_title),
                    description = stringResource(R.string.settings_language_description),
                    contentDescription = stringResource(R.string.settings_cd_language),
                    onClick = {
                        onLanguageSettingClick()
                    }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SimpleColors.TextSecondary.copy(alpha = 0.5f)
                )

                SettingsRow(
                    icon = Icons.Filled.AttachMoney,
                    title = stringResource(R.string.settings_price_title),
                    description = stringResource(R.string.settings_price_description),
                    contentDescription = stringResource(R.string.settings_cd_price),
                    onClick = {
                        onPriceSettingClick()
                    }
                )

                Divider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = SimpleColors.TextSecondary.copy(alpha = 0.5f)
                )

                SettingsRow(
                    icon = Icons.Filled.DeleteForever,
                    title = stringResource(R.string.settings_delete_title),
                    description = stringResource(R.string.settings_delete_description),
                    contentDescription = stringResource(R.string.settings_cd_delete_all),
                    onClick = {
                        showDeleteAllDialog.value = true
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.close_button_text),
                    color = SimpleColors.TextPrimary
                )
            }
        }
    )

    DeleteAllConfirmDialog(
        visible = showDeleteAllDialog.value,
        onDismiss = {
            showDeleteAllDialog.value = false
        },
        onConfirmDeleteAll = {
            onConfirmDeleteAll()
            showDeleteAllDialog.value = false
        }
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    description: String,
    contentDescription: String? = null,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                modifier = Modifier.padding(end = 8.dp),
                tint = SimpleColors.TextPrimary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = SimpleColors.TextPrimary
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = description,
            style = MaterialTheme.typography.bodySmall,
            color = SimpleColors.TextSecondary
        )
    }
}