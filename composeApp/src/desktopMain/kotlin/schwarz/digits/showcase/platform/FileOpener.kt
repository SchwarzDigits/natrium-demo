package schwarz.digits.showcase.platform

import java.awt.Desktop
import java.io.File

actual fun openFile(path: String) {
    Desktop.getDesktop().open(File(path))
}
