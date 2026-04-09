package digital.tonima.kairos.ui.components

import android.content.Intent
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.core.net.toUri
import digital.tonima.kairos.R.drawable.favorite
import digital.tonima.kairos.R.drawable.ic_share
import digital.tonima.kairos.R.drawable.star
import digital.tonima.kairos.core.R
import digital.tonima.kairos.core.R.drawable.ic_k_monochrome
import digital.tonima.kairos.ui.theme.Dimensions

@Composable
fun DrawerContent(
    isProUser: Boolean,
    isAiUser: Boolean,
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
        drawerShape = RoundedCornerShape(topEnd = Dimensions.PaddingLarge, bottomEnd = Dimensions.PaddingLarge),
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState()),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(topEnd = Dimensions.PaddingLarge),
                        ).padding(
                            horizontal = Dimensions.RadiusExtraLarge,
                            vertical = Dimensions.PaddingLarge,
                        ),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier =
                            Modifier
                                .size(Dimensions.IconSizeExtraLarge)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            painter = painterResource(ic_k_monochrome),
                            contentDescription = stringResource(R.string.cd_app_logo),
                            tint = MaterialTheme.colorScheme.onPrimary,
                            modifier = Modifier.size(Dimensions.IconSizeDrawer),
                        )
                    }
                    Spacer(modifier = Modifier.width(Dimensions.EventCardHorizontalPadding))
                    Column {
                        Text(
                            text = stringResource(R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                        if (isProUser) {
                            val label =
                                if (isAiUser) {
                                    stringResource(
                                        R.string.pro_ia_label,
                                    )
                                } else {
                                    stringResource(R.string.pro_label)
                                }
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(Dimensions.PaddingDefault))

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

            if (isAiUser) {
                NavigationDrawerItem(
                    icon = { Icon(painterResource(star), contentDescription = null) },
                    label = { Text(stringResource(R.string.manage_subscription)) },
                    selected = false,
                    onClick = {
                        val browserIntent =
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://play.google.com/store/account/subscriptions".toUri(),
                            )
                        context.startActivity(browserIntent)
                        onCloseDrawer()
                    },
                    modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
                )
            }

            HorizontalDivider(
                modifier =
                    Modifier.padding(
                        horizontal = Dimensions.PaddingNormal,
                        vertical = Dimensions.PaddingSmall,
                    ),
            )

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

            val shareText = stringResource(R.string.share_text)
            NavigationDrawerItem(
                icon = { Icon(painterResource(ic_share), contentDescription = null) },
                label = { Text(stringResource(R.string.share_app)) },
                selected = false,
                onClick = {
                    val sendIntent: Intent =
                        Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, shareText)
                            type = "text/plain"
                        }
                    val shareIntent = Intent.createChooser(sendIntent, null)
                    context.startActivity(shareIntent)
                    onCloseDrawer()
                },
                modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding),
            )

            Spacer(modifier = Modifier.height(Dimensions.PaddingNormal))
            Text(
                text = stringResource(R.string.version, versionName),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(Dimensions.PaddingNormal),
            )
        }
    }
}
