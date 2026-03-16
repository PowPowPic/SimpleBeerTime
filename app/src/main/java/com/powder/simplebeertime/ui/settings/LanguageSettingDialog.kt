package com.powder.simplebeertime.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.powder.simplebeertime.R
import com.powder.simplebeertime.ui.theme.SimpleColors
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.sp

@Composable
fun LanguageSettingDialog(
    languageViewModel: LanguageViewModel,
    onDismiss: () -> Unit
) {
    val currentLang by languageViewModel.appLanguage.collectAsState()
    var pendingLang by remember { mutableStateOf<AppLanguage?>(null) }
    var showConfirm by remember { mutableStateOf(false) }

    LanguageApplyConfirmDialog(
        visible = showConfirm,
        onDismiss = {
            pendingLang = null
            showConfirm = false
        },
        onConfirm = {
            // ★ AppCompatDelegate.setApplicationLocales() で即時適用
            //    restartApp() は廃止 — AppCompat が自動で Activity 再生成する
            val lang = pendingLang ?: return@LanguageApplyConfirmDialog
            languageViewModel.setLanguage(lang)
            showConfirm = false
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
                    label = "日本語",
                    selected = currentLang == AppLanguage.JAPANESE,
                    onSelect = {
                        pendingLang = AppLanguage.JAPANESE
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Español (España)",
                    selected = currentLang == AppLanguage.SPANISH,
                    onSelect = {
                        pendingLang = AppLanguage.SPANISH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Español (México)",
                    selected = currentLang == AppLanguage.SPANISH_MX,
                    onSelect = {
                        pendingLang = AppLanguage.SPANISH_MX
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

                LanguageOption(
                    label = "Polski",
                    selected = currentLang == AppLanguage.POLISH,
                    onSelect = {
                        pendingLang = AppLanguage.POLISH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Română",
                    selected = currentLang == AppLanguage.ROMANIAN,
                    onSelect = {
                        pendingLang = AppLanguage.ROMANIAN
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Oʻzbekcha",
                    selected = currentLang == AppLanguage.UZBEK,
                    onSelect = {
                        pendingLang = AppLanguage.UZBEK
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Қазақша",
                    selected = currentLang == AppLanguage.KAZAKH,
                    onSelect = {
                        pendingLang = AppLanguage.KAZAKH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "اردو",
                    selected = currentLang == AppLanguage.URDU,
                    onSelect = {
                        pendingLang = AppLanguage.URDU
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Кыргызча",
                    selected = currentLang == AppLanguage.KYRGYZ,
                    onSelect = {
                        pendingLang = AppLanguage.KYRGYZ
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Български",
                    selected = currentLang == AppLanguage.BULGARIAN,
                    onSelect = {
                        pendingLang = AppLanguage.BULGARIAN
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "Azərbaycanca",
                    selected = currentLang == AppLanguage.AZERBAIJANI,
                    onSelect = {
                        pendingLang = AppLanguage.AZERBAIJANI
                        showConfirm = true
                    }
                )

                // ── English (regional variants) ──────────────────────────
                LanguageOption(
                    label = "English",
                    selected = currentLang == AppLanguage.ENGLISH,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (US)",
                    selected = currentLang == AppLanguage.ENGLISH_US,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_US
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (UK)",
                    selected = currentLang == AppLanguage.ENGLISH_GB,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_GB
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (Canada)",
                    selected = currentLang == AppLanguage.ENGLISH_CA,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_CA
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (Australia)",
                    selected = currentLang == AppLanguage.ENGLISH_AU,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_AU
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (India)",
                    selected = currentLang == AppLanguage.ENGLISH_IN,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_IN
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (Philippines)",
                    selected = currentLang == AppLanguage.ENGLISH_PH,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_PH
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (Singapore)",
                    selected = currentLang == AppLanguage.ENGLISH_SG,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_SG
                        showConfirm = true
                    }
                )

                LanguageOption(
                    label = "English (South Africa)",
                    selected = currentLang == AppLanguage.ENGLISH_ZA,
                    onSelect = {
                        pendingLang = AppLanguage.ENGLISH_ZA
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

// ★ restartApp() は廃止 — AppCompatDelegate.setApplicationLocales() が自動で再生成する

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
            .padding(vertical = 0.dp),  // 縦の余白なし
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
            modifier = Modifier.size(36.dp),  // 全体サイズを36dpに縮小
            colors = RadioButtonDefaults.colors(
                selectedColor = SimpleColors.TextPrimary,
                unselectedColor = SimpleColors.TextSecondary
            )
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            color = SimpleColors.TextPrimary,
            fontSize = 15.sp
        )
    }
}
