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

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import java.util.Locale




@Composable
fun EditDayAmountDialog(
    currentAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    // ✅ 初期値：0.0なら 1.4、0より大きいなら現在値
    var text by remember(currentAmount) {
        mutableStateOf(
            if (currentAmount > 0.0) {
                String.format(Locale.getDefault(), "%.1f", currentAmount)
            } else {
                "1.4"
            }
        )
    }

    // 入力値の解析
    val parsed = text.trim().toDoubleOrNull()

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
                    text = "About",
                    color = SimpleColors.TextSecondary
                )

                Spacer(modifier = Modifier.width(8.dp))

                OutlinedTextField(
                    value = text,
                    onValueChange = { input ->
                        // ✅ 数字・小数点だけ許可（ゆるめ）
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        text = filtered
                    },
                    singleLine = true,
                    modifier = Modifier.width(110.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )

                Spacer(modifier = Modifier.width(8.dp))

                Text(
                    text = "beers",
                    color = SimpleColors.TextSecondary
                )
            }
        }
    )
}
