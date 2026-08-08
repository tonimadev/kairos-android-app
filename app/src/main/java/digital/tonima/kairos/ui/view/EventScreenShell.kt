package digital.tonima.kairos.ui.view

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import digital.tonima.core.viewmodel.EventScreenUiState
import digital.tonima.kairos.core.R
import digital.tonima.kairos.ui.components.DrawerContent
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventScreenShell(
    uiState: EventScreenUiState,
    isProUser: Boolean,
    isAiUser: Boolean,
    snackbarHostState: SnackbarHostState,
    onUpgradeToPro: () -> Unit,
    onSettingsClick: () -> Unit,
    onChatHistoryClick: () -> Unit,
    onImportCalendarClick: () -> Unit,
    onManageCalendarsClick: () -> Unit,
    onCreateEventClick: () -> Unit,
    onGenerateDailyBriefing: (String) -> Unit,
    onShowAiSuggestions: () -> Unit,
    onBottomTabChange: (Int) -> Unit,
    showShell: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val googleCalendarNotFound = stringResource(R.string.google_calendar_not_found)
    val dailyBriefingPrompt = stringResource(R.string.prompt_daily_briefing)

    if (!showShell) {
        Box(modifier = Modifier.fillMaxSize()) {
            content(PaddingValues(0.dp))
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }
        return
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = drawerState.isOpen,
        drawerContent = {
            DrawerContent(
                isProUser = isProUser,
                isAiUser = isAiUser,
                onUpgradeToProClick = onUpgradeToPro,
                onOurOtherAppsClick = {
                    val browserIntent =
                        Intent(
                            Intent.ACTION_VIEW,
                            "https://play.google.com/store/apps/dev?id=6594602823307179845".toUri(),
                        )
                    context.startActivity(browserIntent)
                },
                onSettingsClick = onSettingsClick,
                onChatHistoryClick = onChatHistoryClick,
                onImportCalendarClick = onImportCalendarClick,
                onManageCalendarsClick = onManageCalendarsClick,
                onCloseDrawer = { scope.launch { drawerState.close() } },
            )
        },
    ) {
        Scaffold(
            topBar = { EventTopBar(onOpenMenu = { scope.launch { drawerState.open() } }) },
            bottomBar = {
                EventBottomBar(
                    uiState = uiState,
                    isAiUser = isAiUser,
                    onOpenCalendar = {
                        val intent =
                            context.packageManager
                                .getLaunchIntentForPackage("com.google.android.calendar")
                        if (intent != null) {
                            context.startActivity(intent)
                        } else {
                            Toast.makeText(context, googleCalendarNotFound, Toast.LENGTH_SHORT).show()
                        }
                    },
                    onBottomTabChange = onBottomTabChange,
                    onGenerateDailyBriefing = { onGenerateDailyBriefing(dailyBriefingPrompt) },
                    onShowAiSuggestions = onShowAiSuggestions,
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onCreateEventClick,
                    containerColor = Color(0xFFDEFA5F),
                    shape = CircleShape,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Add Alarm",
                        tint = Color.Black,
                        modifier = Modifier.size(32.dp),
                    )
                }
            },
            floatingActionButtonPosition = androidx.compose.material3.FabPosition.End,
            containerColor = Color(0xFF25252D),
            snackbarHost = { SnackbarHost(snackbarHostState) },
        ) { paddingValues ->
            Box(modifier = Modifier.fillMaxSize()) {
                content(paddingValues)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventTopBar(onOpenMenu: () -> Unit) {
    TopAppBar(
        title = {
            Text(
                text = stringResource(R.string.alarm),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
        actions = {
            IconButton(onClick = onOpenMenu) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = stringResource(R.string.cd_open_menu),
                    tint = Color.White,
                )
            }
        },
    )
}

@Composable
private fun EventBottomBar(
    uiState: EventScreenUiState,
    isAiUser: Boolean,
    onOpenCalendar: () -> Unit,
    onBottomTabChange: (Int) -> Unit,
    onGenerateDailyBriefing: () -> Unit,
    onShowAiSuggestions: () -> Unit,
) {
    NavigationBar(
        containerColor = Color(0xFF2C2C38),
        contentColor = Color.White,
        tonalElevation = 0.dp,
    ) {
        val navItemColors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = Color(0xFFDEFA5F),
                selectedTextColor = Color(0xFFDEFA5F),
                indicatorColor = Color.Transparent,
                unselectedIconColor = Color(0xFFB0B0C0),
                unselectedTextColor = Color(0xFFB0B0C0),
            )

        NavigationBarItem(
            selected = uiState.selectedBottomTab == 0,
            onClick = { onBottomTabChange(0) },
            icon = { Icon(Icons.Rounded.Alarm, contentDescription = stringResource(R.string.alarms)) },
            label = { Text(stringResource(R.string.alarms)) },
            colors = navItemColors,
        )

        NavigationBarItem(
            selected = uiState.selectedBottomTab == 1,
            onClick = { onBottomTabChange(1) },
            icon = {
                Icon(
                    Icons.Rounded.AutoAwesome,
                    contentDescription = stringResource(R.string.insights_title),
                )
            },
            label = { Text(stringResource(R.string.insights_title)) },
            colors = navItemColors,
        )

        if (isAiUser) {
            NavigationBarItem(
                selected = false,
                onClick = onOpenCalendar,
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.calendar)) },
                label = { Text(stringResource(R.string.calendar)) },
                colors = navItemColors,
            )

            NavigationBarItem(
                selected = false,
                onClick = onGenerateDailyBriefing,
                icon = { Icon(Icons.Rounded.AutoAwesome, contentDescription = stringResource(R.string.ai_briefing)) },
                label = { Text(stringResource(R.string.briefing)) },
                colors = navItemColors,
            )
            NavigationBarItem(
                selected = false,
                onClick = onShowAiSuggestions,
                icon = { Icon(Icons.Rounded.Mic, contentDescription = stringResource(R.string.voice)) },
                label = { Text(stringResource(R.string.voice)) },
                colors = navItemColors,
            )
        } else {
            NavigationBarItem(
                selected = false,
                onClick = onOpenCalendar,
                icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = stringResource(R.string.calendar)) },
                label = { Text(stringResource(R.string.calendar)) },
                colors = navItemColors,
            )
        }
    }
}
