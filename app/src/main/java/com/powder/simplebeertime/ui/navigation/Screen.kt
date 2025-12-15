package com.powder.simplebeertime.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.ui.graphics.vector.ImageVector
import com.powder.simplebeertime.R

sealed class Screen(
    val route: String,
    val icon: ImageVector,
    val labelResId: Int
) {

    object Main : Screen(
        route = "main",
        icon = Icons.Filled.Home,
        labelResId = R.string.nav_home
    )

    object History : Screen(
        route = "history",
        icon = Icons.Filled.History,
        labelResId = R.string.nav_history
    )

    object Calendar : Screen(
        route = "calendar",
        icon = Icons.Filled.CalendarMonth,
        labelResId = R.string.nav_calendar
    )

    object Graph : Screen(
        route = "graph",
        icon = Icons.Filled.ShowChart,
        labelResId = R.string.nav_graph
    )

    object Settings : Screen(
        route = "settings",
        icon = Icons.Filled.Settings,
        labelResId = R.string.nav_settings
    )
}