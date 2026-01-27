package com.banyg

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.banyg.navigation.BanygNavHost
import com.banyg.navigation.BottomNavDestination
import com.banyg.ui.components.BanygBottomNav
import com.banyg.ui.components.BottomNavItem
import com.banyg.ui.theme.BanygTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BanygApp()
        }
    }
}

@Composable
fun BanygApp() {
    BanygTheme {
        val navController = rememberNavController()
        
        // Get current back stack entry to determine selected tab
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination
        
        // Determine selected index based on current route
        val selectedIndex = BottomNavDestination.entries.indexOfFirst { destination ->
            currentDestination?.hierarchy?.any { it.route == destination.route } == true
        }.coerceAtLeast(0)
        
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = BanygTheme.colors.backgroundDark,
            bottomBar = {
                BanygBottomNav(
                    selectedIndex = selectedIndex,
                    onItemSelected = { index ->
                        val destination = BottomNavDestination.entries[index]
                        navController.navigate(destination.route) {
                            // Pop up to the start destination of the graph to
                            // avoid building up a large stack of destinations
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            // Avoid multiple copies of the same destination when
                            // reselecting the same item
                            launchSingleTop = true
                            // Restore state when reselecting a previously selected item
                            restoreState = true
                        }
                    },
                    items = BottomNavDestination.entries.map { destination ->
                        BottomNavItem(
                            icon = destination.icon,
                            contentDescription = destination.label
                        )
                    }
                )
            }
        ) { paddingValues ->
            BanygNavHost(
                navController = navController,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun BanygAppPreview() {
    BanygTheme {
        BanygApp()
    }
}
