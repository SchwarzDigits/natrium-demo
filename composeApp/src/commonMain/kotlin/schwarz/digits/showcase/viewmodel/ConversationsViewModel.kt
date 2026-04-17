package schwarz.digits.showcase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.conversation.ConversationOperations
import schwarz.digits.natrium.conversation.CreateConversationResult
import schwarz.digits.natrium.conversation.GetConversationInfoResult
import schwarz.digits.natrium.conversation.DeleteConversationResult
import schwarz.digits.natrium.conversation.GetJoinLinkResult
import schwarz.digits.natrium.conversation.JoinConversationResult
import schwarz.digits.natrium.conversation.JoinLink
import schwarz.digits.natrium.session.Session

data class ConversationItem(
    val operations: ConversationOperations,
    val title: String,
    val isArchived: Boolean,
)

data class ConversationsUiState(
    val conversations: List<ConversationItem> = emptyList(),
    val isLoading: Boolean = true,
    val showCreateDialog: Boolean = false,
    val createError: String? = null,
    val showJoinDialog: Boolean = false,
    val joinError: String? = null,
    val isJoining: Boolean = false,
    val showActionsDialog: ConversationItem? = null,
    val showConfirmDelete: ConversationItem? = null,
    val deleteError: String? = null,
    val showShareDialog: Boolean = false,
    val isLoadingJoinLink: Boolean = false,
    val joinCode: String? = null,
    val joinLinkError: String? = null,
)

class ConversationsViewModel(private val session: Session) : ViewModel() {

    private val _uiState = MutableStateFlow(ConversationsUiState())
    val uiState: StateFlow<ConversationsUiState> = _uiState.asStateFlow()

    private val _selectedConversation = MutableStateFlow<ConversationOperations?>(null)
    val selectedConversation: StateFlow<ConversationOperations?> = _selectedConversation.asStateFlow()

    fun selectConversation(ops: ConversationOperations) {
        _selectedConversation.value = ops
    }

    fun openCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = true, createError = null) }
    }

    fun dismissCreateDialog() {
        _uiState.update { it.copy(showCreateDialog = false, createError = null) }
    }

    fun openJoinDialog() {
        _uiState.update { it.copy(showJoinDialog = true, joinError = null) }
    }

    fun dismissJoinDialog() {
        _uiState.update { it.copy(showJoinDialog = false, joinError = null) }
    }

    fun showActions(item: ConversationItem) {
        _uiState.update { it.copy(showActionsDialog = item) }
    }

    fun dismissActions() {
        _uiState.update { it.copy(showActionsDialog = null) }
    }

    fun requestDeleteConversation() {
        val item = _uiState.value.showActionsDialog ?: return
        _uiState.update { it.copy(showActionsDialog = null, showConfirmDelete = item, deleteError = null) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showConfirmDelete = null, deleteError = null) }
    }

    fun confirmDeleteConversation() {
        val item = _uiState.value.showConfirmDelete ?: return
        viewModelScope.launch {
            when (item.operations.delete()) {
                is DeleteConversationResult.Success ->
                    _uiState.update { it.copy(showConfirmDelete = null, deleteError = null) }
                is DeleteConversationResult.Failure ->
                    _uiState.update { it.copy(deleteError = "Fehler beim Löschen der Conversation") }
            }
        }
    }

    fun requestShareConversation() {
        val item = _uiState.value.showActionsDialog ?: return
        _uiState.update { it.copy(showActionsDialog = null, showShareDialog = true, isLoadingJoinLink = true, joinLinkError = null, joinCode = null) }
        viewModelScope.launch {
            when (val r = item.operations.getJoinLink()) {
                is GetJoinLinkResult.Success ->
                    _uiState.update { it.copy(joinCode = r.joinLink.value, isLoadingJoinLink = false) }
                is GetJoinLinkResult.Failure ->
                    _uiState.update { it.copy(joinLinkError = "Fehler beim Laden des Links", isLoadingJoinLink = false) }
            }
        }
    }

    fun dismissShareDialog() {
        _uiState.update { it.copy(showShareDialog = false, joinCode = null, joinLinkError = null, isLoadingJoinLink = false) }
    }

    fun joinConversation(code: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isJoining = true) }
            when (session.conversationManager.joinConversation(JoinLink(code.trim()))) {
                is JoinConversationResult.Success ->
                    _uiState.update { it.copy(showJoinDialog = false, joinError = null, isJoining = false) }
                is JoinConversationResult.Failure.InvalidLink ->
                    _uiState.update { it.copy(joinError = "Ungültiger Link", isJoining = false) }
                is JoinConversationResult.Failure.IncorrectPassword ->
                    _uiState.update { it.copy(joinError = "Falsches Passwort", isJoining = false) }
                is JoinConversationResult.Failure ->
                    _uiState.update { it.copy(joinError = "Fehler beim Einlösen des Links", isJoining = false) }
            }
        }
    }

    fun createConversation(title: String) {
        viewModelScope.launch {
            when (val result = session.conversationManager.createConversation(title)) {
                is CreateConversationResult.Success -> {
                    _uiState.update { it.copy(showCreateDialog = false, createError = null) }
                }
                is CreateConversationResult.Failure.InvalidTitle -> {
                    _uiState.update { it.copy(createError = result.message) }
                }
                is CreateConversationResult.Failure -> {
                    _uiState.update { it.copy(createError = "Fehler beim Erstellen der Conversation") }
                }
            }
        }
    }

    private val cancellable: Cancellable = session.conversationManager.observeConversations { updatedConversations ->
        viewModelScope.launch {
            val items = updatedConversations.map { ops ->
                val info = (ops.getConversationInfo() as? GetConversationInfoResult.Success)?.conversationInfo
                ConversationItem(
                    operations = ops,
                    title      = info?.title ?: "-",
                    isArchived = info?.isArchived ?: false,
                )
            }
            _uiState.update { it.copy(conversations = items, isLoading = false) }
        }
    }

    override fun onCleared() {
        super.onCleared()
        cancellable.cancel()
    }
}
