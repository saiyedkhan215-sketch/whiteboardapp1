package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

sealed class AppScreen {
    data object Home : AppScreen()
    data class Whiteboard(val boardId: Long = 0L) : AppScreen()
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContext = applicationContext
        val database = WhiteboardDatabase.getDatabase(appContext)
        val repository = BoardRepository(database.boardDao())

        setContent {
            // Defer heavy SDK initializations (AdMob & ML Kit) until after first frame renders
            LaunchedEffect(Unit) {
                withContext(Dispatchers.IO) {
                    AdManager.initialize(appContext)
                    MlKitManager.initialize(appContext)
                }
            }

            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color(0xFF1E1E1E)
                ) {
                    var currentScreen by remember { mutableStateOf<AppScreen>(AppScreen.Home) }
                    val whiteboardViewModel: WhiteboardViewModel = viewModel()

                    when (currentScreen) {
                        is AppScreen.Home -> {
                            HomeScreen(
                                repository = repository,
                                onNewBoard = {
                                    whiteboardViewModel.loadBoard(0L)
                                    AdManager.showInterstitial(this@MainActivity) {
                                        currentScreen = AppScreen.Whiteboard(0L)
                                    }
                                },
                                onOpenBoard = { boardId ->
                                    whiteboardViewModel.loadBoard(boardId)
                                    AdManager.showInterstitial(this@MainActivity) {
                                        currentScreen = AppScreen.Whiteboard(boardId)
                                    }
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
