package schwarz.digits.showcase.platform

import android.content.Intent
import android.net.Uri
import schwarz.digits.showcase.ShowcaseApplication

actual fun openUrl(url: String) {
    val context = ShowcaseApplication.appContext
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
}
