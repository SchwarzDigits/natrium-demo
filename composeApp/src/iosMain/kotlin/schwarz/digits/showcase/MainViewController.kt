package schwarz.digits.showcase

import androidx.compose.ui.window.ComposeUIViewController
import schwarz.digits.natrium.BackendConfig
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.NatriumPlatform

fun MainViewController() = run {
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
    ComposeUIViewController { App() }
}
