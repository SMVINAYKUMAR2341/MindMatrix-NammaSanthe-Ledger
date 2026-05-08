package com.nammasanthe.ledger.ui.screens

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nammasanthe.ledger.gemini.GeminiService
import com.nammasanthe.ledger.gemini.GeminiSettings
import com.nammasanthe.ledger.gemini.GeminiSettingsStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeminiSettingsScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settingsStore = remember { GeminiSettingsStore(context) }
    val settings by settingsStore.settings.collectAsStateWithLifecycle(
        initialValue = GeminiSettings()
    )

    var apiKey by remember { mutableStateOf("") }
    var useGemini by remember { mutableStateOf(false) }
    var geminiEnabled by remember { mutableStateOf(false) }
    var showApiKey by remember { mutableStateOf(false) }
    var isValidating by remember { mutableStateOf(false) }
    var validationResult by remember { mutableStateOf<Boolean?>(null) }

    // Sync with stored settings
    LaunchedEffect(settings) {
        apiKey = settings.apiKey
        useGemini = settings.useGeminiForOcr
        geminiEnabled = settings.geminiEnabled
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gemini API Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            scope.launch {
                                settingsStore.saveSettings(
                                    GeminiSettings(
                                        apiKey = apiKey,
                                        useGeminiForOcr = useGemini,
                                        geminiEnabled = geminiEnabled
                                    )
                                )
                                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
                            }
                        }
                    ) {
                        Icon(Icons.Default.Check, contentDescription = "Save")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Info Card
            OutlinedCard(
                colors = CardDefaults.outlinedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "About Gemini API",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Gemini provides more accurate OCR for handwritten Kannada text compared to on-device recognition. You need your own API key from Google AI Studio.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            // Enable Gemini Toggle
            Card {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable Gemini OCR",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "Use Gemini API for text recognition",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = geminiEnabled,
                        onCheckedChange = { geminiEnabled = it }
                    )
                }
            }

            // API Key Input
            if (geminiEnabled) {
                OutlinedTextField(
                    value = apiKey,
                    onValueChange = { 
                        apiKey = it
                        validationResult = null // Reset validation on change
                    },
                    label = { Text("Gemini API Key") },
                    placeholder = { Text("Paste your API key here") },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = if (showApiKey) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Done
                    ),
                    trailingIcon = {
                        Row {
                            if (apiKey.isNotBlank()) {
                                IconButton(onClick = { apiKey = "" }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                            IconButton(onClick = { showApiKey = !showApiKey }) {
                                Text(if (showApiKey) "Hide" else "Show")
                            }
                        }
                    },
                    supportingText = {
                        Text("Get your key from ai.google.dev")
                    }
                )

                // Validate Button
                Button(
                    onClick = {
                        if (apiKey.isBlank()) {
                            Toast.makeText(context, "Please enter an API key", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        scope.launch {
                            isValidating = true
                            val service = GeminiService()
                            validationResult = service.validateApiKey(apiKey)
                            isValidating = false
                            
                            Toast.makeText(
                                context,
                                if (validationResult == true) "API key is valid!" else "Invalid API key",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    },
                    enabled = apiKey.isNotBlank() && !isValidating,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isValidating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text("Validate API Key")
                }

                // Validation Result
                validationResult?.let { isValid ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isValid) {
                                Color(0xFF4CAF50).copy(alpha = 0.1f)
                            } else {
                                Color(0xFFF44336).copy(alpha = 0.1f)
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (isValid) Icons.Default.Check else Icons.Default.Warning,
                                contentDescription = null,
                                tint = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (isValid) "API key is valid and working!" 
                                else "Invalid API key. Please check and try again.",
                                color = if (isValid) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                        }
                    }
                }

                // Use Gemini for OCR Toggle
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Use Gemini for Bill Scanning",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            "When enabled, bill scans will use Gemini API instead of on-device OCR",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = useGemini,
                        onCheckedChange = { useGemini = it },
                        enabled = apiKey.isNotBlank() && validationResult == true
                    )
                }
            }

            // Warning about data usage
            if (geminiEnabled && useGemini) {
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFF9800).copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFFF9800)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Note: Using Gemini requires internet connection. Images will be sent to Google's servers for processing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFFF9800)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            // Get API Key Button
            OutlinedButton(
                onClick = {
                    // Open browser to get API key
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://ai.google.dev/")
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Get Free API Key from Google AI")
            }
        }
    }
}
