package schwarz.digits.showcase.screen

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import schwarz.digits.natrium.conversation.ConversationOperations
import schwarz.digits.showcase.viewmodel.ConversationItem
import schwarz.digits.showcase.viewmodel.ConversationsViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConversationsScreen(viewModel: ConversationsViewModel, onConversationClick: (ConversationOperations) -> Unit) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Conversations") },
                actions = {
                    IconButton(onClick = { viewModel.openJoinDialog() }) {
                        Icon(Icons.Default.PersonAdd, contentDescription = "Code einlösen")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { viewModel.openCreateDialog() }) {
                Icon(Icons.Default.Add, contentDescription = "Conversation erstellen")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (uiState.conversations.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No conversations found",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding)
            ) {
                items(uiState.conversations) { conversation ->
                    ListItem(
                        headlineContent = { Text(conversation.title) },
                        supportingContent = null,
                        trailingContent = {
                            if (conversation.isArchived) {
                                Text(
                                    text  = "Archiviert",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        modifier = Modifier.combinedClickable(
                            onClick = { onConversationClick(conversation.operations) },
                            onLongClick = { viewModel.showActions(conversation) },
                        )
                    )
                    HorizontalDivider()
                }
            }
        }
    }

    if (uiState.showCreateDialog) {
        CreateConversationDialog(
            onDismiss    = { viewModel.dismissCreateDialog() },
            onCreate     = { title -> viewModel.createConversation(title) },
            errorMessage = uiState.createError,
        )
    }

    if (uiState.showJoinDialog) {
        JoinConversationDialog(
            isJoining    = uiState.isJoining,
            errorMessage = uiState.joinError,
            onJoin       = { code -> viewModel.joinConversation(code) },
            onDismiss    = { viewModel.dismissJoinDialog() },
        )
    }

    uiState.showActionsDialog?.let { conversation ->
        ConversationActionsDialog(
            conversation = conversation,
            onShare      = { viewModel.requestShareConversation() },
            onDelete     = { viewModel.requestDeleteConversation() },
            onDismiss    = { viewModel.dismissActions() },
        )
    }

    uiState.showConfirmDelete?.let { conversation ->
        DeleteConversationDialog(
            conversationTitle = conversation.title,
            errorMessage      = uiState.deleteError,
            onConfirm         = { viewModel.confirmDeleteConversation() },
            onDismiss         = { viewModel.dismissDeleteDialog() },
        )
    }

    if (uiState.showShareDialog || uiState.isLoadingJoinLink || uiState.joinCode != null || uiState.joinLinkError != null) {
        ShareConversationDialog(
            isLoading    = uiState.isLoadingJoinLink,
            joinCode     = uiState.joinCode,
            errorMessage = uiState.joinLinkError,
            onDismiss    = { viewModel.dismissShareDialog() },
        )
    }
}

@Composable
private fun JoinConversationDialog(
    isJoining: Boolean,
    errorMessage: String?,
    onJoin: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var code by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conversation beitreten") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it },
                    label = { Text("Einladungslink") },
                    singleLine = true,
                    isError = errorMessage != null,
                )
                if (errorMessage != null) {
                    Text(
                        text  = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onJoin(code) },
                enabled = code.isNotBlank() && !isJoining,
            ) {
                if (isJoining) CircularProgressIndicator(modifier = Modifier.size(16.dp))
                else Text("Beitreten")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun ConversationActionsDialog(
    conversation: ConversationItem,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(conversation.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Teilen")
                }
                Button(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text("Löschen")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun DeleteConversationDialog(
    conversationTitle: String,
    errorMessage: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Conversation löschen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("\"$conversationTitle\" wirklich löschen?")
                if (errorMessage != null) {
                    Text(
                        text  = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Löschen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}

@Composable
private fun ShareConversationDialog(
    isLoading: Boolean,
    joinCode: String?,
    errorMessage: String?,
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
                        text  = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Schließen") }
        },
    )
}

@Composable
private fun CreateConversationDialog(
    onDismiss: () -> Unit,
    onCreate: (title: String) -> Unit,
    errorMessage: String?,
) {
    var title by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Neue Conversation erstellen") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Titel") },
                    singleLine = true,
                    isError = errorMessage != null,
                )
                if (errorMessage != null) {
                    Text(
                        text  = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onCreate(title) },
                enabled = title.isNotBlank(),
            ) { Text("Erstellen") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen") }
        },
    )
}
