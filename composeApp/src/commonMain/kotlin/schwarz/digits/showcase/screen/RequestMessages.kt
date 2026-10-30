package schwarz.digits.showcase.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RadioButtonChecked
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.vinceglb.filekit.compose.rememberFilePickerLauncher
import io.github.vinceglb.filekit.core.PickerType
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import okio.FileSystem
import okio.SYSTEM
import schwarz.digits.natrium.chat.ChatMessage
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.Request
import schwarz.digits.natrium.chat.Response
import schwarz.digits.showcase.viewmodel.PickedFile

// Rendering of structured requests/responses (see natrium-core/docs/requests.md).

/** A request's first response, shown inside the request bubble instead of as a separate message. */
internal data class RequestAnswer(
    val message: ChatMessage,
    val response: Response,
    val attachments: List<ChatMessage>,
)

internal class AnswerIndex(
    val byRequestId: Map<String, RequestAnswer>,
    /** File messages referenced by a free-text message, keyed by the text message id. */
    val textAttachments: Map<String, List<ChatMessage>>,
    /** Response messages and file messages that are rendered inside another bubble. */
    val hiddenMessageIds: Set<String>,
)

internal fun indexAnswers(messages: List<ChatMessage>): AnswerIndex {
    val byId = messages.associateBy { it.id }
    val answers = mutableMapOf<String, RequestAnswer>()
    val textAttachments = mutableMapOf<String, List<ChatMessage>>()
    val hidden = mutableSetOf<String>()
    messages.forEach { message ->
        val text = message.value as? MessageValue.TextValue
        if (text != null && text.attachmentMessageIds.isNotEmpty()) {
            val attachments = text.attachmentMessageIds.mapNotNull { byId[it] }
            textAttachments[message.id] = attachments
            hidden += attachments.map { it.id }
            return@forEach
        }
        if (message.value !is MessageValue.RequestValue) return@forEach
        val responseMessage = message.answeredByMessageId?.let { byId[it] } ?: return@forEach
        val response = (responseMessage.value as? MessageValue.ResponseValue)?.response ?: return@forEach
        val attachments = response.attachmentIds().mapNotNull { byId[it] }
        answers[message.id] = RequestAnswer(responseMessage, response, attachments)
        hidden += responseMessage.id
        hidden += attachments.map { it.id }
    }
    return AnswerIndex(answers, textAttachments, hidden)
}

@Composable
internal fun RequestContent(
    request: Request,
    isAnswered: Boolean,
    answer: RequestAnswer?,
    onAnswer: () -> Unit,
    onOpenAttachment: (ChatMessage) -> Unit,
) {
    val response = answer?.response
    Column(
        modifier = Modifier.widthIn(max = 320.dp).padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                request.icon(),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(6.dp))
            Text(
                request.typeLabel(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f, fill = false),
            )
            val deadline = request.deadline
            if (deadline != null && !isAnswered) {
                Spacer(Modifier.width(8.dp))
                DeadlineBadge(deadline)
            }
        }
        if (request.subject.isNotBlank()) {
            Text(request.subject, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        }
        Text(request.message, style = MaterialTheme.typography.bodyMedium)

        when (request) {
            is Request.SingleSelect -> {
                val selected = (response as? Response.SingleSelect)?.selectedOptionId
                request.options.forEach { option ->
                    val checked = option.id == selected
                    OptionStateLine(
                        title = option.title,
                        checked = checked,
                        icon = if (checked) Icons.Default.RadioButtonChecked else Icons.Default.RadioButtonUnchecked,
                    )
                }
            }
            is Request.MultiSelect -> {
                val selected = (response as? Response.MultiSelect)?.selectedOptionIds.orEmpty()
                request.options.forEach { option ->
                    val checked = option.id in selected
                    OptionStateLine(
                        title = option.title,
                        checked = checked,
                        icon = if (checked) Icons.Default.CheckBox else Icons.Default.CheckBoxOutlineBlank,
                    )
                }
            }
            is Request.Appointment -> {
                AppointmentLine(request.from, request.to)
                if (response is Response.Appointment) {
                    OptionStateLine(
                        title = if (response.accepted) "Accepted" else "Declined",
                        checked = true,
                        icon = if (response.accepted) Icons.Default.CheckCircle else Icons.Default.Cancel,
                    )
                }
            }
            is Request.Asset -> Unit
        }

        if (answer != null && answer.attachments.isNotEmpty()) {
            answer.attachments.forEach { AttachmentLine(it, onOpenAttachment) }
        }

        if (response is Response.FreeText) {
            HorizontalDivider()
            Text(response.text, style = MaterialTheme.typography.bodyMedium)
        }

        if (isAnswered) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(4.dp))
                val by = answer?.message?.let { if (it.isSelf) "you" else it.sender.name }
                Text(
                    if (by != null) "Answered by $by" else "Answered",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        } else {
            Button(onClick = onAnswer, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(12.dp)) {
                Text("Answer")
            }
        }
    }
}

@Composable
private fun OptionStateLine(title: String, checked: Boolean, icon: ImageVector) {
    val color = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = color)
        Spacer(Modifier.width(8.dp))
        Text(
            title,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (checked) FontWeight.Medium else FontWeight.Normal,
            color = if (checked) MaterialTheme.colorScheme.onSurface else color,
        )
    }
}

@Composable
internal fun AttachmentLine(fileMessage: ChatMessage, onOpen: (ChatMessage) -> Unit) {
    val name = (fileMessage.value as? MessageValue.FileValue)?.fileLink?.fileName ?: "Attachment"
    Row(
        modifier = Modifier.fillMaxWidth().clickable { onOpen(fileMessage) }.padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            name,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ResponseContent(response: Response, request: Request?) {
    Column(
        modifier = Modifier.widthIn(max = 320.dp).padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text("Answer", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(responseSummary(response, request), style = MaterialTheme.typography.bodyMedium)
        val attachments = response.attachmentIds()
        if (attachments.isNotEmpty()) {
            AttachmentHint(attachments.size)
        }
    }
}

@Composable
internal fun UnsupportedContent() {
    Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "This message is not supported. Please update the app.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun AttachmentHint(count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(14.dp))
        Text(
            if (count == 1) "1 attachment" else "$count attachments",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** Bottom sheet to answer [request]: type-specific input, or free text for every type. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RequestAnswerSheet(
    request: Request,
    onDismiss: () -> Unit,
    onSubmit: (Response, List<PickedFile>) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showFreeText by remember { mutableStateOf(false) }
    var freeText by remember { mutableStateOf("") }
    var selectedSingle by remember { mutableStateOf<String?>(null) }
    var selectedMulti by remember { mutableStateOf(emptySet<String>()) }
    var appointmentAccepted by remember { mutableStateOf<Boolean?>(null) }
    val files = remember { mutableStateListOf<PickedFile>() }

    val scope = rememberCoroutineScope()
    val filePicker = rememberFilePickerLauncher(type = PickerType.File()) { file ->
        if (file != null) {
            scope.launch {
                val bytes = file.readBytes()
                val tempPath = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "natrium-upload-${file.name}"
                FileSystem.SYSTEM.write(tempPath) { write(bytes) }
                files += PickedFile(tempPath, file.name, bytes.size.toLong())
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("My answer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                if (showFreeText) "Write a message instead of a structured answer." else request.answerHint(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (showFreeText) {
                OutlinedTextField(
                    value = freeText,
                    onValueChange = { freeText = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Your message...") },
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3,
                )
            } else {
                when (request) {
                    is Request.SingleSelect -> OptionsCard(request.options.map { it.id to it.title }) { id, title ->
                        ChoiceRow(title, onClick = { selectedSingle = id }) {
                            RadioButton(selected = selectedSingle == id, onClick = { selectedSingle = id })
                        }
                    }
                    is Request.MultiSelect -> OptionsCard(request.options.map { it.id to it.title }) { id, title ->
                        val toggle = { selectedMulti = if (id in selectedMulti) selectedMulti - id else selectedMulti + id }
                        ChoiceRow(title, onClick = toggle) {
                            Checkbox(checked = id in selectedMulti, onCheckedChange = { toggle() })
                        }
                    }
                    is Request.Asset -> FilesCard(files, onPick = { filePicker.launch() }, onRemove = { files.remove(it) })
                    is Request.Appointment -> {
                        AppointmentLine(request.from, request.to)
                        OptionsCard(listOf("accept" to "Accept appointment", "decline" to "Decline appointment")) { id, title ->
                            val accepted = id == "accept"
                            ChoiceRow(title, onClick = { appointmentAccepted = accepted }) {
                                RadioButton(selected = appointmentAccepted == accepted, onClick = { appointmentAccepted = accepted })
                            }
                        }
                    }
                }
            }

            val structured: Response? = when (request) {
                is Request.SingleSelect -> selectedSingle?.let { Response.SingleSelect(it) }
                is Request.MultiSelect -> selectedMulti.takeIf { it.isNotEmpty() }
                    ?.let { selected -> Response.MultiSelect(request.options.map { it.id }.filter { it in selected }) }
                is Request.Asset -> if (files.isNotEmpty()) Response.Asset() else null
                is Request.Appointment -> appointmentAccepted?.let { Response.Appointment(it) }
            }
            val submitEnabled = if (showFreeText) freeText.isNotBlank() else structured != null

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { showFreeText = !showFreeText },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text(if (showFreeText) "Show options" else "Free text")
                }
                Button(
                    onClick = {
                        if (showFreeText) {
                            onSubmit(Response.FreeText(freeText.trim()), emptyList())
                        } else if (structured != null) {
                            onSubmit(structured, if (structured is Response.Asset) files.toList() else emptyList())
                        }
                    },
                    enabled = submitEnabled,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                ) {
                    Text("Submit")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun OptionsCard(
    options: List<Pair<String, String>>,
    row: @Composable (id: String, title: String) -> Unit,
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        options.forEachIndexed { index, (id, title) ->
            row(id, title)
            if (index < options.lastIndex) HorizontalDivider()
        }
    }
}

@Composable
private fun ChoiceRow(title: String, onClick: () -> Unit, control: @Composable () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        control()
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun FilesCard(files: List<PickedFile>, onPick: () -> Unit, onRemove: (PickedFile) -> Unit) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        files.forEach { file ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    file.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Remove",
                    modifier = Modifier.size(20.dp).clickable { onRemove(file) },
                )
            }
            HorizontalDivider()
        }
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onPick).padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(Icons.Default.AttachFile, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (files.isEmpty()) "Select document" else "Add another document", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AppointmentLine(from: Instant, to: Instant) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.DateRange, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(6.dp))
        Text(
            "${formatDateTime(from)} – ${formatTime(to)}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
        )
    }
}

@Composable
private fun DeadlineBadge(deadline: Instant) {
    val days = (deadline - Clock.System.now()).inWholeDays
    val text = when {
        deadline < Clock.System.now() -> "Overdue"
        days == 0L -> "Due today"
        days == 1L -> "Due in 1 day"
        else -> "Due in $days days"
    }
    val (background, foreground) = if (days <= 2) {
        MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
    }
    Text(
        text,
        style = MaterialTheme.typography.labelSmall,
        color = foreground,
        modifier = Modifier.background(background, RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

internal fun Request.typeLabel(): String = when (this) {
    is Request.SingleSelect -> "Single choice"
    is Request.MultiSelect -> "Multiple choice"
    is Request.Asset -> "Document upload"
    is Request.Appointment -> "Appointment"
}

private fun Request.icon(): ImageVector = when (this) {
    is Request.SingleSelect -> Icons.Default.RadioButtonChecked
    is Request.MultiSelect -> Icons.Default.CheckBox
    is Request.Asset -> Icons.Default.Description
    is Request.Appointment -> Icons.Default.DateRange
}

private fun Request.answerHint(): String = when (this) {
    is Request.SingleSelect -> "Please choose one option or write a free-text message."
    is Request.MultiSelect -> "You can choose several options or write a free-text message."
    is Request.Asset -> "Please upload the requested documents or write a free-text message."
    is Request.Appointment -> "Please accept or decline the suggested appointment or write a free-text message."
}

internal fun responseSummary(response: Response, request: Request?): String {
    val titles = when (request) {
        is Request.SingleSelect -> request.options.associate { it.id to it.title }
        is Request.MultiSelect -> request.options.associate { it.id to it.title }
        else -> emptyMap()
    }
    return when (response) {
        is Response.SingleSelect -> titles[response.selectedOptionId] ?: response.selectedOptionId
        is Response.MultiSelect -> response.selectedOptionIds.joinToString(", ") { titles[it] ?: it }
        is Response.Asset -> "Documents submitted"
        is Response.Appointment -> if (response.accepted) "Appointment accepted" else "Appointment declined"
        is Response.FreeText -> response.text
    }
}

private fun Response.attachmentIds(): List<String> = when (this) {
    is Response.Asset -> attachmentMessageIds
    is Response.FreeText -> attachmentMessageIds
    else -> emptyList()
}

private fun formatDateTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.dayOfMonth.pad()}.${local.monthNumber.pad()}.${local.year} ${local.hour.pad()}:${local.minute.pad()}"
}

private fun formatTime(instant: Instant): String {
    val local = instant.toLocalDateTime(TimeZone.currentSystemDefault())
    return "${local.hour.pad()}:${local.minute.pad()}"
}

private fun Int.pad() = toString().padStart(2, '0')
