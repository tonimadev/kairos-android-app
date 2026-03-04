package digital.tonima.kairos.wear.service

import android.content.Context
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.ColorBuilders
import androidx.wear.protolayout.DeviceParametersBuilders
import androidx.wear.protolayout.DimensionBuilders
import androidx.wear.protolayout.DimensionBuilders.expand
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.LayoutElementBuilders.ARC_DIRECTION_COUNTER_CLOCKWISE
import androidx.wear.protolayout.LayoutElementBuilders.Arc
import androidx.wear.protolayout.LayoutElementBuilders.ArcText
import androidx.wear.protolayout.LayoutElementBuilders.Box
import androidx.wear.protolayout.LayoutElementBuilders.Column
import androidx.wear.protolayout.LayoutElementBuilders.Row
import androidx.wear.protolayout.LayoutElementBuilders.Spacer
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.material.Colors
import androidx.wear.protolayout.material.CompactChip
import androidx.wear.protolayout.material.Text
import androidx.wear.protolayout.material.Typography
import androidx.wear.protolayout.modifiers.clickable
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import digital.tonima.core.model.Event
import digital.tonima.kairos.core.R
import digital.tonima.kairos.wear.sync.WearEventCache.load
import logcat.LogPriority
import logcat.logcat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val RESOURCES_VERSION = "1"
private const val ID_UPDATE_BUTTON = "update_button"

val customTileColors =
    Colors(
        // primary =
        0xFF6200EE.toInt(),
        // onPrimary =
        0xFFFFFFFF.toInt(),
        // surface =
        0xFF212121.toInt(),
        // onSurface =
        0xFFFFFFFF.toInt(),
    )

@AndroidEntryPoint
@OptIn(ExperimentalHorologistApi::class)
class NextEventTileService : SuspendingTileService() {
    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources =
        ResourceBuilders.Resources
            .Builder()
            .setVersion(RESOURCES_VERSION)
            .build()

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        val deviceParameters = requestParams.deviceConfiguration
        val nextEvent =
            try {
                val cached = load(this@NextEventTileService)
                cached.minByOrNull { it.startTime }
            } catch (t: Throwable) {
                logcat(LogPriority.ERROR) { "Erro ao obter o próximo evento: ${t.localizedMessage}" }
                null
            }
        return TileBuilders.Tile
            .Builder()
            .setResourcesVersion(RESOURCES_VERSION)
            .setTileTimeline(
                TimelineBuilders.Timeline
                    .Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry
                            .Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout
                                    .Builder()
                                    .setRoot(
                                        layout(this@NextEventTileService, deviceParameters, nextEvent),
                                    ).build(),
                            ).build(),
                    ).build(),
            ).build()
    }

    private fun smallSpacer(): Spacer = Spacer.Builder().setHeight(DimensionBuilders.dp(4f)).build()

    private fun layout(
        context: Context,
        deviceParameters: DeviceParametersBuilders.DeviceParameters,
        event: Event?,
    ): LayoutElementBuilders.LayoutElement {
        val launchAppAction =
            ActionBuilders.LaunchAction
                .Builder()
                .setAndroidActivity(
                    ActionBuilders.AndroidActivity
                        .Builder()
                        .setClassName("digital.tonima.kairos.wear.MainActivity")
                        .setPackageName(context.packageName)
                        .build(),
                ).build()

        return Box
            .Builder()
            .setWidth(expand())
            .setHeight(expand())
            .addContent(
                Arc
                    .Builder()
                    .setAnchorAngle(DimensionBuilders.degrees(0f))
                    .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_CENTER)
                    .addContent(
                        ArcText
                            .Builder()
                            .setText(formatCurrentTimeLocalized(context))
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle
                                    .Builder()
                                    .setSize(DimensionBuilders.sp(16f))
                                    .build(),
                            ).build(),
                    ).build(),
            ).addContent(
                Arc
                    .Builder()
                    .setAnchorAngle(DimensionBuilders.degrees(180f))
                    .setAnchorType(LayoutElementBuilders.ARC_ANCHOR_CENTER)
                    .addContent(
                        ArcText
                            .Builder()
                            .setArcDirection(
                                ARC_DIRECTION_COUNTER_CLOCKWISE,
                            ).setText(formatCurrentDateLocalized(context))
                            .setFontStyle(
                                LayoutElementBuilders.FontStyle
                                    .Builder()
                                    .setSize(DimensionBuilders.sp(16f))
                                    .build(),
                            ).build(),
                    ).build(),
            ).addContent(
                Column
                    .Builder()
                    .setModifiers(
                        ModifiersBuilders.Modifiers
                            .Builder()
                            .setPadding(
                                ModifiersBuilders.Padding
                                    .Builder()
                                    .setTop(DimensionBuilders.dp(32f))
                                    .build(),
                            ).setClickable(
                                ModifiersBuilders.Clickable
                                    .Builder()
                                    .setId("whole_tile_clickable")
                                    .setOnClick(launchAppAction)
                                    .build(),
                            ).build(),
                    ).addContent(
                        Row
                            .Builder()
                            .addContent(
                                Text
                                    .Builder(context, context.getString(R.string.next_event))
                                    .setTypography(Typography.TYPOGRAPHY_TITLE3)
                                    .setColor(ColorBuilders.ColorProp.Builder(customTileColors.onSurface).build())
                                    .build(),
                            ).build(),
                    ).addContent(smallSpacer())
                    .apply {
                        if (event != null) {
                            addContent(
                                Text
                                    .Builder(context, event.title)
                                    .setTypography(Typography.TYPOGRAPHY_BODY1)
                                    .setMaxLines(2)
                                    .setColor(ColorBuilders.ColorProp.Builder(customTileColors.onSurface).build())
                                    .build(),
                            )
                            addContent(smallSpacer())
                            addContent(
                                Text
                                    .Builder(context, formatEventTimeLocalized(context, event.startTime))
                                    .setTypography(Typography.TYPOGRAPHY_CAPTION1)
                                    .setColor(ColorBuilders.ColorProp.Builder(customTileColors.onSurface).build())
                                    .build(),
                            )
                            addContent(smallSpacer())
                            addContent(
                                CompactChip
                                    .Builder(
                                        context,
                                        context.getString(R.string.open_event_button),
                                        clickable(launchAppAction, ID_UPDATE_BUTTON),
                                        deviceParameters,
                                    ).build(),
                            )
                        } else {
                            addContent(
                                Text
                                    .Builder(context, context.getString(R.string.no_upcoming_events))
                                    .setTypography(Typography.TYPOGRAPHY_BODY1)
                                    .setColor(ColorBuilders.ColorProp.Builder(customTileColors.onSurface).build())
                                    .setMaxLines(3)
                                    .build(),
                            )
                            addContent(smallSpacer())
                            addContent(
                                CompactChip
                                    .Builder(
                                        context,
                                        context.getString(R.string.open_calendar),
                                        clickable(launchAppAction, ID_UPDATE_BUTTON),
                                        deviceParameters,
                                    ).build(),
                            )
                        }
                    }.build(),
            ).build()
    }

    private fun formatEventTimeLocalized(
        context: Context,
        epochMillis: Long,
    ): String {
        val instant = Instant.ofEpochMilli(epochMillis)
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val locale = context.resources.configuration.locales[0]
        val formatter =
            DateTimeFormatter
                .ofPattern("EEE, HH:mm")
                .withLocale(locale)
        return formatter.format(zonedDateTime)
    }

    private fun formatCurrentTimeLocalized(context: Context): String {
        val instant = Instant.now()
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val locale = context.resources.configuration.locales[0]
        val formatter =
            DateTimeFormatter
                .ofLocalizedTime(java.time.format.FormatStyle.SHORT)
                .withLocale(locale)
        return formatter.format(zonedDateTime)
    }

    private fun formatCurrentDateLocalized(context: Context): String {
        val instant = Instant.now()
        val zonedDateTime = instant.atZone(ZoneId.systemDefault())
        val locale = context.resources.configuration.locales[0]
        val formatter =
            DateTimeFormatter
                .ofLocalizedDate(java.time.format.FormatStyle.MEDIUM)
                .withLocale(locale)
        return formatter.format(zonedDateTime)
    }
}
