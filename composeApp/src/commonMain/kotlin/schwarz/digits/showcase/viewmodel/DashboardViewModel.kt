package schwarz.digits.showcase.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.session.Session

data class DashboardUiState(val userName: String? = null)

class DashboardViewModel(session: Session) : ViewModel() {
    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    private val cancellable: Cancellable = session.observeSessionInfo { info ->
        _uiState.update { it.copy(userName = info.user.name) }
    }

    override fun onCleared() {
        super.onCleared()
        cancellable.cancel()
    }
}
