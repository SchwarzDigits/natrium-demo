package schwarz.digits.showcase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import schwarz.digits.natrium.caselink.GenerateCaseLinkResult
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import schwarz.digits.natrium.session.Session
import schwarz.digits.natrium.session.UpdateDisplayNameResult
import schwarz.digits.natrium.session.UpdateEmailResult
import schwarz.digits.natrium.session.UpdateHandleResult

data class SettingsUiState(
    val userId: String = "",
    val displayName: String = "",
    val handle: String = "",
    val email: String = "",
    val isUpdating: Boolean = false,
    val updateMessage: String? = null,
    val caseLink: String? = null,
    val caseLinkError: String? = null,
)

class SettingsViewModel(private val session: Session) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val info = session.sessionInfo()
            if (info != null) {
                _uiState.value = _uiState.value.copy(
                    userId = info.user.userId.toString(),
                    displayName = info.user.name ?: "",
                    handle = info.handle ?: "",
                    email = info.email ?: ""
                )
            }
        }
    }

    /**
     * "Behördenlink generieren": a new case link on every press. The demo has no ids of its own to
     * attach a link to, so every press passes a new random UUID as the link's ref.
     */
    @OptIn(ExperimentalUuidApi::class)
    fun generateCaseLink() {
        viewModelScope.launch {
            _uiState.value = when (val r = session.generateCaseLink(ref = Uuid.random().toString())) {
                is GenerateCaseLinkResult.Success ->
                    _uiState.value.copy(caseLink = r.caseLink.url, caseLinkError = null)
                GenerateCaseLinkResult.Failure.NotConfigured ->
                    _uiState.value.copy(caseLink = null, caseLinkError = "Not configured: set backend.caseLink in local.properties.")
                GenerateCaseLinkResult.Failure.InvalidRef ->
                    _uiState.value.copy(caseLink = null, caseLinkError = "Invalid ref.")
                GenerateCaseLinkResult.Failure.NotLoggedIn ->
                    _uiState.value.copy(caseLink = null, caseLinkError = "Not logged in.")
                is GenerateCaseLinkResult.Failure.Unknown ->
                    _uiState.value.copy(caseLink = null, caseLinkError = r.message)
            }
        }
    }

    fun onDisplayNameChange(name: String) {
        _uiState.value = _uiState.value.copy(displayName = name, updateMessage = null)
    }

    fun onHandleChange(handle: String) {
        _uiState.value = _uiState.value.copy(handle = handle, updateMessage = null)
    }

    fun onEmailChange(email: String) {
        _uiState.value = _uiState.value.copy(email = email, updateMessage = null)
    }

    fun updateDisplayName() {
        val name = _uiState.value.displayName.trim()
        if (name.isBlank()) return
        _uiState.value = _uiState.value.copy(isUpdating = true, updateMessage = null)
        viewModelScope.launch {
            when (val result = session.updateDisplayName(name)) {
                is UpdateDisplayNameResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Display name updated"
                    )
                }
                is UpdateDisplayNameResult.Failure.Unknown -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Failed: ${result.message}"
                    )
                }
            }
        }
    }

    fun updateHandle() {
        val handle = _uiState.value.handle.trim()
        if (handle.isBlank()) return
        _uiState.value = _uiState.value.copy(isUpdating = true, updateMessage = null)
        viewModelScope.launch {
            when (val result = session.updateHandle(handle)) {
                is UpdateHandleResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Handle updated"
                    )
                }
                is UpdateHandleResult.Failure.InvalidHandle -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Invalid handle format"
                    )
                }
                is UpdateHandleResult.Failure.HandleExists -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Handle already taken"
                    )
                }
                is UpdateHandleResult.Failure.Unknown -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Failed: ${result.message}"
                    )
                }
            }
        }
    }

    fun updateEmail() {
        val email = _uiState.value.email.trim()
        if (email.isBlank()) return
        _uiState.value = _uiState.value.copy(isUpdating = true, updateMessage = null)
        viewModelScope.launch {
            when (val result = session.updateEmail(email)) {
                is UpdateEmailResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Verification email sent — check your inbox"
                    )
                }
                is UpdateEmailResult.Failure.InvalidEmail -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Invalid email address"
                    )
                }
                is UpdateEmailResult.Failure.EmailAlreadyInUse -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Email already in use"
                    )
                }
                is UpdateEmailResult.Failure.Unknown -> {
                    _uiState.value = _uiState.value.copy(
                        isUpdating = false,
                        updateMessage = "Failed: ${result.message}"
                    )
                }
            }
        }
    }

    fun logout(onLogoutSuccess: () -> Unit) {
        viewModelScope.launch {
            session.logout()
            onLogoutSuccess()
        }
    }
}
