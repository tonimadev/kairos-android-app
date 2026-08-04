package digital.tonima.kairos.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.compose.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.compose.cartesian.data.columnModel
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import digital.tonima.core.model.InsightsPeriod
import digital.tonima.kairos.core.R

@Composable
fun MeetingTimeChart(
    meetingStats: List<Pair<String, Float>>,
    selectedPeriod: InsightsPeriod,
    onPeriodChange: (InsightsPeriod) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C38)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = stringResource(id = R.string.insights_time_in_meetings),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options =
                    listOf(
                        InsightsPeriod.DAY to stringResource(id = R.string.insights_period_day),
                        InsightsPeriod.WEEK to stringResource(id = R.string.insights_period_week),
                        InsightsPeriod.MONTH to stringResource(id = R.string.insights_period_month),
                    )
                options.forEachIndexed { index, (period, label) ->
                    SegmentedButton(
                        selected = period == selectedPeriod,
                        onClick = { onPeriodChange(period) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                    ) {
                        Text(text = label)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (meetingStats.isNotEmpty()) {
                val modelProducer = remember { CartesianChartModelProducer() }

                LaunchedEffect(meetingStats) {
                    modelProducer.runTransaction {
                        columnModel {
                            series(meetingStats.map { it.second })
                        }
                    }
                }

                val bottomAxisValueFormatter =
                    CartesianValueFormatter { _, value, _ ->
                        meetingStats.getOrNull(value.toInt())?.first.takeUnless { it.isNullOrBlank() }
                            ?: value.toInt().toString()
                    }

                val axisTitle = stringResource(id = R.string.insights_hours_axis_title)

                ProvideVicoTheme(rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart =
                            rememberCartesianChart(
                                rememberColumnCartesianLayer(),
                                startAxis =
                                    VerticalAxis.rememberStart(
                                        title = { axisTitle },
                                        valueFormatter =
                                            CartesianValueFormatter { _, value, _ ->
                                                "${value.toInt()}h"
                                            },
                                    ),
                                bottomAxis =
                                    HorizontalAxis.rememberBottom(
                                        valueFormatter = bottomAxisValueFormatter,
                                        itemPlacer =
                                            remember { HorizontalAxis.ItemPlacer.aligned(spacing = { 1 }) },
                                    ),
                            ),
                        modelProducer = modelProducer,
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                    )
                }
            } else {
                Text(stringResource(id = R.string.insights_no_data), color = Color.Gray)
            }
        }
    }
}
