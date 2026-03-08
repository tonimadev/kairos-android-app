package digital.tonima.core.usecases

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test

class GetSmartNotificationSuggestionsUseCaseTest {
    private val useCase = GetSmartNotificationSuggestionsUseCaseImpl()

    @Test
    fun `when recent events list is empty should return null`() =
        runBlocking {
            val result = useCase.invoke(emptyList(), "Instruction")
            assertNull(result)
        }
}
