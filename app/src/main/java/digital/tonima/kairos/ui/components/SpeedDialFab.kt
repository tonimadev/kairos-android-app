package digital.tonima.kairos.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.R
import digital.tonima.kairos.ui.theme.Dimensions

data class SpeedDialItem(
    val icon: Painter,
    val label: String,
    val containerColor: Color? = null,
    val contentColor: Color? = null,
    val onClick: () -> Unit,
)

@Composable
fun SpeedDialFab(
    items: List<SpeedDialItem>,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    val rotation by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "rotation")

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
    ) {
        Column(horizontalAlignment = Alignment.End) {
            items.forEach { item ->
                AnimatedVisibility(
                    visible = expanded,
                    enter = fadeIn() + scaleIn(),
                    exit = fadeOut() + scaleOut(),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = Dimensions.SpacingSmall),
                    ) {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier.padding(end = 8.dp),
                            shadowElevation = 2.dp,
                        ) {
                            Text(
                                text = item.label,
                                style = MaterialTheme.typography.labelMedium,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        SmallFloatingActionButton(
                            onClick = {
                                item.onClick()
                                expanded = false
                            },
                            containerColor = item.containerColor ?: MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = item.contentColor ?: MaterialTheme.colorScheme.onSecondaryContainer,
                        ) {
                            Icon(painter = item.icon, contentDescription = null)
                        }
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { expanded = !expanded },
            containerColor =
                if (expanded) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
            contentColor =
                if (expanded) {
                    MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onPrimary
                },
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_expand_less),
                contentDescription = null,
                modifier = Modifier.rotate(rotation),
            )
        }
    }
}
