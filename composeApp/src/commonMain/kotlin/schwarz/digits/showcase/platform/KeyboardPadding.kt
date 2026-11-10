package schwarz.digits.showcase.platform

import androidx.compose.ui.Modifier

/**
 * Makes room for the software keyboard where the platform does not do it itself.
 *
 * iOS: imePadding — MainViewController turns off Compose's own shift of the whole view,
 * which pushed the top bar off screen. Android: nothing, the window already resizes for
 * the keyboard and imePadding on top of that left a gap. Desktop: nothing.
 */
expect fun Modifier.keyboardPadding(): Modifier
