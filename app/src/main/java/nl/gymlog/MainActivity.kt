package nl.gymlog

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import nl.gymlog.ui.screens.*
import nl.gymlog.ui.theme.GymLogTheme
import nl.gymlog.viewmodel.CaptureViewModel
import nl.gymlog.viewmodel.HomeViewModel

class MainActivity : ComponentActivity() {
    private val homeVm: HomeViewModel by viewModels()
    private val captureVm: CaptureViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GymLogTheme {
                val navController = rememberNavController()
                val sessions by homeVm.sessions.collectAsStateWithLifecycle()
                val parsed by captureVm.parsed.collectAsStateWithLifecycle()
                val isProcessing by captureVm.isProcessing.collectAsStateWithLifecycle()

                NavHost(navController, startDestination = "home") {
                    composable("home") {
                        HomeScreen(
                            sessions = sessions,
                            onCapture = { navController.navigate("capture") },
                            onMetricClick = { key -> navController.navigate("detail/$key") }
                        )
                    }
                    composable("capture") {
                        if (isProcessing) {
                            LoadingScreen()
                        } else if (parsed != null) {
                            ReviewScreen(
                                parsed = parsed!!,
                                onSave = { session ->
                                    captureVm.saveSession(session)
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onRetake = { captureVm.processPhoto(""); navController.navigate("capture") }
                            )
                        } else {
                            CaptureScreen(
                                onPhotoCaptured = { path -> captureVm.processPhoto(path) },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }
                    composable("detail/{metricKey}") { backStack ->
                        val key = backStack.arguments?.getString("metricKey") ?: return@composable
                        DetailScreen(
                            metricKey = key,
                            sessions = sessions,
                            onBack = { navController.popBackStack() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun LoadingScreen() {
    androidx.compose.foundation.layout.Box(
        modifier = androidx.compose.ui.Modifier
            .fillMaxSize()
            .background(nl.gymlog.ui.theme.Background),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        androidx.compose.material3.CircularProgressIndicator(color = nl.gymlog.ui.theme.AccentCalories)
    }
}
