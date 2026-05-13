package digital.tonima.core.usecases

import digital.tonima.core.permissions.PermissionManager
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CheckPermissionsUseCaseTest {
    private val permissionManager = mockk<PermissionManager>()
    private lateinit var useCase: CheckPermissionsUseCase

    @Before
    fun setup() {
        useCase = CheckPermissionsUseCase(permissionManager)
    }

    @Test
    fun `when all permissions are granted then state should reflect it`() {
        every { permissionManager.hasCalendarPermission() } returns true
        every { permissionManager.hasPostNotificationsPermission() } returns true
        every { permissionManager.hasExactAlarmPermission() } returns true
        every { permissionManager.hasFullScreenIntentPermission() } returns true
        every { permissionManager.hasLocationPermission() } returns true
        every { permissionManager.hasBackgroundLocationPermission() } returns true

        val state = useCase()

        assertEquals(true, state.hasCalendarPermission)
        assertEquals(true, state.hasPostNotificationsPermission)
        assertEquals(true, state.hasExactAlarmPermission)
        assertEquals(true, state.hasFullScreenIntentPermission)
        assertEquals(true, state.hasLocationPermission)
        assertEquals(true, state.hasBackgroundLocationPermission)
    }

    @Test
    fun `when some permissions are denied then state should reflect it`() {
        every { permissionManager.hasCalendarPermission() } returns true
        every { permissionManager.hasPostNotificationsPermission() } returns false
        every { permissionManager.hasExactAlarmPermission() } returns true
        every { permissionManager.hasFullScreenIntentPermission() } returns false
        every { permissionManager.hasLocationPermission() } returns true
        every { permissionManager.hasBackgroundLocationPermission() } returns false

        val state = useCase()

        assertEquals(true, state.hasCalendarPermission)
        assertEquals(false, state.hasPostNotificationsPermission)
        assertEquals(true, state.hasExactAlarmPermission)
        assertEquals(false, state.hasFullScreenIntentPermission)
        assertEquals(true, state.hasLocationPermission)
        assertEquals(false, state.hasBackgroundLocationPermission)
    }
}
