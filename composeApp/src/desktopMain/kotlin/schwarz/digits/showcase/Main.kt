package schwarz.digits.showcase

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import schwarz.digits.natrium.BackendConfig
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.NatriumPlatform

fun main() {
    Natrium.initialize(
        BackendConfig(
            name = BackendProperties.NAME,
            api = BackendProperties.API,
            accounts = BackendProperties.ACCOUNTS,
            webSocket = BackendProperties.WEB_SOCKET,
            teams = BackendProperties.TEAMS,
            blackList = BackendProperties.BLACK_LIST,
            website = BackendProperties.WEBSITE,
        ),
        NatriumPlatform(),
    )

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Showcase"
        ) {
            App()
        }
    }
}
