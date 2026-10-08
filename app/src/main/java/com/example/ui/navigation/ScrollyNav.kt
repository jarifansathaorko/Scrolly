package com.example.ui.navigation

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.battles.BattlesScreen
import com.example.ui.screens.battles.BattlesViewModel
import com.example.ui.screens.block.BlockScreen
import com.example.ui.screens.block.BlockViewModel
import com.example.ui.screens.home.HomeScreen
import com.example.ui.screens.home.HomeViewModel
import com.example.ui.screens.profile.ProfileScreen
import com.example.ui.screens.profile.ProfileViewModel
import com.example.ui.screens.stats.StatsScreen
import com.example.ui.screens.stats.StatsViewModel
import com.example.ui.theme.SleekBg
import com.example.ui.theme.SleekBorder
import com.example.ui.theme.SleekCardSurface
import com.example.ui.theme.SleekPillBg
import com.example.ui.theme.SleekPillText
import com.example.ui.theme.SleekTextMuted

enum class ScrollyTab(
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    HOME("Home", Icons.Filled.Home, Icons.Outlined.Home),
    STATS("Stats", Icons.Filled.BarChart, Icons.Outlined.BarChart),
    BATTLES("Battles", Icons.Filled.FlashOn, Icons.Outlined.FlashOn),
    BLOCK("Block", Icons.Filled.Block, Icons.Outlined.Block),
    PROFILE("Profile", Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun ScrollyAppScaffold() {
    var selectedTab by rememberSaveable { mutableStateOf(ScrollyTab.HOME) }

    // Only the visible tab's ViewModel is created. The previous version instantiated all
    // five up front, so every screen's repository flows and permission queries ran even
    // when their tab was never opened.
    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = SleekBg,
            bottomBar = {
                // No navigationBarsPadding() here: the Scaffold already applies the bottom
                // system-bar inset, and doing both doubled the gap above the bar.
                NavigationBar(
                    modifier = Modifier
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                        .border(1.dp, SleekBorder, RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    containerColor = SleekCardSurface,
                    tonalElevation = 0.dp
                ) {
                    ScrollyTab.entries.forEach { tab ->
                        val isSelected = selectedTab == tab
                        NavigationBarItem(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            icon = {
                                Icon(
                                    imageVector = if (isSelected) tab.selectedIcon else tab.unselectedIcon,
                                    contentDescription = null,
                                    modifier = Modifier.size(22.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = tab.title,
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                )
                            },
                            alwaysShowLabel = true,
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = SleekPillText,
                                selectedTextColor = SleekPillText,
                                indicatorColor = SleekPillBg,
                                unselectedIconColor = SleekTextMuted,
                                unselectedTextColor = SleekTextMuted
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (selectedTab) {
                ScrollyTab.HOME -> HomeTab(innerPadding)
                ScrollyTab.STATS -> StatsTab(innerPadding)
                ScrollyTab.BATTLES -> BattlesTab(innerPadding)
                ScrollyTab.BLOCK -> BlockTab(innerPadding)
                ScrollyTab.PROFILE -> ProfileTab(innerPadding)
            }
        }
    }
}

@Composable
private fun HomeTab(innerPadding: PaddingValues) {
    val viewModel: HomeViewModel = viewModel()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        HomeScreen(viewModel = viewModel)
    }
}

@Composable
private fun StatsTab(innerPadding: PaddingValues) {
    val viewModel: StatsViewModel = viewModel()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        StatsScreen(viewModel = viewModel)
    }
}

@Composable
private fun BattlesTab(innerPadding: PaddingValues) {
    val viewModel: BattlesViewModel = viewModel()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        BattlesScreen(viewModel = viewModel)
    }
}

@Composable
private fun BlockTab(innerPadding: PaddingValues) {
    val viewModel: BlockViewModel = viewModel()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        BlockScreen(viewModel = viewModel)
    }
}

@Composable
private fun ProfileTab(innerPadding: PaddingValues) {
    val viewModel: ProfileViewModel = viewModel()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
    ) {
        ProfileScreen(viewModel = viewModel)
    }
}
