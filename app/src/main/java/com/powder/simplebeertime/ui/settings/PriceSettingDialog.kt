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
import com.powder.simplebeertime.ui.settings.getInputMaxDecimalPlaces
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
    // ★ 通貨ごとの最大小数桁数を取得（JPY/KRW/TWD/KZT/KGS→1, IDR/VND/UZS→0, USD等→2）
    //   従来: getCurrencyFractionDigits() → 整数通貨は0桁固定
    //   変更後: getInputMaxDecimalPlaces() → DECIMAL1通貨は1桁まで許可
    val maxDecimals = remember { getInputMaxDecimalPlaces() }

    var textState = remember {
        mutableStateOf(
            if (maxDecimals == 0) {
                String.format(locale, "%d", Math.round(currentPrice))
            } else {
                // ★ maxDecimalsに応じたフォーマット文字列を使用
                //   例: JPY(maxDecimals=1) → "%.1f", USD(maxDecimals=2) → "%.2f"
                String.format(locale, "%.${maxDecimals}f", currentPrice)
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
                        // ★ 通貨ごとの小数桁数制限付きバリデーション
                        //   maxDecimals=0 → 整数のみ（IDR/VND/UZS）
                        //   maxDecimals=1 → 小数点第1位まで（JPY/KRW/TWD/KZT/KGS）
                        //   maxDecimals=2 → 小数点第2位まで（USD/EUR等、従来通り）
                        val isValid = if (maxDecimals == 0) {
                            new.matches(Regex("""\d*"""))
                        } else {
                            new.matches(Regex("""\d*[.,]?\d{0,$maxDecimals}"""))
                        }
                        if (isValid) {
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
