package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.WhiteboardDatabase
import com.example.data.repository.BoardRepository
import com.example.ui.ads.AdManager
import com.example.ui.home.HomeScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.whiteboard.WhiteboardScreen
import com.example.ui.whiteboard.WhiteboardViewModel
import com.example.ui.whiteboard.ml.MlKitManager

sealed class AppScreen {
    data object Home : AppScreen()
    data class Whiteboard(val boardId: Long = 0L) : AppScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = WhiteboardDatabase.getDatabase(applicationContext)
        val repository = BoardRepository(database.boardDao())

        setContent {
            // Defer heavy SDK & ML model initialization until right after first UI frame is composed
            LaunchedEffect(Unit) {
                AdManager.initialize(applicationContext)
                MlKitManager.initialize(applicationContext)
            }

            MyApplicationTheme {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFF1E1E1E))
                ) {
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
                    val whiteboardViewModel: WhiteboardViewModel = viewModel()

                    when (currentScreen) {
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
                                    AdManager.showInterstitial(this@MainActivity) {
                                        currentScreen = AppScreen.Home
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
