package schwarz.digits.showcase.screen

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import schwarz.digits.natrium.session.Session
import schwarz.digits.showcase.navigation.Route
import schwarz.digits.showcase.viewmodel.ConversationsViewModel
import schwarz.digits.showcase.viewmodel.ChatViewModel
import schwarz.digits.showcase.viewmodel.DashboardViewModel
import schwarz.digits.showcase.viewmodel.SettingsViewModel

@Composable
fun MainScreen(session: Session, onLogout: () -> Unit) {
    val navController = rememberNavController()
    val currentDest by navController.currentBackStackEntryAsState()
    val currentRoute = currentDest?.destination?.route
    val conversationsViewModel = viewModel { ConversationsViewModel(session) }

    Scaffold(
        bottomBar = {
            if (currentRoute != Route.CHAT) NavigationBar {
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Home, contentDescription = "Dashboard") },
                    label = { Text("Dashboard") },
                    selected = currentRoute == Route.DASHBOARD,
                    onClick = {
                        navController.navigate(Route.DASHBOARD) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(Route.DASHBOARD) { saveState = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Conversations") },
                    label = { Text("Conversations") },
                    selected = currentRoute == Route.CONVERSATIONS,
                    onClick = {
                        navController.navigate(Route.CONVERSATIONS) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(Route.DASHBOARD) { saveState = true }
                        }
                    }
                )
                NavigationBarItem(
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") },
                    selected = currentRoute == Route.SETTINGS,
                    onClick = {
                        navController.navigate(Route.SETTINGS) {
                            launchSingleTop = true
                            restoreState = true
                            popUpTo(Route.DASHBOARD) { saveState = true }
                        }
                    }
                )
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Route.DASHBOARD,
            modifier = Modifier.padding(padding)
        ) {
            composable(Route.DASHBOARD) {
                DashboardScreen(viewModel { DashboardViewModel(session) })
            }
            composable(Route.CONVERSATIONS) {
                ConversationsScreen(
                    viewModel = conversationsViewModel,
                    onConversationClick = { ops ->
                        conversationsViewModel.selectConversation(ops)
                        navController.navigate(Route.CHAT)
                    }
                )
            }
            composable(Route.SETTINGS) {
                SettingsScreen(viewModel { SettingsViewModel(session) }, onLogout)
            }
            composable(Route.CHAT) {
                val ops = conversationsViewModel.selectedConversation.collectAsState().value
                if (ops != null) {
                    ChatScreen(
                        viewModel = viewModel { ChatViewModel(ops) },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
