package com.example.protfoliomanagerv1.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.protfoliomanagerv1.ui.theme.*

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Portfolio : Screen("portfolio", "Portfolio", Icons.Default.Home)
    object Futures : Screen("futures", "Futures", Icons.Default.Star)
    object History : Screen("history", "History", Icons.Default.Info)
}

@Composable
fun CryptoBottomNavigation(
    currentRoute: String,
    onNavigate: (String) -> Unit
) {
    val items = listOf(
        Screen.Portfolio,
        Screen.Futures,
        Screen.History
    )
    
    NavigationBar(
        containerColor = CryptoDarkSurface,
        contentColor = TextPrimary,
        tonalElevation = 8.dp
    ) {
        items.forEach { screen ->
            NavigationBarItem(
                icon = {
                    Icon(
                        imageVector = screen.icon,
                        contentDescription = screen.title
                    )
                },
                label = {
                    Text(
                        text = screen.title,
                        style = MaterialTheme.typography.labelSmall
                    )
                },
                selected = currentRoute == screen.route,
                onClick = { onNavigate(screen.route) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = NeonGreen,
                    selectedTextColor = NeonGreen,
                    indicatorColor = ProfitGreenBg,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary
                )
            )
        }
    }
}

