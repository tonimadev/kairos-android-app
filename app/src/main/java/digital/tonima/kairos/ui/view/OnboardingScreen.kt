package digital.tonima.kairos.ui.view

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Alarm
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import digital.tonima.kairos.core.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    val titleRes: Int,
    val descRes: Int,
    val icon: ImageVector,
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    val pages =
        listOf(
            OnboardingPage(
                titleRes = R.string.onboarding_title_1,
                descRes = R.string.onboarding_desc_1,
                icon = Icons.Rounded.CalendarMonth,
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_2,
                descRes = R.string.onboarding_desc_2,
                icon = Icons.Rounded.Alarm,
            ),
            OnboardingPage(
                titleRes = R.string.onboarding_title_3,
                descRes = R.string.onboarding_desc_3,
                icon = Icons.Rounded.AutoAwesome,
            ),
        )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        containerColor = Color(0xFF25252D),
    ) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier =
                    Modifier
                        .weight(1f)
                        .fillMaxWidth(),
            ) { position ->
                OnboardingPageContent(page = pages[position])
            }

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Page Indicators
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    repeat(pages.size) { iteration ->
                        val color = if (pagerState.currentPage == iteration) Color(0xFFDEFA5F) else Color.DarkGray
                        Box(
                            modifier =
                                Modifier
                                    .padding(2.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .size(if (pagerState.currentPage == iteration) 10.dp else 8.dp),
                        )
                    }
                }

                // Buttons
                if (pagerState.currentPage == pages.lastIndex) {
                    Button(
                        onClick = onFinish,
                        modifier = Modifier.height(48.dp),
                        colors =
                            ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDEFA5F),
                                contentColor = Color.Black,
                            ),
                        shape = RoundedCornerShape(24.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_btn_start),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                } else {
                    TextButton(
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        modifier = Modifier.height(48.dp),
                    ) {
                        Text(
                            text = stringResource(id = R.string.onboarding_btn_next),
                            color = Color(0xFFDEFA5F),
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = page.icon,
            contentDescription = null,
            tint = Color(0xFFDEFA5F),
            modifier = Modifier.size(120.dp),
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = stringResource(id = page.titleRes),
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(id = page.descRes),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.LightGray,
            textAlign = TextAlign.Center,
        )
    }
}
