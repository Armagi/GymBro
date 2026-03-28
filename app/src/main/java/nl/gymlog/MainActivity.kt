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
                val pendingDate by captureVm.pendingDate.collectAsStateWithLifecycle()

                NavHost(navController, startDestination = "home") {

                    composable("home") {
                        HomeScreen(
                            sessions = sessions,
                            onCapture = { navController.navigate("capture") },
                            onMetricClick = { key -> navController.navigate("detail/$key") },
                            onSessionEdit = { session -> navController.navigate("edit/${session.id}") },
                            onSessionDelete = { session -> homeVm.delete(session) }
                        )
                    }

                    composable("capture") {
                        when {
                            isProcessing -> LoadingScreen()
                            parsed != null -> ReviewScreen(
                                parsed = parsed!!,
                                existingSession = null,
                                initialDate = pendingDate,
                                onSave = { session ->
                                    captureVm.saveSession(session)
                                    navController.navigate("home") {
                                        popUpTo("home") { inclusive = true }
                                    }
                                },
                                onUpdate = {},
                                onRetake = {
                                    captureVm.reset()
                                    navController.navigate("capture") {
                                        popUpTo("capture") { inclusive = true }
                                    }
                                }
                            )
                            else -> CaptureScreen(
                                onPhotoCaptured = { path -> captureVm.processPhoto(path) },
                                onGalleryImagePicked = { uri -> captureVm.processGalleryUri(uri) },
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

                    composable("edit/{sessionId}") { backStack ->
                        val sessionId = backStack.arguments?.getString("sessionId")?.toIntOrNull()
                            ?: return@composable
                        // Reactive lookup — avoids null on cold start
                        val session = sessions.find { it.id == sessionId } ?: return@composable
                        ReviewScreen(
                            parsed = null,
                            existingSession = session,
                            initialDate = session.date,
                            onSave = {},
                            onUpdate = { updated ->
                                homeVm.update(updated)
                                navController.popBackStack()
                            },
                            onRetake = { navController.popBackStack() }
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
