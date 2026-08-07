package digital.tonima.kairos.ui.components

import android.Manifest
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import digital.tonima.core.model.Weather
import digital.tonima.kairos.core.R.string.location_permission_weather_desc
import digital.tonima.kairos.core.R.string.provide_permission
import digital.tonima.kairos.ui.theme.Dimensions

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherCard(
    weather: Weather?,
    isWeatherLoading: Boolean,
    weatherError: String?,
    isTemperatureInCelsius: Boolean,
    onFetchWeather: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val locationPermissionsState =
        rememberMultiplePermissionsState(
            permissions =
                listOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
        )

    LaunchedEffect(locationPermissionsState.allPermissionsGranted) {
        if (locationPermissionsState.allPermissionsGranted && weather == null) {
            onFetchWeather()
        }
    }

    val gradientBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    MaterialTheme.colorScheme.primaryContainer,
                    MaterialTheme.colorScheme.secondaryContainer,
                ),
        )

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = Dimensions.ElevationMedium),
        shape = RoundedCornerShape(Dimensions.RadiusLarge),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(gradientBrush)
                    .padding(Dimensions.PaddingNormal),
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                if (locationPermissionsState.allPermissionsGranted) {
                    if (weather != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier =
                                        Modifier
                                            .size(48.dp)
                                            .clip(CircleShape)
                                            .background(
                                                MaterialTheme.colorScheme
                                                    .onPrimaryContainer.copy(alpha = 0.1f),
                                            ),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector =
                                            if (weather.conditionCode == 800) {
                                                Icons.Rounded.WbSunny
                                            } else {
                                                Icons.Rounded.Cloud
                                            },
                                        contentDescription = weather.description,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                                Spacer(modifier = Modifier.width(Dimensions.SpacingNormal))
                                Column {
                                    val unit = if (isTemperatureInCelsius) "°C" else "°F"
                                    val tempInt = weather.temperature.toInt()
                                    Text(
                                        text = "$tempInt$unit",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    )
                                    Text(
                                        text = weather.description.replaceFirstChar { it.uppercase() },
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                                    )
                                }
                            }
                        }
                    } else if (weatherError != null) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Warning,
                                contentDescription = weatherError,
                                tint = MaterialTheme.colorScheme.error,
                            )
                            Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                            Text(
                                text = weatherError,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.weight(1f),
                            )
                            TextButton(onClick = onFetchWeather) {
                                Text(stringResource(digital.tonima.kairos.core.R.string.try_again))
                            }
                        }
                    } else {
                        // Loading state or trigger
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Cloud,
                                contentDescription =
                                    stringResource(
                                        digital.tonima.kairos.core.R.string.loading_weather,
                                    ),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Spacer(modifier = Modifier.width(Dimensions.SpacingSmall))
                            Text(
                                text = stringResource(digital.tonima.kairos.core.R.string.loading_weather),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                        }
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.LocationOn,
                                contentDescription = stringResource(location_permission_weather_desc),
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(Dimensions.SpacingNormal))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(digital.tonima.kairos.core.R.string.weather_updates),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            Text(
                                text = stringResource(location_permission_weather_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Dimensions.SpacingNormal))
                    Button(
                        onClick = { locationPermissionsState.launchMultiplePermissionRequest() },
                        modifier = Modifier.fillMaxWidth(),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                                contentColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    ) {
                        Text(stringResource(provide_permission), fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}
