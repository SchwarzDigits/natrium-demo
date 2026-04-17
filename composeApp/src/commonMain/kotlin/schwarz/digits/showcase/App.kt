package schwarz.digits.showcase

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import schwarz.digits.natrium.AuthEvent
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.session.Session
import schwarz.digits.showcase.navigation.Route
import schwarz.digits.showcase.screen.LoginScreen
import schwarz.digits.showcase.screen.MainScreen

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            val navController = rememberNavController()
            var session by remember { mutableStateOf<Session?>(null) }
            var isRestoring by remember { mutableStateOf(true) }

            DisposableEffect(Unit) {
                val cancellable = Natrium.observeAuthEvents { event ->
                    when (event) {
                        is AuthEvent.LoggedIn -> {
                            println("[Showcase] AuthEvent: LoggedIn")
                            session = event.session
                        }
                        is AuthEvent.LoggedOut -> {
                            println("[Showcase] AuthEvent: LoggedOut")
                            session = null
                        }
                    }
                }
                onDispose { cancellable.cancel() }
            }

            LaunchedEffect(Unit) {
                Natrium.restoreLastSession()
                isRestoring = false
            }

            LaunchedEffect(session, isRestoring) {
                if (!isRestoring && session == null) {
                    val currentRoute = navController.currentBackStackEntry?.destination?.route
                    if (currentRoute != Route.LOGIN) {
                        navController.navigate(Route.LOGIN) {
                            popUpTo(Route.MAIN) { inclusive = true }
                        }
                    }
                }
            }

            if (isRestoring) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                val startDestination = if (session != null) Route.MAIN else Route.LOGIN

                NavHost(navController = navController, startDestination = startDestination) {
                    composable(Route.LOGIN) {
                        LoginScreen(
                            onLoginSuccess = { loginSession ->
                                session = loginSession
                                navController.navigate(Route.MAIN) {
                                    popUpTo(Route.LOGIN) { inclusive = true }
                                }
                            }
                        )
                    }
                    composable(Route.MAIN) {
                        session?.let { activeSession ->
                            MainScreen(
                                session = activeSession,
                                onLogout = {
                                    session = null
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
