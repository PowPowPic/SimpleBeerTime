package com.powder.simplebeertime.ui.settings

import android.app.Activity
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun LanguageSettingDialog(
    languageViewModel: LanguageViewModel,
    onDismiss: () -> Unit
) {
    val currentLang by languageViewModel.appLanguage.collectAsState()
    val activity = LocalContext.current as? Activity
    val scope = rememberCoroutineScope()
    var pendingLang by remember { mutableStateOf<AppLanguage?>(null) }
    var showConfirm by remember { mutableStateOf(false) }

    LanguageApplyConfirmDialog(
        visible = showConfirm,
        onDismiss = {
            pendingLang = null
            showConfirm = false
        },
        onConfirm = {
            val lang = pendingLang ?: return@LanguageApplyConfirmDialog
            scope.launch {
                languageViewModel.setLanguageSuspend(lang)
                delay(150)
                restartApp(activity)
            }
        }
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.DialogBackground,
        titleContentColor = SimpleColors.TextPrimary,
        textContentColor = SimpleColors.TextPrimary,
        title = {
            Text(text = stringResource(R.string.settings_language_title))
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                LanguageOption(
                    label = stringResource(R.string.language_system_default),
                    selected = currentLang == AppLanguage.SYSTEM,
                    onSelect = {
                        pendingLang = AppLanguage.SYSTEM
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English",
                    selected = currentLang == AppLanguage.ENGLISH,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "日本語",
                    selected = currentLang == AppLanguage.JAPANESE,
                    onSelect = {
                        pendingLang = AppLanguage.JAPANESE
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Español",
                    selected = currentLang == AppLanguage.SPANISH,
                    onSelect = {
                        pendingLang = AppLanguage.SPANISH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Italiano",
                    selected = currentLang == AppLanguage.ITALIAN,
                    onSelect = {
                        pendingLang = AppLanguage.ITALIAN
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Português (Brasil)",
                    selected = currentLang == AppLanguage.PORTUGUESE_BR,
                    onSelect = {
                        pendingLang = AppLanguage.PORTUGUESE_BR
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Français",
                    selected = currentLang == AppLanguage.FRENCH,
                    onSelect = {
                        pendingLang = AppLanguage.FRENCH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Deutsch",
                    selected = currentLang == AppLanguage.GERMAN,
                    onSelect = {
                        pendingLang = AppLanguage.GERMAN
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "العربية",
                    selected = currentLang == AppLanguage.ARABIC,
                    onSelect = {
                        pendingLang = AppLanguage.ARABIC
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Bahasa Indonesia",
                    selected = currentLang == AppLanguage.INDONESIAN,
                    onSelect = {
                        pendingLang = AppLanguage.INDONESIAN
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "ภาษาไทย",
                    selected = currentLang == AppLanguage.THAI,
                    onSelect = {
                        pendingLang = AppLanguage.THAI
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Türkçe",
                    selected = currentLang == AppLanguage.TURKISH,
                    onSelect = {
                        pendingLang = AppLanguage.TURKISH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Tiếng Việt",
                    selected = currentLang == AppLanguage.VIETNAMESE,
                    onSelect = {
                        pendingLang = AppLanguage.VIETNAMESE
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "繁體中文",
                    selected = currentLang == AppLanguage.CHINESE_TRADITIONAL,
                    onSelect = {
                        pendingLang = AppLanguage.CHINESE_TRADITIONAL
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "한국어",
                    selected = currentLang == AppLanguage.KOREAN,
                    onSelect = {
                        pendingLang = AppLanguage.KOREAN
                        showConfirm = true
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
}

private fun restartApp(activity: Activity?) {
    if (activity == null) return
    val intent = activity.packageManager.getLaunchIntentForPackage(activity.packageName) ?: return
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
    activity.startActivity(intent)
    activity.finishAffinity()
}

@Composable
private fun LanguageApplyConfirmDialog(
    visible: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    if (!visible) return

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SimpleColors.DialogBackground,
        title = {
            Text(
                text = stringResource(R.string.language_confirm_title),
                color = SimpleColors.TextPrimary
            )
        },
        text = {
            Text(
                text = stringResource(R.string.language_confirm_message),
                color = SimpleColors.TextSecondary
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = stringResource(R.string.language_confirm_ok),
                    color = SimpleColors.TextPrimary
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.language_confirm_cancel),
                    color = SimpleColors.TextPrimary
                )
            }
        }
    )
}

@Composable
private fun LanguageOption(
    label: String,
    selected: Boolean,
    onSelect: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .height(48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            colors = RadioButtonDefaults.colors(
                selectedColor = SimpleColors.TextPrimary,
                unselectedColor = SimpleColors.TextSecondary
            )
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            color = SimpleColors.TextPrimary
        )
    }
}