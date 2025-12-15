package com.powder.simplebeertime.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.powder.simplebeertime.ui.theme.SimpleColors

@Composable
fun NavBar(
    navController: NavHostController,
    onSettingsClick: () -> Unit
) {
    val items = listOf(
        Screen.Main,
        Screen.History,
        Screen.Calendar,
        Screen.Graph,
        Screen.Settings
    )

    NavigationBar(
        containerColor = Color.Transparent,
        tonalElevation = 0.dp
    ) {

        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentRoute = navBackStackEntry?.destination?.route

        items.forEach { screen ->
            NavigationBarItem(
                selected = currentRoute == screen.route,
                onClick = {
                    if (screen == Screen.Settings) {
                        onSettingsClick()
                    } else {
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Main.route) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = stringResource(screen.labelResId)
                    )
                },
                label = {
                    Text(text = stringResource(screen.labelResId))
                },
                colors = NavigationBarItemDefaults.colors(
                    indicatorColor = SimpleColors.ButtonPrimary,
                    selectedIconColor = SimpleColors.TextPrimary,
                    selectedTextColor = SimpleColors.TextPrimary,
                    unselectedIconColor = SimpleColors.TextSecondary,
                    unselectedTextColor = SimpleColors.TextSecondary
                )
            )
        }
    }
}