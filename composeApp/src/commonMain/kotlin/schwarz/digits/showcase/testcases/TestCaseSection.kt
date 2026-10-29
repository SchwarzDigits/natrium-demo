package schwarz.digits.showcase.testcases

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import schwarz.digits.natrium.session.Session

data class TestCaseUiState(
    val isCreating: Boolean = false,
    val message: String? = null,
)

class TestCaseViewModel(private val session: Session) : ViewModel() {

    private val _uiState = MutableStateFlow(TestCaseUiState())
    val uiState: StateFlow<TestCaseUiState> = _uiState.asStateFlow()

    fun createTestCase() {
        _uiState.update { it.copy(isCreating = true, message = null) }
        viewModelScope.launch {
            val result = TestCaseGenerator.create(session)
            _uiState.update {
                it.copy(
                    isCreating = false,
                    message = result.fold(
                        onSuccess = { title -> "Created \"$title\"" },
                        onFailure = { e -> "Failed: ${e.message}" },
                    ),
                )
            }
        }
    }
}

@Composable
fun TestCaseSection(viewModel: TestCaseViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Column {
        OutlinedButton(
            onClick = viewModel::createTestCase,
            enabled = !uiState.isCreating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (uiState.isCreating) "Creating test case..." else "Create request test case")
        }
        uiState.message?.let {
            Spacer(modifier = Modifier.height(4.dp))
            Text(it, style = MaterialTheme.typography.bodySmall)
        }
    }
}
