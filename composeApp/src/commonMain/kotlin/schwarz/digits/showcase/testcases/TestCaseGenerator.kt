package schwarz.digits.showcase.testcases

// Test-case generator for structured requests. Isolated on purpose — to remove it, delete this
// package and the TestCaseSection usage in MainScreen.

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import schwarz.digits.natrium.chat.MessageValue
import schwarz.digits.natrium.chat.Option
import schwarz.digits.natrium.chat.Request
import schwarz.digits.natrium.chat.SendMessageResult
import schwarz.digits.natrium.conversation.CreateConversationResult
import schwarz.digits.natrium.session.Session
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.minutes

internal object TestCaseGenerator {

    /** Creates a new conversation containing one request per request type. Returns the conversation title. */
    suspend fun create(session: Session): Result<String> {
        val now = Clock.System.now()
        val local = now.toLocalDateTime(TimeZone.currentSystemDefault())
        val title = "Test case ${local.date} ${local.hour.pad()}:${local.minute.pad()}"

        val conversation = when (val result = session.conversationManager.createConversation(title)) {
            is CreateConversationResult.Success -> result.conversationOperation
            is CreateConversationResult.Failure -> return Result.failure(IllegalStateException(result.toString()))
        }

        val appointmentStart = now + 3.days
        val messages = listOf(
            MessageValue.TextValue("Generated test case with one request per type."),
            MessageValue.RequestValue(
                Request.SingleSelect(
                    subject = "Main residence",
                    message = "Is the address on file your main residence?",
                    options = listOf(Option("yes", "Yes"), Option("no", "No")),
                    deadline = now + 14.days,
                )
            ),
            MessageValue.RequestValue(
                Request.MultiSelect(
                    subject = "Available documents",
                    message = "Which of the following documents do you have?",
                    options = listOf(
                        Option("id_card", "ID card"),
                        Option("passport", "Passport"),
                        Option("birth_certificate", "Birth certificate"),
                    ),
                )
            ),
            MessageValue.RequestValue(
                Request.Asset(
                    subject = "Proof of income",
                    message = "Please upload your last three payslips.",
                    deadline = now + 7.days,
                )
            ),
            MessageValue.RequestValue(
                Request.Appointment(
                    subject = "Appointment",
                    message = "We suggest the following appointment at our office.",
                    from = appointmentStart,
                    to = appointmentStart + 30.minutes,
                )
            ),
        )

        val chat = conversation.chat()
        messages.forEach { value ->
            val result = chat.sendMessage(value)
            if (result is SendMessageResult.Failure) {
                return Result.failure(IllegalStateException("Sending test message failed: $result"))
            }
        }
        return Result.success(title)
    }

    private fun Int.pad() = toString().padStart(2, '0')
}
