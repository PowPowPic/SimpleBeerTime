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
import com.powder.simplebeertime.ui.settings.getCurrentLocale
import com.powder.simplebeertime.ui.theme.SimpleColors
import java.text.NumberFormat
import java.util.Locale

@Composable
fun PriceSettingDialog(
    currentPrice: Float,
    onConfirm: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    val locale = getCurrentLocale()
    val fractionDigits = getCurrencyFractionDigits()
    var textState = remember {
        mutableStateOf(
            if (fractionDigits == 0) {
                String.format(locale, "%d", Math.round(currentPrice))
            } else {
                String.format(locale, "%.${fractionDigits}f", currentPrice)
                    .trimEnd('0').trimEnd('.').trimEnd(',')
            }
        )
    }

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
                        // ★ ロケール対応: ドットとカンマの両方を許可
                        if (new.matches(Regex("""\d*[.,]?\d*"""))) {
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
                    // ★ ロケール対応: en-ZA は実生活慣習に合わせて "." 小数点でパース
                    val parseLoc = Locale.getDefault().let {
                        if (it.country == "ZA") Locale.US else it
                    }
                    val value = try {
                        NumberFormat.getInstance(parseLoc)
                            .parse(textState.value.trim())?.toFloat()
                    } catch (e: Exception) {
                        textState.value.trim().replace(',', '.').toFloatOrNull()
                    }
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