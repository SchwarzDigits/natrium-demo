package schwarz.digits.showcase.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.automirrored.filled.Reply
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import okio.FileSystem
import okio.Path.Companion.toPath
import okio.SYSTEM
import schwarz.digits.natrium.chat.ChatMessage
import schwarz.digits.natrium.chat.FileDownloadResult
import schwarz.digits.natrium.chat.FileTransferStatus
import schwarz.digits.natrium.chat.MessageStatus
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.QuotedMessage
import schwarz.digits.natrium.chat.ReactionInfo
import schwarz.digits.showcase.platform.openFile
import schwarz.digits.showcase.viewmodel.ChatViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(viewModel: ChatViewModel, onBack: () -> Unit) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberLazyListState()
    var showSheet by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val filePicker = rememberFilePickerLauncher(type = PickerType.File()) { file ->
        if (file != null) {
            scope.launch {
                val bytes = file.readBytes()
                val tempPath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "natrium-upload-${file.name}"
                FileSystem.SYSTEM.write(tempPath) { write(bytes) }
                viewModel.sendFile(tempPath, file.name, bytes.size.toLong())
            }
        }
    }

    LaunchedEffect(uiState.messages.size) {
        if (uiState.messages.isNotEmpty())
            listState.animateScrollToItem(uiState.messages.lastIndex)
    }

    if (showSheet) {
        ModalBottomSheet(onDismissRequest = { showSheet = false }) {
            uiState.members.forEach { member ->
                ListItem(
                    headlineContent = { Text(member.name) },
                    supportingContent = { Text(member.userId.value, style = MaterialTheme.typography.labelSmall) },
                )
            }
        }
    }

    if (uiState.isLoadingJoinLink || uiState.joinCode != null || uiState.joinLinkError != null) {
        InviteCodeDialog(
            isLoading = uiState.isLoadingJoinLink,
            joinCode = uiState.joinCode,
            errorMessage = uiState.joinLinkError,
            onRevoke = viewModel::revokeJoinLink,
            onDismiss = viewModel::dismissJoinLinkDialog,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column(modifier = Modifier.clickable { showSheet = true }) {
                        Text(uiState.title)
                        Text(
                            "${uiState.members.size} Mitglieder",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::getJoinLink) {
                        Icon(Icons.Default.Link, contentDescription = "Einladungslink generieren")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            val sendError = uiState.sendError
            if (sendError != null) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            Icons.Default.Error,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp),
                        )
                        Text(
                            sendError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                        )
                        TextButton(onClick = viewModel::dismissSendError) {
                            Text("Dismiss")
                        }
                    }
                }
            }

            if (uiState.typingUsers.isNotEmpty()) {
                Text(
                    "${uiState.typingUsers.joinToString(", ")} is typing...",
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            LazyColumn(state = listState, modifier = Modifier.weight(1f).fillMaxWidth()) {
                items(uiState.messages) { msg ->
                    MessageBubble(msg, viewModel, scope)
                }
            }

            val replyingTo = uiState.replyingTo
            if (replyingTo != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            .height(IntrinsicSize.Min),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            modifier = Modifier
                                .width(3.dp)
                                .fillMaxHeight()
                                .background(
                                    MaterialTheme.colorScheme.primary,
                                    RoundedCornerShape(2.dp),
                                )
                        )
                        Column(
                            modifier = Modifier.weight(1f).padding(start = 8.dp),
                        ) {
                            Text(
                                replyingTo.sender.name ?: "Unknown",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                messagePreviewText(replyingTo.value),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = viewModel::clearReplyTo) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Cancel reply",
                                modifier = Modifier.size(18.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { filePicker.launch() }) {
                    Icon(Icons.Default.AttachFile, contentDescription = "Attach file")
                }
                OutlinedTextField(
                    value = uiState.inputText,
                    onValueChange = viewModel::onInputChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Message...") },
                )
                IconButton(
                    onClick = viewModel::sendMessage,
                    enabled = uiState.inputText.isNotBlank(),
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "Send")
                }
            }
        }
    }
}

@Composable
private fun InviteCodeDialog(
    isLoading: Boolean,
    joinCode: String?,
    errorMessage: String?,
    onRevoke: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboardManager = LocalClipboardManager.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Einladungslink") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                when {
                    isLoading -> CircularProgressIndicator()
                    joinCode != null -> {
                        SelectionContainer {
                            Text(joinCode, style = MaterialTheme.typography.bodyMedium)
                        }
                        TextButton(onClick = {
                            clipboardManager.setText(AnnotatedString(joinCode))
                        }) {
                            Text("Kopieren")
                        }
                    }
                    errorMessage != null -> Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            if (joinCode != null) {
                TextButton(onClick = onRevoke) { Text("Widerrufen") }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
    )
}

@Composable
private fun MessageBubble(
    msg: ChatMessage,
    viewModel: ChatViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
) {
    when (msg.value) {
        is MessageValue.SystemValue -> SystemMessageRow(msg)
        is MessageValue.KnockValue -> KnockMessageRow(msg)
        else -> RegularMessageBubble(msg, viewModel, coroutineScope)
    }
}

@Composable
private fun SystemMessageRow(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${msg.sender.name ?: "Unknown"} ${msg.systemText ?: ""}",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun KnockMessageRow(msg: ChatMessage) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "${msg.sender.name ?: "Unknown"} knocked",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun RegularMessageBubble(
    msg: ChatMessage,
    viewModel: ChatViewModel,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
) {
    val alignment = if (msg.isSelf) Alignment.End else Alignment.Start
    val bubbleColor = if (msg.isSelf)
        MaterialTheme.colorScheme.primaryContainer
    else
        MaterialTheme.colorScheme.surfaceVariant

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalAlignment = alignment,
    ) {
        if (!msg.isSelf) {
            Text(msg.sender.name ?: "", style = MaterialTheme.typography.labelSmall)
        }
        var showContextMenu by remember { mutableStateOf(false) }

        Box {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = bubbleColor,
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = { showContextMenu = true },
                ),
            ) {
                Column {
                    val quote = msg.quotedMessage
                    if (quote != null) {
                        QuotedMessagePreview(quote)
                    }
                    when (val value = msg.value) {
                        is MessageValue.TextValue -> {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(value.value)
                                if (msg.isEdited) {
                                    Text(
                                        "(edited)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                        is MessageValue.FileValue -> {
                        val isUploading = msg.fileTransferStatus == FileTransferStatus.UPLOADING
                        val uploadFailed = msg.fileTransferStatus == FileTransferStatus.UPLOAD_FAILED
                        var isDownloading by remember { mutableStateOf(false) }
                        var downloadError by remember { mutableStateOf<String?>(null) }

                        Row(
                            modifier = Modifier
                                .clickable(enabled = !isDownloading && !isUploading) {
                                    coroutineScope.launch {
                                        isDownloading = true
                                        downloadError = null
                                        when (val result = viewModel.downloadFile(msg.id)) {
                                            is FileDownloadResult.Success -> {
                                                openFile(result.filePath.toString())
                                            }
                                            is FileDownloadResult.Failure -> {
                                                downloadError = when (result) {
                                                    is FileDownloadResult.Failure.NotLoggedIn -> "Not logged in"
                                                    is FileDownloadResult.Failure.Unknown -> result.message
                                                }
                                            }
                                        }
                                        isDownloading = false
                                    }
                                }
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            when {
                                isUploading || isDownloading ->
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                uploadFailed ->
                                    Icon(
                                        Icons.Default.Error,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.error,
                                    )
                                else ->
                                    Icon(
                                        Icons.Default.AttachFile,
                                        contentDescription = null,
                                        modifier = Modifier.size(20.dp),
                                    )
                            }
                            Column {
                                Text(
                                    value.fileLink.fileName,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                when {
                                    isUploading -> Text(
                                        "Uploading...",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    uploadFailed -> Text(
                                        "Upload failed",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                    else -> Text(
                                        formatFileSize(value.fileLink.dataSize),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (downloadError != null) {
                                    Text(
                                        downloadError!!,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error,
                                    )
                                }
                                if (msg.isEdited) {
                                    Text(
                                        "(edited)",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    is MessageValue.LocationValue -> {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                value.name ?: "Location",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                            Text(
                                "${value.latitude}, ${value.longitude}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (msg.isEdited) {
                                Text(
                                    "(edited)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                        is MessageValue.KnockValue,
                        is MessageValue.SystemValue -> {
                            // Handled by separate composables above
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showContextMenu,
                onDismissRequest = { showContextMenu = false },
            ) {
                Row(modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                    listOf("\uD83D\uDC4D", "\u2764\uFE0F", "\uD83D\uDE02", "\uD83D\uDE2E", "\uD83D\uDE22", "\uD83D\uDC4E").forEach { emoji ->
                        TextButton(onClick = {
                            viewModel.toggleReaction(msg.id, emoji)
                            showContextMenu = false
                        }) { Text(emoji, fontSize = 20.sp) }
                    }
                }
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Reply") },
                    onClick = {
                        viewModel.setReplyTo(msg)
                        showContextMenu = false
                    },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.Reply, contentDescription = null) },
                )
            }
        }
        ReactionsRow(
            reactions = msg.reactions,
            onToggleReaction = { emoji -> viewModel.toggleReaction(msg.id, emoji) },
        )
        if (msg.isSelf) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                val (icon, tint) = when (msg.status) {
                    MessageStatus.PENDING -> Icons.Default.Schedule to Color.Gray
                    MessageStatus.SENT -> Icons.Default.Check to Color.Gray
                    MessageStatus.DELIVERED -> Icons.Default.DoneAll to Color.Gray
                    MessageStatus.READ -> Icons.Default.DoneAll to MaterialTheme.colorScheme.primary
                    MessageStatus.FAILED,
                    MessageStatus.FAILED_REMOTELY -> Icons.Default.Error to MaterialTheme.colorScheme.error
                }
                Icon(icon, contentDescription = null, modifier = Modifier.size(12.dp), tint = tint)
            }
        }
    }
}

@Composable
private fun QuotedMessagePreview(quote: QuotedMessage) {
    Row(
        modifier = Modifier
            .padding(start = 8.dp, end = 8.dp, top = 8.dp)
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .background(
                    MaterialTheme.colorScheme.primary,
                    RoundedCornerShape(2.dp),
                )
        )
        Column(modifier = Modifier.padding(start = 8.dp)) {
            val senderName = quote.senderName
            if (senderName != null) {
                Text(
                    senderName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            val previewText = quote.previewText
            if (previewText != null) {
                Text(
                    previewText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionsRow(
    reactions: Map<String, ReactionInfo>,
    onToggleReaction: (String) -> Unit,
) {
    if (reactions.isEmpty()) return

    FlowRow(
        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        reactions.forEach { (emoji, info) ->
            val bgColor = if (info.isSelf)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
            Surface(
                onClick = { onToggleReaction(emoji) },
                shape = RoundedCornerShape(12.dp),
                color = bgColor,
            ) {
                Text(
                    "$emoji ${info.count}",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                    style = MaterialTheme.typography.labelSmall,
                )
            }
        }
    }
}

private fun messagePreviewText(value: MessageValue): String = when (value) {
    is MessageValue.TextValue -> value.value
    is MessageValue.FileValue -> value.fileLink.fileName
    is MessageValue.LocationValue -> value.name ?: "Location"
    else -> "Message"
}

private fun formatFileSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> {
        val mb = bytes / 1024 / 1024
        val remainder = (bytes % (1024 * 1024)) * 10 / (1024 * 1024)
        "$mb.$remainder MB"
    }
}
