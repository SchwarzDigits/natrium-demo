package schwarz.digits.showcase

import androidx.compose.ui.uikit.OnFocusBehavior
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
            caseLinkBase = BackendProperties.CASE_LINK,
        ),
        NatriumPlatform(),
    )
    // The default (FocusableAboveKeyboard) shifts the whole view up when a text field
    // gets focus, which pushes the top bar and back button off screen. The screens make
    // room for the keyboard themselves with keyboardPadding() (imePadding on iOS) instead.
    ComposeUIViewController(configure = { onFocusBehavior = OnFocusBehavior.DoNothing }) { App() }
}
