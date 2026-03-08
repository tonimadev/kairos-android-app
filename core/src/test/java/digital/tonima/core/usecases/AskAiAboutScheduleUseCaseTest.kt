package digital.tonima.core.usecases

import digital.tonima.core.model.Event
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNull
import org.junit.Test

class AskAiAboutScheduleUseCaseTest {
    private val useCase =
        object : AskAiAboutScheduleUseCase {
            override suspend fun invoke(
                events: List<Event>,
                question: String,
                languageInstruction: String,
            ): String? {
                return if (question.isEmpty()) null else "Response to $question"
            }
        }

    @Test
    fun `when question is empty should return null`() =
        runTest {
            val result = useCase.invoke(emptyList(), "", "Instruction")
            assertNull(result)
        }
}
