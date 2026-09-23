package digital.tonima.kairos.navigation

import digital.tonima.feature.ai.bridge.AiNavKey
import digital.tonima.feature.calendar.bridge.CalendarNavKey
import digital.tonima.feature.settings.bridge.SettingsNavKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NavigationStateNavigatorTest {
    private val navigator = NavigationStateNavigator()

    @Test
    fun `the app starts on the calendar and cannot pop past it`() {
        assertEquals(listOf(CalendarNavKey.Main), navigator.backStack.toList())
        assertFalse(navigator.popBackStack())
        assertEquals(listOf(CalendarNavKey.Main), navigator.backStack.toList())
    }

    @Test
    fun `screens are pushed and popped in order`() {
        navigator.navigateTo(SettingsNavKey.Root)
        navigator.navigateTo(CalendarNavKey.ImportCalendar)

        assertTrue(navigator.popBackStack())
        assertEquals(listOf(CalendarNavKey.Main, SettingsNavKey.Root), navigator.backStack.toList())
    }

    @Test
    fun `popTo keeps or drops the target screen`() {
        navigator.navigateTo(AiNavKey.ChatHistory)
        navigator.navigateTo(AiNavKey.ChatDetail(1L))
        navigator.navigateTo(AiNavKey.ChatDetail(2L))

        navigator.popTo(AiNavKey.ChatHistory, inclusive = false)
        assertEquals(listOf(CalendarNavKey.Main, AiNavKey.ChatHistory), navigator.backStack.toList())

        navigator.popTo(AiNavKey.ChatHistory, inclusive = true)
        assertEquals(listOf(CalendarNavKey.Main), navigator.backStack.toList())
    }

    @Test
    fun `popTo a screen that is not open does nothing`() {
        navigator.navigateTo(SettingsNavKey.Root)

        navigator.popTo(AiNavKey.ChatHistory, inclusive = true)

        assertEquals(listOf(CalendarNavKey.Main, SettingsNavKey.Root), navigator.backStack.toList())
    }
}
