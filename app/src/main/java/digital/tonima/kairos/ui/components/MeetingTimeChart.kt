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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.Chart
import com.patrykandpatrick.vico.compose.chart.column.columnChart
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.entry.ChartEntryModelProducer
import com.patrykandpatrick.vico.core.entry.FloatEntry
import digital.tonima.core.model.InsightsPeriod

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
                text = "Time in Meetings",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.align(Alignment.Start),
            )

            Spacer(modifier = Modifier.height(16.dp))

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val options =
                    listOf(InsightsPeriod.DAY to "Day", InsightsPeriod.WEEK to "Week", InsightsPeriod.MONTH to "Month")
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
                val chartEntryModelProducer =
                    remember(meetingStats) {
                        val entries =
                            meetingStats.mapIndexed { index, pair ->
                                FloatEntry(x = index.toFloat(), y = pair.second)
                            }
                        ChartEntryModelProducer(entries)
                    }

                val bottomAxisValueFormatter =
                    AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _ ->
                        val index = value.toInt()
                        if (index >= 0 && index < meetingStats.size) {
                            meetingStats[index].first
                        } else {
                            ""
                        }
                    }

                ProvideChartStyle {
                    Chart(
                        chart = columnChart(),
                        chartModelProducer = chartEntryModelProducer,
                        startAxis =
                            rememberStartAxis(
                                title = "Hours",
                                valueFormatter = { value, _ -> "${value.toInt()}h" },
                            ),
                        bottomAxis = rememberBottomAxis(valueFormatter = bottomAxisValueFormatter),
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                    )
                }
            } else {
                Text("No data available", color = Color.Gray)
            }
        }
    }
}
