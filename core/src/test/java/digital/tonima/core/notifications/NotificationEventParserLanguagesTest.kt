package digital.tonima.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Locale

/** Today is Monday 2026-10-05, 10:00 UTC. */
class NotificationEventParserLanguagesTest {
    private val zone: ZoneId = ZoneOffset.UTC
    private val now = LocalDateTime.of(2026, 10, 5, 10, 0).atZone(zone).toInstant().toEpochMilli()

    private fun millis(
        month: Int,
        day: Int,
        hour: Int,
        minute: Int = 0,
        year: Int = 2026,
    ) = LocalDateTime.of(year, month, day, hour, minute).atZone(zone).toInstant().toEpochMilli()

    private fun assertStart(
        expected: Long,
        text: String,
        locale: Locale = Locale.ENGLISH,
    ) {
        val event = NotificationEventParser.parse(text, now, zone, locale)
        assertNotNull("no event found in: $text", event)
        assertEquals("wrong start for: $text", expected, event!!.startMillis)
    }

    @Test
    fun `tomorrow with a time is understood in all ten languages`() {
        val tomorrow15 = millis(10, 6, 15)
        listOf(
            "Reunião amanhã às 15h",
            "Meeting tomorrow at 15:00",
            "Reunión mañana a las 15:00",
            "Réunion demain à 15h",
            "Besprechung morgen um 15 Uhr",
            "Встреча завтра в 15:00",
            "اجتماع غدا الساعة 15:00",
            "会議 明日 15時",
            "会议 明天 15点",
        ).forEach { assertStart(tomorrow15, it) }
    }

    @Test
    fun `hindi uses a weekday or a date because tomorrow is ambiguous`() {
        assertStart(millis(10, 8, 15), "मीटिंग गुरुवार 15:00 बजे")
        assertStart(millis(10, 20, 15), "मीटिंग 20 अक्टूबर 3 बजे दोपहर")
    }

    @Test
    fun `month names work in every language that spells them out`() {
        val oct20 = millis(10, 20, 14, 30)
        listOf(
            "Reunião 20 de outubro às 14:30",
            "Meeting October 20 at 14:30",
            "Meeting 20th of October 14:30",
            "Reunión el 20 de octubre a las 14:30",
            "Réunion le 20 octobre à 14h30",
            "Besprechung am 20. Oktober um 14:30 Uhr",
            "Встреча 20 октября в 14:30",
            "اجتماع 20 أكتوبر الساعة 14:30",
            "会議 10月20日 14時30分",
            "会议 10月20日 14点30分",
        ).forEach { assertStart(oct20, it) }
    }

    @Test
    fun `weekday names resolve to the next occurrence`() {
        val friday = millis(10, 9, 15)
        listOf(
            "Reunião sexta às 15h",
            "Meeting Friday at 3pm",
            "Reunión el viernes a las 15:00",
            "Réunion vendredi à 15h",
            "Besprechung Freitag um 15 Uhr",
            "Встреча в пятницу в 15:00",
            "اجتماع الجمعة الساعة 15:00",
            "会議 金曜日 15時",
            "会议 周五 15点",
        ).forEach { assertStart(friday, it) }
    }

    @Test
    fun `a weekday that already passed today means next week`() {
        // Monday 08:00 has passed (it is Monday 10:00), so this is next Monday.
        assertStart(millis(10, 12, 8), "Meeting Monday at 08:00")
        assertStart(millis(10, 5, 18), "Meeting Monday at 18:00")
    }

    @Test
    fun `day periods move a 12 hour time to the right half of the day`() {
        val afternoon = millis(10, 6, 15)
        assertStart(afternoon, "Meeting tomorrow at 3 in the afternoon")
        assertStart(afternoon, "Reunião amanhã 3 da tarde")
        assertStart(afternoon, "Reunión mañana 3 de la tarde")
        assertStart(afternoon, "Встреча завтра 3:00 дня")
        assertStart(afternoon, "会议 明天 下午3点")
        assertStart(afternoon, "会議 明日 午後3時")
        assertStart(millis(10, 6, 9), "会议 明天 上午9点")
    }

    @Test
    fun `half hours in east asian text`() {
        assertStart(millis(10, 6, 15, 30), "会议 明天 15点半")
    }

    @Test
    fun `numbers written in other scripts are read`() {
        val tomorrow15 = millis(10, 6, 15)
        assertStart(tomorrow15, "اجتماع غدا الساعة ١٥:٠٠")
        assertStart(tomorrow15, "会议 明天 １５点")
        assertStart(tomorrow15, "मीटिंग ६/१० १५:००", Locale("pt", "BR"))
    }

    @Test
    fun `an ambiguous numeric date follows the locale order`() {
        assertStart(millis(3, 4, 14, year = 2027), "Meeting 03/04 14:00", Locale.US)
        assertStart(millis(4, 3, 14, year = 2027), "Reunião 03/04 14:00", Locale("pt", "BR"))
        assertStart(millis(4, 3, 14, year = 2027), "Meeting 03/04 14:00", Locale.UK)
    }

    @Test
    fun `a number above 12 is always the day whatever the locale`() {
        assertStart(millis(10, 20, 14), "Meeting 20/10 14:00", Locale.US)
        assertStart(millis(10, 20, 14), "Meeting 10/20 14:00", Locale.US)
        assertStart(millis(10, 20, 14), "Reunião 20/10 14:00", Locale("pt", "BR"))
    }

    @Test
    fun `iso dates are understood`() {
        assertStart(millis(12, 15, 9, 30), "Meeting 2026-12-15 09:30")
    }

    @Test
    fun `two digit hours with am and pm and a dotted german date`() {
        assertStart(millis(12, 15, 9, 30), "Besprechung 15.12.2026 um 9.30 Uhr")
        assertStart(millis(10, 6, 15), "Lunch meeting tomorrow 3:00 PM")
    }

    @Test
    fun `text without an appointment word is ignored in every language`() {
        listOf(
            "Chego amanhã às 15h",
            "Arrive tomorrow at 15:00",
            "Llego mañana a las 15:00",
            "J'arrive demain à 15h",
            "Ich komme morgen um 15 Uhr",
            "Приеду завтра в 15:00",
            "明天15点到",
        ).forEach { assertNull(it, NotificationEventParser.parse(it, now, zone, Locale.ENGLISH)) }
    }

    @Test
    fun `titles lose the date and the dangling prepositions`() {
        fun title(text: String) = NotificationEventParser.parse(text, now, zone, Locale.ENGLISH)!!.title
        assertEquals("Reunión de producto", title("Reunión de producto mañana a las 15:00"))
        assertEquals("Réunion produit", title("Réunion produit demain à 15h"))
        assertEquals("Besprechung Produkt", title("Besprechung Produkt morgen um 15 Uhr"))
    }
}
