package nl.gymlog.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import nl.gymlog.ui.theme.*

@Composable
fun BulkImportScreen(
    isRunning: Boolean,
    processed: Int,
    total: Int,
    imported: Int,
    failed: Int,
    done: Boolean,
    onPicked: (List<android.net.Uri>) -> Unit,
    onBack: () -> Unit,
    onReset: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(maxItems = 100)
    ) { uris ->
        if (uris.isNotEmpty()) onPicked(uris)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Background)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Terug", tint = TextPrimary)
            }
            Text("Bulk import", color = TextPrimary, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Text(
            "Kies meerdere foto's uit je galerij. De datum komt uit de EXIF-info " +
                "(wanneer je de foto hebt gemaakt), niet van vandaag.",
            color = TextSecondary,
            fontSize = 14.sp
        )

        Spacer(Modifier.height(24.dp))

        when {
            isRunning -> {
                LinearProgressIndicator(
                    progress = if (total == 0) 0f else processed / total.toFloat(),
                    modifier = Modifier.fillMaxWidth(),
                    color = AccentCalories,
                    trackColor = Surface
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "$processed / $total verwerkt",
                    color = TextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Text("$imported sessies opgeslagen", color = TextSecondary, fontSize = 13.sp)
                if (failed > 0) {
                    Text("$failed mislukt", color = Color(0xFFFF6B6B), fontSize = 13.sp)
                }
            }
            done -> {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Klaar!", color = TextPrimary, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text("$imported sessies opgeslagen van $total foto's", color = TextSecondary, fontSize = 14.sp)
                        if (failed > 0) {
                            Text("$failed konden niet verwerkt worden", color = Color(0xFFFF6B6B), fontSize = 13.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        onReset()
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCalories),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Nog een batch", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        onReset()
                        onBack()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Terug naar overzicht", color = TextSecondary)
                }
            }
            else -> {
                Button(
                    onClick = {
                        launcher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentCalories),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Selecteer foto's", color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
