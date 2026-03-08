package digital.tonima.core.usecases

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test

class GenerateDailyBriefingUseCaseTest {
    private val useCase = GenerateDailyBriefingUseCaseImpl()

    @Test
    fun `when events list is empty should return null`() =
        runBlocking {
            val result = useCase.invoke(emptyList(), "Instruction", null)
            assertNull(result)
        }
}
