package digital.tonima.core.notifications

/** Which notifications may be held back while a meeting is in focus. */
object FocusFilterPolicy {
    // Values of android.app.Notification.CATEGORY_*; kept as strings so this stays JVM-testable.
    private val neverHeldCategories =
        setOf("call", "alarm", "reminder", "navigation", "transport", "progress", "sys", "err", "service")

    fun shouldHold(
        packageName: String,
        category: String?,
        isOngoing: Boolean,
        isGroupSummary: Boolean,
        ownPackage: String,
    ): Boolean =
        packageName != ownPackage &&
            packageName in NotificationPackages.FOCUS_DIGEST &&
            !isOngoing &&
            !isGroupSummary &&
            category !in neverHeldCategories
}
