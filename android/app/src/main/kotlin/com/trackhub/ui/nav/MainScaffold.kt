package com.trackhub.ui.nav

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.trackhub.ui.food.FoodScreen
import com.trackhub.ui.home.HomeScreen
import com.trackhub.ui.journal.JournalScreen
import com.trackhub.ui.profile.ProfileScreen
import com.trackhub.ui.workout.WorkoutScreen
import kotlinx.coroutines.launch

private data class MainTab(
    val title: String,
    val icon: ImageVector,
)

// Order matches web: food / workout / home / journal / profile — home in center.
private val TABS = listOf(
    MainTab("Питание", Icons.Default.Restaurant),
    MainTab("Тренировки", Icons.Default.FitnessCenter),
    MainTab("Главная", Icons.Default.Home),
    MainTab("Журнал", Icons.Default.EditNote),
    MainTab("Профиль", Icons.Default.AccountCircle),
)

@Composable
fun MainScaffold(onLoggedOut: () -> Unit) {
    val pagerState = rememberPagerState(initialPage = 2, pageCount = { TABS.size })
    val scope = rememberCoroutineScope()

    Scaffold(
        bottomBar = {
            NavigationBar {
                TABS.forEachIndexed { idx, t ->
                    NavigationBarItem(
                        selected = pagerState.currentPage == idx,
                        onClick = { scope.launch { pagerState.animateScrollToPage(idx) } },
                        icon = { Icon(t.icon, contentDescription = t.title) },
                        label = { Text(t.title) },
                    )
                }
            }
        },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.padding(padding),
            beyondViewportPageCount = 1,
        ) { page ->
            when (page) {
                0 -> FoodScreen()
                1 -> WorkoutScreen()
                2 -> HomeScreen()
                3 -> JournalScreen()
                4 -> ProfileScreen(onLoggedOut = onLoggedOut)
            }
        }
    }
}
