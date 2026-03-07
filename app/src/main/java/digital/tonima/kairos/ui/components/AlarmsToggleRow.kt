package digital.tonima.kairos.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import digital.tonima.kairos.R.drawable.alarm
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun AlarmsToggleRow(
    modifier: Modifier = Modifier,
    alarmsEnabled: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    val cardColor by animateColorAsState(
        targetValue =
            if (alarmsEnabled) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceVariant
            },
        animationSpec = tween(400),
        label = "alarmCardBg",
    )
    val iconBg by animateColorAsState(
        targetValue =
            if (alarmsEnabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.outline
            },
        animationSpec = tween(400),
        label = "alarmIconBg",
    )

    val stateDescription =
        stringResource(
            if (alarmsEnabled) R.string.cd_alarms_enabled else R.string.cd_alarms_disabled,
        )

    Card(
        modifier =
            modifier
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {
                    role = Role.Switch
                    this.stateDescription = stateDescription
                },
        shape = RoundedCornerShape(Dimensions.RadiusExtraLarge),
        elevation =
            CardDefaults.cardElevation(
                defaultElevation = if (alarmsEnabled) Dimensions.ElevationMedium else Dimensions.ElevationExtraSmall,
            ),
        colors = CardDefaults.cardColors(containerColor = cardColor),
        onClick = { onToggle(!alarmsEnabled) },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = Dimensions.PaddingNormal, vertical = Dimensions.EventCardHorizontalPadding),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimensions.SpacingDefault),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(Dimensions.IconSizeLarge)
                            .clip(CircleShape)
                            .background(iconBg),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(alarm),
                        contentDescription = stringResource(R.string.cd_alarm_icon),
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(Dimensions.IconSizeMedium),
                    )
                }
                Column {
                    Text(
                        text = stringResource(R.string.activate_event_alarms),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color =
                            if (alarmsEnabled) {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                    Text(
                        text = if (alarmsEnabled) "✓ Ativo" else "Desativado",
                        style = MaterialTheme.typography.labelSmall,
                        color =
                            if (alarmsEnabled) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.outline
                            },
                    )
                }
            }
            Switch(
                checked = alarmsEnabled,
                onCheckedChange = null, // Handled by Card onClick for better a11y
                colors =
                    SwitchDefaults.colors(
                        checkedTrackColor = MaterialTheme.colorScheme.primary,
                        checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AlarmsToggleRowPreview() {
    AlarmsToggleRow(
        alarmsEnabled = true,
        onToggle = {},
    )
}
