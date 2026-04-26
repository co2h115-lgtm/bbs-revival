package com.bbsrevival.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bbsrevival.ui.screens.*

sealed class Screen(val route: String) {
    // Auth
    object Login       : Screen("login")
    object Register    : Screen("register")

    // Main tabs (bottom nav)
    object Boards      : Screen("boards")
    object Chat        : Screen("chat")
    object Files       : Screen("files")
    object Doors       : Screen("doors")
    object Profile     : Screen("profile")

    // Drill-down
    object ThreadList  : Screen("thread_list/{boardId}?name={name}") {
        fun go(boardId: String, name: String) = "thread_list/$boardId?name=$name"
    }
    object Thread      : Screen("thread/{threadId}") {
        fun go(threadId: String) = "thread/$threadId"
    }
    object Gallery     : Screen("gallery")
    object Messages    : Screen("messages")
    object Search      : Screen("search")
    object DoorGame    : Screen("door_game/{gameId}/{gameSlug}/{gameName}") {
        fun go(gameId: String, gameSlug: String, gameName: String) =
            "door_game/$gameId/$gameSlug/${gameName.replace("/","_")}"
    }
}

@Composable
fun BbsNavHost(navController: NavHostController, isLoggedIn: Boolean) {
    NavHost(
        navController = navController,
        startDestination = if (isLoggedIn) Screen.Boards.route else Screen.Login.route,
    ) {
        composable(Screen.Login.route)    { LoginScreen(navController) }
        composable(Screen.Register.route) { RegisterScreen(navController) }

        composable(Screen.Boards.route)   { BoardsScreen(navController) }
        composable(Screen.Chat.route)     { ChatScreen() }
        composable(Screen.Files.route)    { FilesScreen(navController) }
        composable(Screen.Doors.route)    { DoorsScreen(navController) }
        composable(Screen.Profile.route)  { ProfileScreen(navController) }
        composable(Screen.Gallery.route)  { GalleryScreen() }
        composable(Screen.Messages.route) { MessagesScreen() }
        composable(Screen.Search.route)   { SearchScreen(navController) }

        composable(
            route = Screen.ThreadList.route,
            arguments = listOf(
                navArgument("boardId") { type = NavType.StringType },
                navArgument("name")    { type = NavType.StringType; defaultValue = "Board" },
            )
        ) { back ->
            ThreadListScreen(
                boardId = back.arguments!!.getString("boardId")!!,
                boardName = back.arguments!!.getString("name") ?: "Board",
                navController = navController,
            )
        }

        composable(
            route = Screen.Thread.route,
            arguments = listOf(navArgument("threadId") { type = NavType.StringType })
        ) { back ->
            ThreadScreen(
                threadId = back.arguments!!.getString("threadId")!!,
                navController = navController,
            )
        }

        composable(
            route = Screen.DoorGame.route,
            arguments = listOf(
                navArgument("gameId")   { type = NavType.StringType },
                navArgument("gameSlug") { type = NavType.StringType },
                navArgument("gameName") { type = NavType.StringType },
            )
        ) { back ->
            DoorGameScreen(
                gameId   = back.arguments!!.getString("gameId")!!,
                gameSlug = back.arguments!!.getString("gameSlug")!!,
                gameName = back.arguments!!.getString("gameName")!!,
                navController = navController,
            )
        }
    }
}
