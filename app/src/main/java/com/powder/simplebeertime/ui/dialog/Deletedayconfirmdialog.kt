package com.powder.simplebeertime.ui.dialog

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors

@Composable
fun DeleteDayConfirmDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.DialogBackground,
        titleContentColor = SimpleColors.TextPrimary,
        textContentColor = SimpleColors.TextPrimary,
        title = {
            Text(text = stringResource(R.string.delete_day_dialog_title))
        },
        text = {
            Text(text = stringResource(R.string.delete_day_dialog_message))
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.history_delete_yes),
                    color = SimpleColors.PureRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.history_delete_no),
                    color = SimpleColors.TextPrimary
                )
            }
        }
    )
}