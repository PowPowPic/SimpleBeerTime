package com.powder.simplebeertime.ui.settings

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors

@Composable
fun DeleteAllConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirmDeleteAll: () -> Unit,
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.DialogBackground,
        titleContentColor = SimpleColors.TextPrimary,
        textContentColor = SimpleColors.TextPrimary,

        title = {
            Text(text = stringResource(R.string.delete_all_title))
        },
        text = {
            Text(text = stringResource(R.string.delete_all_message))
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmDeleteAll()
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.delete_all_confirm),
                    color = SimpleColors.PureRed
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.delete_all_cancel),
                    color = SimpleColors.TextPrimary
                )
            }
        }
    )
}