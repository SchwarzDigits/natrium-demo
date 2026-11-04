package schwarz.digits.showcase.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.session.Session

/** [updateRecommended]: a newer protocol version than this app's arrived, see Session.observeUpdateRecommended. */
data class DashboardUiState(val userName: String? = null, val updateRecommended: Boolean = false)

class DashboardViewModel(session: Session) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val cancellable: Cancellable = session.observeSessionInfo { info ->
        _uiState.update { it.copy(userName = info.user.name) }
    }

    private val updates: Cancellable = session.observeUpdateRecommended {
        _uiState.update { it.copy(updateRecommended = true) }
    }

    override fun onCleared() {
        super.onCleared()
        cancellable.cancel()
        updates.cancel()
    }
}
