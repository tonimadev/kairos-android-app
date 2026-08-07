package digital.tonima.kairos.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import digital.tonima.core.viewmodel.EventIntent
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R

@Composable
fun InsightsContent(
    uiState: EventScreenUiState,
    onIntent: (EventIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = stringResource(id = R.string.insights_title),
            style =
                MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                ),
            modifier = Modifier.padding(bottom = 8.dp),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            InsightCard(
                modifier = Modifier.weight(1f),
                title = stringResource(id = R.string.insights_streak),
                value = stringResource(id = R.string.insights_streak_days, uiState.currentStreak),
                icon = Icons.Rounded.LocalFireDepartment,
                iconTint = Color(0xFFFF9800),
                backgroundColor = Color(0xFF2C2C38),
            )

            InsightCard(
                modifier = Modifier.weight(1f),
                title = stringResource(id = R.string.insights_total_snoozes),
                value = "${uiState.snoozeCount}",
                icon = Icons.Rounded.Bedtime,
                iconTint = Color(0xFF9FA8DA),
                backgroundColor = Color(0xFF2C2C38),
            )
        }

        MeetingTimeChart(
            meetingStats = uiState.meetingStats,
            selectedPeriod = uiState.selectedInsightsPeriod,
            onPeriodChange = { onIntent(EventIntent.ChangeInsightsPeriod(it)) },
        )

        PunctualityCard(score = uiState.punctualityScore)

        if (uiState.isAiUser) {
            InsightCard(
                modifier = Modifier.fillMaxWidth(),
                title = stringResource(id = R.string.insights_ai_usage),
                value = "${uiState.aiUsageCount}",
                icon = Icons.Rounded.AutoAwesome,
                iconTint = Color(0xFFDEFA5F),
                backgroundColor = Color(0xFF2C2C38),
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun InsightCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    iconTint: Color,
    backgroundColor: Color,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconTint,
                modifier = Modifier.size(32.dp),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = value,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = title,
                fontSize = 14.sp,
                color = Color.Gray,
            )
        }
    }
}

@Composable
fun PunctualityCard(score: Int) {
    var animationPlayed by remember { mutableStateOf(false) }

    val animatedProgress by animateFloatAsState(
        targetValue = if (animationPlayed) score / 100f else 0f,
        animationSpec = tween(durationMillis = 1500),
        label = "progress",
    )

    LaunchedEffect(key1 = true) {
        animationPlayed = true
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2C38)),
        shape = RoundedCornerShape(16.dp),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(contentAlignment = Alignment.Center) {
                CircularProgressIndicator(
                    progress = { 1f },
                    color = Color.DarkGray,
                    strokeWidth = 8.dp,
                    modifier = Modifier.size(80.dp),
                )
                CircularProgressIndicator(
                    progress = { animatedProgress },
                    color =
                        if (score > 80) {
                            Color(0xFF4CAF50)
                        } else if (score > 50) {
                            Color(0xFFFFC107)
                        } else {
                            Color(0xFFF44336)
                        },
                    strokeWidth = 8.dp,
                    modifier = Modifier.size(80.dp),
                )
                Text(
                    text = "$score%",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                )
            }

            Column(
                modifier =
                    Modifier
                        .padding(start = 16.dp)
                        .weight(1f),
            ) {
                Text(
                    text = stringResource(id = R.string.insights_punctuality),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text =
                        if (score > 80) {
                            stringResource(id = R.string.insights_punctuality_great)
                        } else {
                            stringResource(id = R.string.insights_punctuality_bad)
                        },
                    fontSize = 14.sp,
                    color = Color.Gray,
                )
            }

            Icon(
                imageVector = Icons.Rounded.CheckCircle,
                contentDescription =
                    stringResource(
                        if (score > 80) R.string.status_active else R.string.status_disabled,
                    ),
                tint = if (score > 80) Color(0xFF4CAF50) else Color.Gray,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}
