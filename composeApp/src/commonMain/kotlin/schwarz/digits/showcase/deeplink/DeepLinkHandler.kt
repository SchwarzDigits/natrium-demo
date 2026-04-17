package schwarz.digits.showcase.deeplink

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object DeepLinkHandler {
    private val _deepLinks = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val deepLinks: SharedFlow<String> = _deepLinks.asSharedFlow()

    fun handleDeepLink(uri: String) {
        _deepLinks.tryEmit(uri)
    }
}
