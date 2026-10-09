package digital.tonima.kairos.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

private const val DAYS_BEFORE = 90L
private const val DAYS_AFTER = 365L
private const val PADDING_DAYS = 14L

/** Days shown by the strip: a window around today that always includes [selectedEpochDay]. */
internal fun dayStripRange(
    todayEpochDay: Long,
    selectedEpochDay: Long,
): LongRange =
    minOf(
        todayEpochDay - DAYS_BEFORE,
        selectedEpochDay - PADDING_DAYS,
    )..maxOf(todayEpochDay + DAYS_AFTER, selectedEpochDay + PADDING_DAYS)

/**
 * The events are loaded one month at a time, so picking a day of another month must also move the
 * loaded month. Returns the epoch day of that month's first day, or null when [epochDay] is in the
 * month that is already loaded.
 */
internal fun monthToLoadFor(
    epochDay: Long,
    currentMonthEpochDay: Long,
): Long? {
    val month = YearMonth.from(LocalDate.ofEpochDay(epochDay))
    val firstDay = month.atDay(1).toEpochDay()
    return firstDay.takeIf { it != currentMonthEpochDay }
}

/** A single scrollable row of days to pick the day whose events are listed. */
@Composable
fun DayStrip(
    selectedDate: Long,
    currentMonth: Long,
    onMonthChanged: (Long) -> Unit,
    onDateSelected: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val today = remember { LocalDate.now().toEpochDay() }
    val range = remember(today, selectedDate) { dayStripRange(today, selectedDate) }
    val days = remember(range) { range.toList() }
    val state = rememberLazyListState()
    val itemWidthPx = with(LocalDensity.current) { DAY_WIDTH.roundToPx() }

    // Keeps the selected day centered, also when it was picked from the month calendar.
    LaunchedEffect(selectedDate, days) {
        val index = days.indexOf(selectedDate)
        if (index >= 0) {
            val centering = (state.layoutInfo.viewportSize.width - itemWidthPx) / 2
            state.animateScrollToItem(index, scrollOffset = -centering.coerceAtLeast(0))
        }
    }

    LazyRow(
        state = state,
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(days, key = { it }) { epochDay ->
            DayChip(
                epochDay = epochDay,
                isSelected = epochDay == selectedDate,
                isToday = epochDay == today,
                onClick = {
                    monthToLoadFor(epochDay, currentMonth)?.let(onMonthChanged)
                    onDateSelected(epochDay)
                },
            )
        }
    }
}

private val DAY_WIDTH = 52.dp

@Composable
private fun DayChip(
    epochDay: Long,
    isSelected: Boolean,
    isToday: Boolean,
    onClick: () -> Unit,
) {
    val date = remember(epochDay) { LocalDate.ofEpochDay(epochDay) }
    // Read from the configuration so the labels follow a runtime locale change.
    val locale = LocalConfiguration.current.locales[0]
    // The first day of a month shows the month instead of the weekday, so the strip tells months apart.
    val label =
        remember(date, locale) {
            if (date.dayOfMonth == 1) {
                date.month.getDisplayName(TextStyle.SHORT_STANDALONE, locale)
            } else {
                date.dayOfWeek.getDisplayName(TextStyle.SHORT, locale)
            }
        }
    val description =
        remember(date, locale) { date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.FULL).withLocale(locale)) }
    val colors = MaterialTheme.colorScheme

    Surface(
        onClick = onClick,
        modifier =
            Modifier
                .width(DAY_WIDTH)
                .height(64.dp)
                .semantics {
                    contentDescription = description
                    selected = isSelected
                },
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) colors.primary else colors.surfaceVariant,
        contentColor = if (isSelected) colors.onPrimary else colors.onSurfaceVariant,
        border = if (isToday && !isSelected) BorderStroke(1.5.dp, colors.primary) else null,
    ) {
        Column(
            modifier = Modifier.padding(vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = MaterialTheme.typography.labelSmall, maxLines = 1)
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (isSelected || isToday) FontWeight.Bold else FontWeight.Normal,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DayStripPreview() {
    val today = LocalDate.now()
    DayStrip(
        selectedDate = today.toEpochDay(),
        currentMonth = YearMonth.from(today).atDay(1).toEpochDay(),
        onMonthChanged = {},
        onDateSelected = {},
    )
}
