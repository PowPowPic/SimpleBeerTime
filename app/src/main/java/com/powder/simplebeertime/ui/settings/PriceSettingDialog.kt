package com.powder.simplebeertime.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors

@Composable
fun PriceSettingDialog(
    currentPrice: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    var textState = remember { mutableStateOf(currentPrice.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.DialogBackground,
        titleContentColor = SimpleColors.TextPrimary,
        textContentColor = SimpleColors.TextPrimary,
        title = {
            Text(stringResource(R.string.price_dialog_title))
        },
        text = {
            Column {
                Text(
                    text = stringResource(R.string.price_dialog_message),
                    color = SimpleColors.TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                TextField(
                    value = textState.value,
                    onValueChange = { new ->
                        if (new.matches(Regex("""\d*\.?\d*"""))) {
                            textState.value = new
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedTextColor = SimpleColors.TextPrimary,
                        unfocusedTextColor = SimpleColors.TextPrimary,
                        cursorColor = SimpleColors.TextPrimary,
                        focusedIndicatorColor = SimpleColors.ButtonPrimary,
                        unfocusedIndicatorColor = SimpleColors.TextSecondary
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = textState.value.toFloatOrNull()
                    if (value != null) {
                        onConfirm(value)
                    }
                    onDismiss()
                }
            ) {
                Text(
                    text = stringResource(R.string.common_ok),
                    color = SimpleColors.TextPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.common_cancel),
                    color = SimpleColors.TextPrimary
                )
            }
        }
    )
}