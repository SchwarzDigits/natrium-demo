package schwarz.digits.showcase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import schwarz.digits.natrium.BackendConfig
import schwarz.digits.natrium.Natrium
import schwarz.digits.natrium.devices.DeviceInfo
import schwarz.digits.natrium.devices.DeviceLimitResolver
import schwarz.digits.natrium.devices.ListDevicesResult
import schwarz.digits.natrium.devices.RemoveDeviceResult
import schwarz.digits.natrium.session.LoginError
import schwarz.digits.natrium.session.LoginResult
import schwarz.digits.natrium.session.SSOLoginError
import schwarz.digits.natrium.session.SSOLoginResult
import schwarz.digits.natrium.session.Session
import schwarz.digits.showcase.deeplink.DeepLinkHandler

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val secondFactorCode: String = "",
    val requiresSecondFactor: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showDeviceList: Boolean = false,
    val devices: List<DeviceInfo> = emptyList(),
    val ssoCode: String = "",
    val ssoAuthorizationUrl: String? = null,
    val ssoCookie: String = "",
)

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var deviceLimitResolver: DeviceLimitResolver? = null
    private var deepLinkJob: Job? = null

    fun onEmailChanged(email: String) {
        _uiState.update { it.copy(email = email) }
    }

    fun onPasswordChanged(password: String) {
        _uiState.update { it.copy(password = password) }
    }

    fun onSecondFactorCodeChanged(code: String) {
        _uiState.update { it.copy(secondFactorCode = code) }
    }

    fun login(onLoginSuccess: (Session) -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = Natrium.login(
                email = _uiState.value.email,
                password = _uiState.value.password,
                secondFactorVerificationCode = _uiState.value.secondFactorCode.ifBlank { null },
            )
            handleLoginResult(result, onLoginSuccess)
        }
    }

    fun onSsoCodeChanged(code: String) {
        _uiState.update { it.copy(ssoCode = code) }
    }

    fun onSsoCookieChanged(cookie: String) {
        _uiState.update { it.copy(ssoCookie = cookie) }
    }

    fun ssoLoginWithCode(onLoginSuccess: (Session) -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = Natrium.ssoLoginWithCode(_uiState.value.ssoCode)
            handleSsoResult(result, onLoginSuccess)
        }
    }

    fun ssoLogin(onLoginSuccess: (Session) -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = Natrium.ssoLogin(_uiState.value.email)
            handleSsoResult(result, onLoginSuccess)
        }
    }

    private fun handleSsoResult(result: SSOLoginResult, onLoginSuccess: (Session) -> Unit) {
        when (result) {
            is SSOLoginResult.Success -> {
                _uiState.update {
                    it.copy(
                        ssoAuthorizationUrl = result.authorizationUrl,
                        isLoading = false,
                    )
                }
                observeDeepLinks(onLoginSuccess)
            }
            is SSOLoginResult.Failure.Error -> {
                val message = when (result.reason) {
                    SSOLoginError.SSO_NOT_AVAILABLE -> "SSO is not available for this domain"
                    SSOLoginError.INVALID_CODE -> "Invalid SSO code"
                    SSOLoginError.INVALID_CODE_FORMAT -> "Invalid SSO code format"
                    SSOLoginError.SERVER_VERSION_NOT_SUPPORTED -> "Server version not supported"
                    SSOLoginError.APP_UPDATE_REQUIRED -> "App update required"
                    SSOLoginError.CONNECTION_ERROR -> "Connection error"
                }
                _uiState.update { it.copy(errorMessage = message, isLoading = false) }
            }
        }
    }

    fun completeSsoLogin(onLoginSuccess: (Session) -> Unit) {
        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        viewModelScope.launch {
            val result = Natrium.completeSSOLogin(_uiState.value.ssoCookie)
            handleLoginResult(result, onLoginSuccess)
        }
    }

    fun cancelSso() {
        deepLinkJob?.cancel()
        deepLinkJob = null
        _uiState.update { it.copy(ssoAuthorizationUrl = null, ssoCookie = "", errorMessage = null) }
    }

    private fun observeDeepLinks(onLoginSuccess: (Session) -> Unit) {
        deepLinkJob?.cancel()
        deepLinkJob = viewModelScope.launch {
            DeepLinkHandler.deepLinks.collect { uri ->
                when {
                    uri.startsWith("wire://sso-login/success") -> {
                        val cookie = extractQueryParam(uri, "cookie")
                        if (cookie != null) {
                            _uiState.update { it.copy(ssoCookie = cookie) }
                            completeSsoLogin(onLoginSuccess)
                        }
                    }
                    uri.startsWith("wire://sso-login/failure") -> {
                        val errorCode = extractQueryParam(uri, "errorCode") ?: "Unknown error"
                        _uiState.update { it.copy(errorMessage = "SSO failed: $errorCode", isLoading = false) }
                    }
                }
            }
        }
    }

    private fun extractQueryParam(uri: String, param: String): String? {
        val queryStart = uri.indexOf('?')
        if (queryStart == -1) return null
        val query = uri.substring(queryStart + 1)
        return query.split('&')
            .map { it.split('=', limit = 2) }
            .firstOrNull { it[0] == param }
            ?.getOrNull(1)
            ?.decodeUrlComponent()
    }

    private fun String.decodeUrlComponent(): String = buildString {
        var i = 0
        while (i < this@decodeUrlComponent.length) {
            when (val c = this@decodeUrlComponent[i]) {
                '%' -> {
                    if (i + 2 < this@decodeUrlComponent.length) {
                        val hex = this@decodeUrlComponent.substring(i + 1, i + 3)
                        val code = hex.toIntOrNull(16)
                        if (code != null) {
                            append(code.toChar())
                            i += 3
                            continue
                        }
                    }
                    append(c)
                    i++
                }
                '+' -> {
                    append(' ')
                    i++
                }
                else -> {
                    append(c)
                    i++
                }
            }
        }
    }

    fun removeDeviceAndRetry(deviceId: String, onLoginSuccess: (Session) -> Unit) {
        val resolver = deviceLimitResolver ?: return
        _uiState.update { it.copy(isLoading = true, errorMessage = null, showDeviceList = false) }
        viewModelScope.launch {
            when (val removeResult = resolver.removeDevice(deviceId, _uiState.value.password)) {
                is RemoveDeviceResult.Success -> {
                    handleLoginResult(resolver.retry(), onLoginSuccess)
                }
                is RemoveDeviceResult.Failure.PasswordRequired -> {
                    _uiState.update { it.copy(errorMessage = "Password required to remove device", isLoading = false, showDeviceList = true) }
                }
                is RemoveDeviceResult.Failure.InvalidCredentials -> {
                    _uiState.update { it.copy(errorMessage = "Invalid credentials", isLoading = false, showDeviceList = true) }
                }
                is RemoveDeviceResult.Failure -> {
                    _uiState.update { it.copy(errorMessage = "Failed to remove device", isLoading = false, showDeviceList = true) }
                }
            }
        }
    }

    private suspend fun handleLoginResult(result: LoginResult, onLoginSuccess: (Session) -> Unit) {
        when (result) {
            is LoginResult.Success -> onLoginSuccess(result.session)
            is LoginResult.Failure.TooManyDevices -> {
                deviceLimitResolver = result.resolver
                when (val listResult = result.resolver.listDevices()) {
                    is ListDevicesResult.Success -> {
                        _uiState.update {
                            it.copy(
                                showDeviceList = true,
                                devices = listResult.devices,
                                errorMessage = null,
                                isLoading = false,
                            )
                        }
                    }
                    is ListDevicesResult.Failure -> {
                        _uiState.update { it.copy(errorMessage = "Too many devices, but failed to load device list", isLoading = false) }
                    }
                }
            }
            is LoginResult.Failure.Error -> {
                if (result.reason == LoginError.SECOND_FA_CODE_REQUIRED) {
                    _uiState.update { it.copy(requiresSecondFactor = true, errorMessage = null, isLoading = false) }
                } else {
                    _uiState.update { it.copy(errorMessage = result.reason.name, isLoading = false) }
                }
            }
        }
    }
}
