package digital.tonima.kairos

import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class ResourcesExistenceTest {
    @Test
    fun vibrateOnlyString_isAvailableFromCoreResources() {
        val context = RuntimeEnvironment.getApplication()
        val text = context.getString(digital.tonima.kairos.core.R.string.vibrate_only)
        assertTrue(text.isNotBlank())
    }
}
