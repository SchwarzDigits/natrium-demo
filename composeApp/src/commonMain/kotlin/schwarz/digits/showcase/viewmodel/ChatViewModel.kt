package schwarz.digits.showcase.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import schwarz.digits.natrium.Cancellable
import schwarz.digits.natrium.conversation.ConversationMember
import schwarz.digits.natrium.conversation.ConversationOperations
import schwarz.digits.natrium.conversation.GetConversationInfoResult
import schwarz.digits.natrium.conversation.GetJoinLinkResult
import schwarz.digits.natrium.conversation.GetMembersResult
import schwarz.digits.natrium.chat.ChatMessage
import schwarz.digits.natrium.chat.FileDownloadResult
import schwarz.digits.natrium.chat.InvalidResponseReason
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.Response
import schwarz.digits.natrium.chat.SendMessageResult
import schwarz.digits.natrium.file.FileLink
import okio.FileSystem
import okio.Path

data class ChatUiState(
    val title: String = "",
    val messages: List<ChatMessage> = emptyList(),
    val inputText: String = "",
    val isLoading: Boolean = true,
    val typingUsers: List<String> = emptyList(),
    val members: List<ConversationMember> = emptyList(),
    val joinCode: String? = null,
    val isLoadingJoinLink: Boolean = false,
    val joinLinkError: String? = null,
    val sendError: String? = null,
    val replyingTo: ChatMessage? = null,
    val pendingAttachments: List<PickedFile> = emptyList(),
)

class ChatViewModel(private val conversationOperations: ConversationOperations) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var messagesCancellable: Cancellable? = null
    private var typingCancellable: Cancellable? = null

    init {
        viewModelScope.launch {
            val title = (conversationOperations.getConversationInfo() as? GetConversationInfoResult.Success)
                ?.conversationInfo?.title ?: ""
            _uiState.update { it.copy(title = title) }

            val membersResult = conversationOperations.getMembers()
            if (membersResult is GetMembersResult.Success) {
                _uiState.update { it.copy(members = membersResult.members) }
            }

            messagesCancellable = conversationOperations.chat().observeMessages { msgs ->
                _uiState.update {
                    it.copy(
                        messages = msgs.sortedBy { m -> m.timestamp },
                        isLoading = false,
                    )
                }
            }

            typingCancellable = conversationOperations.chat().observeTyping { users ->
                _uiState.update { it.copy(typingUsers = users.map { u -> u.name ?: "" }) }
            }
        }
    }

    fun getJoinLink() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingJoinLink = true, joinLinkError = null) }
            when (val r = conversationOperations.getJoinLink()) {
                is GetJoinLinkResult.Success ->
                    _uiState.update { it.copy(joinCode = r.joinLink.value, isLoadingJoinLink = false) }
                is GetJoinLinkResult.Failure ->
                    _uiState.update { it.copy(joinLinkError = "Fehler beim Laden des Links", isLoadingJoinLink = false) }
            }
        }
    }

    fun revokeJoinLink() {
        viewModelScope.launch {
            conversationOperations.revokeJoinLink()
            _uiState.update { it.copy(joinCode = null) }
        }
    }

    fun dismissJoinLinkDialog() {
        _uiState.update { it.copy(joinCode = null, joinLinkError = null, isLoadingJoinLink = false) }
    }

    fun onInputChanged(text: String) {
        _uiState.update { it.copy(inputText = text) }
        viewModelScope.launch {
            if (text.isNotEmpty()) conversationOperations.chat().sendTypingStarted()
            else conversationOperations.chat().sendTypingStopped()
        }
    }

    fun setReplyTo(message: ChatMessage) {
        _uiState.update { it.copy(replyingTo = message) }
    }

    fun clearReplyTo() {
        _uiState.update { it.copy(replyingTo = null) }
    }

    fun toggleReaction(messageId: String, emoji: String) {
        viewModelScope.launch {
            conversationOperations.chat().toggleReaction(messageId, emoji)
        }
    }

    fun addAttachment(file: PickedFile) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments + file) }
    }

    fun removeAttachment(file: PickedFile) {
        _uiState.update { it.copy(pendingAttachments = it.pendingAttachments - file) }
    }

    fun sendMessage() {
        val text = _uiState.value.inputText.trim()
        val attachments = _uiState.value.pendingAttachments
        if (text.isEmpty() && attachments.isEmpty()) return
        val replyTo = _uiState.value.replyingTo
        _uiState.update { it.copy(inputText = "", replyingTo = null, pendingAttachments = emptyList(), sendError = null) }
        viewModelScope.launch {
            if (attachments.isNotEmpty()) {
                // Natrium uploads the files first and references them from the text message
                handleSendResult(conversationOperations.chat().sendText(text, attachments.map { it.toFileLink() }))
            } else if (replyTo != null) {
                conversationOperations.chat().sendReply(MessageValue.TextValue(text), replyTo.id)
            } else {
                conversationOperations.chat().sendMessage(MessageValue.TextValue(text))
            }
            conversationOperations.chat().sendTypingStopped()
        }
    }

    suspend fun downloadFile(messageId: String): FileDownloadResult {
        return conversationOperations.chat().downloadFile(messageId)
    }

    /** Answers a request; [attachments] are uploaded by Natrium before the response is sent. */
    fun sendResponse(requestMessageId: String, response: Response, attachments: List<PickedFile> = emptyList()) {
        viewModelScope.launch {
            _uiState.update { it.copy(sendError = null) }
            val fileLinks = attachments.map { it.toFileLink() }
            handleSendResult(conversationOperations.chat().sendResponse(requestMessageId, response, fileLinks))
        }
    }

    private fun handleSendResult(result: SendMessageResult) {
        val error = when (result) {
            is SendMessageResult.Success -> return
            is SendMessageResult.Failure.DisabledByTeam -> "File sharing is disabled."
            is SendMessageResult.Failure.RestrictedFileType -> "This file type is not allowed."
            is SendMessageResult.Failure.FileTooLarge -> "File too large (max ${result.limitBytes / 1024 / 1024} MB)."
            is SendMessageResult.Failure.NotLoggedIn -> "Not logged in."
            is SendMessageResult.Failure.InvalidResponse -> when (result.reason) {
                InvalidResponseReason.ALREADY_ANSWERED -> "This request has already been answered."
                InvalidResponseReason.REQUEST_NOT_FOUND -> "The request could not be found."
                InvalidResponseReason.SELECTION_OUT_OF_RANGE -> "Please choose the number of options the request asks for."
                else -> "Invalid answer (${result.reason})."
            }
            is SendMessageResult.Failure.Unknown -> result.message
        }
        _uiState.update { it.copy(sendError = error) }
    }

    fun dismissSendError() {
        _uiState.update { it.copy(sendError = null) }
    }

    override fun onCleared() {
        super.onCleared()
        messagesCancellable?.cancel()
        typingCancellable?.cancel()
    }
}

/** A locally copied file picked by the user. */
data class PickedFile(val path: Path, val fileName: String, val size: Long)

private fun PickedFile.toFileLink() = FileLink.fromLocal(path, fileName, guessMimeType(fileName), size)

private fun guessMimeType(fileName: String): String {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> "application/pdf"
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "gif" -> "image/gif"
        "txt" -> "text/plain"
        "zip" -> "application/zip"
        "doc" -> "application/msword"
        "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        "xls" -> "application/vnd.ms-excel"
        "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        else -> "application/octet-stream"
    }
}
