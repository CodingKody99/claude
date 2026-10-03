package io.github.codingkody99.einkaufsliste.ui

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Dictates text using Android's own speech recognition.
 *
 * Going through [RecognizerIntent] rather than the `SpeechRecognizer` API means
 * the app needs no microphone permission at all: the system's recogniser asks
 * for it and hands back only the finished transcript.
 */
@Composable
fun DictateButton(
    onText: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "Diktieren",
    prompt: String = "Sag, was du brauchst",
) {
    var unavailable by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        result.data
            ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            ?.firstOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let(onText)
    }

    Column(modifier = modifier) {
        OutlinedButton(
            onClick = {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(
                        RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                        RecognizerIntent.LANGUAGE_MODEL_FREE_FORM,
                    )
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE, "de-DE")
                    putExtra(RecognizerIntent.EXTRA_PROMPT, prompt)
                }
                // Not every device has a recogniser installed.
                unavailable = runCatching { launcher.launch(intent) }.isFailure
            },
        ) {
            Text("🎤")
            Spacer(Modifier.width(8.dp))
            Text(label)
        }

        if (unavailable) {
            Text(
                text = "Auf diesem Gerät ist keine Spracheingabe verfügbar.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}
