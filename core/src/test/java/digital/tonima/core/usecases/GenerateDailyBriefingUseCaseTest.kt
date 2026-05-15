package digital.tonima.core.usecases

import androidx.test.core.app.ApplicationProvider
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.ai.GenerativeModel
import com.google.firebase.ai.ai
import digital.tonima.core.model.Event
import digital.tonima.core.repository.DailyBriefingRepository
import digital.tonima.core.repository.WeatherRepository
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GenerateDailyBriefingUseCaseTest {
    private val weatherRepository = mockk<WeatherRepository>()
    private val dailyBriefingRepository = mockk<DailyBriefingRepository>(relaxed = true)
    private val useCase = GenerateDailyBriefingUseCaseImpl(weatherRepository, dailyBriefingRepository)

    @Before
    fun setup() {
        if (FirebaseApp.getApps(ApplicationProvider.getApplicationContext()).isEmpty()) {
            FirebaseApp.initializeApp(
                ApplicationProvider.getApplicationContext(),
                FirebaseOptions.Builder()
                    .setApplicationId("abc")
                    .setApiKey("xyz")
                    .setProjectId("123")
                    .build(),
            )
        }
        mockkStatic("com.google.firebase.ai.FirebaseAIKt")
    }

    @After
    fun tearDown() {
        unmockkStatic("com.google.firebase.ai.FirebaseAIKt")
    }

    @Test
    fun `when events list is empty should return null`() =
        runBlocking {
            coEvery { weatherRepository.getWeather(any<String>(), any<Boolean>(), any<String>()) } returns null
            val mockModel = mockk<GenerativeModel>()
            coEvery { mockModel.generateContent(any<String>()) } returns
                mockk {
                    every { text } returns null
                }

            every { Firebase.ai(any(), any()) } returns
                mockk {
                    every { generativeModel(any(), any(), any(), any(), any(), any(), any()) } returns mockModel
                }

            val result = useCase.invoke(emptyList(), "Instruction", null)
            assertNull(result)
        }

    @Test
    fun `when events exist should return briefing text`() =
        runBlocking {
            val mockModel = mockk<GenerativeModel>()
            coEvery { mockModel.generateContent(any<String>()) } returns
                mockk {
                    every { text } returns "Briefing content"
                }

            every { Firebase.ai(any(), any()) } returns
                mockk {
                    every { generativeModel(any(), any(), any(), any(), any(), any(), any()) } returns mockModel
                }

            val events =
                listOf(
                    Event(
                        id = 1L,
                        title = "Event 1",
                        startTime = 1710000000000L,
                        isAllDay = false,
                    ),
                )

            val result = useCase.invoke(events, "Instruction", "08:00")
            assertEquals("Briefing content", result)
        }
}
