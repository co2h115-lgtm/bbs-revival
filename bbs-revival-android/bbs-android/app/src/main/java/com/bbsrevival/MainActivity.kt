package com.bbsrevival

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bbsrevival.data.api.TokenStore
import com.bbsrevival.ui.navigation.BbsNavHost
import com.bbsrevival.ui.navigation.Screen
import com.bbsrevival.ui.theme.BbsColors
import com.bbsrevival.ui.theme.BbsRevivalTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

data class NavItem(val label: String, val icon: ImageVector, val route: String)

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var tokenStore: TokenStore

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val isLoggedIn = tokenStore.accessToken != null

        setContent {
            BbsRevivalTheme {
                BbsApp(isLoggedIn = isLoggedIn)
            }
        }
    }
}

@Composable
fun BbsApp(isLoggedIn: Boolean) {
    val navController = rememberNavController()
    val currentRoute by navController.currentBackStackEntryAsState()
    val route = currentRoute?.destination?.route

    val topLevelRoutes = listOf(
        Screen.Boards.route,
        Screen.Chat.route,
        Screen.Files.route,
        Screen.Doors.route,
        Screen.Profile.route,
    )
    val isTopLevel = topLevelRoutes.any { route?.startsWith(it.substringBefore("{")) == true }
    val isAuthScreen = route == Screen.Login.route || route == Screen.Register.route

    val navItems = listOf(
        NavItem("BOARDS",  Icons.Default.Forum,          Screen.Boards.route),
        NavItem("CHAT",    Icons.Default.Chat,           Screen.Chat.route),
        NavItem("FILES",   Icons.Default.Folder,         Screen.Files.route),
        NavItem("DOORS",   Icons.Default.SportsEsports,  Screen.Doors.route),
        NavItem("PROFILE", Icons.Default.Person,         Screen.Profile.route),
    )

    Scaffold(
        containerColor = BbsColors.Bg,
        bottomBar = {
            if (!isAuthScreen && isLoggedIn) {
                NavigationBar(
                    containerColor = BbsColors.BgSurface,
                    tonalElevation = androidx.compose.ui.unit.Dp.Unspecified,
                ) {
                    navItems.forEach { item ->
                        val selected = route?.startsWith(item.route.substringBefore("{")) == true
                        NavigationBarItem(
                            selected = selected,
                            onClick  = {
                                navController.navigate(item.route) {
                                    popUpTo(Screen.Boards.route) { saveState = true }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(item.icon, contentDescription = item.label) },
                            label = {
                                Text(
                                    item.label,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            },
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor       = BbsColors.Cyan,
                                selectedTextColor       = BbsColors.Cyan,
                                unselectedIconColor     = BbsColors.FgDim,
                                unselectedTextColor     = BbsColors.FgDim,
                                indicatorColor          = BbsColors.CyanDim,
                            ),
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            BbsNavHost(navController = navController, isLoggedIn = isLoggedIn)
        }
    }
}
