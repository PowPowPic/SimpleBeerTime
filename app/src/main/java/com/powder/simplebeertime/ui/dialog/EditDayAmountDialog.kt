package com.powder.simplebeertime.ui.dialog

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors

import java.text.NumberFormat
import java.util.Locale




@Composable
fun EditDayAmountDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    // ★ ロケール対応: 初期値をロケールの小数点記号で表示
    var text by remember(currentAmount) {
        mutableStateOf(
            if (currentAmount > 0.0) {
                String.format(if (Locale.getDefault().country == "ZA") Locale.US else Locale.getDefault(), "%.1f", currentAmount)
            } else {
                // ★ ロケール対応: デフォルト値もロケール表記
                String.format(if (Locale.getDefault().country == "ZA") Locale.US else Locale.getDefault(), "%.1f", 1.4)
            }
        )
    }

    // ★ ロケール対応: NumberFormat.parse()でカンマ小数点も正しくパース
    val parsed = try {
        NumberFormat.getInstance(Locale.getDefault()).parse(text.trim())?.toDouble()
    } catch (e: Exception) {
        text.trim().replace(',', '.').toDoubleOrNull()
    }

    // ✅ 小数1桁に丸めて使う（安全策）
    val normalized: Double? = parsed?.let { v ->
        if (v < 0.0) null else kotlin.math.round(v * 10.0) / 10.0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.edit_day_dialog_title)) },
        confirmButton = {
            TextButton(
                onClick = { normalized?.let(onConfirm) },
                enabled = normalized != null
            ) {
                Text(text = stringResource(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(android.R.string.cancel))
            }
        },
        text = {
            // About [   ] beers の並び
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.edit_day_about_label),
                    color = SimpleColors.TextSecondary
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        // ★ ロケール対応: ドットとカンマの両方を許可
                        val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
                        text = filtered
                    },
                    singleLine = true,
                    modifier = Modifier.width(110.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = stringResource(R.string.main_amount_suffix),
                    color = SimpleColors.TextSecondary
                )
            }
        }
    )
}
