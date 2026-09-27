package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.WhiteboardDatabase
import com.example.data.repository.BoardRepository
import com.example.ui.ads.RewardedAdManager
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.whiteboard.WhiteboardScreen
import com.example.ui.whiteboard.WhiteboardViewModel

sealed class AppScreen {
    data object Home : AppScreen()
    data class Whiteboard(val boardId: Long = 0L) : AppScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WhiteboardDatabase.getDatabase(this)
        val repository = BoardRepository(database.boardDao())

        // Initialize Google Mobile Ads and preload rewarded test ad
        RewardedAdManager.initialize(this)

        setContent {
            MyApplicationTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
                    val whiteboardViewModel: WhiteboardViewModel = viewModel()

                    when (val screen = currentScreen) {
                        is AppScreen.Home -> {
                            HomeScreen(
                                repository = repository,
                                onNewBoard = {
                                    whiteboardViewModel.loadBoard(0L)
                                    currentScreen = AppScreen.Whiteboard(0L)
                                },
                                onOpenBoard = { boardId ->
                                    whiteboardViewModel.loadBoard(boardId)
                                    currentScreen = AppScreen.Whiteboard(boardId)
                                }
                            )
                        }
                        is AppScreen.Whiteboard -> {
                            WhiteboardScreen(
                                viewModel = whiteboardViewModel,
                                onNavigateBack = {
                                    currentScreen = AppScreen.Home
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
