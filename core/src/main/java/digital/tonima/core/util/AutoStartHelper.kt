package digital.tonima.core.util

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import digital.tonima.kairos.core.R

/**
 * Tenta abrir a tela de configurações de "Início Automático" (Autostart) específica do fabricante.
 * Muitos fabricantes (Xiaomi, Huawei, etc.) exigem essa permissão para que os apps
 * funcionem corretamente em segundo plano após uma reinicialização.
 */
fun openAutostartSettings(context: Context) {
    try {
        val intents = listOf(
            Intent().setComponent(
                ComponentName(
                    "com.miui.securitycenter",
                    "com.miui.permcenter.autostart.AutoStartManagementActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.letv.android.letvsafe",
                    "com.letv.android.letvsafe.AutobootManageActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.huawei.systemmanager",
                    "com.huawei.systemmanager.optimize.process.ProtectActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.coloros.safecenter",
                    "com.coloros.safecenter.permission.startup.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.oppo.safe",
                    "com.oppo.safe.permission.startup.StartupAppListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.iqoo.secure",
                    "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.vivo.permissionmanager",
                    "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"
                )
            ),
            Intent().setComponent(
                ComponentName(
                    "com.samsung.android.lool",
                    "com.samsung.android.sm.ui.battery.BatteryActivity"
                )
            ),
            Intent().setComponent(ComponentName("com.htc.pitroad", "com.htc.pitroad.landingpage.LandingPageActivity")),
            Intent().setComponent(
                ComponentName(
                    "com.asus.mobilemanager",
                    "com.asus.mobilemanager.autostart.AutoStartActivity"
                )
            )
        )

        var didStartActivity = false
        for (intent in intents) {
            if (context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) != null) {
                try {
                    context.startActivity(intent)
                    didStartActivity = true
                    break
                } catch (e: Exception) {
                }
            }
        }
        if (!didStartActivity) {
            openAppDetailsSettingsWithToast(context)
        }
    } catch (e: RuntimeException) {
        openAppDetailsSettingsWithToast(context)
    }
}

private fun openAppDetailsSettingsWithToast(context: Context) {
    Toast.makeText(context, context.getString(R.string.autostart_fallback_toast), Toast.LENGTH_LONG).show()
    val settingsIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    try {
        settingsIntent.data = Uri.fromParts("package", context.packageName, null)
    } catch (e: RuntimeException) {
    }
    context.startActivity(settingsIntent)
}

/**
 * Verifica se o dispositivo é de um fabricante conhecido por exigir
 * permissões de início automático.
 */
fun needsAutostartPermission(): Boolean {
    val manufacturer = Build.MANUFACTURER.lowercase()
    val knownManufacturers =
        listOf("xiaomi", "oppo", "vivo", "oneplus", "huawei", "samsung", "asus", "letv", "iqoo", "htc")
    return knownManufacturers.any { manufacturer.contains(it) }
}
