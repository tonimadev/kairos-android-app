package digital.tonima.core.notifications

/**
 * Packages the notification features are allowed to look at. Anything else is ignored without
 * reading its content.
 */
object NotificationPackages {
    val CALENDAR =
        setOf(
            "com.google.android.calendar",
            "com.samsung.android.calendar",
            "com.android.calendar",
        )

    /** Chat, e-mail and social apps held back during a meeting (name shown in the summary). */
    val FOCUS_DIGEST: Map<String, String> =
        mapOf(
            "com.whatsapp" to "WhatsApp",
            "com.whatsapp.w4b" to "WhatsApp Business",
            "org.telegram.messenger" to "Telegram",
            "com.facebook.orca" to "Messenger",
            "com.facebook.katana" to "Facebook",
            "com.instagram.android" to "Instagram",
            "com.twitter.android" to "X",
            "com.google.android.gm" to "Gmail",
            "com.microsoft.office.outlook" to "Outlook",
            "com.google.android.apps.messaging" to "Messages",
            "com.discord" to "Discord",
        )

    /** Apps whose notifications may be turned into an event suggestion. */
    val EVENT_SUGGESTIONS =
        setOf(
            "com.google.android.gm",
            "com.microsoft.office.outlook",
            "com.google.android.apps.messaging",
            "com.whatsapp",
            "com.whatsapp.w4b",
            "org.telegram.messenger",
        )
}
