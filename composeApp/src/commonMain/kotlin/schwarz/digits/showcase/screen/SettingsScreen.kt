package schwarz.digits.showcase.screen

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import schwarz.digits.showcase.platform.keyboardPadding
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import schwarz.digits.showcase.viewmodel.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onLogout: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(title = { Text("Settings") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .keyboardPadding()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            Text("User ID", style = MaterialTheme.typography.labelMedium)
            Row(verticalAlignment = Alignment.CenterVertically) {
                SelectionContainer(modifier = Modifier.weight(1f)) {
                    Text(uiState.userId, style = MaterialTheme.typography.bodyMedium)
                }
                IconButton(
                    onClick = { clipboardManager.setText(AnnotatedString(uiState.userId)) },
                    enabled = uiState.userId.isNotEmpty(),
                ) {
                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy user ID")
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = { viewModel.generateCaseLink() }) {
                Text("Behördenlink generieren")
            }
            uiState.caseLink?.let { link ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SelectionContainer(modifier = Modifier.weight(1f)) {
                        Text(link, style = MaterialTheme.typography.bodySmall)
                    }
                    IconButton(onClick = { clipboardManager.setText(AnnotatedString(link)) }) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy link")
                    }
                }
            }
            uiState.caseLinkError?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.displayName,
                onValueChange = { viewModel.onDisplayNameChange(it) },
                label = { Text("Display Name") },
                singleLine = true,
                enabled = !uiState.isUpdating,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.updateDisplayName() },
                enabled = !uiState.isUpdating && uiState.displayName.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Display Name")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.handle,
                onValueChange = { viewModel.onHandleChange(it) },
                label = { Text("Handle") },
                singleLine = true,
                enabled = !uiState.isUpdating,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.updateHandle() },
                enabled = !uiState.isUpdating && uiState.handle.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Handle")
            }
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = uiState.email,
                onValueChange = { viewModel.onEmailChange(it) },
                label = { Text("Email") },
                singleLine = true,
                enabled = !uiState.isUpdating,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(4.dp))
            Button(
                onClick = { viewModel.updateEmail() },
                enabled = !uiState.isUpdating && uiState.email.isNotBlank(),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save Email")
            }
            if (uiState.updateMessage != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = uiState.updateMessage!!,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(onClick = { viewModel.logout(onLogout) }) {
                Text("Logout")
            }
        }
    }
}
