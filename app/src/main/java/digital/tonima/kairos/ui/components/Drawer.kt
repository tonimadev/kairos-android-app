package digital.tonima.kairos.ui.components

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.R.drawable.favorite
import digital.tonima.kairos.R.drawable.star
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.R.drawable.ic_k_monochrome

@Composable
fun DrawerContent(
    isProUser: Boolean,
    onUpgradeToProClick: () -> Unit,
    onOurOtherAppsClick: () -> Unit,
    onCloseDrawer: () -> Unit,
) {
    val context = LocalContext.current
    val versionName =
        remember {
            try {
                val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                packageInfo.versionName ?: "N/A"
            } catch (e: PackageManager.NameNotFoundException) {
                e.printStackTrace()
                "N/A"
            }
        }

    ModalDrawerSheet(
        drawerShape = RoundedCornerShape(topEnd = 24.dp, bottomEnd = 24.dp),
    ) {
        Box(modifier = Modifier.fillMaxHeight()) {
            Column {
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(topEnd = 24.dp),
                            ).padding(horizontal = 20.dp, vertical = 24.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier =
                                Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                painter = painterResource(ic_k_monochrome),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                            )
                            if (isProUser) {
                                Text(
                                    text = "Pro ✨",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                if (!isProUser) {
                    NavigationDrawerItem(
                        icon = { Icon(painterResource(star), contentDescription = null) },
                        label = { Text(stringResource(R.string.remove_ads)) },
                        selected = false,
                        onClick = {
                            onUpgradeToProClick()
                            onCloseDrawer()
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                    )
                }

                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

                NavigationDrawerItem(
                    icon = { Icon(painterResource(favorite), contentDescription = null) },
                    label = { Text(stringResource(R.string.our_other_apps)) },
                    selected = false,
                    onClick = {
                        onOurOtherAppsClick()
                        onCloseDrawer()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            Text(
                text = stringResource(R.string.version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
            )
        }
    }
}
