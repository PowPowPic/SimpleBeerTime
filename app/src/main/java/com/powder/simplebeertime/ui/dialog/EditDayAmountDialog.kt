package com.powder.simplebeertime.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors
import kotlin.math.roundToInt

@Composable
fun EditDayAmountDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(
        if (currentAmount > 0.0) String.format("%.1f", currentAmount) else ""
    ) }

    // 入力値のバリデーション
    val parsedAmount = amountText.toDoubleOrNull()
    val isValid = parsedAmount != null && parsedAmount >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.DialogBackground,
        titleContentColor = SimpleColors.TextPrimary,
        title = {
            Text(text = stringResource(R.string.edit_day_dialog_title))
        },
        text = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.main_amount_prefix),
                    color = SimpleColors.TextPrimary
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = amountText,
                    onValueChange = { new ->
                        // 数字と小数点のみ許可
                        if (new.isEmpty() || new.matches(Regex("""^\d*\.?\d*$"""))) {
                            amountText = new
                        }
                    },
                    modifier = Modifier.width(80.dp),
                    singleLine = true,
                    placeholder = {
                        Text(
                            text = "1.4",
                            color = SimpleColors.TextSecondary
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Decimal,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            if (isValid && parsedAmount != null) {
                                val rounded = (parsedAmount * 10).roundToInt() / 10.0
                                onConfirm(rounded)
                            }
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = SimpleColors.TextPrimary,
                        unfocusedTextColor = SimpleColors.TextPrimary,
                        cursorColor = SimpleColors.TextPrimary,
                        focusedBorderColor = SimpleColors.ButtonPrimary,
                        unfocusedBorderColor = SimpleColors.TextSecondary
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.main_amount_suffix),
                    color = SimpleColors.TextPrimary
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (isValid && parsedAmount != null) {
                        val rounded = (parsedAmount * 10).roundToInt() / 10.0
                        onConfirm(rounded)
                    }
                },
                enabled = isValid
            ) {
                Text(
                    text = stringResource(R.string.common_ok),
                    color = if (isValid) SimpleColors.PureBlue else SimpleColors.TextSecondary
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