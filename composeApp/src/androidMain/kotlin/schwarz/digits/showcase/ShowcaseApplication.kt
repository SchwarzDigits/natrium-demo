package schwarz.digits.showcase

import android.app.Application
import android.content.Context
import schwarz.digits.natrium.BackendConfig
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.NatriumPlatform

class ShowcaseApplication : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
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
            NatriumPlatform(this),
        )
    }
}
