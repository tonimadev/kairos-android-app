package digital.tonima.kairos.wear

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import digital.tonima.core.receiver.AlarmReceiver
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class WearAlarmActivityTest {
    private lateinit var intent: Intent

    @Before
    fun setup() {
        intent =
            Intent(ApplicationProvider.getApplicationContext(), WearAlarmActivity::class.java).apply {
                putExtra(AlarmReceiver.EXTRA_EVENT_TITLE, "Test Event")
                putExtra(AlarmReceiver.EXTRA_UNIQUE_ID, 1)
                putExtra(AlarmReceiver.EXTRA_EVENT_ID, 2L)
                putExtra(AlarmReceiver.EXTRA_EVENT_START_TIME, 3L)
            }
    }

    @Test
    fun `should display event title from intent`() {
        val activity = Robolectric.buildActivity(WearAlarmActivity::class.java, intent).setup().get()
        assertEquals("Test Event", activity.intent.getStringExtra(AlarmReceiver.EXTRA_EVENT_TITLE))
    }
}
