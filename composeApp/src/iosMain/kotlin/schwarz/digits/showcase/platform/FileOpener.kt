package schwarz.digits.showcase.platform

import platform.Foundation.NSURL
import platform.UIKit.UIApplication

actual fun openFile(path: String) {
    val url = NSURL.fileURLWithPath(path)
    UIApplication.sharedApplication.openURL(url)
}
