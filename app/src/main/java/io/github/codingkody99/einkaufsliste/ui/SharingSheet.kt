package io.github.codingkody99.einkaufsliste.ui

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.codingkody99.einkaufsliste.domain.HouseholdCode

/**
 * Pairs two phones onto the same lists.
 *
 * One device starts a household and reads out the code, the other types it in.
 * No accounts, no e-mail addresses — the code is the whole handshake.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharingSheet(
    state: SharingState,
    onJoinCodeChange: (String) -> Unit,
    onStart: () -> Unit,
    onJoin: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .navigationBarsPadding(),
        ) {
            Text(
                text = "Gemeinsam nutzen",
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Listen und Rezepte auf beiden Handys – auch auf dem iPhone.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(20.dp))

            if (state.isShared) {
                val code = state.householdCode.orEmpty()
                val readable = HouseholdCode.format(code)

                Surface(
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dein Haushalts-Code",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            text = readable,
                            style = MaterialTheme.typography.headlineSmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(
                                    Intent.EXTRA_TEXT,
                                    "Unser Einkaufslisten-Code: $readable",
                                )
                            }
                            context.startActivity(Intent.createChooser(intent, "Code senden"))
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Code senden")
                    }
                    TextButton(
                        onClick = { clipboard.setText(AnnotatedString(readable)) },
                    ) {
                        Text("Kopieren")
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Auf dem anderen Gerät unten eintragen – dann hängen beide " +
                        "an denselben Listen.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(
                    text = "Deine Listen liegen nur auf diesem Handy.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Spacer(Modifier.height(12.dp))
                Button(onClick = onStart, modifier = Modifier.fillMaxWidth()) {
                    Text("Haushalt anlegen")
                }
            }

            Spacer(Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(20.dp))

            Text(
                text = "Oder einem Haushalt beitreten",
                style = MaterialTheme.typography.titleSmall,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.Top) {
                OutlinedTextField(
                    value = state.joinCode,
                    onValueChange = onJoinCodeChange,
                    singleLine = true,
                    isError = state.joinFailed,
                    label = { Text("Code") },
                    placeholder = { Text("ABCD-EFGH-JKMN") },
                    supportingText = if (state.joinFailed) {
                        { Text("Dieser Code stimmt nicht – bitte nochmal prüfen.") }
                    } else {
                        null
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Characters,
                        keyboardType = KeyboardType.Ascii,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = onJoin,
                    enabled = state.canJoin,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text("Beitreten")
                }
            }

            if (state.isShared) {
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onStop) {
                    Text("Gemeinsame Nutzung beenden")
                }
            }

            Spacer(Modifier.height(16.dp))
            Text(
                text = "Hinweis: Der Code legt fest, welche Geräte zusammengehören. " +
                    "Der Abgleich übers Internet wird gerade eingerichtet – bis dahin " +
                    "bleibt alles auf dem jeweiligen Gerät.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}
